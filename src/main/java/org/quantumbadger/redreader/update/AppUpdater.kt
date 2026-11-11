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

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.Request
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.RedReader
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.http.okhttp.OKHTTPBackend
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object AppUpdater {

	const val VERSION_JSON_URL =
		"https://raw.githubusercontent.com/dwicao/RedReader/master/version.json"

	private const val TAG = "AppUpdater"

	private const val FILE_PROVIDER_AUTHORITY = "org.quantumbadger.redreader.fileprovider"

	private val sCheckStarted = AtomicBoolean(false)

	@JvmStatic
	fun checkForUpdates(activity: AppCompatActivity) {

		if(!sCheckStarted.compareAndSet(false, true)) {
			return
		}

		Thread {
			val info = try {
				fetchUpdateInfo()
			} catch(e: Exception) {
				Log.i(TAG, "Update check failed", e)
				return@Thread
			} ?: return@Thread

			val installedCode = try {
				RedReader.getInstance(activity).packageInfo.versionCode
			} catch(e: Exception) {
				Log.i(TAG, "Could not read installed version", e)
				return@Thread
			}

			if(!info.isNewerThan(installedCode)) {
				return@Thread
			}

			AndroidCommon.runOnUiThread { showUpdateDialog(activity, info) }
		}.start()
	}

	private fun fetchUpdateInfo(): AppUpdateInfo? {

		val request = Request.Builder().url(VERSION_JSON_URL).get().build()

		OKHTTPBackend.getClient().newCall(request).execute().use { response ->
			if(!response.isSuccessful) {
				return null
			}

			val body = response.body?.string() ?: return null

			return AppUpdateInfo.parse(body)
		}
	}

	private fun showUpdateDialog(activity: AppCompatActivity, info: AppUpdateInfo) {

		if(activity.isFinishing || activity.isDestroyed) {
			return
		}

		MaterialAlertDialogBuilder(activity)
			.setTitle(R.string.update_available_title)
			.setMessage(activity.getString(R.string.update_available_message, info.version))
			.setPositiveButton(R.string.update_button_download) { _, _ ->
				downloadUpdate(activity, info)
			}
			.setNegativeButton(R.string.update_button_later, null)
			.show()
	}

	private fun downloadUpdate(activity: AppCompatActivity, info: AppUpdateInfo) {

		Thread {
			try {
				val dir = File(activity.cacheDir, "updates").apply { mkdirs() }
				val outFile = File(dir, "RedReader-" + info.version + ".apk")

				val request = Request.Builder().url(info.apkUrl).get().build()

				OKHTTPBackend.getClient().newCall(request).execute().use { response ->
					if(!response.isSuccessful) {
						throw IOException("HTTP " + response.code)
					}

					val body = response.body ?: throw IOException("Empty body")
					val digest = MessageDigest.getInstance("SHA-256")

					FileOutputStream(outFile).use { out ->
						body.byteStream().use { input ->
							val buffer = ByteArray(64 * 1024)

							while(true) {
								val read = input.read(buffer)
								if(read < 0) {
									break
								}
								digest.update(buffer, 0, read)
								out.write(buffer, 0, read)
							}
						}
					}

					if(info.size != null && info.size != outFile.length()) {
						throw IOException("Size mismatch")
					}

					if(info.sha256 != null) {
						val actual = digest.digest().joinToString("") {
							String.format(Locale.US, "%02x", it)
						}

						if(!actual.equals(info.sha256, ignoreCase = true)) {
							throw IOException("Checksum mismatch")
						}
					}
				}

				AndroidCommon.runOnUiThread { promptInstall(activity, outFile) }

			} catch(e: Exception) {
				Log.i(TAG, "Update download failed", e)
				AndroidCommon.runOnUiThread {
					General.quickToast(activity, R.string.update_download_failed)
				}
			}
		}.start()
	}

	private fun promptInstall(activity: AppCompatActivity, apkFile: File) {

		if(activity.isFinishing || activity.isDestroyed) {
			return
		}

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
			&& !activity.packageManager.canRequestPackageInstalls()) {

			activity.startActivity(
				Intent(
					Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
					("package:" + activity.packageName).toUri()))

			General.quickToast(activity, R.string.update_allow_install_apps)
			return
		}

		val uri = FileProvider.getUriForFile(activity, FILE_PROVIDER_AUTHORITY, apkFile)

		val intent = Intent(Intent.ACTION_VIEW)
			.setDataAndType(uri, "application/vnd.android.package-archive")
			.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

		activity.startActivity(intent)
	}
}
