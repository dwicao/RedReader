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
import org.junit.Assert.assertNull
import org.junit.Test
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.kthings.RedditPost
import org.quantumbadger.redreader.reddit.kthings.RedditTimestampUTC
import org.quantumbadger.redreader.reddit.kthings.UrlEncodedString
import org.quantumbadger.redreader.reddit.prepared.FavouritePostStore

class FavouritePostStoreTest {

	private fun post(id: String, title: String) = RedditPost(
		id = id,
		name = RedditIdAndType("t3_$id"),
		title = UrlEncodedString(title),
		subreddit = UrlEncodedString("test"),
		num_comments = 3,
		score = 42,
		permalink = UrlEncodedString("/r/test/comments/$id"),
		created_utc = RedditTimestampUTC(TimestampUTC.ZERO)
	)

	@Test
	fun postRoundTrip() {
		val original = post("abc123", "Some title")
		val restored = FavouritePostStore.deserializePost(
			FavouritePostStore.serializePost(original))

		assertEquals(original.id, restored?.id)
		assertEquals(original.title, restored?.title)
		assertEquals(original.name, restored?.name)
		assertEquals(original.permalink, restored?.permalink)
	}

	@Test
	fun corruptPostReturnsNull() {
		assertNull(FavouritePostStore.deserializePost("{not json"))
	}

	@Test
	fun storeRoundTrip() {
		val entries = mapOf(
			"abc123" to FavouritePostStore.serializePost(post("abc123", "First")),
			"def456" to FavouritePostStore.serializePost(post("def456", "Second")))

		val restored = FavouritePostStore.deserializeStore(
			FavouritePostStore.serializeStore(entries))

		assertEquals(entries.keys, restored.keys)

		val first = FavouritePostStore.deserializePost(restored.getValue("abc123"))
		assertEquals("First", first?.title?.decoded)
	}

	@Test
	fun corruptStoreReturnsEmpty() {
		assertEquals(
			emptyMap<String, String>(),
			FavouritePostStore.deserializeStore("{not json"))
	}

	@Test
	fun scoreFormatsCompactly() {
		assertEquals("42", FavouritePostStore.formatScoreCompact(42))
		assertEquals("1.5K", FavouritePostStore.formatScoreCompact(1500))
		assertEquals("23.6K", FavouritePostStore.formatScoreCompact(23567))
		assertEquals("1.2M", FavouritePostStore.formatScoreCompact(1234567))
		assertEquals("1M", FavouritePostStore.formatScoreCompact(999999))
		assertEquals("-2.5K", FavouritePostStore.formatScoreCompact(-2500))
	}
}
