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
package ch.njol.skript.classes;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.data.DefaultChangers;
import ch.njol.skript.lang.Expression;

/**
 * An interface to declare changeable values. All Expressions implement something similar like this by default, but refuse any change if {@link Expression#acceptChange(ChangeMode)}
 * isn't overridden.
 * <p>
 * Some useful Changers can be found in {@link DefaultChangers}
 * 
 * @see DefaultChangers
 * @see Expression
 */
public interface Changer<T> {

	public static enum ChangeMode {
		ADD, SET, REMOVE, REMOVE_ALL, DELETE, RESET;
	}

	/**
	 * Tests whether this changer supports the given mode, and if yes what type(s) it expects the elements of <code>delta</code> to be.
	 * <p>
	 * Unlike {@link Expression#acceptChange(ChangeMode)} this method must not print errors.
	 * 
	 * @param mode
	 * @return An array of types that {@link #change(Object[], Object[], ChangeMode)} accepts as its <code>delta</code> parameter (which can be arrays to denote that multiple of
	 *         that type are accepted), or null if the given mode is not supported. For {@link ChangeMode#DELETE} and {@link ChangeMode#RESET} this can return any non-null array to
	 *         mark them as supported.
	 */
	@Nullable
	public abstract Class<?>[] acceptChange(ChangeMode mode);

	/**
	 * @param what The objects to change
	 * @param delta An array with one or more instances of one or more of the the classes returned by {@link #acceptChange(ChangeMode)} for the given change mode (null for
	 *            {@link ChangeMode#DELETE} and {@link ChangeMode#RESET}). <b>This can be a Object[], thus casting is not allowed.</b>
	 * @param mode
	 * @throws UnsupportedOperationException (optional) if this method was called on an unsupported ChangeMode.
	 */
	public abstract void change(T[] what, @Nullable Object[] delta, ChangeMode mode);

	public static abstract class ChangerUtils {

		@SuppressWarnings("unchecked")
		public static <T, V> void change(Changer<T> changer, Object[] what, @Nullable Object[] delta, ChangeMode mode) {
			changer.change((T[]) what, delta, mode);
		}

		/**
		 * Tests whether an expression accepts changes of a certain type. If multiple types are given it test for whether any of the types is accepted.
		 * 
		 * @param expression The expression to test
		 * @param mode The ChangeMode to use in the test
		 * @param types The types to test for
		 * @return Whether <tt>e.{@link Expression#change(Event, Object[], ChangeMode) change}(event, type[], mode)</tt> can be used or not.
		 */
		public static boolean acceptsChange(Expression<?> expression, ChangeMode mode, Class<?>... types) {
			Class<?>[] classes = expression.acceptChange(mode);
			if (classes == null)
				return false;
			for (Class<?> type : types) {
				for (Class<?> c : classes) {
					if (c.isArray() ? c.getComponentType().isAssignableFrom(type) : c.isAssignableFrom(type))
						return true;
				}
			}
			return false;
		}

		/**
		 * Prints a Skript.error depending on the changer used for the provided expression.
		 * Useful when providing error messages in acceptChange methods and init methods.
		 * 
		 * @param expression The expression that cannot accept the provided {@link ChangeMode}
		 * @param mode The {@link ChangeMode} that the expression cannot accept.
		 */
		public static void cannotChange(Expression<?> expression, ChangeMode mode) {
			cannotChange(expression, mode, null);
		}

		/**
		 * Prints a Skript.error depending on the changer used for the provided expression.
		 * Useful when providing error messages in acceptChange methods and init methods.
		 * 
		 * @param expression The expression that cannot accept the provided {@link ChangeMode}
		 * @param mode The {@link ChangeMode} that the expression cannot accept.
		 * @param additional An optional string message to append to the end of the error.
		 */
		public static void cannotChange(Expression<?> expression, ChangeMode mode, @Nullable String additional) {
			additional = additional == null ? "" : " " + additional;
			switch (mode) {
				case SET:
					Skript.error("'" + expression.toString(null, false) + "' can't be set to anything." + additional);
					break;
				case DELETE:
					Skript.error("'" + expression.toString(null, false) + "' can't be deleted/cleared." + additional);
					break;
				case REMOVE_ALL:
				case ADD:
					//$FALL-THROUGH$
				case REMOVE:
					Skript.error("'" + expression.toString(null, false) + "' can't have anything " + (mode == ChangeMode.ADD ? "added to" : "removed from") + " it." + additional);
					break;
				case RESET:
					Skript.error("'" + expression.toString(null, false) + "' can't be reset." + additional);
			}
		}

	}

}
