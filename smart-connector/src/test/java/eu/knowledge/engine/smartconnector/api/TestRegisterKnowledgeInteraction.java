package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import eu.knowledge.engine.smartconnector.impl.SmartConnectorBuilder;

public class TestRegisterKnowledgeInteraction {
	@Test
	public void testRegisterKnowledgeInteractionWithSameName() {
		var sc1 = SmartConnectorBuilder.newSmartConnector(new KnowledgeBase() {

			@Override
			public URI getKnowledgeBaseId() {
				return URI.create("http://www.tno.nl/kb1");
			}

			@Override
			public String getKnowledgeBaseName() {
				return "";
			}

			@Override
			public String getKnowledgeBaseDescription() {
				return "";
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
			public void smartConnectorStopped(SmartConnector aSC) {
			}
		}).create();

		String kiName = "some-name";

		sc1.register(new AskKnowledgeInteraction(new CommunicativeAct(), new GraphPattern("?a <foo> ?c"), kiName));

		assertThrows(IllegalArgumentException.class, () -> {
			sc1.register(new AskKnowledgeInteraction(new CommunicativeAct(), new GraphPattern("?a <bar> ?c"), kiName));
		});

		sc1.stop();
	}

}
