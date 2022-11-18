package eu.knowledge.engine.reasoner2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.util.FmtUtils;
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
import eu.knowledge.engine.reasoner.TransformBindingSetHandler;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.Util;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

@TestInstance(Lifecycle.PER_CLASS)
public class BackwardTest {
  private RuleStore store;
  private static final Logger LOG = LoggerFactory.getLogger(BackwardTest.class);

  @BeforeAll
	public void init() throws URISyntaxException {
		// Initialize
		store = new RuleStore();
		store.addRule(new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?a <type> <Sensor>"), new TriplePattern("?a <hasValInC> ?b"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"a", "b"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor1>,\"22.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						"<sensor2>,\"21.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						//@formatter:on
				}))));

		store.addRule(new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?e <type> <Sensor>"), new TriplePattern("?e <hasValInF> ?f"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"e", "f"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor3>,\"69.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						"<sensor4>,\"71.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						//@formatter:on
				}))));

		store.addRule(new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?k <type> <Sensor>"), new TriplePattern("?k <hasValInK> ?w"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"k", "w"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor5>,\"295.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						"<sensor6>,\"294.0\"^^<http://www.w3.org/2001/XMLSchema#float>"
						//@formatter:on
				}))));

		store.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <Sensor>"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <Device>")))));

		store.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?x <hasValInF> ?y"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?x <hasValInC> ?z"))), new TransformBindingSetHandler() {

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {
						StringBuilder bindings = new StringBuilder();
						for (Binding b : bs) {
							Object kelvinObj = b.get("y").getLiteralValue();
							Float celsius = null;
							if (kelvinObj instanceof BigDecimal) {
								celsius = (Float) ((BigDecimal) kelvinObj).floatValue();
							} else if (kelvinObj instanceof Integer) {
								celsius = (float) ((int) kelvinObj);
							} else {
								celsius = (Float) kelvinObj;
							}
							bindings.append("z=" + "\"" + convert(celsius)
									+ "\"^^<http://www.w3.org/2001/XMLSchema#float>" + ",x="
									+ FmtUtils.stringForNode(b.get("x"), new PrefixMappingZero()) + "|");
						}
						BindingSet bindingSet = Util.toBindingSet(bindings.toString());

						CompletableFuture<BindingSet> future = new CompletableFuture<>();

						future.handle((r, e) -> {

							if (r == null) {
								LOG.error("An exception has occured in Celsius <-> Fahrenheit test", e);
								return null;
							} else {
								return r;
							}
						});
						future.complete(bindingSet);
						return future;
					}

					public float convert(float fahrenheit) {
						return ((fahrenheit - 32) * 5) / 9;
					}

				}));

		store.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?u <hasValInK> ?i"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?u <hasValInC> ?z"))), new TransformBindingSetHandler() {

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {
						StringBuilder bindings = new StringBuilder();
						for (Binding b : bs) {

							b.get("i").getLiteralLexicalForm();

							Object kelvinObj = b.get("i").getLiteralValue();
							Float kelvin = null;
							if (kelvinObj instanceof BigDecimal) {
								kelvin = (Float) ((BigDecimal) kelvinObj).floatValue();
							} else {
								kelvin = (Float) kelvinObj;
							}
							bindings.append("z=")
									.append("\"" + convert(kelvin) + "\"^^<http://www.w3.org/2001/XMLSchema#float>")
									.append(",x=").append(FmtUtils.stringForNode(b.get("u"), new PrefixMappingZero()))
									.append("|");
						}
						BindingSet bindingSet = Util.toBindingSet(bindings.toString());

						CompletableFuture<BindingSet> future = new CompletableFuture<>();

						future.handle((r, e) -> {

							if (r == null) {
								LOG.error("An exception has occured in Celsius <-> Kelvin test", e);
								return null;
							} else {
								return r;
							}
						});
						future.complete(bindingSet);
						return future;
					}

					public float convert(float kelvin) {
						return kelvin - 273;
					}
				}));

		store.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?u <hasValInK> ?i"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?u <hasValInF> ?z"))), new TransformBindingSetHandler() {

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {

						StringBuilder bindings = new StringBuilder();
						for (Binding b : bs) {

							Object kelvinObj = b.get("i").getLiteralValue();
							Float kelvin = null;
							if (kelvinObj instanceof BigDecimal) {
								kelvin = (Float) ((BigDecimal) kelvinObj).floatValue();
							} else {
								kelvin = (Float) kelvinObj;
							}

							bindings.append("z=")
									.append("\"" + convertK(kelvin) + "\"^^<http://www.w3.org/2001/XMLSchema#float>")
									.append(",u=").append(FmtUtils.stringForNode(b.get("u"), new PrefixMappingZero()))
									.append("|");
						}

						BindingSet bindingSet = Util.toBindingSet(bindings.toString());

						CompletableFuture<BindingSet> future = new CompletableFuture<>();

						future.handle((r, e) -> {

							if (r == null) {
								LOG.error("An exception has occured in Fahrenheit <-> Kelvin test", e);
								return null;
							} else {
								return r;
							}
						});
						future.complete(bindingSet);
						return future;

					}

					public float convertK(float kelvin) {
						return ((kelvin * 9) / 5) - 459;
					}
				}));

		store.addRule(new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?i <type> <Sensor>"), new TriplePattern("?i <inRoom> ?j"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"i", "j"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor3>,<kitchen>",
						"<sensor4>,<livingroom>",
						"<sensor1>,<bathroom>",
						"<sensor2>,<bedroom>",
						//@formatter:on
				}))));
	}

  // TODO: Add more detailed assertions: What do I expect here?
  @Test
	public void testConverter() {
    // Formulate objective
		HashSet<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		ProactiveRule converterRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(converterRule);

		ReasonerPlan plan = new ReasonerPlan(store, converterRule);

    plan.execute(new BindingSet(new Binding()));
		BindingSet result = plan.getResults();

		System.out.println("bindings: " + result);
		assertFalse(result.isEmpty());

	}
}
