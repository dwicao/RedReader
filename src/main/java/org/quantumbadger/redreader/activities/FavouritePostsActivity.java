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

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.RedReaderPicasso;
import org.quantumbadger.redreader.common.StringUtils;
import org.quantumbadger.redreader.common.time.TimestampUTC;
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

	@Override
	protected int baseActivityNavigationBarColour() {
		return Color.rgb(0x55, 0x55, 0x55);
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {

		PrefsUtility.applySettingsTheme(this);

		super.onCreate(savedInstanceState);

		setTitle(R.string.favourites_title);

		final LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);

		mEmptyView = new TextView(this);
		mEmptyView.setText(R.string.favourites_empty);
		mEmptyView.setGravity(Gravity.CENTER);
		final int padding = General.dpToPixels(this, 24);
		mEmptyView.setPadding(padding, padding, padding, padding);

		mRecyclerView = new RecyclerView(this);
		mRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));

		root.addView(mEmptyView);
		root.addView(mRecyclerView);

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

	private void showRemoveConfirmDialog(final RedditParsedPost post) {
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
					reloadPosts();
				})
				.setNegativeButton(R.string.dialog_cancel, null)
				.show();
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

				if(mRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
					((GridLayoutManager)mRecyclerView.getLayoutManager())
							.setSpanCount(mode.getColumns());
				}

				mRecyclerView.setAdapter(new FavouriteAdapter(posts, mode));

				mEmptyView.setVisibility(
						posts.isEmpty() ? View.VISIBLE : View.GONE);
				mRecyclerView.setVisibility(
						posts.isEmpty() ? View.GONE : View.VISIBLE);
			});
		}).start();
	}

	private void openPost(final RedditParsedPost post) {
		LinkHandler.onLinkClicked(
				this,
				PostCommentListingURL.forPostId(
						post.getIdAndType().toString()).toUriString());
	}

	private void toggleUpvote(final RedditPreparedPost post) {
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
		reloadPosts();
	}

	private class FavouriteAdapter extends RecyclerView.Adapter<FavouriteViewHolder> {

		private final List<RedditPreparedPost> posts;
		private final FavouriteViewMode mode;

		FavouriteAdapter(
				final List<RedditPreparedPost> posts,
				final FavouriteViewMode mode) {
			this.posts = posts;
			this.mode = mode;
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
				return makeGridHolder();
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

			holder.title.setText(post.getTitle());

			if(holder.subtitle != null) {
				holder.subtitle.setText(prepared.buildSubtitle(
						FavouritePostsActivity.this,
						false));
				holder.subtitle.setMaxLines(mode == FavouriteViewMode.LIST ? 5 : 3);
				holder.subtitle.setEllipsize(TextUtils.TruncateAt.END);
			}

			if(holder.image != null) {
				RedReaderPicasso.INSTANCE.get(FavouritePostsActivity.this)
						.cancelRequest(holder.image);
				if(post.getThumbnailUrl() != null) {
					RedReaderPicasso.INSTANCE.get(FavouritePostsActivity.this)
							.load(post.getThumbnailUrl().value)
							.fit()
							.centerCrop()
							.into(holder.image);
				} else {
					holder.image.setImageDrawable(null);
				}
			}

			if(holder.voteButton != null) {
				holder.voteButton.setText(String.format(
						Locale.US,
						"\u25B2 %s",
						FavouritePostStore.INSTANCE.formatScoreCompact(
								prepared.computeScore())));
				holder.voteButton.setContentDescription(getString(
						prepared.isUpvoted()
								? R.string.action_vote_remove
								: R.string.action_upvote));
				holder.voteButton.setOnClickListener(
						view -> toggleUpvote(prepared));
			}

			if(holder.commentsButton != null) {
				final int commentCount = post.getCommentCount();
				holder.commentsButton.setText(String.valueOf(commentCount));
				holder.commentsButton.setContentDescription(getString(
						R.string.action_comments_with_count,
						commentCount));
				holder.commentsButton.setOnClickListener(
						view -> openPost(post));
			}

			holder.itemView.setOnClickListener(
					view -> openPost(post));

			holder.itemView.setOnLongClickListener(view -> {
				showRemoveConfirmDialog(post);
				return true;
			});
		}

		@Override
		public int getItemCount() {
			return posts.size();
		}

		@Override
		public void onViewRecycled(@NonNull final FavouriteViewHolder holder) {
			if(holder.image != null) {
				RedReaderPicasso.INSTANCE.get(FavouritePostsActivity.this)
						.cancelRequest(holder.image);
			}
			super.onViewRecycled(holder);
		}

		private LinearLayout makeButtonRow(
				final Button voteButton,
				final Button commentsButton) {

			final LinearLayout buttons = new LinearLayout(
					FavouritePostsActivity.this);
			buttons.setOrientation(LinearLayout.HORIZONTAL);

			final LinearLayout.LayoutParams buttonParams
					= new LinearLayout.LayoutParams(
							0,
							LinearLayout.LayoutParams.WRAP_CONTENT,
							1f);

			buttons.addView(voteButton, buttonParams);
			buttons.addView(commentsButton, buttonParams);

			return buttons;
		}

		private FavouriteViewHolder makeListHolder() {

			final LinearLayout layout = new LinearLayout(
					FavouritePostsActivity.this);
			layout.setOrientation(LinearLayout.VERTICAL);

			final int rowPadding = General.dpToPixels(
					FavouritePostsActivity.this, 12);
			layout.setPadding(rowPadding, rowPadding, rowPadding, rowPadding);

			final TextView titleView = new TextView(
					FavouritePostsActivity.this);
			titleView.setTextSize(16);

			final TextView subtitleView = new TextView(
					FavouritePostsActivity.this);
			subtitleView.setTextSize(13);

			final Button voteButton = new Button(FavouritePostsActivity.this);
			final Button commentsButton = new Button(FavouritePostsActivity.this);

			layout.addView(titleView);
			layout.addView(subtitleView);
			layout.addView(makeButtonRow(voteButton, commentsButton));

			return new FavouriteViewHolder(
					layout,
					null,
					titleView,
					subtitleView,
					voteButton,
					commentsButton);
		}

		private FavouriteViewHolder makeThumbHolder() {

			final LinearLayout layout = new LinearLayout(
					FavouritePostsActivity.this);
			layout.setOrientation(LinearLayout.VERTICAL);

			final int rowPadding = General.dpToPixels(
					FavouritePostsActivity.this, 12);
			layout.setPadding(rowPadding, rowPadding, 0, rowPadding);

			final LinearLayout topRow = new LinearLayout(
					FavouritePostsActivity.this);
			topRow.setOrientation(LinearLayout.HORIZONTAL);
			topRow.setGravity(Gravity.CENTER_VERTICAL);
			topRow.setPadding(0, 0, rowPadding, 0);

			final ImageView imageView = new ImageView(
					FavouritePostsActivity.this);
			final int thumbSize = General.dpToPixels(
					FavouritePostsActivity.this, 72);
			imageView.setLayoutParams(new LinearLayout.LayoutParams(
					thumbSize,
					thumbSize));
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

			final LinearLayout texts = new LinearLayout(
					FavouritePostsActivity.this);
			texts.setOrientation(LinearLayout.VERTICAL);
			texts.setLayoutParams(new LinearLayout.LayoutParams(
					0,
					LinearLayout.LayoutParams.WRAP_CONTENT,
					1f));
			final int textPadding = General.dpToPixels(
					FavouritePostsActivity.this, 12);
			texts.setPadding(textPadding, 0, 0, 0);

			final TextView titleView = new TextView(
					FavouritePostsActivity.this);
			titleView.setTextSize(16);
			titleView.setMaxLines(3);
			titleView.setEllipsize(TextUtils.TruncateAt.END);

			final TextView subtitleView = new TextView(
					FavouritePostsActivity.this);
			subtitleView.setTextSize(13);
			subtitleView.setMaxLines(3);
			subtitleView.setEllipsize(TextUtils.TruncateAt.END);

			texts.addView(titleView);
			texts.addView(subtitleView);

			topRow.addView(imageView);
			topRow.addView(texts);

			final Button voteButton = new Button(FavouritePostsActivity.this);
			final Button commentsButton = new Button(FavouritePostsActivity.this);

			layout.addView(topRow);
			layout.addView(makeButtonRow(voteButton, commentsButton));

			return new FavouriteViewHolder(
					layout,
					imageView,
					titleView,
					subtitleView,
					voteButton,
					commentsButton);
		}

		private FavouriteViewHolder makeGridHolder() {

			final LinearLayout layout = new LinearLayout(
					FavouritePostsActivity.this);
			layout.setOrientation(LinearLayout.VERTICAL);

			final int cellPadding = General.dpToPixels(
					FavouritePostsActivity.this, 4);
			layout.setPadding(cellPadding, cellPadding, cellPadding, cellPadding);

			final ImageView imageView = new ImageView(
					FavouritePostsActivity.this);
			imageView.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					General.dpToPixels(FavouritePostsActivity.this, 120)));
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

			final TextView titleView = new TextView(
					FavouritePostsActivity.this);
			titleView.setTextSize(13);
			titleView.setMaxLines(2);
			titleView.setEllipsize(TextUtils.TruncateAt.END);

			final TextView subtitleView = new TextView(
					FavouritePostsActivity.this);
			subtitleView.setTextSize(12);

			final Button voteButton = new Button(FavouritePostsActivity.this);
			final Button commentsButton = new Button(FavouritePostsActivity.this);
			voteButton.setTextSize(12);
			commentsButton.setTextSize(12);

			layout.addView(imageView);
			layout.addView(titleView);
			layout.addView(subtitleView);
			layout.addView(makeButtonRow(voteButton, commentsButton));

			return new FavouriteViewHolder(
					layout,
					imageView,
					titleView,
					subtitleView,
					voteButton,
					commentsButton);
		}
	}

	private static class FavouriteViewHolder extends RecyclerView.ViewHolder {

		private final ImageView image;
		private final TextView title;
		private final TextView subtitle;
		private final Button voteButton;
		private final Button commentsButton;

		FavouriteViewHolder(
				final View itemView,
				final ImageView image,
				final TextView title,
				final TextView subtitle,
				final Button voteButton,
				final Button commentsButton) {
			super(itemView);
			this.image = image;
			this.title = title;
			this.subtitle = subtitle;
			this.voteButton = voteButton;
			this.commentsButton = commentsButton;
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
