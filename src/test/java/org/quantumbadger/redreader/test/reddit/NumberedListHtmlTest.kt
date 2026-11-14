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
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElementNumberedListElement
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElementVerticalSequence
import org.quantumbadger.redreader.reddit.prepared.html.HtmlReader
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NumberedListHtmlTest {

	private val activity: AppCompatActivity =
		Robolectric.buildActivity(AppCompatActivity::class.java).get()

	private fun numberedListIndexes(body: String): List<Int> {

		val parsed = HtmlReader.parse(
				"<div class=\"md\">$body\n</div>",
				activity,
				null
		)

		assertTrue(
				"Expected parse to succeed, got $parsed",
				parsed is BodyElementVerticalSequence
		)

		@Suppress("UNCHECKED_CAST")
		val children: List<BodyElement> =
			(parsed as BodyElementVerticalSequence).elements

		val indexes = mutableListOf<Int>()

		for(element in children) {

			if(element is BodyElementNumberedListElement) {
				try {
					val field = BodyElementNumberedListElement::class.java
						.getDeclaredField("mListIndex")
					field.isAccessible = true
					indexes.add(field.getInt(element))
				} catch(e: NoSuchFieldException) {
					throw AssertionError("mListIndex field not found", e)
				}
			}
		}

		return indexes
	}

	@Test
	fun startAttributeIsRespected() {
		assertEquals(
				listOf(5, 6, 7),
				numberedListIndexes("<ol start=\"5\"><li>a</li><li>b</li><li>c</li></ol>"))
	}

	@Test
	fun startDefaultsToOne() {
		assertEquals(
				listOf(1, 2),
				numberedListIndexes("<ol><li>a</li><li>b</li></ol>"))
	}

	@Test
	fun startBelowOneIsClamped() {
		assertEquals(
				listOf(1, 2),
				numberedListIndexes("<ol start=\"0\"><li>a</li><li>b</li></ol>"))
	}

	@Test
	fun startGarbageFallsBackToOne() {
		assertEquals(
				listOf(1, 2),
				numberedListIndexes("<ol start=\"lots\"><li>a</li><li>b</li></ol>"))
	}
}
