package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

public class TestRegisterKnowledgeInteraction {
	@Test
	public void testRegisterKnowledgeInteractionWithSameName() {
		var kn = new KnowledgeNetwork();
		var kb1 = new MockedKnowledgeBase("kb1");
		kn.addKB(kb1);

		kn.startAndWaitForReady();

		String kiName = "some-name";

		kb1.register(new AskKnowledgeInteraction(new CommunicativeAct(), new GraphPattern("?a <foo> ?c"), kiName));

		var kisBefore = kb1.getKnowledgeInteractions().size();
		
		assertThrows(IllegalArgumentException.class, () -> {
			kb1.register(new AskKnowledgeInteraction(new CommunicativeAct(), new GraphPattern("?a <bar> ?c"), kiName));
		});
		
		var kisAfter = kb1.getKnowledgeInteractions().size();

		assertEquals(kisBefore, kisAfter);

		try {
			kn.stop().get();
		} catch (InterruptedException | ExecutionException e) {
			fail("Could not stop knowledge network after test.");
		}
	}
}
