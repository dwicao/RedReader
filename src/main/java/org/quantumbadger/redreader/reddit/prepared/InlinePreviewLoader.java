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

package org.quantumbadger.redreader.reddit.prepared;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

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
import org.quantumbadger.redreader.common.Optional;
import org.quantumbadger.redreader.common.PrioritisedCachedThreadPool;
import org.quantumbadger.redreader.common.Priority;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.common.UriString;
import org.quantumbadger.redreader.common.datastream.SeekableInputStream;
import org.quantumbadger.redreader.common.time.TimestampUTC;
import org.quantumbadger.redreader.image.ScaledBitmapDecoder;

import java.io.IOException;
import java.util.UUID;

/**
 * Downloads and decodes the inline image preview for a single post.
 *
 * <p>The decoded bitmap is held here, rather than by the view which displays it, so that it
 * survives that view being recycled. The post listing keeps every post within a few
 * positions of the visible area active (see
 * {@link org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter#setPreloadWindow}),
 * which lets the user scroll back and forth over the same posts without any preview having
 * to be downloaded or decoded again.
 *
 * <p>When a post leaves that window, its preview is released and any download still in
 * progress for it is cancelled, which bounds the number of previews held in memory at once.
 *
 * <p>Every method must be called on the UI thread.
 */
public final class InlinePreviewLoader {

	private static final String TAG = "InlinePreviewLoader";

	public enum State {
		NOT_LOADED,
		LOADING,
		LOADED,
		FAILED
	}

	public interface Listener {
		@UiThread
		void onInlinePreviewStateChanged(@NonNull InlinePreviewLoader loader);
	}

	/**
	 * The preview image to show for a post, together with the size of the image itself and
	 * the size of the box to decode it into.
	 */
	public static final class PreviewDetails {

		@NonNull public final UriString url;

		// The size of the image itself, which determines the shape of the area it is
		// displayed in
		public final int imageWidthPx;
		public final int imageHeightPx;

		// The bounds the image is decoded within, which limit the memory it uses. These
		// are not the size it is displayed at, which is only known once the view
		// displaying it is measured.
		public final int boxWidthPx;
		public final int boxHeightPx;

		// True when the JSON had no preview metadata and the source URL is a fallback
		public final boolean isFallback;

		// True when the source is a frame to be extracted from the post's video
		public final boolean isVideoPoster;

		// When set, loaded after the placeholder URL as a higher-quality upgrade
		@Nullable public final UriString upgradeUrl;

		private PreviewDetails(
				@NonNull final UriString url,
				final int imageWidthPx,
				final int imageHeightPx,
				final int boxWidthPx,
				final int boxHeightPx,
				final boolean isFallback,
				final boolean isVideoPoster,
				@Nullable final UriString upgradeUrl) {

			this.url = url;
			this.imageWidthPx = imageWidthPx;
			this.imageHeightPx = imageHeightPx;
			this.boxWidthPx = boxWidthPx;
			this.boxHeightPx = boxHeightPx;
			this.isFallback = isFallback;
			this.isVideoPoster = isVideoPoster;
			this.upgradeUrl = upgradeUrl;
		}
	}

	/**
	 * The maximum height, in pixels, which an inline preview may take up, given the height
	 * of the area it is displayed in. Taller images are letterboxed into this height, so
	 * that a post never takes up so much of the list that it is hard to scroll past.
	 */
	public static int getMaxPreviewHeightPx(final int displayAreaHeightPx) {
		return Math.max(1, (Math.max(400, displayAreaHeightPx) * 7) / 8);
	}

