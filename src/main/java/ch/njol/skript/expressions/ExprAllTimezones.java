package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;

public class ExprAllTimezones extends SimpleExpression<String> {

	static {
		Skript.registerExpression(ExprAllTimezones.class, String.class, ExpressionType.SIMPLE, "all time[ ]zones");
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		return true;
	}

	@Override
	@Nullable
	protected String[] get(Event event) {
		return ZoneId.getAvailableZoneIds().toArray(new String[0]);
	}

	@Override
	public boolean isSingle() {
		return false;
	}
	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "all timezones";
	}

}
