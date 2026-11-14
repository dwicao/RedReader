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
import java.util.List;

/**
 * Remembers which links the user has opened, so visited links can be drawn in a
 * muted colour. Stored as one string of newline-separated SHA-256 hashes;
 * capping the size keeps the pref from growing forever.
 */
public final class VisitedLinks {

	private static final String PREF_KEY = "visited_links";

	private static final int MAX_ENTRIES = 2000;

	private static final Object LOCK = new Object();

	@Nullable
	private static ArrayList<String> sCached = null;

	private VisitedLinks() {
	}

	public static void markVisited(@NonNull final Context context, @NonNull final String url) {

		synchronized(LOCK) {
			final ArrayList<String> list = load(context);
			final String hash = General.sha256(url);

			// Move to the end (most recent) if already present
			list.remove(hash);
			list.add(hash);

			while(list.size() > MAX_ENTRIES) {
				list.remove(0);
			}

			sCached = list;

			General.getSharedPrefs(context)
					.edit()
					.putString(PREF_KEY, TextUtils.join("\n", list))
					.apply();
		}
	}

	public static boolean isVisited(@NonNull final Context context, @NonNull final String url) {

		synchronized(LOCK) {
			return load(context).contains(General.sha256(url));
		}
	}

	@NonNull
	private static ArrayList<String> load(@NonNull final Context context) {

		if(sCached == null) {
			final ArrayList<String> list = new ArrayList<>();

			final String raw = General.getSharedPrefs(context).getString(PREF_KEY, "");

			if(raw != null && !raw.isEmpty()) {
				for(final String line : raw.split("\n")) {
					if(!line.isEmpty()) {
						list.add(line);
					}
				}
			}

			sCached = list;
		}

		return sCached;
	}
}
