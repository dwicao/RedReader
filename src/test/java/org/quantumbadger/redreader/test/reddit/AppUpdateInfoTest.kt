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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.quantumbadger.redreader.update.AppUpdateInfo

class AppUpdateInfoTest {

	private val fullJson = """
		{
			"version": "1.37",
			"versionCode": 142,
			"apkUrl": "https://github.com/dwicao/RedReader/releases/download/v1.37/RedReader-release.apk",
			"sha256": "1a426c891c53a4fe9d40b1f4532c64bec3bf11e7de01b44710430dd1b65ebbed",
			"size": 9642929,
			"unrelatedFutureField": true
		}
	""".trimIndent()

	@Test
	fun parsesFullDocument() {
		val info = AppUpdateInfo.parse(fullJson)

		assertEquals("1.37", info.version)
		assertEquals(142, info.versionCode)
		assertEquals(
			"https://github.com/dwicao/RedReader/releases/download/v1.37/RedReader-release.apk",
			info.apkUrl)
		assertEquals(
			"1a426c891c53a4fe9d40b1f4532c64bec3bf11e7de01b44710430dd1b65ebbed",
			info.sha256)
		assertEquals(9642929L, info.size)
	}

	@Test
	fun newerVersionCodeMeansUpdate() {
		val info = AppUpdateInfo.parse(fullJson)
		assertTrue(info.isNewerThan(141))
	}

	@Test
	fun sameVersionCodeMeansNoUpdate() {
		val info = AppUpdateInfo.parse(fullJson)
		assertFalse(info.isNewerThan(142))
	}

	@Test
	fun olderInstalledVersionCodeMeansNoUpdate() {
		val info = AppUpdateInfo.parse(fullJson)
		assertFalse(info.isNewerThan(143))
	}

	@Test(expected = IllegalArgumentException::class)
	fun missingVersionCodeFailsToParse() {
		AppUpdateInfo.parse(
			"""
			{
				"version": "4.0.0",
				"apkUrl": "https://github.com/dwicao/Litube/releases/download/v4.0.0/Litube-4.0.0.apk",
				"sha256": "dc971c58731087e4d6058a4b65ccda8c9b5d2ec709cc411eab39ce59a2542f1b",
				"size": 9370916
			}
			""".trimIndent())
	}
}
