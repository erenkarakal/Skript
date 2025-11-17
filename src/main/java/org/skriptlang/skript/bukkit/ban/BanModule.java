package org.skriptlang.skript.bukkit.ban;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import org.bukkit.BanEntry;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.ban.elements.*;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Arrays;
import java.util.function.Consumer;

public class BanModule implements AddonModule {

	@Override
	public void load(SkriptAddon addon) {
		register(addon.syntaxRegistry(),
			EffBan::register,
			ExprBanDate::register,
			ExprBanExpiration::register,
			ExprBanReason::register,
			ExprBanSource::register
		);

		Classes.registerClass(new ClassInfo<>(BanEntry.class, "banentry")
			.name("Ban Entry")
			.description("Represents a ban entry.")
			.since("INSERT VERSION")
			.examples("send all ban entries")
		);
	}

	private void register(SyntaxRegistry registry, Consumer<SyntaxRegistry>... consumers) {
		Arrays.stream(consumers).forEach(consumer -> consumer.accept(registry));
	}

}
