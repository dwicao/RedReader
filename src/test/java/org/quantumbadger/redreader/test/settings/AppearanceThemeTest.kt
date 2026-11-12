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

package org.quantumbadger.redreader.test.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import org.quantumbadger.redreader.settings.types.AppearanceTheme

class AppearanceThemeTest {

	@Test
	fun autoResolvesToNightWhenSystemNight() {
		assertEquals(AppearanceTheme.NIGHT, AppearanceTheme.AUTO.resolved(true))
	}

	@Test
	fun autoResolvesToRedWhenSystemDay() {
		assertEquals(AppearanceTheme.RED, AppearanceTheme.AUTO.resolved(false))
	}

	@Test
	fun explicitThemesPassThrough() {
		assertEquals(AppearanceTheme.RED, AppearanceTheme.RED.resolved(true))
		assertEquals(AppearanceTheme.NIGHT, AppearanceTheme.NIGHT.resolved(false))
		assertEquals(
			AppearanceTheme.ULTRABLACK,
			AppearanceTheme.ULTRABLACK.resolved(true))
	}
}
