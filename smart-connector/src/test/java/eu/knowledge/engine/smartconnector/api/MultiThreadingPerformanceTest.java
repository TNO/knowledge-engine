package eu.knowledge.engine.smartconnector.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;

/**
 * This test tries to balance the number of threads posting to a single KB to
 * keep the message duration below a certain threshold. Threads are
 * added/removed if we are below or above the threshold.
 * 
 * @author nouwtb
 *
 */
public class MultiThreadingPerformanceTest {

	private static final Logger LOG = LoggerFactory.getLogger(MultiThreadingPerformanceTest.class);

	private static class MessageTime {
		public boolean isStart;
		public long time;
		public String id;

		public MessageTime(boolean anIsStart, long aTime, String anId) {
			isStart = anIsStart;
			this.time = aTime;
			this.id = anId;
		}
	}

	private static final int INITIAL_NR_OF_THREADS = 25;

	protected static final long WAIT_TIME = 0;
	private static final int NR_OF_POSTS = 1000;

	protected static final double DURATION_THRESHOLD_MS = 10;

	protected static final int DURATION_DIFF_COUNT = 50;

	private CountDownLatch latch = new CountDownLatch(INITIAL_NR_OF_THREADS);

	private Runnable postRunnable = new Runnable() {

		@Override
		public void run() {

//			// ready
//			latch.countDown();
//			// set
//			try {
//				latch.await();
//				// go!
//			} catch (InterruptedException e1) {
//				e1.printStackTrace();
//			}

			for (int i = 0; i < NR_OF_POSTS && !Thread.currentThread().isInterrupted(); i++) {
				BindingSet argument = new BindingSet();
				Binding e = new Binding();
				String id = "<" + UUID.randomUUID().toString() + ">";
				e.put("s", id);
				argument.add(e);
				// we ignore the result
				poster.post(postKI, argument);
				timings.add(new MessageTime(true, System.nanoTime(), id));
			}
		}
	};

	private Queue<MessageTime> timings = new ConcurrentLinkedQueue<MessageTime>();

	private KnowledgeNetwork kn = null;
	private KnowledgeBaseImpl poster = null;
	private KnowledgeBaseImpl reacter = null;
	private PostKnowledgeInteraction postKI = null;
	private ReactKnowledgeInteraction reactKI = null;

	private ArrayList<Thread> threads = new ArrayList<Thread>();

	private Thread consumeThread = new Thread(new Runnable() {

		@Override
		public void run() {
			int durationsBelowThreshold = 0;
			int durationsAboveThreshold = 0;
			while (true) {
				MessageTime mt = timings.poll();
				if (mt != null) {
					if (alreadyFound.containsKey(mt.id)) {
						MessageTime mt2 = alreadyFound.get(mt.id);

						double duration;
						if (mt.isStart) {
							assert !mt2.isStart;
							duration = ((double) mt2.time - (double) mt.time) / 1_000_000;
						} else {
							assert mt2.isStart;
							duration = ((double) mt.time - (double) mt2.time) / 1_000_000;
						}

						if (duration <= DURATION_THRESHOLD_MS)
							durationsBelowThreshold++;
						else
							durationsAboveThreshold++;

						LOG.info("{} took {}ms ({})", mt.id, duration, timings.size());

						if (durationsBelowThreshold > DURATION_DIFF_COUNT) {
							Thread e = new Thread(MultiThreadingPerformanceTest.this.postRunnable);
							threads.add(e);
							e.start();
							durationsBelowThreshold = 0;
							durationsAboveThreshold = 0;
							LOG.info("Thread added ({})", threads.size());
						} else if (durationsAboveThreshold > DURATION_DIFF_COUNT) {
							if (threads.size() > 1) {
								threads.remove(threads.size() - 1).interrupt();
								LOG.info("Thread removed ({})", threads.size());
							}
							durationsBelowThreshold = 0;
							durationsAboveThreshold = 0;
						}

					} else {
						alreadyFound.put(mt.id, mt);
					}
				}

			}

		}

	});

	private Map<String, MessageTime> alreadyFound = new HashMap<>();

	@Disabled
	@Test
	public void test() throws InterruptedException {

		kn = new KnowledgeNetwork();
		poster = new KnowledgeBaseImpl("poster");
		poster.setIsThreadSafe(true);
		kn.addKB(poster);
		reacter = new KnowledgeBaseImpl("reacter");
		reacter.setIsThreadSafe(true);
		kn.addKB(reacter);

		// add KIs
		GraphPattern gp1 = new GraphPattern("?s <pred> <obj> .");
		this.postKI = new PostKnowledgeInteraction(new CommunicativeAct(), gp1, null);
		this.poster.register(this.postKI);

		GraphPattern gp2 = new GraphPattern("?s <pred> <obj> .");
		this.reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), gp2, null);
		this.reacter.register(this.reactKI, (ReactHandler) (aReactKI, anExchangeInfo) -> {
			timings.add(new MessageTime(false, System.nanoTime(),
					anExchangeInfo.getArgumentBindings().iterator().next().get("s")));
			return new BindingSet();
		});

		kn.sync();

		for (int i = 0; i < INITIAL_NR_OF_THREADS; i++) {
			Thread e = new Thread(this.postRunnable);
			threads.add(e);
			e.start();
		}

		consumeThread.start();
		consumeThread.join();

	}

}
