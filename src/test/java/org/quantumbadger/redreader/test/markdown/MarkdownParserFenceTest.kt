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

package org.quantumbadger.redreader.test.markdown

import org.junit.Assert.assertEquals
import org.junit.Test
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParser

// These tests only parse fenced code blocks, whose construction path doesn't
// touch android.text, so no Robolectric is needed
class MarkdownParserFenceTest {

	private fun paragraphs(text: String): Array<out Any> {

		val group = MarkdownParser.parse(text.toCharArray())
		val field = group.javaClass.getDeclaredField("paragraphs")
		field.setAccessible(true)
		@Suppress("UNCHECKED_CAST")
		return field.get(group) as Array<out Any>
	}

	private fun paragraphTypes(text: String): List<String> {

		return paragraphs(text).map { paragraph ->
			val typeField = paragraph!!.javaClass.getDeclaredField("type")
			typeField.setAccessible(true)
			typeField.get(paragraph).toString()
		}
	}

	private fun paragraphRawStrings(text: String): List<String> {

		return paragraphs(text).map { paragraph ->
			val rawField = paragraph.javaClass.getDeclaredField("raw")
			rawField.setAccessible(true)
			val raw = rawField.get(paragraph) ?: return@map ""

			val arrField = raw.javaClass.getDeclaredField("arr")
			arrField.setAccessible(true)
			val startField = raw.javaClass.getDeclaredField("start")
			startField.setAccessible(true)
			val lengthField = raw.javaClass.getDeclaredField("length")
			lengthField.setAccessible(true)

			val arr = arrField.get(raw) as CharArray
			val start = startField.getInt(raw)
			val length = lengthField.getInt(raw)

			String(arr, start, length)
		}
	}

	@Test
	fun fencedCodeBlockContentsBecomeCodeParagraphs() {

		val raw = "before\n```\nline one\nline two\n```\nafter"

		assertEquals(
			listOf("TEXT", "CODE", "CODE", "TEXT"),
			paragraphTypes(raw)
		)
	}

	@Test
	fun tildeFenceWorksToo() {

		val raw = "~~~\nsome code\n~~~"

		assertEquals(listOf("CODE"), paragraphTypes(raw))
	}

	@Test
	fun unclosedFenceConsumesRest() {

		val raw = "```\nline one\nline two"

		assertEquals(listOf("CODE", "CODE"), paragraphTypes(raw))
	}

	@Test
	fun fenceContentsPreserveLeadingSpaces() {

		val raw = "```\n    indented\n```"

		val codeLine = paragraphRawStrings(raw)[0]

		assertEquals("    indented", codeLine)
	}

	@Test
	fun doubleSpacesInsideFenceDoNotMergeLines() {

		// With regular (TEXT) paragraphs, lines with fewer than two spaces at
		// the end get rejoined with a space. Fenced code lines must never merge.
		val raw = "```\nalpha\nbeta\ngamma\n```"

		assertEquals(listOf("CODE", "CODE", "CODE"), paragraphTypes(raw))

		assertEquals(
			listOf("alpha", "beta", "gamma"),
			paragraphRawStrings(raw)
		)
	}
}
