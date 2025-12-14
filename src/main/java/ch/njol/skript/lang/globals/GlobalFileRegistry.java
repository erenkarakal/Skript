package ch.njol.skript.lang.globals;

import ch.njol.skript.Skript;
import org.skriptlang.skript.util.Registry;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GlobalFileRegistry implements Registry<GlobalFile> {

	public static GlobalFileRegistry get() {
		return Skript.instance().registry(GlobalFileRegistry.class);
	}

	private final Set<GlobalFile> globals = new HashSet<>();

	public void registerGlobal(GlobalFile file) {
		globals.add(file);
	}

	public void unregisterGlobal(GlobalFile file) {
		globals.remove(file);
	}

	public void reloadAll() {
		globals.forEach(GlobalFile::load);
	}

	@Override
	public Collection<GlobalFile> elements() {
		return globals;
	}

}
