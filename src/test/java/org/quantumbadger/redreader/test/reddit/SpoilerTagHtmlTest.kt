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

import androidx.appcompat.app.AppCompatActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElement
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElementSpoilerButton
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElementTextSpanned
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElementVerticalSequence
import org.quantumbadger.redreader.reddit.prepared.html.HtmlReader
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpoilerTagHtmlTest {

	private val activity: AppCompatActivity =
		Robolectric.buildActivity(AppCompatActivity::class.java).get()

	private fun parse(body: String): List<BodyElement> {

		val parsed = HtmlReader.parse("<div class=\"md\">$body</div>", activity, null)

		assertTrue(
				"Expected parse to succeed, got $parsed",
				parsed is BodyElementVerticalSequence
		)

		@Suppress("UNCHECKED_CAST")
		return (parsed as BodyElementVerticalSequence).elements
	}

	private fun spannedText(element: BodyElement): String {

		return try {
			val field = BodyElementTextSpanned::class.java.getDeclaredField("mSpanned")
			field.isAccessible = true
			field.get(element)?.toString() ?: ""
		} catch(e: NoSuchFieldException) {
			throw AssertionError("mSpanned field not found", e)
		}
	}

	@Test
	fun spoilerWithSpacesIsHidden() {

		val elements = parse("<p>&gt;! One two three. !&lt;</p>")

		assertEquals(1, elements.size)
		assertTrue(elements[0] is BodyElementSpoilerButton)
	}

	@Test
	fun spoilerWithoutSpacesIsHidden() {

		val elements = parse("<p>&gt;!one two three. !&lt;</p>")

		assertEquals(1, elements.size)
		assertTrue(elements[0] is BodyElementSpoilerButton)
	}

	@Test
	fun inlineSpoilerInTextIsHidden() {

		val elements = parse("<p>public &gt;!secret!&lt; rest</p>")

		assertEquals(3, elements.size)
		assertTrue("element 0: ${elements[0]}", elements[0] is BodyElementTextSpanned)
		assertTrue("element 1: ${elements[1]}", elements[1] is BodyElementSpoilerButton)
		assertTrue("element 2: ${elements[2]}", elements[2] is BodyElementTextSpanned)

		assertEquals("public ", spannedText(elements[0]))
		assertEquals(" rest", spannedText(elements[2]))
	}

	@Test
	fun spoilerSpanningInlineFormattingIsHidden() {

		val elements = parse("<p>&gt;! hello <em>world</em> !&lt;</p>")

		assertEquals(1, elements.size)
		assertTrue(elements[0] is BodyElementSpoilerButton)
	}

	@Test
	fun unmatchedOpenTagIsLiteral() {

		val elements = parse("<p>&gt;! no closing marker</p>")

		assertTrue(elements.none { it is BodyElementSpoilerButton })
		assertEquals(">! no closing marker", spannedText(elements[0]))
	}

	@Test
	fun unmatchedCloseTagIsLiteral() {

		val elements = parse("<p>no opening marker !&lt;</p>")

		assertTrue(elements.none { it is BodyElementSpoilerButton })
		assertEquals("no opening marker !<", spannedText(elements[0]))
	}

	@Test
	fun twoSpoilersInOneParagraph() {

		val elements = parse("<p>a &gt;!one!&lt; b &gt;!two!&lt; c</p>")

		assertEquals(5, elements.size)
		assertTrue(elements[0] is BodyElementTextSpanned)
		assertTrue(elements[1] is BodyElementSpoilerButton)
		assertTrue(elements[2] is BodyElementTextSpanned)
		assertTrue(elements[3] is BodyElementSpoilerButton)
		assertTrue(elements[4] is BodyElementTextSpanned)

		assertEquals("a ", spannedText(elements[0]))
		assertEquals(" b ", spannedText(elements[2]))
		assertEquals(" c", spannedText(elements[4]))
	}
}
