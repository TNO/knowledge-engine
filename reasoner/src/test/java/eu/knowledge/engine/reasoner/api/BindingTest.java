package eu.knowledge.engine.reasoner.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.util.FmtUtils;
import org.junit.Test;


public class BindingTest {

	@Test
	public void testGraphPatternBindingSets() {
		TriplePattern t1 = new TriplePattern("?a <type> <Sensor>");
		TriplePattern t2 = new TriplePattern("?a <hasVal> ?b");
		TripleVarBinding tb1 = new TripleVarBinding();
		tb1.put(new TripleVar(t1, "?a"), SSE.parseNode("<sensor1>"));

		TripleVarBinding tb2 = new TripleVarBinding();
		tb2.put(new TripleVar(t2, "?b"), "22");
		tb2.put(new TripleVar(t2, "?a"), "<sensor1>");
		tb2.put(new TripleVar(t1, "?a"), "<sensor1>");

		Set<TriplePattern> aGraphPattern = new HashSet<>(Arrays.asList(t1, t2));
		TripleVarBindingSet gbs = new TripleVarBindingSet(aGraphPattern);
		gbs.add(tb1);
		gbs.add(tb2);

		System.out.println(gbs);

		BindingSet bs = gbs.toBindingSet();
		System.out.println(bs);

		TripleVarBindingSet gbsReturned = bs.toGraphBindingSet(aGraphPattern);

		System.out.println(gbsReturned);
	}

	@Test
	public void testTripleVarBinding() {
		TriplePattern tp1 = new TriplePattern("?s <type> <Sensor>");
		TriplePattern tp2 = new TriplePattern("?s <hasVal> ?v");

		TripleVarBinding tvb1 = new TripleVarBinding();
		tvb1.put(new TripleVar(tp1, "?s"), "<sensor1>");

		TripleVarBinding tvb2 = new TripleVarBinding();
		tvb2.put(new TripleVar(tp2, "?s"), "<sensor1>");
		tvb2.put(new TripleVar(tp2, "?v"), "22");

		System.out.println(tvb1.merge(tvb2));
	}

	@Test
	public void testTripleVarBindingComplication() {
		TriplePattern tp1 = new TriplePattern("?s <type> ?t");
		TriplePattern tp2 = new TriplePattern("?s <hasVal> ?v");

		TripleVarBindingSet gbs1 = new TripleVarBindingSet(new HashSet<>(Arrays.asList(tp1, tp2)));
		TripleVarBinding tvb1 = new TripleVarBinding();
		tvb1.put(new TripleVar(tp1, "?s"), "<sensor1>");
		tvb1.put(new TripleVar(tp1, "?t"), "<Sensor>");
		tvb1.put(new TripleVar(tp2, "?s"), "<sensor1>");
		tvb1.put(new TripleVar(tp2, "?v"), "22");
		gbs1.add(tvb1);

		TripleVarBindingSet gbs2 = new TripleVarBindingSet(new HashSet<>(Arrays.asList(tp1, tp2)));
		TripleVarBinding tvb2 = new TripleVarBinding();
		tvb2.put(new TripleVar(tp1, "?s"), "<sensor1>");
		tvb2.put(new TripleVar(tp1, "?t"), "<Device>");
		gbs2.add(tvb2);

		TripleVarBindingSet merge = gbs1.merge(gbs2);
		System.out.println(merge);

		assertTrue(!merge.isEmpty());

		// we want two full TripleVarBindings but one with Device and the other with
		// Sensor. The question is how to achieve this when there is only a single
		// TripleVar,value in common. And do we want the same behaviour if the common
		// TripleVar,value is the 22?
	}

	@Test
	public void testTripleVarBindingComplication2() {
		TriplePattern tp1 = new TriplePattern("?s <type> ?t");
		TriplePattern tp2 = new TriplePattern("?s <hasVal> ?v");

		TripleVarBindingSet gbs1 = new TripleVarBindingSet(new HashSet<>(Arrays.asList(tp1, tp2)));
		TripleVarBinding tvb1 = new TripleVarBinding();
		tvb1.put(new TripleVar(tp1, "?s"), "<sensor1>");
		tvb1.put(new TripleVar(tp1, "?t"), "<Sensor>");
		tvb1.put(new TripleVar(tp2, "?s"), "<sensor1>");
		tvb1.put(new TripleVar(tp2, "?v"), "22");
		gbs1.add(tvb1);

		TripleVarBindingSet gbs2 = new TripleVarBindingSet(new HashSet<>(Arrays.asList(tp1, tp2)));
		TripleVarBinding tvb2 = new TripleVarBinding();
		tvb2.put(new TripleVar(tp2, "?s"), "<sensor2>");
		tvb2.put(new TripleVar(tp2, "?v"), "22");
		gbs2.add(tvb2);

		TripleVarBindingSet merge = gbs1.merge(gbs2);
		System.out.println(merge);

		assertTrue(!merge.isEmpty());

		// Do we want the same behavior (as complication1) if the common
		// TripleVar,value is the 22? Probably not, so, apparently, we cannot just
		// blindly look at the bindingsets and ignore the triples when merging.
		// Is it caused by the type of the 'thing in common'? Do we need to distinguish
		// between IRIs and Literals?
	}

	@Test
	public void testParseAndFormatBinding() {
		var b = new Binding("a", "<sensor2>");
		Node node = b.get("a");
		assertEquals("<sensor2>", FmtUtils.stringForNode(node, new PrefixMappingZero()));
	}
}
