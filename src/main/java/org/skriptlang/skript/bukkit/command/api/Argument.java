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
package org.skriptlang.skript.bukkit.command.api;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.WeakHashMap;

public class Argument<T> {

	@Nullable
	private final String name;
	private final Class<T> type;

	private final boolean optional;
	private final boolean single;

	@Nullable
	private final Expression<? extends T> defaultValue;

	// TODO in the future this map should be replaced and argument values
	// should be stored on a argument-value map on each execution's "TriggerContext"
	private final Map<ScriptCommandEvent, T[]> valueMap = new WeakHashMap<>();

	private Argument(
		@Nullable String name, Class<T> type,
		boolean optional, boolean single,
		@Nullable Expression<? extends T> defaultValue
	) {
		this.name = name;
		this.type = type;
		this.optional = optional;
		this.single = single;
		this.defaultValue = defaultValue;
	}

	@ApiStatus.Internal
	@Nullable
	@SuppressWarnings("unchecked")
	public static <T> Argument<T> of(
		@Nullable String name, Class<T> type,
		boolean optional, boolean single,
		@Nullable String defaultExpression
	) {
		if (name != null && !Variable.isValidVariableName(name, false, false)) {
			Skript.error("An argument's name must be a valid variable name, and cannot be a list variable.");
			return null;
		}

		// parse the default expression (if it was provided)
		Expression<? extends T> parsedDefaultExpression = null;
		if (defaultExpression != null) {
			try (RetainingLogHandler log = new RetainingLogHandler()) {

				if (defaultExpression.startsWith("%") && defaultExpression.endsWith("%")) {
					// attempt to parse this as an expression

					parsedDefaultExpression = new SkriptParser(
						defaultExpression.substring(1, defaultExpression.length() - 1),
						SkriptParser.PARSE_EXPRESSIONS,
						ParseContext.COMMAND
					).parseExpression(type);

				} else {
					// attempt to parse this as a literal

					if (type == String.class) { // this is a string literal
						if (defaultExpression.startsWith("\"") && defaultExpression.endsWith("\"")) {
							parsedDefaultExpression =
								(Expression<? extends T>) VariableString.newInstance(defaultExpression.substring(1, defaultExpression.length() - 1));
						} else {
							parsedDefaultExpression =
								(Expression<? extends T>) new SimpleLiteral<>(defaultExpression, false);
						}
					} else { // any other kind of literal
						// TODO is ParseContext.DEFAULT correct?
						parsedDefaultExpression = new SkriptParser(
							defaultExpression, SkriptParser.PARSE_LITERALS, ParseContext.DEFAULT
						).parseExpression(type);
					}

				}

				if (parsedDefaultExpression == null) {
					log.printErrors("Can't understand this expression: '" + defaultExpression + "'");
					return null;
				}

				log.printLog();
			}
		}

		optional = defaultExpression != null || optional;
		return new Argument<>(name, type, optional, single, parsedDefaultExpression);
	}

	@Nullable
	public String getName() {
		return name;
	}

	public Class<T> getType() {
		return type;
	}

	public boolean isOptional() {
		return optional;
	}

	public boolean isSingle() {
		return single;
	}

	public void setValues(ScriptCommandEvent event, T[] values) {
		valueMap.put(event, values);
	}

	@Nullable
	public T[] getValues(ScriptCommandEvent event) {
		return valueMap.getOrDefault(event, defaultValue != null ? defaultValue.getArray(event) : null);
	}

}
