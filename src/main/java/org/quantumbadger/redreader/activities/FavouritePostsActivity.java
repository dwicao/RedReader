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
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.reddit.kthings.RedditPost;
import org.quantumbadger.redreader.reddit.prepared.FavouritePostStore;
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FavouritePostsActivity extends ViewsBaseActivity {

	private TextView mEmptyView;
	private RecyclerView mRecyclerView;

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
		mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

		mRecyclerView.setAdapter(new FavouriteAdapter(new ArrayList<>()));

		root.addView(mEmptyView);
		root.addView(mRecyclerView);

		setBaseActivityListing(root);
	}

	private class FavouriteAdapter extends RecyclerView.Adapter<FavouriteViewHolder> {

		private final List<RedditParsedPost> posts;

		FavouriteAdapter(final List<RedditParsedPost> posts) {
			this.posts = posts;
		}

		@NonNull
		@Override
		public FavouriteViewHolder onCreateViewHolder(
					@NonNull final ViewGroup parent,
					final int viewType) {

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

				layout.addView(titleView);
				layout.addView(subtitleView);

				return new FavouriteViewHolder(layout, titleView, subtitleView);
			}

			@Override
			public void onBindViewHolder(
					@NonNull final FavouriteViewHolder holder,
					final int position) {

				final RedditParsedPost post = posts.get(position);

				holder.title.setText(post.getTitle());

				final String author = post.getAuthor() == null
						? "-"
						: post.getAuthor();
				holder.subtitle.setText(String.format(
						Locale.US,
						"r/%s \u2022 u/%s \u2022 %d points \u2022 %d comments",
						post.getSubreddit(),
						author,
						post.getScoreExcludingOwnVote(),
						post.getCommentCount()));

				holder.itemView.setOnClickListener(view -> LinkHandler.onLinkClicked(
						FavouritePostsActivity.this,
						PostCommentListingURL.forPostId(
								post.getIdAndType().toString()).toUriString()));

				holder.itemView.setOnLongClickListener(view -> {
					FavouritePostStore.INSTANCE.setFavourite(
							FavouritePostsActivity.this,
							post.getSrc(),
							false);
					General.quickToast(
							FavouritePostsActivity.this,
							R.string.favourite_removed);
					reloadPosts();
					return true;
				});
			}

			@Override
			public int getItemCount() {
				return posts.size();
			}
	}

	@Override
	protected void onResume() {
		super.onResume();
		reloadPosts();
	}

	private void reloadPosts() {

		final List<RedditParsedPost> posts = new ArrayList<>();
		for(final RedditPost raw : FavouritePostStore.INSTANCE.getFavourites(this)) {
			posts.add(new RedditParsedPost(this, raw, false));
		}

		mRecyclerView.setAdapter(new FavouriteAdapter(posts));

		mEmptyView.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
		mRecyclerView.setVisibility(posts.isEmpty() ? View.GONE : View.VISIBLE);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return false;
	}

	private static class FavouriteViewHolder extends RecyclerView.ViewHolder {

		private final TextView title;
		private final TextView subtitle;

		FavouriteViewHolder(
				final View itemView,
				final TextView title,
				final TextView subtitle) {
			super(itemView);
			this.title = title;
			this.subtitle = subtitle;
		}
	}
}
