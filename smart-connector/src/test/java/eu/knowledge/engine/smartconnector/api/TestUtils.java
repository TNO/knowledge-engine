package eu.knowledge.engine.smartconnector.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.graph.Node;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.TransformBindingSetHandler;
import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.ReasoningNode;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.AnswerBindingSetHandler;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.ReactBindingSetHandler;
import eu.knowledge.engine.smartconnector.impl.SmartConnectorBuilder;

public class TestUtils {

	/**
	 * The log facility of this class.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

	public static final GraphPattern SAREF_MEASUREMENT_PATTERN = new GraphPattern(
			"?m <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/core/Measurement> .",
			"?m <https://saref.etsi.org/core/hasValue> ?v .");

	private static final String KE_PREFIX = "https://www.interconnectproject.eu/";

	public static final SmartConnector getSmartConnector(final String aName) {
		return SmartConnectorBuilder.newSmartConnector(new KnowledgeBase() {

			@Override
			public URI getKnowledgeBaseId() {
				URI uri = null;
				try {
					uri = new URI(KE_PREFIX + aName);
				} catch (URISyntaxException e) {
					LOG.error("Could not parse the uri.", e);
				}
				return uri;
			}

			@Override
			public String getKnowledgeBaseDescription() {
				return null;
			}

			@Override
			public void smartConnectorReady(SmartConnector aSC) {
			}

			@Override
			public void smartConnectorConnectionLost(SmartConnector aSC) {
			}

			@Override
			public void smartConnectorConnectionRestored(SmartConnector aSC) {
			}

			@Override
			public String getKnowledgeBaseName() {
				return null;
			}

			@Override
			public void smartConnectorStopped(SmartConnector aSC) {
				// TODO Auto-generated method stub

			}
		}).create();
	}

	public static final Set<Binding> getSingleBinding(String name, String value) {
		Set<Binding> bindings = new HashSet<>();
		Binding b = new Binding();
		b.put(name, value);
		bindings.add(b);
		return bindings;
	}

	public static final BindingSet getSingleBinding(String name1, String value1, String name2, String value2) {
		BindingSet bindings = new BindingSet();
		Binding b = new Binding();
		b.put(name1, value1);
		b.put(name2, value2);
		bindings.add(b);
		return bindings;
	}

	public static final int getIntegerValueFromString(String value) {
		// TODO See issue #3
		Pattern p = Pattern.compile("^\"(\\w+)\"\\^\\^\\<http://www.w3.org/2001/XMLSchema#integer\\>$");
		Matcher m = p.matcher(value);
		return Integer.parseInt(m.group(1));
	}

	public static final String getStringValueFromInteger(int value) {
		return "\"" + value + "\"^^<http://www.w3.org/2001/XMLSchema#integer>";
	}

	public static final String getWithPrefix(String value) {
		return KE_PREFIX + value;
	}

	/**
	 * Output the text required to generate a sequence diagram via
	 * <a href="https://www.sequencediagram.org/">sequencediagram.org</a>.
	 * 
	 * @param proactiveKB
	 * @param kiType
	 * @param gp
	 * @param rn
	 * @param prefixes
	 * @throws URISyntaxException
	 */
	public static void printSequenceDiagram(String proactiveKB, String kiType, GraphPattern gp, ReasoningNode rn,
			PrefixMapping prefixes) throws URISyntaxException {

//		System.out.println(rn.toString());

		Queue<ReasoningNode> queue = new LinkedList<ReasoningNode>();
		queue.add(rn);

		List<String> actors = new ArrayList<>();
		actors.add(proactiveKB);

		class Pair {
			String first;
			String second;

			public Pair(String aFirst, String aSecond) {
				first = aFirst;
				second = aSecond;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((first == null) ? 0 : first.hashCode());
				result = prime * result + ((second == null) ? 0 : second.hashCode());
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Pair other = (Pair) obj;
				if (first == null) {
					if (other.first != null)
						return false;
				} else if (!first.equals(other.first))
					return false;
				if (second == null) {
					if (other.second != null)
						return false;
				} else if (!second.equals(other.second))
					return false;
				return true;
			}

		}

		// arrows to actors that need to come back
		Map<Pair, ReasoningNode> toFromExchanges = new HashMap<>();

		// arrows to actors that do not need to come back (only to ourselves)
		Map<Pair, ReasoningNode> toExchanges = new HashMap<>();

		while (!queue.isEmpty()) {

			ReasoningNode node = queue.poll();

			String currentActor = null;
			TransformBindingSetHandler bsh = node.getRule().getBindingSetHandler();
			ReactBindingSetHandler rbsh = null;
			AnswerBindingSetHandler absh = null;
			if (bsh instanceof ReactBindingSetHandler) {
				rbsh = (ReactBindingSetHandler) bsh;

				currentActor = rbsh.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString();
				if (!actors.contains(currentActor))
					actors.add(currentActor);
			} else if (bsh instanceof AnswerBindingSetHandler) {
				absh = (AnswerBindingSetHandler) bsh;
				currentActor = absh.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString();
				if (!actors.contains(currentActor))
					actors.add(currentActor);
			} else {
				currentActor = proactiveKB;
			}

			for (ReasoningNode neighbor : node.getAntecedentNeighbors().keySet()) {
				TransformBindingSetHandler bsh2 = neighbor.getRule().getBindingSetHandler();

				AnswerBindingSetHandler absh2 = null;
				ReactBindingSetHandler rbsh2 = null;
				if (bsh2 instanceof ReactBindingSetHandler) {
					rbsh2 = (ReactBindingSetHandler) bsh2;

					ReactKnowledgeInteraction react = (ReactKnowledgeInteraction) rbsh2.getKnowledgeInteractionInfo()
							.getKnowledgeInteraction();

					if (!react.isMeta()) {
						if (react.getResult() != null) {
							toFromExchanges.put(
									new Pair(proactiveKB,
											rbsh2.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString()),
									neighbor);
						} else {
							toExchanges.put(
									new Pair(proactiveKB,
											rbsh2.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString()),
									neighbor);
						}
					}

				} else if (bsh2 instanceof AnswerBindingSetHandler) {
					absh2 = (AnswerBindingSetHandler) bsh2;
					if (!absh2.getKnowledgeInteractionInfo().getKnowledgeInteraction().isMeta()) {
						toFromExchanges.put(new Pair(proactiveKB,
								absh2.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString()), neighbor);
					}
				} else {
					if (!neighbor.getRule().getAntecedent().isEmpty() && !neighbor.getRule().getConsequent().isEmpty())
						toExchanges.put(new Pair(proactiveKB, proactiveKB), neighbor);
				}
			}

			for (ReasoningNode neighbor : node.getConsequentNeighbors().keySet()) {
				TransformBindingSetHandler bsh2 = neighbor.getRule().getBindingSetHandler();

				AnswerBindingSetHandler absh2 = null;
				ReactBindingSetHandler rbsh2 = null;
				if (bsh2 instanceof ReactBindingSetHandler) {
					rbsh2 = (ReactBindingSetHandler) bsh2;

					ReactKnowledgeInteraction react = (ReactKnowledgeInteraction) rbsh2.getKnowledgeInteractionInfo()
							.getKnowledgeInteraction();

					if (!react.isMeta()) {
						if (react.getResult() != null) {
							toFromExchanges.put(
									new Pair(proactiveKB,
											rbsh2.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString()),
									neighbor);
						} else {
							toExchanges.put(
									new Pair(proactiveKB,
											rbsh2.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString()),
									neighbor);
						}
					}
				} else if (bsh2 instanceof AnswerBindingSetHandler) {
					absh2 = (AnswerBindingSetHandler) bsh2;

					if (!absh2.getKnowledgeInteractionInfo().getKnowledgeInteraction().isMeta()) {
						toFromExchanges.put(new Pair(proactiveKB,
								absh2.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString()), neighbor);
					}
				} else {
					if (!neighbor.getRule().getAntecedent().isEmpty() && !neighbor.getRule().getConsequent().isEmpty())
						toExchanges.put(new Pair(proactiveKB, proactiveKB), neighbor);
				}
			}

			queue.addAll(node.getAntecedentNeighbors().keySet());
			queue.addAll(node.getConsequentNeighbors().keySet());
		}

		String title = kiType + " data exchange";

		System.out.println("title " + title);

		for (String actor : actors) {
			System.out.println("participant " + new URI(actor).getPath().substring(1));
		}

		System.out.println("activate " + new URI(proactiveKB).getPath().substring(1));

		System.out.println("aboxright left of " + new URI(proactiveKB).getPath().substring(1) + ":"
				+ convertGP(prefixes, (rn.getRule().getAntecedent()).isEmpty() ? rn.getRule().getConsequent()
						: rn.getRule().getAntecedent()));

		for (Pair pair : toFromExchanges.keySet()) {
			ReasoningNode node = toFromExchanges.get(pair);

			assert node != null;

			BaseRule rule = node.getRule();
			boolean empty = rule.getAntecedent().isEmpty();

			System.out.println(new URI(pair.first).getPath().substring(1) + "->"
					+ new URI(pair.second).getPath().substring(1) + ":" + convertGP(prefixes, rule.getAntecedent())
					+ "\\n" + (empty ? "" : convertBindingSet(prefixes, node.getBindingSetToHandler())));

			if (!pair.second.equals(proactiveKB))
				System.out.println("deactivate " + new URI(proactiveKB).getPath().substring(1));

			System.out.println("activate " + new URI(pair.second).getPath().substring(1));
			System.out.println(new URI(pair.second).getPath().substring(1) + "-->"
					+ new URI(pair.first).getPath().substring(1) + ":" + convertGP(prefixes, rule.getConsequent())
					+ "\\n" + convertBindingSet(prefixes, node.getBindingSetFromHandler()));

			System.out.println("deactivate " + new URI(pair.second).getPath().substring(1));
			if (!pair.second.equals(proactiveKB))
				System.out.println("activate " + new URI(proactiveKB).getPath().substring(1));

		}

		for (Pair pair : toExchanges.keySet()) {

			ReasoningNode node = toExchanges.get(pair);

			BaseRule rule = node.getRule();

			System.out.println(new URI(pair.first).getPath().substring(1) + "->"
					+ new URI(pair.second).getPath().substring(1) + ":" + convertGP(prefixes, rule.getAntecedent())
					+ " => " + convertGP(prefixes, rule.getConsequent()) + "\\n"
					+ convertBindingSet(prefixes, node.getBindingSetToHandler()));

		}

	}

