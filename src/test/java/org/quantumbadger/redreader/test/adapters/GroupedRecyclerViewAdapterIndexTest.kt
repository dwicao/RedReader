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

package org.quantumbadger.redreader.test.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupedRecyclerViewAdapterIndexTest {

	private class TestItem : GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder>() {

		var hidden = false
		val preloadEvents = ArrayList<Boolean>()

		override fun getViewType(): Class<*> = TestItem::class.java

		override fun onCreateViewHolder(viewGroup: ViewGroup): RecyclerView.ViewHolder =
			throw IllegalStateException("Not used by this test")

		override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {}

		override fun isHidden(): Boolean = hidden

		override fun onPreloadWindowChanged(inWindow: Boolean) {
			preloadEvents.add(inWindow)
		}
	}

	private class RecordingObserver : RecyclerView.AdapterDataObserver() {

		val inserted = ArrayList<Pair<Int, Int>>()
		val removed = ArrayList<Pair<Int, Int>>()

		override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
			inserted.add(positionStart to itemCount)
		}

		override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
			removed.add(positionStart to itemCount)
		}
	}

	private fun item(): TestItem = TestItem()

	@Test
	fun appendPositionsAndGroupIds() {

		val adapter = GroupedRecyclerViewAdapter(3)

		val a = item()
		val b = item()
		val c = item()
		val d = item()

		adapter.appendToGroup(0, a)
		adapter.appendToGroup(0, b)
		adapter.appendToGroup(1, c)
		adapter.appendToGroup(2, d)

		assertEquals(4, adapter.itemCount)
		assertSame(a, adapter.getItemAtPosition(0))
		assertSame(b, adapter.getItemAtPosition(1))
		assertSame(c, adapter.getItemAtPosition(2))
		assertSame(d, adapter.getItemAtPosition(3))

		assertEquals(0, adapter.getGroupIdAtPosition(0))
		assertEquals(0, adapter.getGroupIdAtPosition(1))
		assertEquals(1, adapter.getGroupIdAtPosition(2))
		assertEquals(2, adapter.getGroupIdAtPosition(3))
	}

	@Test
	fun hiddenResyncViaUpdateHiddenStatus() {

		val adapter = GroupedRecyclerViewAdapter(3)

		val a = item()
		val b = item()
		val c = item()
		val d = item()
		val e = item()

		adapter.appendToGroup(0, a)
		adapter.appendToGroup(0, b)
		adapter.appendToGroup(1, c)
		adapter.appendToGroup(2, d)
		adapter.appendToGroup(2, e)

		assertEquals(5, adapter.itemCount)

		b.hidden = true
		adapter.updateHiddenStatus()

		assertEquals(4, adapter.itemCount)
		assertSame(a, adapter.getItemAtPosition(0))
		assertSame(c, adapter.getItemAtPosition(1))
		assertSame(d, adapter.getItemAtPosition(2))
		assertSame(e, adapter.getItemAtPosition(3))
		assertEquals(0, adapter.getGroupIdAtPosition(0))
		assertEquals(1, adapter.getGroupIdAtPosition(1))
		assertEquals(2, adapter.getGroupIdAtPosition(2))
		assertEquals(2, adapter.getGroupIdAtPosition(3))

		b.hidden = false
		adapter.updateHiddenStatus()

		assertEquals(5, adapter.itemCount)
		assertSame(b, adapter.getItemAtPosition(1))
		assertSame(c, adapter.getItemAtPosition(2))
	}

	@Test
	fun appendCollectionAtGroupBoundaryNotifiesRange() {

		val adapter = GroupedRecyclerViewAdapter(3)
		val observer = RecordingObserver()
		adapter.registerAdapterDataObserver(observer)

		val a = item()
		val hiddenInGroupOne = item()
		val d = item()

		adapter.appendToGroup(0, a)
		adapter.appendToGroup(1, hiddenInGroupOne)
		adapter.appendToGroup(2, d)

		hiddenInGroupOne.hidden = true
		adapter.updateHiddenStatus()

		observer.inserted.clear()
		observer.removed.clear()

		val y1 = item()
		val y2 = item()
		adapter.appendToGroup(1, listOf(y1, y2))

		// The hidden item in group one must not shift the insertion position
		assertEquals(listOf(1 to 2), observer.inserted)
		assertEquals(4, adapter.itemCount)
		assertSame(a, adapter.getItemAtPosition(0))
		assertSame(y1, adapter.getItemAtPosition(1))
		assertSame(y2, adapter.getItemAtPosition(2))
		assertSame(d, adapter.getItemAtPosition(3))
	}

	@Test
	fun removeAllFromGroupNotifiesCorrectPositions() {

		val adapter = GroupedRecyclerViewAdapter(3)
		val observer = RecordingObserver()
		adapter.registerAdapterDataObserver(observer)

		val a = item()
		val b = item()
		val c = item()
		val d = item()
		val e = item()

		adapter.appendToGroup(0, a)
		adapter.appendToGroup(0, b)
		adapter.appendToGroup(1, c)
		adapter.appendToGroup(2, d)
		adapter.appendToGroup(2, e)

		observer.inserted.clear()
		observer.removed.clear()

		adapter.removeAllFromGroup(1)

		assertEquals(listOf(2 to 1), observer.removed)
		assertEquals(4, adapter.itemCount)
		assertSame(a, adapter.getItemAtPosition(0))
		assertSame(b, adapter.getItemAtPosition(1))
		assertSame(d, adapter.getItemAtPosition(2))
		assertSame(e, adapter.getItemAtPosition(3))
		assertEquals(2, adapter.getGroupIdAtPosition(3))

		observer.removed.clear()

		adapter.removeAllFromGroup(0)

		// Removed back to front: item at position 1 first, then position 0
		assertEquals(listOf(1 to 1, 0 to 1), observer.removed)
		assertEquals(2, adapter.itemCount)
		assertSame(d, adapter.getItemAtPosition(0))
		assertSame(e, adapter.getItemAtPosition(1))
	}

	@Test
	fun removeFromGroupNotifiesAndRejectsForeignItem() {

		val adapter = GroupedRecyclerViewAdapter(3)
		val observer = RecordingObserver()
		adapter.registerAdapterDataObserver(observer)

		val a = item()
		val b = item()
		val c = item()

		adapter.appendToGroup(0, a)
		adapter.appendToGroup(0, b)
		adapter.appendToGroup(1, c)

		observer.inserted.clear()
		observer.removed.clear()

		adapter.removeFromGroup(0, b)

		assertEquals(listOf(1 to 1), observer.removed)
		assertEquals(2, adapter.itemCount)
		assertSame(a, adapter.getItemAtPosition(0))
		assertSame(c, adapter.getItemAtPosition(1))

		assertThrows(RuntimeException::class.java) {
			adapter.removeFromGroup(0, c)
		}
	}

	@Test
	fun outOfRangeLookupsThrow() {

		val adapter = GroupedRecyclerViewAdapter(2)
		adapter.appendToGroup(0, item())

		assertThrows(RuntimeException::class.java) {
			adapter.getItemAtPosition(-1)
		}

		assertThrows(RuntimeException::class.java) {
			adapter.getItemAtPosition(1)
		}

		assertThrows(RuntimeException::class.java) {
			adapter.getGroupIdAtPosition(-1)
		}

		assertThrows(RuntimeException::class.java) {
			adapter.getGroupIdAtPosition(1)
		}
	}

	@Test
	fun preloadWindowDedupeAndMutationInvalidation() {

		val adapter = GroupedRecyclerViewAdapter(1)
		val items = ArrayList<TestItem>()

		for(i in 0 until 20) {
			val next = item()
			items.add(next)
			adapter.appendToGroup(0, next)
		}

		adapter.setPreloadWindow(5, 9)

		var inWindowCount = 0
		for(next in items) {
			if(next.preloadEvents == listOf(true)) {
				inWindowCount++
			}
		}
		assertEquals(15, inWindowCount)

		for(next in items) {
			next.preloadEvents.clear()
		}

		// Identical range: nothing may be re-notified
		adapter.setPreloadWindow(5, 9)
		for(next in items) {
			assertEquals(0, next.preloadEvents.size)
		}

		// A structural change must invalidate the dedupe even for an identical range
		items[3].hidden = true
		adapter.updateHiddenStatus()

		for(next in items) {
			next.preloadEvents.clear()
		}

		adapter.setPreloadWindow(5, 9)

		assertEquals(listOf(false), items[3].preloadEvents)
		assertEquals(listOf(true), items[15].preloadEvents)

		for(i in items.indices) {
			if(i == 3 || i == 15) {
				continue
			}
			assertEquals(0, items[i].preloadEvents.size)
		}
	}
}
