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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.reddit.KeywordPostFilter
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.kthings.RedditPost
import org.quantumbadger.redreader.reddit.kthings.RedditTimestampUTC
import org.quantumbadger.redreader.reddit.kthings.UrlEncodedString

class KeywordPostFilterTest {

	@Test
	fun parseSplitsOnCommasAndNewlines() {
		assertEquals(
			listOf("ai", "art", "news"),
			KeywordPostFilter.parse("AI, art\nnews"))

		assertEquals(
			listOf("a", "b"),
			KeywordPostFilter.parse("a,\n\nb,"))

		assertEquals(listOf("x"), KeywordPostFilter.parse("  x  "))

		assertEquals(emptyList<String>(), KeywordPostFilter.parse(""))
		assertEquals(emptyList<String>(), KeywordPostFilter.parse(null))
	}

	@Test
	fun parseIsCaseInsensitive() {
		assertEquals(listOf("ai"), KeywordPostFilter.parse("AI"))
	}

	@Test
	fun matchesTitleCaseInsensitively() {
		val post = post(title = "New AI breakthrough announced")

		assertTrue(KeywordPostFilter.matches(listOf("ai"), post))
		assertFalse(KeywordPostFilter.matches(listOf("quantum"), post))
	}

	@Test
	fun matchesSelfText() {
		val post = post(selftext = "I am totally not talking about cryptocurrency here")

		assertTrue(KeywordPostFilter.matches(listOf("crypto"), post))
		assertFalse(KeywordPostFilter.matches(listOf("nft"), post))
	}

	@Test
	fun matchesFlair() {
		val post = post(linkFlair = "Discussion")

		assertTrue(KeywordPostFilter.matches(listOf("discussion"), post))
		assertFalse(KeywordPostFilter.matches(listOf("oc"), post))
	}

	@Test
	fun missingFieldsAreIgnored() {
		val post = post()

		assertFalse(KeywordPostFilter.matches(listOf("anything"), post))
	}

	@Test
	fun emptyTermListMatchesNothing() {
		assertFalse(KeywordPostFilter.matches(emptyList(), post(title = "AI")))
	}

	private fun post(
		title: String? = null,
		selftext: String? = null,
		linkFlair: String? = null
	) = RedditPost(
		id = "id",
		name = RedditIdAndType("t3_id"),
		subreddit = UrlEncodedString("test"),
		num_comments = 0,
		score = 0,
		permalink = UrlEncodedString("/r/test/comments/id"),
		created_utc = RedditTimestampUTC(TimestampUTC.ZERO),
		url = UrlEncodedString("https://example.com"),
		title = title?.let(::UrlEncodedString),
		selftext = selftext?.let(::UrlEncodedString),
		link_flair_text = linkFlair?.let(::UrlEncodedString))
}
