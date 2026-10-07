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

package org.quantumbadger.redreader.adapters;

import android.content.Context;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.views.LoadingSpinnerView;
import org.quantumbadger.redreader.views.RedditPostHeaderView;
import org.quantumbadger.redreader.views.liststatus.ErrorView;

import java.util.Collection;

public abstract class RedditListingManager {

	private final GroupedRecyclerViewAdapter mAdapter = new GroupedRecyclerViewAdapter(7);
	private RecyclerView.LayoutManager mLayoutManager;

	private static final int GROUP_HEADER = 0;
	private static final int GROUP_NOTIFICATIONS = 1;
	private static final int GROUP_POST_SELFTEXT = 2;
	private static final int GROUP_ITEMS = 3;
	private static final int GROUP_LOAD_MORE_BUTTON = 4;
	private static final int GROUP_LOADING = 5;
	private static final int GROUP_FOOTER_ERRORS = 6;

	private final GroupedRecyclerViewItemFrameLayout mLoadingItem;
	private boolean mWorkaroundDone = false;

	protected RedditListingManager(final Context context) {
		General.checkThisIsUIThread();
		final LoadingSpinnerView loadingSpinnerView = new LoadingSpinnerView(context);
		final int paddingPx = General.dpToPixels(context, 30);
		loadingSpinnerView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

		mLoadingItem = new GroupedRecyclerViewItemFrameLayout(loadingSpinnerView);
		mAdapter.appendToGroup(GROUP_LOADING, mLoadingItem);
	}

	public void setLayoutManager(final RecyclerView.LayoutManager layoutManager) {
		General.checkThisIsUIThread();
		mLayoutManager = layoutManager;

		// In grid mode the adapter marks chrome items (headers, load-more button,
		// loading spinner, footer errors) as spanning the full width; in list mode
		// this is a no-op because those layout params are never used.
		mAdapter.setFullSpanChecker(this::isGridFullSpanItem);
	}

	// Workaround for RecyclerView scrolling behaviour
	private void doWorkaround() {
		if(!mWorkaroundDone && mLayoutManager != null) {
			if(mLayoutManager instanceof LinearLayoutManager) {
				((LinearLayoutManager)mLayoutManager).scrollToPositionWithOffset(0, 0);
			} else {
				mLayoutManager.scrollToPosition(0);
			}
			mWorkaroundDone = true;
		}
	}

	public void addFooterError(final ErrorView view) {
		General.checkThisIsUIThread();
		mAdapter.appendToGroup(
				GROUP_FOOTER_ERRORS,
				new GroupedRecyclerViewItemFrameLayout(view));
	}

	public void addPostHeader(final RedditPostHeaderView view) {
		General.checkThisIsUIThread();
		mAdapter.appendToGroup(
				GROUP_HEADER,
				new GroupedRecyclerViewItemFrameLayout(view));
		doWorkaround();
	}

	public void addPostListingHeader(final View view) {
		General.checkThisIsUIThread();
		mAdapter.appendToGroup(
				GROUP_HEADER,
				new GroupedRecyclerViewItemFrameLayout(view));
		doWorkaround();
	}

	public void addPostSelfText(final View view) {
		General.checkThisIsUIThread();
		mAdapter.appendToGroup(
				GROUP_POST_SELFTEXT,
				new GroupedRecyclerViewItemFrameLayout(view));
		doWorkaround();
	}

	public void addNotification(final View view) {
		General.checkThisIsUIThread();
		mAdapter.appendToGroup(
				GROUP_NOTIFICATIONS,
				new GroupedRecyclerViewItemFrameLayout(view));
		doWorkaround();
	}

	public void addItems(final Collection<GroupedRecyclerViewAdapter.Item<?>> items) {
		General.checkThisIsUIThread();
		mAdapter.appendToGroup(GROUP_ITEMS, items);
		doWorkaround();
	}