	private static String convertGP(PrefixMapping prefixes, Set<TriplePattern> aGraphPattern) {

		StringBuilder sb = new StringBuilder();

		for (TriplePattern tp : aGraphPattern) {
			Node ns = tp.getSubject();
			Node np = tp.getPredicate();
			Node no = tp.getObject();
			String s = ns.isURI() ? prefixes.shortForm(ns.getURI()) : ns.toString();
			String p = np.isURI() ? prefixes.shortForm(np.getURI()) : np.toString();
			String o = no.isURI() ? prefixes.shortForm(no.getURI()) : no.toString();
			sb.append(s).append(" ").append(p).append(" ").append(o).append(". ");
		}

		if (sb.length() == 0)
			sb.append("<empty>");

		return sb.toString();
	}

	private static String convertBindingSet(PrefixMapping prefixes,
			eu.knowledge.engine.reasoner.api.BindingSet toFromHandler) {

		int MAX = 5;
		String GAP = "  ";
		StringBuilder sb = new StringBuilder();

		if (!toFromHandler.isEmpty() && !toFromHandler.iterator().next().isEmpty()) {

			// header
			eu.knowledge.engine.reasoner.api.Binding forHeader = toFromHandler.iterator().next();

			Var[] vars = new Var[forHeader.size()];

			int count = 0;
			sb.append("**");
			for (Var var : forHeader.keySet()) {
				sb.append(var.toString()).append(GAP);
				vars[count] = var;
				count++;
			}
			sb.append("**");
			sb.append("\\n");

			// data
			// TODO if there are too many bindings, truncate the rows with '...'.
			Iterator<eu.knowledge.engine.reasoner.api.Binding> bIter = toFromHandler.iterator();
			while (bIter.hasNext()) {
				eu.knowledge.engine.reasoner.api.Binding b = bIter.next();
				for (Var var : vars) {
					sb.append(prefixes.shortForm(b.get(var).toString())).append(GAP);
				}
				sb.append("\\n");
			}
			sb.delete(sb.length() - 2, sb.length());

		} else {
			sb.append("<empty>");
		}
		return sb.toString();
	}

