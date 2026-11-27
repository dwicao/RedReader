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

package org.quantumbadger.redreader.activities;

import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.DisplayUtils;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.RedReaderPicasso;
import org.quantumbadger.redreader.common.StringUtils;
import org.quantumbadger.redreader.common.UriString;
import org.quantumbadger.redreader.common.time.TimestampUTC;
import org.quantumbadger.redreader.reddit.DomainPostFilter;
import org.quantumbadger.redreader.reddit.KeywordPostFilter;
import org.quantumbadger.redreader.reddit.PostFilter;
import org.quantumbadger.redreader.reddit.api.RedditPostActions;
import org.quantumbadger.redreader.reddit.kthings.RedditPost;
import org.quantumbadger.redreader.reddit.prepared.FavouritePostStore;
import org.quantumbadger.redreader.reddit.prepared.FavouriteViewMode;
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FavouritePostsActivity extends ViewsBaseActivity {

	private static final int VIEW_TYPE_LIST = 0;
	private static final int VIEW_TYPE_THUMB = 1;
	private static final int VIEW_TYPE_GRID = 2;

	private TextView mEmptyView;
	private RecyclerView mRecyclerView;
	private MenuItem mViewModeItem;
	private FavouriteAdapter mAdapter;

	@Override
	protected boolean baseActivityContentExtendsBehindNavigationBar() {
		return true;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {

		PrefsUtility.applyTheme(this);

		super.onCreate(savedInstanceState);

		final TypedArray typedArray = obtainStyledAttributes(new int[] {
				R.attr.rrListBackgroundCol,
				R.attr.rrMainTextCol
		});
		final int mainTextCol;
		try {
			getWindow().setBackgroundDrawable(
					new ColorDrawable(typedArray.getColor(0, 0)));
			mainTextCol = typedArray.getColor(1, 0);
		} finally {
			typedArray.recycle();
		}

		setTitle(R.string.favourites_title);

		final FrameLayout root = new FrameLayout(this);

		mEmptyView = new TextView(this);
		mEmptyView.setText(R.string.favourites_empty);
		mEmptyView.setTextColor(mainTextCol);
		mEmptyView.setGravity(Gravity.CENTER);
		final int padding = General.dpToPixels(this, 24);
		mEmptyView.setPadding(padding, padding, padding, padding);

		mRecyclerView = new RecyclerView(this);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		mRecyclerView.setClipToPadding(false);
		General.applySystemBarPadding(mRecyclerView);

		root.addView(
				mEmptyView,
				new FrameLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT));
		root.addView(
				mRecyclerView,
				new FrameLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT));

		setBaseActivityListing(root);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {

		final MenuItem filterItem = menu.add(
				Menu.NONE,
				Menu.NONE,
				Menu.NONE,
				R.string.favourite_filter_title);
		filterItem.setOnMenuItemClickListener(item -> {
			showFilterDialog();
			return true;
		});
		filterItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		filterItem.setIcon(R.drawable.ic_filter_dark);

		mViewModeItem = menu.add(
				Menu.NONE,
				Menu.NONE,
				Menu.NONE,
				R.string.favourite_view_mode_title);
		mViewModeItem.setOnMenuItemClickListener(item -> {
			showViewModeDialog();
			return true;
		});
		mViewModeItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		updateViewModeIcon();

		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		reloadPosts();
	}

	private FavouriteViewMode currentViewMode() {
		return FavouriteViewMode.Companion.fromString(
				PrefsUtility.favourite_posts_view_mode());
	}

	private PostFilter currentFilter() {
		try {
			return PostFilter.valueOf(StringUtils.asciiUppercase(
					PrefsUtility.favourite_posts_filter()));
		} catch(final IllegalArgumentException e) {
			return PostFilter.ALL;
		}
	}

	private void updateViewModeIcon() {
		if(mViewModeItem == null) {
			return;
		}
		if(currentViewMode().getColumns() > 1) {
			mViewModeItem.setIcon(R.drawable.view_grid_dark);
		} else {
			mViewModeItem.setIcon(R.drawable.view_list_dark);
		}
	}

	private void showViewModeDialog() {

		final FavouriteViewMode[] modes = FavouriteViewMode.values();
		final CharSequence[] names = new CharSequence[modes.length];
		for(int i = 0; i < modes.length; i++) {
			final FavouriteViewMode mode = modes[i];
			if(mode == FavouriteViewMode.LIST) {
				names[i] = getString(R.string.favourite_view_list);
			} else if(mode == FavouriteViewMode.LIST_THUMBNAILS) {
				names[i] = getString(R.string.favourite_view_list_thumbnails);
			} else {
				names[i] = getResources().getQuantityString(
						R.plurals.favourite_view_grid,
						mode.getColumns(),
						mode.getColumns());
			}
		}

		new MaterialAlertDialogBuilder(this)
				.setTitle(R.string.favourite_view_mode_title)
				.setItems(names, (dialog, which) -> {
					General.getSharedPrefs(this)
							.edit()
							.putString(
									getString(R.string.pref_favourite_view_mode_key),
									modes[which].getStringValue())
							.apply();
					updateViewModeIcon();
					reloadPosts();
				})
				.show();
	}

	private void showFilterDialog() {

		final PostFilter[] filters = PostFilter.values();
		final CharSequence[] names = new CharSequence[filters.length];
		for(int i = 0; i < filters.length; i++) {
			names[i] = getString(filters[i].getMenuTitle());
		}

		new MaterialAlertDialogBuilder(this)
				.setTitle(R.string.favourite_filter_title)
				.setItems(names, (dialog, which) -> {
					General.getSharedPrefs(this)
							.edit()
							.putString(
									getString(R.string.pref_favourite_filter_key),
									filters[which].name().toLowerCase(Locale.US))
							.apply();
					reloadPosts();
				})
				.show();
	}

	private void showRemoveConfirmDialog(
			final RedditParsedPost post,
			final int adapterPosition) {
		new MaterialAlertDialogBuilder(this)
				.setTitle(R.string.favourite_remove_confirm_title)
				.setMessage(getString(
						R.string.favourite_remove_confirm_message,
						post.getTitle()))
				.setPositiveButton(R.string.action_delete, (dialog, which) -> {
					FavouritePostStore.INSTANCE.setFavourite(
							this,
							post.getSrc(),
							false);
					General.quickToast(
							this,
							R.string.favourite_removed);
					if(mAdapter != null && adapterPosition != RecyclerView.NO_POSITION) {
						mAdapter.removeAt(adapterPosition);
						updateEmptyVisibility(mAdapter.getItemCount() == 0);
					} else {
						reloadPosts();
					}
				})
				.setNegativeButton(R.string.dialog_cancel, null)
				.show();
	}

	private void updateEmptyVisibility(final boolean isEmpty) {
		mEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
		mRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
	}

	private void reloadPosts() {

		final FavouriteViewMode mode = currentViewMode();
		final PostFilter filter = currentFilter();
		final FavouritePostsActivity activity = this;

		new Thread(() -> {

			final CacheManager cm = CacheManager.getInstance(activity);

			final List<RedditPreparedPost> posts = new ArrayList<>();
			int listId = 0;
			for(final RedditPost raw
					: FavouritePostStore.INSTANCE.getFavourites(activity)) {

				if(!filter.matches(raw)) {
					continue;
				}

				if(KeywordPostFilter.matches(raw) || DomainPostFilter.matches(raw)) {
					continue;
				}

				posts.add(new RedditPreparedPost(
						activity,
						cm,
						listId++,
						new RedditParsedPost(
								activity,
								raw,
								false),
						TimestampUTC.now(),
						true,
						false,
						false,
						false));
			}

			runOnUiThread(() -> {
				if(isFinishing()) {
					return;
				}

				applyLayoutManager(mode);
				mAdapter = new FavouriteAdapter(posts, mode);
				mRecyclerView.setAdapter(mAdapter);
				updateEmptyVisibility(posts.isEmpty());
			});
		}).start();
	}

	private void applyLayoutManager(@NonNull final FavouriteViewMode mode) {
		final int columns = mode.getColumns();
		final RecyclerView.LayoutManager current = mRecyclerView.getLayoutManager();
		if(columns > 1) {
			if(current instanceof StaggeredGridLayoutManager) {
				final StaggeredGridLayoutManager staggered
						= (StaggeredGridLayoutManager)current;
				if(staggered.getSpanCount() != columns) {
					staggered.setSpanCount(columns);
				}
			} else {
				mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(
						columns,
						StaggeredGridLayoutManager.VERTICAL));
			}
		} else if(!(current instanceof LinearLayoutManager)) {
			mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		}
	}

	private void openPost(final RedditParsedPost post) {
		LinkHandler.onLinkClicked(
				this,
				PostCommentListingURL.forPostId(
						post.getIdAndType().toString(),
						PostCommentListingURL.suggestedOrderFor(post)).toUriString());
	}

	private void toggleUpvote(
			final RedditPreparedPost post,
			final int adapterPosition) {
		if(post.isUpvoted()) {
			RedditPostActions.INSTANCE.onActionMenuItemSelected(
					post,
					this,
					RedditPostActions.Action.UNVOTE);
		} else {
			RedditPostActions.INSTANCE.onActionMenuItemSelected(
					post,
					this,
					RedditPostActions.Action.UPVOTE);
		}
		if(mAdapter != null && adapterPosition != RecyclerView.NO_POSITION) {
			mAdapter.notifyItemChanged(adapterPosition);
		}
	}

	private int getScreenWidthPx() {
		final int frameWidth = DisplayUtils.getWindowVisibleDisplayFrame(this).width();
		if(frameWidth > 0) {
			return frameWidth;
		}
		return getResources().getDisplayMetrics().widthPixels;
	}

	private class FavouriteAdapter extends RecyclerView.Adapter<FavouriteViewHolder> {

		private final List<RedditPreparedPost> posts;
		private final FavouriteViewMode mode;
		private final int[] subtitleThemeColors;
		private final int titleColor;
		private final int dividerColor;
		private final int upvoteColor;
		private final int buttonTextColor;

		FavouriteAdapter(
				final List<RedditPreparedPost> posts,
				final FavouriteViewMode mode) {
			this.posts = posts;
			this.mode = mode;

			final TypedArray attr = obtainStyledAttributes(new int[] {
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
					R.attr.rrLockedpostBackCol,
					R.attr.rrPostTitleCol,
					R.attr.rrListDividerCol,
					R.attr.rrPostCommentsButtonTextCol
			});
			subtitleThemeColors = new int[] {
					attr.getColor(0, 255),
					attr.getColor(1, 255),
					attr.getColor(2, 255),
					attr.getColor(3, 255),
					attr.getColor(4, 255),
					attr.getColor(5, 255),
					attr.getColor(6, 255),
					attr.getColor(7, 255),
					attr.getColor(8, 255),
					attr.getColor(9, 255),
					attr.getColor(10, 255)
			};
			upvoteColor = subtitleThemeColors[1];
			titleColor = attr.getColor(11, 0);
			dividerColor = attr.getColor(12, 0);
			buttonTextColor = attr.getColor(13, 0);
			attr.recycle();
		}

		void removeAt(final int position) {
			if(position >= 0 && position < posts.size()) {
				posts.remove(position);
				notifyItemRemoved(position);
			}
		}

		@Override
		public int getItemViewType(final int position) {
			if(mode == FavouriteViewMode.LIST) {
				return VIEW_TYPE_LIST;
			} else if(mode == FavouriteViewMode.LIST_THUMBNAILS) {
				return VIEW_TYPE_THUMB;
			} else {
				return VIEW_TYPE_GRID;
			}
		}

		@NonNull
		@Override
		public FavouriteViewHolder onCreateViewHolder(
				@NonNull final ViewGroup parent,
				final int viewType) {

			if(viewType == VIEW_TYPE_GRID) {
				return makeGridHolder(parent);
			} else if(viewType == VIEW_TYPE_THUMB) {
				return makeThumbHolder();
			} else {
				return makeListHolder();
			}
		}

		@Override
		public void onBindViewHolder(
				@NonNull final FavouriteViewHolder holder,
				final int position) {

			final RedditPreparedPost prepared = posts.get(position);
			final RedditParsedPost post = prepared.src;

			cancelImageLoads(holder);
			final int bindUsageId = ++holder.usageId;

			holder.title.setText(post.getTitle());
			holder.subtitle.setText(prepared.buildSubtitle(
					FavouritePostsActivity.this,
					false,
					false,
					subtitleThemeColors));
			holder.subtitle.setMaxLines(mode == FavouriteViewMode.LIST ? 5 : 3);
			holder.subtitle.setEllipsize(TextUtils.TruncateAt.END);

			if(holder.getItemViewType() == VIEW_TYPE_GRID) {
				bindGridImage(holder, prepared, bindUsageId);
				bindGridOverlayBadges(holder, prepared);
			} else if(holder.image != null) {
				bindListThumbnail(holder, post);
			}

			holder.voteText.setText(FavouritePostStore.INSTANCE.formatScoreCompact(
					prepared.computeScore()));
			if(prepared.isUpvoted()) {
				holder.voteIcon.setColorFilter(upvoteColor, PorterDuff.Mode.SRC_IN);
				holder.voteText.setTextColor(upvoteColor);
			} else {
				holder.voteIcon.clearColorFilter();
				holder.voteText.setTextColor(buttonTextColor);
			}
			holder.voteButton.setContentDescription(getString(
					prepared.isUpvoted()
							? R.string.action_vote_remove
							: R.string.action_upvote));
			holder.voteButton.setOnClickListener(view -> toggleUpvote(
					prepared,
					holder.getBindingAdapterPosition()));

			final int commentCount = post.getCommentCount();
			holder.commentsText.setText(String.valueOf(commentCount));
			holder.commentsButton.setContentDescription(getString(
					R.string.action_comments_with_count,
					commentCount));
			holder.commentsButton.setOnClickListener(view -> openPost(post));

			holder.itemView.setOnClickListener(view -> openPost(post));

			AndroidCommon.setExtendedLongClickListener(holder.itemView, view -> {
				showRemoveConfirmDialog(post, holder.getBindingAdapterPosition());
				return true;
			});
		}

		@Override
		public int getItemCount() {
			return posts.size();
		}

		@Override
		public void onViewRecycled(@NonNull final FavouriteViewHolder holder) {
			holder.usageId++;
			cancelImageLoads(holder);
			super.onViewRecycled(holder);
		}

		private void cancelImageLoads(@NonNull final FavouriteViewHolder holder) {
			final Picasso picasso = RedReaderPicasso.INSTANCE.get(
					FavouritePostsActivity.this);
			if(holder.image != null) {
				picasso.cancelRequest(holder.image);
				holder.image.setImageDrawable(null);
			}
			if(holder.activeTarget != null) {
				picasso.cancelRequest(holder.activeTarget);
				holder.activeTarget = null;
			}
		}

		private void bindGridOverlayBadges(
				@NonNull final FavouriteViewHolder holder,
				@NonNull final RedditPreparedPost prepared) {
			if(holder.imageCount != null) {
				final int count = prepared.src.getGalleryImageCount();
				if(count > 1) {
					//noinspection SetTextI18n
					holder.imageCount.setText("[1/" + count + "]");
					holder.imageCount.setVisibility(View.VISIBLE);
				} else {
					holder.imageCount.setVisibility(View.GONE);
				}
			}
			if(holder.playOverlay != null) {
				holder.playOverlay.setVisibility(View.GONE);
			}
		}

		private void bindListThumbnail(
				@NonNull final FavouriteViewHolder holder,
				@NonNull final RedditParsedPost post) {
			final int thumbSize = General.dpToPixels(
					FavouritePostsActivity.this, 72);
			final UriString source = resolveBestImageUrl(post, thumbSize * 2);
			if(source != null) {
				holder.image.setVisibility(View.VISIBLE);
				RedReaderPicasso.INSTANCE.get(FavouritePostsActivity.this)
						.load(source.value)
						.resize(thumbSize, thumbSize)
						.centerCrop()
						.onlyScaleDown()
						.into(holder.image);
			} else {
				holder.image.setVisibility(View.GONE);
			}
		}

		private void bindGridImage(
				@NonNull final FavouriteViewHolder holder,
				@NonNull final RedditPreparedPost prepared,
				final int usageId) {

			final RedditParsedPost post = prepared.src;
			final int screenWidth = getScreenWidthPx();
			final int previewWidth = Math.min(1080, Math.max(640, screenWidth));
			final int cardWidth = Math.max(
					General.dpToPixels(FavouritePostsActivity.this, 180),
					screenWidth / Math.max(1, mode.getColumns()));

			final RedditParsedPost.ImagePreviewDetails preview
					= findPreviewDetails(post, previewWidth);

			final boolean isVideo = prepared.isVideoPreview()
					|| post.getYoutubeId() != null;

			if(preview != null && preview.width >= 10 && preview.height >= 10) {
				showGridPreview(holder, prepared, preview, cardWidth, isVideo, usageId);
				return;
			}

			if(post.getYoutubeId() != null) {
				loadYoutubeGridImage(
						holder,
						post,
						post.getYoutubeId(),
						cardWidth,
						usageId);
				return;
			}

			loadGridFallbackUrl(holder, prepared, cardWidth, isVideo, usageId);
		}

		private void showGridPreview(
				@NonNull final FavouriteViewHolder holder,
				@NonNull final RedditPreparedPost prepared,
				@NonNull final RedditParsedPost.ImagePreviewDetails preview,
				final int cardWidth,
				final boolean isVideo,
				final int usageId) {

			holder.imageArea.setVisibility(View.VISIBLE);
			holder.image.setVisibility(View.VISIBLE);
			holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
			applyGridImageAspect(holder, preview.width, preview.height);

			final int targetHeight = Math.min(
					2048,
					Math.max(
							1,
							Math.round(cardWidth
									* ((float)preview.height / (float)preview.width))));

			RedReaderPicasso.INSTANCE.get(FavouritePostsActivity.this)
					.load(preview.url.value)
					.resize(cardWidth, targetHeight)
					.centerInside()
					.onlyScaleDown()
					.into(holder.image, new Callback() {
						@Override
						public void onSuccess() {
							if(holder.usageId != usageId) {
								return;
							}
							final Drawable d = holder.image.getDrawable();
							if(d != null
									&& d.getIntrinsicWidth() > 0
									&& d.getIntrinsicHeight() > 0) {
								applyGridImageAspect(
										holder,
										d.getIntrinsicWidth(),
										d.getIntrinsicHeight());
							}
							if(isVideo && holder.playOverlay != null) {
								holder.playOverlay.setVisibility(View.VISIBLE);
							}
						}

						@Override
						public void onError(final Exception e) {
							if(holder.usageId != usageId) {
								return;
							}
							loadGridFallbackUrl(
									holder,
									prepared,
									cardWidth,
									isVideo,
									usageId);
						}
					});
		}

		private void loadYoutubeGridImage(
				@NonNull final FavouriteViewHolder holder,
				@NonNull final RedditParsedPost post,
				@NonNull final String youtubeId,
				final int cardWidth,
				final int usageId) {

			holder.imageArea.setVisibility(View.VISIBLE);
			holder.image.setVisibility(View.VISIBLE);
			holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
			applyGridImageAspect(holder, 16, 9);

			final String maxRes = "https://i.ytimg.com/vi/"
					+ youtubeId
					+ "/maxresdefault.jpg";
			final String hqRes = "https://i.ytimg.com/vi/"
					+ youtubeId
					+ "/hqdefault.jpg";

			loadBitmapWithTarget(
					holder,
					maxRes,
					cardWidth,
					true,
					usageId,
					() -> loadBitmapWithTarget(
							holder,
							hqRes,
							cardWidth,
							true,
							usageId,
							() -> {
								final UriString thumb = post.getThumbnailUrl();
								if(RedditPreparedPost.isUsableThumbnailUrl(thumb)) {
									loadBitmapWithTarget(
											holder,
											thumb.value,
											cardWidth,
											true,
											usageId,
											() -> holder.imageArea.setVisibility(View.GONE));
								} else {
									holder.imageArea.setVisibility(View.GONE);
								}
							}));
		}

		private void loadGridFallbackUrl(
				@NonNull final FavouriteViewHolder holder,
				@NonNull final RedditPreparedPost prepared,
				final int cardWidth,
				final boolean isVideo,
				final int usageId) {

			final RedditParsedPost post = prepared.src;
			final UriString postUrl = post.getUrl();
			final UriString thumbUrl = post.getThumbnailUrl();
			final boolean isDirect = LinkHandler.isDirectImageUrl(postUrl);
			final boolean hasThumb = RedditPreparedPost.isUsableThumbnailUrl(thumbUrl);

			if(!isDirect && !hasThumb) {
				holder.imageArea.setVisibility(View.GONE);
				return;
			}

			holder.imageArea.setVisibility(View.VISIBLE);
			holder.image.setVisibility(View.VISIBLE);
			holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
			applyGridImageAspect(holder, 16, 9);

			if(isDirect && postUrl != null) {
				loadBitmapWithTarget(
						holder,
						postUrl.value,
						cardWidth,
						isVideo,
						usageId,
						() -> {
							if(hasThumb) {
								loadBitmapWithTarget(
										holder,
										thumbUrl.value,
										cardWidth,
										isVideo,
										usageId,
										() -> holder.imageArea.setVisibility(View.GONE));
							} else {
								holder.imageArea.setVisibility(View.GONE);
							}
						});
			} else if(hasThumb) {
				loadBitmapWithTarget(
						holder,
						thumbUrl.value,
						cardWidth,
						isVideo,
						usageId,
						() -> holder.imageArea.setVisibility(View.GONE));
			}
		}

		private void loadBitmapWithTarget(
				@NonNull final FavouriteViewHolder holder,
				@NonNull final String url,
				final int targetSize,
				final boolean showPlay,
				final int usageId,
				@NonNull final Runnable onFallback) {

			final int decodeMax = Math.max(640, targetSize);
			final Target target = new Target() {
				@Override
				public void onBitmapLoaded(
						@NonNull final Bitmap bitmap,
						@NonNull final Picasso.LoadedFrom from) {
					if(holder.usageId != usageId) {
						return;
					}
					holder.activeTarget = null;
					if(bitmap.getWidth() < 10 || bitmap.getHeight() < 10) {
						onFallback.run();
						return;
					}
					applyGridImageAspect(
							holder,
							bitmap.getWidth(),
							bitmap.getHeight());
					holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
					holder.image.setImageBitmap(bitmap);
					if(showPlay && holder.playOverlay != null) {
						holder.playOverlay.setVisibility(View.VISIBLE);
					}
				}

				@Override
				public void onBitmapFailed(
						@NonNull final Exception e,
						@Nullable final Drawable errorDrawable) {
					if(holder.usageId != usageId) {
						return;
					}
					holder.activeTarget = null;
					onFallback.run();
				}

				@Override
				public void onPrepareLoad(@Nullable final Drawable placeHolderDrawable) {
					// Placeholder intentionally left blank
				}
			};

			holder.activeTarget = target;
			RedReaderPicasso.INSTANCE.get(FavouritePostsActivity.this)
					.load(url)
					.resize(decodeMax, decodeMax)
					.centerInside()
					.onlyScaleDown()
					.into(target);
		}

		private void applyGridImageAspect(
				@NonNull final FavouriteViewHolder holder,
				final int widthPx,
				final int heightPx) {
			if(holder.imageHolder == null || widthPx < 1 || heightPx < 1) {
				return;
			}
			final ConstraintLayout.LayoutParams params
					= (ConstraintLayout.LayoutParams)holder.imageHolder.getLayoutParams();
			final String ratio = String.valueOf((float)widthPx / (float)heightPx);
			if(params.width == ViewGroup.LayoutParams.MATCH_PARENT
					&& params.height == 0
					&& ratio.equals(params.dimensionRatio)) {
				return;
			}
			params.width = ViewGroup.LayoutParams.MATCH_PARENT;
			params.height = 0;
			params.dimensionRatio = ratio;
			holder.imageHolder.setLayoutParams(params);
		}

		@Nullable
		private RedditParsedPost.ImagePreviewDetails findPreviewDetails(
				@NonNull final RedditParsedPost post,
				final int previewWidth) {
			final RedditParsedPost.ImagePreviewDetails own
					= post.getPreview(previewWidth, 0);
			if(own != null && own.width >= 10 && own.height >= 10) {
				return own;
			}
			final List<RedditPost> parents = post.getSrc().getCrosspost_parent_list();
			if(parents != null) {
				for(final RedditPost parent : parents) {
					final RedditParsedPost parsedParent = new RedditParsedPost(
							FavouritePostsActivity.this,
							parent,
							false);
					final RedditParsedPost.ImagePreviewDetails parentPreview
							= parsedParent.getPreview(previewWidth, 0);
					if(parentPreview != null
							&& parentPreview.width >= 10
							&& parentPreview.height >= 10) {
						return parentPreview;
					}
				}
			}
			return null;
		}

		@Nullable
		private UriString resolveBestImageUrl(
				@NonNull final RedditParsedPost post,
				final int previewWidth) {
			final RedditParsedPost.ImagePreviewDetails preview
					= findPreviewDetails(post, previewWidth);
			if(preview != null) {
				return preview.url;
			}
			if(post.getYoutubeId() != null) {
				return new UriString(
						"https://i.ytimg.com/vi/"
								+ post.getYoutubeId()
								+ "/hqdefault.jpg");
			}
			return RedditPreparedPost.resolveThumbnailSource(
					null,
					post.getThumbnailUrl(),
					post.getUrl(),
					LinkHandler.isDirectImageUrl(post.getUrl()));
		}

		private View inflateActionBar(@NonNull final ViewGroup parent) {
			return LayoutInflater.from(FavouritePostsActivity.this).inflate(
					R.layout.favourite_post_action_bar,
					parent,
					false);
		}

		private View makeDivider() {
			final View divider = new View(FavouritePostsActivity.this);
			divider.setLayoutParams(new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					General.dpToPixels(FavouritePostsActivity.this, 1)));
			divider.setBackgroundColor(dividerColor);
			return divider;
		}

		private FavouriteViewHolder makeListHolder() {

			final LinearLayout layout = new LinearLayout(FavouritePostsActivity.this);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.setBackgroundResource(R.drawable.rr_postlist_item_selector_main);
			layout.setLayoutParams(new RecyclerView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));

			final LinearLayout textBlock = new LinearLayout(FavouritePostsActivity.this);
			textBlock.setOrientation(LinearLayout.VERTICAL);
			final int hPad = General.dpToPixels(FavouritePostsActivity.this, 12);
			final int vPad = General.dpToPixels(FavouritePostsActivity.this, 10);
			textBlock.setPadding(hPad, vPad, hPad, vPad);

			final TextView titleView = new TextView(FavouritePostsActivity.this);
			titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
			titleView.setTextColor(titleColor);

			final TextView subtitleView = new TextView(FavouritePostsActivity.this);
			subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			subtitleView.setPadding(0, General.dpToPixels(FavouritePostsActivity.this, 4), 0, 0);

			textBlock.addView(titleView);
			textBlock.addView(subtitleView);

			final View actionBar = inflateActionBar(layout);
			layout.addView(textBlock);
			layout.addView(actionBar);
			layout.addView(makeDivider());

			return new FavouriteViewHolder(
					layout,
					null,
					null,
					null,
					null,
					null,
					titleView,
					subtitleView,
					actionBar);
		}

		private FavouriteViewHolder makeThumbHolder() {

			final LinearLayout layout = new LinearLayout(FavouritePostsActivity.this);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.setBackgroundResource(R.drawable.rr_postlist_item_selector_main);
			layout.setLayoutParams(new RecyclerView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));

			final LinearLayout topRow = new LinearLayout(FavouritePostsActivity.this);
			topRow.setOrientation(LinearLayout.HORIZONTAL);
			topRow.setGravity(Gravity.CENTER_VERTICAL);

			final ImageView imageView = new ImageView(FavouritePostsActivity.this);
			final int thumbSize = General.dpToPixels(FavouritePostsActivity.this, 72);
			imageView.setLayoutParams(new LinearLayout.LayoutParams(thumbSize, thumbSize));
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

			final LinearLayout texts = new LinearLayout(FavouritePostsActivity.this);
			texts.setOrientation(LinearLayout.VERTICAL);
			texts.setLayoutParams(new LinearLayout.LayoutParams(
					0,
					LinearLayout.LayoutParams.WRAP_CONTENT,
					1f));
			final int pad = General.dpToPixels(FavouritePostsActivity.this, 10);
			texts.setPadding(pad, pad, pad, pad);

			final TextView titleView = new TextView(FavouritePostsActivity.this);
			titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
			titleView.setTextColor(titleColor);
			titleView.setMaxLines(3);
			titleView.setEllipsize(TextUtils.TruncateAt.END);

			final TextView subtitleView = new TextView(FavouritePostsActivity.this);
			subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			subtitleView.setPadding(0, General.dpToPixels(FavouritePostsActivity.this, 3), 0, 0);
			subtitleView.setMaxLines(3);
			subtitleView.setEllipsize(TextUtils.TruncateAt.END);

			texts.addView(titleView);
			texts.addView(subtitleView);

			topRow.addView(imageView);
			topRow.addView(texts);

			final View actionBar = inflateActionBar(layout);
			layout.addView(topRow);
			layout.addView(actionBar);
			layout.addView(makeDivider());

			return new FavouriteViewHolder(
					layout,
					null,
					null,
					imageView,
					null,
					null,
					titleView,
					subtitleView,
					actionBar);
		}

		private FavouriteViewHolder makeGridHolder(@NonNull final ViewGroup parent) {

			final View view = LayoutInflater.from(FavouritePostsActivity.this).inflate(
					R.layout.favourite_post_grid,
					parent,
					false);

			return new FavouriteViewHolder(
					view,
					view.findViewById(R.id.fav_grid_image_area),
					view.findViewById(R.id.fav_grid_image_holder),
					view.findViewById(R.id.fav_grid_image),
					view.findViewById(R.id.fav_grid_play_overlay),
					view.findViewById(R.id.fav_grid_image_count),
					view.findViewById(R.id.fav_grid_title),
					view.findViewById(R.id.fav_grid_subtitle),
					view);
		}
	}

	static class FavouriteViewHolder extends RecyclerView.ViewHolder {

		final View imageArea;
		final FrameLayout imageHolder;
		final ImageView image;
		final View playOverlay;
		final TextView imageCount;
		final TextView title;
		final TextView subtitle;
		final View voteButton;
		final ImageView voteIcon;
		final TextView voteText;
		final View commentsButton;
		final TextView commentsText;
		int usageId;
		Target activeTarget;

		FavouriteViewHolder(
				final View itemView,
				@Nullable final View imageArea,
				@Nullable final FrameLayout imageHolder,
				@Nullable final ImageView image,
				@Nullable final View playOverlay,
				@Nullable final TextView imageCount,
				final TextView title,
				final TextView subtitle,
				final View actionContainer) {
			super(itemView);
			this.imageArea = imageArea;
			this.imageHolder = imageHolder;
			this.image = image;
			this.playOverlay = playOverlay;
			this.imageCount = imageCount;
			this.title = title;
			this.subtitle = subtitle;
			this.voteButton = actionContainer.findViewById(R.id.fav_vote_button);
			this.voteIcon = actionContainer.findViewById(R.id.fav_vote_icon);
			this.voteText = actionContainer.findViewById(R.id.fav_vote_text);
			this.commentsButton = actionContainer.findViewById(R.id.fav_comments_button);
			this.commentsText = actionContainer.findViewById(R.id.fav_comments_text);
		}
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return false;
	}
}
