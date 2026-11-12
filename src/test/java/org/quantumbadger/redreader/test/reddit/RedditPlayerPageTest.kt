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
import org.quantumbadger.redreader.common.LinkHandler
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.image.RedditPlayerPageAPI

class RedditPlayerPageTest {

	@Test
	fun playerUrlsAreRecognised() {
		assertTrue(LinkHandler.isRedditPlayerPage(UriString(
			"https://www.reddit.com/link/abc123/video/def456789/player")))
		assertTrue(LinkHandler.isRedditPlayerPage(UriString(
			"https://reddit.com/link/abc123/video/def456789/player")))
		assertTrue(LinkHandler.isRedditPlayerPage(UriString(
			"http://old.reddit.com/link/abc123/video/def456789/player")))
		assertTrue(LinkHandler.isRedditPlayerPage(UriString(
			"https://m.reddit.com/link/abc123/video/def456789/player?x=1")))
	}

	@Test
	fun nonPlayerUrlsAreRejected() {
		assertFalse(LinkHandler.isRedditPlayerPage(UriString(
			"https://www.reddit.com/link/abc123/video/def456789")))
		assertFalse(LinkHandler.isRedditPlayerPage(UriString(
			"https://www.reddit.com/r/videos/comments/abc123/title_here/")))
		assertFalse(LinkHandler.isRedditPlayerPage(UriString(
			"https://v.redd.it/def456789")))
		assertFalse(LinkHandler.isRedditPlayerPage(UriString(
			"https://notreddit.com/link/abc123/video/def456789/player")))
		assertFalse(LinkHandler.isRedditPlayerPage(UriString("")))
	}

	@Test
	fun dashPlaylistMentionYieldsVideoId() {
		assertEquals(
			"abc123xyz00",
			RedditPlayerPageAPI.extractVRedditId(
				"<html>\"dashUrl\":\"https://v.redd.it/abc123xyz00/DASHPlaylist.mpd\"</html>"))
	}

	@Test
	fun hlsPlaylistMentionYieldsVideoId() {
		assertEquals(
			"abc123xyz00",
			RedditPlayerPageAPI.extractVRedditId(
				"sources:[{src:\"https://v.redd.it/abc123xyz00/HLSPlaylist.m3u8\"}]"))
	}

	@Test
	fun escapedSlashesAreHandled() {
		assertEquals(
			"abc123xyz00",
			RedditPlayerPageAPI.extractVRedditId(
				"{\\\"dashUrl\\\":\\\"https:\\/\\/v.redd.it\\/abc123xyz00\\/DASHPlaylist.mpd\\\"}"))
	}

	@Test
	fun playlistMentionWinsOverBareMention() {
		assertEquals(
			"xyz987uvw12",
			RedditPlayerPageAPI.extractVRedditId(
				"poster=\"https://v.redd.it/abc00/DASH_thumb.jpg\" "
					+ "src=\"https://v.redd.it/xyz987uvw12/DASHPlaylist.mpd\""))
	}

	@Test
	fun bareMentionYieldsVideoId() {
		assertEquals(
			"abc123xyz00",
			RedditPlayerPageAPI.extractVRedditId(
				"<video src=\"https://v.redd.it/abc123xyz00/DASH_720.mp4\"/>"))
	}

	@Test
	fun garbageYieldsNull() {
		assertNull(RedditPlayerPageAPI.extractVRedditId(""))
		assertNull(RedditPlayerPageAPI.extractVRedditId("<html><body>no video here</body></html>"))
		assertNull(RedditPlayerPageAPI.extractVRedditId(
			"https://v.redd.it/ab/DASHPlaylist.mpd"))
	}
}
