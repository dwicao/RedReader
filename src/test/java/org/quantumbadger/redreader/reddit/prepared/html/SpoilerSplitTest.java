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

package org.quantumbadger.redreader.reddit.prepared.html;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpoilerSplitTest {

	private static String plainText(final HtmlRawElement element) {
		final StringBuilder sb = new StringBuilder();
		element.getPlainText(sb);
		return sb.toString();
	}

	private static ArrayList<HtmlRawElement> children(final String... texts) {
		final ArrayList<HtmlRawElement> result = new ArrayList<>();
		for(final String text : texts) {
			result.add(new HtmlRawElementPlainText(text));
		}
		return result;
	}

	@Test
	public void wholeParagraphSpoilerWithSpaces() {

		final ArrayList<HtmlRawElement> out =
				HtmlRawElement.splitSpoilerTags(children(">! One two three. !<"));

		assertEquals(1, out.size());
		assertTrue(out.get(0) instanceof HtmlRawElementSpoiler);
		assertEquals(" One two three. ", plainText(out.get(0)));
	}

	@Test
	public void inlineSpoilerWithSpaces() {

		final ArrayList<HtmlRawElement> out =
				HtmlRawElement.splitSpoilerTags(children("before >! secret !< after"));

		assertEquals(3, out.size());
		assertTrue(out.get(0) instanceof HtmlRawElementPlainText);
		assertTrue(out.get(1) instanceof HtmlRawElementSpoiler);
		assertTrue(out.get(2) instanceof HtmlRawElementPlainText);
		assertEquals("before ", plainText(out.get(0)));
		assertEquals(" secret ", plainText(out.get(1)));
		assertEquals(" after", plainText(out.get(2)));
	}

	@Test
	public void inlineSpoilerWithoutSpaces() {

		final ArrayList<HtmlRawElement> out =
				HtmlRawElement.splitSpoilerTags(children(">!secret!<"));

		assertEquals(1, out.size());
		assertTrue(out.get(0) instanceof HtmlRawElementSpoiler);
		assertEquals("secret", plainText(out.get(0)));
	}

	@Test
	public void twoSpoilersInOneParagraph() {

		final ArrayList<HtmlRawElement> out =
				HtmlRawElement.splitSpoilerTags(children("a >!one!< b >!two!< c"));

		assertEquals(5, out.size());
		assertTrue(out.get(0) instanceof HtmlRawElementPlainText);
		assertTrue(out.get(1) instanceof HtmlRawElementSpoiler);
		assertTrue(out.get(2) instanceof HtmlRawElementPlainText);
		assertTrue(out.get(3) instanceof HtmlRawElementSpoiler);
		assertTrue(out.get(4) instanceof HtmlRawElementPlainText);
		assertEquals("a ", plainText(out.get(0)));
		assertEquals("one", plainText(out.get(1)));
		assertEquals(" b ", plainText(out.get(2)));
		assertEquals("two", plainText(out.get(3)));
		assertEquals(" c", plainText(out.get(4)));
	}

	@Test
	public void spoilerSpanningFormattingElement() {

		// Simulate ">! hello <em>world</em> !<": plain text, styled element, plain text
		final ArrayList<HtmlRawElement> children = new ArrayList<>();
		children.add(new HtmlRawElementPlainText(">! hello "));
		children.add(new HtmlRawElementTagPassthrough(
				new ArrayList<>(java.util.Collections.singleton(
						(HtmlRawElement)new HtmlRawElementPlainText("world")))));
		children.add(new HtmlRawElementPlainText(" !<"));

		final ArrayList<HtmlRawElement> out = HtmlRawElement.splitSpoilerTags(children);

		assertEquals(1, out.size());
		assertTrue(out.get(0) instanceof HtmlRawElementSpoiler);
		assertEquals(" hello world ", plainText(out.get(0)));
	}

	@Test
	public void spoilerAcrossTextNodes() {

		final ArrayList<HtmlRawElement> out = HtmlRawElement.splitSpoilerTags(
				children(">! start", "middle", "end !<"));

		assertEquals(1, out.size());
		assertTrue(out.get(0) instanceof HtmlRawElementSpoiler);
		assertEquals(" startmiddleend ", plainText(out.get(0)));
	}

	@Test
	public void unmatchedOpenIsLiteral() {

		final ArrayList<HtmlRawElement> out =
				HtmlRawElement.splitSpoilerTags(children(">! no close"));

		assertEquals(1, out.size());
		assertTrue(out.get(0) instanceof HtmlRawElementPlainText);
		assertEquals(">! no close", plainText(out.get(0)));
	}

	@Test
	public void unmatchedCloseIsLiteral() {

		final ArrayList<HtmlRawElement> out =
				HtmlRawElement.splitSpoilerTags(children("no open !<"));

		assertEquals(1, out.size());
		assertTrue(out.get(0) instanceof HtmlRawElementPlainText);
		assertEquals("no open !<", plainText(out.get(0)));
	}

	@Test
	public void extraClosePairsWithFirstOpen() {

		final ArrayList<HtmlRawElement> out =
				HtmlRawElement.splitSpoilerTags(children("a >!one!< two !< b"));

		assertEquals(3, out.size());
		assertTrue(out.get(0) instanceof HtmlRawElementPlainText);
		assertTrue(out.get(1) instanceof HtmlRawElementSpoiler);
		assertTrue(out.get(2) instanceof HtmlRawElementPlainText);
		assertEquals("a ", plainText(out.get(0)));
		assertEquals("one", plainText(out.get(1)));
		assertEquals(" two !< b", plainText(out.get(2)));
	}

	@Test
	public void noMarkersLeavesChildrenUntouched() {

		final ArrayList<HtmlRawElement> in = children("plain ", "text");

		assertEquals(in, HtmlRawElement.splitSpoilerTags(in));
	}
}
