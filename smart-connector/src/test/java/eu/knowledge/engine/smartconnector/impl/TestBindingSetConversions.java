package eu.knowledge.engine.smartconnector.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.junit.jupiter.api.Test;

import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;

class TestBindingSetConversions {

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
		eu.knowledge.engine.reasoner.api.BindingSet reasonerBS = Util.translateFromApiBindingSet(bs);
		assertEquals(literalNodeVersion, reasonerBS.iterator().next().get(varStringVersion));

		eu.knowledge.engine.reasoner.api.BindingSet bsR = new eu.knowledge.engine.reasoner.api.BindingSet();
		eu.knowledge.engine.reasoner.api.Binding bR = new eu.knowledge.engine.reasoner.api.Binding();
		bR.put(varNodeVersion, (Node) literalNodeVersion);
		bsR.add(bR);
		BindingSet otherBS = Util.translateToApiBindingSet(bsR);
		assertEquals(literalStringVersion, otherBS.iterator().next().get(varStringVersion));
	}

	/**
	 * Test whether converting typed literals between reasoner and knowledge engine
	 * binding sets works correctly.
	 */
	@Test
	void testTypedLiteralString() {

		String varStringVersion = "a";
		Var varNodeVersion = Var.alloc(varStringVersion);
		Node literalNodeVersion = NodeFactory.createLiteral("bla", XSDDatatype.XSDstring);
		var literalStringVersion = "\"bla\"^^<http://www.w3.org/2001/XMLSchema#string>";
		var literalSimpleStringVersion = "\"bla\"";

		BindingSet bs = new BindingSet();
		Binding b = new Binding();
		b.put(varStringVersion, literalStringVersion);
		bs.add(b);
		eu.knowledge.engine.reasoner.api.BindingSet reasonerBS = Util.translateFromApiBindingSet(bs);
		assertTrue(literalNodeVersion.sameValueAs(reasonerBS.iterator().next().get(varStringVersion)));

		eu.knowledge.engine.reasoner.api.BindingSet bsR = new eu.knowledge.engine.reasoner.api.BindingSet();
		eu.knowledge.engine.reasoner.api.Binding bR = new eu.knowledge.engine.reasoner.api.Binding();
		bR.put(varNodeVersion, (Node) literalNodeVersion);
		bsR.add(bR);
		BindingSet otherBS = Util.translateToApiBindingSet(bsR);
		assertEquals(literalSimpleStringVersion, otherBS.iterator().next().get(varStringVersion));
	}
}
