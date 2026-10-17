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

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * A list of observers which are held weakly.
 *
 * <p>{@link java.util.Observable} holds its observers strongly, so an observer registered from
 * something short-lived (such as a list row which is recycled, or a text buffer which is rebuilt
 * every time the row is bound) keeps that object alive forever, and every later notification
 * walks the whole accumulated list. Registering from a recycled view therefore makes both memory
 * use and notification cost grow with the number of binds rather than the number of live views.
 *
 * <p>This list holds only weak references, so an observer whose target has been garbage
 * collected is dropped on the next notification, and it is never notified again.
 *
 * @param <T> the type of argument passed to the observers.
 */
public final class WeakObserverList<T> {

	private final ArrayList<WeakReference<Consumer<T>>> mObservers = new ArrayList<>(2);

	public void add(@NonNull final Consumer<T> observer) {
		synchronized(mObservers) {
			mObservers.add(new WeakReference<>(observer));
		}
	}

	public void notifyObservers(@NonNull final T arg) {

		final ArrayList<Consumer<T>> live;

		synchronized(mObservers) {
			if(mObservers.isEmpty()) {
				return;
			}
			live = new ArrayList<>(mObservers.size());

			final Iterator<WeakReference<Consumer<T>>> it = mObservers.iterator();
			while(it.hasNext()) {
				final Consumer<T> observer = it.next().get();

				if(observer == null) {
					// The observer's target has been collected, so this registration
					// is dead; drop it rather than notifying it ever again.
					it.remove();
				} else {
					live.add(observer);
				}
			}
		}

		// Notified outside the lock, so an observer may register another one itself.
		for(final Consumer<T> observer : live) {
			observer.accept(arg);
		}
	}

	/**
	 * @return the number of registered observers, including any which have been collected but
	 * not yet purged. Only intended for tests.
	 */
	public int size() {
		synchronized(mObservers) {
			return mObservers.size();
		}
	}
}
