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
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.apache.commons.lang.SerializationUtils;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Name("Script Options")
@Description("It returns one or more options from any script.")
@Examples({
	"options:",
	"\ttest: Hello World",
	"",
	"command /test:",
	"\ttrigger:",
	"\t\tsend \"The option is %skript option \"test\"%\" in \"test.sk\""
})
@Since("INSERT VERSION")
public class ExprOption extends SimpleExpression<String> {

	static {
		Skript.registerExpression(ExprOption.class, String.class, ExpressionType.COMBINED, "[the] [s(c|k)ript] option[s] %strings% (from|in) [s(k|c)ript] %string%");
	}

	public static void updateOptions(String scriptName, Map<String, String> options){
		SKRIPT_OPTIONS.put(scriptName, (Map<String, String>) SerializationUtils.clone(options));
	}

	private final static Map<String, Map<String, String>> SKRIPT_OPTIONS = new HashMap<>();

	private Expression<String> requestedOptions;
	private Expression<String> skriptName;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		requestedOptions = (Expression<String>) exprs[0];
		skriptName = (Expression<String>) exprs[1];
		return true;
	}

	@Override
	protected String[] get(final Event e) {
		String key = skriptName.getSingle(e);
		key = key.endsWith(".sk") ? key : key + ".sk";
		List<String> result = new ArrayList<>();
		for (String option : requestedOptions.getAll(e)) {
			if (SKRIPT_OPTIONS.containsKey(key) && SKRIPT_OPTIONS.get(key).containsKey(option))
				result.add(SKRIPT_OPTIONS.get(key).get(option));
		}
		return result.toArray(new String[result.size()]);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<String> getReturnType() {
		return String.class;
	}

	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return "Expression to get option";
	}
	
}
