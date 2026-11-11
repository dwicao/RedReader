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

package org.quantumbadger.redreader.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AppUpdateInfo(
	val version: String,
	val versionCode: Int,
	val apkUrl: String,
	val sha256: String? = null,
	val size: Long? = null
) {

	fun isNewerThan(installedVersionCode: Int): Boolean {
		return versionCode > installedVersionCode
	}

	companion object {

		private val JSON = Json {
			ignoreUnknownKeys = true
		}

		fun parse(raw: String): AppUpdateInfo {
			return JSON.decodeFromString(raw)
		}
	}
}
