package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

public class TaskBoardTest {

	private static final int SLEEP_TIME_MS = 1000;
	private static final Logger LOG = LoggerFactory.getLogger(TaskBoardTest.class);
	private ExecutorService es = Executors.newFixedThreadPool(2);

	/**
	 * Test whether the TaskBoard startTime local variable is separated from
	 * subsequent iterations in the loop. If these were not separated and
	 * distinguished, the start time of the first node that finishes would take on
	 * the value of the start time of the second node that started.
	 * 
	 * See source code of {@link TaskBoard#executeScheduledTasks()}
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Test
	public void testTimer() throws InterruptedException, ExecutionException {

		TaskBoard tb = new TaskBoard();

		Rule r = new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <Test>"))), new HashSet<>(),
				new BindingSetHandler() {

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {

						try {
							Thread.sleep(250);
						} catch (InterruptedException e1) {
							LOG.error("{}", e1);
						}

						Callable<BindingSet> callable = new Callable<BindingSet>() {

							public BindingSet call() {
								try {
									Thread.sleep(SLEEP_TIME_MS);
								} catch (InterruptedException e) {
									LOG.error("{}", e);
								}
								return new BindingSet();
							}

						};
						CompletableFuture<BindingSet> future = CompletableFuture.supplyAsync(() -> {
							try {
								return callable.call();
							} catch (Exception ex) {
								throw new CompletionException(ex);
							} // Or return default value
						}, TaskBoardTest.this.es);

						return future;
					}

				});

		ReasoningNode aFirstNode = new ReasoningNode(new ArrayList<>(), null, r, null, false);
		ReasoningNode aSecondNode = new ReasoningNode(new ArrayList<>(), null, r, null, false);
		tb.addTask(aFirstNode, new BindingSet());
		tb.addTask(aSecondNode, new BindingSet());

		tb.executeScheduledTasks().get();

		LOG.info("1) Starttime: {}", aFirstNode.getStartTime());
		LOG.info("1) Endtime:   {}", aFirstNode.getEndTime());
		LOG.info("");
		LOG.info("2) Starttime: {}", aSecondNode.getStartTime());
		LOG.info("2) Endtime:   {}", aSecondNode.getEndTime());

		assertFalse(aFirstNode.getStartTime().equals(aSecondNode.getStartTime()));
	}

}
