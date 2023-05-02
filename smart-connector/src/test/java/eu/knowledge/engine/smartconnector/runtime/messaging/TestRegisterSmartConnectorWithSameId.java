package eu.knowledge.engine.smartconnector.runtime.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.knowledgedirectory.KnowledgeDirectory;
import eu.knowledge.engine.smartconnector.util.MockedKnowledgeBase;

public class TestRegisterSmartConnectorWithSameId {
  private static final Logger LOG = LoggerFactory.getLogger(TestRegisterSmartConnectorWithSameId.class);

  @Test
  public void testRegisterSmartConnectorWithSameIdInSameRuntimeThrows() {
    var kb1 = new MockedKnowledgeBase("http://example.org/kb1");
    kb1.start();

    var kb1AsWell = new MockedKnowledgeBase("http://example.org/kb1");

    assertThrows(IllegalArgumentException.class, () -> {
      kb1AsWell.start();
    });
    
    kb1.stop();
  }

  @Test
	void testRegisterSmartConnectorWithSameIdInDifferentRuntimeThrows() throws Exception {
		assertTrue(NetUtils.portAvailable(8080));
		KnowledgeDirectory kd = new KnowledgeDirectory(8080);
		MessageDispatcher md1 = new MessageDispatcher(8081, new URI("http://localhost:8081"),
				new URI("http://localhost:8080"));
		MessageDispatcher md2 = new MessageDispatcher(8082, new URI("http://localhost:8082"),
				new URI("http://localhost:8080"));

		kd.start();

		Thread.sleep(1000);

		// this ID is REUSED in both smart connectors!
		URI kb1Id = new URI("http://example.org/kb1");
		
		md1.start();
		MockSmartConnector sc1 = new MockSmartConnector(kb1Id);
		md1.getLocalSmartConnectorConnectionManager().smartConnectorAdded(sc1);
		
		md2.start();
		Thread.sleep(5000);
		MockSmartConnector sc2 = new MockSmartConnector(kb1Id);
		assertThrows(IllegalArgumentException.class, () -> {
			md2.getLocalSmartConnectorConnectionManager().smartConnectorAdded(sc2);
		});

		Thread.sleep(5000);

		md1.stop();
		md2.stop();

		kd.stop();
		assertTrue(NetUtils.portAvailable(8080));
	}
}
