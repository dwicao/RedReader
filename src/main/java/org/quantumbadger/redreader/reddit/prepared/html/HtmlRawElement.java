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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.quantumbadger.redreader.common.StringUtils;
import org.quantumbadger.redreader.common.UriString;
import org.quantumbadger.redreader.reddit.prepared.InlineMedia;
import org.quantumbadger.redreader.reddit.prepared.bodytext.BlockType;
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElement;

import java.util.ArrayList;
import java.util.Objects;

public abstract class HtmlRawElement {

	// TODO potential improvements:
	//		- Profile performance
	//		- Test left/right swiping interaction with table scrollview

	public static class LinkButtonDetails {

		@Nullable public final String name;
		@NonNull public final UriString url;


		public LinkButtonDetails(
				@Nullable final String name,
				@NonNull final UriString url) {
			this.name = name;
			this.url = url;
		}

		@NonNull
		public final String getButtonTitle() {

			if(name == null || name.isEmpty()) {
				return url.value;
			} else {
				return name;
			}
		}

		@Nullable
		public final String getButtonSubtitle() {

			if(name == null || name.isEmpty()) {
				return null;
			} else {
				return url.value;
			}
		}
	}

	@NonNull
	public final String getPlainText() {
		final StringBuilder sb = new StringBuilder();
		getPlainText(sb);
		return sb.toString();
	}

	public abstract void getPlainText(@NonNull final StringBuilder stringBuilder);


	public abstract void reduce(
			@NonNull HtmlTextAttributes activeAttributes,
			@NonNull AppCompatActivity activity,
			@NonNull ArrayList<HtmlRawElement> destination,
			@NonNull ArrayList<LinkButtonDetails> linkButtons);

	public abstract void generate(
			@NonNull final AppCompatActivity activity,
			@NonNull ArrayList<BodyElement> destination);

