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

package org.quantumbadger.redreader.reddit.prepared

enum class FavouriteViewMode(
	val stringValue: String,
	val columns: Int,
	val showThumbnails: Boolean
) {
	LIST("list", 1, false),
	LIST_THUMBNAILS("list_thumbnails", 1, true),
	GRID_1("grid_1", 1, true),
	GRID_2("grid_2", 2, true),
	GRID_3("grid_3", 3, true),
	GRID_4("grid_4", 4, true);

	companion object {
		fun fromString(value: String?): FavouriteViewMode =
			entries.firstOrNull { it.stringValue == value } ?: LIST
	}
}
