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

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class GroupedRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final AtomicLong ITEM_UNIQUE_ID_GENERATOR = new AtomicLong(100_000);

	// Used by staggered (masonry) grid layouts to decide which items should span
	// the full width instead of occupying a single column.
	public interface FullSpanChecker {
		boolean isFullSpan(final int position);
	}

	public static abstract class Item<VH extends RecyclerView.ViewHolder> {

		private final long mUniqueId = ITEM_UNIQUE_ID_GENERATOR.incrementAndGet();
		private boolean mCurrentlyHidden = false;

		public abstract Class<?> getViewType();

		public abstract VH onCreateViewHolder(final ViewGroup viewGroup);

		public abstract void onBindViewHolder(final VH viewHolder);

		public abstract boolean isHidden();

		/**
		 * Called when this item enters or leaves the preload window -- the range of items at
		 * or near the visible area of the list. Items use this to load, and release, any
		 * expensive resources they need in order to be displayed.
		 */
		public void onPreloadWindowChanged(final boolean inWindow) {}

		private void onBindViewHolderInner(
				final RecyclerView.ViewHolder viewHolder) {
			//noinspection unchecked
			onBindViewHolder((VH)viewHolder);
		}
	}

	// The number of items to keep preloaded either side of the visible area of the list
	private static final int PRELOAD_WINDOW_EXTRA_ITEMS = 5;

	private final ArrayList<Item<?>>[] mItems;
	private final HashMap<Class<?>, Integer> mItemViewTypeMap = new HashMap<>();
	private final HashMap<Integer, Item<?>> mViewTypeItemMap = new HashMap<>();
	private FullSpanChecker mFullSpanChecker;

	private final HashSet<Item<?>> mPreloadWindow = new HashSet<>();

	private ArrayList<Item<?>> mVisibleIndex = null;
	private int[] mGroupVisibleStarts = null;

	private boolean mPreloadRangeValid = false;
	private int mLastPreloadFirst = -1;
	private int mLastPreloadLast = -1;

	public GroupedRecyclerViewAdapter(final int groups) {
		//noinspection unchecked
		mItems = (ArrayList<Item<?>>[])new ArrayList[groups];

		for(int i = 0; i < groups; i++) {
			mItems[i] = new ArrayList<>();
		}

		setHasStableIds(true);
	}

	public void setFullSpanChecker(final FullSpanChecker fullSpanChecker) {
		mFullSpanChecker = fullSpanChecker;
	}

	private int getItemPositionInternal(final int groupId, final Item<?> item) {

		final ArrayList<Item<?>> group = mItems[groupId];

		for(int i = 0; i < group.size(); i++) {
			if(group.get(i) == item) {
				return getItemPositionInternal(groupId, i);
			}
		}

		throw new RuntimeException("Item not found");
	}

	// "positionInGroup" should include both hidden and visible items
	private int getItemPositionInternal(final int group, final int positionInGroup) {

		ensureIndex();

		final int groupStart = group >= mItems.length
				? mVisibleIndex.size()
				: mGroupVisibleStarts[group];

		int visibleBefore = 0;

		if(positionInGroup > 0) {

			final ArrayList<Item<?>> itemsInGroup = mItems[group];

			for(int i = 0; i < positionInGroup; i++) {
				if(!itemsInGroup.get(i).mCurrentlyHidden) {
					visibleBefore++;
				}
			}
		}

		return groupStart + visibleBefore;
	}

	private void ensureIndex() {

		if(mVisibleIndex != null) {
			return;
		}

		mVisibleIndex = new ArrayList<>();
		mGroupVisibleStarts = new int[mItems.length];

		for(int groupId = 0; groupId < mItems.length; groupId++) {

			mGroupVisibleStarts[groupId] = mVisibleIndex.size();

			final ArrayList<Item<?>> group = mItems[groupId];

			for(int positionInGroup = 0; positionInGroup < group.size(); positionInGroup++) {
				final Item<?> item = group.get(positionInGroup);
				if(!item.mCurrentlyHidden) {
					mVisibleIndex.add(item);
				}
			}
		}
	}

	private void invalidateIndex() {
		mVisibleIndex = null;
		mGroupVisibleStarts = null;
		mPreloadRangeValid = false;
	}

	private Item<?> getItemInternal(final int desiredPosition) {

		ensureIndex();

		if(desiredPosition < 0) {
			throw new RuntimeException("Item desiredPosition "
					+ desiredPosition
					+ " is too low");
		}

		if(desiredPosition >= mVisibleIndex.size()) {
			throw new RuntimeException("Item desiredPosition "
					+ desiredPosition
					+ " is too high");
		}

		return mVisibleIndex.get(desiredPosition);
	}

	private void getItemsInRange(
			final int firstPosition,
			final int lastPosition,
			final Collection<Item<?>> output) {

		ensureIndex();

		final int from = Math.max(0, firstPosition);
		final int to = Math.min(lastPosition, mVisibleIndex.size() - 1);

		for(int i = from; i <= to; i++) {
			output.add(mVisibleIndex.get(i));
		}
	}

	/**
	 * Updates the set of items which are at, or near, the visible area of the list, and
	 * notifies any item which has entered or left that set. Pass a negative first position
	 * to empty the window.
	 */
	public void setPreloadWindow(
			final int firstVisiblePosition,
			final int lastVisiblePosition) {

		if(mPreloadRangeValid
				&& firstVisiblePosition == mLastPreloadFirst
				&& lastVisiblePosition == mLastPreloadLast) {
			return;
		}

		mPreloadRangeValid = true;
		mLastPreloadFirst = firstVisiblePosition;
		mLastPreloadLast = lastVisiblePosition;

		final HashSet<Item<?>> newWindow = new HashSet<>();

		if(firstVisiblePosition >= 0 && lastVisiblePosition >= firstVisiblePosition) {
			getItemsInRange(
					firstVisiblePosition - PRELOAD_WINDOW_EXTRA_ITEMS,
					lastVisiblePosition + PRELOAD_WINDOW_EXTRA_ITEMS,
					newWindow);
		}

		for(final Item<?> item : mPreloadWindow) {
			if(!newWindow.contains(item)) {
				item.onPreloadWindowChanged(false);
			}
		}

		for(final Item<?> item : newWindow) {
			if(!mPreloadWindow.contains(item)) {
				item.onPreloadWindowChanged(true);
			}
		}

		mPreloadWindow.clear();
		mPreloadWindow.addAll(newWindow);
	}

	/**
	 * Notifies every item in the preload window that it is in the window, despite it having
	 * been there already. Used when the amount of space available to display each item has
	 * changed, as an item may need to reload what it has preloaded at a different size.
	 */
	public void refreshPreloadWindow() {

		for(final Item<?> item : mPreloadWindow) {
			item.onPreloadWindowChanged(true);
		}
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(
			@NonNull final ViewGroup viewGroup,
			final int viewType) {

		final RecyclerView.ViewHolder viewHolder = mViewTypeItemMap.get(viewType)
				.onCreateViewHolder(viewGroup);

		final RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);

		if(viewHolder.itemView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {

			final ViewGroup.MarginLayoutParams oldLayoutParams
					= (ViewGroup.MarginLayoutParams)viewHolder.itemView.getLayoutParams();

			layoutParams.setMargins(
					oldLayoutParams.leftMargin,
					oldLayoutParams.topMargin,
					oldLayoutParams.rightMargin,
					oldLayoutParams.bottomMargin);
		}

		viewHolder.itemView.setLayoutParams(layoutParams);

		return viewHolder;
	}

	@Override
	public void onBindViewHolder(
			@NonNull final RecyclerView.ViewHolder viewHolder,
			final int position) {
		getItemInternal(position).onBindViewHolderInner(viewHolder);

		if(mFullSpanChecker != null) {
			applyFullSpan(viewHolder.itemView, position);
		}
	}

	@Override
	public void onViewAttachedToWindow(
			@NonNull final RecyclerView.ViewHolder viewHolder) {
		super.onViewAttachedToWindow(viewHolder);

		if(mFullSpanChecker == null) {
			return;
		}

		final int position = viewHolder.getLayoutPosition();

		if(position != RecyclerView.NO_POSITION) {
			applyFullSpan(viewHolder.itemView, position);
		}
	}

	// In staggered (masonry) grid layouts items are confined to a single column;
	// chrome items (headers, loading spinners, load-more buttons, errors) must
	// span the full width so the masonry flow isn't broken. In list mode (or any
	// other layout manager) this is a no-op, because the layout params never are
	// StaggeredGridLayoutManager.LayoutParams.
	private void applyFullSpan(
			@NonNull final View itemView,
			final int position) {

		final ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();

		if(layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) {
			((StaggeredGridLayoutManager.LayoutParams)layoutParams)
					.setFullSpan(mFullSpanChecker.isFullSpan(position));
		}
	}

	@Override
	public int getItemViewType(final int position) {

		final Item<?> item = getItemInternal(position);
		final Class<?> viewTypeClass = item.getViewType();

		Integer typeId = mItemViewTypeMap.get(viewTypeClass);

		if(typeId == null) {
			typeId = mItemViewTypeMap.size();
			mItemViewTypeMap.put(viewTypeClass, typeId);
			mViewTypeItemMap.put(typeId, item);
		}

		return typeId;
	}

	@Override
	public long getItemId(final int position) {
		return getItemInternal(position).mUniqueId;
	}

	@Override
	public int getItemCount() {

		ensureIndex();

		return mVisibleIndex.size();
	}

	public Item<?> getItemAtPosition(final int position) {
		return getItemInternal(position);
	}

	public int getGroupIdAtPosition(final int position) {

		ensureIndex();

		if(position < 0 || position >= mVisibleIndex.size()) {
			throw new RuntimeException("Item position "
					+ position
					+ " is too high");
		}

		for(int groupId = mItems.length - 1; groupId >= 0; groupId--) {
			if(mGroupVisibleStarts[groupId] <= position) {
				return groupId;
			}
		}

		throw new RuntimeException("Item position "
				+ position
				+ " is too high");
	}

	public void appendToGroup(final int group, final Item<?> item) {

		ensureIndex();

		final int position = group + 1 < mItems.length
				? mGroupVisibleStarts[group + 1]
				: mVisibleIndex.size();

		mItems[group].add(item);

		if(!item.mCurrentlyHidden) {
			notifyItemInserted(position);
		}

		invalidateIndex();
	}

	public void appendToGroup(final int group, final Collection<Item<?>> items) {

		ensureIndex();

		final int position = group + 1 < mItems.length
				? mGroupVisibleStarts[group + 1]
				: mVisibleIndex.size();

		mItems[group].addAll(items);

		for(final Item<?> item : items) {
			item.mCurrentlyHidden = false;
		}

		notifyItemRangeInserted(position, items.size());

		invalidateIndex();
	}

	public void removeAllFromGroup(final int groupId) {

		final ArrayList<Item<?>> group = mItems[groupId];

		ensureIndex();

		final int[] positions = new int[group.size()];
		int position = mGroupVisibleStarts[groupId];

		for(int i = 0; i < group.size(); i++) {
			positions[i] = position;
			if(!group.get(i).mCurrentlyHidden) {
				position++;
			}
		}

		for(int i = group.size() - 1; i >= 0; i--) {

			final Item<?> item = group.get(i);

			group.remove(i);

			if(!item.mCurrentlyHidden) {
				notifyItemRemoved(positions[i]);
			}
		}

		invalidateIndex();
	}

	public void removeFromGroup(final int groupId, final Item<?> item) {

		final ArrayList<Item<?>> group = mItems[groupId];

		int indexInGroup = -1;

		for(int i = 0; i < group.size(); i++) {
			if(group.get(i) == item) {
				indexInGroup = i;
				break;
			}
		}

		if(indexInGroup < 0) {
			throw new RuntimeException("Item not found");
		}

		ensureIndex();

		int position = mGroupVisibleStarts[groupId];

		for(int i = 0; i < indexInGroup; i++) {
			if(!group.get(i).mCurrentlyHidden) {
				position++;
			}
		}

		group.remove(indexInGroup);

		if(!item.mCurrentlyHidden) {
			notifyItemRemoved(position);
		}

		invalidateIndex();
	}

	public void updateHiddenStatus() {

		int position = 0;

		for(int groupId = 0; groupId < mItems.length; groupId++) {

			final ArrayList<Item<?>> group = mItems[groupId];

			for(int positionInGroup = 0;
				positionInGroup < group.size();
				positionInGroup++) {

				final Item<?> item = group.get(positionInGroup);

				final boolean wasHidden = item.mCurrentlyHidden;
				final boolean isHidden = item.isHidden();
				item.mCurrentlyHidden = isHidden;

				if(isHidden && !wasHidden) {
					notifyItemRemoved(position);

				} else if(!isHidden && wasHidden) {
					notifyItemInserted(position);
				}

				if(!isHidden) {
					position++;
				}
			}
		}

		invalidateIndex();
	}

	public void notifyItemChanged(final int groupId, final Item<?> item) {
		final int position = getItemPositionInternal(groupId, item);
		notifyItemChanged(position);
	}
}
