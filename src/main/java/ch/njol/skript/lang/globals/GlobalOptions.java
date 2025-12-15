package ch.njol.skript.lang.globals;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Config;
import ch.njol.skript.lang.OptionRegistry;

import java.io.IOException;
import java.util.*;

/**
 * Represents the 'globals/options.sk' file
 */
public class GlobalOptions extends GlobalFile {

	public GlobalOptions() {
		super(Skript.getAddonInstance(), "options");
	}

	/**
	 * Loads the 'globals/options.sk' file
	 */
	@Override
	public void load() {
		try {
			Config config = new Config(file, true, false, ":");
			config.getMainNode().convertToEntries(-1);
			OptionRegistry optionRegistry = OptionRegistry.get();
			optionRegistry.loadGlobalOptions(config.getMainNode());

			// for unit tests
			if (Skript.testing()) {
				optionRegistry.setGlobalOption("GlobalOptionTest", "works!!!");
				optionRegistry.setGlobalOption("GlobalOptionOverrideTest", "shouldn't work!!!");
			}
		} catch (IOException e) {
			Skript.exception(e, "Error while loading global options");
		}
	}

}
