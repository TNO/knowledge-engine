package eu.knowledge.engine.admin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Phaser;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.ReactExchangeInfo;

/**
 * Knowledge Base that regularly prints an overview of the currently available
 * Knowledge Bases within the network and also provides REST APIs to access the
 * same information including relations between knowledge bases.
 */
public class AdminUI extends MetadataKB {

	private static final Logger LOG = LoggerFactory.getLogger(AdminUI.class);

	private static AdminUI instance;
	private static boolean continuousLog = true;

	private static String knowledgeBaseId = "https://knowledge-engine.eu/adminui-" + UUID.randomUUID();
	private static String knowledgeBaseName = "Admin UI";
	private static String knowledgeBaseDescription = "Publishes an overview of all the available Knowledge Bases via a REST API.";

	private AdminUI() {
		super(knowledgeBaseId, knowledgeBaseName, knowledgeBaseDescription);
	}

	public static AdminUI newInstance(boolean useLog) {
		if (instance == null) {
			continuousLog = useLog;
			instance = new AdminUI();
		}

		instance.setPhaser(new Phaser(1));
		instance.start();
		instance.syncKIs();

		return instance;
	}

	@Override
	public BindingSet handleNewKnowledgeBase(ReactExchangeInfo ei) {
		BindingSet bs = super.handleNewKnowledgeBase(ei);

		// when result available (and the config is enabled), we print the
		// knowledge bases to the console.
		this.printKnowledgeBases(this.getMetadata());
		return bs;
	}

	@Override
	public BindingSet handleChangedKnowledgeBase(ReactExchangeInfo ei) {
		BindingSet bs = super.handleChangedKnowledgeBase(ei);

		// when result available (and the config is enabled), we print the
		// knowledge bases to the console.
		this.printKnowledgeBases(this.getMetadata());
		return bs;
	}

	@Override
	public BindingSet handleRemovedKnowledgeBase(ReactExchangeInfo ei) {
		BindingSet bs = super.handleRemovedKnowledgeBase(ei);

		// when result available (and the config is enabled), we print the
		// knowledge bases to the console.
		this.printKnowledgeBases(this.getMetadata());

		return bs;
	}

	@Override
	public void fetchInitialData() {
		super.fetchInitialData();

		// when result available (and the config is enabled), we print the
		// knowledge bases to the console.

		this.printKnowledgeBases(this.getMetadata());
	}

	private void printKnowledgeBases(Model model) {

		if (continuousLog) {
			// LOG.info("{}", this.getRDF(model));

			LOG.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
			LOG.info("-=-=-=-=-=-=-= Admin UI -=-=-=-=-=-=-=-");
			LOG.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
			if (!model.isEmpty()) {
				Set<Resource> kbs = Util.getKnowledgeBaseURIs(model);

				int i = 0;
				for (Resource kbRes : kbs) {
					i++;

					if (i > 1) {
						LOG.info("");
					}

					LOG.info("Knowledge Base <{}>", kbRes);

					LOG.info("\t* Name: {}", Util.getName(model, kbRes));
					LOG.info("\t* Description: {}", Util.getDescription(model, kbRes));

					Set<Resource> kiResources = Util.getKnowledgeInteractionURIs(model, kbRes);

					for (Resource kiRes : kiResources) {
						String knowledgeInteractionType = Util.getKnowledgeInteractionType(model, kiRes);
						LOG.info("\t* {}{}", knowledgeInteractionType, (Util.isMeta(model, kiRes) ? " (meta)" : ""));
						if (knowledgeInteractionType.equals("AskKnowledgeInteraction")
								|| knowledgeInteractionType.equals("AnswerKnowledgeInteraction")) {
							LOG.info("\t\t- GraphPattern: {}", Util.getGraphPattern(model, kiRes));
						} else if (knowledgeInteractionType.equals("PostKnowledgeInteraction")
								|| knowledgeInteractionType.equals("ReactKnowledgeInteraction")) {
							LOG.info("\t\t- Argument GP: {}", Util.getArgument(model, kiRes));
							LOG.info("\t\t- Result GP: {}", Util.getResult(model, kiRes));
						}
					}
				}
			} else {
				LOG.info("No other knowledge bases found.");
			}
		}
	}

}
