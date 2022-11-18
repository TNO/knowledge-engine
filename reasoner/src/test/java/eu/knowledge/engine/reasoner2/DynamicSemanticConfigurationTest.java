package eu.knowledge.engine.reasoner2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.util.FmtUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.Table;
import eu.knowledge.engine.reasoner.TaskBoard;
import eu.knowledge.engine.reasoner.TransformBindingSetHandler;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

@TestInstance(Lifecycle.PER_CLASS)
public class DynamicSemanticConfigurationTest {

	private static final Logger LOG = LoggerFactory.getLogger(DynamicSemanticConfigurationTest.class);

	private RuleStore ruleStore;

	@BeforeAll
	public void init() throws URISyntaxException {
		ruleStore = new RuleStore();

		// Initialize
		ruleStore.addRule(new Rule(new HashSet<>(), new HashSet<>(
				Arrays.asList(new TriplePattern("?id <type> <Target>"), new TriplePattern("?id <hasName> ?name"))),
				new TransformBindingSetHandler() {

					private Table data = new Table(new String[] {
				//@formatter:off
							"id", "name"
							//@formatter:on
					}, new String[] {
				//@formatter:off
							"<https://www.tno.nl/target0>,\"Eek\"",
							"<https://www.tno.nl/target1>,\"Bla\"",
							//@formatter:on
					});

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {

						BindingSet newBS = new BindingSet();
						if (!bs.isEmpty()) {

							for (Binding b : bs) {

								if (!b.isEmpty()) {
									Set<Map<String, String>> map = data.query(b.toMap());
									if (!map.isEmpty())
										newBS.addAll(map);
								} else {
									newBS.addAll(this.data.getData());
								}
							}
						} else {
							newBS.addAll(this.data.getData());
						}
						CompletableFuture<BindingSet> future = new CompletableFuture<>();

						future.handle((r, e) -> {

							if (r == null) {
								LOG.error("An exception has occured", e);
								return null;
							} else {
								return r;
							}
						});
						future.complete(newBS);

						return future;
					}

				}));

		ruleStore.addRule(new Rule(
				new HashSet<>(Arrays.asList(new TriplePattern("?id <type> <Target>"),
						new TriplePattern("?id <hasCountry> \"Russia\""))),
				new HashSet<>(Arrays.asList(new TriplePattern("?id <type> <HighValueTarget>")))));

		ruleStore.addRule(new Rule(
				new HashSet<>(Arrays.asList(new TriplePattern("?id <type> <Target>"),
						new TriplePattern("?id <hasName> ?name"))),

				new HashSet<>(Arrays.asList(new TriplePattern("?id <hasCountry> ?c"))),
				new TransformBindingSetHandler() {

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {
						assert bs.iterator().hasNext();
						BindingSet newBS = new BindingSet();
						for (Binding incomingB : bs) {
							Binding resultBinding = new Binding();

							Node id;
							if (incomingB.containsKey("id")
									&& incomingB.get("id").equals(SSE.parseNode("<https://www.tno.nl/target1>"))) {

								id = incomingB.get("id");
								resultBinding.put("id", FmtUtils.stringForNode(id, new PrefixMappingZero()));
								resultBinding.put("c", "\"Russia\"");
								resultBinding.put("lang", "\"Russian\"");
							} else if (incomingB.containsKey("id")
									&& incomingB.get("id").equals(SSE.parseNode("<https://www.tno.nl/target0>"))) {
								id = incomingB.get("id");
								resultBinding.put("id", FmtUtils.stringForNode(id, new PrefixMappingZero()));
								resultBinding.put("c", "\"Holland\"");
								resultBinding.put("lang", "\"Dutch\"");
							} else {
								id = incomingB.get("id");
								resultBinding.put("id", FmtUtils.stringForNode(id, new PrefixMappingZero()));
								resultBinding.put("c", "\"Belgium\"");
								resultBinding.put("lang", "\"Flemish\"");
							}
							newBS.add(resultBinding);
						}
						CompletableFuture<BindingSet> future = new CompletableFuture<>();

						future.handle((r, e) -> {

							if (r == null) {
								LOG.error("An exception has occured", e);
								return null;
							} else {
								return r;
							}
						});
						future.complete(newBS);
						return future;
					}
				}));
	}

	@Test
	public void test() throws InterruptedException, ExecutionException {
		// Formulate objective
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?id <type> <HighValueTarget>"));
		objective.add(new TriplePattern("?id <hasName> ?name"));

		ProactiveRule aRule = new ProactiveRule(objective, new HashSet<>());
		ruleStore.addRule(aRule);

		// Make a plan
		ReasonerPlan root = new ReasonerPlan(this.ruleStore, aRule);
		LOG.info("\n{}", root);
		BindingSet bs = new BindingSet();

		// Start reasoning
		root.execute(bs);
		BindingSet bind = root.getResults();

		LOG.info("bindings: {}", bind);
		assertFalse(bind.isEmpty());

	}
}