	@NonNull
	public static HtmlRawElement readFrom(@NonNull final HtmlReaderPeekable reader)
			throws MalformedHtmlException {

		final HtmlReader.Token startToken = reader.peek();
		reader.advance();

		final ArrayList<HtmlRawElement> children = new ArrayList<>();

		if(startToken.type == HtmlReader.TokenType.TAG_START_AND_END) {

			switch(startToken.text) {
				case "hr":
					return new HtmlRawElementTagHorizontalRule();

				case "br":
					return new HtmlRawElementBreak();

				case "img":
					return new HtmlRawElementImg(children,
							startToken.title == null || startToken.title.isEmpty()
									? "emote" : startToken.title,
							startToken.src);

				default:
					return HtmlRawElementInlineErrorMessage.create(
							"Error: Unexpected tag <" + startToken.text + "/>");
			}

		} else if(startToken.type == HtmlReader.TokenType.TAG_START) {

			while(reader.peek().type != HtmlReader.TokenType.TAG_END
					&& reader.peek().type != HtmlReader.TokenType.EOF) {

				children.add(HtmlRawElement.readFrom(reader));
			}

			{
				final HtmlReader.Token endToken = reader.peek();

				// Reddit sometimes doesn't close tags properly :'(
				if(endToken.text.equalsIgnoreCase(startToken.text)) {
					reader.advance();
				}
			}

			final HtmlRawElement result;

			switch(StringUtils.asciiLowercase(startToken.text)) {
				case "code":
					result = new HtmlRawElementTagCode(children);
					break;
				case "del":
					result = new HtmlRawElementTagDel(children);
					break;
				case "em":
					result = new HtmlRawElementTagEmphasis(children);
					break;
				case "div":
					result = new HtmlRawElementBlock(
							BlockType.VERTICAL_SEQUENCE,
							children);
					break;
				case "h1":
					result = new HtmlRawElementBlock(
							BlockType.HEADER,
							new HtmlRawElementTagH1(children));
					break;
				case "h2":
					result = new HtmlRawElementBlock(
							BlockType.HEADER,
							new HtmlRawElementTagH2(children));
					break;
				case "h3":
					result = new HtmlRawElementBlock(
							BlockType.HEADER,
							new HtmlRawElementTagH3(children));
					break;
				case "h4":
					result = new HtmlRawElementBlock(
							BlockType.HEADER,
							new HtmlRawElementTagH4(children));
					break;
				case "h5":
					result = new HtmlRawElementBlock(
							BlockType.HEADER,
							new HtmlRawElementTagH5(children));
					break;
				case "h6":
					result = new HtmlRawElementBlock(
							BlockType.HEADER,
							new HtmlRawElementTagH6(children));
					break;
				case "strong":
					result = new HtmlRawElementTagStrong(children);
					break;
				case "p":
					result = new HtmlRawElementBlock(BlockType.NORMAL_TEXT,
							splitSpoilerTags(children));
					break;
				case "th":
				case "td":
					result = new HtmlRawElementTableCell(new HtmlRawElementBlock(
							BlockType.TABLE_CELL,
							children));
					break;
				case "sup":
					result = new HtmlRawElementTagSuperscript(children);
					break;
				case "a": {

					final String href = Objects.requireNonNull(startToken.href);

					if(href.startsWith("/spoiler")) {
						// Old spoiler syntax
						result = new HtmlRawElementSpoiler(new HtmlRawElementBlock(
								BlockType.BUTTON,
								children));

					} else if(href.length() == 2
							&& (href.charAt(0) == '#' || href.charAt(0) == '/')
							&& startToken.title != null) {

						// Another old spoiler syntax

						children.add(new HtmlRawElementSpoiler(
								new HtmlRawElementBlock(
										BlockType.NORMAL_TEXT,
										new HtmlRawElementPlainText(startToken.title))));

						result = new HtmlRawElementTagPassthrough(children);

					} else if(href.startsWith("#")) {
						// Probably an emote: pass through the text, but don't make a link
						result = new HtmlRawElementTagPassthrough(children);

					} else {
						final InlineMediaCollector inlineMedia = reader.getInlineMedia();

						final InlineMedia media = inlineMedia == null
								? null
								: inlineMedia.resolve(href);

						if(media != null) {
							result = new HtmlRawElementInlineMedia(
									children,
									new UriString(href),
									media,
									inlineMedia);
						} else {
							result = new HtmlRawElementTagAnchor(children, new UriString(href));
						}
					}
					break;
				}
				case "pre":
					result = new HtmlRawElementBlock(
							BlockType.CODE_BLOCK,
							new HtmlRawElementTagCode(children));
					break;
				case "ul":
					result = new HtmlRawElementBulletList(children);
					break;
				case "ol":
					int startIndex = 1;
					if(startToken.start != null) {
						try {
							startIndex = Math.max(1, Integer.parseInt(startToken.start.trim()));
						} catch(final NumberFormatException e) {
							startIndex = 1;
						}
					}
					result = new HtmlRawElementNumberedList(children, startIndex);
					break;
				case "li":
					result = new HtmlRawElementBlock(BlockType.LIST_ELEMENT,
							splitSpoilerTags(children));
					break;
				case "blockquote":
					result = new HtmlRawElementQuote(new HtmlRawElementBlock(
							BlockType.QUOTE,
							children));
					break;
				case "span":

					if("md-spoiler-text".equalsIgnoreCase(startToken.cssClass)) {
						result = new HtmlRawElementSpoiler(new HtmlRawElementBlock(
								BlockType.BUTTON,
								children));

					} else {
						result = new HtmlRawElementTagPassthrough(children);
					}
					break;

				case "thead":
					result = new HtmlRawElementTagStrong(children);
					break;

				case "tbody":
					result = new HtmlRawElementTagPassthrough(children);
					break;

				case "table":
					result = new HtmlRawElementTable(children);
					break;

				case "tr":
					result = new HtmlRawElementTableRow(children);
					break;

				case "emote":
					final UriString src = Objects.requireNonNull(startToken.src);
					result = new HtmlRawElementImg(children,
							startToken.title == null || startToken.title.isEmpty()
									? "emote" : startToken.title,
							src);
					break;

				default:
					return HtmlRawElementInlineErrorMessage.appendError(
							"Error: Unexpected tag start <" + startToken.text + ">",
							new HtmlRawElementBlock(BlockType.NORMAL_TEXT, children));
			}

			return result;

		} else if(startToken.type == HtmlReader.TokenType.TEXT) {
			return new HtmlRawElementPlainText(startToken.text);

		} else if(startToken.type == HtmlReader.TokenType.EOF) {
			throw new MalformedHtmlException(
					"Unexpected EOF",
					reader.getHtml(),
					reader.getPos());

		} else {
			return HtmlRawElementInlineErrorMessage.create(
					"Error: Unexpected token type " + startToken.type);
		}
	}

