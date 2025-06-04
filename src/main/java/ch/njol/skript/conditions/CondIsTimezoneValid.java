package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.time.DateTimeException;
import java.time.ZoneId;

@Name("Is Timezone Valid")
@Description("Checks if a timezone is valid.")
@Example("""
	set {_timezone} to "America/New_York"
	if timezone {_timezone} is valid:
		set {_date} to now in timezone {_timezone}
	""")
@Since("1.4")
public class CondIsTimezoneValid extends Condition {

	static {
		Skript.registerCondition(CondIsTimezoneValid.class, "time[ ]zone[s] %strings% (is|are) [negate:in]valid");
	}

	private Expression<String> timezones;
	private boolean isNegated;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		timezones = (Expression<String>) expressions[0];
		isNegated = parseResult.hasTag("negate");
		return true;
	}

	@Override
	public boolean check(Event event) {
		for (String timezone : timezones.getAll(event)) {
			if (timezone == null) {
				return isNegated;
			}

			try {
				ZoneId.of(timezone);
			} catch (DateTimeException e) {
				return isNegated;
			}
		}

		return !isNegated;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "timezone " + timezones.toString(event, debug) + " is " + (isNegated ? "in" : "") + "valid";
	}

}
