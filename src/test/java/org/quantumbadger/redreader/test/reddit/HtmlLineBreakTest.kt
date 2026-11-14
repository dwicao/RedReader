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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.quantumbadger.redreader.reddit.prepared.html.HtmlReader
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HtmlLineBreakTest {

	private fun textFrom(html: String): String {

		val reader = HtmlReader(html)
		val text = StringBuilder()

		while(true) {
			val token = reader.readNext()

			if(token.type == HtmlReader.TokenType.EOF) {
				break
			}

			if(token.type == HtmlReader.TokenType.TEXT) {
				text.append(token.text)
			}
		}

		return text.toString()
	}

	@Test
	fun singleLineBreakBecomesSpace() {
		assertEquals(
			"consultant at the Computer Center",
			textFrom("<p>consultant at the\nComputer Center</p>"))
	}

	@Test
	fun singleLineBreakCollapsesWithAdjacentSpaces() {
		assertEquals(
			"one two",
			textFrom("<p>one  \n   two</p>"))
	}

	@Test
	fun carriageReturnedLineEndingsBecomeSpace() {
		assertEquals(
			"one two",
			textFrom("<p>one\r\ntwo</p>"))
	}

	@Test
	fun multipleLineBreaksCollapseToSingleSpace() {
		assertEquals(
			"one two",
			textFrom("<p>one\n\n\ntwo</p>"))
	}

	@Test
	fun preformattedTextKeepsLineBreaks() {

		val text = textFrom("<pre>line one\nline two</pre>")

		assertTrue(
				"Expected newline preserved inside <pre>, got '$text'",
				text.contains("one\nline"))
	}
}
