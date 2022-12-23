package eu.knowledge.engine.reasoner2;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.DataBindingSetHandler;
import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.Table;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

@TestInstance(Lifecycle.PER_CLASS)
public class VerySimpleBackwardTest {

	private static final Logger LOG = LoggerFactory.getLogger(VerySimpleBackwardTest.class);

	private RuleStore store;
	private ProxyDataBindingSetHandler bindingSetHandler;

	private ProactiveRule startRule;

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

	@BeforeAll
	public void init() throws URISyntaxException {
		// Initialize
		store = new RuleStore();
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
		store.addRule(new Rule(
				new HashSet<>(
						Arrays.asList(new TriplePattern("?a <type> <Sensor>"), new TriplePattern("?a <hasValInC> ?b"))),
				bindingSetHandler));

		store.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <Sensor>"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <Device>")))));

		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Device>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));

		startRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(startRule);
	}

	@Test
	public void doReasoning() throws InterruptedException, ExecutionException {
		// Start reasoning
		ReasonerPlan plan = new ReasonerPlan(store, startRule);

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		binding2.put("p", "<sensor1>");
		binding2.put("q", "22");
		bs.add(binding2);
		TaskBoard tb;
		while ((tb = plan.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}
		var result = plan.getResults();
		System.out.println(result);
	}

}
