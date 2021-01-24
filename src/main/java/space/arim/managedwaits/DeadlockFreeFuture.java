
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
import space.arim.omnibus.util.concurrent.impl.BaseCentralisedFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static space.arim.managedwaits.Reporting.ABSENT_VALUE;
import static space.arim.managedwaits.Reporting.reportGet;
import static space.arim.managedwaits.Reporting.reportJoin;

class DeadlockFreeFuture<T> extends BaseCentralisedFuture<T> {

	private final DeadlockFreeFutureFactory factory;
	
	private boolean dontSignalChildFuture;
	
	/**
	 * Creates without signalling completion
	 * 
	 * @param factory the deadlock free future factory
	 * @param sig parameter used to distinguish signature 
	 */
	private DeadlockFreeFuture(DeadlockFreeFutureFactory factory, @SuppressWarnings("unused") Void sig) {
		super(factory.trustedSyncExecutor);
		this.factory = factory;
	}
	
	DeadlockFreeFuture(DeadlockFreeFutureFactory factory) {
		this(factory, null);
		if (factory.requireSignalWhenFutureCompleted()) {
			whenCompleteSignal();
		}
	}
	
	/*
	 * Completion signalling
	 */
	
	@Override
	public <U> CentralisedFuture<U> newIncompleteFuture() {
		DeadlockFreeFuture<U> childFuture = new DeadlockFreeFuture<>(factory, null);

		if (factory.requireSignalWhenFutureCompleted()) {
			synchronized (this) {
				if (!dontSignalChildFuture) {
					childFuture.whenCompleteSignal();
				}
			}
		}
		return childFuture;
	}
	
	private synchronized void whenCompleteSignal() {
		dontSignalChildFuture = true;
		super.whenComplete((ignore1, ignore2) -> factory.signalFutureCompleted());
		dontSignalChildFuture = false;
	}
	
	// Managed waits
	
	@Override
	public T join() {
		if (!factory.isPrimaryThread()) {
			return super.join();
		}
		T result;
		if ((result = reportJoin(this)) != ABSENT_VALUE) {		// if (isDone()) {
			return result;										// return super.join(); }
		}
		return factory.await(this);
	}
	
	@Override
	public T get() throws InterruptedException, ExecutionException {
		if (!factory.isPrimaryThread()) {
			return super.get();
		}
		T result;
		if ((result = reportGet(this)) != ABSENT_VALUE) {	// if (isDone()) {
			return result;									// return super.get(); }
		}
		return factory.awaitInterruptibly(this);
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (!factory.isPrimaryThread()) {
			return super.get(timeout, unit);
		}
		T result;
		if ((result = reportGet(this)) != ABSENT_VALUE) {	// if (isDone()) {
			return result;									// return super.get(); }
		}
		if (timeout <= 0L) {
			throw new TimeoutException();
		}
		return factory.awaitUntil(this, timeout, unit);
	}

}
