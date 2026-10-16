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
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// A single user-defined subreddit combination (e.g. "pics+funny+askreddit"),
// shown as its own main menu entry. Stored as one raw string so it can be
// pasted straight from a multireddit URL. No resource keys are used, so this
// needs no strings.xml changes.
public final class CustomSubreddits {

	private static final String PREF_KEY = "custom_subscribed_subreddits";

	private static final Pattern VALID_NAME = Pattern.compile("[\\w.]+");

	private CustomSubreddits() {
	}

	@NonNull
	public static String get(final Context context) {
		final String value = General.getSharedPrefs(context).getString(PREF_KEY, "");
		return value == null ? "" : value;
	}

	public static void set(final Context context, @NonNull final String combination) {
		General.getSharedPrefs(context).edit().putString(PREF_KEY, combination).apply();
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
	public static List<String> members(@NonNull final Context context) {
		final String raw = get(context);
		if(raw == null || raw.isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.asList(raw.split("\\+", -1));
	}

	public static boolean isMember(@NonNull final Context context,
			@NonNull final String subreddit) {
		final String name = subreddit.trim().toLowerCase(Locale.ROOT);
		if(name.startsWith("r/")) {
			return members(context).contains(name.substring(2));
		}
		return members(context).contains(name);
	}

	public static void add(@NonNull final Context context,
			@NonNull final String subreddit) {
		final String name = subreddit.trim().toLowerCase(Locale.ROOT);
		final String normalized = CustomSubreddits.normalize(
				name.startsWith("r/") ? name.substring(2) : name);
		if(normalized != null) {
			set(context, normalized);
		}
	}

	public static void remove(@NonNull final Context context,
			@NonNull final String subreddit) {
		final String name = subreddit.trim().toLowerCase(Locale.ROOT);
		final String target = name.startsWith("r/") ? name.substring(2) : name;
		final List<String> members = members(context);
		if(!members.contains(target)) {
			return;
		}
		final List<String> updated = new ArrayList<>(members);
		updated.remove(target);
		set(context, TextUtils.join("+", updated));
	}
}
