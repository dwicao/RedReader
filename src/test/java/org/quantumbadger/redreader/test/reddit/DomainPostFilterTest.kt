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
import org.quantumbadger.redreader.reddit.DomainPostFilter
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.kthings.RedditPost
import org.quantumbadger.redreader.reddit.kthings.RedditTimestampUTC
import org.quantumbadger.redreader.reddit.kthings.UrlEncodedString

class DomainPostFilterTest {

	@Test
	fun normalizeStripsSchemeAndPath() {
		assertEquals("twitter.com", DomainPostFilter.normalizeTerm("https://twitter.com"))
		assertEquals("twitter.com", DomainPostFilter.normalizeTerm("http://twitter.com/status/123"))
		assertEquals("twitter.com", DomainPostFilter.normalizeTerm("www.twitter.com"))
		assertEquals("twitter.com", DomainPostFilter.normalizeTerm("*.twitter.com"))
		assertEquals("twitter.com", DomainPostFilter.normalizeTerm("  Twitter.COM  "))
		assertEquals("twitter.com", DomainPostFilter.normalizeTerm("https://www.twitter.com/a?b=c"))
	}

	@Test
	fun parseSplitsAndNormalizes() {
		assertEquals(
			listOf("twitter.com", "x.com", "nitter.net"),
			DomainPostFilter.parse("Twitter.com\nx.com, nitter.net"))

		assertEquals(emptyList<String>(), DomainPostFilter.parse(""))
		assertEquals(emptyList<String>(), DomainPostFilter.parse(null))
	}

	@Test
	fun matchesExactDomain() {
		val post = post(domain = "twitter.com")

		assertTrue(DomainPostFilter.matches(listOf("twitter.com"), post))
		assertFalse(DomainPostFilter.matches(listOf("facebook.com"), post))
	}

	@Test
	fun matchesSubdomains() {
		val post = post(domain = "mobile.twitter.com")

		assertTrue(DomainPostFilter.matches(listOf("twitter.com"), post))
		assertFalse(DomainPostFilter.matches(listOf("twit.com"), post))
	}

	@Test
	fun matchesCaseInsensitively() {
		val post = post(domain = "TWITTER.com")

		assertTrue(DomainPostFilter.matches(listOf("twitter.com"), post))
	}

	@Test
	fun doesNotMatchSuffixOfLabel() {

		// "itt.com" must not match "twitter.com"
		val post = post(domain = "twitter.com")

		assertFalse(DomainPostFilter.matches(listOf("itt.com"), post))
	}

	@Test
	fun selfPostsAreNotFiltered() {
		val post = post(domain = "self.SomeSubreddit")

		assertFalse(DomainPostFilter.matches(listOf("twitter.com"), post))
	}

	@Test
	fun postsWithoutDomainAreIgnored() {
		val post = post(domain = null)

		assertFalse(DomainPostFilter.matches(listOf("twitter.com"), post))
	}

	@Test
	fun emptyTermListMatchesNothing() {
		assertFalse(DomainPostFilter.matches(emptyList(), post(domain = "twitter.com")))
	}

	private fun post(domain: String?) = RedditPost(
		id = "id",
		name = RedditIdAndType("t3_id"),
		subreddit = UrlEncodedString("test"),
		num_comments = 0,
		score = 0,
		permalink = UrlEncodedString("/r/test/comments/id"),
		created_utc = RedditTimestampUTC(TimestampUTC.ZERO),
		url = UrlEncodedString("https://${domain ?: "example.com"}/x"),
		title = UrlEncodedString("title"),
		domain = domain?.let(::UrlEncodedString))
}
