package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Date;
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
public class ExprBanData extends SimpleExpression<Object> {

	private static final Patterns<BanEntryType> patterns;

	static {
		patterns = new Patterns<>(new Object[][]{
			{ "[the] date %offlineplayers/strings% (was|were) banned", BanEntryType.BAN_DATE },
			{ "[the] date[s] of %offlineplayers/strings%'[s] ban",     BanEntryType.BAN_DATE },
			{ "[the] ban date[s] of %offlineplayers/strings%",         BanEntryType.BAN_DATE },

			{ "[the] source[s] of %offlineplayers/strings%'s ban", BanEntryType.SOURCE },
			{ "[the] ban source of %offlineplayers/strings%",      BanEntryType.SOURCE },
			{ "[the] date %offlineplayers/strings%'s ban expires", BanEntryType.SOURCE },

			{ "[the] expiration date[s] of %offlineplayers/strings%'s ban", BanEntryType.EXPIRE_DATE },
			{ "[the] ban expiration date[s] of %offlineplayers/strings%",   BanEntryType.EXPIRE_DATE },

			{ "[the] reason %offlineplayers/strings% (was|were) banned", BanEntryType.REASON },
			{ "[the] reason[s] for %offlineplayers/strings%'s ban",      BanEntryType.REASON },
			{ "[the] ban reason[s] of %offlineplayers/strings%",         BanEntryType.REASON },
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
		Object[] targets = banTarget.getAll(event);
		Object[] results = new Object[targets.length];

		for (int i = 0; i < targets.length; i++) {
			Object target = targets[i];
			BanEntry<?> banEntry = getBanEntry(target);

			if (banEntry == null) {
				continue;
			}

			results[i] = switch (entryType) {
				case BAN_DATE -> Date.fromJavaDate(banEntry.getCreated());
				case SOURCE -> banEntry.getSource();
				case EXPIRE_DATE -> Date.fromJavaDate(banEntry.getExpiration());
				case REASON -> banEntry.getReason();
			};
		}

		return results;
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
		Object[] targets = banTarget.getAll(event);

		for (Object target : targets) {
			BanEntry<?> banEntry = getBanEntry(target);

			if (banEntry == null) {
				continue;
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
							continue;
						}
						banEntry.setReason(newReason);
					}
				}
			} else {
				Timespan timespan = (Timespan) delta[0];
				if (timespan == null) {
					continue;
				}

				if (entryType == BanEntryType.EXPIRE_DATE) {
					Date expiration = Date.fromJavaDate(banEntry.getExpiration());

					if (mode == ChangeMode.ADD) {
						expiration = expiration.plus(timespan);
					} else if (mode == ChangeMode.REMOVE) {
						expiration = expiration.minus(timespan);
					} else {
						continue;
					}

					banEntry.setExpiration(expiration);
				}
			}
			banEntry.save();
		}
	}


	@Override
	public boolean isSingle() {
		return banTarget.isSingle();
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

	private static BanEntry<?> getBanEntry(Object target) {
		if (target instanceof String ipTarget) {
			return getBanEntry(ipTarget);
		} else if (target instanceof OfflinePlayer playerTarget) {
			return getBanEntry(playerTarget);
		}

		return null;
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