	/**
	 * Returns null if there is no inline preview to show for this post.
	 */
	@Nullable
	@UiThread
	public static PreviewDetails calculatePreviewDetails(
			@NonNull final BaseActivity activity,
			@NonNull final RedditPreparedPost post) {

		if(!post.shouldShowInlinePreview()) {
			return null;
		}

		final Rect windowVisibleDisplayFrame
				= DisplayUtils.getWindowVisibleDisplayFrame(activity);

		final int windowWidth = Math.max(1, windowVisibleDisplayFrame.width());

		// Bounded to keep the memory used by each preview reasonable
		final int boxWidth = Math.min(1080, Math.max(720, windowWidth));

		final RedditParsedPost.ImagePreviewDetails preview
				= post.src.getPreview(boxWidth, 0);

		// A preview is displayed at the width of the post, which is normally the width of
		// the window, so scaling the height limit by the same factor as the width gives the
		// height to decode within. This is only an estimate of the size the image will be
		// displayed at, as the list is not necessarily as large as the window: the post is
		// narrower in the two pane tablet layout, and shorter wherever there is a toolbar.
		// Both make this an overestimate, which costs a little memory but never quality.
		final int maxBoxHeight = Math.max(1, (int)(
				((long)getMaxPreviewHeightPx(windowVisibleDisplayFrame.height()) * boxWidth)
						/ windowWidth));

		final UriString sourceUrl;
		final int imageWidthPx;
		final int imageHeightPx;
		final int boxHeight;
		final boolean isFallback;
		final boolean isVideoPoster;
		final UriString upgradeUrl;

		if(preview != null && preview.width >= 10 && preview.height >= 10) {

			sourceUrl = preview.url;
			imageWidthPx = preview.width;
			imageHeightPx = preview.height;
			isFallback = false;
			isVideoPoster = false;
			upgradeUrl = null;
			boxHeight = Math.max(1, Math.min(
					maxBoxHeight,
					(int)(((long)preview.height * boxWidth) / preview.width)));

		} else {

			// The listing JSON sometimes has no preview metadata at all. Prefer the small
			// thumbnail as an instant placeholder, upgrading to the full-size image once
			// it arrives; the holder's provisional square ratio is corrected when each
			// bitmap's real size is known.
			final UriString thumb = post.src.getThumbnailUrl();
			final UriString postUrl = post.src.getUrl();
			final boolean directImage = LinkHandler.isDirectImageUrl(postUrl);

			UriString resolved;
			UriString upgrade = null;

			if(RedditPreparedPost.isUsableThumbnailUrl(thumb) && directImage) {
				resolved = thumb;
				upgrade = postUrl;
			} else {
				resolved = RedditPreparedPost.resolveInlinePreviewSource(
						null,
						thumb,
						postUrl,
						directImage);
			}

			boolean videoPoster = false;

			if(resolved == null
					&& post.isVideoPreview()
					&& postUrl != null) {
				resolved = postUrl;
				videoPoster = true;
			}

			if(resolved == null) {
				return null;
			}

			sourceUrl = resolved;
			upgradeUrl = upgrade;
			isVideoPoster = videoPoster;
			isFallback = !videoPoster && postUrl != null && resolved.equals(postUrl);
			imageWidthPx = boxWidth;
			imageHeightPx = boxWidth;
			boxHeight = Math.max(1, Math.min(maxBoxHeight, boxWidth));
		}

		return new PreviewDetails(
				sourceUrl,
				imageWidthPx,
				imageHeightPx,
				boxWidth,
				boxHeight,
				isFallback,
				isVideoPoster,
				upgradeUrl);
	}

	private static final long LOAD_TIMEOUT_MS = 20_000;

	@NonNull private final BaseActivity mActivity;
	@NonNull private final RedditPreparedPost mPost;

	@Nullable private Listener mListener;

	private boolean mActive;

	@NonNull private State mState = State.NOT_LOADED;
	@Nullable private Bitmap mBitmap;
	@Nullable private RRError mError;

	@Nullable private CacheRequest mRequest;

	@Nullable private Runnable mTimeoutRunnable;

	@Nullable private UriString mPendingUpgradeUrl;

	// Incremented whenever the current load is superseded or released, so that results
	// arriving later from a background thread can be discarded
	private int mGeneration;

	// The size of the display box which the current bitmap, or in-flight request, is for
	private int mLoadedForWidthPx;
	private int mLoadedForHeightPx;

	InlinePreviewLoader(
			@NonNull final BaseActivity activity,
			@NonNull final RedditPreparedPost post) {

		mActivity = activity;
		mPost = post;
	}

	@NonNull
	public State getState() {
		return mState;
	}

	@Nullable
	public Bitmap getBitmap() {
		return mBitmap;
	}

	@Nullable
	public RRError getError() {
		return mError;
	}

