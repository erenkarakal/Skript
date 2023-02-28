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
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import com.google.common.collect.Iterators;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;

@Name("Elements")
@Description({
		"The first, last, range or a random element of a set, e.g. a list variable.",
		"See also: <a href='#ExprRandom'>random</a>"
})
@Examples("broadcast the first 3 elements of {top players::*}")
@Since("2.0, INSERT VERSION (relative to last element, range of elements)")
public class ExprElement extends SimpleExpression<Object> {

	private static final String objects = " [out] of %objects%";
	private static final Patterns<ElementType> PATTERNS = new Patterns<>(new Object[][]{
		{"[the] (first|:last) element" + objects, ElementType.SINGLE},
		{"[the] (first|:last) %number% elements" + objects, ElementType.MULTIPLE},
		{"[a] random element" + objects, ElementType.RANDOM},
		{"[the] %number%(st|nd|rd|th) [last:[to] last] element" + objects, ElementType.ORDINAL},
		{"[the] elements (from|between) %number% (to|and) %number%" + objects, ElementType.RANGE}
	});

	static {
		Skript.registerExpression(ExprElement.class, Object.class, ExpressionType.PROPERTY, PATTERNS.getPatterns());
	}

	private enum ElementType {
		SINGLE, MULTIPLE, RANDOM, ORDINAL, RANGE
	}

	private Expression<?> expr;
	private	@Nullable Expression<Number> number, to;
	private ElementType type;
	private boolean last;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		type = PATTERNS.getInfo(matchedPattern);
		expr = LiteralUtils.defendExpression(exprs[exprs.length - 1]);
		switch (type) {
			case RANGE:
				to = (Expression<Number>) exprs[1];
				//$FALL-THROUGH$
			case MULTIPLE:
			case ORDINAL:
				number = (Expression<Number>) exprs[0];
				break;
			default:
				number = null;
				break;
		}
		last = parseResult.hasTag("last");
		return LiteralUtils.canInitSafely(expr);
	}

	@Override
	@Nullable
	protected Object[] get(Event event) {
		Iterator<?> iterator = expr.iterator(event);
		if (iterator == null || !iterator.hasNext())
			return null;
		Object element = null;
		switch (type) {
			case SINGLE:
				element = last ? Iterators.getLast(iterator) : iterator.next();
				break;
			case RANDOM:
				Object[] allIterValues = Iterators.toArray(iterator, Object.class);
				element = CollectionUtils.getRandom(allIterValues);
				break;
			case ORDINAL:
				allIterValues = Iterators.toArray(iterator, Object.class);
				assert number != null;
				Number number = this.number.getSingle(event);
				if (number == null)
					return null;
				int ordinal = number.intValue();
				if (ordinal <= 0 || ordinal > allIterValues.length)
					return null;
				element = allIterValues[last ? allIterValues.length - ordinal : ordinal - 1];
				break;
			case MULTIPLE:
				assert this.number != null;
				number = this.number.getSingle(event);
				if (number == null)
					return null;
				allIterValues = Iterators.toArray(iterator, Object.class);
				int size = Math.min(number.intValue(), allIterValues.length);
				if (size <= 0)
					return null;
				Object[] elementArray = (Object[]) Array.newInstance(getReturnType(), size);
				System.arraycopy(allIterValues, last ? allIterValues.length - size : 0, elementArray, 0, size);
				return elementArray;
			case RANGE:
				assert this.number != null;
				assert to != null;
				Number from = this.number.getSingle(event);
				Number to = this.to.getSingle(event);
				if (from == null || to == null)
					return null;

				allIterValues = Iterators.toArray(iterator, Object.class);
				int min = Math.min(from.intValue(), to.intValue()) - 1;
				int max = Math.max(from.intValue(), to.intValue());
				min = Math.max(min, 0);
				max = Math.min(Math.max(max, 0), allIterValues.length);

				size = max - min;
				if (size <= 0)
					return null;

				elementArray = (Object[]) Array.newInstance(getReturnType(), size);
				System.arraycopy(allIterValues, min, elementArray, 0, size);
				return elementArray;
		}
		Object[] elementArray = (Object[]) Array.newInstance(getReturnType(), 1);
		elementArray[0] = element;
		return elementArray;
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public <R> Expression<? extends R> getConvertedExpression(Class<R>... to) {
		Expression<? extends R> convExpr = expr.getConvertedExpression(to);
		if (convExpr == null)
			return null;

		ExprElement exprElement = new ExprElement();
		exprElement.expr = convExpr;
		exprElement.number = this.number;
		exprElement.to = this.to;
		exprElement.type = this.type;
		exprElement.last = this.last;
		return (Expression<? extends R>) exprElement;
	}

	@Override
	public boolean isSingle() {
		return !(type == ElementType.MULTIPLE || type == ElementType.RANGE);
	}

	@Override
	public Class<?> getReturnType() {
		return expr.getReturnType();
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String prefix = "";
		switch (type) {
			case SINGLE:
				prefix = last ? "the last" : "the first";
				break;
			case MULTIPLE:
				assert number != null;
				prefix = (last ? "the last " : "the first ") + number.toString(event, debug);
				break;
			case RANDOM:
				prefix = "a random";
				break;
			case ORDINAL:
				assert number != null;
				prefix = "the ";
				// Proper ordinal number
				if (number instanceof Literal) {
					Number number = ((Literal<Number>) this.number).getSingle();
					if (number == null)
						prefix += this.number.toString(event, debug) + "th";
					else
						prefix += StringUtils.fancyOrderNumber(number.intValue());
				} else {
					prefix += number.toString(event, debug) + "th";
				}
				if (last)
					prefix += " last";
				break;
			case RANGE:
				assert number != null && to != null;
				return "the elements from " + number.toString(event, debug) + " to " + to.toString(event, debug) + " of " + expr.toString(event, debug);
			default:
				throw new IllegalStateException();
		}
		return prefix + (isSingle() ? " element" : " elements") + " of " + expr.toString(event, debug);
	}

}
