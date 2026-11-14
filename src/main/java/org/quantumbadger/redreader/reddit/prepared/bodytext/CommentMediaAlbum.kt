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

package org.quantumbadger.redreader.reddit.prepared.bodytext

import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.image.AlbumInfo
import org.quantumbadger.redreader.image.ImageInfo
import org.quantumbadger.redreader.image.ImageSize
import org.quantumbadger.redreader.image.ImageUrlInfo
import org.quantumbadger.redreader.reddit.prepared.InlineMedia

/**
 * Builds an [AlbumInfo] from the media attached to a single comment (or post), so that the
 * image viewer can show them as one album which the user can swipe through, instead of
 * opening each one on its own.
 */
object CommentMediaAlbum {

	/**
	 * @param pageUrl The URL of the page the media was linked to (used for album fetch
	 * caching when navigating between images)
	 */
	@JvmStatic
	fun buildAlbum(
		elements: List<BodyElementInlineMedia>,
		pageUrl: UriString
	): AlbumInfo {
		val images = elements.map { element ->
			val media = element.media

			val animated = media.type == InlineMedia.Type.ANIMATED

			ImageInfo(
				original = ImageUrlInfo(
					url = media.stills.last().url,
					size = ImageSize(media.width, media.height)
				),
				caption = element.caption,
				type = if (animated) "AnimatedImage" else "Image",
				isAnimated = animated,
				mediaType = if (animated) {
					ImageInfo.MediaType.GIF
				} else {
					ImageInfo.MediaType.IMAGE
				},
				hasAudio = ImageInfo.HasAudio.NO_AUDIO
			)
		}

		return AlbumInfo(pageUrl, null, null, images)
	}
}
