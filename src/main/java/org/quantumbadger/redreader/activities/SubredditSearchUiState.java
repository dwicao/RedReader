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

package org.quantumbadger.redreader.activities;

public final class SubredditSearchUiState {

	private SubredditSearchUiState() {
	}

	public static boolean isLoadingVisible(
			final boolean subscriptionsReady,
			final boolean subscriptionsError,
			final boolean queryEmpty,
			final boolean queryResultsPresent,
			final boolean queryError) {

		if(subscriptionsError) {
			return false;
		}

		if(!subscriptionsReady) {
			return true;
		}

		if(queryResultsPresent) {
			return false;
		}

		if(queryEmpty) {
			return false;
		}

		if(queryError) {
			return false;
		}

		return true;
	}
}
