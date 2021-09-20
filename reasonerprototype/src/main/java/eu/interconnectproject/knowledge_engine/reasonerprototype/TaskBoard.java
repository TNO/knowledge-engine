package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;

public class TaskBoard {

	public Map<NodeAlt, BindingSet> tasks;

	private static TaskBoard instance;

	private TaskBoard() {
		tasks = new HashMap<>();
	}

	public static TaskBoard instance() {

		if (instance == null)
			instance = new TaskBoard();

		return instance;

	}

	public void executeScheduledTasks() {

		BindingSet resultingBindingSet;
		NodeAlt node;
		RuleAlt rule;

		Iterator<Map.Entry<NodeAlt, BindingSet>> iter = tasks.entrySet().iterator();

		while (iter.hasNext()) {
			Map.Entry<NodeAlt, BindingSet> entry = iter.next();
			node = entry.getKey();
			assert node != null;
			rule = node.getRule();
			assert rule != null;
			assert entry.getValue() != null;
			resultingBindingSet = rule.getBindingSetHandler().handle(entry.getValue());
			entry.getKey().setBindingSet(resultingBindingSet);
			iter.remove();

		}

	}

}
