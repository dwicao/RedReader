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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.quantumbadger.redreader.reddit.prepared.FavouriteViewMode

class FavouriteViewModeTest {

	@Test
	fun knownValuesParse() {
		assertEquals(FavouriteViewMode.LIST, FavouriteViewMode.fromString("list"))
		assertEquals(
			FavouriteViewMode.LIST_THUMBNAILS,
			FavouriteViewMode.fromString("list_thumbnails"))
		assertEquals(FavouriteViewMode.GRID_3, FavouriteViewMode.fromString("grid_3"))
	}

	@Test
	fun unknownValuesFallBackToList() {
		assertEquals(FavouriteViewMode.LIST, FavouriteViewMode.fromString(null))
		assertEquals(FavouriteViewMode.LIST, FavouriteViewMode.fromString("grid_9"))
	}

	@Test
	fun gridModesExposeColumnCounts() {
		assertEquals(1, FavouriteViewMode.GRID_1.columns)
		assertEquals(2, FavouriteViewMode.GRID_2.columns)
		assertEquals(3, FavouriteViewMode.GRID_3.columns)
		assertEquals(4, FavouriteViewMode.GRID_4.columns)
		assertEquals(1, FavouriteViewMode.LIST.columns)
	}

	@Test
	fun onlyListHidesThumbnails() {
		assertEquals(false, FavouriteViewMode.LIST.showThumbnails)
		assertEquals(true, FavouriteViewMode.LIST_THUMBNAILS.showThumbnails)
		assertEquals(true, FavouriteViewMode.GRID_2.showThumbnails)
	}
}
