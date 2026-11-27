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

package org.quantumbadger.redreader.common;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

// User-defined subreddit combinations (e.g. "pics+funny+askreddit"), shown as
// named entries in the main menu. Supports multiple lists backed by a JSON
// array in SharedPreferences, with transparent fallback from the legacy
// single-list preference key.
public final class CustomSubreddits {

	public static final String PREF_KEY = "custom_subscribed_subreddits_v2";
	private static final String PREF_KEY_LEGACY = "custom_subscribed_subreddits";
	private static final String LEGACY_ID = "legacy";

	private static final Pattern VALID_NAME = Pattern.compile("[\\w.]+");

	public static final class Entry {
		@NonNull public final String id;
		@NonNull public final String name;
		@NonNull public final String subreddits;

		public Entry(
				@NonNull final String id,
				@NonNull final String name,
				@NonNull final String subreddits) {
			this.id = id;
			this.name = name;
			this.subreddits = subreddits;
		}

		@NonNull
		public String displayName() {
			return name.isEmpty() ? subreddits : name;
		}

		@NonNull
		public List<String> members() {
			if(subreddits.isEmpty()) {
				return Collections.emptyList();
			}
			return Arrays.asList(subreddits.split("\\+", -1));
		}

		public boolean contains(@Nullable final String subreddit) {
			final String normalized = normalizeSingle(subreddit);
			if(normalized == null) {
				return false;
			}
			return members().contains(normalized);
		}

