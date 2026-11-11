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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.listingcontrollers.PostListingPaginationPolicy
import java.util.UUID

class PostListingPaginationPolicyTest {

	@Test
	fun freshListingAlwaysDownloadsNextPage() {
		assertTrue(
			PostListingPaginationPolicy.shouldDownloadNextPage(
				TimestampUTC.now(),
				true))

		assertTrue(
			PostListingPaginationPolicy.shouldDownloadNextPage(
				TimestampUTC.now(),
				false))
	}

	@Test
	fun staleListingWithCurrentSessionDownloadsNextPage() {
		assertTrue(
			PostListingPaginationPolicy.shouldDownloadNextPage(
				TimestampUTC.ZERO,
				false))
	}

	@Test
	fun staleListingWithSupersededSessionDoesNotDownload() {
		assertFalse(
			PostListingPaginationPolicy.shouldDownloadNextPage(
				TimestampUTC.ZERO,
				true))
	}

	@Test
	fun unknownTimestampDownloadsNextPage() {
		assertTrue(
			PostListingPaginationPolicy.shouldDownloadNextPage(
				null,
				true))
	}

	@Test
	fun newerForeignSessionMeansSuperseded() {
		val mine = UUID.randomUUID()

		val records = listOf(
			PostListingPaginationPolicy.SessionRecord(mine, TimestampUTC.ZERO),
			PostListingPaginationPolicy.SessionRecord(
				UUID.randomUUID(),
				TimestampUTC.now()))

		assertTrue(
			PostListingPaginationPolicy.isSessionSuperseded(
				mine,
				TimestampUTC.ZERO,
				records))
	}

	@Test
	fun onlyOwnSessionsMeansNotSuperseded() {
		val mine = UUID.randomUUID()

		val records = listOf(
			PostListingPaginationPolicy.SessionRecord(mine, TimestampUTC.ZERO),
			PostListingPaginationPolicy.SessionRecord(mine, TimestampUTC.now()))

		assertFalse(
			PostListingPaginationPolicy.isSessionSuperseded(
				mine,
				TimestampUTC.ZERO,
				records))
	}

	@Test
	fun olderForeignSessionMeansNotSuperseded() {
		val mine = UUID.randomUUID()

		val records = listOf(
			PostListingPaginationPolicy.SessionRecord(
				UUID.randomUUID(),
				TimestampUTC.ZERO))

		assertFalse(
			PostListingPaginationPolicy.isSessionSuperseded(
				mine,
				TimestampUTC.now(),
				records))
	}

	@Test
	fun noKnownSessionsMeansNotSuperseded() {
		assertFalse(
			PostListingPaginationPolicy.isSessionSuperseded(
				UUID.randomUUID(),
				TimestampUTC.ZERO,
				emptyList()))
	}

	@Test
	fun nullSessionOrTimestampMeansNotSuperseded() {
		assertFalse(
			PostListingPaginationPolicy.isSessionSuperseded(
				null,
				TimestampUTC.ZERO,
				emptyList()))

		assertFalse(
			PostListingPaginationPolicy.isSessionSuperseded(
				UUID.randomUUID(),
				null,
				emptyList()))
	}
}
