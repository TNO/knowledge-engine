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
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.AntSide;
import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.ConsSide;
import eu.knowledge.engine.reasoner.ReasonerPlan;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulenode.RuleNode;
import eu.knowledge.engine.smartconnector.impl.KnowledgeInteractionInfo;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.AnswerBindingSetHandler;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.ReactBindingSetHandler;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.ReactVoidBindingSetHandler;

public class TestUtils {

	/**
	 * The log facility of this class.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

	public static final GraphPattern SAREF_MEASUREMENT_PATTERN = new GraphPattern(
			"?m <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/core/Measurement> .",
			"?m <https://saref.etsi.org/core/hasValue> ?v .");

	private static final String KE_PREFIX = "https://www.interconnectproject.eu/";

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
	public static void printSequenceDiagram(String proactiveKB, String kiType, GraphPattern gp, ReasonerPlan plan,
			PrefixMapping prefixes) throws URISyntaxException {

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

		RuleNode rn = plan.getStartNode();
//		System.out.println(rn.toString());

		Queue<RuleNode> queue = new LinkedList<RuleNode>();
		queue.add(rn);

		List<String> actors = new ArrayList<>();
		actors.add(proactiveKB);

		// arrows to actors that need to come back
		Map<Pair, RuleNode> toFromExchanges = new HashMap<>();

		// arrows to actors that do not need to come back (only to ourselves)
		Map<Pair, RuleNode> toExchanges = new HashMap<>();

		Set<RuleNode> visited = new HashSet<>();

		while (!queue.isEmpty()) {

			RuleNode node = queue.poll();
			if (!visited.contains(node)) {

				String currentActor = null;

				BaseRule rule = node.getRule();
				ReactBindingSetHandler rbsh = null;
				AnswerBindingSetHandler absh = null;

				if (rule instanceof Rule) {

					Rule r = (Rule) rule;
					KnowledgeInteractionInfo info = null;
					if (r.getAntecedent().isEmpty()) {
						if (!r.getConsequent().isEmpty()) {
							// only consequent
							if (r.getBindingSetHandler() instanceof AnswerBindingSetHandler)
								info = ((AnswerBindingSetHandler) r.getBindingSetHandler())
										.getKnowledgeInteractionInfo();
							currentActor = info.getKnowledgeBaseId().toString();
							if (!actors.contains(currentActor))
								actors.add(currentActor);
						}

					} else {
						if (rule.getConsequent().isEmpty()) {
							// only antecedent
							info = ((ReactVoidBindingSetHandler) r.getSinkBindingSetHandler())
									.getKnowledgeInteractionInfo();

							currentActor = info.getKnowledgeBaseId().toString();
							if (!actors.contains(currentActor))
								actors.add(currentActor);

						} else {
							// both antecedent and consequent
							if (r.getBindingSetHandler() instanceof ReactBindingSetHandler) {
								info = ((ReactBindingSetHandler) r.getBindingSetHandler())
										.getKnowledgeInteractionInfo();

								currentActor = info.getKnowledgeBaseId().toString();
								if (!actors.contains(currentActor))
									actors.add(currentActor);

							}
						}
					}
				} else {
					currentActor = proactiveKB;
				}

				if (node instanceof AntSide) {
					for (RuleNode neighbor : ((AntSide) node).getAntecedentNeighbours().keySet()) {

						BaseRule neighborRule = neighbor.getRule();

						KnowledgeInteractionInfo info = null;

						if (!(neighborRule instanceof Rule))
							continue;

						Rule rule2 = (Rule) neighborRule;
						if (rule2.getAntecedent().isEmpty()) {
							if (!rule2.getConsequent().isEmpty()) {
								// only consequent
								if (rule2.getBindingSetHandler() instanceof AnswerBindingSetHandler)
									info = ((AnswerBindingSetHandler) rule2.getBindingSetHandler())
											.getKnowledgeInteractionInfo();
								if (!info.isMeta())
									toFromExchanges.put(new Pair(proactiveKB, info.getKnowledgeBaseId().toString()),
											neighbor);
							}

						} else {
							if (rule2.getConsequent().isEmpty()) {
								// only antecedent
								info = ((ReactVoidBindingSetHandler) rule2.getSinkBindingSetHandler())
										.getKnowledgeInteractionInfo();
								if (!info.isMeta())
									toExchanges.put(new Pair(proactiveKB, info.getKnowledgeBaseId().toString()),
											neighbor);

							} else {
								// both antecedent and consequent

								if (rule2.getBindingSetHandler() instanceof ReactBindingSetHandler) {

									info = ((ReactBindingSetHandler) rule2.getBindingSetHandler())
											.getKnowledgeInteractionInfo();
									if (!info.isMeta())
										toFromExchanges.put(new Pair(proactiveKB, info.getKnowledgeBaseId().toString()),
												neighbor);

								} else {
									toExchanges.put(new Pair(proactiveKB, proactiveKB), neighbor);
								}

							}
						}
					}
					queue.addAll(((AntSide) node).getAntecedentNeighbours().keySet());
				}

				if (node instanceof ConsSide) {
					for (RuleNode neighbor : ((ConsSide) node).getConsequentNeighbours().keySet()) {

						BaseRule neighborRule = neighbor.getRule();

						KnowledgeInteractionInfo info = null;

						if (!(neighborRule instanceof Rule))
							continue;

						Rule rule2 = (Rule) neighborRule;
						if (rule2.getAntecedent().isEmpty()) {
							if (!rule2.getConsequent().isEmpty()) {
								// only consequent
								if (rule2.getBindingSetHandler() instanceof AnswerBindingSetHandler)
									info = ((AnswerBindingSetHandler) rule2.getBindingSetHandler())
											.getKnowledgeInteractionInfo();
								if (!info.isMeta())
									toFromExchanges.put(new Pair(proactiveKB, info.getKnowledgeBaseId().toString()),
											neighbor);
							}

						} else {
							if (rule2.getConsequent().isEmpty()) {
								// only antecedent
								info = ((ReactVoidBindingSetHandler) rule2.getSinkBindingSetHandler())
										.getKnowledgeInteractionInfo();
								if (!info.isMeta())
									toExchanges.put(new Pair(proactiveKB, info.getKnowledgeBaseId().toString()),
											neighbor);

							} else {
								// both antecedent and consequent

								if (rule2.getBindingSetHandler() instanceof ReactBindingSetHandler) {

									info = ((ReactBindingSetHandler) rule2.getBindingSetHandler())
											.getKnowledgeInteractionInfo();
									if (!info.isMeta())
										toFromExchanges.put(new Pair(proactiveKB, info.getKnowledgeBaseId().toString()),
												neighbor);
								} else {
									toExchanges.put(new Pair(proactiveKB, proactiveKB), neighbor);
								}

							}
						}

					}
					queue.addAll(((ConsSide) node).getConsequentNeighbours().keySet());
				}
				visited.add(node);
			}
		}

		String title = kiType + " data exchange";

		System.out.println("title " + title);

		for (String actor : actors) {
			System.out.println("participant " + new URI(actor).getPath().substring(1));
		}

		System.out.println("activate " + new URI(proactiveKB).getPath().substring(1));

		System.out.println("aboxright left of " + new URI(proactiveKB).getPath().substring(1) + ":" +

				convertGP(prefixes, (rn.getRule().getAntecedent()).isEmpty() ? rn.getRule().getConsequent()
						: rn.getRule().getAntecedent()));

		for (Pair pair : toFromExchanges.keySet()) {
			RuleNode node = toFromExchanges.get(pair);

			assert node != null;

			BaseRule rule = node.getRule();
			boolean empty = rule.getAntecedent().isEmpty();

			System.out.println(
					new URI(pair.first).getPath().substring(1) + "->" + new URI(pair.second).getPath().substring(1)
							+ ":" + convertGP(prefixes, rule.getAntecedent()) + "\\n"
							+ (empty ? ""
									: convertBindingSet(prefixes,
											node.getResultBindingSetInput().getFullBindingSet().toBindingSet())));

			if (!pair.second.equals(proactiveKB))
				System.out.println("deactivate " + new URI(proactiveKB).getPath().substring(1));

			System.out.println("activate " + new URI(pair.second).getPath().substring(1));
			System.out.println(new URI(pair.second).getPath().substring(1) + "-->"
					+ new URI(pair.first).getPath().substring(1) + ":" + convertGP(prefixes, rule.getConsequent())
					+ "\\n" + convertBindingSet(prefixes, node.getResultBindingSetOutput().toBindingSet()));

			System.out.println("deactivate " + new URI(pair.second).getPath().substring(1));
			if (!pair.second.equals(proactiveKB))
				System.out.println("activate " + new URI(proactiveKB).getPath().substring(1));

		}

		for (Pair pair : toExchanges.keySet()) {

			RuleNode node = toExchanges.get(pair);

			BaseRule rule = node.getRule();

			System.out.println(new URI(pair.first).getPath().substring(1) + "->"
					+ new URI(pair.second).getPath().substring(1) + ":" + convertGP(prefixes, rule.getAntecedent())
					+ " => " + convertGP(prefixes, rule.getConsequent()) + "\\n"
					+ convertBindingSet(prefixes, node.getResultBindingSetInput().getFullBindingSet().toBindingSet()));

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

	public static String[] convertGP(String multiLineString) {
		var set = new HashSet<String>();
		String[] lines = multiLineString.split("\n");

		for (String line : lines) {

			if (!line.isEmpty()) {
				set.add(line);
			}
		}

		return set.toArray(new String[0]);
	}

	/**
	 * parses every line of the string into a triple pattern and combines them into
	 * a set. Note that if it ends with a period (.) it will be removed.
	 * 
	 * @param gp
	 * @return
	 */
	public static Set<TriplePattern> toGP(PrefixMapping prefixes, String gp) {

		var set = new HashSet<TriplePattern>();
		String[] lines = gp.split("\n");

		for (String line : lines) {

			String trimmed = line.strip();
			if (trimmed.endsWith("."))
				trimmed = trimmed.substring(0, trimmed.length() - 2);

			if (!trimmed.isEmpty()) {
				set.add(new TriplePattern(prefixes, trimmed));
			}
		}

		return set;
	}

	public static Set<TriplePattern> toGP(String gp) {
		return toGP(new PrefixMappingZero(), gp);
	}

	private static String convertBindingSet(PrefixMapping prefixes,
			eu.knowledge.engine.reasoner.api.BindingSet toFromHandler) {

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
}
