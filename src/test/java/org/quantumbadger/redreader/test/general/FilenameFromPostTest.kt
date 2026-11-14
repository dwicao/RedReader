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

package org.quantumbadger.redreader.test.general

import org.junit.Assert.assertEquals
import org.junit.Test
import org.quantumbadger.redreader.common.General

class FilenameFromPostTest {

	@Test
	fun fullComponents() {
		assertEquals(
			"RedReader-aww-user456-checkoutmycat.jpg",
			General.filenameFromPost(
				"https://i.redd.it/checkoutmycat.jpg",
				"aww",
				"user456",
				"Check out my CAT!!!"))
	}

	@Test
	fun nonAsciiTitleIsDropped() {
		assertEquals(
			"RedReader-askreddit-user.jpg",
			General.filenameFromPost(
				"https://i.redd.it/x.jpg",
				"AskReddit",
				"user",
				"ほげふが"))
	}

	@Test
	fun vRedditHasNoExtensionGetsMp4() {
		assertEquals(
			"RedReader-gifs-user-mycat.mp4",
			General.filenameFromPost(
				"https://v.redd.it/abc123",
				"gifs",
				"user",
				"my cat"))
	}

	@Test
	fun extensionTakenFromUrlPath() {
		assertEquals(
			"RedReader-gifs-user-cat.png",
			General.filenameFromPost(
				"https://i.redd.it/cat.png?width=640&auto=webp",
				"gifs",
				"user",
				"cat"))
	}

	@Test
	fun authorAndSubredditPreserveUnderscore() {
		assertEquals(
			"RedReader-some_sub-some_user-title.jpg",
			General.filenameFromPost(
				"https://i.redd.it/x.jpg",
				"some_sub",
				"some_user",
				"Title"))
	}

	@Test
	fun missingComponentsAreSkipped() {
		assertEquals(
			"RedReader-aww.jpg",
			General.filenameFromPost(
				"https://i.redd.it/x.jpg",
				"aww",
				null,
				""))
	}

	@Test
	fun nothingUsableFallsBackToUrlName() {
		assertEquals(
			General.filenameFromString("https://i.redd.it/abc.jpg"),
			General.filenameFromPost(
				"https://i.redd.it/abc.jpg",
				null,
				null,
				""))
	}

	@Test
	fun longTitleIsTruncated() {
		val result = General.filenameFromPost(
			"https://i.redd.it/x.jpg",
			"aww",
			"user",
			"a".repeat(200))

		// RedReader-aww-user-<48 chars>.jpg
		assertEquals("RedReader-aww-user-" + "a".repeat(48) + ".jpg", result)
	}
}
