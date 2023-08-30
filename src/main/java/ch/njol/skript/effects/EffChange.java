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
package ch.njol.skript.effects;

import java.util.Arrays;
import java.util.logging.Level;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.script.ScriptWarning;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.log.CountingLogHandler;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;

/**
 * @author Peter Güttinger
 */
@Name("Change: Set/Add/Remove/Delete/Reset")
@Description("A very general effect that can change many <a href='./expressions'>expressions</a>. Many expressions can only be set and/or deleted, while some can have things added to or removed from them.")
@Examples({"# set:",
		"Set the player's display name to \"&lt;red&gt;%name of player%\"",
		"set the block above the victim to lava",
		"# add:",
		"add 2 to the player's health # preferably use '<a href='#EffHealth'>heal</a>' for this",
		"add argument to {blacklist::*}",
		"give a diamond pickaxe of efficiency 5 to the player",
		"increase the data value of the clicked block by 1",
		"# remove:",
		"remove 2 pickaxes from the victim",
		"subtract 2.5 from {points::%uuid of player%}",
		"# remove all:",
		"remove every iron tool from the player",
		"remove all minecarts from {entitylist::*}",
		"# delete:",
		"delete the block below the player",
		"clear drops",
		"delete {variable}",
		"# reset:",
		"reset walk speed of player",
		"reset chunk at the targeted block"})
@Since("1.0 (set, add, remove, delete), 2.0 (remove all)")
public class EffChange extends Effect {
	private static Patterns<ChangeMode> patterns = new Patterns<>(new Object[][] {
			{"(add|give) %objects% to %~objects%", ChangeMode.ADD},
			{"increase %~objects% by %objects%", ChangeMode.ADD},
			{"give %~objects% %objects%", ChangeMode.ADD},
			
			{"set %~objects% to %objects%", ChangeMode.SET},
			
			{"remove (all|every) %objects% from %~objects%", ChangeMode.REMOVE_ALL},
			
			{"(remove|subtract) %objects% from %~objects%", ChangeMode.REMOVE},
			{"reduce %~objects% by %objects%", ChangeMode.REMOVE},
			
			{"(delete|clear) %~objects%", ChangeMode.DELETE},
			
			{"reset %~objects%", ChangeMode.RESET}
	});
	
	static {
		Skript.registerEffect(EffChange.class, patterns.getPatterns());
	}
	
	@SuppressWarnings("null")
	private Expression<?> changed;
	@Nullable
	private Expression<?> changer = null;
	
	@SuppressWarnings("null")
	private ChangeMode mode;
	
	private boolean single;
	
//	private Changer<?, ?> c = null;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parser) {
		mode = patterns.getInfo(matchedPattern);
		
		switch (mode) {
			case ADD:
				if (matchedPattern == 0) {
					changer = exprs[0];
					changed = exprs[1];
				} else {
					changer = exprs[1];
					changed = exprs[0];
				}
				break;
			case SET:
				changer = exprs[1];
				changed = exprs[0];
				break;
			case REMOVE_ALL:
				changer = exprs[0];
				changed = exprs[1];
				break;
			case REMOVE:
				if (matchedPattern == 5) {
					changer = exprs[0];
					changed = exprs[1];
				} else {
					changer = exprs[1];
					changed = exprs[0];
				}
				break;
			case DELETE:
				changed = exprs[0];
				break;
			case RESET:
				changed = exprs[0];
		}

		Class<?>[] types;
		String what;
		try (CountingLogHandler logger = new CountingLogHandler(Level.SEVERE)) {
			types = changed.acceptChange(mode);
			ClassInfo<?> c = Classes.getSuperClassInfo(changed.getReturnType());
			Changer<?> changer = c.getChanger();
			what = changer == null || !Arrays.equals(changer.acceptChange(mode), types) ? changed.toString(null, false) : c.getName().withIndefiniteArticle();
			/**
			 * If the logger caught any messages while collecting acceptChange types,
			 * 		there is likely other errors and they take priority.
			 */
			if (types == null && logger.getCount() > 0)
				return false;
		}
		if (types == null) {
			cannotChange(what, changed, mode);
			return false;
		}

		Class<?>[] rs2 = new Class<?>[types.length];
		for (int i = 0; i < types.length; i++)
			rs2[i] = types[i].isArray() ? types[i].getComponentType() : types[i];
		boolean allSingle = Arrays.equals(types, rs2);

