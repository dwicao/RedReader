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

package org.quantumbadger.redreader.reddit.prepared.bodytext;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.text.style.ClickableSpan;

import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.UriString;

/**
 * A link span which remembers its URL, so the view can repaint it in a muted
 * colour if the user has already visited it.
 */
public class VisitableClickableSpan extends ClickableSpan {

	@NonNull public final String url;

	@NonNull private final AppCompatActivity mActivity;

	public VisitableClickableSpan(
			@NonNull final AppCompatActivity activity,
			@NonNull final String url) {

		mActivity = activity;
		this.url = url;
	}

	@Override
	public void onClick(@NonNull final View widget) {
		LinkHandler.onLinkClicked(mActivity, new UriString(url));
	}
}
