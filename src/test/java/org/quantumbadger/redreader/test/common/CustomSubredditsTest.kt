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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.quantumbadger.redreader.common.CustomSubreddits
import org.quantumbadger.redreader.common.CustomSubreddits.Entry

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

	@Test
	fun jsonRoundTripPreservesMultipleEntriesAndEscapes() {
		val original = listOf(
			Entry("id-1", "Tech \"Main\" \\ Feed", "android+linux+programming"),
			Entry("id-2", "", "pics+funny"),
			Entry("id-3", "Line1\nLine2", "news+worldnews")
		)
		val json = CustomSubreddits.serializeEntries(original)
		val restored = CustomSubreddits.parseEntries(json)
		assertEquals(original, restored)
		assertEquals("Tech \"Main\" \\ Feed", restored[0].displayName())
		assertEquals("pics+funny", restored[1].displayName())
	}

	@Test
	fun corruptJsonReturnsEmptyList() {
		assertTrue(CustomSubreddits.parseEntries(null).isEmpty())
		assertTrue(CustomSubreddits.parseEntries("").isEmpty())
		assertTrue(CustomSubreddits.parseEntries("{bad json").isEmpty())
		assertTrue(CustomSubreddits.parseEntries("[{\"id\":\"1\"}]").isEmpty())
	}

	@Test
	fun legacyMigrationProducesSingleEntry() {
		val migrated = CustomSubreddits.fromLegacyRaw("Pics+FUNNY")
		assertEquals(1, migrated.size)
		assertEquals("pics+funny", migrated[0].subreddits)
		assertEquals("pics+funny", migrated[0].displayName())
		assertTrue(CustomSubreddits.fromLegacyRaw("").isEmpty())
	}

	@Test
	fun addAndRemoveSubredditInSpecificList() {
		val initial = listOf(
			Entry("1", "Tech", "android+linux"),
			Entry("2", "Fun", "pics+funny")
		)
		val added = CustomSubreddits.addSubredditInList(initial, "1", "/r/Kotlin")
		assertNotNull(added)
		assertEquals("android+linux+kotlin", added!![0].subreddits)
		assertEquals("pics+funny", added[1].subreddits)

		// Adding a duplicate returns null (no-op)
		assertNull(CustomSubreddits.addSubredditInList(added, "1", "r/LINUX"))

		val removed = CustomSubreddits.removeSubredditInList(added, "1", "linux")
		assertNotNull(removed)
		assertEquals("android+kotlin", removed!![0].subreddits)
		assertEquals("pics+funny", removed[1].subreddits)
	}

	@Test
	fun removingLastSubredditDeletesEmptyEntry() {
		val initial = listOf(
			Entry("1", "Solo", "pics"),
			Entry("2", "Other", "funny")
		)
		val updated = CustomSubreddits.removeSubredditInList(initial, "1", "pics")
		assertNotNull(updated)
		assertEquals(1, updated!!.size)
		assertEquals("2", updated[0].id)
	}

	@Test
	fun applyMembershipUpdatesMultipleListsAtomically() {
		val initial = listOf(
			Entry("1", "Tech", "android+linux"),
			Entry("2", "News", "worldnews+news"),
			Entry("3", "Solo", "kotlin")
		)
		assertTrue(initial[0].contains("r/Android"))
		assertFalse(initial[1].contains("kotlin"))

		val updated = CustomSubreddits.applyMembershipInList(
			initial,
			"kotlin",
			setOf("1", "2")
		)
		assertNotNull(updated)
		assertEquals(2, updated!!.size)
		assertEquals("android+linux+kotlin", updated[0].subreddits)
		assertEquals("worldnews+news+kotlin", updated[1].subreddits)
	}

	@Test
	fun updateAndDeleteEntryInList() {
		val initial = listOf(
			Entry("1", "Tech", "android+linux"),
			Entry("2", "Fun", "pics+funny")
		)
		val updated = CustomSubreddits.updateInList(initial, "1", " Dev ", "r/java+r/kotlin")
		assertNotNull(updated)
		assertEquals("Dev", updated!![0].name)
		assertEquals("java+kotlin", updated[0].subreddits)
		assertEquals(initial[1], updated[1])

		val deleted = CustomSubreddits.deleteInList(updated, "1")
		assertEquals(1, deleted.size)
		assertEquals("2", deleted[0].id)
	}
}
