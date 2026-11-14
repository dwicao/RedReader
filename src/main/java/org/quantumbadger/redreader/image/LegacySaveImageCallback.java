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

package org.quantumbadger.redreader.image;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.Nullable;

import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.activities.BaseActivity;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.common.FileUtils;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.Optional;
import org.quantumbadger.redreader.common.UriString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class LegacySaveImageCallback implements BaseActivity.PermissionCallback {
	private final BaseActivity activity;
	private final UriString uri;

	@Nullable
	private final String suggestedName;

	public LegacySaveImageCallback(final BaseActivity activity, final UriString uri) {
		this(activity, uri, null);
	}

	public LegacySaveImageCallback(
			final BaseActivity activity,
			final UriString uri,
			@Nullable final String suggestedName) {
		this.activity = activity;
		this.uri = uri;
		this.suggestedName = suggestedName;
	}

	@Override
	public void onPermissionGranted() {

		FileUtils.downloadImageToSave(
				activity,
				uri,
				(info, cacheFile, mimetype) -> {

					final String filename = suggestedName != null
							? suggestedName
							: General.filenameFromString(info.original.url.value);

					File dst = new File(
							Environment.getExternalStoragePublicDirectory(
									Environment.DIRECTORY_PICTURES),
							filename);

					if(dst.exists()) {
						int count = 0;

						while(dst.exists()) {
							count++;
							dst = new File(
									Environment.getExternalStoragePublicDirectory(
											Environment.DIRECTORY_PICTURES),
									count + "_" + filename.substring(1));
						}
					}

					try(InputStream cacheFileInputStream = cacheFile.getInputStream()) {
						FileUtils.copyFile(cacheFileInputStream, dst);

					} catch(final IOException e) {

						General.showResultDialog(
								activity,
								General.getGeneralErrorForFailure(
										activity,
										CacheRequest.RequestFailureType.STORAGE,
										new RuntimeException("Could not copy file", e),
										null,
										uri,
										Optional.empty()));

						return;
					}

					activity.sendBroadcast(new Intent(
							Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
							Uri.parse("file://" + dst.getAbsolutePath()))
					);

					General.quickToast(
							activity,
							activity.getString(R.string.action_save_image_success)
									+ " "
									+ dst.getAbsolutePath());
				});
	}

	@Override
	public void onPermissionDenied() {
		General.quickToast(activity, R.string.save_image_permission_denied);
	}
}
