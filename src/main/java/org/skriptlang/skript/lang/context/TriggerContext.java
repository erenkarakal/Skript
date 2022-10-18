/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package org.skriptlang.skript.lang.context;

/**
 * TriggerContext is used to for providing essential information for {@link org.skriptlang.skript.lang.SyntaxElement}s
 *  that depend on specific information during runtime execution.
 * In cases where no specific context is necessary, {@link #dummy()} context is available.
 */
public interface TriggerContext {

	/**
	 * @return A name representing this context.
	 */
	String getName();

	/**
	 * @return A method to obtain a default context implementation (where no specific context is actually needed).
	 */
	static TriggerContext dummy() {
		return new DummyContext();
	}

}