		Expression<?> ch = changer;
		if (ch != null) {
			Expression<?> v = null;
			final ParseLogHandler log = SkriptLogger.startParseLogHandler();
			try {
				for (final Class<?> r : types) {
					log.clear();
					if ((r.isArray() ? r.getComponentType() : r).isAssignableFrom(ch.getReturnType())) {
						v = ch.getConvertedExpression(Object.class);
						break; // break even if v == null as it won't convert to Object apparently
					}
				}
				if (v == null)
					v = ch.getConvertedExpression((Class<Object>[]) rs2);
				if (v == null) {
					if (log.hasError()) {
						log.printError();
						return false;
					}
					log.clear();
					log.stop();
					final Class<?>[] r = new Class[types.length];
					for (int i = 0; i < types.length; i++)
						r[i] = types[i].isArray() ? types[i].getComponentType() : types[i];
					if (r.length == 1 && r[0] == Object.class)
						Skript.error("Can't understand this expression: " + changer, ErrorQuality.NOT_AN_EXPRESSION);
					else if (mode == ChangeMode.SET)
						Skript.error(what + " can't be set to " + changer + " because the latter is " + SkriptParser.notOfType(r));
					else
						Skript.error(changer + " can't be " + (mode == ChangeMode.ADD ? "added to" : "removed from") + " " + what + " because the former is " + SkriptParser.notOfType(r));
					log.printError();
					return false;
				}
				log.printLog();
			} finally {
				log.stop();
			}
			
			Class<?> x = Utils.getSuperType(rs2);
			single = allSingle;
			for (int i = 0; i < types.length; i++) {
				if (rs2[i].isAssignableFrom(v.getReturnType())) {
					single = !types[i].isArray();
					x = rs2[i];
					break;
				}
			}
			assert x != null;
			changer = ch = v;
			
			if (!ch.isSingle() && single) {
				if (mode == ChangeMode.SET)
					Skript.error(changed + " can only be set to one " + Classes.getSuperClassInfo(x).getName() + ", not more.");
				else
					Skript.error("only one " + Classes.getSuperClassInfo(x).getName() + " can be " + (mode == ChangeMode.ADD ? "added to" : "removed from") + " " + changed + ", not more.");
				return false;
			}
			
			if (changed instanceof Variable && !((Variable<?>) changed).isLocal() && (mode == ChangeMode.SET || ((Variable<?>) changed).isList() && mode == ChangeMode.ADD)) {
				final ClassInfo<?> ci = Classes.getSuperClassInfo(ch.getReturnType());
				if (ci.getC() != Object.class && ci.getSerializer() == null && ci.getSerializeAs() == null && !SkriptConfig.disableObjectCannotBeSavedWarnings.value()) {
					if (getParser().isActive() && !getParser().getCurrentScript().suppressesWarning(ScriptWarning.VARIABLE_SAVE)) {
						Skript.warning(ci.getName().withIndefiniteArticle() + " cannot be saved, i.e. the contents of the variable " + changed + " will be lost when the server stops.");
					}
				}
			}
		}
		return true;
	}
	
	@Override
	protected void execute(Event e) {
		Object[] delta = changer == null ? null : changer.getArray(e);
		delta = changer == null ? delta : changer.beforeChange(changed, delta);

		if ((delta == null || delta.length == 0) && (mode != ChangeMode.DELETE && mode != ChangeMode.RESET)) {
			if (mode == ChangeMode.SET && changed.acceptChange(ChangeMode.DELETE) != null)
				changed.change(e, null, ChangeMode.DELETE);
			return;
		}
		changed.change(e, delta, mode);
	}
	
	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		final Expression<?> changer = this.changer;
		switch (mode) {
			case ADD:
				assert changer != null;
				return "add " + changer.toString(e, debug) + " to " + changed.toString(e, debug);
			case SET:
				assert changer != null;
				return "set " + changed.toString(e, debug) + " to " + changer.toString(e, debug);
			case REMOVE:
				assert changer != null;
				return "remove " + changer.toString(e, debug) + " from " + changed.toString(e, debug);
			case REMOVE_ALL:
				assert changer != null;
				return "remove all " + changer.toString(e, debug) + " from " + changed.toString(e, debug);
			case DELETE:
				return "delete/clear " + changed.toString(e, debug);
			case RESET:
				return "reset " + changed.toString(e, debug);
		}
		assert false;
		return "";
	}

	/**
	 * This is duplicated from {@link Changer.ChangerUtils} but checks acceptChange which the ChangerUtils class cannot because of recursion.
	 * Prints a Skript.error depending on the changer used for the provided expression.
	 * Useful when providing error messages in acceptChange methods and init methods.
	 * 
	 * @param what The string that represents what cannot be changed.
	 * @param expression The expression that cannot accept the provided {@link ChangeMode}
	 * @param mode The {@link ChangeMode} that the expression cannot accept.
	 */
	private void cannotChange(String what, Expression<?> expression, ChangeMode mode) {
		switch (mode) {
			case SET:
				Skript.error("'" + what + " can't be set to anything.");
				break;
			case DELETE:
				if (expression.acceptChange(ChangeMode.RESET) != null)
					Skript.error("'" + what + "' can't be deleted/cleared. It can however be reset which might result in the desired effect.");
				else
					Skript.error("'" + what + "' can't be deleted/cleared.");
				break;
			case REMOVE_ALL:
				if (expression.acceptChange(ChangeMode.REMOVE) != null) {
					Skript.error("'" + what + "' can't have 'all of something' removed from it. Use 'remove' instead of 'remove all' to fix this.");
					break;
				}
				//$FALL-THROUGH$
			case ADD:
			case REMOVE:
				Skript.error("'" + what + "' can't have anything " + (mode == ChangeMode.ADD ? "added to" : "removed from") + " it.");
				break;
			case RESET:
				if (expression.acceptChange(ChangeMode.DELETE) != null)
					Skript.error("'" + what + "' can't be reset. It can however be deleted which might result in the desired effect.");
				else
					Skript.error("'" + what + "' can't be reset.");
		}
	}

}
