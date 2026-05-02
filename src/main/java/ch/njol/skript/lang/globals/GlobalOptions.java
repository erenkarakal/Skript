package ch.njol.skript.lang.globals;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Config;
import ch.njol.skript.lang.OptionRegistry;
import org.skriptlang.skript.addon.SkriptAddon;

import java.io.IOException;
import java.util.*;

/**
 * Represents the 'globals/options.sk' file
 */
public class GlobalOptions extends GlobalFile {

	public GlobalOptions(SkriptAddon addon) {
		super(addon, "options.sk");
	}

	@Override
	public void init() {
		if (!file.exists()) {
			copyFile("options.sk");
		}
		load();
	}

	@Override
	public void load() {
		try {
			Config config = new Config(file, true, false, ":");
			config.getMainNode().convertToEntries(-1);
			getAddon().registry(OptionRegistry.class).loadGlobalOptions(config.getMainNode());
		} catch (IOException e) {
			Skript.exception(e, "Error while loading global options");
		}
	}

	@Override
	public void unload() {
		getAddon().registry(OptionRegistry.class).getGlobalOptions().clear();
	}

}
