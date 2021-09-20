package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.HashMap;
import java.util.Map;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;

public class TaskBoard {

	public Map<RuleAlt, BindingSet> tasks;

	private static TaskBoard instance;

	private TaskBoard() {
		tasks = new HashMap<>();
	}

	public static TaskBoard instance() {

		if (instance == null)
			instance = new TaskBoard();

		return instance;

	}

}
