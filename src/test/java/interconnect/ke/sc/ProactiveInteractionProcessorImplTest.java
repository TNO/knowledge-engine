package interconnect.ke.sc;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.KnowledgeMessage;
import interconnect.ke.messaging.MessageDispatcherEndpoint;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;
import interconnect.ke.messaging.SmartConnectorEndpoint;

class ProactiveInteractionProcessorImplTest {

	private MessageDispatcherEndpoint dispatcher = new TestMessageDispatcherEndpoint();
	private ProactiveInteractionProcessor interactionProcessor = new ProactiveInteractionProcessorImpl(
			new TestOtherKnowledgeBaseStore(), new MessageReplyTracker(this.dispatcher));

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void test() {
		fail("Not yet implemented");
	}

	static class TestOtherKnowledgeBaseStore implements OtherKnowledgeBaseStore {

		@Override
		public List<OtherKnowledgeBase> getOtherKnowledgeBases() {

			List<OtherKnowledgeBase> others = new ArrayList<>();
			String knowledgeBaseId2 = "https://www.tno.nl/interconnect/kb2";
			SmartConnectorEndpoint anEndpoint = new SmartConnectorEndpoint() {
				@Override
				public URI getKnowledgeBaseId() {
					return new URI(knowledgeBaseId2);
				}

				@Override
				public void handleAnswerMessage(AnswerMessage message) {}
				@Override
				public void handleAskMessage(AskMessage message) {}
				@Override
				public void handlePostMessage(PostMessage message) {}
				@Override
				public void handleReactMessage(ReactMessage message) {}
			};
			
			List<KnowledgeInteraction> someKIs = new ArrayList<>();
			someKIs.add(new AnswerKnowledgeInteraction(null, new GraphPattern("?s <https://www.tno.nl/example/predicate1> ?o")));
			
			OtherKnowledgeBase other = new OtherKnowledgeBase(knowledgeBaseId2, "kb2", "this is kb2", someKIs, anEndpoint)
			
			others.add(other);

			return null;
		}

	}

	static class TestMessageDispatcherEndpoint implements MessageDispatcherEndpoint {

		@Override
		public void send(KnowledgeMessage message) throws IOException {

		}

	}
}
