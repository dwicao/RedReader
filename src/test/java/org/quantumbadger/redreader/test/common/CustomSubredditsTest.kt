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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.quantumbadger.redreader.common.CustomSubreddits

class CustomSubredditsTest {

	@Test
	fun plainListIsKept() {
		assertEquals(
			"pics+funny+askreddit",
			CustomSubreddits.normalize("pics+funny+askreddit"))
	}

	@Test
	fun namesAreLowercased() {
		assertEquals("pics+funny", CustomSubreddits.normalize("Pics+FUNNY"))
	}

	@Test
	fun duplicatesAreRemoved() {
		assertEquals("pics+funny", CustomSubreddits.normalize("pics+funny+Pics"))
	}

	@Test
	fun slashRPrefixIsStripped() {
		assertEquals("pics+funny", CustomSubreddits.normalize("/r/pics+funny"))
		assertEquals("pics+funny", CustomSubreddits.normalize("r/pics+r/funny"))
	}

	@Test
	fun fullUrlsAreAccepted() {
		assertEquals(
			"pics+funny",
			CustomSubreddits.normalize("https://www.reddit.com/r/pics+funny/hot/"))
	}

	@Test
	fun commasAndWhitespaceAreSeparators() {
		assertEquals(
			"pics+funny+askreddit",
			CustomSubreddits.normalize("pics, funny\naskreddit"))
	}

	@Test
	fun singleNameIsAccepted() {
		assertEquals("pics", CustomSubreddits.normalize("pics"))
	}

	@Test
	fun invalidNameRejectsAll() {
		assertNull(CustomSubreddits.normalize("pics+fun-ny"))
		assertNull(CustomSubreddits.normalize("pics+with!bang"))
	}

	@Test
	fun emptyInputIsNull() {
		assertNull(CustomSubreddits.normalize(null))
		assertNull(CustomSubreddits.normalize(""))
		assertNull(CustomSubreddits.normalize("   "))
	}
}
