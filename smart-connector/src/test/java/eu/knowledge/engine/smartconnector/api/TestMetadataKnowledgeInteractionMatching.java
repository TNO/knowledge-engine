package eu.knowledge.engine.smartconnector.api;

import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test checks whether we can use knowledge interactions to react to (part
 * of) the metadata of a knowledge base We thought this would be impossible
 * right now as someone tried to disable this because of the slow graph matching
 * algorithm. However, we can still match on a small graph pattern, this may be
 * caused by the order in which it is checked whether one pattern is a subset of
 * the other.
 */
public class TestMetadataKnowledgeInteractionMatching {
	private static final Logger LOG = LoggerFactory.getLogger(TestDynamicSemanticComposition.class);

	private static KnowledgeNetwork kn;
	private static KnowledgeBaseImpl kb1, kb2;
	private static PrefixMappingMem prefixes;

	@BeforeEach
	public void setup() {
		kn = new KnowledgeNetwork();
		kb1 = new KnowledgeBaseImpl("Kb1");

		prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ke", Vocab.ONTO_URI);
	}

	@Test
	public void testNewKB() {
		ReactKnowledgeInteraction rKI = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.NEW_KNOWLEDGE_PURPOSE))),
				new GraphPattern(prefixes, "?k ke:hasName ?n . ?k ke:hasDescription ?d ."), null, true);
		kb1.register(rKI, ((anRKI, aReactExchangeInfo) -> {
			var argument = aReactExchangeInfo.getArgumentBindings();
			Iterator<Binding> iter = argument.iterator();
			assertTrue(iter.hasNext(), "There should be matches on metadata when a new KB is registered.");
			return new BindingSet();
		}));
		kn.addKB(kb1);
		kn.sync();

		kb2 = new KnowledgeBaseImpl("Kb2");
		kn.addKB(kb2);
		kn.sync();
	}

	@Test
	public void testChangedKB() {
		ReactKnowledgeInteraction rKI = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.CHANGED_KNOWLEDGE_PURPOSE))),
				new GraphPattern(prefixes, "?k ke:hasName ?n . ?k ke:hasDescription ?d ."), null, true);
		kb1.register(rKI, ((anRKI, aReactExchangeInfo) -> {
			var argument = aReactExchangeInfo.getArgumentBindings();
			Iterator<Binding> iter = argument.iterator();
			assertTrue(iter.hasNext(), "There should be matches on metadata when the network changes.");
			return new BindingSet();
		}));
		kn.addKB(kb1);
		kn.sync();

		kb2 = new KnowledgeBaseImpl("Kb2");
		kn.addKB(kb2);
		kn.sync();

		GraphPattern gp1 = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		kb1.register(aKI, (anAKI, anAnswerExchangeInfo) -> new BindingSet());
	}

	@Test
	public void testRemovedKB() {
		ReactKnowledgeInteraction rKI = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE)),
						new HashSet<>(Arrays.asList(Vocab.REMOVED_KNOWLEDGE_PURPOSE))),
				new GraphPattern(prefixes, "?k ke:hasName ?n . ?k ke:hasDescription ?d ."), null, true);
		kb1.register(rKI, ((anRKI, aReactExchangeInfo) -> {
			var argument = aReactExchangeInfo.getArgumentBindings();
			Iterator<Binding> iter = argument.iterator();
			assertTrue(iter.hasNext(), "There should be matches on metadata when a KB is removed from the network.");
			return new BindingSet();
		}));
		kn.addKB(kb1);
		kb2 = new KnowledgeBaseImpl("Kb2");
		kn.addKB(kb2);
		kn.sync();

		kb2.stop();
	}

	@AfterEach
	public void cleanup() {
		try {
			kb1.stop();
		} catch (IllegalStateException e) {
			LOG.error("Stopping a knowledge base should succeed: {}", e.getMessage());
		}
		try {
			kb2.stop();
		} catch (IllegalStateException e) {
			LOG.error("Stopping a knowledge base should succeed. {}", e.getMessage());
		}
	}
}
