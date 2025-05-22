package ch.njol.skript.expressions;

import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Date;
import ch.njol.util.Kleenean;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Name("Now")
@Description("The current <a href='classes.html#date'>system time</a> of the server. Use <a href='#ExprTime'>time</a> to get the <a href='classes.html#time'>Minecraft time</a> of a world.")
@Examples({"broadcast \"Current server time: %now%\""})
@Since("1.4, INSERT VERSION (timezones)")
public class ExprNow extends SimpleExpression<Date> {
	
	static {
		Skript.registerExpression(ExprNow.class, Date.class, ExpressionType.SIMPLE, "now [timezone:in time[ ]zone %-string%]");
	}

	private boolean usingTimezone;
	private Expression<String> timezone;
	
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		usingTimezone = parseResult.hasTag("timezone");
		timezone = (Expression<String>) exprs[0];
		return true;
	}
	
	@Override
	protected Date[] get(Event event) {
		if (usingTimezone) {
			String timezone = this.timezone.getSingle(event);
			if (timezone == null) {
				return new Date[0];
			}

			ZoneId zoneId;
			try {
				zoneId = ZoneId.of(timezone);
			} catch (DateTimeException e) { // invalid zone format
				return new Date[0];
			}

			Instant instant = ZonedDateTime.now(zoneId).toInstant();
			java.util.Date javaDate = java.util.Date.from(instant);
			Date date = Date.fromJavaDate(javaDate);
			return new Date[]{ date };
 		}
		return new Date[]{ new Date() };
	}
	
	@Override
	public boolean isSingle() {
		return true;
	}
	
	@Override
	public Class<? extends Date> getReturnType() {
		return Date.class;
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		if (usingTimezone) {
			return "now in timezone " + timezone.toString(e, debug);
		}
		return "now";
	}
	
}
