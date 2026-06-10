package ch.njol.skript.hooks;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import ch.njol.skript.doc.Documentation;
import ch.njol.skript.lang.SyntaxElement;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import ch.njol.skript.Skript;
import ch.njol.skript.localization.ArgsMessage;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.ClassLoader;

public abstract class Hook<P extends Plugin> {

	private final static ArgsMessage m_hooked = new ArgsMessage("hooks.hooked");
	private final static ArgsMessage m_hook_error = new ArgsMessage("hooks.error");
	
	protected final P plugin;
	
	public final P getPlugin() {
		return plugin;
	}
	
	@SuppressWarnings("null")
	public Hook() throws IOException {
		// noinspection unchecked
		P plugin = (P) Bukkit.getPluginManager().getPlugin(getName());
		this.plugin = plugin;
		if (plugin == null || !plugin.isEnabled()) {
			if (Documentation.canGenerateUnsafeDocs()) {
				loadClasses();
				if (Skript.logHigh())
					Skript.info(m_hooked.toString(getName()));
			}
			return;
		}

		if (!init()) {
			Skript.error(m_hook_error.toString(plugin.getName()));
			return;
		}

		loadClasses();

		if (Skript.logHigh())
			Skript.info(m_hooked.toString(plugin.getName()));
	}
	
	protected void loadClasses() throws IOException {
		SyntaxRegistry registry = Skript.instance().syntaxRegistry();
		ClassLoader.builder()
			.basePackage(getClass().getPackage().getName())
			.deep(true)
			.initialize(true)
			.forEachClass(clazz -> {
				if (SyntaxElement.class.isAssignableFrom(clazz)) {
					try {
						clazz.getMethod("register", SyntaxRegistry.class).invoke(null, registry);
					} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
						// noinspection ThrowableNotThrown
						Skript.exception(e, "Error while loading a hook at " + clazz.getName());
					}
				}
			})
			.build()
			.loadClasses(Skript.class);
	}
	
	/**
	 * @return The hooked plugin's exact name
	 */
	public abstract String getName();
	
	/**
	 * Called when the plugin has been successfully hooked
	 */
	protected boolean init() {
		return true;
	}
	
}
