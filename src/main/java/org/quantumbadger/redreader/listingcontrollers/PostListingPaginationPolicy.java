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

package org.quantumbadger.redreader.listingcontrollers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.quantumbadger.redreader.common.time.TimeDuration;
import org.quantumbadger.redreader.common.time.TimestampUTC;

import java.util.List;
import java.util.UUID;

public final class PostListingPaginationPolicy {

	private PostListingPaginationPolicy() {
	}

	public static final class SessionRecord {

		@NonNull public final UUID session;
		@NonNull public final TimestampUTC timestamp;

		public SessionRecord(
				@NonNull final UUID session,
				@NonNull final TimestampUTC timestamp) {
			this.session = session;
			this.timestamp = timestamp;
		}
	}

	public static boolean isSessionSuperseded(
			@Nullable final UUID currentSession,
			@Nullable final TimestampUTC currentTimestamp,
			@NonNull final List<SessionRecord> knownSessions) {

		if(currentSession == null || currentTimestamp == null) {
			return false;
		}

		for(final SessionRecord record : knownSessions) {
			if(!record.session.equals(currentSession)
					&& record.timestamp.isGreaterThan(currentTimestamp)) {
				return true;
			}
		}

		return false;
	}

	public static boolean shouldDownloadNextPage(
			@Nullable final TimestampUTC listingTimestamp,
			final boolean sessionSuperseded) {

		if(listingTimestamp == null) {
			return true;
		}

		// TODO customise (currently 3 hrs)
		if(listingTimestamp.elapsed().isLessThan(TimeDuration.hours(3))) {
			return true;
		}

		return !sessionSuperseded;
	}
}
