package ch.njol.skript.bukkitutil;

import java.lang.reflect.Method;

import org.bukkit.entity.Entity;

import ch.njol.skript.Skript;

/**
 * @author Peter Güttinger and contributors
 */
@SuppressWarnings("null")
public abstract class PassengerUtils {
	
	private PassengerUtils() {}

	public static Entity[] getPassenger(Entity e) {
		return e.getPassengers().toArray(new Entity[0]);
	}
	/**
	 * Add the passenger to the vehicle
	 * @param vehicle - The entity vehicle
	 * @param passenger - The entity passenger
	 */
	public static void addPassenger(Entity vehicle, Entity passenger) {
		if (vehicle == null || passenger == null)
			return;
		vehicle.addPassenger(passenger);
	}
	/**
	 * Remove the passenger from the vehicle.
	 * @param vehicle - The entity vehicle
	 * @param passenger - The entity passenger
	 */
	public static void removePassenger(Entity vehicle, Entity passenger){
		if (vehicle == null || passenger == null)
			return;
		vehicle.removePassenger(passenger);
	}
	
}
