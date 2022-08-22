package eu.knowledge.engine.reasoner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.api.BindingSet;

/**
 * TODO replace part of this code with java's Executor Services.
 * 
 * @author nouwtb
 *
 */
public class TaskBoard {

	private static final Logger LOG = LoggerFactory.getLogger(TaskBoard.class);

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
	public void addTask(RuleNode aNode, BindingSet aBindingSet) {
		tasks.add(new Task(aNode, aBindingSet));
	}

	/**
	 * Add a task that does not result in a bindingset.
	 * 
	 */
	public void addVoidTask(RuleNode aNode, BindingSet aBindingSet) {
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
			final RuleNode node = task.getNodes().iterator().next();
			assert node != null;
			assert node
					.getRule() instanceof Rule : "A rule node of a task should always be a Rule, i.e. non-ProactiveRule.";
			rule = (Rule) node.getRule();
			assert rule != null;

			assert task.getBindingSet(node) != null;

			resultingBindingSetFuture = rule.getBindingSetHandler().handle(task.getBindingSet(node));
			resultingBindingSetFuture.thenAccept((bs) -> {

				// TODO this assumes every node only occurs once in all tasks.
				node.setBindingSet(bs);
			}).handle((r, e) -> {

				if (r == null && e != null) {
					LOG.error("An exception has occured while executing scheduled tasks ", e);
					return null;
				} else {
					return r;
				}
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
		private Map<RuleNode, BindingSet> collectedBindingSets;

		/**
		 * The rule that is being applied for this task.
		 */
		private BaseRule rule;

		/**
		 * Whether this task results in a bindingset or it is void.
		 */
		private boolean hasResult;

		public Task(RuleNode aNode, BindingSet aBindingSet) {
			this(aNode, aBindingSet, true);
		}

		public Task(RuleNode aNode, BindingSet aBindingSet, boolean aHasResult) {
			collectedBindingSets = new HashMap<>();
			collectedBindingSets.put(aNode, aBindingSet);
			rule = aNode.getRule();
			hasResult = aHasResult;
		}

		public BindingSet getBindingSet(RuleNode node) {
			return this.collectedBindingSets.get(node);
		}

		public BaseRule getRule() {
			return this.rule;
		}

		public boolean hasResult() {
			return this.hasResult;
		}

		public void mergeWith(RuleNode aNode, BindingSet aBindingSet) {

		}

		public Set<RuleNode> getNodes() {
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
