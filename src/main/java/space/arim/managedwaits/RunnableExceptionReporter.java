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

class RunnableExceptionReporter implements Runnable {

	private final Runnable command;

	private static final System.Logger logger = System.getLogger(RunnableExceptionReporter.class.getName());

	RunnableExceptionReporter(Runnable command) {
		this.command = command;
	}

	@Override
	public void run() {
		try {
			command.run();
		} catch (Exception ex) {
			logger.log(System.Logger.Level.WARNING, "Exception while executing command " + command, ex);
		}
	}
}
