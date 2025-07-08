package ch.njol.skript.skcommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SkriptCommand implements TabExecutor {

	private static final Set<SubCommand> SUB_COMMANDS = Set.of(
		new HelpCommand(),
		new TestCommand()
	);

	/**
	 * Gets a SubCommand by its alias
	 * @param alias The alias
	 * @return The SubCommand or null if no such alias exists
	 */
	private SubCommand findSubCommand(String alias) {
		for (SubCommand subCommand : SUB_COMMANDS) {
			for (String subCommandAlias : subCommand.getAliases()) {
				if (subCommandAlias.equalsIgnoreCase(alias)) {
					return subCommand;
				}
			}
		}
		return null;
	}

	public static List<String> getAllAliases() {
		Set<String> aliases = new HashSet<>();

		for (SubCommand subCommand : SUB_COMMANDS) {
			aliases.addAll(Arrays.asList(subCommand.getAliases()));
		}

		return new ArrayList<>(aliases);
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
							 @NotNull String @NotNull [] args) {
		SubCommand subCommand = null;

		if (args.length > 0) {
			subCommand = findSubCommand(args[0]);
		}

		if (subCommand == null) {
			// TODO - send unknown command text
			sender.sendMessage("");
			return true;
		}

		subCommand.execute(sender, args);
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
												@NotNull String @NotNull [] args) {
		if (args.length == 0 || args[0].isEmpty()) {
			return getAllAliases();
		}

		SubCommand subCommand = findSubCommand(args[0]);
		if (subCommand == null) {
			return List.of();
		}

		return subCommand.getTabCompletions(sender, args);
	}

	public static abstract class SubCommand {

		public final String[] aliases;

		public SubCommand(String... aliases) {
			this.aliases = aliases;
		}

		public String[] getAliases() {
			return aliases;
		}

		/**
		 * Executes this SubCommand
		 * @param sender Source of the command
		 * @param args The arguments
		 */
		public abstract void execute(@NotNull CommandSender sender, @NotNull String @NotNull [] args);

		/**
		 * Returns the tab completions for this SubCommand
		 * @param sender Source of the command
		 * @param args The arguments
		 * @return The tab completions
		 */
		public abstract List<String> getTabCompletions(@NotNull CommandSender sender, @NotNull String @NotNull [] args);

	}

}
