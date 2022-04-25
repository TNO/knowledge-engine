package eu.knowledge.engine.reasoner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.Rule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

public class VerySimpleBackwardTest {

	private static final Logger LOG = LoggerFactory.getLogger(VerySimpleBackwardTest.class);

	private KeReasoner reasoner;
	private ProxyDataBindingSetHandler bindingSetHandler;

	/**
	 * A simple proxy class that captures the incoming bindingsets so that we are
	 * able to compare them and see if they are equal using a unit test.
	 * 
	 * @author nouwtb
	 *
	 */
	public static class ProxyDataBindingSetHandler extends DataBindingSetHandler {

		private List<BindingSet> incomingBindings = new ArrayList<BindingSet>();

		public ProxyDataBindingSetHandler(Table someData) {
			super(someData);
		}

		public List<BindingSet> getBindingSets() {
			return this.incomingBindings;
		}

		@Override
		public CompletableFuture<BindingSet> handle(BindingSet bs) {
			this.incomingBindings.add(bs);
			return super.handle(bs);
		}

		/**
		 * Remove all entries.
		 */
		public void clear() {
			this.incomingBindings.clear();
		}
	}

	@Before
	public void init() throws URISyntaxException {
		// Initialize
		reasoner = new KeReasoner();
		bindingSetHandler = new ProxyDataBindingSetHandler(new Table(new String[] {
		//@formatter:off
				"a", "b"
				//@formatter:on
		}, new String[] {
		//@formatter:off
				"<sensor1>,22",
				"<sensor2>,21",
				//@formatter:on
		}));
		reasoner.addRule(new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?a <type> <Sensor>"), new TriplePattern("?a <hasValInC> ?b"))),
				bindingSetHandler));

		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <Sensor>"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <Device>")))));
	}

	@Test
	public void testConverter() {

		// test with taskboard
		doReasoning(new TaskBoard());
		List<BindingSet> bsWithTaskBoard = new ArrayList<>(this.bindingSetHandler.getBindingSets());

		// emtpy the bindingsets collected
		this.bindingSetHandler.clear();
		LOG.info("----------------- new reasoning ------------------");

		// test without taskboard
		doReasoning(null);
		List<BindingSet> bsWithoutTaskBoard = new ArrayList<>(this.bindingSetHandler.getBindingSets());

		// incoming bindingset for handler should be the same when using TaskBoard and
		// not using taskboard. Ordering is not important. Is the number of times it is
		// called important? Maybe not because the TaskBoard might aggregate multiple
		// calls together into a single one?
		LOG.info("With TaskBoard   : " + bsWithTaskBoard);
		LOG.info("Without TaskBoard: " + bsWithoutTaskBoard);

		assertEquals(new HashSet<>(bsWithTaskBoard), new HashSet<>(bsWithoutTaskBoard));

	}

	private void doReasoning(TaskBoard aTaskBoard) {
		// Formulate objective
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Device>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, aTaskBoard);
		LOG.info("\n{}", root);

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		binding2.put("p", "<sensor1>");
		binding2.put("q", "22");
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			LOG.info("\n{}", root);

			if (aTaskBoard != null)
				aTaskBoard.executeScheduledTasks();
		}

		LOG.info("bindings: " + bind);
		assertTrue(!bind.isEmpty() && !bind.iterator().next().isEmpty());
	}

}
