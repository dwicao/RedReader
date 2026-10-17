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
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.BaseActivity;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.CacheRequestCallbacks;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.Constants;
import org.quantumbadger.redreader.common.DisplayUtils;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.GenericFactory;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.Priority;
import org.quantumbadger.redreader.common.RedReaderPicasso;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.common.UriString;
import org.quantumbadger.redreader.common.datastream.SeekableInputStream;
import org.quantumbadger.redreader.common.time.TimestampUTC;
import org.quantumbadger.redreader.fragments.PostListingFragment;
import org.quantumbadger.redreader.image.GetImageInfoListener;
import org.quantumbadger.redreader.image.ImageInfo;
import org.quantumbadger.redreader.reddit.api.RedditPostActions;
import org.quantumbadger.redreader.reddit.kthings.JsonUtils;
import org.quantumbadger.redreader.reddit.kthings.MaybeParseError;
import org.quantumbadger.redreader.reddit.kthings.RedditPost;
import org.quantumbadger.redreader.reddit.kthings.RedditThing;
import org.quantumbadger.redreader.reddit.prepared.InlinePreviewLoader;
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.views.liststatus.ErrorView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class RedditPostView extends FlingableItemView
		implements RedditPreparedPost.ThumbnailLoadedCallback,
		InlinePreviewLoader.Listener {

	private static final String TAG = "RedditPostView";

	private static final int GRID_THUMBNAIL_HEIGHT_DP = 110;

	private final AccessibilityActionManager mAccessibilityActionManager;

	private RedditPreparedPost mPost = null;
	private final TextView title;
	private final TextView subtitle;

	@NonNull private final ImageView mThumbnailView;
	@NonNull private final ImageView mOverlayIcon;

	@NonNull private final LinearLayout mOuterView;
	@NonNull private final LinearLayout mInnerView;
	@NonNull private final LinearLayout mCommentsButton;
	@NonNull private final TextView mCommentsText;
	@Nullable private TextView mScoreText = null;
	@Nullable private TextView mGridImageCount = null;
	@NonNull private final LinearLayout mPostErrors;
	@NonNull private final InlinePreviewHolderView mImagePreviewHolder;
	@NonNull private final ImageView mImagePreviewImageView;
	@NonNull private final ConstraintLayout mImagePreviewPlayOverlay;
	@NonNull private final LinearLayout mImagePreviewOuter;
	@NonNull private final LoadingSpinnerView mImagePreviewLoadingSpinner;

	// These are assigned only in the constructor, but only when mGridMode is true
	// (the grid layout has these views; the regular layout does not). mGridMode is
	// set from a constructor parameter, so javac's definite-assignment rules mean
	// these cannot be declared final — hence the PMD suppressions.
	@SuppressWarnings("PMD.ImmutableField")
	private ConstraintLayout mGridImageArea;
	@SuppressWarnings("PMD.ImmutableField")
	private FrameLayout mGridImageHolder;
	@SuppressWarnings("PMD.ImmutableField")
	private ConstraintLayout mGridPlayOverlay;
	@SuppressWarnings("PMD.ImmutableField")
	private LoadingSpinnerView mGridLoadingSpinner;

	private static final long GRID_LOAD_TIMEOUT_MS = 20_000;

	@Nullable private Runnable mGridLoadTimeout;

	private boolean mGridOriginalShown;

	private int mUsageId = 0;

	@Nullable private InlinePreviewLoader mPreviewLoader = null;

	@Nullable private Target mGridFallbackTarget = null;

	@Nullable private int[] mSubtitleThemeColors = null;

	private final Handler thumbnailHandler;

	private final BaseActivity mActivity;

	private final PrefsUtility.PostFlingAction mLeftFlingPref;
	private final PrefsUtility.PostFlingAction mRightFlingPref;
	private RedditPostActions.ActionDescriptionPair mLeftFlingAction;
	private RedditPostActions.ActionDescriptionPair mRightFlingAction;

	private final boolean mCommentsButtonPref;

	private final int
			rrPostTitleReadCol;
	private final int rrPostTitleCol;

	private final int mThumbnailSizePrefPixels;

	private final boolean mGridMode;

	@Override
	protected void onSetItemFlingPosition(final float position) {
		mOuterView.setTranslationX(position);
	}

	@NonNull
	@Override
	protected String getFlingLeftText() {

		mLeftFlingAction = RedditPostActions.ActionDescriptionPair.from(mPost, mLeftFlingPref);

		if(mLeftFlingAction != null) {
			return mActivity.getString(mLeftFlingAction.getDescriptionRes());
		} else {
			return "Disabled";
		}
	}

	@NonNull
	@Override
	protected String getFlingRightText() {

		mRightFlingAction = RedditPostActions.ActionDescriptionPair.from(mPost, mRightFlingPref);

		if(mRightFlingAction != null) {
			return mActivity.getString(mRightFlingAction.getDescriptionRes());
		} else {
			return "Disabled";
		}
	}

	@Override
	protected boolean allowFlingingLeft() {
		return mLeftFlingAction != null;
	}

	@Override
	protected boolean allowFlingingRight() {
		return mRightFlingAction != null;
	}

	@Override
	protected void onFlungLeft() {
		RedditPostActions.INSTANCE.onActionMenuItemSelected(
				mPost,
				mActivity,
				mLeftFlingAction.getAction());
	}

	@Override
	protected void onFlungRight() {
		RedditPostActions.INSTANCE.onActionMenuItemSelected(
				mPost,
				mActivity,
				mRightFlingAction.getAction());
	}



	public RedditPostView(
			final Context context,
			final PostListingFragment fragmentParent,
			final BaseActivity activity,
			final boolean leftHandedMode,
			final boolean gridMode) {

		super(context);
		mActivity = activity;
		mGridMode = gridMode;

		mAccessibilityActionManager = new AccessibilityActionManager(
				this,
				context.getResources());

		thumbnailHandler = new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(@NonNull final Message msg) {
				if(mUsageId != msg.what) {
					return;
				}
				if(msg.obj == null) {
					// Thumbnail download failed
					if(mGridMode) {
						mGridLoadingSpinner.setVisibility(GONE);
					}
					return;
				}
				final Bitmap bitmap = (Bitmap)msg.obj;

				if(!mGridMode || !mGridOriginalShown) {
					mThumbnailView.setImageBitmap(bitmap);
					if(mGridMode) {
						applyGridThumbnailPolicy(bitmap);
					}
				}
				if(mGridMode) {
					mGridLoadingSpinner.setVisibility(GONE);

					if(isGridVideo(mPost)) {
						applyGridColumnImageAspect(
								bitmap.getWidth(),
								bitmap.getHeight());
					}
				}
			}
		};

		final float dpScale = context.getResources().getDisplayMetrics().density;

		final float titleFontScale = PrefsUtility.appearance_fontscale_posts();
		final float subtitleFontScale = PrefsUtility.appearance_fontscale_post_subtitles();

		final View rootView =
				LayoutInflater.from(context).inflate(
						mGridMode ? R.layout.reddit_post_grid : R.layout.reddit_post,
						this,
						true);

		mOuterView = Objects.requireNonNull(rootView.findViewById(R.id.reddit_post_layout_outer));
		mInnerView = Objects.requireNonNull(rootView.findViewById(R.id.reddit_post_layout_inner));

		mPostErrors = Objects.requireNonNull(rootView.findViewById(R.id.reddit_post_errors));

		mImagePreviewHolder = Objects.requireNonNull(
				rootView.findViewById(R.id.reddit_post_image_preview_holder));

		mImagePreviewImageView = Objects.requireNonNull(
				rootView.findViewById(R.id.reddit_post_image_preview_imageview));

		mImagePreviewPlayOverlay = Objects.requireNonNull(
				rootView.findViewById(R.id.reddit_post_image_preview_play_overlay));

		mImagePreviewOuter = Objects.requireNonNull(
				rootView.findViewById(R.id.reddit_post_image_preview_outer));


		mImagePreviewLoadingSpinner = new LoadingSpinnerView(activity);
		mImagePreviewHolder.addView(mImagePreviewLoadingSpinner);

		if(mGridMode) {
			mGridImageArea = Objects.requireNonNull(
					rootView.findViewById(R.id.reddit_post_grid_image_area));

			mGridImageHolder = Objects.requireNonNull(
					rootView.findViewById(R.id.reddit_post_grid_image_holder));

			mGridPlayOverlay = Objects.requireNonNull(
					rootView.findViewById(R.id.reddit_post_grid_play_overlay));

			mGridLoadingSpinner = new LoadingSpinnerView(activity);
			final FrameLayout.LayoutParams gridSpinnerLayoutParams
					= new FrameLayout.LayoutParams(
							FrameLayout.LayoutParams.WRAP_CONTENT,
							FrameLayout.LayoutParams.WRAP_CONTENT,
							Gravity.CENTER);
			mGridImageHolder.addView(mGridLoadingSpinner, gridSpinnerLayoutParams);

			mGridImageCount = Objects.requireNonNull(
					rootView.findViewById(R.id.reddit_post_grid_image_count));
		}

		mThumbnailView = Objects.requireNonNull(
				rootView.findViewById(R.id.reddit_post_thumbnail_view));

		mOverlayIcon = Objects.requireNonNull(
				rootView.findViewById(R.id.reddit_post_overlay_icon));

		title = Objects.requireNonNull(rootView.findViewById(R.id.reddit_post_title));
		subtitle = Objects.requireNonNull(rootView.findViewById(R.id.reddit_post_subtitle));

		General.suppressAccessibilityTextChangeEvents(subtitle);

		mCommentsButtonPref =
				PrefsUtility.appearance_post_show_comments_button();

		mCommentsButton = rootView.findViewById(R.id.reddit_post_comments_button);
		mCommentsText = mCommentsButton.findViewById(R.id.reddit_post_comments_text);
		mScoreText = mCommentsButton.findViewById(R.id.reddit_post_score_text);

		if(!mCommentsButtonPref) {
			mInnerView.removeView(mCommentsButton);
		}

		// The elements in the layout below get reversed in left-hand mode. This only
		// makes sense for the horizontal list layout, not for the vertical grid cards.
		if(leftHandedMode && !mGridMode) {
			final ArrayList<View> innerViewElements = new ArrayList<>(3);
			for(int i = mInnerView.getChildCount() - 1; i >= 0; i--) {
				innerViewElements.add(mInnerView.getChildAt(i));
				mInnerView.removeViewAt(i);
			}

			for(int i = 0; i < innerViewElements.size(); i++) {
				mInnerView.addView(innerViewElements.get(i));
			}

			mInnerView.setNextFocusRightId(NO_ID);
			if (mCommentsButtonPref) {
				mInnerView.setNextFocusLeftId(mCommentsButton.getId());

				mCommentsButton.setNextFocusForwardId(R.id.reddit_post_layout_outer);
				mCommentsButton.setNextFocusRightId(R.id.reddit_post_layout_outer);
				mCommentsButton.setNextFocusLeftId(NO_ID);
			}
		}

		final OnLongClickListener longClickListener = v -> {
			RedditPostActions.INSTANCE.showActionMenu(mActivity, mPost);
			return true;
		};

		switch(PrefsUtility.pref_behaviour_post_tap_action()) {
			case LINK:
				mOuterView.setOnClickListener(v -> fragmentParent.onPostSelected(mPost));
				AndroidCommon.removeClickListeners(mThumbnailView);
				AndroidCommon.removeClickListeners(mImagePreviewOuter);
				AndroidCommon.removeClickListeners(title);
				break;

			case COMMENTS:
				mOuterView.setOnClickListener(v -> fragmentParent.onPostCommentsSelected(mPost));

				mThumbnailView.setOnClickListener(v -> fragmentParent.onPostSelected(mPost));
				mThumbnailView.setOnLongClickListener(longClickListener);

				mImagePreviewOuter.setOnClickListener(v -> fragmentParent.onPostSelected(mPost));
				mImagePreviewOuter.setOnLongClickListener(longClickListener);

				AndroidCommon.removeClickListeners(title);

				break;

			case TITLE_COMMENTS:
				mOuterView.setOnClickListener(v -> fragmentParent.onPostSelected(mPost));

				AndroidCommon.removeClickListeners(mThumbnailView);
				AndroidCommon.removeClickListeners(mImagePreviewOuter);

				title.setOnClickListener(v -> fragmentParent.onPostCommentsSelected(mPost));
				title.setOnLongClickListener(longClickListener);
				break;
		}

		mOuterView.setOnLongClickListener(longClickListener);

		if(mCommentsButtonPref) {
			mCommentsButton.setOnClickListener(v -> fragmentParent.onPostCommentsSelected(mPost));
		}

		title.setTextSize(
				TypedValue.COMPLEX_UNIT_PX,
				title.getTextSize() * titleFontScale);
		subtitle.setTextSize(
				TypedValue.COMPLEX_UNIT_PX,
				subtitle.getTextSize() * subtitleFontScale);

		mLeftFlingPref =
				PrefsUtility.pref_behaviour_fling_post_left();
		mRightFlingPref =
				PrefsUtility.pref_behaviour_fling_post_right();

		{
			final TypedArray attr = context.obtainStyledAttributes(new int[] {
					R.attr.rrPostTitleCol,
					R.attr.rrPostTitleReadCol,
			});

			rrPostTitleCol = attr.getColor(0, 0);
			rrPostTitleReadCol = attr.getColor(1, 0);
			attr.recycle();
		}

		mThumbnailSizePrefPixels = (int)(dpScale * PrefsUtility.images_thumbnail_size_dp());
	}

	@UiThread
	public void reset(@NonNull final RedditPreparedPost newPost) {

		final boolean isNewPost = newPost != mPost;
		final boolean showInlinePreview = !mGridMode && newPost.shouldShowInlinePreview();

		if(isNewPost) {

			if(mPreviewLoader != null) {
				mPreviewLoader.setListener(null);
				mPreviewLoader = null;
			}

			mThumbnailView.setImageBitmap(null);
			mImagePreviewImageView.setImageBitmap(null);
			mImagePreviewPlayOverlay.setVisibility(GONE);
			mPostErrors.removeAllViews();

			mUsageId++;
			mGridOriginalShown = false;

			// Cancel any in-flight grid image load for this view, so a stale
			// request from a previous bind can't paint over the new post's
			// content (Picasso only cancels when a new request targets the view).
			RedReaderPicasso.INSTANCE.get(mActivity).cancelRequest(mThumbnailView);

			if(mGridFallbackTarget != null) {
				RedReaderPicasso.INSTANCE.get(mActivity).cancelRequest(mGridFallbackTarget);
				mGridFallbackTarget = null;
			}

			resetSwipeState();

			title.setText(newPost.src.getTitle());

			// TextView doesn't request a layout if the new text fits in the same number
			// of lines at the view's current width, which leaves stale sizes in its
			// measure cache (and its parent's). When this recycled view is then measured
			// at a different width (e.g. the thumbnail column appearing or disappearing),
			// those stale sizes are reused, and the title is cut off.
			title.requestLayout();

			if(mCommentsButtonPref) {
				mCommentsText.setText(String.valueOf(newPost.src.getSrc().getNum_comments()));
			}

			final boolean showGridImage = mGridMode && newPost.shouldShowGridImage();

			final boolean showGridYoutube = mGridMode && newPost.shouldShowGridYoutube();

			final boolean showGridCrosspost = mGridMode && newPost.shouldShowGridCrosspost();

			final boolean showThumbnail = !showInlinePreview
					&& !showGridImage
					&& !showGridYoutube
					&& !showGridCrosspost
					&& newPost.hasThumbnail;

			if(!showInlinePreview) {
				mImagePreviewLoadingSpinner.setVisibility(GONE);
				mImagePreviewOuter.setVisibility(GONE);
				setBottomMargin(false);
			}

			if(mGridMode) {

				if(showGridYoutube) {
					// YouTube links carry no usable Reddit preview, so the card
					// uses YouTube's own thumbnail with a play overlay.
					mGridImageArea.setVisibility(VISIBLE);
					downloadGridYoutube(newPost, mUsageId);

				} else if(showGridCrosspost) {
					// Crossposts often carry no preview of their own; fall back
					// to the parent post's preview.
					mGridImageArea.setVisibility(VISIBLE);
					downloadGridCrosspost(newPost, mUsageId);

				} else if(showGridImage) {
					// Full-width high-res preview with the image's real aspect ratio
					mGridImageArea.setVisibility(VISIBLE);
					downloadGridPreview(newPost, mUsageId);

				} else if(showThumbnail) {
					// Try the linked image first (for posts that link directly to an
					// image but have no preview data), falling back to the strip.
					mGridImageArea.setVisibility(VISIBLE);
					downloadGridFallbackImage(newPost, mUsageId);

				} else {
					mGridImageArea.setVisibility(GONE);
					mInnerView.setMinimumHeight(General.dpToPixels(mActivity, 64));
				}

			} else if(showThumbnail) {

				final Bitmap thumbnail = newPost.getThumbnail(this, mUsageId);
				mThumbnailView.setImageBitmap(thumbnail);

				if(thumbnail == null && newPost.isVideoPreview()) {
					newPost.ensureVideoPoster(mActivity, mThumbnailSizePrefPixels);
				}

				mThumbnailView.setVisibility(VISIBLE);

				mThumbnailView.setMinimumWidth(mThumbnailSizePrefPixels);

				General.setLayoutWidthHeight(
						mThumbnailView,
						ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.MATCH_PARENT);

				mInnerView.setMinimumHeight(mThumbnailSizePrefPixels);

			} else {
				mThumbnailView.setMinimumWidth(0);
				mThumbnailView.setVisibility(GONE);
				mInnerView.setMinimumHeight(General.dpToPixels(mActivity, 64));
			}

			if(mGridMode) {
				// Grid card footer: upvote count and multi-image indicator
				if(mScoreText != null) {
					mScoreText.setText(formatScore(newPost.computeScore()));
				}

				if(mGridImageCount != null) {
					final int imageCount = newPost.src.getGalleryImageCount();

					if(imageCount > 1) {
						//noinspection SetTextI18n
						mGridImageCount.setText("[1/" + imageCount + "]");
						mGridImageCount.setVisibility(VISIBLE);
					} else {
						mGridImageCount.setVisibility(GONE);
					}
				}
			}
		}

		if(mPost != null) {
			mPost.unbind(this);
		}

		newPost.bind(this);

		mPost = newPost;

		// Attaching to the preview loader calls back into this view, so must happen after
		// mPost has been updated
		if(isNewPost && showInlinePreview) {
			attachInlinePreview(newPost);
		}

		updateAppearance();
	}

	private int[] getSubtitleThemeColors() {

		if(mSubtitleThemeColors == null) {

			final TypedArray appearance = mActivity.obtainStyledAttributes(new int[] {
					R.attr.rrPostSubtitleBoldCol,
					R.attr.rrPostSubtitleUpvoteCol,
					R.attr.rrPostSubtitleDownvoteCol,
					R.attr.rrFlairBackCol,
					R.attr.rrFlairTextCol,
					R.attr.rrGoldTextCol,
					R.attr.rrGoldBackCol,
					R.attr.rrCrosspostTextCol,
					R.attr.rrCrosspostBackCol,
					R.attr.rrLockedpostTextCol,
					R.attr.rrLockedpostBackCol
			});

			mSubtitleThemeColors = new int[] {
					appearance.getColor(0, 255),
					appearance.getColor(1, 255),
					appearance.getColor(2, 255),
					appearance.getColor(3, 255),
					appearance.getColor(4, 255),
					appearance.getColor(5, 255),
					appearance.getColor(6, 255),
					appearance.getColor(7, 255),
					appearance.getColor(8, 255),
					appearance.getColor(9, 255),
					appearance.getColor(10, 255)
			};

			appearance.recycle();
		}

		return mSubtitleThemeColors;
	}

	public void updateAppearance() {

		mOuterView.setBackgroundResource(
				R.drawable.rr_postlist_item_selector_main);

		if(mCommentsButtonPref) {
			mCommentsButton.setBackgroundResource(
					R.drawable.rr_postlist_commentbutton_selector_main);
		}

		if(mPost.isRead()) {
			title.setTextColor(rrPostTitleReadCol);
		} else {
			title.setTextColor(rrPostTitleCol);
		}

		title.setContentDescription(mPost.buildAccessibilityTitle(mActivity, false));

		final int[] themeColors = getSubtitleThemeColors();

		subtitle.setText(mGridMode
				? mPost.buildSubtitle(mActivity, false, false, themeColors)
				: mPost.buildSubtitle(mActivity, false, true, themeColors));
		subtitle.requestLayout(); // See the comment on the title in reset()
		subtitle.setContentDescription(mPost.buildAccessibilitySubtitle(mActivity, false));

		boolean overlayVisible = true;

		if(mPost.isSaved()) {
			mOverlayIcon.setImageResource(R.drawable.star_dark);

		} else if(mPost.isHidden()) {
			mOverlayIcon.setImageResource(R.drawable.ic_action_cross_dark);

		} else if(mPost.isUpvoted()) {
			mOverlayIcon.setImageResource(R.drawable.arrow_up_bold_orangered);

		} else if(mPost.isDownvoted()) {
			mOverlayIcon.setImageResource(R.drawable.arrow_down_bold_periwinkle);

		} else {
			overlayVisible = false;
		}

		if(overlayVisible) {
			mOverlayIcon.setVisibility(VISIBLE);
		} else {
			mOverlayIcon.setVisibility(GONE);
		}

		RedditPostActions.INSTANCE.setupAccessibilityActions(
				mAccessibilityActionManager,
				mPost,
				mActivity,
				false);
	}

	@Override
	public void betterThumbnailAvailable(
			final Bitmap thumbnail,
			final int callbackUsageId) {
		final Message msg = Message.obtain();
		msg.obj = thumbnail;
		msg.what = callbackUsageId;
		thumbnailHandler.sendMessage(msg);
	}

	@Override
	public void thumbnailDownloadFailed(final int callbackUsageId) {
		final Message msg = Message.obtain();
		msg.obj = null;
		msg.what = callbackUsageId;
		thumbnailHandler.sendMessage(msg);
	}

	public interface PostSelectionListener {
		void onPostSelected(RedditPreparedPost post);

		void onPostCommentsSelected(RedditPreparedPost post);
	}

	private void setBottomMargin(final boolean enabled) {

		final MarginLayoutParams layoutParams
				= (MarginLayoutParams)mOuterView.getLayoutParams();

		if(enabled) {
			layoutParams.bottomMargin = General.dpToPixels(mActivity, 6);
		} else {
			layoutParams.bottomMargin = 0;
		}

		mOuterView.setLayoutParams(layoutParams);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		if(mPreviewLoader != null) {
			mPreviewLoader.setListener(this);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		// While this view is recycled it has no claim on the preview, which allows the
		// preview to be released if the post is also outside the preload window
		if(mPreviewLoader != null) {
			mPreviewLoader.setListener(null);
		}
	}

	private void attachInlinePreview(@NonNull final RedditPreparedPost post) {

		final InlinePreviewLoader.PreviewDetails details
				= InlinePreviewLoader.calculatePreviewDetails(mActivity, post);

		final InlinePreviewLoader loader = post.getInlinePreviewLoader(mActivity);

		if(details == null || loader == null) {
			mImagePreviewHolder.setImageSize(0, 0);
			mImagePreviewOuter.setVisibility(GONE);
			mImagePreviewLoadingSpinner.setVisibility(GONE);
			setBottomMargin(false);
			return;
		}

		mImagePreviewHolder.setImageSize(details.imageWidthPx, details.imageHeightPx);

		mImagePreviewOuter.setVisibility(VISIBLE);
		setBottomMargin(true);

		mPreviewLoader = loader;

		// This calls back into onInlinePreviewStateChanged with the current state, which
		// will be the fully loaded image if this post has already been preloaded
		loader.setListener(this);
	}

	private static boolean isGridVideo(@Nullable final RedditPreparedPost post) {
		return post != null
				&& (post.isVideoPreview() || post.src.getYoutubeId() != null);
	}

	private int getGridCardSize() {
		return Math.max(
				mThumbnailSizePrefPixels,
				DisplayUtils.getWindowVisibleDisplayFrame(mActivity).width() / 2);
	}

	// Tiny sources must not be upscaled into a blurry mess: show them at
	// native size, centered on the card background, instead.
	private void applyGridThumbnailPolicy(@Nullable final Bitmap bitmap) {
		if(bitmap != null && bitmap.getWidth() < getGridCardSize()) {
			mThumbnailView.setScaleType(ImageView.ScaleType.CENTER);
		} else {
			mThumbnailView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		}
	}

	private void downloadGridPreview(
			@NonNull final RedditPreparedPost post,
			final int usageId) {

		final Rect windowVisibleDisplayFrame
				= DisplayUtils.getWindowVisibleDisplayFrame(mActivity);

		// Request a preview rung above the card width (cards are a fraction of the
		// screen width in 2-4 column grids). This is bounded like the list preview
		// path: asking for the full window width on a high resolution screen can
		// exceed every rung Reddit offers for the post, which then selects the
		// full resolution original instead of a ladder rung.
		final int previewWidth = Math.min(
				1080,
				Math.max(640, windowVisibleDisplayFrame.width()));

		final RedditParsedPost.ImagePreviewDetails preview
				= post.src.getPreview(previewWidth, 0);

		displayGridPreviewDetails(post, preview, usageId);
	}

	private void displayGridPreviewDetails(
			@NonNull final RedditPreparedPost post,
			@Nullable final RedditParsedPost.ImagePreviewDetails preview,
			final int usageId) {

		if(preview == null || preview.width < 10 || preview.height < 10) {
			showGridFallbackThumbnail(post, usageId);
			return;
		}

		mThumbnailView.setVisibility(VISIBLE);
		mThumbnailView.setScaleType(ImageView.ScaleType.FIT_CENTER);

		// Size the card image to the image's real aspect ratio, so the whole
		// image is visible without cropping. Reset the holder to a
		// constraint-sized height so the ratio is actually applied (a previous
		// bind may have left an explicit fixed height behind).
		final ConstraintLayout.LayoutParams imageHolderLayoutParams
				= (ConstraintLayout.LayoutParams)mGridImageHolder.getLayoutParams();

		imageHolderLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
		imageHolderLayoutParams.height = 0;

		imageHolderLayoutParams.dimensionRatio
				= String.valueOf((float)preview.width / (float)preview.height);
		mGridImageHolder.setLayoutParams(imageHolderLayoutParams);

		mGridLoadingSpinner.setVisibility(VISIBLE);
		mGridPlayOverlay.setVisibility(GONE);

		startGridLoadTimeout(post, usageId);

		// Load the preview through Picasso with fit(), which downsamples the
		// decoded bitmap to the card's actual size (a fraction of the screen
		// width) instead of decoding the full-resolution image into memory.
		RedReaderPicasso.INSTANCE.get(mActivity)
				.load(preview.url.value)
				.fit()
				.centerInside()
				.into(mThumbnailView, new Callback() {
					@Override
					public void onSuccess() {

						if(usageId != mUsageId) {
							return;
						}

						cancelGridLoadTimeout();

						mGridLoadingSpinner.setVisibility(GONE);

						if(isGridVideo(post)) {
							mGridPlayOverlay.setVisibility(VISIBLE);
						}
					}

					@Override
					public void onError(final Exception e) {

						if(usageId != mUsageId) {
							return;
						}

						Log.e(TAG, "Failed to download grid preview: " + e, e);

						showGridFallbackThumbnail(post, usageId);
					}
				});
	}

	// YouTube links carry no usable Reddit preview data, so grid cards use
	// YouTube's own thumbnails: maxresdefault (1280x720) when it exists,
	// falling back to hqdefault (480x360, always available for public videos).
	private void downloadGridYoutube(
			@NonNull final RedditPreparedPost post,
			final int usageId) {

		final String youtubeId = post.src.getYoutubeId();

		if(youtubeId == null) {
			showGridFallbackThumbnail(post, usageId);
			return;
		}

		mThumbnailView.setVisibility(VISIBLE);
		mThumbnailView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		mGridLoadingSpinner.setVisibility(VISIBLE);
		mGridPlayOverlay.setVisibility(GONE);

		startGridLoadTimeout(post, usageId);

		mGridOriginalShown = false;

		// Provisional 16:9 aspect ratio, corrected once the image is loaded.
		final ConstraintLayout.LayoutParams imageHolderLayoutParams
				= (ConstraintLayout.LayoutParams)mGridImageHolder.getLayoutParams();

		imageHolderLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
		imageHolderLayoutParams.height = 0;
		imageHolderLayoutParams.dimensionRatio = "16:9";
		mGridImageHolder.setLayoutParams(imageHolderLayoutParams);

		final boolean[] triedStandardThumb = {false};

		final Target target = new Target() {

			@Override
			public void onBitmapLoaded(
					@NonNull final Bitmap bitmap,
					@NonNull final Picasso.LoadedFrom from) {

				if(usageId != mUsageId) {
					return;
				}

				// Cleared only after the generation check: doing it first would let a
				// late callback from the previous post clear the target this bind owns.
				mGridFallbackTarget = null;

				cancelGridLoadTimeout();

				mGridOriginalShown = true;

				final int width = bitmap.getWidth();
				final int height = bitmap.getHeight();

				if(width < 10 || height < 10) {
					showGridFallbackThumbnail(post, usageId);
					return;
				}

				imageHolderLayoutParams.dimensionRatio
						= String.valueOf((float)width / (float)height);
				mGridImageHolder.setLayoutParams(imageHolderLayoutParams);

				mThumbnailView.setImageBitmap(bitmap);
				mGridLoadingSpinner.setVisibility(GONE);
				mGridPlayOverlay.setVisibility(VISIBLE);
			}

			@Override
			public void onBitmapFailed(
					@NonNull final Exception e,
					@Nullable final Drawable errorDrawable) {

				if(usageId != mUsageId) {
					return;
				}

				if(!triedStandardThumb[0]) {
					triedStandardThumb[0] = true;
					startGridLoadTimeout(post, usageId);
					final Target retryTarget = mGridFallbackTarget;
					if(retryTarget != null) {
						RedReaderPicasso.INSTANCE.get(mActivity)
								.load("https://i.ytimg.com/vi/" + youtubeId + "/hqdefault.jpg")
								.into(retryTarget);
					} else {
						// The retry target has gone (e.g. the view was recycled), so
						// there is nothing to retry into; fall back rather than leaving
						// the card spinning until the load timeout expires.
						Log.e(TAG, "No target to retry YouTube thumbnail with: " + e, e);
						showGridFallbackThumbnail(post, usageId);
					}
				} else {
					mGridFallbackTarget = null;
					Log.e(TAG, "Failed to download YouTube thumbnail: " + e, e);
					showGridFallbackThumbnail(post, usageId);
				}
			}

			@Override
			public void onPrepareLoad(
					@Nullable final Drawable placeHolderDrawable) {

				// The loading spinner is already shown
			}
		};

		mGridFallbackTarget = target;

		RedReaderPicasso.INSTANCE.get(mActivity)
				.load("https://i.ytimg.com/vi/" + youtubeId + "/maxresdefault.jpg")
				.into(target);
	}

	// For posts where reddit didn't include usable preview data but the post
	// links directly to an image (e.g. a preview.redd.it URL): fetch the image
	// itself and display it at the card width with its real aspect ratio,
	// instead of the small fixed-height thumbnail strip.
	private void downloadGridFallbackImage(
			@NonNull final RedditPreparedPost post,
			final int usageId) {

		if(!post.shouldShowGridImageFallback()) {
			showGridFallbackThumbnail(post, usageId);
			return;
		}

		final UriString url = post.src.getUrl();

		if(url == null) {
			showGridFallbackThumbnail(post, usageId);
			return;
		}

		if(LinkHandler.isDirectImageUrl(url)) {
			loadGridOriginal(post, url, usageId, false);
			return;
		}

		// Image pages (imgur albums, gif hosts...) link to a page rather than
		// the media itself: resolve to the direct media URL first.
		mThumbnailView.setVisibility(VISIBLE);
		mGridLoadingSpinner.setVisibility(VISIBLE);
		mGridPlayOverlay.setVisibility(GONE);

		startGridLoadTimeout(post, usageId);

		LinkHandler.getGridCardImage(
				mActivity,
				url,
				new Priority(Constants.Priority.MEDIA_FALLBACK),
				new GetImageInfoListener() {
					@Override
					public void onFailure(@NonNull final RRError error) {
						AndroidCommon.runOnUiThread(() -> {
							if(usageId != mUsageId) {
								return;
							}
							showGridFallbackThumbnail(post, usageId);
						});
					}

					@Override
					public void onSuccess(final ImageInfo info) {
						AndroidCommon.runOnUiThread(() -> {
							if(usageId != mUsageId) {
								return;
							}
							if(info != null
									&& info.original != null
									&& info.original.url != null
									&& info.mediaType != ImageInfo.MediaType.VIDEO) {
								loadGridOriginal(
										post,
										info.original.url,
										usageId,
										LinkHandler.isEmbedVideoPage(url));
							} else {
								showGridFallbackThumbnail(post, usageId);
							}
						});
					}

					@Override
					public void onNotAnImage() {
						AndroidCommon.runOnUiThread(() -> {
							if(usageId != mUsageId) {
								return;
							}
							showGridFallbackThumbnail(post, usageId);
						});
					}
				});
	}

	private void loadGridOriginal(
			@NonNull final RedditPreparedPost post,
			@NonNull final UriString url,
			final int usageId,
			final boolean showPlayOverlay) {

		mThumbnailView.setVisibility(VISIBLE);
		mThumbnailView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		mGridLoadingSpinner.setVisibility(VISIBLE);
		mGridPlayOverlay.setVisibility(GONE);

		startGridLoadTimeout(post, usageId);

		mGridOriginalShown = false;

		final Bitmap placeholder = post.getThumbnail(this, mUsageId);
		if(placeholder != null) {
			mThumbnailView.setImageBitmap(placeholder);
		}
		post.ensureThumbnail(mActivity, mThumbnailSizePrefPixels);

		// Provisional square aspect ratio, corrected once the image is loaded.
		// Reset the holder to a constraint-sized height so the ratio actually
		// applies (a previous bind may have left an explicit fixed height).
		final ConstraintLayout.LayoutParams imageHolderLayoutParams
				= (ConstraintLayout.LayoutParams)mGridImageHolder.getLayoutParams();

		imageHolderLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
		imageHolderLayoutParams.height = 0;
		imageHolderLayoutParams.dimensionRatio = "1:1";
		mGridImageHolder.setLayoutParams(imageHolderLayoutParams);

		// Decode at roughly the card size rather than the full-resolution
		// image, so memory stays low; the loaded bitmap's dimensions give the
		// image's real aspect ratio.
		final Rect windowVisibleDisplayFrame
				= DisplayUtils.getWindowVisibleDisplayFrame(mActivity);

		final int targetSize = Math.max(640, windowVisibleDisplayFrame.width());

		final Target target = new Target() {

			@Override
			public void onBitmapLoaded(
					@NonNull final Bitmap bitmap,
					@NonNull final Picasso.LoadedFrom from) {

				if(usageId != mUsageId) {
					return;
				}

				// Cleared only after the generation check: doing it first would let a
				// late callback from the previous post clear the target this bind owns.
				mGridFallbackTarget = null;

				cancelGridLoadTimeout();

				mGridOriginalShown = true;

				final int width = bitmap.getWidth();
				final int height = bitmap.getHeight();

				if(width < 10 || height < 10) {
					showGridFallbackThumbnail(post, usageId);
					return;
				}

				imageHolderLayoutParams.dimensionRatio
						= String.valueOf((float)width / (float)height);
				mGridImageHolder.setLayoutParams(imageHolderLayoutParams);

				mThumbnailView.setImageBitmap(bitmap);
				mGridLoadingSpinner.setVisibility(GONE);

				if(isGridVideo(post) || showPlayOverlay) {
					mGridPlayOverlay.setVisibility(VISIBLE);
				}
			}

			@Override
			public void onBitmapFailed(
					@NonNull final Exception e,
					@Nullable final Drawable errorDrawable) {

				if(usageId != mUsageId) {
					return;
				}

				// Cleared only after the generation check: doing it first would let a
				// late callback from the previous post clear the target this bind owns.
				mGridFallbackTarget = null;

				Log.e(TAG, "Failed to download grid fallback image: " + e, e);

				showGridFallbackThumbnail(post, usageId);
			}

			@Override
			public void onPrepareLoad(
					@Nullable final Drawable placeHolderDrawable) {

				// The loading spinner is already shown
			}
		};

		mGridFallbackTarget = target;

		RedReaderPicasso.INSTANCE.get(mActivity)
				.load(url.value)
				.resize(targetSize, targetSize)
				.centerInside()
				.onlyScaleDown()
				.into(target);
	}

	// Crossposts often carry no preview of their own: use the parent post's
	// preview, from the embedded parent data when available, or fetched via
	// api/info.json otherwise (cached on disk by the CacheManager).
	private void downloadGridCrosspost(
			@NonNull final RedditPreparedPost post,
			final int usageId) {

		final int previewWidth = Math.max(640, getGridCardSize() * 2);

		final List<RedditPost> parents = post.src.getSrc().getCrosspost_parent_list();

		if(parents != null) {
			for(final RedditPost parent : parents) {
				final RedditParsedPost parsedParent
						= new RedditParsedPost(mActivity, parent, false);
				final RedditParsedPost.ImagePreviewDetails details
						= parsedParent.getPreview(previewWidth, 0);
				if(details != null) {
					displayGridPreviewDetails(post, details, usageId);
					return;
				}
			}
		}

		final String parentFullname = post.src.getCrosspostParent();

		if(parentFullname == null) {
			showGridFallbackThumbnail(post, usageId);
			return;
		}

		mThumbnailView.setVisibility(VISIBLE);
		mThumbnailView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		mGridLoadingSpinner.setVisibility(VISIBLE);
		mGridPlayOverlay.setVisibility(GONE);

		startGridLoadTimeout(post, usageId);

		mGridOriginalShown = false;

		// Provisional 16:9 aspect ratio, corrected once the image is loaded.
		final ConstraintLayout.LayoutParams imageHolderLayoutParams
				= (ConstraintLayout.LayoutParams)mGridImageHolder.getLayoutParams();

		imageHolderLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
		imageHolderLayoutParams.height = 0;
		imageHolderLayoutParams.dimensionRatio = "16:9";
		mGridImageHolder.setLayoutParams(imageHolderLayoutParams);

		fetchCrosspostParentPreview(post, parentFullname, previewWidth, usageId);
	}

	private void fetchCrosspostParentPreview(
			@NonNull final RedditPreparedPost post,
			@NonNull final String parentFullname,
			final int previewWidth,
			final int usageId) {

		final UriString url
				= Constants.Reddit.getUri("/api/info.json?id=" + parentFullname);

		CacheManager.getInstance(mActivity).makeRequest(new CacheRequest(
				url,
				RedditAccountManager.getInstance(mActivity).getDefaultAccount(),
				null,
				new Priority(Constants.Priority.COMMENT_PRECACHE),
				DownloadStrategyIfNotCached.INSTANCE,
				Constants.FileType.POST_LIST,
				CacheRequest.DownloadQueueType.REDDIT_API,
				mActivity,
				new CacheRequestCallbacks() {
					@Override
					public void onFailure(@NonNull final RRError error) {
						AndroidCommon.runOnUiThread(() -> {
							if(usageId != mUsageId) {
								return;
							}
							showGridFallbackThumbnail(post, usageId);
						});
					}

					@Override
					public void onDataStreamAvailable(
							final GenericFactory<SeekableInputStream, IOException> streamFactory,
							final TimestampUTC timestamp,
							final UUID session,
							final boolean fromCache,
							final String mimetype) {

						new Thread(null, () -> {
							final RedditParsedPost.ImagePreviewDetails details
									= parseCrosspostParentPreview(
											streamFactory,
											previewWidth);
							AndroidCommon.runOnUiThread(() -> {
								if(usageId != mUsageId) {
									return;
								}
								if(details != null) {
									displayGridPreviewDetails(post, details, usageId);
								} else {
									showGridFallbackThumbnail(post, usageId);
								}
							});
						}, "Crosspost preview fetch", 1_000_000).start();
					}
				}));
	}

	@Nullable
	private RedditParsedPost.ImagePreviewDetails parseCrosspostParentPreview(
			final GenericFactory<SeekableInputStream, IOException> streamFactory,
			final int previewWidth) {

		try {
			final RedditThing thing = JsonUtils.INSTANCE
					.decodeRedditThingFromStream(streamFactory.create());

			if(thing instanceof RedditThing.Listing) {
				for(final MaybeParseError<RedditThing> child
						: ((RedditThing.Listing)thing).getData().getChildren()) {

					if(!(child instanceof MaybeParseError.Ok)) {
						continue;
					}

					final RedditThing value
							= ((MaybeParseError.Ok<RedditThing>)child).getValue();

					if(!(value instanceof RedditThing.Post)) {
						continue;
					}

					final RedditParsedPost parsedParent = new RedditParsedPost(
							mActivity,
							((RedditThing.Post)value).getData(),
							false);
					final RedditParsedPost.ImagePreviewDetails found
							= parsedParent.getPreview(previewWidth, 0);

					if(found != null) {
						return found;
					}
				}
			}

		} catch(final Exception e) {
			return null;
		}

		return null;
	}

	// Formats a post score compactly, e.g. 23567 -> "23.6K", 1500 -> "1.5K",
	// 1234567 -> "1.2M", and smaller scores as plain numbers.
	private static String formatScore(final int score) {

		final boolean negative = score < 0;
		final long absolute = Math.abs((long)score);

		final String result;
		if(absolute >= 1_000_000L) {
			result = formatCompact(absolute / 1_000_000.0, "M");
		} else if(absolute >= 1_000L) {
			final String compact = formatCompact(absolute / 1_000.0, "K");
			result = "1000K".equals(compact) ? "1M" : compact;
		} else {
			result = String.valueOf(absolute);
		}

		return negative ? "-" + result : result;
	}

	private static String formatCompact(final double value, final String suffix) {

		final long tenths = Math.round(value * 10.0);

		if(tenths % 10 == 0) {
			return (tenths / 10) + suffix;
		}

		return (tenths / 10.0) + suffix;
	}

	private void startGridLoadTimeout(
			@NonNull final RedditPreparedPost post,
			final int usageId) {

		cancelGridLoadTimeout();

		mGridLoadTimeout = new Runnable() {
			@Override
			public void run() {

				mGridLoadTimeout = null;

				if(usageId != mUsageId || mGridLoadingSpinner.getVisibility() != VISIBLE) {
					return;
				}

				Log.e(
						TAG,
						"MediaTrace: grid image load timed out for " + post.src.getUrl());

				showGridFallbackThumbnail(post, usageId);
			}
		};

		thumbnailHandler.postDelayed(mGridLoadTimeout, GRID_LOAD_TIMEOUT_MS);
	}

	private void cancelGridLoadTimeout() {

		if(mGridLoadTimeout != null) {
			thumbnailHandler.removeCallbacks(mGridLoadTimeout);
			mGridLoadTimeout = null;
		}
	}

	private void applyGridColumnImageAspect(
			final int imageWidthPx,
			final int imageHeightPx) {

		if(imageWidthPx < 1 || imageHeightPx < 1) {
			return;
		}

		final ConstraintLayout.LayoutParams imageHolderLayoutParams
				= (ConstraintLayout.LayoutParams)mGridImageHolder.getLayoutParams();

		imageHolderLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
		imageHolderLayoutParams.height = 0;
		imageHolderLayoutParams.dimensionRatio
				= String.valueOf((float)imageWidthPx / (float)imageHeightPx);

		mGridImageHolder.setLayoutParams(imageHolderLayoutParams);
	}

	private void showGridFallbackThumbnail(
			@NonNull final RedditPreparedPost post,
			final int usageId) {

		cancelGridLoadTimeout();

		mGridLoadingSpinner.setVisibility(GONE);
		mGridPlayOverlay.setVisibility(GONE);

		// The strip is shown at full card width: request the thumbnail at card
		// size rather than the small list-strip size, so it isn't upscaled
		// from a tiny bitmap.
		final int cardSize = getGridCardSize();

		// A fixed-height strip, as used before the high-res grid previews
		final ConstraintLayout.LayoutParams imageHolderLayoutParams
				= (ConstraintLayout.LayoutParams)mGridImageHolder.getLayoutParams();

		imageHolderLayoutParams.dimensionRatio = null;
		mGridImageHolder.setLayoutParams(imageHolderLayoutParams);

		General.setLayoutWidthHeight(
				mGridImageHolder,
				ViewGroup.LayoutParams.MATCH_PARENT,
				General.dpToPixels(mActivity, GRID_THUMBNAIL_HEIGHT_DP));

		final Bitmap thumbnail = post.getThumbnail(this, usageId);
		mThumbnailView.setImageBitmap(thumbnail);
		mThumbnailView.setVisibility(VISIBLE);
		applyGridThumbnailPolicy(thumbnail);

		if(isGridVideo(post)) {
			mGridPlayOverlay.setVisibility(VISIBLE);
		}

		if(post.isVideoPreview()) {

			if(thumbnail != null) {
				applyGridColumnImageAspect(
						thumbnail.getWidth(),
						thumbnail.getHeight());
			}

			// Reddit supplies a still frame for hosted videos; use it when present.
			// Extracting a frame from the video instead means downloading the whole
			// multi-megabyte file to produce a single card image.
			final RedditParsedPost.ImagePreviewDetails videoPoster
					= post.src.getVideoPosterImage();

			if(videoPoster != null) {
				displayGridPreviewDetails(post, videoPoster, usageId);
			} else {
				post.ensureVideoPoster(
						mActivity,
						Math.max(mThumbnailSizePrefPixels, 720));
			}

		} else {

			post.ensureThumbnail(mActivity, cardSize);

			if(thumbnail == null && post.hasThumbnail) {
				mGridLoadingSpinner.setVisibility(VISIBLE);
			}
		}
	}

	@Override
	public void onInlinePreviewStateChanged(@NonNull final InlinePreviewLoader loader) {

		if(loader != mPreviewLoader) {
			return;
		}

		mPostErrors.removeAllViews();

		switch(loader.getState()) {

			case LOADED:

				final Bitmap loadedPreviewBitmap = loader.getBitmap();

				mImagePreviewImageView.setImageBitmap(loadedPreviewBitmap);
				mImagePreviewLoadingSpinner.setVisibility(GONE);
				mImagePreviewOuter.setVisibility(VISIBLE);
				setBottomMargin(true);

				if(loadedPreviewBitmap != null) {
					mImagePreviewHolder.setImageSize(
							loadedPreviewBitmap.getWidth(),
							loadedPreviewBitmap.getHeight());
				}

				if(mPost.isVideoPreview()) {
					mImagePreviewPlayOverlay.setVisibility(VISIBLE);
				}

				break;

			case FAILED:

				mImagePreviewImageView.setImageBitmap(null);
				mImagePreviewPlayOverlay.setVisibility(GONE);
				mImagePreviewLoadingSpinner.setVisibility(GONE);
				mImagePreviewOuter.setVisibility(GONE);

				final RRError error = loader.getError();

				if(error != null) {
					final ErrorView errorView = new ErrorView(mActivity, error);
					mPostErrors.addView(errorView);
					General.setLayoutMatchWidthWrapHeight(errorView);
				}

				break;

			case NOT_LOADED:
			case LOADING:
			default:

				mImagePreviewImageView.setImageBitmap(null);
				mImagePreviewPlayOverlay.setVisibility(GONE);
				mImagePreviewLoadingSpinner.setVisibility(VISIBLE);
				mImagePreviewOuter.setVisibility(VISIBLE);
				setBottomMargin(true);

				break;
		}
	}
}
