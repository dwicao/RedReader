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

package org.quantumbadger.redreader.compose.ctx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.quantumbadger.redreader.compose.prefs.ComposePrefs
import org.quantumbadger.redreader.compose.prefs.LocalComposePrefs
import org.quantumbadger.redreader.compose.prefs.Preference
import org.quantumbadger.redreader.compose.theme.RRComposeContextTheme
import org.quantumbadger.redreader.settings.types.AlbumViewMode
import org.quantumbadger.redreader.settings.types.AppearanceTheme

@Composable
fun RRComposePreviewContext(content: @Composable () -> Unit) {

	val prefValues = object : ComposePrefs {
		override val appearanceTheme: Preference<AppearanceTheme> = previewPref(AppearanceTheme.RED)
		override val appearanceFontScaleGlobal = 1f
		override val appearanceFontScaleBodyText = 1f
		override val appearanceFontScalePosts = 1f
		override val appearanceFontScalePostSubtitles = 1f
		override val albumViewMode = previewPref(AlbumViewMode.Cards)
		override val albumCardShowButtons = previewPref(true)
		override val albumListShowThumbnails = previewPref(true)
		override val albumGridCropToSquare = previewPref(false)
		override val albumGridColumns = previewPref(3)
		override val albumListThumbnailSize = previewPref(64)
		override val albumListShowButtons = previewPref(true)
		override val albumGridRoundedCorners = previewPref(true)
		override val albumGridHorizontalPadding = previewPref(true)
		override val albumCompactTitle = previewPref(false)
	}

	CompositionLocalProvider(
		LocalComposePrefs provides prefValues,
		LocalLauncher provides {},
	) {
		RRComposeContextTheme {
			content()
		}
	}
}

private fun <T> previewPref(value: T) = object : Preference<T> {
	override var value: T
		get() = value
		set(value) {}
}
