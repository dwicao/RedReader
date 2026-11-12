/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.image

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.General.readWholeStreamAsUTF8
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.LinkHandler
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.http.FailedRequestBody
import java.io.IOException
import java.util.UUID
import java.util.regex.Pattern

object RedditPlayerPageAPI {

	private const val TAG = "RedditPlayerPageAPI"

	private val JSON = Json {
		ignoreUnknownKeys = true
	}

	private val vRedditPlaylistPattern =
		Pattern.compile("https://v\\.redd\\.it/([A-Za-z0-9]+)/(?:DASHPlaylist\\.mpd|HLSPlaylist\\.m3u8)")

	private val vRedditUrlPattern =
		Pattern.compile("https://v\\.redd\\.it/([A-Za-z0-9]+)")

	fun playerLinkId(playerPageUrl: UriString): String? {
		return LinkHandler.getRedditPlayerPageLinkId(playerPageUrl)
	}

	fun playerVideoId(playerPageUrl: UriString): String? {
		return LinkHandler.getRedditPlayerPageVideoId(playerPageUrl)
	}

	fun oldRedditCommentsUrl(linkId: String): UriString {
		return UriString("https://old.reddit.com/comments/" + linkId + "/")
	}

	fun extractVRedditId(html: String): String? {

		// Video URLs are often backslash-escaped inside embedded JSON blobs.
		val unescaped = html.replace("\\/", "/")

		val playlistMatcher = vRedditPlaylistPattern.matcher(unescaped)
		if(playlistMatcher.find()) {
			val id = playlistMatcher.group(1)
			if(id != null && id.length > 3) {
				return id
			}
		}

		val urlMatcher = vRedditUrlPattern.matcher(unescaped)
		if(urlMatcher.find()) {
			val id = urlMatcher.group(1)
			if(id != null && id.length > 3) {
				return id
			}
		}

		return null
	}

	fun extractApiVideoId(postListingJson: String): String? {

		return try {
			val root = JSON.parseToJsonElement(postListingJson).jsonObject
			val children = root["data"]?.jsonObject?.get("children")?.jsonArray
				?: return null
			val data = children.firstOrNull()?.jsonObject?.get("data")?.jsonObject
				?: return null
			val video = data["secure_media"]?.jsonObject?.get("reddit_video")?.jsonObject
				?: data["media"]?.jsonObject?.get("reddit_video")?.jsonObject
				?: return null
			val dashUrl = video["dash_url"]?.jsonPrimitive?.content ?: return null

			val matcher = vRedditUrlPattern.matcher(dashUrl)
			if(matcher.find()) {
				matcher.group(1)
			} else {
				null
			}
		} catch(e: Exception) {
			null
		}
	}

	fun getImageInfo(
		context: Context,
		playerPageUrl: UriString,
		priority: Priority,
		listener: GetImageInfoListener
	) {
		tryDirectVideoId(context, playerPageUrl, priority, listener)
	}

	private fun tryDirectVideoId(
		context: Context,
		playerPageUrl: UriString,
		priority: Priority,
		listener: GetImageInfoListener
	) {
		val directId = playerVideoId(playerPageUrl)

		if(directId == null) {
			tryPostApi(context, playerPageUrl, priority, listener)
			return
		}

		// The /video/ segment is often the v.redd.it id itself: no page
		// fetch needed at all. On failure fall through to the other methods.
		RedditVideosAPI.getImageInfo(
			context,
			directId,
			priority,
			object : GetImageInfoListener {
				override fun onSuccess(info: ImageInfo) {
					listener.onSuccess(info)
				}

				override fun onFailure(error: RRError) {
					tryPostApi(context, playerPageUrl, priority, listener)
				}

				override fun onNotAnImage() {
					tryPostApi(context, playerPageUrl, priority, listener)
				}
			})
	}

	private fun tryPostApi(
		context: Context,
		playerPageUrl: UriString,
		priority: Priority,
		listener: GetImageInfoListener
	) {
		val linkId = playerLinkId(playerPageUrl)

		if(linkId == null) {
			tryOldRedditPage(context, null, playerPageUrl, priority, listener)
			return
		}

		fetchApiVideoId(context, linkId, priority,
			onResult = { videoId ->
				if(videoId == null) {
					tryOldRedditPage(context, linkId, playerPageUrl, priority, listener)
				} else {
					RedditVideosAPI.getImageInfo(context, videoId, priority, listener)
				}
			},
			onError = {
				tryOldRedditPage(context, linkId, playerPageUrl, priority, listener)
			})
	}

