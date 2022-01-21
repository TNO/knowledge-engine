package eu.knowledge.engine.smartconnector.runtime.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.Test;

import eu.knowledge.engine.knowledgedirectory.KnowledgeDirectory;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.messaging.AnswerMessage;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;
import eu.knowledge.engine.smartconnector.messaging.KnowledgeMessage;

public class DistributedMessageDispatcherTest {

	@Test
	void testLocalMessageExchange() throws Exception {
		assertTrue(NetUtils.portAvailable(8080));
		KnowledgeDirectory kd = new KnowledgeDirectory(8080);
		MessageDispatcher md = new MessageDispatcher(8081, new URI("http://localhost:8081"), "localhost", 8080);

		kd.start();

		Thread.sleep(1000);

		md.start();

		URI kb1Id = new URI("http://test.com/kb1");
		URI kb2Id = new URI("http://test.com/kb2");
		MockSmartConnector sc1 = new MockSmartConnector(kb1Id);
		MockSmartConnector sc2 = new MockSmartConnector(kb2Id);

		md.getLocalSmartConnectorConnectionManager().smartConnectorAdded(sc1);
		md.getLocalSmartConnectorConnectionManager().smartConnectorAdded(sc2);

		Thread.sleep(5000);

		BindingSet bindingSet = new BindingSet();
		Binding binding = new Binding();
		binding.put("a", "b");
		binding.put("c", "d");
		bindingSet.add(binding);

		// -- Ask message -->
		AskMessage message1 = new AskMessage(kb1Id, kb1Id, kb2Id, kb2Id, bindingSet);
		sc1.send(message1);

		Thread.sleep(1000);

		KnowledgeMessage message1Received = sc2.getLastMessage();
		assertEquals(message1.toString(), message1Received.toString());

		// <-- Answer message --
		AnswerMessage message2 = new AnswerMessage(kb2Id, kb2Id, kb1Id, kb1Id, message1.getMessageId(), bindingSet);
		sc2.send(message2);

		Thread.sleep(1000);

		KnowledgeMessage message2Received = sc1.getLastMessage();
		assertEquals(message2.toString(), message2Received.toString());

		md.stop();
		kd.stop();
		assertTrue(NetUtils.portAvailable(8080));
	}

	@Test
	void testRemoteMessageExchange() throws Exception {
		assertTrue(NetUtils.portAvailable(8080));
		KnowledgeDirectory kd = new KnowledgeDirectory(8080);
		MessageDispatcher md1 = new MessageDispatcher(8081, new URI("http://localhost:8081"), "localhost", 8080);
		MessageDispatcher md2 = new MessageDispatcher(8082, new URI("http://localhost:8082"), "localhost", 8080);

		kd.start();

		Thread.sleep(1000);

		md1.start();
		md2.start();

		URI kb1Id = new URI("http://test.com/kb1");
		URI kb2Id = new URI("http://test.com/kb2");
		MockSmartConnector sc1 = new MockSmartConnector(kb1Id);
		MockSmartConnector sc2 = new MockSmartConnector(kb2Id);

		md1.getLocalSmartConnectorConnectionManager().smartConnectorAdded(sc1);
		md2.getLocalSmartConnectorConnectionManager().smartConnectorAdded(sc2);

		Thread.sleep(5000);

		BindingSet bindingSet = new BindingSet();
		Binding binding = new Binding();
		binding.put("a", "b");
		binding.put("c", "d");
		bindingSet.add(binding);

		// -- Ask message -->
		AskMessage message1 = new AskMessage(kb1Id, kb1Id, kb2Id, kb2Id, bindingSet);
		sc1.send(message1);

		Thread.sleep(1000);

		KnowledgeMessage message1Received = sc2.getLastMessage();
		assertEquals(message1.toString(), message1Received.toString());

		// <-- Answer message --
		AnswerMessage message2 = new AnswerMessage(kb2Id, kb2Id, kb1Id, kb1Id, message1.getMessageId(), bindingSet);
		sc2.send(message2);

		Thread.sleep(1000);

		KnowledgeMessage message2Received = sc1.getLastMessage();
		assertEquals(message2.toString(), message2Received.toString());

		md1.stop();
		md2.stop();

		kd.stop();
		assertTrue(NetUtils.portAvailable(8080));
	}
}
