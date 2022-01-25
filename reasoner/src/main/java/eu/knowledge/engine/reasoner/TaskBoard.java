package eu.knowledge.engine.reasoner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import eu.knowledge.engine.reasoner.ReasoningNode;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.BindingSet;

public class TaskBoard {

	public Set<Task> tasks;

	public TaskBoard() {
		tasks = new HashSet<>();
	}

	/**
	 * Add the task to the list. TODO Currently, there is no aggregation of tasks
	 * which we do typically want to happen here. We should find a structure in
	 * which new tasks are merged with existing ones. The end result of a task need
	 * to split again before sending it back to the correct node. Note that this
	 * only seems to work for backward chaining and not for forward chaining (double
	 * check).
	 * 
	 * @param aNode
	 * @param aBindingSet
	 */
	public void addTask(ReasoningNode aNode, BindingSet aBindingSet) {
		tasks.add(new Task(aNode, aBindingSet));
	}

	/**
	 * Executes all tasks that are on the taskboard and returns a future that is
	 * completed when all the tasks have been completed (i.e. a bindingset has been
	 * returned by the bindingsethandlers).
	 * 
	 * @return
	 */
	public CompletableFuture<Void> executeScheduledTasks() {

		CompletableFuture<BindingSet> resultingBindingSetFuture;
		Rule rule;

		Iterator<Task> iter = tasks.iterator();
		Set<CompletableFuture<?>> futures = new HashSet<>();
		while (iter.hasNext()) {
			Task task = iter.next();
			final ReasoningNode node = task.getNodes().iterator().next();
			assert node != null;
			rule = node.getRule();
			assert rule != null;
			assert task.getBindingSet(node) != null;
			resultingBindingSetFuture = rule.getBindingSetHandler().handle(task.getBindingSet(node));
			resultingBindingSetFuture.thenAccept((bs) -> {
				// TODO this assumes every node only occurs once in all tasks.
				node.setBindingSet(bs);
			});

			futures.add(resultingBindingSetFuture);

			iter.remove();
		}
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()]));
	}

	private static class Task {
		/**
		 * Contains all the bindingSets that were merged into a single one to be handled
		 * as a single task.
		 */
		private Map<ReasoningNode, BindingSet> collectedBindingSets;

		/**
		 * The rule that is being applied for this task.
		 */
		private Rule rule;

		public Task(ReasoningNode aNode, BindingSet aBindingSet) {
			collectedBindingSets = new HashMap<>();
			collectedBindingSets.put(aNode, aBindingSet);
			rule = aNode.getRule();
		}

		public BindingSet getBindingSet(ReasoningNode node) {
			return this.collectedBindingSets.get(node);
		}

		public Rule getRule() {
			return this.rule;
		}

		public void mergeWith(ReasoningNode aNode, BindingSet aBindingSet) {

		}

		public Set<ReasoningNode> getNodes() {
			return this.collectedBindingSets.keySet();
		}

		@Override
		public String toString() {
			return "Task [rule=" + rule + "]";
		}

	}

	@Override
	public String toString() {
		return "TaskBoard [tasks=" + tasks + "]";
	}

}