	/**
	 * Called when the post enters or leaves the preload window.
	 */
	@UiThread
	public void setActive(final boolean active) {
		General.checkThisIsUIThread();
		mActive = active;
		update();
	}

	/**
	 * Attaches the view which is currently displaying this post, which will be notified of
	 * the current state before this method returns, and again whenever it changes. Pass
	 * null when the view is recycled.
	 */
	@UiThread
	public void setListener(@Nullable final Listener listener) {

		General.checkThisIsUIThread();

		mListener = listener;

		update();

		// Deliver the current state, in case the call above didn't change it
		notifyListener();
	}

	private void update() {

		// A preview is held in memory while the post is either near the visible area of the
		// list, or is actually bound to a view. The latter can happen first, during a fast
		// scroll, as the preload window is only recalculated once per frame.
		if(!mActive && mListener == null) {
			release();
			return;
		}

		final PreviewDetails details = calculatePreviewDetails(mActivity, mPost);

		if(details == null) {
			return;
		}

		if(mState != State.NOT_LOADED
				&& mLoadedForWidthPx >= details.boxWidthPx
				&& mLoadedForHeightPx >= details.boxHeightPx) {

			// Already loading, or loaded at a sufficient resolution. A larger box than
			// before means the screen has been rotated, and we need a sharper image.
			return;
		}

		startLoad(details);
	}

	private void release() {

		cancelTimeout();

		mPendingUpgradeUrl = null;

		// Discard any result which is still on its way from a background thread
		mGeneration++;

		if(mRequest != null) {
			mRequest.cancel();
			mRequest = null;
		}

		mBitmap = null;
		mError = null;
		mState = State.NOT_LOADED;
		mLoadedForWidthPx = 0;
		mLoadedForHeightPx = 0;
	}

	private void startLoad(@NonNull final PreviewDetails details) {

		if(mRequest != null) {
			mRequest.cancel();
		}

		cancelTimeout();

		final int generation = ++mGeneration;

		mLoadedForWidthPx = details.boxWidthPx;
		mLoadedForHeightPx = details.boxHeightPx;
		mPendingUpgradeUrl = details.upgradeUrl;

		// When reloading at a higher resolution, keep showing the image we already have
		// until the new one is ready, rather than flashing up a loading spinner
		if(mState != State.LOADED || mBitmap == null) {
			mBitmap = null;
			mError = null;
			mState = State.LOADING;
			notifyListener();
		}

		if(details.isVideoPoster) {

			Log.d(TAG, "MediaTrace: extracting video poster for " + details.url);

			RedditPreparedPost.VIDEO_POSTER_POOL.add(
					new PrioritisedCachedThreadPool.Task() {

						@NonNull
						@Override
						public Priority getPriority() {
							return new Priority(Constants.Priority.MEDIA_FALLBACK);
						}

						@Override
						public void run() {

							final Bitmap poster = RedditPreparedPost.extractVideoPoster(
									mActivity,
									details.url,
									details.boxWidthPx);

							AndroidCommon.runOnUiThread(() -> {

								if(poster == null) {
									onLoadFailed(generation, new RRError(
											mActivity.getString(R.string.error_connection_title),
											mActivity.getString(R.string.error_connection_message),
											false,
											new IOException("Failed to extract video poster")));
								} else {
									onLoadSucceeded(generation, poster);
								}
							});
						}
					});

			scheduleTimeout(generation, details.url);
			return;
		}

		final int previewPriority = details.isFallback
				? Constants.Priority.MEDIA_FALLBACK
				: Constants.Priority.INLINE_IMAGE_PREVIEW;

		Log.d(TAG, "MediaTrace: loading preview " + details.url);

		mRequest = new CacheRequest(
				details.url,
				RedditAccountManager.getAnon(),
				null,
				new Priority(previewPriority),
				DownloadStrategyIfNotCached.INSTANCE,
				Constants.FileType.INLINE_IMAGE_PREVIEW,
				CacheRequest.DownloadQueueType.IMMEDIATE,
				mActivity,
				new LoadCallbacks(generation, details));

		CacheManager.getInstance(mActivity).makeRequest(mRequest);

		scheduleTimeout(generation, details.url);
	}

