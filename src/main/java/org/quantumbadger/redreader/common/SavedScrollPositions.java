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

/**
 * Remembers the scroll position in a post's comment listing, so reopening a long
 * thread (e.g. after the process was killed in the background) puts you back
 * where you were. Stored as lines of "postId|position|offset" in SharedPreferences.
 */
public final class SavedScrollPositions {

	private static final String PREF_KEY = "saved_scroll_positions";

	private static final int MAX_ENTRIES = 500;

	private static final Object LOCK = new Object();

	@Nullable
	private static ArrayList<String> sCached = null;

	private SavedScrollPositions() {
	}

	@Nullable
	public static Position load(@NonNull final Context context, @NonNull final String postId) {

		synchronized(LOCK) {
			for(final String line : loadLines(context)) {
				final String[] parts = line.split("\\|", 3);

				if(parts.length != 3 || !parts[0].equals(postId)) {
					continue;
				}

				try {
					return new Position(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));

				} catch(final NumberFormatException e) {
					// Corrupt entry: ignore it
					return null;
				}
			}

			return null;
		}
	}

	public static void save(
			@NonNull final Context context,
			@NonNull final String postId,
			final int position,
			final int offset) {

		synchronized(LOCK) {
			final ArrayList<String> lines = new ArrayList<>();

			for(final String line : loadLines(context)) {
				final int sep = line.indexOf('|');

				if(sep < 0 || !line.substring(0, sep).equals(postId)) {
					lines.add(line);
				}
			}

			lines.add(postId + "|" + position + "|" + offset);

			while(lines.size() > MAX_ENTRIES) {
				lines.remove(0);
			}

			sCached = lines;

			General.getSharedPrefs(context)
					.edit()
					.putString(PREF_KEY, TextUtils.join("\n", lines))
					.apply();
		}
	}

	@NonNull
	private static ArrayList<String> loadLines(@NonNull final Context context) {

		if(sCached == null) {
			final ArrayList<String> lines = new ArrayList<>();

			final String raw = General.getSharedPrefs(context).getString(PREF_KEY, "");

			if(raw != null && !raw.isEmpty()) {
				for(final String line : raw.split("\n")) {
					if(!line.isEmpty()) {
						lines.add(line);
					}
				}
			}

			sCached = lines;
		}

		return sCached;
	}

	public static final class Position {
		public final int position;
		public final int offset;

		public Position(final int position, final int offset) {
			this.position = position;
			this.offset = offset;
		}
	}
}
