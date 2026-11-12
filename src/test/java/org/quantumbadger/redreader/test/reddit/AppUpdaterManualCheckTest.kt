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
import org.quantumbadger.redreader.update.AppUpdateInfo
import org.quantumbadger.redreader.update.AppUpdater

class AppUpdaterManualCheckTest {

	private val info = AppUpdateInfo.parse(
		"""
		{
			"version": "1.40",
			"versionCode": 142,
			"apkUrl": "https://example.com/RedReader-release.apk"
		}
		""".trimIndent())

	@Test
	fun newerVersionCodeOffersUpdate() {
		assertEquals(
			AppUpdater.ManualCheckOutcome.UPDATE_AVAILABLE,
			AppUpdater.decideManualOutcome(info, 141))
	}

	@Test
	fun sameVersionCodeIsUpToDate() {
		assertEquals(
			AppUpdater.ManualCheckOutcome.UP_TO_DATE,
			AppUpdater.decideManualOutcome(info, 142))
	}

	@Test
	fun olderVersionCodeIsUpToDate() {
		assertEquals(
			AppUpdater.ManualCheckOutcome.UP_TO_DATE,
			AppUpdater.decideManualOutcome(info, 143))
	}

	@Test
	fun missingInfoIsFailed() {
		assertEquals(
			AppUpdater.ManualCheckOutcome.FAILED,
			AppUpdater.decideManualOutcome(null, 141))
	}

	@Test
	fun missingInstalledCodeIsFailed() {
		assertEquals(
			AppUpdater.ManualCheckOutcome.FAILED,
			AppUpdater.decideManualOutcome(info, null))
	}
}
