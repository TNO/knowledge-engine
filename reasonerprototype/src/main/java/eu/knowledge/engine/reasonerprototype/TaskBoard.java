package eu.knowledge.engine.reasonerprototype;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasonerprototype.api.BindingSet;

public class TaskBoard {

	public Set<Task> tasks;
	private static TaskBoard instance;

	private TaskBoard() {
		tasks = new HashSet<>();
	}

	public static TaskBoard instance() {

		if (instance == null)
			instance = new TaskBoard();

		return instance;

	}

	/**
	 * Add the task to the list. TODO Currently, there is no aggregation of tasks
	 * which we do typically want to happen here. We should find a structure in
	 * which new tasks are merged with existing ones. The endresult of a task need
	 * to split again before sending it back to the correct node.
	 * 
	 * @param aNode
	 * @param aBindingSet
	 */
	public void addTask(NodeAlt aNode, BindingSet aBindingSet) {
		Set<Task> tasks = TaskBoard.instance().tasks;
		tasks.add(new Task(aNode, aBindingSet));
	}

	public void executeScheduledTasks() {

		BindingSet resultingBindingSet;
		NodeAlt node;
		RuleAlt rule;

		Iterator<Task> iter = tasks.iterator();

		while (iter.hasNext()) {
			Task task = iter.next();
			node = task.getNodes().iterator().next();
			assert node != null;
			rule = node.getRule();
			assert rule != null;
			assert task.getBindingSet(node) != null;
			resultingBindingSet = rule.getBindingSetHandler().handle(task.getBindingSet(node));
			node.setBindingSet(resultingBindingSet);
			iter.remove();
		}

	}

	private static class Task {
		/**
		 * Contains all the bindingSets that were merged into a single one to be handled
		 * as a single task.
		 */
		private Map<NodeAlt, BindingSet> collectedBindingSets;

		/**
		 * The rule that is being applied for this task.
		 */
		private RuleAlt rule;

		public Task(NodeAlt aNode, BindingSet aBindingSet) {
			collectedBindingSets = new HashMap<>();
			collectedBindingSets.put(aNode, aBindingSet);
			rule = aNode.getRule();
		}

		public BindingSet getBindingSet(NodeAlt node) {
			return this.collectedBindingSets.get(node);
		}

		public RuleAlt getRule() {
			return this.rule;
		}

		public void mergeWith(NodeAlt aNode, BindingSet aBindingSet) {

		}

		public Set<NodeAlt> getNodes() {
			return this.collectedBindingSets.keySet();
		}

	}

}
