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

package org.quantumbadger.redreader.views.bezelmenu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class BezelSwipeOverlay extends View {

	public static final int LEFT = 0;
	public static final int RIGHT = 1;

	@IntDef({LEFT, RIGHT})
	@Retention(RetentionPolicy.SOURCE)
	public @interface SwipeEdge {
	}

	private final BezelSwipeListener listener;

	private final int mSwipeZonePixels;

	public BezelSwipeOverlay(final Context context, final BezelSwipeListener listener) {
		super(context);
		this.listener = listener;

		final int swipeZoneDp = PrefsUtility.pref_behaviour_bezel_toolbar_swipezone_dp();

		mSwipeZonePixels = General.dpToPixels(getContext(), swipeZoneDp);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(final MotionEvent event) {

		final int action = event.getAction() & MotionEvent.ACTION_MASK;

		if(action == MotionEvent.ACTION_DOWN) {

			final int edge;

			if(event.getX() < mSwipeZonePixels) {
				edge = LEFT;

			} else if(event.getX() > getWidth() - mSwipeZonePixels) {
				edge = RIGHT;

			} else {
				return listener.onTap();
			}

			return listener.onSwipe(edge);
		}

		return false;
	}

	@Override
	protected void onSizeChanged(
			final int w,
			final int h,
			final int oldw,
			final int oldh) {

		super.onSizeChanged(w, h, oldw, oldh);
		updateSystemGestureExclusionRects();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		// Insets may not be available yet if this runs before layout
		post(this::updateSystemGestureExclusionRects);
	}

	// Ask the system to keep its navigation gestures out of the swipe zones,
	// so the post-side toolbar still works with gesture navigation enabled
	@RequiresApi(Build.VERSION_CODES.Q)
	private void updateSystemGestureExclusionRectsApi29(
			final int w,
			final int h) {

		final java.util.List<Rect> rects = new java.util.ArrayList<>(2);

		rects.add(new Rect(0, 0, mSwipeZonePixels, h));
		rects.add(new Rect(w - mSwipeZonePixels, 0, w, h));

		setSystemGestureExclusionRects(rects);
	}

	private void updateSystemGestureExclusionRects() {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && getWidth() > 0 && getHeight() > 0) {
			updateSystemGestureExclusionRectsApi29(getWidth(), getHeight());
		}
	}

	public interface BezelSwipeListener {
		boolean onSwipe(@SwipeEdge int edge);

		boolean onTap();
	}
}
