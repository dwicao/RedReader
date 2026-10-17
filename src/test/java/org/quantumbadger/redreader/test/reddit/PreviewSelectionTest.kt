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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.quantumbadger.redreader.reddit.kthings.UrlEncodedString
import org.quantumbadger.redreader.reddit.kthings.RedditPost
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost

/**
 * Tests for choosing which preview image to load.
 *
 * A grid card is a fraction of the screen width, but the image behind it may be
 * several thousand pixels across, so choosing the full resolution original where a
 * ladder rung would do makes cards load many times more slowly (and decode far
 * larger bitmaps) for no visible gain.
 */
class PreviewSelectionTest {

	private fun rung(url: String, width: Int, height: Int) =
		RedditPost.Preview.ImageDetails(UrlEncodedString(url), width, height)

	private fun select(
		source: RedditPost.Preview.ImageDetails?,
		rungs: List<RedditPost.Preview.ImageDetails>?,
		minWidth: Int,
		minHeight: Int = 0
	) = RedditParsedPost.selectPreviewDetails(source, rungs, minWidth, minHeight)

	@Test
	fun picksLargestRungMeetingTheRequestedWidth() {
		val picked = select(
			source = rung("orig", 2000, 1500),
			rungs = listOf(
				rung("640", 640, 480),
				rung("1080", 1080, 810),
				rung("256", 256, 192)
			),
			minWidth = 1080)

		assertEquals("1080", picked?.url?.value)
	}

	@Test
	fun doesNotFallBackToTheFullResolutionSourceWhenNoRungIsBigEnough() {
		// The case behind slow i.redd.it grid cards: the ladder tops out below what
		// was asked for, and the original is far larger than the card.
		val picked = select(
			source = rung("orig", 3000, 2000),
			rungs = listOf(
				rung("320", 320, 213),
				rung("640", 640, 427)
			),
			minWidth = 1080)

		assertNotNull("a rung should have been chosen", picked)
		assertEquals(
			"the full resolution original must not be used",
			"640",
			picked?.url?.value)
	}

	@Test
	fun usesTheSourceWhenThereIsNoResolutionLadder() {
		// Some previews carry only a source image; dropping it would leave the post
		// with nothing but its tiny thumbnail.
		val picked = select(
			source = rung("only", 800, 600),
			rungs = null,
			minWidth = 1080)

		assertEquals("only", picked?.url?.value)
	}

	@Test
	fun usesTheSourceWhenEveryRungIsUnusable() {
		// Undersized rungs are rejected outright, so the ladder offers nothing and
		// the source is the only remaining option.
		val picked = select(
			source = rung("orig", 900, 700),
			rungs = listOf(rung("tiny", 20, 20)),
			minWidth = 1080)

		assertEquals("orig", picked?.url?.value)
	}

	@Test
	fun prefersASmallerRungThatStillMeetsTheRequestedSize() {
		// Once a rung is large enough, the smallest such rung is preferred over a
		// bigger one.
		val picked = select(
			source = rung("orig", 2000, 1500),
			rungs = listOf(
				rung("1080", 1080, 810),
				rung("2048", 2048, 1536)
			),
			minWidth = 720)

		assertEquals("1080", picked?.url?.value)
	}

	@Test
	fun growsTheChoiceUntilItMeetsTheRequestedSize() {
		val picked = select(
			source = rung("orig", 2000, 1500),
			rungs = listOf(
				rung("320", 320, 240),
				rung("640", 640, 480),
				rung("1080", 1080, 810)
			),
			minWidth = 1000)

		assertEquals("1080", picked?.url?.value)
	}

	@Test
	fun rejectsRungsFarTallerThanTheSourceWouldBe() {
		// A rung whose real height would exceed the decode limit is unusable; this
		// is what previously left tall images with no preview at all.
		val picked = select(
			source = rung("orig", 400, 4000),
			rungs = listOf(rung("wide", 1200, 12000)),
			minWidth = 1080)

		assertEquals("4000px tall original is over the limit, so nothing is usable",
			null, picked)
	}

	@Test
	fun returnsNothingWhenThereIsNoSourceAndNoLadder() {
		assertNull(select(source = null, rungs = null, minWidth = 1080))
		assertNull(select(source = null, rungs = emptyList(), minWidth = 1080))
	}

	@Test
	fun selectionIsIndependentOfLadderOrder() {
		val ladder = listOf(
			rung("320", 320, 240),
			rung("1080", 1080, 810),
			rung("640", 640, 480)
		)

		assertEquals(
			select(rung("orig", 2000, 1500), ladder, 1080)?.url?.value,
			select(rung("orig", 2000, 1500), ladder.reversed(), 1080)?.url?.value)
	}
}
