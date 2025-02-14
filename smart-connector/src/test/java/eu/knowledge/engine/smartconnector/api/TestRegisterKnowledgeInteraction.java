package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

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

		sc1.register(
				new AskKnowledgeInteraction(new CommunicativeAct(), new GraphPattern("?a <foo> ?c"), kiName, false));

		assertThrows(IllegalArgumentException.class, () -> {
			sc1.register(new AskKnowledgeInteraction(new CommunicativeAct(), new GraphPattern("?a <bar> ?c"), kiName,
					false));
		});

		sc1.stop();
	}

}
