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

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.quantumbadger.redreader.reddit.url.RedditURLParser
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RedditURLParserTest {

	@Test
	fun clickRedditmailRedirectDecodesTarget() {

		val uri = Uri.parse(
			"https://click.redditmail.com/track?url=https%3A%2F%2Fwww.reddit.com%2Fr%2Fredreader%2Fcomments%2F1abc234%2Ftitle%2F")

		val parsed = RedditURLParser.parse(uri)

		assertEquals(RedditURLParser.POST_COMMENT_LISTING_URL, parsed?.pathType())
	}

	@Test
	fun clickRedditmailRedirectWithShortParamNameDecodes() {

		val uri = Uri.parse(
			"https://click.redditmail.com/track?u=https%3A%2F%2Fwww.reddit.com%2Fr%2Faskreddit%2F")

		val parsed = RedditURLParser.parse(uri)

		assertEquals(RedditURLParser.SUBREDDIT_POST_LISTING_URL, parsed?.pathType())
	}

	@Test
	fun clickRedditmailRedirectWithRedirectParamDecodes() {

		val uri = Uri.parse(
			"https://click.redditmail.com/track?redirect=https%3A%2F%2Freddit.com%2Fcomments%2F1abc234")

		val parsed = RedditURLParser.parse(uri)

		assertEquals(RedditURLParser.POST_COMMENT_LISTING_URL, parsed?.pathType())
	}

	@Test
	fun clickRedditmailWithoutRedirectReturnsNull() {

		val uri = Uri.parse("https://click.redditmail.com/track")

		assertNull(RedditURLParser.parse(uri))
	}

	@Test
	fun clickRedditmailRedirectToNonHttpTargetIsRejected() {

		val uri = Uri.parse(
			"https://click.redditmail.com/track?url=javascript%3Aalert(1)")

		assertNull(RedditURLParser.parse(uri))
	}

	@Test
	fun redditmediaCommentsLinkIsAccepted() {

		val uri = Uri.parse(
			"https://www.redditmedia.com/r/shittyaskscience/comments/1fkt457/if_speed_bumps/")

		val parsed = RedditURLParser.parse(uri)

		assertEquals(RedditURLParser.POST_COMMENT_LISTING_URL, parsed?.pathType())
	}

	@Test
	fun redditmediaSubredditLinkIsAccepted() {

		val uri = Uri.parse("https://redditmedia.com/r/all/")

		val parsed = RedditURLParser.parse(uri)

		assertEquals(RedditURLParser.SUBREDDIT_POST_LISTING_URL, parsed?.pathType())
	}

	@Test
	fun redditmediaRootIsNotARecognisedPage() {

		val uri = Uri.parse("https://www.redditmedia.com/")

		// Not a post listing, comment listing or profile; must not crash or misroute
		RedditURLParser.parse(uri)
	}
}
