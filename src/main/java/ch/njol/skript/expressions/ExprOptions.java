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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.script.Script;

import com.google.common.collect.Lists;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.structures.StructOptions.OptionsData;
import ch.njol.util.Kleenean;

@Name("Script Options")
@Description("Returns one or more options from any script. If no script is provided, it will be the current script of the syntax.")
@Examples({
	"# example.sk",
	"options:",
		"\texample: Hello World",
	"",
	"command /example <string=%script option \"test\"%>:",
		"\ttrigger:",
			"\t\tsend \"The option is %script option \"\"example\"\"%\"",
	"",
	"# this.sk",
	"options:",
		"\texample 2: 1337",
	"command /anotherFile:",
		"\ttrigger:",
			"\t\tsend \"The option is %script option \"\"example 2\"\" and \"\"example\"\" from scripts \"\"this.sk\"\" and \"\"example.sk\"\"%\"",
})
@Since("INSERT VERSION")
public class ExprOptions extends SimpleExpression<String> {

	static {
		Skript.registerExpression(ExprOptions.class, String.class, ExpressionType.COMBINED, "[the] [s(c|k)ript] option[s] %strings% [(from|in) [script[s]] %-strings%]");
	}

	@Nullable
	private Expression<String> scripts;
	private Expression<String> options;
	private Script current;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		current = getParser().getCurrentScript();
		options = (Expression<String>) exprs[0];
		scripts = (Expression<String>) exprs[1];
		return true;
	}

	@Override
	protected String[] get(Event event) {
		List<Script> scripts = Lists.newArrayList(current);
		if (this.scripts != null) {
			Set<Script> loadedScripts = ScriptLoader.getLoadedScripts();
			scripts = this.scripts.stream(event)
					.map(fileName -> loadedScripts.stream()
							.filter(loadedScript -> loadedScript.getConfig().getFileName().equalsIgnoreCase(fileName))
							.findFirst()
					)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.collect(Collectors.toList());
		}
		if (scripts.size() < 1)
			return new String[0];

		Map<String, String> data = scripts.stream()
				.map(script -> script.getData(OptionsData.class).getOptions())
				.flatMap(options -> options.entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		return this.options.stream(event)
				.map(data::get)
				.filter(Objects::nonNull)
				.toArray(String[]::new);
	}

	@Override
	public boolean isSingle() {
		return options.isSingle();
	}

	@Override
	public Class<String> getReturnType() {
		return String.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "script option" + (!isSingle() ? "s " : " ") + options.toString(event, debug) + " from script" +
				(scripts == null ? " " + current.getConfig().getFileName() : (!scripts.isSingle() ? "s " : " ") + scripts.toString(event, debug));
	}

}
