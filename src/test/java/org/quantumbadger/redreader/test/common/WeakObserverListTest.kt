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

package org.quantumbadger.redreader.test.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.quantumbadger.redreader.common.WeakObserverList
import java.lang.ref.WeakReference
import java.util.function.Consumer

class WeakObserverListTest {

	// Keeps the "still reachable" observers alive across a collection loop.
	// A static field is a GC root, whereas a local variable (or an instance field
	// of the test, which JUnit only holds weakly) may be treated as unreachable
	// once it has stopped being read, which would collect the observers these
	// tests expect to survive.
	companion object {
		private val strongRefs = ArrayList<Any>()
	}

	@Test
	fun registeredObserverIsNotified() {
		val list = WeakObserverList<String>()
		val received = ArrayList<String>()

		list.add { received.add(it) }
		list.notifyObservers("first")

		assertEquals(listOf("first"), received)
	}

	@Test
	fun storedObserverIsNotified() {
		val list = WeakObserverList<String>()
		val received = ArrayList<String>()

		val observer: (String) -> Unit = { text -> received.add("stored:$text") }
		list.add(observer)
		list.notifyObservers("x")

		assertEquals(listOf("stored:x"), received)
	}

	@Test
	fun everyRegisteredObserverIsNotified() {
		val list = WeakObserverList<String>()
		val received = ArrayList<String>()

		repeat(5) { i ->
			list.add { received.add("$i:$it") }
		}

		list.notifyObservers("x")

		assertEquals(5, received.size)
		assertTrue(received.containsAll(
			listOf("0:x", "1:x", "2:x", "3:x", "4:x")))
	}

	@Test
	fun notifyingWithNoObserversDoesNothing() {
		val list = WeakObserverList<String>()

		list.notifyObservers("nothing happens")

		assertEquals(0, list.size())
	}

	@Test
	fun observerMayRegisterAnotherObserverWhileNotified() {
		val list = WeakObserverList<String>()
		val received = ArrayList<String>()

		val laterObserver: (String) -> Unit = { text -> received.add("later:$text") }

		list.add {
			received.add("first:$it")
			list.add(laterObserver)
		}

		list.notifyObservers("a")
		list.notifyObservers("b")

		// The observer added during the first notification must be notified from
		// the next one onwards, and only from then on.
		assertEquals(listOf("first:a", "first:b", "later:b"), received)
	}

	@Test
	fun observerIsPurgedOnceItsTargetIsCollected() {
		val list = WeakObserverList<String>()
		val received = ArrayList<String>()

		val observerRef = registerCollectableObserver(list, received)
		assertEquals(1, list.size())

		awaitCollection(observerRef)
		assertNull(observerRef.get())

		// The notification purges the dead registration instead of calling it, so
		// a collected observer is never called and never walked again afterwards.
		list.notifyObservers("after collection")

		assertEquals(0, list.size())
		assertTrue(received.isEmpty())
	}

	@Test
	fun liveObserverSurvivesAlongsideCollectedOne() {
		val list = WeakObserverList<String>()
		val received = ArrayList<String>()

		// A Consumer is held here, not a Kotlin lambda: the list holds the observer
		// it was given, which for a lambda argument is a freshly created wrapper.
		val live = Consumer<String> { text -> received.add("live:$text") }
		strongRefs.add(live)
		list.add(live)

		val collectedRef = registerCollectableObserver(list, received)

		awaitCollection(collectedRef)
		assertNull(collectedRef.get())

		list.notifyObservers("value")

		assertEquals(listOf("live:value"), received)
		assertEquals(1, list.size())
	}

	@Test
	fun perBindObserversDoNotAccumulate() {
		val list = WeakObserverList<String>()
		val received = ArrayList<String>()

		val live = Consumer<String> { text -> received.add("live:$text") }
		strongRefs.add(live)
		list.add(live)

		// Registering one observer per bind, with nothing else keeping them
		// reachable, is the pattern this class exists for: the registrations must
		// not pile up as the same view is bound again and again.
		repeat(50) { i ->
			val perBind = Consumer<String> { text -> received.add("$i:$text") }
			list.add(perBind)
		}

		assertEquals(51, list.size())

		// Collecting is best-effort, so purge repeatedly until only the observer
		// which is still reachable is left.
		for(attempt in 0 until 100) {
			System.gc()
			Thread.sleep(10)
			list.notifyObservers("purge")

			if(list.size() == 1) {
				break
			}
		}

		assertEquals(1, list.size())

		received.clear()
		list.notifyObservers("value")

		assertEquals(listOf("live:value"), received)
	}

	// Registers an observer which is only reachable through the weak reference it
	// returns, so it becomes collectable as soon as this call returns.
	private fun registerCollectableObserver(
		list: WeakObserverList<String>,
		received: ArrayList<String>
	): WeakReference<(String) -> Unit> {
		val observer: (String) -> Unit = { text -> received.add("collected:$text") }
		val ref = WeakReference(observer)
		list.add(observer)
		return ref
	}

	private fun awaitCollection(ref: WeakReference<*>) {
		for(attempt in 0 until 100) {
			System.gc()
			Thread.sleep(10)

			if(ref.get() == null) {
				return
			}
		}
	}
}
