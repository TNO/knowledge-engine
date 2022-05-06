package eu.knowledge.engine.smartconnector.api;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnowledgeNetwork {

	private static final Logger LOG = LoggerFactory.getLogger(KnowledgeNetwork.class);

	/**
	 * our synchronization mechanism, which allows to synchronize for multiple
	 * phases (with dynamic number of parties). Phase 1 = smart connectors get
	 * ready, Phase 2 = everybody is aware of everybody.
	 */
	private Phaser readyPhaser;
	private Set<MockedKnowledgeBase> knowledgeBases;
	private Map<MockedKnowledgeBase, AskKnowledgeInteraction> knowledgeInteractionMetadata;
	private PrefixMapping prefixMapping;

	public KnowledgeNetwork() {
		this.knowledgeBases = ConcurrentHashMap.newKeySet();
		this.knowledgeInteractionMetadata = new ConcurrentHashMap<>();
		this.readyPhaser = new Phaser(1);
		this.prefixMapping = new PrefixMappingMem();
		this.prefixMapping.setNsPrefixes(PrefixMapping.Standard);
		this.prefixMapping.setNsPrefix("kb", Vocab.ONTO_URI);
	}

	public void addKB(MockedKnowledgeBase aKB) {
		aKB.setPhaser(this.readyPhaser);
		knowledgeBases.add(aKB);
	}

	/**
	 * wait until all knowledge bases are up and running and know of each other
	 * existence.
	 */
	public void startAndWaitForReady() {

		for (MockedKnowledgeBase kb : this.knowledgeBases) {
			kb.start();
		}

		// wait until all smart connectors have given the 'ready' signal (and registered
		// all their knowledge interactions).
		LOG.debug("Waiting for ready.");
		readyPhaser.arriveAndAwaitAdvance();
		LOG.debug("Everyone is ready!");

		// register our state check Knowledge Interaction on each Smart Connecotr
		GraphPattern gp = new GraphPattern(this.prefixMapping,
		//@formatter:off
				"?kb rdf:type kb:KnowledgeBase .",
				"?kb kb:hasName ?name .", 
				"?kb kb:hasDescription ?description .",
				"?kb kb:hasKnowledgeInteraction ?ki .", 
				"?ki rdf:type ?kiType .", 
				"?ki kb:isMeta ?isMeta .",
				"?ki kb:hasCommunicativeAct ?act .", 
				"?act rdf:type kb:CommunicativeAct .",
				"?act kb:hasRequirement ?req .", 
				"?act kb:hasSatisfaction ?sat .", 
				"?req rdf:type ?reqType .",
				"?sat rdf:type ?satType .", 
				"?ki kb:hasGraphPattern ?gp .", 
				"?ki ?patternType ?gp .",
				"?gp rdf:type kb:GraphPattern .", 
				"?gp kb:hasPattern ?pattern ."
				//@formatter:on
		);
		for (MockedKnowledgeBase kb : this.knowledgeBases) {
			AskKnowledgeInteraction anAskKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp, true);
			this.knowledgeInteractionMetadata.put(kb, anAskKI);
			kb.register(anAskKI);
		}
	}

	public void waitForUpToDate() {
		LOG.debug("Waiting for up to date.");
		// manually check if every Knowledge Base is up to date
		boolean allUpToDate = false;

		int count = 0;

		while (!allUpToDate) {
			count++;
			LOG.info("Checking up to date knowledge bases.");
			allUpToDate = true;
			boolean kbUpToDate;
			for (MockedKnowledgeBase kb : this.knowledgeBases) {
				LOG.info("Before isUpToDate");
				kbUpToDate = kb.isUpToDate(this.knowledgeInteractionMetadata.get(kb), this.knowledgeBases);
				LOG.info("After isUpToDate");
				allUpToDate &= kbUpToDate;
				if (kbUpToDate) {
					LOG.info("Knowledge Base {} is up to date.", kb.getKnowledgeBaseName());
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				LOG.error("An error occured while waiting for up-to-date.", e);
			}
		}
		LOG.info("Everyone is up to date after {} rounds!", count);
	}

	/**
	 * Stops all knowledge bases in this network, and returns a future that
	 * completes when all SCs have stopped.
	 */
	public CompletableFuture<Void> stop() {
		var kbStoppedFutures = new ArrayList<CompletableFuture<Void>>();
		this.knowledgeBases.forEach(kb -> {
			kb.stop();
			kbStoppedFutures.add(kb.getStopFuture());
		});
		this.knowledgeBases.clear();
		return CompletableFuture.allOf(kbStoppedFutures.toArray(new CompletableFuture<?>[kbStoppedFutures.size()]));
	}
}
