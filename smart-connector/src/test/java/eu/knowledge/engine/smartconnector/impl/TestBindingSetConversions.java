package eu.knowledge.engine.smartconnector.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.junit.jupiter.api.Test;

import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;

class TestBindingSetConversions {

	private OpenReasonerProcessor rp = new OpenReasonerProcessor(new HashSet<>(), null, new HashSet<>());

	/**
	 * Test whether converting typed literals between reasoner and knowledge engine
	 * binding sets works correctly.
	 */
	@Test
	void testTypedLiteral() {

		String varStringVersion = "a";
		Var varNodeVersion = Var.alloc(varStringVersion);
		Node literalNodeVersion = NodeFactory.createLiteral("true", XSDDatatype.XSDboolean);
		var literalStringVersion = "\"true\"^^<http://www.w3.org/2001/XMLSchema#boolean>";

		BindingSet bs = new BindingSet();
		Binding b = new Binding();
		b.put(varStringVersion, literalStringVersion);
		bs.add(b);
		eu.knowledge.engine.reasoner.api.BindingSet reasonerBS = rp.testerTo(bs);
		assertEquals(literalNodeVersion, reasonerBS.iterator().next().get(varStringVersion));

		eu.knowledge.engine.reasoner.api.BindingSet bsR = new eu.knowledge.engine.reasoner.api.BindingSet();
		eu.knowledge.engine.reasoner.api.Binding bR = new eu.knowledge.engine.reasoner.api.Binding();
		bR.put(varNodeVersion, (Node) literalNodeVersion);
		bsR.add(bR);
		BindingSet otherBS = rp.testerFrom(bsR);
		assertEquals(literalStringVersion, otherBS.iterator().next().get(varStringVersion));
	}

	/**
	 * Make some binding set translation methods available.
	 */
	public static class OpenReasonerProcessor extends ReasonerProcessor {
		public OpenReasonerProcessor(Set<KnowledgeInteractionInfo> knowledgeInteractions, MessageRouter messageRouter,
				Set<Rule> someDomainKnowledge) {
			super(knowledgeInteractions, messageRouter, someDomainKnowledge);
		}

		public eu.knowledge.engine.reasoner.api.BindingSet testerTo(BindingSet bs) {
			return this.translateBindingSetTo(bs);
		}

		public BindingSet testerFrom(eu.knowledge.engine.reasoner.api.BindingSet bs) {
			return this.translateBindingSetFrom(bs);
		}
	}

}
