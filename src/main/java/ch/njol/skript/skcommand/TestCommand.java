package ch.njol.skript.skcommand;

import ch.njol.skript.skcommand.SkriptCommand.SubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TestCommand extends SubCommand {

	public TestCommand() {
		super("test");
	}

	@Override
	public void execute(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
		sender.sendMessage("test");
	}

	@Override
	public List<String> getTabCompletions(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
		return List.of("test");
	}

}
