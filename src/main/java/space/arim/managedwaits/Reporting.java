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

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

final class Reporting {

	private Reporting() {}

	/**
	 * Placeholder for value not yet complete
	 *
	 */
	static final Object ABSENT_VALUE = new Object();

	/**
	 * Gets the completed value, or {@code ABSENT_VALUE} if not completed. If completed exceptionally,
	 * throws in accordance with {@link CompletableFuture#join()}. <br>
	 * <br>
	 * This method is used primarily for performance purposes. It avoids the double volatile read
	 * which would be associated with: <br>
	 * <code>if (future.isDone()) { return future.join(); }</code>
	 *
	 * @param <T> the type of the future
	 * @param future the future
	 * @return the completed value or {@code ABSENT_VALUE}
	 * @throws CancellationException if the computation was cancelled
	 * @throws CompletionException if this future completed exceptionally or a completion computation threw an exception
	 */
	@SuppressWarnings("unchecked")
	static <T> T reportJoin(CentralisedFuture<T> future) {
		return future.getNow((T) ABSENT_VALUE);
	}

	/**
	 * Gets the completed value, or {@code ABSENT_VALUE} if not completed. If completed exceptionally,
	 * throws in accordance with {@link CompletableFuture#get()}
	 *
	 * @param <T> the type of the future
	 * @param future the future
	 * @return the completed value or {@code ABSENT_VALUE}
	 * @throws CancellationException if the computation was cancelled
	 * @throws ExecutionException if this future completed exceptionally
	 */
	static <T> T reportGet(CentralisedFuture<T> future) throws ExecutionException {
		try {
			return reportJoin(future);
		} catch (CompletionException ex) {
			Throwable cause = ex.getCause();
			throw new ExecutionException((cause == null) ? ex : cause);
		}
	}

}
