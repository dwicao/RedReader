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

package org.quantumbadger.redreader.reddit;

import androidx.annotation.Nullable;

import org.quantumbadger.redreader.reddit.kthings.RedditComment;
import org.quantumbadger.redreader.reddit.kthings.UrlEncodedString;

/**
 * Detects comments from AutoModerator and from accounts speaking with moderator
 * or admin distinction, when those comments aren't stickied. These are usually
 * automated rule responses or boilerplate rather than actual conversation.
 */
public final class BotCommentFilter {

	private BotCommentFilter() {
	}

	public static boolean matches(@Nullable final RedditComment comment) {

		if(comment == null) {
			return false;
		}

		// Stickied comments are kept: they carry intentional announcements
		if(comment.getStickied()) {
			return false;
		}

		final String distinguished = comment.getDistinguished();

		if(distinguished != null
				&& (distinguished.equalsIgnoreCase("moderator")
				|| distinguished.equalsIgnoreCase("admin"))) {
			return true;
		}

		final UrlEncodedString author = comment.getAuthor();

		if(author != null && author.getDecoded().trim().equalsIgnoreCase("automoderator")) {
			return true;
		}

		return false;
	}
}
