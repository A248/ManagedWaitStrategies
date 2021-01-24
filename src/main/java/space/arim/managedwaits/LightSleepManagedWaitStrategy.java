/*
 * managedwaits
 * Copyright © 2021 Anand Beh
 *
 * managedwaits is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * managedwaits is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with managedwaits. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */

package space.arim.managedwaits;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

import static space.arim.managedwaits.Reporting.*;

/**
 * Implementation of {@code ManagedWaitStrategy} which sleeps for small
 * amounts of time, and polls whether the future is complete, while waiting. <br>
 * <br>
 * This wait strategy has the biggest advantage when the main thread is blocked
 * minimally for short periods of time, if at all. This is the majority of cases,
 * so this strategy is the default.
 */
public final class LightSleepManagedWaitStrategy implements ManagedWaitStrategy {

	private final long sleepTime;

	/**
	 * Creates using the specified sleep time. <br>
	 * <br>
	 * The sleep time ought to be small.
	 *
	 * @param sleepTime the sleep time, in nanoseconds
	 * @throws IllegalArgumentException if sleep time is negative
	 */
	public LightSleepManagedWaitStrategy(long sleepTime) {
		if (sleepTime <= 0) {
			throw new IllegalArgumentException("sleep time must be positive");
		}
		this.sleepTime = sleepTime;
	}

	/**
	 * Creates using the default sleep time
	 *
	 */
	public LightSleepManagedWaitStrategy() {
		this(300L);
	}

	@Override
	public boolean requireSignalWhenFutureCompleted() {
		return false;
	}

	@Override
	public void signalWhenFutureCompleted(Thread mainThread) {
	}

	@Override
	public void signalWhenTaskAdded(Thread mainThread) {
		/*
		 * It would be unwise to unpark the main thread here, as such
		 * would cause spurious wakeups in other uses of LockSupport.
		 * Sleep times are intended to be minimal anyway
		 */
	}

	@Override
	public <T> T await(Runnable runQueuedTasks, CentralisedFuture<T> future) {
		for (;;) {
			runQueuedTasks.run();

			T result;
			if ((result = reportJoin(future)) != ABSENT_VALUE) {	// if (isDone()) {
				return result;										// return super.join(); }
			}
			LockSupport.parkNanos(this, sleepTime);
		}
	}

	@Override
	public <T> T awaitInterruptibly(Runnable runQueuedTasks, CentralisedFuture<T> future)
			throws InterruptedException, ExecutionException {
		for (;;) {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			runQueuedTasks.run();

			T result;
			if ((result = reportGet(future)) != ABSENT_VALUE) {	// if (isDone()) {
				return result;									// return super.get(); }
			}
			LockSupport.parkNanos(this, sleepTime);
		}
	}

	@Override
	public <T> T awaitUntil(Runnable runQueuedTasks, CentralisedFuture<T> future, long timeout, TimeUnit unit)
			throws InterruptedException, TimeoutException, ExecutionException {
		long deadline = System.nanoTime() + unit.toNanos(timeout);
		for (;;) {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			runQueuedTasks.run();

			T result;
			if ((result = reportGet(future)) != ABSENT_VALUE) {	// if (isDone()) {
				return result;									// return super.get(); }
			}
			long delay = deadline - System.nanoTime();
			if (delay <= 0) {
				throw new TimeoutException();
			}
			// Here, parkFor is used as a micro-optimization to likely avoid a call to System.nanoTime()
			long parkFor = Math.min(delay, sleepTime);
			LockSupport.parkNanos(this, parkFor);

			// If delay != parkFor, it is not possible to have timed out at this moment
			if (delay == parkFor && deadline - System.nanoTime() <= 0) {
				throw new TimeoutException();
			}
		}
	}

	/*
	 * JDK-8074773 - lost unparks due to classloading of LockSupport - does not apply here,
	 * because we do not rely on unparks
	 */

}
