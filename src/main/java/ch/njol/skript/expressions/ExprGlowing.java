package ch.njol.skript.expressions;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;

@Name("Glowing")
@Description("Indicates if targeted entity is glowing or not. Glowing entities can be seen through walls.")
@Examples({"set glowing of player to true"})
@Since("2.2-dev18")
public class ExprGlowing extends SimplePropertyExpression<Entity, Boolean> {
	
	static {
		register(ExprGlowing.class, Boolean.class, "glowing", "entities");
	}
	
	@Override
	public Boolean convert(Entity entity) {
		return entity.isGlowing();
	}
	
	@Override
	protected String getPropertyName() {
		return "glowing";
	}
	
	@Override
	public Class<Boolean> getReturnType() {
		return Boolean.class;
	}
	
	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (mode == ChangeMode.SET || mode == ChangeMode.RESET)
			return new Class[] {Boolean.class};
		return null;
	}
	
	@Override
	public void change(Event event, @Nullable Object[] delta, ChangeMode mode) throws UnsupportedOperationException {
		for (final Entity entity : getExpr().getArray(event))
			entity.setGlowing(delta != null && (Boolean) delta[0]);
	}
}
