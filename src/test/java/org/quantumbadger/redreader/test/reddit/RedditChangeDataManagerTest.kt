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

package org.quantumbadger.redreader.test.reddit

import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.kthings.RedditPost
import org.quantumbadger.redreader.reddit.kthings.RedditTimestampUTC
import org.quantumbadger.redreader.reddit.kthings.UrlEncodedString
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class RedditChangeDataManagerTest {

	@Before
	fun setUp() {
		PrefsUtility.init(RuntimeEnvironment.getApplication())
	}

	private fun managerFor(testName: String): RedditChangeDataManager =
		RedditChangeDataManager.getInstance(RedditAccount(testName, null, 0, null))

	private fun post(id: String, likes: Boolean? = null) = RedditPost(
		id = id,
		name = RedditIdAndType("t3_$id"),
		subreddit = UrlEncodedString("test"),
		num_comments = 0,
		score = 0,
		permalink = UrlEncodedString("/r/test/comments/$id"),
		created_utc = RedditTimestampUTC(TimestampUTC.ZERO),
		url = UrlEncodedString("https://example.com/image.jpg"),
		likes = likes)

	@Test
	fun identicalUpdateDoesNotNotifyListeners() {

		val manager = managerFor("identical_update_test")
		val thing = RedditIdAndType("t3_identical")

		var notifications = 0
		val listener = RedditChangeDataManager.Listener { notifications++ }
		manager.addListener(thing, listener)

		val firstTimestamp = TimestampUTC.fromUtcMs(1_000)

		manager.update(firstTimestamp, post("identical", likes = true))
		shadowOf(Looper.getMainLooper()).idle()
		assertEquals(1, notifications)

		manager.update(firstTimestamp, post("identical", likes = true))
		shadowOf(Looper.getMainLooper()).idle()
		assertEquals(1, notifications)

		manager.update(firstTimestamp, post("identical", likes = false))
		shadowOf(Looper.getMainLooper()).idle()
		assertEquals(2, notifications)

		manager.update(TimestampUTC.fromUtcMs(2_000), post("identical", likes = false))
		shadowOf(Looper.getMainLooper()).idle()
		assertEquals(2, notifications)
	}

	@Test
	fun clearStateNotifiesOnlyWhenSomethingWasRemoved() {

		val manager = managerFor("clear_state_test")
		val thing = RedditIdAndType("t3_clear")

		var notifications = 0
		val listener = RedditChangeDataManager.Listener { notifications++ }
		manager.addListener(thing, listener)

		manager.update(TimestampUTC.fromUtcMs(1_000), post("clear", likes = null))
		shadowOf(Looper.getMainLooper()).idle()
		assertEquals(0, notifications)

		manager.update(TimestampUTC.fromUtcMs(1_000), post("clear", likes = true))
		shadowOf(Looper.getMainLooper()).idle()
		assertEquals(1, notifications)
	}
}
