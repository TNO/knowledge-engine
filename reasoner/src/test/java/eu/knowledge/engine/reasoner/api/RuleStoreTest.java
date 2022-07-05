/**
 * 
 */
package eu.knowledge.engine.reasoner.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.util.FmtUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BindingSetHandler;
import eu.knowledge.engine.reasoner.ReactiveRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.Table;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

/**
 * @author nouwtb
 *
 */
class RuleStoreTest {

	private static final Logger LOG = LoggerFactory.getLogger(RuleStoreTest.class);
	private Rule produceTargetsRule;
	private Rule produceHvtRule;
	private Rule produceCountryRule;
	private Rule consumeHvtNameRule;

	@Test
	void test() {

		RuleStore store = new RuleStore();

		produceTargetsRule = new ReactiveRule(new HashSet<>(), new HashSet<>(
				Arrays.asList(new TriplePattern("?id rdf:type <Target>"), new TriplePattern("?id <hasName> ?name"))),
				new BindingSetHandler() {

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

						future.complete(newBS);

						return future;
					}

				});
		store.addRule(produceTargetsRule);

		produceHvtRule = new ReactiveRule(
				new HashSet<>(Arrays.asList(new TriplePattern("?id rdf:type <Target>"),
						new TriplePattern("?id <hasCountry> \"Russia\""))),
				new HashSet<>(Arrays.asList(new TriplePattern("?id rdf:type <HighValueTarget>"))));
		store.addRule(produceHvtRule);

		produceCountryRule = new ReactiveRule(
				new HashSet<>(Arrays.asList(new TriplePattern("?id rdf:type <Target>"),
						new TriplePattern("?id <hasName> ?name"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?id <hasCountry> ?c"))), new BindingSetHandler() {

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
							} else if (incomingB.containsKey("id")
									&& incomingB.get("id").equals(SSE.parseNode("<https://www.tno.nl/target0>"))) {
								id = incomingB.get("id");
								resultBinding.put("id", FmtUtils.stringForNode(id, new PrefixMappingZero()));
								resultBinding.put("c", "\"Holland\"");
							} else {
								id = incomingB.get("id");
								resultBinding.put("id", FmtUtils.stringForNode(id, new PrefixMappingZero()));
								resultBinding.put("c", "\"Belgium\"");
							}
							newBS.add(resultBinding);
						}
						CompletableFuture<BindingSet> future = new CompletableFuture<>();
						future.complete(newBS);
						return future;
					}
				});
		store.addRule(produceCountryRule);

		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?id rdf:type <HighValueTarget>"));
		objective.add(new TriplePattern("?id <hasName> ?name"));
		consumeHvtNameRule = new ReactiveRule(objective, new HashSet<>(), new BindingSetHandler() {

			public CompletableFuture<BindingSet> handle(BindingSet incomingBS) {

				LOG.info("receives {}", incomingBS);
				CompletableFuture<BindingSet> future = new CompletableFuture<BindingSet>();
				future.complete(new BindingSet());
				return future;
			}
		});
		store.addRule(consumeHvtNameRule);

		Set<Rule> rules = store.getRules();

		Map<Rule, Set<Rule>> ruleToAntecedentNeighbors = new HashMap<>();
		ruleToAntecedentNeighbors.put(produceTargetsRule, new HashSet<>());
		ruleToAntecedentNeighbors.put(this.produceCountryRule, new HashSet<>(Arrays.asList(this.produceTargetsRule)));
		ruleToAntecedentNeighbors.put(this.produceHvtRule,
				new HashSet<>(Arrays.asList(this.produceTargetsRule, this.produceCountryRule)));
		ruleToAntecedentNeighbors.put(this.consumeHvtNameRule,
				new HashSet<>(Arrays.asList(this.produceHvtRule, this.produceTargetsRule)));

		Map<Rule, Set<Rule>> ruleToConsequentNeighbors = new HashMap<>();
		ruleToConsequentNeighbors.put(this.produceTargetsRule,
				new HashSet<>(Arrays.asList(this.produceCountryRule, this.produceHvtRule, this.consumeHvtNameRule)));
		ruleToConsequentNeighbors.put(this.produceCountryRule, new HashSet<>(Arrays.asList(this.produceHvtRule)));
		ruleToConsequentNeighbors.put(this.produceHvtRule, new HashSet<>(Arrays.asList(this.consumeHvtNameRule)));
		ruleToConsequentNeighbors.put(this.consumeHvtNameRule, new HashSet<>());

		long start = System.nanoTime();
		for (Rule r : rules) {

			LOG.info("-------------------");
			Set<Rule> antecedentNeighbors = store.getAntecedentNeighbors(r).keySet();

			assertEquals(ruleToAntecedentNeighbors.get(r), antecedentNeighbors);

			for (Rule aNeighbor : antecedentNeighbors) {
				LOG.info("{} -> {}", aNeighbor.getAntecedent(), aNeighbor.getConsequent());
			}

			LOG.info("\t\t{} -> {}", r.getAntecedent(), r.getConsequent());

			Set<Rule> consequentNeighbors = store.getConsequentNeighbors(r).keySet();

			assertEquals(ruleToConsequentNeighbors.get(r), consequentNeighbors);

			for (Rule aNeighbor : consequentNeighbors) {
				LOG.info("\t\t\t{} -> {}", aNeighbor.getAntecedent(), aNeighbor.getConsequent());
			}
		}

		LOG.info("Time: {} ms", ((double) (System.nanoTime() - start)) / 1000000d);

		store.printGraphVizCode();
	}
}
