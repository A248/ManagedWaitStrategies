/* 
 * ArimAPI-env
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * ArimAPI-env is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimAPI-env is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimAPI-env. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.managedwaits;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class FactoryImpl extends DeadlockFreeFutureFactory implements ExtensionContext.Store.CloseableResource {

	private final ScheduledExecutorService mainThreadExecutor;
	private final Thread mainThread;
	private final ScheduledFuture<?> task;
	
	private FactoryImpl(TaskQueue taskQueue, ManagedWaitStrategy waitStrategy,
						Thread mainThread, ScheduledExecutorService mainThreadExecutor, ScheduledFuture<?> task) {
		super(taskQueue, waitStrategy);
		this.mainThread = mainThread;
		this.mainThreadExecutor = mainThreadExecutor;
		this.task = task;
	}

	static FactoryImpl create(ManagedWaitStrategy waitStrategy) {
		ScheduledExecutorService mainThreadExecutor = Executors.newScheduledThreadPool(1,
				(runnable) -> new Thread(runnable, "true-main-thread"));
		Thread mainThread;
		try {
			mainThread = mainThreadExecutor.submit(Thread::currentThread).get();
		} catch (InterruptedException | ExecutionException ex) {
			throw Assertions.<RuntimeException>fail(ex);
		}
		TaskQueue taskQueue = new SimpleTaskQueue();
		ScheduledFuture<?> task = mainThreadExecutor.scheduleWithFixedDelay(
				taskQueue::pollAndRunAll, 0L, 100L, TimeUnit.MILLISECONDS);
		return new FactoryImpl(taskQueue, waitStrategy, mainThread, mainThreadExecutor, task);
	}

	ScheduledExecutorService mainThreadExecutor() {
		return mainThreadExecutor;
	}
	
	@Override
	public boolean isPrimaryThread() {
		return Thread.currentThread() == mainThread;
	}

	@Override
	public Thread getPrimaryThread() {
		return mainThread;
	}

	@Override
	public void close() throws Throwable {
		task.cancel(false);
		mainThreadExecutor.shutdown();
		assertTrue(mainThreadExecutor.awaitTermination(1L, TimeUnit.SECONDS));
	}

}