	private fun tryOldRedditPage(
		context: Context,
		linkId: String?,
		playerPageUrl: UriString,
		priority: Priority,
		listener: GetImageInfoListener
	) {
		if(linkId == null) {
			fetchHtmlVideoId(context, playerPageUrl, priority, listener, true)
			return
		}

		fetchHtmlVideoId(
			context,
			oldRedditCommentsUrl(linkId),
			priority,
			object : GetImageInfoListener {
				override fun onSuccess(info: ImageInfo) {
					listener.onSuccess(info)
				}

				override fun onFailure(error: RRError) {
					fetchHtmlVideoId(context, playerPageUrl, priority, listener, true)
				}

				override fun onNotAnImage() {
					fetchHtmlVideoId(context, playerPageUrl, priority, listener, true)
				}
			})
	}

	private fun fetchApiVideoId(
		context: Context,
		linkId: String,
		priority: Priority,
		onResult: (String?) -> Unit,
		onError: () -> Unit
	) {
		val apiUrl = UriString(Constants.Reddit.getUri("/by_id/t3_" + linkId).value)

		CacheManager.getInstance(context).makeRequest(
			CacheRequest(
				apiUrl,
				RedditAccountManager.getInstance(context).getDefaultAccount(),
				null,
				priority,
				DownloadStrategyIfNotCached.INSTANCE,
				Constants.FileType.IMAGE_INFO,
				CacheRequest.DownloadQueueType.IMMEDIATE,
				context,
				object : CacheRequestCallbacks {

					override fun onDataStreamComplete(
						stream: GenericFactory<SeekableInputStream, IOException>,
						timestamp: TimestampUTC,
						session: UUID,
						fromCache: Boolean,
						mimetype: String?
					) {
						val json = try {
							stream.create().use(::readWholeStreamAsUTF8)
						} catch(e: IOException) {
							Log.e(TAG, "Got exception", e)
							onError()
							return
						}

						onResult(extractApiVideoId(json))
					}

					override fun onFailure(error: RRError) {
						onError()
					}
				}))
	}

	private fun fetchHtmlVideoId(
		context: Context,
		pageUrl: UriString,
		priority: Priority,
		listener: GetImageInfoListener,
		preferWwwHost: Boolean = false
	) {
		// The player page is served from www.reddit.com regardless of which
		// reddit mirror the link was copied from.
		val fetchUrl = if(preferWwwHost) {
			val normalized = LinkHandler.convertAndNormalizeUri(pageUrl)
			UriString(
				normalized.buildUpon().encodedAuthority("www.reddit.com").build().toString())
		} else {
			pageUrl
		}

		CacheManager.getInstance(context).makeRequest(
			CacheRequest(
				pageUrl,
				RedditAccountManager.getAnon(),
				null,
				priority,
				DownloadStrategyIfNotCached.INSTANCE,
				Constants.FileType.IMAGE_INFO,
				CacheRequest.DownloadQueueType.IMMEDIATE,
				context,
				object : CacheRequestCallbacks {

					override fun onDataStreamComplete(
						stream: GenericFactory<SeekableInputStream, IOException>,
						timestamp: TimestampUTC,
						session: UUID,
						fromCache: Boolean,
						mimetype: String?
					) {
						val html = try {
							stream.create().use(::readWholeStreamAsUTF8)
						} catch(e: IOException) {
							Log.e(TAG, "Got exception", e)
							listener.onFailure(
								getGeneralErrorForFailure(
									context,
									CacheRequest.RequestFailureType.STORAGE,
									e,
									null,
									pageUrl,
									FailedRequestBody.from(stream)))
							return
						}

						val videoId = try {
							extractVRedditId(html)
						} catch(e: Exception) {
							Log.e(TAG, "Got exception", e)
							null
						}

						if(videoId == null) {
							listener.onFailure(
								getGeneralErrorForFailure(
									context,
									CacheRequest.RequestFailureType.PARSE,
									null,
									null,
									pageUrl,
									Optional.of(FailedRequestBody(html))))
							return
						}

						RedditVideosAPI.getImageInfo(context, videoId, priority, listener)
					}

					override fun onFailure(error: RRError) {
						listener.onFailure(error)
					}
				}))
	}
}