	private void scheduleTimeout(final int generation, @NonNull final UriString url) {

		mTimeoutRunnable = () -> {

			mTimeoutRunnable = null;

			if(generation != mGeneration || mState != State.LOADING) {
				return;
			}

			Log.e(TAG, "MediaTrace: preview load timed out for " + url);

			mRequest = null;
			mBitmap = null;
			mError = new RRError(
					mActivity.getString(R.string.error_connection_title),
					mActivity.getString(R.string.error_connection_message),
					false,
					new IOException("Timed out loading inline preview"));
			mState = State.FAILED;
			notifyListener();
		};

		AndroidCommon.UI_THREAD_HANDLER.postDelayed(mTimeoutRunnable, LOAD_TIMEOUT_MS);
	}

	private void cancelTimeout() {

		if(mTimeoutRunnable != null) {
			AndroidCommon.UI_THREAD_HANDLER.removeCallbacks(mTimeoutRunnable);
			mTimeoutRunnable = null;
		}
	}

	@UiThread
	private void onLoadSucceeded(final int generation, @NonNull final Bitmap bitmap) {

		if(generation != mGeneration) {
			// Superseded, or released since this load was started
			return;
		}

		cancelTimeout();

		mRequest = null;
		mBitmap = bitmap;
		mError = null;
		mState = State.LOADED;

		notifyListener();

		if(mPendingUpgradeUrl != null) {

			final UriString upgradeUrl = mPendingUpgradeUrl;
			mPendingUpgradeUrl = null;

			startLoad(new PreviewDetails(
					upgradeUrl,
					mLoadedForWidthPx,
					mLoadedForHeightPx,
					mLoadedForWidthPx,
					mLoadedForHeightPx,
					true,
					false,
					null));
		}
	}

	@UiThread
	private void onLoadFailed(final int generation, @NonNull final RRError error) {

		if(generation != mGeneration) {
			return;
		}

		cancelTimeout();

		mRequest = null;

		if(mPendingUpgradeUrl != null) {

			final UriString upgradeUrl = mPendingUpgradeUrl;
			mPendingUpgradeUrl = null;

			startLoad(new PreviewDetails(
					upgradeUrl,
					mLoadedForWidthPx,
					mLoadedForHeightPx,
					mLoadedForWidthPx,
					mLoadedForHeightPx,
					true,
					false,
					null));

			return;
		}

		if(mState == State.LOADED && mBitmap != null) {
			// A reload at a higher resolution failed -- keep showing the lower resolution
			// image, which is much better than showing an error in its place
			return;
		}

		mBitmap = null;
		mError = error;
		mState = State.FAILED;

		notifyListener();
	}

	private void notifyListener() {

		if(mListener != null) {
			mListener.onInlinePreviewStateChanged(this);
		}
	}

	private final class LoadCallbacks implements CacheRequestCallbacks {

		private final int mCallbackGeneration;
		@NonNull private final PreviewDetails mDetails;

		private LoadCallbacks(
				final int generation,
				@NonNull final PreviewDetails details) {

			mCallbackGeneration = generation;
			mDetails = details;
		}

		@Override
		public void onDataStreamComplete(
				@NonNull final GenericFactory<SeekableInputStream, IOException> streamFactory,
				final TimestampUTC timestamp,
				@NonNull final UUID session,
				final boolean fromCache,
				@Nullable final String mimetype) {

			final Bitmap bitmap;

			try(SeekableInputStream is = streamFactory.create()) {

				bitmap = ScaledBitmapDecoder.decodeToFitWithin(
						is,
						mDetails.boxWidthPx,
						mDetails.boxHeightPx);

			} catch(final Throwable t) {

				onFailure(General.getGeneralErrorForFailure(
						mActivity,
						CacheRequest.RequestFailureType.CONNECTION,
						t,
						null,
						mDetails.url,
						Optional.empty()));

				return;
			}

			AndroidCommon.runOnUiThread(
					() -> onLoadSucceeded(mCallbackGeneration, bitmap));
		}

		@Override
		public void onFailure(@NonNull final RRError error) {

			Log.e(TAG, "Failed to download image preview: " + error, error.t);

			AndroidCommon.runOnUiThread(() -> onLoadFailed(mCallbackGeneration, error));
		}
	}
}
