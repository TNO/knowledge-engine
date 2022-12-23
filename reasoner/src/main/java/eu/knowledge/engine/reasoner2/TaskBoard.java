package eu.knowledge.engine.reasoner2;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner2.reasoningnode.RuleNode;

/**
 * TODO replace part of this code with java's Executor Services.
 * 
 * @author nouwtb
 *
 */
public class TaskBoard {

	private static final Logger LOG = LoggerFactory.getLogger(TaskBoard.class);

	private Set<RuleNode> tasks;

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
	public void addTask(RuleNode aNode) {
		tasks.add(aNode);
	}

	public boolean hasTasks() {
		return !this.tasks.isEmpty();
	}

	public boolean hasTask(RuleNode node) {
		return this.tasks.contains(node);
	}

	/**
	 * Executes all tasks that are on the taskboard and returns a future that is
	 * completed when all the tasks have been completed (i.e. a bindingset has been
	 * returned by the bindingsethandlers).
	 * 
	 * @return
	 */
	public CompletableFuture<Void> executeScheduledTasks() {
		Set<Future<Void>> futures = new HashSet<>();
		this.tasks.forEach(task -> {
			futures.add(task.applyRule());
		});
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()]));
	}

	@Override
	public String toString() {
		return "TaskBoard [tasks=" + tasks + "]";
	}

}
