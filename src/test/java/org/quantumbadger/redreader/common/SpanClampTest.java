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

package org.quantumbadger.redreader.common;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class SpanClampTest {

	@Test
	public void normalRangeUnchanged() {
		assertArrayEquals(new int[] { 2, 8 }, BetterSSB.clampRange(10, 2, 8));
	}

	@Test
	public void reversedRangeIsSwapped() {
		assertArrayEquals(new int[] { 2, 8 }, BetterSSB.clampRange(10, 8, 2));
	}

	@Test
	public void outOfRangeEndIsClampedToLength() {
		assertArrayEquals(new int[] { 2, 10 }, BetterSSB.clampRange(10, 2, 100));
	}

	@Test
	public void negativeStartIsClampedToZero() {
		assertArrayEquals(new int[] { 0, 5 }, BetterSSB.clampRange(10, -3, 5));
	}

	@Test
	public void reversedAndOutOfRange() {
		assertArrayEquals(new int[] { 2, 10 }, BetterSSB.clampRange(10, 100, 2));
	}

	@Test
	public void bothBeyondLengthCollapseToZeroLengthAtEnd() {
		assertArrayEquals(new int[] { 10, 10 }, BetterSSB.clampRange(10, 55, 77));
	}

	@Test
	public void bothNegativeCollapseToZeroLengthAtStart() {
		assertArrayEquals(new int[] { 0, 0 }, BetterSSB.clampRange(10, -7, -3));
	}
}
