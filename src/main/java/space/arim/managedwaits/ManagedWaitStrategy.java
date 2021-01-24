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

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Strategy for {@link FactoryOfTheFuture} implementations which utilize a managed
 * wait. <br>
 * <br>
 * <b>Implementation</b> <br>
 * The {@code await}, {@code awaitInterruptibly}, and {@code awaitUntil} methods implement
 * the managed wait itself. The core objective of these methods is to wait for the completion
 * of a given future, without stalling absolutely. That is, implementations ought to be able
 * to run queued tasks while simultaneously awaiting the completion of the future. This is
 * somewhat similar to the idea of work-stealing. <br>
 * <br>
 * Implementations are primarily based on some kind of spin loop. It is not necessary to check
 * whether the future is already complete before entering a spin loop. That is, implementations
 * may wait, check for completion, and keep waiting. <br>
 * <br>
 * Each method is provided with a {@code Runnable} which is to be used to run outstanding
 * queued tasks. The runnable may be invoked multiple times, and each time it will run and
 * clear the task queue. <br>
 * <br>
 * It would be folly for implementations to call any of the {@code join} or {@code get} methods
 * on the provided future, as it is these methods which themselves rely on the managed wait
 * strategy.
 */
public interface ManagedWaitStrategy {

	/**
	 * Whether this strategy uses {@link #signalWhenFutureCompleted(Thread)}} to wakeup blocked threads. <br>
	 * <br>
	 * If {@code false}, calls to {@code signalWhenFutureCompleted} may be omitted by the futures
	 * factory.
	 *
	 * @return true if signalling is required, false otherwise
	 */
	boolean requireSignalWhenFutureCompleted();

	/**
	 * Signals when an individual future is complete. May be used to implement
	 * waking up of any spins. <br>
	 * <br>
	 * The main thread parameter may be {@code null} if the factory is not yet aware
	 * of the main thread. When it is null, however, it is guaranteed that no future
	 * has been or is being awaited on the main thread.
	 *
	 * @param mainThread the main thread, may be null if not yet known
	 */
	void signalWhenFutureCompleted(Thread mainThread);

	/**
	 * Signals when another tasks is enqueued. May be used to implement
	 * waking up of any spins. <br>
	 * <br>
	 * The main thread parameter may be {@code null} if the factory is not yet aware
	 * of the main thread. When it is null, however, it is guaranteed that no future
	 * has been or is being awaited on the main thread.
	 *
	 * @param mainThread the main thread, may be null if not yet known
	 */
	void signalWhenTaskAdded(Thread mainThread);

	/**
	 * Awaits indefinitely. See the class javadoc for implementation guidance.
	 *
	 * @param <T> the type of the future result
	 * @param runQueuedTasks a runnable which will run outstanding tasks
	 * @param future the future to await
	 * @return the awaited value
	 * @throws CompletionException if the future completed exceptionally
	 * @throws CancellationException if the future was cancelled
	 */
	<T> T await(Runnable runQueuedTasks, CentralisedFuture<T> future);

	/**
	 * Awaits indefinitely, in an interruptible fashion. See the class javadoc for
	 * implementation guidance.
	 *
	 * @param <T> the type of the future result
	 * @param runQueuedTasks a runnable which will run outstanding tasks
	 * @param future the future to await
	 * @return the awaited value
	 * @throws InterruptedException if interrupted while waiting
	 * @throws ExecutionException if the future contained an exception
	 * @throws CancellationException if the future was cancelled
	 */
	<T> T awaitInterruptibly(Runnable runQueuedTasks, CentralisedFuture<T> future)
			throws InterruptedException, ExecutionException;

	/**
	 * Awaits until the specified timeout elapses, in an interruptible fashion. See the class
	 * javadoc for implementation guidance.
	 *
	 * @param <T> the type of the future result
	 * @param runQueuedTasks a runnable which will run outstanding tasks
	 * @param future the future to await
	 * @param timeout the timeout, always positive
	 * @param unit the unit of the timeout
	 * @return the awaited value
	 * @throws InterruptedException if interrupted while waiting
	 * @throws TimeoutException if the timeout elapsed
	 * @throws ExecutionException if the future contained an exception
	 * @throws CancellationException if the future was cancelled
	 */
	<T> T awaitUntil(Runnable runQueuedTasks, CentralisedFuture<T> future, long timeout, TimeUnit unit)
			throws InterruptedException, TimeoutException, ExecutionException;

}