	/**
	 * Reddit's server does not recognise spoiler tags which contain spaces
	 * (e.g. {@code >! One two three. !<}), leaving them as literal text.
	 * Split the children on {@code >!}...{@code !<} markers and wrap each run
	 * in a spoiler element, mirroring how Reddit renders md-spoiler-text.
	 * Unmatched markers are left as literal text.
	 */
	@NonNull
	static ArrayList<HtmlRawElement> splitSpoilerTags(
			@NonNull final ArrayList<HtmlRawElement> children) {

		// ---------- Pass 1: collect marker events in order of appearance ----------

		final ArrayList<Integer> evChild = new ArrayList<>();
		final ArrayList<Integer> evOffset = new ArrayList<>();
		final ArrayList<Boolean> evIsOpen = new ArrayList<>();

		for(int i = 0; i < children.size(); i++) {
			final HtmlRawElement child = children.get(i);
			if(!(child instanceof HtmlRawElementPlainText)) {
				continue;
			}

			final String text = ((HtmlRawElementPlainText)child).getText();
			int pos = 0;

			while(pos < text.length()) {
				final int open = text.indexOf(">!", pos);
				final int close = text.indexOf("!<", pos);

				if(open < 0 && close < 0) {
					break;
				}

				if(open >= 0 && (close < 0 || open < close)) {
					evChild.add(i);
					evOffset.add(open);
					evIsOpen.add(true);
					pos = open + 2;

				} else {
					evChild.add(i);
					evOffset.add(close);
					evIsOpen.add(false);
					pos = close + 2;
				}
			}
		}

		// ---------- Pass 1b: pair markers sequentially ----------

		final boolean[] evMatched = new boolean[evChild.size()];
		int pending = -1;

		for(int e = 0; e < evChild.size(); e++) {
			if(evIsOpen.get(e)) {
				if(pending == -1) {
					pending = e;
				}

			} else if(pending != -1) {
				evMatched[pending] = true;
				evMatched[e] = true;
				pending = -1;
			}
		}

		boolean anyMatched = false;
		for(final boolean matched : evMatched) {
			if(matched) {
				anyMatched = true;
				break;
			}
		}

		if(!anyMatched) {
			return children;
		}

		// ---------- Pass 2: rebuild the child list ----------

		final ArrayList<HtmlRawElement> output = new ArrayList<>(children.size() + 4);

		ArrayList<HtmlRawElement> normalSegment = new ArrayList<>();
		ArrayList<HtmlRawElement> spoilerSegment = null;

		for(int i = 0; i < children.size(); i++) {
			final HtmlRawElement child = children.get(i);

			if(!(child instanceof HtmlRawElementPlainText)) {

				if(spoilerSegment != null) {
					spoilerSegment.add(child);
				} else {
					normalSegment.add(child);
				}

			} else {

				final String text = ((HtmlRawElementPlainText)child).getText();
				int pos = 0;

				for(int e = 0; e < evChild.size(); e++) {
					if(evChild.get(e) != i || !evMatched[e]) {
						continue;
					}

					final int markerOffset = evOffset.get(e);
					final boolean isOpen = evIsOpen.get(e);

					if(markerOffset > pos) {
						final HtmlRawElementPlainText before =
								new HtmlRawElementPlainText(text.substring(pos, markerOffset));

						if(spoilerSegment != null) {
							spoilerSegment.add(before);
						} else {
							normalSegment.add(before);
						}
					}

					pos = markerOffset + 2;

					if(isOpen) {
						emitSpoilerSplitSegment(output, normalSegment, false);
						normalSegment = new ArrayList<>();
						spoilerSegment = new ArrayList<>();

					} else {
						emitSpoilerSplitSegment(output, spoilerSegment, true);
						spoilerSegment = null;
						normalSegment = new ArrayList<>();
					}
				}

				if(pos < text.length()) {
					final HtmlRawElementPlainText rest =
							new HtmlRawElementPlainText(text.substring(pos));

					if(spoilerSegment != null) {
						spoilerSegment.add(rest);
					} else {
						normalSegment.add(rest);
					}
				}
			}
		}

		// A matched close must always follow a matched open, so spoilerSegment is always
		// null here, but emit literally rather than dropping anything, just in case
		emitSpoilerSplitSegment(output, spoilerSegment, false);
		emitSpoilerSplitSegment(output, normalSegment, false);

		return output;
	}

	private static void emitSpoilerSplitSegment(
			@NonNull final ArrayList<HtmlRawElement> output,
			@Nullable final ArrayList<HtmlRawElement> segment,
			final boolean asSpoiler) {

		if(segment == null || segment.isEmpty()) {
			return;
		}

		if(!asSpoiler) {
			output.addAll(segment);
			return;
		}

		output.add(new HtmlRawElementSpoiler(new HtmlRawElementBlock(
				BlockType.BUTTON,
				segment)));
	}
}
