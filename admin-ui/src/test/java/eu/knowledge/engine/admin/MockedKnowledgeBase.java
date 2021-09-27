package eu.knowledge.engine.admin;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.KnowledgeBase;
import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.impl.SmartConnectorBuilder;

public class MockedKnowledgeBase implements KnowledgeBase {

	private static final Logger LOG = LoggerFactory.getLogger(MockedKnowledgeBase.class);

	private final SmartConnector sc;

	protected String name;

	public MockedKnowledgeBase(String aName) {
		this.name = aName;
		this.sc = SmartConnectorBuilder.newSmartConnector(this).create();
	}

	@Override
	public URI getKnowledgeBaseId() {
		URI uri = null;
		try {
			uri = new URI("https://www.tno.nl/" + this.name);
		} catch (URISyntaxException e) {
			LOG.error("Could not parse the uri.", e);
		}
		return uri;
	}

	@Override
	public String getKnowledgeBaseDescription() {
		return "description of " + this.name;
	}

	@Override
	public void smartConnectorReady(SmartConnector aSC) {
		LOG.info(this.name + " ready");
	}

	@Override
	public void smartConnectorConnectionLost(SmartConnector aSC) {
		LOG.info(this.name + " connection lost");

	}

	@Override
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
		return this.sc;
	}

	public void stop() {
		this.sc.stop();
	}

}
