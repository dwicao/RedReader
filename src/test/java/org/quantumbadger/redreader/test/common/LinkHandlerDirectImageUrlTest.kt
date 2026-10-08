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

package org.quantumbadger.redreader.test.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.quantumbadger.redreader.common.LinkHandler
import org.quantumbadger.redreader.common.UriString

class LinkHandlerDirectImageUrlTest {

	@Test
	fun redditImageHostsAreDirect() {
		assertTrue(LinkHandler.isDirectImageUrl(
			UriString("https://i.redd.it/abc123.jpg")))
		assertTrue(LinkHandler.isDirectImageUrl(
			UriString("https://i.redd.it/abc123")))
		assertTrue(LinkHandler.isDirectImageUrl(
			UriString("https://preview.redd.it/xyz.png?width=1080&format=png&auto=webp&s=abc")))
	}

	@Test
	fun genericImageExtensionsAreDirect() {
		assertTrue(LinkHandler.isDirectImageUrl(
			UriString("https://example.com/photo.jpeg")))
		assertTrue(LinkHandler.isDirectImageUrl(
			UriString("https://example.com/pic.png")))
		assertTrue(LinkHandler.isDirectImageUrl(
			UriString("https://example.com/anim.gif")))
	}

	@Test
	fun videoUrlsAreNotDirect() {
		assertFalse(LinkHandler.isDirectImageUrl(
			UriString("https://v.redd.it/abc123")))
		assertFalse(LinkHandler.isDirectImageUrl(
			UriString("https://example.com/movie.mp4")))
		assertFalse(LinkHandler.isDirectImageUrl(
			UriString("https://example.com/clip.webm")))
	}

	@Test
	fun webPagesAreNotDirect() {
		assertFalse(LinkHandler.isDirectImageUrl(
			UriString("https://imgur.com/abcdefg")))
		assertFalse(LinkHandler.isDirectImageUrl(
			UriString("https://gfycat.com/somegif")))
		assertFalse(LinkHandler.isDirectImageUrl(
			UriString("https://www.reddit.com/r/pics/comments/abc/title/")))
	}

	@Test
	fun nullIsNotDirect() {
		assertFalse(LinkHandler.isDirectImageUrl(null))
	}
}
