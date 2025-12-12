package ch.njol.skript.lang.globals;

import ch.njol.skript.Skript;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Represents a file in the globals folder
 */
public abstract class GlobalFile {

	protected final File file;

	/**
	 * Loads this global file. This method should not handle file creation.
	 */
	public abstract void load();

	public GlobalFile(JavaPlugin plugin, String name) {
		File globalsFolder = new File(plugin.getDataFolder(), "/globals/");
		if (!globalsFolder.exists()) {
			globalsFolder.mkdir();
		}

		String filePath = "globals/" + name + ".sk";
		file = new File(plugin.getDataFolder(), filePath);

		if (!file.exists()) {
			copyFile(plugin, filePath, file);
		}
	}

	/**
	 * Copies a file from the plugin's jar into target file
	 * Replaces existing files
	 */
	private static void copyFile(JavaPlugin plugin, String sourcePath, File targetFile) {
		try (InputStream stream = plugin.getResource(sourcePath)) {
			if (stream == null) {
				Skript.error("The " + sourcePath + " file doesn't exist and couldn't be read from the " + plugin.getName() + " jar file.");
				return;
			}
			Files.copy(stream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			Skript.exception(e, "Error while loading the " + sourcePath + " file from the jar file.");
		}
	}

}