	public void addViewToItems(final View view) {
		General.checkThisIsUIThread();
		mAdapter.appendToGroup(GROUP_ITEMS, new GroupedRecyclerViewItemFrameLayout(view));
		doWorkaround();
	}

	public void addLoadMoreButton(final View view) {
		General.checkThisIsUIThread();
		mAdapter.appendToGroup(
				GROUP_LOAD_MORE_BUTTON,
				new GroupedRecyclerViewItemFrameLayout(view));
		doWorkaround();
	}

	public void removeLoadMoreButton() {
		General.checkThisIsUIThread();
		mAdapter.removeAllFromGroup(GROUP_LOAD_MORE_BUTTON);
	}

	public void setLoadingVisible(final boolean visible) {
		General.checkThisIsUIThread();
		mLoadingItem.setHidden(!visible);
		mAdapter.updateHiddenStatus();
	}

	/**
	 * Recalculates which items are at, or near, the visible area of the list, so that they
	 * can preload whatever they need in order to be displayed. Should be called whenever
	 * the list is scrolled, or items are added to it.
	 */
	public void updatePreloadWindow() {

		General.checkThisIsUIThread();

		if(mLayoutManager == null) {
			return;
		}

		int firstVisible;
		int lastVisible;

		if(mLayoutManager instanceof StaggeredGridLayoutManager) {

			final int[] firstPositions = ((StaggeredGridLayoutManager)mLayoutManager)
					.findFirstVisibleItemPositions(null);
			final int[] lastPositions = ((StaggeredGridLayoutManager)mLayoutManager)
					.findLastVisibleItemPositions(null);

			firstVisible = firstPositions == null || firstPositions.length == 0
					? RecyclerView.NO_POSITION : firstPositions[0];
			lastVisible = lastPositions == null || lastPositions.length == 0
					? RecyclerView.NO_POSITION : lastPositions[0];

		} else if(mLayoutManager instanceof LinearLayoutManager) {

			firstVisible = ((LinearLayoutManager)mLayoutManager).findFirstVisibleItemPosition();
			lastVisible = ((LinearLayoutManager)mLayoutManager).findLastVisibleItemPosition();

		} else {
			firstVisible = RecyclerView.NO_POSITION;
			lastVisible = RecyclerView.NO_POSITION;
		}

		mAdapter.setPreloadWindow(firstVisible, lastVisible);
	}

	/**
	 * As {@link #updatePreloadWindow()}, except that every item in the window is notified,
	 * rather than only those which have entered or left it. Should be called when the size
	 * of the list changes, for example due to the screen being rotated.
	 */
	public void refreshPreloadWindow() {
		General.checkThisIsUIThread();
		updatePreloadWindow();
		mAdapter.refreshPreloadWindow();
	}

	public void clearPreloadWindow() {
		General.checkThisIsUIThread();
		mAdapter.setPreloadWindow(-1, -1);
	}

	public GroupedRecyclerViewAdapter getAdapter() {
		General.checkThisIsUIThread();
		return mAdapter;
	}

	public void updateHiddenStatus() {
		General.checkThisIsUIThread();
		mAdapter.updateHiddenStatus();
	}

	public GroupedRecyclerViewAdapter.Item getItemAtPosition(final int position) {
		return mAdapter.getItemAtPosition(position);
	}

	// Used for grid layouts: only items in GROUP_ITEMS (i.e. the posts themselves)
	// occupy a single grid cell; headers, notifications, the load-more button,
	// the loading spinner and footer errors span the full width.
	public boolean isGridPostItem(final int position) {
		return mAdapter.getGroupIdAtPosition(position) == GROUP_ITEMS;
	}

	public boolean isGridFullSpanItem(final int position) {

		// Defensive: the layout manager can query positions that are temporarily
		// out of range while the list is being updated
		if(position >= mAdapter.getItemCount()) {
			return true;
		}

		return !isGridPostItem(position);
	}
}
