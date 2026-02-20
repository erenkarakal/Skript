package org.skriptlang.skript.lang.experiment;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.globals.GlobalExperiments;
import ch.njol.skript.lang.globals.GlobalFileRegistry;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.script.ScriptData;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * A container for storing and testing experiments.
 */
public class ExperimentSet extends LinkedHashSet<Experiment> implements ScriptData, Experimented {

	public ExperimentSet(@NotNull Collection<? extends Experiment> collection) {
		super(collection);
	}

	public ExperimentSet() {
		super();
	}

	@Override
	public boolean hasExperiment(Experiment experiment) {
		GlobalExperiments globalExperiments = Skript.instance().registry(GlobalFileRegistry.class)
			.getGlobalFile(GlobalExperiments.class);
		return this.contains(experiment) || globalExperiments.getExperiments().contains(experiment);
	}

	@Override
	public boolean hasExperiment(String featureName) {
		for (Experiment experiment : this) {
			if (experiment.matches(featureName))
				return true;
		}
		return false;
	}

}
