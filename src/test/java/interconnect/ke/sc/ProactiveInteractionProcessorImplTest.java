package interconnect.ke.sc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import interconnect.ke.api.AskResult;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.binding.Binding;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.KnowledgeMessage;
import interconnect.ke.messaging.MessageDispatcherEndpoint;

public class ProactiveInteractionProcessorImplTest {

	private ProactiveInteractionProcessor interactionProcessor = null;
	private MessageDispatcherEndpoint dispatcher = null;

	private URI knowledgeBaseId1 = null;
	private String graphPattern1 = null;

	private URI knowledgeBaseId2 = null;
	private String graphPattern2 = null;
	private String subject2 = null;
	private String object2 = null;

	@BeforeEach
	void setUp() throws Exception {

		interactionProcessor = new ProactiveInteractionProcessorImpl(new TestOtherKnowledgeBaseStore());
		this.dispatcher = new TestMessageDispatcherEndpoint(this.interactionProcessor);
		interactionProcessor.setMessageDispatcherEndpoint(this.dispatcher);

		this.knowledgeBaseId1 = new URI("https://www.tno.nl/interconnect/kb1");
		this.graphPattern1 = "?s1 <https://www.tno.nl/example/predicate1> ?o1 .";
		this.knowledgeBaseId2 = new URI("https://www.tno.nl/interconnect/kb2");
		this.graphPattern2 = "?s2 <https://www.tno.nl/example/predicate1> ?o2 .";
		this.subject2 = "<http://www.tno.nl/subject2>";
		this.object2 = "<https://www.tno.nl/object2>";
	}

	@Test
	void test() throws InterruptedException, ExecutionException {

		AskKnowledgeInteraction askInteraction = new AskKnowledgeInteraction(null,
				new GraphPattern(this.graphPattern1));
		CompletableFuture<AskResult> future = interactionProcessor.processAsk(askInteraction, null, new BindingSet());

		BindingSet bindings = future.get().getBindings();

		Binding b = bindings.iterator().next();
		assertNotNull(b);
		assertTrue(b.containsKey("s2"));
		assertTrue(b.get("s2").equals(subject2));
		assertTrue(b.containsKey("o2"));
		assertTrue(b.get("o2").equals(object2));
	}

	public class TestOtherKnowledgeBaseStore implements OtherKnowledgeBaseStore {

		@Override
		public List<OtherKnowledgeBase> getOtherKnowledgeBases() {

			List<OtherKnowledgeBase> others = new ArrayList<>();
			List<KnowledgeInteraction> someKIs = new ArrayList<>();
			someKIs.add(new AnswerKnowledgeInteraction(null,
					new GraphPattern(ProactiveInteractionProcessorImplTest.this.graphPattern2)));

			OtherKnowledgeBase other = new OtherKnowledgeBase(
					ProactiveInteractionProcessorImplTest.this.knowledgeBaseId2, "", "", someKIs, null);
			others.add(other);

			return others;
		}
	}

	class TestMessageDispatcherEndpoint implements MessageDispatcherEndpoint {

		private ExecutorService executor = Executors.newFixedThreadPool(1);
		private ProactiveInteractionProcessor processor = null;

		public TestMessageDispatcherEndpoint(ProactiveInteractionProcessor processor) {
			this.processor = processor;
		}

		@Override
		public void send(KnowledgeMessage message) throws IOException {
			executor.execute(() -> {

				BindingSet bindings = new BindingSet();

				Binding b = new Binding();
				b.put("s2", ProactiveInteractionProcessorImplTest.this.subject2);
				b.put("o2", ProactiveInteractionProcessorImplTest.this.object2);
				bindings.add(b);

				AnswerMessage msg;
				try {
					msg = new AnswerMessage(knowledgeBaseId2, new URI("https://www.tno.nl/ki2"), knowledgeBaseId1,
							new URI("https://www.tno.nl/ki1"), message.getMessageId(), bindings);
					this.processor.handleAnswerMessage(msg);
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		}

	}

}
