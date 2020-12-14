package interconnect.ke.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestUtils {

	public static final GraphPattern SAREF_MEASUREMENT_PATTERN = new GraphPattern(
			"?m <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/core/Measurement> . ?m <https://saref.etsi.org/core/hasValue> ?v");

	private static final String KE_PREFIX = "https://www.interconnectproject.eu/";

	public static final SmartConnector getSmartConnector(final String aName) {
		return new SmartConnector(new KnowledgeBase() {

			public URI getKnowledgeBaseId() throws KnowledgeEngineException {
				URI uri;
				try {
					uri = new URI(KE_PREFIX + aName);
				} catch (URISyntaxException e) {
					throw new KnowledgeEngineException(e);
				}
				return uri;
			}

			public String getKnowledgeBaseDescription() {
				return null;
			}

			public void smartConnectorReady() {
			}

			public void smartConnectorConnectionLost() {
			}

			public void smartConnectorConnectionRestored() {
			}
		});
	}

	public static final Set<Binding> getSingleBinding(String name, String value) {
		Set<Binding> bindings = new HashSet<Binding>();
		Binding b = new Binding();
		b.put(name, value);
		bindings.add(b);
		return bindings;
	}

	public static final Set<Binding> getSingleBinding(String name1, String value1, String name2, String value2) {
		Set<Binding> bindings = new HashSet<Binding>();
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

}
