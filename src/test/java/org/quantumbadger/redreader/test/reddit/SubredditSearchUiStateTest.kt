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
package org.quantumbadger.redreader.test.reddit

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.quantumbadger.redreader.activities.SubredditSearchUiState

class SubredditSearchUiStateTest {

	@Test
	fun subscriptionsNotReadyShowsLoading() {
		assertTrue(
			SubredditSearchUiState.isLoadingVisible(
				false,
				false,
				true,
				false,
				false))
	}

	@Test
	fun subscriptionsErrorHidesLoading() {
		assertFalse(
			SubredditSearchUiState.isLoadingVisible(
				false,
				true,
				false,
				false,
				false))
	}

	@Test
	fun searchResultsHideLoading() {
		assertFalse(
			SubredditSearchUiState.isLoadingVisible(
				true,
				false,
				false,
				true,
				false))
	}

	@Test
	fun emptyQueryWhenIdleHidesLoading() {
		assertFalse(
			SubredditSearchUiState.isLoadingVisible(
				true,
				false,
				true,
				false,
				false))
	}

	@Test
	fun queryErrorHidesLoading() {
		assertFalse(
			SubredditSearchUiState.isLoadingVisible(
				true,
				false,
				false,
				false,
				true))
	}

	@Test
	fun pendingSearchShowsLoading() {
		assertTrue(
			SubredditSearchUiState.isLoadingVisible(
				true,
				false,
				false,
				false,
				false))
	}
}
