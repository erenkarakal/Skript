package ch.njol.skript.lang.globals;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.structures.StructUsing;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.lang.experiment.Experiment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GlobalExperiments extends GlobalFile {

	private static final List<Experiment> globalExperiments = new ArrayList<>();

	public GlobalExperiments(SkriptAddon addon) {
		super(addon, "experiments");
	}

	@Override
	public void load() {
		globalExperiments.clear();

		try {
			Config config = new Config(file, true, false, null);
			for (Node node : config.getMainNode()) {
				if (!(node instanceof SimpleNode simpleNode)) {
					Skript.error("Invalid line");
					continue;
				}

				String name = simpleNode.getKey();
				assert name != null;
				Experiment experiment = Skript.experiments().find(name.trim());

				if (!experiment.isKnown()) {
					Skript.error("Unknown experiment " + name);
					continue;
				}

				StructUsing.validateExperiment(experiment);
				globalExperiments.add(experiment);
			}
		} catch (IOException e) {
			//noinspection ThrowableNotThrown
			Skript.exception(e, "Error while loading the global experiments file.");
		}
	}

	public List<Experiment> getExperiments() {
		return globalExperiments;
	}

}
