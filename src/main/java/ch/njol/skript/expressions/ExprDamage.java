/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.expressions;

import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.HealthUtils;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;

@Name("Damage")
@Description({
	"How much damage is done in a damage event, possibly ignoring armour, criticals and/or enchantments.",
	"Can be changed (remember that in Skript '1' is one full heart, not half a heart)."
})
@Examples({
	"on damage of player:",
		"\tdamage cause was lightning",
		"\tvictim is wearing a leather helmet",
		"\tsubtract 2.5 from the damage",
		"\tmessage \"The smite was lessened by your leather helmet!\" to the victim"
})
@Since("1.3.5")
@RequiredPlugins("Spigot 1.14+ for the item damage event.")
@Events("damage")
public class ExprDamage extends SimpleExpression<Number> {

	private final static boolean ITEM_DAMAGE = Skript.classExists("org.bukkit.event.player.PlayerItemDamageEvent");

	static {
		Skript.registerExpression(ExprDamage.class, Number.class, ExpressionType.SIMPLE, "[the] damage");
	}

	private Kleenean delay;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (ITEM_DAMAGE) {
			if (!getParser().isCurrentEvent(EntityDamageEvent.class, VehicleDamageEvent.class, PlayerItemDamageEvent.class)) {
				Skript.error("The expression 'damage' may only be used in damage events");
				return false;
			}
		} else if (!getParser().isCurrentEvent(EntityDamageEvent.class, VehicleDamageEvent.class)) {
			Skript.error("The expression 'damage' may only be used in damage events");
			return false;
		}
		delay = isDelayed;
		return true;
	}

	@Override
	@Nullable
	protected Number[] get(Event event) {
		if (!(event instanceof EntityDamageEvent || event instanceof VehicleDamageEvent))
			return new Number[0];

		if (event instanceof VehicleDamageEvent)
			return CollectionUtils.array(((VehicleDamageEvent) event).getDamage());

		if (event instanceof EntityDamageEvent)
			return CollectionUtils.array(HealthUtils.getDamage((EntityDamageEvent) event));
	
		if (ITEM_DAMAGE) {
			if (!(event instanceof PlayerItemDamageEvent))
				return new Number[0];
			return CollectionUtils.array(((PlayerItemDamageEvent) event).getDamage());
		}

		assert false;
		return new Number[0];
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (delay != Kleenean.FALSE) {
			Skript.error("Can't change the damage anymore after the event has already passed");
			return null;
		}
		if (mode == ChangeMode.REMOVE_ALL)
			return null;
		return CollectionUtils.array(Number.class);
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, ChangeMode mode) throws UnsupportedOperationException {
		if (!(event instanceof EntityDamageEvent || event instanceof VehicleDamageEvent || (ITEM_DAMAGE && event instanceof PlayerItemDamageEvent))) {
			return;
		}
		Number damage = delta == null ? 0 : (Number) delta[0];
		switch (mode) {
			case SET:
			case DELETE:
				if (event instanceof VehicleDamageEvent) {
					((VehicleDamageEvent) event).setDamage(damage.doubleValue());
				} else if (ITEM_DAMAGE && event instanceof PlayerItemDamageEvent) {
					((PlayerItemDamageEvent) event).setDamage(damage.intValue());
				} else {
					HealthUtils.setDamage((EntityDamageEvent) event, damage.doubleValue());
				}
				break;
			case REMOVE:
				damage = -damage.doubleValue();
				//$FALL-THROUGH$
			case ADD:
				if (event instanceof VehicleDamageEvent) {
					((VehicleDamageEvent) event).setDamage(((VehicleDamageEvent) event).getDamage() + damage.doubleValue());
				} else if (ITEM_DAMAGE && event instanceof PlayerItemDamageEvent) {
					((PlayerItemDamageEvent) event).setDamage(((PlayerItemDamageEvent) event).getDamage() + damage.intValue());
				} else {
					HealthUtils.setDamage((EntityDamageEvent) event, HealthUtils.getDamage((EntityDamageEvent) event) + damage.doubleValue());
				}
				break;
			case REMOVE_ALL:
			case RESET:
				assert false;
		}
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "the damage";
	}

}
