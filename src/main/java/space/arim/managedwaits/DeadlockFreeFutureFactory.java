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
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.SynchronousExecutor;
import space.arim.omnibus.util.concurrent.impl.AbstractFactoryOfTheFuture;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Abstract {@link FactoryOfTheFuture} implementation suitable for applications which
 * indeed have a main thread, and which undertake managed waits when awaiting completion
 * of a future while on the main thread.
 *
 * @author A248
 */
public abstract class DeadlockFreeFutureFactory extends AbstractFactoryOfTheFuture {

	private final TaskQueue taskQueue;
	private final ManagedWaitStrategy waitStrategy;

	final Runnable runQueuedTasks = new PeriodicSyncUnleasher();
	final SynchronousExecutor trustedSyncExecutor = new TrustedSyncExecutor();

	/**
	 * Creates an instance
	 *
	 * @param taskQueue the task queue
	 * @param waitStrategy the wait strategy
	 * @throws NullPointerException if either parameter is null
	 */
	protected DeadlockFreeFutureFactory(TaskQueue taskQueue, ManagedWaitStrategy waitStrategy) {
		this.taskQueue = Objects.requireNonNull(taskQueue);
		this.waitStrategy = Objects.requireNonNull(waitStrategy);
	}

	@Override
	public <T> CentralisedFuture<T> newIncompleteFuture() {
		return new DeadlockFreeFuture<>(this);
	}

	/**
	 * Determines whether the application is on the primary thread
	 *
	 * @return true if on the main thread, false otherwise
	 */
	public abstract boolean isPrimaryThread();

	/**
	 * Gets the primary thread itself, or returns {@code null} if not known
	 *
	 * @return the primary thread or {@code null} if not known
	 */
	public abstract Thread getPrimaryThread();

	private void executeSyncNoExceptionGuard(Runnable command) {
		if (isPrimaryThread()) {
			command.run();
			return;
		}
		if (taskQueue.addTask(command)) {
			waitStrategy.signalWhenTaskAdded(getPrimaryThread());
		}
	}

	boolean requireSignalWhenFutureCompleted() {
		return waitStrategy.requireSignalWhenFutureCompleted();
	}

	void signalFutureCompleted() {
		waitStrategy.signalWhenFutureCompleted(getPrimaryThread());
	}

	<T> T await(DeadlockFreeFuture<T> future) {
		return waitStrategy.await(runQueuedTasks, future);
	}

	<T> T awaitInterruptibly(DeadlockFreeFuture<T> future) throws InterruptedException, ExecutionException {
		return waitStrategy.awaitInterruptibly(runQueuedTasks, future);
	}

	<T> T awaitUntil(DeadlockFreeFuture<T> future, long timeout, TimeUnit unit)
			throws InterruptedException, TimeoutException, ExecutionException {
		return waitStrategy.awaitUntil(runQueuedTasks, future, timeout, unit);
	}

	@Override
	public void executeSync(Runnable command) {
		executeSyncNoExceptionGuard(new RunnableExceptionReporter(command));
	}

	/**
	 * Runs all scheduled tasks. Should only be called if known to be on main thread.
	 *
	 */
	private void unleashSyncTasks() {
		taskQueue.pollAndRunAll();
	}

	private class PeriodicSyncUnleasher implements Runnable {

		@Override
		public void run() {
			unleashSyncTasks();
		}
	}

	private class TrustedSyncExecutor implements SynchronousExecutor {

		@Override
		public void executeSync(Runnable command) {
			executeSyncNoExceptionGuard(command);
		}
	}

	@Override
	public String toString() {
		return "DeadlockFreeFutureFactory{" +
				"taskQueue=" + taskQueue +
				", waitStrategy=" + waitStrategy +
				", getPrimaryThread()=" + getPrimaryThread() +
				'}';
	}
}
