package interconnect.ke.sc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.AskResult;
import interconnect.ke.api.CommunicativeAct;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.binding.Binding;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;

public class InteractionProcessorImplTest {

	private static final Logger LOG = LoggerFactory.getLogger(InteractionProcessorImplTest.class);

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

		this.interactionProcessor = new InteractionProcessorImpl(LoggerFactory::getLogger,
				new TestOtherKnowledgeBaseStore(), null, null);
		this.messageRouter = new TestMessageRouter();
		this.interactionProcessor.setMessageRouter(this.messageRouter);

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
	void test() throws InterruptedException, ExecutionException, URISyntaxException {

		AskKnowledgeInteraction askInteraction = new AskKnowledgeInteraction(new CommunicativeAct(),
				new GraphPattern(this.graphPattern1));

		CompletableFuture<AskResult> future = this.interactionProcessor
				.processAskFromKnowledgeBase(new MyKnowledgeInteractionInfo(new URI("http://www.tno.nl/"),
						new URI("http://www.tno.nl/"), askInteraction, null, null, false), null, new BindingSet());

		BindingSet bindings = future.get().getBindings();

		Iterator<Binding> iterator = bindings.iterator();

		Binding b;
		for (int i = 1; i < 3; i++) // we expect two bindings, but we do not know the ordering.
		{
			assertTrue(iterator.hasNext(), "there should be a binding number " + i);
			b = iterator.next();
			assertNotNull(b);
			if (b.containsKey("s2")) {
				assertTrue(b.get("s2").equals(this.subject2));
				assertTrue(b.containsKey("o2"));
				assertTrue(b.get("o2").equals(this.object2));
			} else if (b.containsKey("s3")) {
				assertTrue(b.get("s3").equals(this.subject3));
				assertTrue(b.containsKey("o3"));
				assertTrue(b.get("o3").equals(this.object3));
			} else {
				fail("Every binding should contain either s2 or s3.");
			}
		}
	}

	public class TestOtherKnowledgeBaseStore implements OtherKnowledgeBaseStore {

		@Override
		public Set<OtherKnowledgeBase> getOtherKnowledgeBases() {

			Set<OtherKnowledgeBase> others = new HashSet<>();
			try {

				// create/add second
				List<KnowledgeInteractionInfo> someKIs2 = new ArrayList<>();
				AnswerKnowledgeInteraction answerKnowledgeInteraction = new AnswerKnowledgeInteraction(new CommunicativeAct(),
						new GraphPattern(InteractionProcessorImplTest.this.graphPattern2));

				KnowledgeInteractionInfo knowledgeInteractionInfo;
				knowledgeInteractionInfo = new KnowledgeInteractionInfo(new URI("https://www.tno.nl/2"),
						InteractionProcessorImplTest.this.knowledgeBaseId2, answerKnowledgeInteraction, false);

				someKIs2.add(knowledgeInteractionInfo);
				OtherKnowledgeBase other2 = new OtherKnowledgeBase(InteractionProcessorImplTest.this.knowledgeBaseId2,
						"", "", someKIs2, null);
				others.add(other2);

				// create/add third
				List<KnowledgeInteractionInfo> someKIs3 = new ArrayList<>();
				answerKnowledgeInteraction = new AnswerKnowledgeInteraction(new CommunicativeAct(),
						new GraphPattern(InteractionProcessorImplTest.this.graphPattern3));

				knowledgeInteractionInfo = new KnowledgeInteractionInfo(new URI("https://www.tno.nl/3"),
						InteractionProcessorImplTest.this.knowledgeBaseId3, answerKnowledgeInteraction, false);

				someKIs3.add(knowledgeInteractionInfo);
				OtherKnowledgeBase other3 = new OtherKnowledgeBase(InteractionProcessorImplTest.this.knowledgeBaseId3,
						"", "", someKIs3, null);
				others.add(other3);
			} catch (URISyntaxException e) {
				LOG.error("", e);
			}
			return others;

		}

		@Override
		public CompletableFuture<Void> start() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void stop() {
			// TODO Auto-generated method stub

		}
	}

	class TestMessageRouter implements MessageRouter {

		private final ExecutorService executor = Executors.newFixedThreadPool(1);

		@Override
		public CompletableFuture<AnswerMessage> sendAskMessage(AskMessage askMessage) throws IOException {

			CompletableFuture<AnswerMessage> future = new CompletableFuture<>();

			this.executor.execute(() -> {

				BindingSet bindings = new BindingSet();

				Binding b = new Binding();
				b.put("s2", InteractionProcessorImplTest.this.subject2);
				b.put("o2", InteractionProcessorImplTest.this.object2);
				bindings.add(b);

				AnswerMessage msg;
				try {
					msg = new AnswerMessage(InteractionProcessorImplTest.this.knowledgeBaseId2,
							new URI("https://www.tno.nl/ki2"), InteractionProcessorImplTest.this.knowledgeBaseId1,
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
		public void registerInteractionProcessor(InteractionProcessor interactionProcessor) {
		}

		@Override
		public void registerMetaKnowledgeBase(MetaKnowledgeBase metaKnowledgeBase) {

		}

	}

}
