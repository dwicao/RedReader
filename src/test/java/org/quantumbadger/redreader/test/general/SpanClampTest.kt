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

import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.quantumbadger.redreader.common.BetterSSB
import org.quantumbadger.redreader.reddit.prepared.bodytext.BlockType
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElementTextSpanned
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SpanClampTest {

	@Test
	fun addSpanDynamicWithReversedIndicesDoesNotCrash() {

		val ssb = SpannableStringBuilder("hello world")
		val element = BodyElementTextSpanned(BlockType.NORMAL_TEXT, ssb)
		val marker = Any()

		// Async image loads can deliver indices computed against a different buffer length
		element.addSpanDynamic(marker, 10, 3, 0)
		shadowOf(Looper.getMainLooper()).idle()

		val spans = ssb.getSpans(0, ssb.length, Any::class.java)

		assertTrue("expected the span to be attached", spans.contains(marker))

		val start = ssb.getSpanStart(marker)
		val end = ssb.getSpanEnd(marker)

		assertEquals(3, start)
		assertEquals(10, end)
	}

	@Test
	fun addSpanDynamicWithOutOfRangeIndicesIsClamped() {

		val ssb = SpannableStringBuilder("hello")
		val element = BodyElementTextSpanned(BlockType.NORMAL_TEXT, ssb)
		val marker = Any()

		element.addSpanDynamic(marker, 2, 100, 0)
		shadowOf(Looper.getMainLooper()).idle()

		val start = ssb.getSpanStart(marker)
		val end = ssb.getSpanEnd(marker)

		assertEquals(2, start)
		assertEquals(5, end)
	}

	@Test
	fun betterSSBSetSpanWithReversedIndicesDoesNotCrash() {

		val sb = BetterSSB()
		sb.append("hello world", 0)

		val marker = ForegroundColorSpan(0)

		sb.setSpan(marker, 9, 2, 0)

		assertEquals(2, sb.get().getSpanStart(marker))
		assertEquals(9, sb.get().getSpanEnd(marker))
	}

	@Test
	fun betterSSBReplaceWithReversedIndicesDoesNotCrash() {

		val sb = BetterSSB()
		sb.append("hello world", 0)

		sb.replace(8, 2, "LOLOLOL")

		assertEquals("heLOLOLOLrld", sb.get().toString())
	}

	@Test
	fun betterSSBReplaceWithOutOfRangeIndicesIsClamped() {

		val sb = BetterSSB()
		sb.append("hello", 0)

		sb.replace(2, 100, "LL")

		assertEquals("heLL", sb.get().toString())
	}

	@Test
	fun betterSSBReplaceMissingPlaceholderIsIgnored() {

		val sb = BetterSSB()
		sb.append("hello world", 0)

		sb.replace("not present", "span")

		assertEquals("hello world", sb.get().toString())
	}

	@Test
	fun betterSSBReplaceEmptyPlaceholderDoesNotCrash() {

		val sb = BetterSSB()
		sb.append("hello", 0)

		// Empty needles resolve to index 0; this must not crash either
		sb.replace("", "span")

		assertEquals("hello", sb.get().toString())
	}
}
