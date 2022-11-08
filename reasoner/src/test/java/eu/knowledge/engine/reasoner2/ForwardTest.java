package eu.knowledge.engine.reasoner2;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.SinkBindingSetHandler;
import eu.knowledge.engine.reasoner.Table;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

@TestInstance(Lifecycle.PER_CLASS)
public class ForwardTest {

	private static final Logger LOG = LoggerFactory.getLogger(ForwardTest.class);

	private RuleStore store;

	private Rule grandParentRule;

	private static class MyBindingSetHandler implements SinkBindingSetHandler {

		private BindingSet bs;

		@Override
		public CompletableFuture<Void> handle(BindingSet bs) {

			this.bs = bs;
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.complete((Void) null);
			return future;
		}

		public BindingSet getBindingSet() {
			return bs;
		}

	}

	@BeforeAll
	public void init() {
		Set<TriplePattern> antecedent = new HashSet<>();
		antecedent.add(new TriplePattern("?x <isParentOf> ?y"));
		antecedent.add(new TriplePattern("?y <isParentOf> ?z"));

		Set<TriplePattern> consequent = new HashSet<>();
		consequent.add(new TriplePattern("?x <isGrandParentOf> ?z"));
		grandParentRule = new Rule(antecedent, consequent);
	}

	@Test
	public void test() throws InterruptedException, ExecutionException {
		store = new RuleStore();
		TriplePattern tp = new TriplePattern("?x <isGrandParentOf> ?z");

		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp)), aBindingSetHandler));
		store.addRule(grandParentRule);

		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isParentOf> ?y"));

		ProactiveRule aStartRule = new ProactiveRule(new HashSet<>(), aGoal);
		store.addRule(aStartRule);

		ReasonerPlan rp = new ReasonerPlan(store, aStartRule);

		System.out.println(rp);

		BindingSet bs = new BindingSet();

		bs.addAll(new Table(new String[] {
				// @formatter:off
				"x", "y"
				// @formatter:on
		}, new String[] {
				// @formatter:off
				"<barry>,<fenna>", 
				"<janny>,<barry>", 
				"<fenna>,<benno>", 
				"<benno>,<loes>",
				// @formatter:on
		}).getData());

		rp.execute(bs);

		System.out.println("Result: " + aBindingSetHandler.getBindingSet());
		assertNotNull(aBindingSetHandler.getBindingSet());
		assertTrue(!aBindingSetHandler.getBindingSet().isEmpty());
	}
}