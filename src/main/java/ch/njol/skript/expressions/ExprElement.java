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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Name("Elements")
@Description({
		"The first, last, range or a random element of a set, e.g. a list variable.",
		"See also: <a href='#ExprRandom'>random</a>"
})
@Examples("broadcast the first 3 elements of {top players::*}")
@Since("2.0, INSERT VERSION (relative to last element, range of elements)")
public class ExprElement extends SimpleExpression<Object> {

	private static final Patterns<PatternInfo> PATTERNS = new Patterns<>(new Object[][]{
		{"[the] (first|:last) element [out] of %objects%", new PatternInfo(ElementType.FIRST_ELEMENT, ElementType.LAST_ELEMENT, true)},
		{"[the] (first|:last) %number% elements [out] of %objects%", new PatternInfo(ElementType.FIRST_X_ELEMENTS, ElementType.LAST_X_ELEMENTS, false)},
		{"[a] random element [out] of %objects%", new PatternInfo(ElementType.RANDOM, true)},
		{"[the] %number%(st|nd|rd|th) [last:[to] last] element [out] of %objects%", new PatternInfo(ElementType.ORDINAL, ElementType.TAIL_END_ORDINAL, false)},
		{"[the] elements (from|between) %number% (to|and) %number% [out] of %objects%", new PatternInfo(ElementType.RANGE, true)}
	});

	private static class PatternInfo {

		private final ElementType first, last;
		private final boolean single;

		private PatternInfo(ElementType first, ElementType last, boolean single) {
			this.first = first;
			this.last = last;
			this.single = single;
		}

		private PatternInfo(ElementType type, boolean single) {
			this(type, type, single);
		}

	}

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
	private	@Nullable Expression<Number> number, to;
	private ElementType type;
	private boolean single;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		PatternInfo info = PATTERNS.getInfo(matchedPattern);
		expr = LiteralUtils.defendExpression(exprs[exprs.length - 1]);
		switch (info.first) {
			case RANGE:
				to = (Expression<Number>) exprs[1];
				//$FALL-THROUGH$
			case FIRST_X_ELEMENTS:
			case ORDINAL:
				number = (Expression<Number>) exprs[0];
				break;
			default:
				number = null;
				break;
		}
		type = parseResult.hasTag("last") ? info.last : info.first;
		single = info.single;
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
			case FIRST_ELEMENT:
				element = iterator.next();
				break;
			case LAST_ELEMENT:
				element = Iterators.getLast(iterator);
				break;
			case RANDOM:
				Object[] allIterValues = Iterators.toArray(iterator, Object.class);
				element = CollectionUtils.getRandom(allIterValues);
				break;
			case ORDINAL:
				assert number != null;
				Number number = this.number.getSingle(event);
				if (number == null)
					return null;
				int ordinal = number.intValue();
				if (ordinal <= 0)
					return null;
				Iterators.advance(iterator, ordinal - 1);
				if (!iterator.hasNext())
					return null;
				element = iterator.next();
				break;
			case TAIL_END_ORDINAL:
				assert this.number != null;
				number = this.number.getSingle(event);
				if (number == null)
					return null;
				ordinal = number.intValue();
				allIterValues = Iterators.toArray(iterator, Object.class);
				if (ordinal <= 0 || ordinal > allIterValues.length)
					return null;
				element = allIterValues[allIterValues.length - ordinal];
				break;
			case FIRST_X_ELEMENTS:
				assert this.number != null;
				int size = this.number.getOptionalSingle(event).orElse(0).intValue();
				if (size <= 0)
					return null;
				List<Object> list = new ArrayList<>(size);
				int i = 0;
				while (iterator.hasNext() && i++ < size)
					list.add(iterator.next());
				Object[] elementArray = (Object[]) Array.newInstance(getReturnType(), 0);
				return list.toArray(elementArray);
			case LAST_X_ELEMENTS:
				assert this.number != null;
				number = this.number.getSingle(event);
				if (number == null)
					return null;
				allIterValues = Iterators.toArray(iterator, Object.class);
				size = Math.min(number.intValue(), allIterValues.length);
				if (size <= 0)
					return null;
				elementArray = (Object[]) Array.newInstance(getReturnType(), size);
				System.arraycopy(allIterValues, allIterValues.length - size, elementArray, 0, size);
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
		return (Expression<? extends R>) exprElement;
	}

	@Override
	public boolean isSingle() {
		return single;
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
				assert number != null;
				prefix = "the first " + number.toString(event, debug);
				break;
			case LAST_X_ELEMENTS:
				assert number != null;
				prefix = "the last " + number.toString(event, debug);
				break;
			case RANDOM:
				prefix = "a random";
				break;
			case ORDINAL:
			case TAIL_END_ORDINAL:
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
				if (type == ElementType.TAIL_END_ORDINAL)
					prefix += " last";
				break;
			case RANGE:
				assert number != null && to != null;
				return "the elements from " + number.toString(event, debug) + " to " + to.toString(event, debug) + " of " + expr.toString(event, debug);
			default:
				throw new IllegalStateException();
		}
		return prefix + (single ? " element" : " elements") + " of " + expr.toString(event, debug);
	}

}