		@Override
		public boolean equals(@Nullable final Object obj) {
			if(this == obj) {
				return true;
			}
			if(!(obj instanceof Entry)) {
				return false;
			}
			final Entry other = (Entry)obj;
			return id.equals(other.id)
					&& name.equals(other.name)
					&& subreddits.equals(other.subreddits);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, name, subreddits);
		}
	}

	private CustomSubreddits() {
	}

	@NonNull
	public static List<Entry> getAll(@NonNull final Context context) {
		final SharedPrefsWrapper prefs = General.getSharedPrefs(context);
		if(prefs.contains(PREF_KEY)) {
			return parseEntries(prefs.getString(PREF_KEY, ""));
		}
		return fromLegacyRaw(prefs.getString(PREF_KEY_LEGACY, ""));
	}

	@Nullable
	public static Entry getById(
			@NonNull final Context context,
			@NonNull final String id) {
		for(final Entry entry : getAll(context)) {
			if(entry.id.equals(id)) {
				return entry;
			}
		}
		return null;
	}

	public static void setAll(
			@NonNull final Context context,
			@NonNull final List<Entry> entries) {
		General.getSharedPrefs(context)
				.edit()
				.putString(PREF_KEY, serializeEntries(entries))
				.apply();
	}

	@Nullable
	public static Entry create(
			@NonNull final Context context,
			@Nullable final String name,
			@Nullable final String rawSubreddits) {
		final String normalized = normalize(rawSubreddits);
		if(normalized == null) {
			return null;
		}
		final String cleanName = cleanEntryName(name);
		final Entry entry = new Entry(
				UUID.randomUUID().toString(),
				cleanName,
				normalized);
		final List<Entry> updated = new ArrayList<>(getAll(context));
		updated.add(entry);
		setAll(context, updated);
		return entry;
	}

	public static boolean update(
			@NonNull final Context context,
			@NonNull final String id,
			@Nullable final String name,
			@Nullable final String rawSubreddits) {
		final List<Entry> existing = getAll(context);
		final List<Entry> updated = updateInList(existing, id, name, rawSubreddits);
		if(updated == null) {
			return false;
		}
		setAll(context, updated);
		return true;
	}

	public static boolean delete(
			@NonNull final Context context,
			@NonNull final String id) {
		final List<Entry> existing = getAll(context);
		final List<Entry> updated = deleteInList(existing, id);
		if(updated.size() == existing.size()) {
			return false;
		}
		setAll(context, updated);
		return true;
	}

	public static boolean addSubreddit(
			@NonNull final Context context,
			@NonNull final String id,
			@NonNull final String subreddit) {
		final List<Entry> existing = getAll(context);
		final List<Entry> updated = addSubredditInList(existing, id, subreddit);
		if(updated == null) {
			return false;
		}
		setAll(context, updated);
		return true;
	}

	public static boolean removeSubreddit(
			@NonNull final Context context,
			@NonNull final String id,
			@NonNull final String subreddit) {
		final List<Entry> existing = getAll(context);
		final List<Entry> updated = removeSubredditInList(existing, id, subreddit);
		if(updated == null) {
			return false;
		}
		setAll(context, updated);
		return true;
	}

	public static int countMembership(
			@NonNull final Context context,
			@NonNull final String subreddit) {
		int count = 0;
		for(final Entry entry : getAll(context)) {
			if(entry.contains(subreddit)) {
				count++;
			}
		}
		return count;
	}

	public static void setMembership(
			@NonNull final Context context,
			@NonNull final String subreddit,
			@NonNull final Set<String> targetListIds) {
		final List<Entry> updated = applyMembershipInList(
				getAll(context),
				subreddit,
				targetListIds);
		if(updated != null) {
			setAll(context, updated);
		}
	}

	@NonNull
	public static List<Entry> fromLegacyRaw(@Nullable final String legacyRaw) {
		final String normalized = normalize(legacyRaw);
		if(normalized == null) {
			return Collections.emptyList();
		}
		return Collections.singletonList(new Entry(LEGACY_ID, "", normalized));
	}

	@Nullable
	public static List<Entry> updateInList(
			@NonNull final List<Entry> entries,
			@NonNull final String id,
			@Nullable final String name,
			@Nullable final String rawSubreddits) {
		final String normalized = normalize(rawSubreddits);
		if(normalized == null) {
			return null;
		}
		final String cleanName = cleanEntryName(name);
		final List<Entry> updated = new ArrayList<>(entries.size());
		boolean found = false;
		for(final Entry entry : entries) {
			if(entry.id.equals(id)) {
				updated.add(new Entry(entry.id, cleanName, normalized));
				found = true;
			} else {
				updated.add(entry);
			}
		}
		return found ? updated : null;
	}

	@NonNull
	public static List<Entry> deleteInList(
			@NonNull final List<Entry> entries,
			@NonNull final String id) {
		final List<Entry> updated = new ArrayList<>(entries.size());
		for(final Entry entry : entries) {
			if(!entry.id.equals(id)) {
				updated.add(entry);
			}
		}
		return updated;
	}

	@Nullable
	public static List<Entry> addSubredditInList(
			@NonNull final List<Entry> entries,
			@NonNull final String id,
			@Nullable final String subreddit) {
		final String single = normalizeSingle(subreddit);
		if(single == null) {
			return null;
		}
		final List<Entry> updated = new ArrayList<>(entries.size());
		boolean changed = false;
		for(final Entry entry : entries) {
			if(entry.id.equals(id) && !entry.contains(single)) {
				final String combined = normalize(entry.subreddits + "+" + single);
				if(combined == null) {
					return null;
				}
				updated.add(new Entry(entry.id, entry.name, combined));
				changed = true;
			} else {
				updated.add(entry);
			}
		}
		return changed ? updated : null;
	}

	@Nullable
	public static List<Entry> removeSubredditInList(
			@NonNull final List<Entry> entries,
			@NonNull final String id,
			@Nullable final String subreddit) {
		final String single = normalizeSingle(subreddit);
		if(single == null) {
			return null;
		}
		final List<Entry> updated = new ArrayList<>(entries.size());
		boolean changed = false;
		for(final Entry entry : entries) {
			if(entry.id.equals(id) && entry.contains(single)) {
				changed = true;
				final String remaining = removeMember(entry.subreddits, single);
				if(!remaining.isEmpty()) {
					updated.add(new Entry(entry.id, entry.name, remaining));
				}
			} else {
				updated.add(entry);
			}
		}
		return changed ? updated : null;
	}

	@Nullable
	public static List<Entry> applyMembershipInList(
			@NonNull final List<Entry> entries,
			@Nullable final String subreddit,
			@NonNull final Set<String> targetListIds) {
		final String single = normalizeSingle(subreddit);
		if(single == null) {
			return null;
		}
		final List<Entry> updated = new ArrayList<>(entries.size());
		boolean changed = false;
		for(final Entry entry : entries) {
			final boolean isMember = entry.contains(single);
			final boolean shouldBeMember = targetListIds.contains(entry.id);
			if(shouldBeMember && !isMember) {
				final String combined = normalize(entry.subreddits + "+" + single);
				updated.add(new Entry(
						entry.id,
						entry.name,
						combined == null ? entry.subreddits : combined));
				changed = true;
			} else if(!shouldBeMember && isMember) {
				changed = true;
				final String remaining = removeMember(entry.subreddits, single);
				if(!remaining.isEmpty()) {
					updated.add(new Entry(entry.id, entry.name, remaining));
				}
			} else {
				updated.add(entry);
			}
		}
		return changed ? updated : null;
	}

	@NonNull
	private static String removeMember(
			@NonNull final String subreddits,
			@NonNull final String target) {
		final StringBuilder builder = new StringBuilder();
		for(final String member : subreddits.split("\\+", -1)) {
			if(!member.isEmpty() && !member.equals(target)) {
				if(builder.length() > 0) {
					builder.append('+');
				}
				builder.append(member);
			}
		}
		return builder.toString();
	}

	@NonNull
	private static String cleanEntryName(@Nullable final String name) {
		return name == null ? "" : name.trim();
	}

	@Nullable
	public static String normalizeSingle(@Nullable final String raw) {
		if(raw == null) {
			return null;
		}
		String text = raw.trim();
		if(text.regionMatches(true, 0, "/r/", 0, 3)) {
			text = text.substring(3);
		} else if(text.regionMatches(true, 0, "r/", 0, 2)) {
			text = text.substring(2);
		}
		if(text.isEmpty() || !VALID_NAME.matcher(text).matches()) {
			return null;
		}
		return StringUtils.asciiLowercase(text);
	}

	// Parses "a+b+c", "/r/a+b+c", full URLs and comma/space/newline separated
	// lists into canonical "a+b+c" form. Returns null when nothing valid
	// was found (empty input) or when any name is invalid.
	@Nullable
	public static String normalize(@Nullable final String raw) {
		if(raw == null) {
			return null;
		}

		String text = raw.trim();

		if(text.isEmpty()) {
			return null;
		}

		// Accept "/r/a+b+c" and full URLs as well as bare "a+b+c" lists.
		// The trailing path cut only applies to the URL form, so per-name
		// "r/" prefixes (e.g. "r/pics+r/funny") survive it.
		final int rIndex = text.indexOf("/r/");

		if(rIndex >= 0) {
			text = text.substring(rIndex + 3);

			// Drop any trailing path, query or fragment (order, .json, ...)
			final String[] head = text.split("[/?#]", 2);
			text = head[0];
		}

		final ArrayList<String> names = new ArrayList<>();
		final HashSet<String> seen = new HashSet<>();

		for(String name : text.split("[+,\\s]+")) {
			name = name.trim();

			if(name.regionMatches(true, 0, "r/", 0, 2)) {
				name = name.substring(2);
			}

			if(name.isEmpty()) {
				continue;
			}

			if(!VALID_NAME.matcher(name).matches()) {
				return null;
			}

			if(seen.add(StringUtils.asciiLowercase(name))) {
				names.add(StringUtils.asciiLowercase(name));
			}
		}

		if(names.isEmpty()) {
			return null;
		}

		final StringBuilder builder = new StringBuilder();

		for(final String name : names) {
			if(builder.length() > 0) {
				builder.append('+');
			}
			builder.append(name);
		}

		return builder.toString();
	}

	@NonNull
	public static String serializeEntries(@NonNull final List<Entry> entries) {
		final StringBuilder sb = new StringBuilder();
		sb.append('[');
		for(int i = 0; i < entries.size(); i++) {
			if(i > 0) {
				sb.append(',');
			}
			final Entry entry = entries.get(i);
			sb.append("{\"id\":");
			appendJsonString(sb, entry.id);
			sb.append(",\"name\":");
			appendJsonString(sb, entry.name);
			sb.append(",\"subreddits\":");
			appendJsonString(sb, entry.subreddits);
			sb.append('}');
		}
		sb.append(']');
		return sb.toString();
	}

	private static void appendJsonString(
			@NonNull final StringBuilder sb,
			@NonNull final String value) {
		sb.append('"');
		for(int i = 0; i < value.length(); i++) {
			final char ch = value.charAt(i);
			switch(ch) {
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				default:
					if(ch < 0x20) {
						sb.append(String.format(
								Locale.ROOT,
								"\\u%04x",
								(int)ch));
					} else {
						sb.append(ch);
					}
					break;
			}
		}
		sb.append('"');
	}

	// Pure-JVM JSON array parser so serialization tests run without Robolectric
	// (org.json is stubbed in plain Android unit tests).
	@NonNull
	public static List<Entry> parseEntries(@Nullable final String rawJson) {
		if(rawJson == null) {
			return Collections.emptyList();
		}
		final JsonCursor cursor = new JsonCursor(rawJson);
		cursor.skipWhitespace();
		if(!cursor.consume('[')) {
			return Collections.emptyList();
		}
		final ArrayList<Entry> result = new ArrayList<>();
		final HashSet<String> seenIds = new HashSet<>();
		cursor.skipWhitespace();
		if(cursor.consume(']')) {
			return result;
		}
		while(cursor.hasRemaining()) {
			cursor.skipWhitespace();
			final Entry parsed = parseEntryObject(cursor);
			if(parsed == null) {
				return Collections.emptyList();
			}
			if(!parsed.id.isEmpty() && seenIds.add(parsed.id)) {
				result.add(parsed);
			}
			cursor.skipWhitespace();
			if(cursor.consume(',')) {
				continue;
			}
			if(cursor.consume(']')) {
				return result;
			}
			return Collections.emptyList();
		}
		return Collections.emptyList();
	}

	@Nullable
	private static Entry parseEntryObject(@NonNull final JsonCursor cursor) {
		if(!cursor.consume('{')) {
			return null;
		}
		String id = null;
		String name = "";
		String subreddits = null;
		cursor.skipWhitespace();
		if(!cursor.consume('}')) {
			while(cursor.hasRemaining()) {
				cursor.skipWhitespace();
				final String key = cursor.readString();
				if(key == null) {
					return null;
				}
				cursor.skipWhitespace();
				if(!cursor.consume(':')) {
					return null;
				}
				cursor.skipWhitespace();
				if(!cursor.skipOrReadStringStart()) {
					return null;
				}
				final String val = cursor.readString();
				if(val == null) {
					return null;
				}
				if("id".equals(key)) {
					id = val;
				} else if("name".equals(key)) {
					name = val;
				} else if("subreddits".equals(key)) {
					subreddits = val;
				}
				cursor.skipWhitespace();
				if(cursor.consume(',')) {
					continue;
				}
				if(cursor.consume('}')) {
					break;
				}
				return null;
			}
		}
		if(id == null || subreddits == null) {
			return null;
		}
		final String normalized = normalize(subreddits);
		if(normalized == null) {
			return null;
		}
		return new Entry(id, cleanEntryName(name), normalized);
	}

	private static final class JsonCursor {
		@NonNull private final String mText;
		private int mPos;

		private JsonCursor(@NonNull final String text) {
			mText = text;
		}

		private boolean hasRemaining() {
			return mPos < mText.length();
		}

		private void skipWhitespace() {
			while(mPos < mText.length()) {
				final char ch = mText.charAt(mPos);
				if(ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
					mPos++;
				} else {
					break;
				}
			}
		}

		private boolean consume(final char expected) {
			if(mPos < mText.length() && mText.charAt(mPos) == expected) {
				mPos++;
				return true;
			}
			return false;
		}

		private boolean skipOrReadStringStart() {
			return mPos < mText.length() && mText.charAt(mPos) == '"';
		}

		@Nullable
		private String readString() {
			if(!consume('"')) {
				return null;
			}
			final StringBuilder sb = new StringBuilder();
			while(mPos < mText.length()) {
				final char ch = mText.charAt(mPos++);
				if(ch == '"') {
					return sb.toString();
				}
				if(ch == '\\') {
					if(mPos >= mText.length()) {
						return null;
					}
					final char esc = mText.charAt(mPos++);
					switch(esc) {
						case '"':
						case '\\':
						case '/':
							sb.append(esc);
							break;
						case 'b':
							sb.append('\b');
							break;
						case 'f':
							sb.append('\f');
							break;
						case 'n':
							sb.append('\n');
							break;
						case 'r':
							sb.append('\r');
							break;
						case 't':
							sb.append('\t');
							break;
						case 'u': {
							if(mPos + 4 > mText.length()) {
								return null;
							}
							try {
								final int code = Integer.parseInt(
										mText.substring(mPos, mPos + 4),
										16);
								sb.append((char)code);
								mPos += 4;
							} catch(final NumberFormatException e) {
								return null;
							}
							break;
						}
						default:
							return null;
					}
				} else {
					sb.append(ch);
				}
			}
			return null;
		}
	}
}
