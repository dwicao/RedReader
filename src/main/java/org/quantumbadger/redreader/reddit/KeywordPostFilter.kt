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

package org.quantumbadger.redreader.reddit

import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.reddit.kthings.RedditPost

/**
 * Finds posts which match the words in the user's keyword filter, the way the RES
 * keyword filter does. Words are separated by commas or new lines, matched
 * case-insensitively against the post's title, flair and self-text, and posts which
 * match are hidden.
 */
object KeywordPostFilter {

	@Volatile
	private var cachedRaw: String? = null

	@Volatile
	private var cachedTerms: List<String> = emptyList()

	/**
	 * Parses the preference value into a list of lowercase terms. Exposed for testing.
	 */
	@JvmStatic
	fun parse(raw: String?): List<String> =
		raw?.split(',', '\n', '\r')
			?.map { it.trim().lowercase() }
			?.filter { it.isNotEmpty() }
			?: emptyList()

	/**
	 * The current terms, using a cached parse of the preference. The cache is rebuilt
	 * whenever the preference value changes.
	 */
	@JvmStatic
	fun terms(): List<String> {
		val raw = PrefsUtility.pref_behaviour_keyword_filter()

		if (raw != cachedRaw) {
			cachedTerms = parse(raw)
			cachedRaw = raw
		}

		return cachedTerms
	}

	/**
	 * True when the filter is active (there is at least one term to match against).
	 */
	@JvmStatic
	fun isEnabled(): Boolean = terms().isNotEmpty()

	/**
	 * True when the post matches at least one of the given terms, and so should be hidden.
	 */
	@JvmStatic
	fun matches(terms: List<String>, post: RedditPost): Boolean {

		if (terms.isEmpty()) {
			return false
		}

		val fields = listOf(
			post.title?.decoded,
			post.selftext?.decoded,
			post.link_flair_text?.decoded)

		for (field in fields) {
			if (field == null) {
				continue
			}

			val lower = field.lowercase()

			for (term in terms) {
				if (lower.contains(term)) {
					return true
				}
			}
		}

		return false
	}

	/**
	 * True when the post matches the current filter, and so should be hidden.
	 */
	@JvmStatic
	fun matches(post: RedditPost): Boolean = matches(terms(), post)
}
