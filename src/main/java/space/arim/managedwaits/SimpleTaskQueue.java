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

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * {@link TaskQueue} implementation backed by a {@link ConcurrentLinkedQueue}
 *
 */
public final class SimpleTaskQueue implements TaskQueue {

	private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

	@Override
	public boolean addTask(Runnable task) {
		return tasks.offer(task); // always true since CLQ is unbounded
	}

	@Override
	public void pollAndRunAll() {
		Runnable syncTask;
		while ((syncTask = tasks.poll()) != null) {
			syncTask.run();
		}
	}

}
