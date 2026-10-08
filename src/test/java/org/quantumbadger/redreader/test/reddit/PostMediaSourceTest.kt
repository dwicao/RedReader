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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost

class PostMediaSourceTest {

	private val previewUrl = UriString("https://preview.redd.it/abc.jpg?width=1080")
	private val thumbnailUrl = UriString("https://b.thumbs.redditmedia.com/thumb.jpg")
	private val imageUrl = UriString("https://i.redd.it/xyz.jpg")
	private val pageUrl = UriString("https://www.example.com/article")

	@Test
	fun previewUrlWinsForBothSourcesWhenPresent() {
		assertEquals(
			previewUrl,
			RedditPreparedPost.resolveThumbnailSource(
				previewUrl, thumbnailUrl, imageUrl, true))
		assertEquals(
			previewUrl,
			RedditPreparedPost.resolveInlinePreviewSource(
				previewUrl, thumbnailUrl, imageUrl, true))
	}

	@Test
	fun thumbnailSourcePrefersNativeThumbnailOverImageUrl() {
		assertEquals(
			thumbnailUrl,
			RedditPreparedPost.resolveThumbnailSource(
				null, thumbnailUrl, imageUrl, true))
	}

	@Test
	fun thumbnailSourceFallsBackToImageUrl() {
		assertEquals(
			imageUrl,
			RedditPreparedPost.resolveThumbnailSource(
				null, UriString("nsfw"), imageUrl, true))
		assertEquals(
			imageUrl,
			RedditPreparedPost.resolveThumbnailSource(null, null, imageUrl, true))
	}

	@Test
	fun inlinePreviewSourcePrefersImageUrlOverThumbnail() {
		assertEquals(
			imageUrl,
			RedditPreparedPost.resolveInlinePreviewSource(
				null, thumbnailUrl, imageUrl, true))
	}

	@Test
	fun inlinePreviewSourceFallsBackToThumbnail() {
		assertEquals(
			thumbnailUrl,
			RedditPreparedPost.resolveInlinePreviewSource(
				null, thumbnailUrl, pageUrl, false))
	}

	@Test
	fun sentinelThumbnailsAreRejected() {
		for(value in listOf("", "nsfw", "self", "default", "spoiler", "NSFW", "Self")) {
			assertFalse(RedditPreparedPost.isUsableThumbnailUrl(UriString(value)))
		}
		assertTrue(RedditPreparedPost.isUsableThumbnailUrl(thumbnailUrl))
		assertFalse(RedditPreparedPost.isUsableThumbnailUrl(null))
	}

	@Test
	fun nothingUsableResolvesToNull() {
		assertNull(
			RedditPreparedPost.resolveThumbnailSource(
				null, UriString("default"), pageUrl, false))
		assertNull(
			RedditPreparedPost.resolveInlinePreviewSource(null, null, pageUrl, false))
		assertNull(
			RedditPreparedPost.resolveInlinePreviewSource(
				UriString(""), null, null, false))
	}
}
