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

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.content.Context;
import android.content.res.TypedArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.quantumbadger.redreader.activities.BaseActivity;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.VisitedLinks;
import org.quantumbadger.redreader.views.LinkifiedTextView;

public class BodyElementTextSpanned extends BodyElement implements DynamicSpanned {

	@NonNull private final SpannableStringBuilder mSpanned;

	private LinkifiedTextView mTextView;

	public BodyElementTextSpanned(
			@NonNull final BlockType blockType,
			@NonNull final SpannableStringBuilder spanned) {
		super(blockType);
		mSpanned = spanned;
	}

	@Override
	public void addSpanDynamic(final Object what, final int start, final int end, final int flags) {
		AndroidCommon.runOnUiThread(() -> {
			final int[] clamped = clampSpan(mSpanned.length(), start, end);
			mSpanned.setSpan(what, clamped[0], clamped[1], flags);
			if(mTextView != null) {
				mTextView.setText(mSpanned);
			}
		});
	}

	// Spans arrive from asynchronous image loads with indices computed earlier.
	// If the text changed size since, the indices may be out of range or reversed,
	// and setSpan would crash. Clamp instead.
	@NonNull
	static int[] clampSpan(final int length, final int start, final int end) {
		final int s = Math.max(0, Math.min(Math.min(start, end), length));
		final int e = Math.max(0, Math.min(Math.max(start, end), length));
		return new int[] { s, e };
	}

	// Repaint already-visited links in a muted colour. Done at view-generation time;
	// views are rebuilt on refresh, like any change to text styling would be.
	private void muteVisitedLinks(@NonNull final Context context) {

		final int mutedColor = resolveThemeColor(context, 0xFF9E9E9E);

		final VisitableClickableSpan[] spans = mSpanned.getSpans(
				0,
				mSpanned.length(),
				VisitableClickableSpan.class);

		for(final VisitableClickableSpan span : spans) {
			if(VisitedLinks.isVisited(context, span.url)) {
				mSpanned.setSpan(
						new ForegroundColorSpan(mutedColor),
						mSpanned.getSpanStart(span),
						mSpanned.getSpanEnd(span),
						Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			}
		}
	}

	// Use the theme's secondary (muted) text colour for visited links, so they
	// fit every theme; fall back to mid-grey if it can't be resolved
	private static int resolveThemeColor(
			@NonNull final Context context,
			final int fallbackColor) {

		final TypedArray typedArray = context.obtainStyledAttributes(
				new int[] { android.R.attr.textColorSecondary });

		try {
			return typedArray.getColor(0, fallbackColor);

		} finally {
			typedArray.recycle();
		}
	}

	@Override
	public View generateView(
			@NonNull final BaseActivity activity,
			@Nullable final Integer textColor,
			@Nullable final Float textSize,
			final boolean showLinkButtons) {

		mTextView = new LinkifiedTextView(activity);

		if(textColor != null) {
			mTextView.setTextColor(textColor);
		}
		if(textSize != null) {
			mTextView.setTextSize(textSize);
		}
		AndroidCommon.applyGlobalLineSpacing(mTextView);

		if(PrefsUtility.pref_appearance_mute_visited_links()) {
			muteVisitedLinks(activity);
		}

		mTextView.setText(mSpanned, LinkifiedTextView.BufferType.SPANNABLE);

		if(PrefsUtility.pref_accessibility_separate_body_text_lines()) {

			mTextView.setFocusable(true);
		}

		return mTextView;
	}
}
