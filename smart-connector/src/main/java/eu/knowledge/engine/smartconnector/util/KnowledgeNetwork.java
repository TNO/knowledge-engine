package eu.knowledge.engine.smartconnector.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.MatchStrategy;
import eu.knowledge.engine.smartconnector.api.Vocab;
import eu.knowledge.engine.smartconnector.impl.SmartConnectorImpl;

public class KnowledgeNetwork {

	private static final Logger LOG = LoggerFactory.getLogger(KnowledgeNetwork.class);

	/**
	 * our synchronization mechanism, which allows to synchronize for multiple
	 * phases (with dynamic number of parties). Phase 1 = smart connectors get
	 * ready, Phase 2 = everybody is aware of everybody.
	 */
	private Phaser readyPhaser;
	private Set<KnowledgeBaseImpl> knowledgeBases;
	private Map<KnowledgeBaseImpl, AskKnowledgeInteraction> knowledgeInteractionMetadata;
	private PrefixMapping prefixMapping;

	public KnowledgeNetwork() {
		this.knowledgeBases = ConcurrentHashMap.newKeySet();
		this.knowledgeInteractionMetadata = new ConcurrentHashMap<>();
		this.readyPhaser = new Phaser(1);
		this.prefixMapping = new PrefixMappingMem();
		this.prefixMapping.setNsPrefixes(PrefixMapping.Standard);
		this.prefixMapping.setNsPrefix("kb", Vocab.ONTO_URI);
	}

	public void addKB(KnowledgeBaseImpl aKB) {
		aKB.setPhaser(this.readyPhaser);
		knowledgeBases.add(aKB);
	}

	public void sync() {
		this.startAndWaitForReady();
		this.waitForUpToDate();
	}

	/**
	 * wait until all knowledge bases are up and running and know of each other
	 * existence.
	 */
	private void startAndWaitForReady() {

		Set<KnowledgeBaseImpl> justStartedKBs = new HashSet<>();

		for (KnowledgeBaseImpl kb : this.knowledgeBases) {
			if (!kb.isStarted()) {
				kb.start();
				justStartedKBs.add(kb);
			}
		}

		// wait until all smart connectors have given the 'ready' signal (and registered
		// all their knowledge interactions).
		LOG.debug("Waiting for ready.");
		readyPhaser.arriveAndAwaitAdvance();
		readyPhaser = new Phaser(1); // reset the phaser
		LOG.debug("Everyone is ready!");

		// register our state check Knowledge Interaction on each Smart Connecotr
		GraphPattern gp = new GraphPattern(this.prefixMapping,
		// @formatter:off
				"?kb rdf:type kb:KnowledgeBase .", "?kb kb:hasName ?name .", "?kb kb:hasDescription ?description .",
				"?kb kb:hasKnowledgeInteraction ?ki .", "?ki rdf:type ?kiType .", "?ki kb:isMeta ?isMeta .",
				"?ki kb:hasCommunicativeAct ?act .", "?act rdf:type kb:CommunicativeAct .",
				"?act kb:hasRequirement ?req .", "?act kb:hasSatisfaction ?sat .", "?req rdf:type ?reqType .",
				"?sat rdf:type ?satType .", "?ki kb:hasGraphPattern ?gp .", "?gp rdf:type ?patternType .",
				"?gp kb:hasPattern ?pattern ."
		// @formatter:on
		);

		for (KnowledgeBaseImpl kb : justStartedKBs) {
			AskKnowledgeInteraction anAskKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp, null, false, true,
					false, MatchStrategy.ENTRY_LEVEL);
			this.knowledgeInteractionMetadata.put(kb, anAskKI);
			kb.register(anAskKI);
		}

		for (KnowledgeBaseImpl kb : this.knowledgeBases) {
			kb.syncKIs();
		}
	}

	private void waitForUpToDate() {
		LOG.debug("Waiting for up to date.");
		// manually check if every Knowledge Base is up to date
		boolean allUpToDate = false;

		int count = 0;

		while (!allUpToDate) {
			count++;
			LOG.info("Checking up to date knowledge bases.");
			allUpToDate = true;
			Map<KnowledgeBaseImpl, Boolean> upToDate = new HashMap<>();
			boolean kbUpToDate;
			for (KnowledgeBaseImpl kb : this.knowledgeBases) {
				kbUpToDate = kb.isUpToDate(this.knowledgeInteractionMetadata.get(kb), this.knowledgeBases);
				allUpToDate &= kbUpToDate;
				upToDate.put(kb, kbUpToDate);
				if (kbUpToDate) {
					LOG.debug("Knowledge Base {} is up to date.", kb.getKnowledgeBaseName());
				}
			}
			try {
				LOG.info("KBs up to date? {}", getUpToDateInfo(upToDate));
				Thread.sleep(100);
			} catch (InterruptedException e) {
				LOG.error("An error occured while waiting for up-to-date.", e);
			}
		}
		LOG.info("Everyone is up to date after {} rounds!", count);
	}

	private String getUpToDateInfo(Map<KnowledgeBaseImpl, Boolean> upToDate) {
		StringBuilder sb = new StringBuilder();

		sb.append("(");
		for (Map.Entry<KnowledgeBaseImpl, Boolean> entry : upToDate.entrySet()) {
			sb.append(entry.getKey().getKnowledgeBaseName()).append("=").append(entry.getValue());
			sb.append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Stops all knowledge bases in this network, and returns a future that
	 * completes when all SCs have stopped.
	 */
	public CompletableFuture<Void> stop() {
		var kbStoppedFutures = new ArrayList<CompletableFuture<Void>>();
		this.knowledgeBases.forEach(kb -> {
			LOG.info("Stopping {}", kb.getKnowledgeBaseName());
			kbStoppedFutures.add(kb.stop());
		});
		this.knowledgeBases.clear();
		return CompletableFuture.allOf(kbStoppedFutures.toArray(new CompletableFuture<?>[kbStoppedFutures.size()]));
	}

	/**
	 * Removes the given KB from this network object, but does not stop it.
	 * 
	 * It can be stopped manually using the {@link SmartConnectorImpl#stop()}
	 * method.
	 * 
	 * @param aKb An existing knowledge base that should be removed.
	 */
	public void removeKB(KnowledgeBaseImpl aKb) {
		this.knowledgeInteractionMetadata.remove(aKb);
		this.knowledgeBases.remove(aKb);
	}
}
