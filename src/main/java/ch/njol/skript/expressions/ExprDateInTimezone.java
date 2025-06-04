package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Date;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Name("Date in Timezone")
@Description({
	"Returns a date in the specified timezone. Note that the result date might not be equal to the input date.",
	"Use <a href='#ExprAllTimezones'>all timezones</a> to get a list of valid timezones."
})
@Example("""
	set {_date} to now in timezone "Europe/Istanbul"
	set {_clock} to {_date} formatted as "kk:mm"
	send "It is currently %{_clock}% in Istanbul!" to player
	""")
@Since("INSERT VERSION")
public class ExprDateInTimezone extends SimpleExpression<Date> {

	static {
		Skript.registerExpression(ExprDateInTimezone.class, Date.class, ExpressionType.SIMPLE, "[the] [date] %date% in time[ ]zone %string%");
	}

	private Expression<Date> date;
	private Expression<String> timezone;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		date = (Expression<Date>) expressions[0];
		timezone = (Expression<String>) expressions[1];
		return true;
	}

	@Override
	protected Date @Nullable [] get(Event event) {
		String timezone = this.timezone.getSingle(event);
		Date date = this.date.getSingle(event);

		if (timezone == null) {
			error("Timezone is not set.");
			return new Date[0];
		}

		if (date == null) {
			return new Date[0];
		}

		ZoneId targetZoneId;
		try {
			targetZoneId = ZoneId.of(timezone);
		} catch (DateTimeException e) { // invalid zone format
			error("Invalid timezone.");
			return new Date[0];
		}

		Instant instantDate = date.toInstant();
		ZoneId localZoneId = ZoneId.systemDefault();
		Instant shiftedNow = ZonedDateTime.ofInstant(instantDate, targetZoneId)
			.toLocalDateTime()
			.atZone(localZoneId)
			.toInstant();
		java.util.Date javaDate = java.util.Date.from(shiftedNow);
		Date shiftedDate = Date.fromJavaDate(javaDate);
		return new Date[]{ shiftedDate };
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
	public String toString(@Nullable Event event, boolean debug) {
		return "date " + date.toString(event, debug) + " in timezone " + timezone.toString(event, debug);
	}

}
