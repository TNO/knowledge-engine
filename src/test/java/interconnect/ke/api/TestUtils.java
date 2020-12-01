package interconnect.ke.api;

import java.net.URI;
import java.net.URISyntaxException;

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

}
