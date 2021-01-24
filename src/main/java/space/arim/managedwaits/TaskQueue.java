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

/**
 * A queue of tasks which may be emptied and run on demand
 *
 */
public interface TaskQueue {

	/**
	 * Adds a task to the queue
	 *
	 * @param task the task
	 * @return true if added successfully, false if the operation somehow failed. If a failure occurs, the
	 * implementation should handle the failure itself; the return value is a mere indicator to the caller.
	 */
	boolean addTask(Runnable task);

	/**
	 * Polls and runs any tasks in the queue. The implementation need not try-catch while calling
	 * any {@code run} methods. <br>
	 * <br>
	 * It may be safely assumed that this method is only called if
	 * {@link DeadlockFreeFutureFactory#isPrimaryThread()} returns true.
	 *
	 */
	void pollAndRunAll();
}
