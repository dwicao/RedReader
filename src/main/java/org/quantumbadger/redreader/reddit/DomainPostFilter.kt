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
 * Finds posts whose link domain matches the words in the user's domain filter.
 * Domains are separated by commas or new lines, matched case-insensitively, and a
 * term also matches every subdomain of itself ("twitter.com" matches
 * "mobile.twitter.com"). Posts which match are hidden.
 */
object DomainPostFilter {

	@Volatile
	private var cachedRaw: String? = null

	@Volatile
	private var cachedTerms: List<String> = emptyList()

	/**
	 * Normalise a term the user typed into a bare domain: scheme, a leading "www.",
	 * a leading "*." and any path/query/fragment are stripped. Exposed for testing.
	 */
	@JvmStatic
	fun normalizeTerm(raw: String): String {

		var t = raw.trim().lowercase()

		t = t.removePrefix("https://")
		t = t.removePrefix("http://")
		t = t.removePrefix("//")

		// Keep only the host part (a path, query or fragment was included)
		t = t.substringBefore('/')
		t = t.substringBefore('?')

		while (t.startsWith("www.")) {
			t = t.removePrefix("www.")
		}

		t = t.removePrefix("*.")
		t = t.trim('.')

		return t
	}

	/**
	 * Parses the preference value into a list of domain terms. Exposed for testing.
	 */
	@JvmStatic
	fun parse(raw: String?): List<String> =
		raw?.split(',', '\n', '\r')
			?.map { normalizeTerm(it) }
			?.filter { it.isNotEmpty() }
			?: emptyList()

	/**
	 * The current terms, using a cached parse of the preference. The cache is rebuilt
	 * whenever the preference value changes.
	 */
	@JvmStatic
	fun terms(): List<String> {
		val raw = PrefsUtility.pref_behaviour_domain_filter()

		if (raw != cachedRaw) {
			cachedTerms = parse(raw)
			cachedRaw = raw
		}

		return cachedTerms
	}

	/**
	 * True when the filter is active (there is at least one domain to match against).
	 */
	@JvmStatic
	fun isEnabled(): Boolean = terms().isNotEmpty()

	/**
	 * True when the post's link domain matches at least one of the given terms.
	 */
	@JvmStatic
	fun matches(terms: List<String>, post: RedditPost): Boolean {

		if (terms.isEmpty()) {
			return false
		}

		val domain = post.domain?.decoded?.lowercase() ?: return false

		for (term in terms) {
			if (domain == term || domain.endsWith(".$term")) {
				return true
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