	public static String removeChars(String path) {
		return path.replace(":", "");
	}

	/**
	 * Just a quick and dirty way of generating dit notation of a reasoning graph.
	 * Not sure if I really want to keep this, though. Can be visualized online
	 * <a href="https://dreampuf.github.io/GraphvizOnline/">here</a>.
	 * 
	 *
	 * 
	 * @param aRootNodeName
	 * @param rn
	 */
//	public static void printReasoningNodeDotNotation(String aRootNodeName, ReasoningNode rn) {
//		Queue<ReasoningNode> queue = new LinkedList<ReasoningNode>();
//		queue.add(rn);
//
//		System.out.println("digraph { concentrate=true");
//
//		Set<String> edges = new HashSet<String>();
//
//		while (!queue.isEmpty()) {
//			ReasoningNode node = queue.poll();
//
//			node.getRule().getBindingSetHandler();
//
//			String currentNodeName = node.getRule().getName();
//
//			if (currentNodeName == null)
//				currentNodeName = aRootNodeName;
//
//			for (ReasoningNode antecedentNode : node.getAntecedentNeighbors().keySet()) {
//				String neighborNodeName = antecedentNode.getRule().getName();
//
//				if (neighborNodeName == null)
//					neighborNodeName = "<unknown>";
//
//				edges.add(neighborNodeName + " -> " + currentNodeName);
//			}
//
//			for (ReasoningNode antecedentNode : node.getConsequentNeighbors().keySet()) {
//				String neighborNodeName = antecedentNode.getRule().getName();
//
//				if (neighborNodeName == null)
//					neighborNodeName = "<unknown>";
//
//				edges.add(currentNodeName + " -> " + neighborNodeName);
//			}
//
//			queue.addAll(node.getAntecedentNeighbors().keySet());
//			queue.addAll(node.getConsequentNeighbors().keySet());
//		}
//
//		for (String edge : edges) {
//			System.out.println(edge);
//		}
//
//		System.out.println("}");
//	}
}
