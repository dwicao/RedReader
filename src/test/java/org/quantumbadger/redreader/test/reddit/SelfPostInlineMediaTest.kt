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

package org.quantumbadger.redreader.test.reddit

import androidx.appcompat.app.AppCompatActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.kthings.RedditPost
import org.quantumbadger.redreader.reddit.kthings.RedditTimestampUTC
import org.quantumbadger.redreader.reddit.kthings.UrlEncodedString
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SelfPostInlineMediaTest {

	private val activity: AppCompatActivity =
		Robolectric.buildActivity(AppCompatActivity::class.java).get()

	@Before
	fun setUp() {
		initPrefs(RuntimeEnvironment.getApplication())
		setInlinePostMedia("never")
	}

	private fun setInlinePostMedia(value: String) {
		General.getSharedPrefs(RuntimeEnvironment.getApplication())
			.edit()
			.putString("pref_images_inline_post_media", value)
			.apply()
	}

	/**
	 * Initialise PrefsUtility's static state directly, as the full init path calls
	 * General.initAppConfig(), which doesn't work under Robolectric.
	 */
	private fun initPrefs(app: android.app.Application) {
		try {
			val cachedPrefsField = General::class.java.getDeclaredField("mPrefs")
			cachedPrefsField.isAccessible = true
			@Suppress("UNCHECKED_CAST")
			(cachedPrefsField.get(null) as java.util.concurrent.atomic.AtomicReference<Any?>)
				.set(null)

			val resField = PrefsUtility::class.java.getDeclaredField("mRes")
			resField.isAccessible = true
			resField.set(null, app.resources)

			val prefsField = PrefsUtility::class.java.getDeclaredField("sharedPrefs")
			prefsField.isAccessible = true
			prefsField.set(null, General.getSharedPrefs(app))

			val contextField = PrefsUtility::class.java.getDeclaredField("appContext")
			contextField.isAccessible = true
			contextField.set(null, app)
		} catch (e: ReflectiveOperationException) {
			throw RuntimeException(e)
		}
	}

	private fun post(selftextHtml: String?) = RedditPost(
		id = "id",
		name = RedditIdAndType("t3_id"),
		subreddit = UrlEncodedString("test"),
		num_comments = 0,
		score = 0,
		permalink = UrlEncodedString("/r/test/comments/id"),
		created_utc = RedditTimestampUTC(TimestampUTC.ZERO),
		is_self = true,
		selftext_html = selftextHtml?.let(::UrlEncodedString),
		media_metadata = InlineMediaResolverTest.loadTestData()
	)

	@Test
	fun inlineImagesDisabledByDefault() {
		val parsed = RedditParsedPost(
			activity,
			post(InlineMediaHtmlTest.IMAGE_LINK),
			parseSelfText = true)

		assertTrue(parsed.inlineMedia.isEmpty())
	}

	@Test
	fun inlineImagesAreCollected() {

		setInlinePostMedia("always")

		val parsed = RedditParsedPost(
			activity,
			post(InlineMediaHtmlTest.IMAGE_LINK),
			parseSelfText = true)

		assertEquals(1, parsed.inlineMedia.size)
	}

	@Test
	fun noMediaMeansNoElements() {

		setInlinePostMedia("always")

		val parsed = RedditParsedPost(
			activity,
			post("<p>Just some text</p>"),
			parseSelfText = true)

		assertTrue(parsed.inlineMedia.isEmpty())
	}

	@Test
	fun imageElementsAreCreatedFromSelfText() {

		setInlinePostMedia("always")

		val parsed = RedditParsedPost(
			activity,
			post(InlineMediaHtmlTest.IMAGE_LINK),
			parseSelfText = true)

		for (media in parsed.inlineMedia) {
			assertNotNull(media.media.stills.last().url)
		}
	}
}
