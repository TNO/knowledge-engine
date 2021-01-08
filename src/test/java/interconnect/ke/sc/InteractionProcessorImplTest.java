package interconnect.ke.sc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
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
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.KnowledgeMessage;
import interconnect.ke.messaging.MessageDispatcherEndpoint;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;

public class InteractionProcessorImplTest {

	private InteractionProcessor interactionProcessor = null;
	private MessageRouter messageRouter = null;

	private URI knowledgeBaseId1 = null;
	private String graphPattern1 = null;

	private URI knowledgeBaseId2 = null;
	private String graphPattern2 = null;
	private String subject2 = null;
	private String object2 = null;

	private URI knowledgeBaseId3 = null;
	public String graphPattern3 = null;
	private String subject3 = null;
	private String object3 = null;

	@BeforeEach
	void setUp() throws Exception {

		interactionProcessor = new InteractionProcessorImpl(new TestOtherKnowledgeBaseStore());
		this.messageRouter = new TestMessageRouter();
		interactionProcessor.setMessageRouter(this.messageRouter);

		this.knowledgeBaseId1 = new URI("https://www.tno.nl/interconnect/kb1");
		this.graphPattern1 = "?s1 <https://www.tno.nl/example/predicate1> ?o1 .";

		this.knowledgeBaseId2 = new URI("https://www.tno.nl/interconnect/kb2");
		this.graphPattern2 = "?s2 <https://www.tno.nl/example/predicate1> ?o2 .";
		this.subject2 = "<http://www.tno.nl/subject2>";
		this.object2 = "<https://www.tno.nl/object2>";

		this.knowledgeBaseId3 = new URI("https://www.tno.nl/interconnect/kb3");
		this.graphPattern3 = "?s3 <https://www.tno.nl/example/predicate1> ?o3 .";
		this.subject3 = "<http://www.tno.nl/subject3>";
		this.object3 = "<https://www.tno.nl/object3>";

	}

	@Test
	void test() throws InterruptedException, ExecutionException {

		AskKnowledgeInteraction askInteraction = new AskKnowledgeInteraction(null,
				new GraphPattern(this.graphPattern1));
		CompletableFuture<AskResult> future = interactionProcessor.processAskFromKnowledgeBase(askInteraction, null,
				new BindingSet());

		BindingSet bindings = future.get().getBindings();

		Iterator<Binding> iterator = bindings.iterator();

		Binding b;
		for (int i = 1; i < 3; i++) // we expect two bindings, but we do not know the ordering.
		{
			assertTrue(iterator.hasNext(), "there should be a binding number " + i);
			b = iterator.next();
			assertNotNull(b);
			if (b.containsKey("s2")) {
				assertTrue(b.get("s2").equals(subject2));
				assertTrue(b.containsKey("o2"));
				assertTrue(b.get("o2").equals(object2));
			} else if (b.containsKey("s3")) {
				assertTrue(b.get("s3").equals(subject3));
				assertTrue(b.containsKey("o3"));
				assertTrue(b.get("o3").equals(object3));
			} else {
				fail("Every binding should contain either s2 or s3.");
			}
		}
	}

	public class TestOtherKnowledgeBaseStore implements OtherKnowledgeBaseStore {

		@Override
		public List<OtherKnowledgeBase> getOtherKnowledgeBases() {

			List<OtherKnowledgeBase> others = new ArrayList<>();

			// create/add second
			List<KnowledgeInteraction> someKIs2 = new ArrayList<>();
			someKIs2.add(new AnswerKnowledgeInteraction(null,
					new GraphPattern(InteractionProcessorImplTest.this.graphPattern2)));
			OtherKnowledgeBase other2 = new OtherKnowledgeBase(InteractionProcessorImplTest.this.knowledgeBaseId2, "",
					"", someKIs2, null);
			others.add(other2);

			// create/add third
			List<KnowledgeInteraction> someKIs3 = new ArrayList<>();
			someKIs3.add(new AnswerKnowledgeInteraction(null,
					new GraphPattern(InteractionProcessorImplTest.this.graphPattern3)));
			OtherKnowledgeBase other3 = new OtherKnowledgeBase(InteractionProcessorImplTest.this.knowledgeBaseId3, "",
					"", someKIs3, null);
			others.add(other3);

			return others;
		}
	}

	class TestMessageRouter implements MessageRouter {

		private ExecutorService executor = Executors.newFixedThreadPool(1);

		@Override
		public CompletableFuture<AnswerMessage> sendAskMessage(AskMessage askMessage) throws IOException {

			CompletableFuture<AnswerMessage> future = new CompletableFuture<AnswerMessage>();

			executor.execute(() -> {

				BindingSet bindings = new BindingSet();

				Binding b = new Binding();
				b.put("s2", InteractionProcessorImplTest.this.subject2);
				b.put("o2", InteractionProcessorImplTest.this.object2);
				bindings.add(b);

				AnswerMessage msg;
				try {
					msg = new AnswerMessage(knowledgeBaseId2, new URI("https://www.tno.nl/ki2"), knowledgeBaseId1,
							new URI("https://www.tno.nl/ki1"), askMessage.getMessageId(), bindings);
					future.complete(msg);
				} catch (URISyntaxException e) {
					fail("No exception should occur.", e);
				}

			});
			return future;
		}

		@Override
		public CompletableFuture<ReactMessage> sendPostMessage(PostMessage postMessage) throws IOException {
			return null;
		}

		@Override
		public void registerMetaKnowledgeBase(MyMetaKnowledgeBase metaKnowledgeBase) {
		}

		@Override
		public void registerInteractionProcessor(InteractionProcessor interactionProcessor) {
		}

	}

}
