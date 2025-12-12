package ch.njol.skript.structures;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.OptionRegistry;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.ScriptData;
import org.skriptlang.skript.lang.structure.Structure;

import java.util.HashMap;
import java.util.Map;

@Name("Options")
@Description({
	"Options are used for replacing parts of a script with something else.",
	"For example, an option may represent a message that appears in multiple locations.",
	"Take a look at the example below that showcases this."
})
@Examples({
	"options:",
	"\tno_permission: You're missing the required permission to execute this command!",
	"",
	"command /ping:",
	"\tpermission: command.ping",
	"\tpermission message: {@no_permission}",
	"\ttrigger:",
	"\t\tmessage \"Pong!\"",
	"",
	"command /pong:",
	"\tpermission: command.pong",
	"\tpermission message: {@no_permission}",
	"\ttrigger:",
	"\t\tmessage \"Ping!\""
})
@Since("1.0")
public class StructOptions extends Structure {

	public static final Priority PRIORITY = new Priority(100);

	static {
		Skript.registerStructure(StructOptions.class, "options");
	}

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult, @Nullable EntryContainer entryContainer) {
		// noinspection ConstantConditions - entry container cannot be null as this structure is not simple
		SectionNode node = entryContainer.getSource();
		node.convertToEntries(-1);
		OptionRegistry optionRegistry = Skript.instance().registry(OptionRegistry.class);
		optionRegistry.loadLocalOptions(getParser().getCurrentScript(), node);
		return true;
	}

	@Override
	public boolean load() {
		return true;
	}

	@Override
	public void unload() {
		OptionRegistry optionRegistry = Skript.instance().registry(OptionRegistry.class);
		optionRegistry.deleteLocalOptions(getParser().getCurrentScript());
	}

	@Override
	public Priority getPriority() {
		return PRIORITY;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "options";
	}

	/**
	 * @deprecated Use <code>Skript.instance().registry(OptionRegistry.class)</code> instead.
	 */
	@Deprecated(since = "INSERT VERSION", forRemoval = true)
	public static final class OptionsData implements ScriptData {

		/**
		 * Replaces all options in the provided String using the options of this data.
		 *
		 * @param string The String to replace options in.
		 * @return A String with all options replaced, or the original String if the provided Script has no options.
		 */
		@SuppressWarnings("ConstantConditions") // no way to get null as callback does not return null anywhere
		public String replaceOptions(String string) {
			/*
			* TODO - couldn't find a way to get the Script from a ScriptData
			* needed for getting option from registry
			*/
			return string;
		}

		/**
		 * @return An unmodifiable version of this data's option mappings.
		 */
		public Map<String, String> getOptions() {
			return new HashMap<>();
		}

	}
}
