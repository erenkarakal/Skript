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
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Name("Elements")
@Description({
		"The first, last, range or a random element of a set, e.g. a list variable.",
		"See also: <a href='#ExprRandom'>random</a>"
})
@Examples("broadcast the first 3 elements of {top players::*}")
@Since("2.0, 2.7 (relative to last element), INSERT VERSION (range of elements)")
public class ExprElement extends SimpleExpression<Object> {

	private static final Patterns<ElementType[]> PATTERNS = new Patterns<>(new Object[][]{
		{"[the] (first|:last) element [out] of %objects%", new ElementType[] {ElementType.FIRST_ELEMENT, ElementType.LAST_ELEMENT}},
		{"[the] (first|:last) %number% elements [out] of %objects%", new ElementType[] {ElementType.FIRST_X_ELEMENTS, ElementType.LAST_X_ELEMENTS}},
		{"[a] random element [out] of %objects%", new ElementType[] {ElementType.RANDOM}},
		{"[the] %number%(st|nd|rd|th) [last:[to] last] element [out] of %objects%", new ElementType[] {ElementType.ORDINAL, ElementType.TAIL_END_ORDINAL}},
		{"[the] elements (from|between) %number% (to|and) %number% [out] of %objects%", new ElementType[] {ElementType.RANGE}}
	});

	static {
		Skript.registerExpression(ExprElement.class, Object.class, ExpressionType.PROPERTY, PATTERNS.getPatterns());
	}

	private enum ElementType {
		FIRST_ELEMENT,
		LAST_ELEMENT,
		FIRST_X_ELEMENTS,
		LAST_X_ELEMENTS,
		RANDOM,
		ORDINAL,
		TAIL_END_ORDINAL,
		RANGE
	}

	private Expression<?> expr;
	private	@Nullable Expression<Number> startIndex, endIndex;
	private ElementType type;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		ElementType[] types = PATTERNS.getInfo(matchedPattern);
		expr = LiteralUtils.defendExpression(exprs[exprs.length - 1]);
		switch (types[0]) {
			case RANGE:
				endIndex = (Expression<Number>) exprs[1];
				//$FALL-THROUGH$
			case FIRST_X_ELEMENTS:
			case ORDINAL:
				startIndex = (Expression<Number>) exprs[0];
				break;
			default:
				startIndex = null;
				break;
		}
		type = types[parseResult.hasTag("last") ? 1 : 0];
		return LiteralUtils.canInitSafely(expr);
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked, rawtypes")
	protected Object[] get(Event event) {
		Iterator<?> iterator = expr.iterator(event);
		if (iterator == null || !iterator.hasNext())
			return null;
		Object element = null;
		int startIndex = 0;
		int endIndex = 0;
		if (this.startIndex != null) {
			Number number = this.startIndex.getSingle(event);
			if (number == null)
				return null;
			startIndex = number.intValue();
			if (startIndex <= 0 && type != ElementType.RANGE)
				return null;
		}
		if (this.endIndex != null) {
			Number number = this.endIndex.getSingle(event);
			if (number == null)
				return null;
			endIndex = number.intValue();
		}
		switch (type) {
			case FIRST_ELEMENT:
				element = iterator.next();
				break;
			case LAST_ELEMENT:
				element = Iterators.getLast(iterator);
				break;
			case RANDOM:
				Object[] allElements = Iterators.toArray(iterator, Object.class);
				element = CollectionUtils.getRandom(allElements);
				break;
			case ORDINAL:
				Iterators.advance(iterator, startIndex - 1);
				if (!iterator.hasNext())
					return null;
				element = iterator.next();
				break;
			case TAIL_END_ORDINAL:
				allElements = Iterators.toArray(iterator, Object.class);
				if (startIndex > allElements.length)
					return null;
				element = allElements[allElements.length - startIndex];
				break;
			case FIRST_X_ELEMENTS:
				return Iterators.toArray((Iterator) Iterators.limit(iterator, startIndex), getReturnType());
			case LAST_X_ELEMENTS:
				allElements = Iterators.toArray((Iterator) iterator, getReturnType());
				startIndex = Math.min(startIndex, allElements.length);
				return ArrayUtils.subarray(allElements, allElements.length - startIndex, allElements.length);
			case RANGE:
				allElements = Iterators.toArray((Iterator) iterator, getReturnType());
				boolean reverse = startIndex > endIndex;
				int from = Math.min(startIndex, endIndex) - 1;
				int to = Math.max(startIndex, endIndex);
				from = Math.max(from, 0);
				to = Math.min(Math.max(to, 0), allElements.length);
				Object[] elements = ArrayUtils.subarray(allElements, from, to);
				if (reverse)
					ArrayUtils.reverse(elements);
				return elements;
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
		exprElement.startIndex = startIndex;
		exprElement.endIndex = this.endIndex;
		exprElement.type = this.type;
		return (Expression<? extends R>) exprElement;
	}

	@Override
	public boolean isSingle() {
		return type != ElementType.FIRST_X_ELEMENTS && type != ElementType.LAST_X_ELEMENTS && type != ElementType.RANGE;
	}

	@Override
	public Class<?> getReturnType() {
		return expr.getReturnType();
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String prefix = "";
		switch (type) {
			case FIRST_ELEMENT:
				prefix = "the first";
				break;
			case LAST_ELEMENT:
				prefix = "the last";
				break;
			case FIRST_X_ELEMENTS:
				assert startIndex != null;
				prefix = "the first " + startIndex.toString(event, debug);
				break;
			case LAST_X_ELEMENTS:
				assert startIndex != null;
				prefix = "the last " + startIndex.toString(event, debug);
				break;
			case RANDOM:
				prefix = "a random";
				break;
			case ORDINAL:
			case TAIL_END_ORDINAL:
				assert startIndex != null;
				prefix = "the ";
				// Proper ordinal number
				if (startIndex instanceof Literal) {
					Number number = ((Literal<Number>) startIndex).getSingle();
					if (number == null)
						prefix += startIndex.toString(event, debug) + "th";
					else
						prefix += StringUtils.fancyOrderNumber(number.intValue());
				} else {
					prefix += startIndex.toString(event, debug) + "th";
				}
				if (type == ElementType.TAIL_END_ORDINAL)
					prefix += " last";
				break;
			case RANGE:
				assert startIndex != null && endIndex != null;
				return "the elements from " + startIndex.toString(event, debug) + " to " + endIndex.toString(event, debug) + " of " + expr.toString(event, debug);
			default:
				throw new IllegalStateException();
		}
		return prefix + (isSingle() ? " element" : " elements") + " of " + expr.toString(event, debug);
	}

}
