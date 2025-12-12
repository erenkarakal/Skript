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

	public GlobalOptions(org.skriptlang.skript.Skript skript) {
		super(Skript.getInstance(), "options");
		skript.registry(GlobalFileRegistry.class).registerGlobal(this);
	}

	/**
	 * Loads the 'globals/options.sk' file
	 */
	@Override
	public void load() {
		try {
			Config config = new Config(file, true, false, ":");
			config.getMainNode().convertToEntries(-1);
			OptionRegistry optionRegistry = Skript.instance().registry(OptionRegistry.class);
			optionRegistry.loadGlobalOptions(config.getMainNode());

			// for unit tests
			if (Skript.testing()) {
				optionRegistry.setGlobalOption("GlobalOptionTest", "works!!!");
				optionRegistry.setGlobalOption("GlobalOptionOverrideTest", "shouldn't work!!!");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
