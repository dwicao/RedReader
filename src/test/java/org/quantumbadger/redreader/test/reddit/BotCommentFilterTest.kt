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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.reddit.BotCommentFilter
import org.quantumbadger.redreader.reddit.kthings.RedditComment
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.kthings.RedditTimestampUTC
import org.quantumbadger.redreader.reddit.kthings.UrlEncodedString

class BotCommentFilterTest {

	@Test
	fun distinguishedModeratorCommentIsHidden() {
		assertTrue(BotCommentFilter.matches(comment(distinguished = "moderator")))
	}

	@Test
	fun distinguishedAdminCommentIsHidden() {
		assertTrue(BotCommentFilter.matches(comment(distinguished = "admin")))
	}

	@Test
	fun distinguishedIsCaseInsensitive() {
		assertTrue(BotCommentFilter.matches(comment(distinguished = "Moderator")))
	}

	@Test
	fun autoModeratorIsHidden() {
		assertTrue(BotCommentFilter.matches(comment(author = "AutoModerator")))
		assertTrue(BotCommentFilter.matches(comment(author = "automoderator")))
	}

	@Test
	fun stickiedCommentIsKeptEvenIfBot() {
		assertFalse(BotCommentFilter.matches(comment(author = "AutoModerator", stickied = true)))
		assertFalse(BotCommentFilter.matches(comment(distinguished = "moderator", stickied = true)))
	}

	@Test
	fun ordinaryCommentIsKept() {
		assertFalse(BotCommentFilter.matches(comment(author = "some_user")))
		assertFalse(BotCommentFilter.matches(comment(author = "AutoModerator2")))
	}

	@Test
	fun commentWithoutAuthorOrDistinctionIsKept() {
		assertFalse(BotCommentFilter.matches(comment(author = null, distinguished = null)))
	}

	private fun comment(
		author: String? = "some_user",
		distinguished: String? = null,
		stickied: Boolean = false
	) = RedditComment(
		id = "id",
		name = RedditIdAndType("t1_id"),
		created_utc = RedditTimestampUTC(TimestampUTC.ZERO),
		author = author?.let(::UrlEncodedString),
		distinguished = distinguished,
		stickied = stickied)
}
