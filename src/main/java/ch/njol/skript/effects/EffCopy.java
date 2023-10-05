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

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Map;

@Name("Copy")
@Description("Copies objects into a variable. When copying a list over to another list, the source list and its sublists are also copied over.")
@Examples({
	"set {_foo::bar} to 1",
	"set {_foo::sublist::foobar} to \"hey\"",
	"copy {_foo::*} to {_copy::*}",
	"broadcast indices of {_copy::*} # bar, sublist",
	"broadcast {_copy::bar} # 1",
	"broadcast {_copy::sublist::foobar} # \"hey!\""
})
@Since("INSERT VERSION")
@Keywords({"copy", "clone"})
public class EffCopy extends Effect {

	static {
		Skript.registerEffect(EffCopy.class, "copy %~objects% [in]to %~objects%");
	}

	private Expression<?> source;
	private Variable<?> destination;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (!(exprs[1] instanceof Variable)) {
			Skript.error("You can only copy objects into variables");
			return false;
		}
		source = exprs[0];
		destination = (Variable<?>) exprs[1];
		if (!source.isSingle() && destination.isSingle()) {
			Skript.error("Cannot copy multiple objects into a single variable");
			return false;
		}
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void execute(Event event) {
		if (!(source instanceof Variable) || source.isSingle()) {
			ChangeMode mode = ChangeMode.SET;
			Object[] clone = (Object[]) Classes.clone(source.getArray(event));
			if (clone.length == 0)
				mode = ChangeMode.DELETE;
			destination.change(event, clone, mode);
			return;
		}
		destination.change(event, null, ChangeMode.DELETE);
		Map<String, Object> source = (Map<String, Object>) ((Variable<Object>) this.source).getRaw(event);
		if (source == null)
			return;
		String target = destination.getName().getSingle(event);
		copy(event, source, target.substring(0, target.length() - 3), destination.isLocal());
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "copy " + source.toString(event, debug) + " into " + destination.toString(event, debug);
	}

	@SuppressWarnings("unchecked")
	private static void copy(Event event, Map<String, Object> source, String targetName, boolean local) {
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			if (entry.getKey() == null)
				continue;
			String node = targetName + Variable.SEPARATOR + entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Map) {
				copy(event, (Map<String, Object>) value, node, local);
				return;
			}
			Variables.setVariable(node, value, event, local);
		}
	}

}
