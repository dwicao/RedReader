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

package org.quantumbadger.redreader.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.lzyzsd.circleprogress.DonutProgress;

import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.General;

public class LoadingSpinnerView extends RelativeLayout {

	private static final int ANIMATIONS_DISABLED_PROGRESS_ID = generateViewId();

	private static final int ANIMATIONS_DISABLED_LABEL_ID = generateViewId();

	final DonutProgress mProgressView;

	public LoadingSpinnerView(
			@NonNull final Context context,
			@Nullable final AttributeSet attributeSet,
			final int defStyle) {

		super(context, attributeSet, defStyle);

		final TypedArray typedArray = context.obtainStyledAttributes(new int[] {
				R.attr.rrLoadingRingForegroundCol,
				R.attr.rrLoadingRingBackgroundCol
		});

		final int foreground = typedArray.getColor(0, Color.MAGENTA);
		final int background = typedArray.getColor(1, Color.GREEN);

		typedArray.recycle();

		mProgressView = new DonutProgress(context);
		mProgressView.setAspectIndicatorDisplay(false);
		mProgressView.setIndeterminate(true);
		mProgressView.setFinishedStrokeColor(foreground);
		mProgressView.setUnfinishedStrokeColor(background);
		final int progressStrokeWidthPx = General.dpToPixels(context, 10);
		mProgressView.setUnfinishedStrokeWidth(progressStrokeWidthPx);
		mProgressView.setFinishedStrokeWidth(progressStrokeWidthPx);
		mProgressView.setStartingDegree(-90);
		mProgressView.initPainters();

		// Animations disabled (e.g. battery saver or the accessibility setting):
		// show a static indicator with a label, so it is not mistaken for a stuck loader
		if(AndroidCommon.areAnimationsDisabled(context)) {
			mProgressView.setIndeterminate(false);
			mProgressView.setProgress(0.3f);
			mProgressView.setId(ANIMATIONS_DISABLED_PROGRESS_ID);

			final TextView label = new TextView(context);
			label.setId(ANIMATIONS_DISABLED_LABEL_ID);
			label.setText(R.string.download_loading);
			label.setTextColor(foreground);
			label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			label.setGravity(Gravity.CENTER);
			label.setPadding(0, General.dpToPixels(context, 4), 0, 0);

			addView(mProgressView);
			addView(label);

			final RelativeLayout.LayoutParams progressParams
					= (RelativeLayout.LayoutParams)mProgressView.getLayoutParams();
			progressParams.addRule(ABOVE, label.getId());
			progressParams.addRule(CENTER_HORIZONTAL);

			final RelativeLayout.LayoutParams labelParams
					= (RelativeLayout.LayoutParams)label.getLayoutParams();
			labelParams.addRule(CENTER_HORIZONTAL);
			labelParams.addRule(ALIGN_PARENT_BOTTOM);

			final int progressDimensionsPx = General.dpToPixels(context, 100);
			mProgressView.getLayoutParams().width = progressDimensionsPx;
			mProgressView.getLayoutParams().height = progressDimensionsPx;

			return;
		}

		addView(mProgressView);
		final int progressDimensionsPx = General.dpToPixels(context, 100);
		mProgressView.getLayoutParams().width = progressDimensionsPx;
		mProgressView.getLayoutParams().height = progressDimensionsPx;
		((LayoutParams)mProgressView.getLayoutParams()).addRule(CENTER_IN_PARENT);
	}

	public LoadingSpinnerView(
			@NonNull final Context context,
			@Nullable final AttributeSet attributeSet) {

		this(context, attributeSet, 0);
	}

	public LoadingSpinnerView(@NonNull final Context context) {
		this(context, null);
	}
}
