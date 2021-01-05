package interconnect.ke.api;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockedKnowledgeBase implements KnowledgeBase {

	private static final Logger LOG = LoggerFactory.getLogger(MockedKnowledgeBase.class);

	private SmartConnector sc;

	private String name;

	public MockedKnowledgeBase(String aName) {
		this.name = aName;
		this.sc = new SmartConnector(this);
	}

	public URI getKnowledgeBaseId() {
		URI uri = null;
		try {
			uri = new URI("https://www.tno.nl/" + this.name);
		} catch (URISyntaxException e) {
			LOG.error("Could not parse the uri.", e);
		}
		return uri;
	}

	public String getKnowledgeBaseDescription() {
		return "description of " + this.name;
	}

	public void smartConnectorReady(SmartConnector aSC) {
		LOG.info(this.name + " ready");
	}

	public void smartConnectorConnectionLost(SmartConnector aSC) {
		LOG.info(this.name + " connection lost");

	}

	public void smartConnectorConnectionRestored(SmartConnector aSC) {
		LOG.info(this.name + " connection restored");
	}

	@Override
	public void smartConnectorStopped(SmartConnector aSC) {
		LOG.info(this.name + " smartconnnector stopped");

	}

	@Override
	public String getKnowledgeBaseName() {
		return this.name;
	}

	public SmartConnector getSmartConnector() {
		return sc;
	}

}
