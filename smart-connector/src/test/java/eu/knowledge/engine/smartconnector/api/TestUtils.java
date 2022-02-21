package eu.knowledge.engine.smartconnector.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BindingSetHandler;
import eu.knowledge.engine.reasoner.ReasoningNode;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeBase;
import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.impl.SmartConnectorBuilder;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.AnswerBindingSetHandler;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.ReactBindingSetHandler;

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

	public static void printSequenceDiagram(String proactiveKB, String kiType, GraphPattern gp, ReasoningNode rn) {

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
		}

		Map<Pair, ReasoningNode> toFromExchanges = new HashMap<>();
		Map<Pair, ReasoningNode> toExchanges = new HashMap<>();

		while (!queue.isEmpty()) {

			ReasoningNode node = queue.poll();

			String currentActor = null;
			BindingSetHandler bsh = node.getRule().getBindingSetHandler();
			ReactBindingSetHandler rbsh = null;
			AnswerBindingSetHandler absh = null;
			if (bsh instanceof ReactBindingSetHandler) {
				rbsh = (ReactBindingSetHandler) bsh;

				currentActor = rbsh.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString();
				actors.add(currentActor);
			} else if (bsh instanceof AnswerBindingSetHandler) {
				absh = (AnswerBindingSetHandler) bsh;
				currentActor = absh.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString();
				actors.add(currentActor);
			} else {
				currentActor = proactiveKB;
			}

			for (ReasoningNode neighbor : node.getAntecedentNeighbors().keySet()) {
				BindingSetHandler bsh2 = neighbor.getRule().getBindingSetHandler();

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
					toExchanges.put(new Pair(proactiveKB, proactiveKB), neighbor);
				}
			}

			for (ReasoningNode neighbor : node.getConsequentNeighbors().keySet()) {
				BindingSetHandler bsh2 = neighbor.getRule().getBindingSetHandler();

				AnswerBindingSetHandler absh2 = null;
				ReactBindingSetHandler rbsh2 = null;
				if (bsh2 instanceof ReactBindingSetHandler) {
					rbsh2 = (ReactBindingSetHandler) bsh2;

					ReactKnowledgeInteraction react = (ReactKnowledgeInteraction) rbsh2.getKnowledgeInteractionInfo()
							.getKnowledgeInteraction();

					if (react.isMeta()) {
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
					toExchanges.put(new Pair(currentActor, currentActor), neighbor);
				}
			}

			queue.addAll(node.getAntecedentNeighbors().keySet());
			queue.addAll(node.getConsequentNeighbors().keySet());
		}

		String title = kiType + " data exchange";

		System.out.println("title " + title);

		for (String actor : actors) {
			System.out.println("actor " + removeChars(actor));
		}

		System.out.println("activate " + removeChars(proactiveKB));

		for (Pair pair : toExchanges.keySet()) {

			ReasoningNode node = toExchanges.get(pair);

			System.out.println(removeChars(pair.first) + "->" + removeChars(pair.second) + ":"
					+ checkSize(node.getBindingSetToHandler().toString()));

		}

		for (Pair pair : toFromExchanges.keySet()) {
			ReasoningNode node = toFromExchanges.get(pair);

			assert node != null;

			String toHandler = node.getBindingSetToHandler().toString();
			String fromHandler = node.getBindingSetFromHandler().toString();
			System.out.println(removeChars(pair.first) + "->" + removeChars(pair.second) + ":" + checkSize(toHandler));

			if (!pair.second.equals(proactiveKB))
				System.out.println("deactivate " + removeChars(proactiveKB));

			System.out.println("activate " + removeChars(pair.second));
			System.out
					.println(removeChars(pair.second) + "-->" + removeChars(pair.first) + ":" + checkSize(fromHandler));

			System.out.println("deactivate " + removeChars(pair.second));
			if (!pair.second.equals(proactiveKB))
				System.out.println("activate " + removeChars(proactiveKB));

		}

	}

	private static String checkSize(String toHandler) {
		int endIndex = 100;
		if (toHandler.length() > endIndex)
			return "[{...}]";
		else
			return toHandler;
	}

	public static String removeChars(String path) {
		return path.replace(":", "");
	}
}
