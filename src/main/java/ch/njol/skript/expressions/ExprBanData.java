package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.ban.BanListType;
import org.bukkit.BanEntry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

@Name("Ban Details")
@Description("Returns data about a player or IP ban. " +
	"The source can be any string, but it's usually the player's name. " +
	"The expiration date won't return a value if the ban is permanent.")
@Example("""
	set {_player} to offlineplayer("Notch", false)",
	set {_expiration} to the date {_player}'s ban expires",
	set {_time.left} to difference between now and {_expiration}",
	set {_reason} to the reason {_player} was banned",
	send "There is %{_time.left}% before %{_player}% gets unbanned! They were banned for '%{_reason}%'" to player
	""")
@Since("INSERT VERSION")
@RequiredPlugins("Spigot 1.20.1+")
public class ExprBanData extends SimpleExpression<Object> {

	private static final Patterns<BanEntryType> patterns;

	static {
		patterns = new Patterns<>(new Object[][]{
			{ "[the] date %offlineplayer/string% was banned", BanEntryType.BAN_DATE },
			{ "[the] date of %offlineplayer/string%'s ban",   BanEntryType.BAN_DATE },
			{ "[the] ban date of %offlineplayer/string%",     BanEntryType.BAN_DATE },

			{ "[the] source of %offlineplayer/string%'s ban",    BanEntryType.SOURCE },
			{ "[the] ban source of %offlineplayer/string%",      BanEntryType.SOURCE },
			{ "[the] date %offlineplayer/string%'s ban expires", BanEntryType.SOURCE },

			{ "[the] expiration date of %offlineplayer/string%'s ban", BanEntryType.EXPIRE_DATE },
			{ "[the] ban expiration date of %offlineplayer/string%",   BanEntryType.EXPIRE_DATE },

			{ "[the] reason %offlineplayer/string% was banned", BanEntryType.REASON },
			{ "[the] reason for %offlineplayer/string%'s ban",  BanEntryType.REASON },
			{ "[the] ban reason of %offlineplayer/string%",     BanEntryType.REASON },
		});

		Skript.registerExpression(ExprBanData.class, Object.class, ExpressionType.COMBINED, patterns.getPatterns());
	}

	private BanEntryType entryType;
	private Expression<Object> banTarget; // offline player or string (ip)

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		entryType = patterns.getInfo(matchedPattern);
		// noinspection unchecked
		banTarget = (Expression<Object>) expressions[0];
		return true;
	}

	@Override
	protected Object @Nullable [] get(Event event) {
		Object target = banTarget.getSingle(event);
		BanEntry<?> banEntry;

		if (target instanceof String ipTarget) {
			banEntry = getBanEntry(ipTarget);
		} else if (target instanceof OfflinePlayer playerTarget) {
			banEntry = getBanEntry(playerTarget);
		} else {
			return null;
		}

		if (banEntry == null) {
			return null;
		}

		return switch (entryType) {
			case BAN_DATE -> {
				Date creation = banEntry.getCreated();
				yield new Date[]{ creation };
			}
			case SOURCE -> new String[]{ banEntry.getSource() };
			case EXPIRE_DATE -> {
				Date expiration = banEntry.getExpiration();
				if (expiration == null) {
					yield null;
				}
				yield new Date[]{ expiration };
			}
			case REASON -> new String[]{ banEntry.getReason() };
		};
	}

	@Override
	public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
		if (mode == ChangeMode.SET) {
			return switch (entryType) {
				case EXPIRE_DATE -> new Class<?>[]{ Date.class };
				case SOURCE, REASON -> new Class<?>[]{ String.class };
				default -> null;
			};
		} else if (mode == ChangeMode.ADD || mode == ChangeMode.REMOVE) {
			if (entryType == BanEntryType.EXPIRE_DATE) {
				return new Class<?>[]{ Timespan.class };
			}
		}
		return null;
	}

	@Override
	public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
		Object target = banTarget.getSingle(event);
		BanEntry<?> banEntry;

		if (target instanceof String ipTarget) {
			banEntry = getBanEntry(ipTarget);
		} else if (target instanceof OfflinePlayer playerTarget) {
			banEntry = getBanEntry(playerTarget);
		} else {
			return;
		}

		if (banEntry == null) { // target isn't banned
			return;
		}

		assert delta != null;
		if (mode == ChangeMode.SET) {
			switch (entryType) {
				case SOURCE -> {
					String newSource = (String) delta[0];
					banEntry.setSource(newSource);
				}
				case EXPIRE_DATE -> {
					Date newDate = (Date) delta[0];
					banEntry.setExpiration(newDate);
				}
				case REASON -> {
					String newReason = (String) delta[0];
					if (newReason == null) {
						return;
					}
					banEntry.setReason(newReason);
				}
			}
		} else {
			Timespan timespan = (Timespan) delta[0];
			if (timespan == null)
				return;

			if (entryType == BanEntryType.EXPIRE_DATE) {
				ch.njol.skript.util.Date expiration = ch.njol.skript.util.Date.fromJavaDate(banEntry.getExpiration());

				if (mode == ChangeMode.ADD) {
					expiration = expiration.plus(timespan);
				} else if (mode == ChangeMode.REMOVE) {
					expiration = expiration.minus(timespan);
				} else {
					return;
				}

				banEntry.setExpiration(expiration);
			}
		}
		banEntry.save();
	}


	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<?> getReturnType() {
		return switch (entryType) {
			case BAN_DATE, EXPIRE_DATE -> Date.class;
			case SOURCE, REASON -> String.class;
		};
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String target = banTarget.toString(event, debug);
		return switch (entryType) {
			case BAN_DATE -> "the date " + target + " was banned";
			case SOURCE -> "the source of " + target + "'s ban";
			case EXPIRE_DATE -> "the date " + target + "'s ban expires";
			case REASON -> "the reason " + target + " was banned";
		};
	}

	private static BanEntry<InetAddress> getBanEntry(String ipTarget) {
		try {
			InetAddress address = InetAddress.getByName(ipTarget);
			return Bukkit.getBanList(BanListType.IP).getBanEntry(address);
		} catch (UnknownHostException ignored) {} // this only happens when you pass a url and it performs a lookup
		return null;
	}

	private static BanEntry<PlayerProfile> getBanEntry(OfflinePlayer playerTarget) {
		return Bukkit.getBanList(BanListType.PROFILE).getBanEntry(playerTarget.getPlayerProfile());
	}

	private enum BanEntryType {
		BAN_DATE, SOURCE, EXPIRE_DATE, REASON
	}

}
