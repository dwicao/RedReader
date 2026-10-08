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

import org.junit.Assert.assertTrue
import org.junit.Test
import org.quantumbadger.redreader.common.PrioritisedCachedThreadPool
import org.quantumbadger.redreader.common.Priority
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PrioritisedCachedThreadPoolTest {

	@Test
	fun poolKeepsProcessingAfterAThrowingTask() {

		// A single-thread pool makes the failure observable: if the worker dies
		// with the throwing task, mRunningThreads leaks and the second task is
		// never scheduled.
		val pool = PrioritisedCachedThreadPool(1, "TestPool")
		val taskFailed = CountDownLatch(1)
		val taskCompleted = CountDownLatch(1)

		pool.add(object : PrioritisedCachedThreadPool.Task() {
			override fun getPriority(): Priority = Priority(0)

			override fun run() {
				taskFailed.countDown()
				throw IllegalStateException("deliberate test failure")
			}
		})

		assertTrue(taskFailed.await(5, TimeUnit.SECONDS))

		pool.add(object : PrioritisedCachedThreadPool.Task() {
			override fun getPriority(): Priority = Priority(0)

			override fun run() {
				taskCompleted.countDown()
			}
		})

		assertTrue(
			"pool should keep processing tasks after a failing task",
			taskCompleted.await(5, TimeUnit.SECONDS))
	}
}
