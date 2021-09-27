package eu.knowledge.engine.smartconnector.util;

import java.util.Iterator;

import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.util.FmtUtils;

public class GraphPatternSerialization {
	/**
	 * Serialize the ElementPathBlock to a canonical graph pattern. That is, no
	 * prefixes, and no newlines but spaces between the triples.
	 */
	public static String serialize(ElementPathBlock epb) {
		Iterator<TriplePath> iter = epb.patternElts();

		StringBuilder sb = new StringBuilder();

		while (iter.hasNext()) {
			TriplePath tp = iter.next();
			sb.append(FmtUtils.stringForTriple(tp.asTriple(), new PrefixMappingZero()));
			sb.append(" . ");
		}

		return sb.toString();
	}
}
