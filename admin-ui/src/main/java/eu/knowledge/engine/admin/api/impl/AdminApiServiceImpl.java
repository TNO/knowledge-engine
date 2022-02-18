package eu.knowledge.engine.admin.api.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.admin.AdminUI;
import eu.knowledge.engine.admin.Util;
import eu.knowledge.engine.admin.model.AnswerKnowledgeInteraction;
import eu.knowledge.engine.admin.model.AskKnowledgeInteraction;
import eu.knowledge.engine.admin.model.Connection;
import eu.knowledge.engine.admin.model.KnowledgeInteractionBase;
import eu.knowledge.engine.admin.model.PostKnowledgeInteraction;
import eu.knowledge.engine.admin.model.ReactKnowledgeInteraction;
import eu.knowledge.engine.admin.model.SmartConnector;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeInteraction;
import eu.knowledge.engine.smartconnector.impl.InteractionProcessor;
import eu.knowledge.engine.smartconnector.impl.KnowledgeInteractionInfo;
import eu.knowledge.engine.smartconnector.impl.MessageRouter;
import eu.knowledge.engine.smartconnector.impl.MetaKnowledgeBase;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor;
import eu.knowledge.engine.smartconnector.messaging.AnswerMessage;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;
import eu.knowledge.engine.smartconnector.messaging.PostMessage;
import eu.knowledge.engine.smartconnector.messaging.ReactMessage;
import io.swagger.annotations.ApiParam;

@Path("/")
public class AdminApiServiceImpl {

	private static final Logger LOG = LoggerFactory.getLogger(AdminApiServiceImpl.class);

	private AdminUI admin;

	private Model model;

	private static final String[] FILTERED_SYNTAX = { "rdf", "owl" };
	private static final List FILTERED_SYNTAX_TOKENS = Arrays.asList(".", "a", ";", "", " ");

	// todo: Add TKE runtimes + Smart connectors per runtime in JSON response - get
	// from knowledge directory?!
	// todo: add active=true|false (show historical SCs, even after lease is
	// expired. Missing or lost SCs can also be valuable information.)
	// todo: add actual knowledge interaction instances -> make "shadow copies" of
	// KI's? (with meta flag for filtering out in front-end admin UI's)
	// todo: add log with timestamps of all instances of knowledge interactions (in
	// client/GUI we can show time since last interaction)
	// todo: make route which only provides updated information since <timestamp>
	// (for longpolling)
	// todo: remove async response. See e.g., scKiGet(String knowledgeBaseId,
	// SecurityContext

	@GET
	@Path("/sc/all/{include-meta}")
	@Produces({ "application/json; charset=UTF-8", "text/plain; charset=UTF-8" })
	@io.swagger.annotations.ApiOperation(value = "Get all smart connectors in the network.", notes = "", response = SmartConnector.class, responseContainer = "List", tags = {
			"admin UI API", })
	@io.swagger.annotations.ApiResponses(value = {
			@io.swagger.annotations.ApiResponse(code = 200, message = "A list of smart connectors.", response = SmartConnector.class, responseContainer = "List"),
			@io.swagger.annotations.ApiResponse(code = 500, message = "If a problem occurred.", response = String.class) })
	public void getSCOverview(
			@ApiParam(value = "Include Meta-Knowledge-Interactions.", defaultValue = "true") @PathParam("include-meta") boolean includeMeta,
			@Suspended final AsyncResponse asyncResponse, @Context SecurityContext securityContext)
			throws NotFoundException {
		admin = AdminUI.newInstance(false);
		model = this.admin.getModel(); // todo: needs locking for multi-threading? Read while write is busy.
		if (model != null && !model.isEmpty()) {
			Set<Resource> kbs = Util.getKnowledgeBaseURIs(model);
			SmartConnector[] responses = findAndAddConnections2(convertToModel(kbs, model, includeMeta));
			asyncResponse.resume(Response.ok().entity(responses).build());
		} else {
			asyncResponse.resume(Response.ok().entity(new ArrayList<SmartConnector>()).build());
		}
	}

	private eu.knowledge.engine.admin.model.SmartConnector[] findAndAddConnections(SmartConnector[] smartConnectors) {
		HashSet<Connection> allPossibleConnections = new HashSet<Connection>();
		boolean stop;
		for (SmartConnector sc : smartConnectors) {
			allPossibleConnections.addAll(sc.getConnections());
		}

		for (SmartConnector sc : smartConnectors) {
			List<Connection> identifiedConnections = new ArrayList<>();
			for (Connection currentPossibleConnection : sc.getConnections()) {
				String[] tokens = StringUtils.splitPreserveAllTokens(currentPossibleConnection.getMatchedKeyword(),
						" ");
				for (int i = 0; i < tokens.length; i++) {
					stop = false;
					// ignore tokens with rdf/owl syntax
					if (FILTERED_SYNTAX_TOKENS.contains(tokens[i])) {
						stop = true;
					}
					if (!stop) {
						for (String syntaxFilterToken : FILTERED_SYNTAX) {
							if (tokens[i].contains(syntaxFilterToken) && !stop) {
								stop = true;
							}
						}
						if (!stop) {
							for (Connection allPossibleConnection : allPossibleConnections) {
								// try to match subject, predicate and object constructs or variables between
								// graphpatterns
								if (!sc.getKnowledgeBaseId().equals(allPossibleConnection.getKnowledgeBaseId())
										&& allPossibleConnection.getMatchedKeyword().contains(tokens[i])) {
									identifiedConnections.add(
											new Connection().knowledgeBaseId(allPossibleConnection.getKnowledgeBaseId())
													.connectionType("outgoing")
													.interactionType(currentPossibleConnection.getInteractionType())
													.matchedKeyword(tokens[i]));
								}
							}
						}
					}
				}
			}
			sc.connections(identifiedConnections); // overwrite temporaryConnections with identified connections via
													// graph pattern token match
		}

		return smartConnectors;
	}

	private eu.knowledge.engine.admin.model.SmartConnector[] findAndAddConnections2(SmartConnector[] smartConnectors) {

		Set<KnowledgeInteractionInfo> allRelevantKnowledgeInteractions = new HashSet<>();
		KnowledgeInteractionInfo kii;
		for (SmartConnector sc : smartConnectors) {
			for (KnowledgeInteractionBase ki : sc.getKnowledgeInteractions()) {
				kii = createKnowledgeInteractionInfoObject(ki);
				allRelevantKnowledgeInteractions.add(kii);

			}
		}

		// TODO first filter on communicative act!
		ReasonerProcessor rp = new ReasonerProcessor(allRelevantKnowledgeInteractions, (MessageRouter) null,
				new HashSet<Rule>());

		return smartConnectors;

	}

	private KnowledgeInteractionInfo createKnowledgeInteractionInfoObject(KnowledgeInteractionBase incomingKI) {

		URI id = null;
		URI knowledgeBaseId = null;
		KnowledgeInteraction newKI = null;
		try {

			if (incomingKI instanceof AskKnowledgeInteraction) {

				newKI = new eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction(
						new CommunicativeAct(transformPurpose(incomingKI.getCommunicativeAct().getRequiredPurposes()),
								transformPurpose(incomingKI.getCommunicativeAct().getSatisfiedPurposes())),
						new GraphPattern(((AskKnowledgeInteraction) incomingKI).getGraphPattern()),
						Boolean.valueOf(incomingKI.getIsMeta()), Boolean.valueOf(incomingKI.getIsMeta()));

			} else if (incomingKI instanceof AnswerKnowledgeInteraction) {
				newKI = new eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction(
						new CommunicativeAct(transformPurpose(incomingKI.getCommunicativeAct().getRequiredPurposes()),
								transformPurpose(incomingKI.getCommunicativeAct().getSatisfiedPurposes())),
						new GraphPattern(((AnswerKnowledgeInteraction) incomingKI).getGraphPattern()),
						Boolean.valueOf(incomingKI.getIsMeta()), Boolean.valueOf(incomingKI.getIsMeta()));
			} else if (incomingKI instanceof PostKnowledgeInteraction) {
				newKI = new eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction(
						new CommunicativeAct(transformPurpose(incomingKI.getCommunicativeAct().getRequiredPurposes()),
								transformPurpose(incomingKI.getCommunicativeAct().getSatisfiedPurposes())),
						new GraphPattern(((PostKnowledgeInteraction) incomingKI).getArgumentGraphPattern()),
						new GraphPattern(((PostKnowledgeInteraction) incomingKI).getResultGraphPattern()),
						Boolean.valueOf(incomingKI.getIsMeta()), Boolean.valueOf(incomingKI.getIsMeta()));
			} else if (incomingKI instanceof ReactKnowledgeInteraction) {
				newKI = new eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction(
						new CommunicativeAct(transformPurpose(incomingKI.getCommunicativeAct().getRequiredPurposes()),
								transformPurpose(incomingKI.getCommunicativeAct().getSatisfiedPurposes())),
						new GraphPattern(((ReactKnowledgeInteraction) incomingKI).getArgumentGraphPattern()),
						new GraphPattern(((ReactKnowledgeInteraction) incomingKI).getResultGraphPattern()),
						Boolean.valueOf(incomingKI.getIsMeta()), Boolean.valueOf(incomingKI.getIsMeta()));
			} else {
				assert false : "Should either be Ask/Answer/Post/React Knowledge Interaction and not: "
						+ incomingKI.getClass();
			}
			id = new URI(incomingKI.getKnowledgeInteractionId());
			knowledgeBaseId = new URI("https://test");

		} catch (URISyntaxException e) {

		}
		return new KnowledgeInteractionInfo(id, knowledgeBaseId, newKI);

	}

	public Set<Resource> transformPurpose(List<String> purposes) {
		Set<Resource> newPurposes = new HashSet<Resource>();

		for (String purpose : purposes) {
			newPurposes.add(ResourceFactory.createResource(purpose));
		}

		return newPurposes;
	}

	private eu.knowledge.engine.admin.model.SmartConnector[] convertToModel(Set<Resource> kbs, Model model,
			boolean includeMeta) {
		return kbs.stream().map((kbRes) -> {
			Set<Resource> kiResources = Util.getKnowledgeInteractionURIs(model, kbRes);
			List<KnowledgeInteractionBase> knowledgeInteractions = new ArrayList<>();
			List<Connection> possibleConnections = new ArrayList<>();
			for (Resource kiRes : kiResources) {
				if (includeMeta || !Util.isMeta(model, kiRes)) {
					String type = Util.getKnowledgeInteractionType(model, kiRes);
					KnowledgeInteractionBase ki = new KnowledgeInteractionBase();
					if (type.equals("AskKnowledgeInteraction")) {
						var aki = (AskKnowledgeInteraction) new AskKnowledgeInteraction()
								.knowledgeInteractionType(type);
						aki.setGraphPattern(Util.getGraphPattern(model, kiRes));
						ki = aki;
					} else if (type.equals("AnswerKnowledgeInteraction")) {
						var aki = (AnswerKnowledgeInteraction) new AnswerKnowledgeInteraction()
								.knowledgeInteractionType(type);
						aki.setGraphPattern(Util.getGraphPattern(model, kiRes));
						ki = aki;
					} else if (type.equals("PostKnowledgeInteraction")) {
						var pki = (PostKnowledgeInteraction) new PostKnowledgeInteraction()
								.knowledgeInteractionType(type);
						pki.setArgumentGraphPattern(Util.getArgument(model, kiRes));
						pki.setResultGraphPattern(Util.getResult(model, kiRes));
						ki = pki;
					} else if (type.equals("ReactKnowledgeInteraction")) {
						var rki = (ReactKnowledgeInteraction) new ReactKnowledgeInteraction()
								.knowledgeInteractionType(type);
						rki.setArgumentGraphPattern(Util.getArgument(model, kiRes));
						rki.setResultGraphPattern(Util.getResult(model, kiRes));
						ki = rki;
					}
					ki.setKnowledgeInteractionId(kiRes.getURI());
					ki.setIsMeta(String.valueOf(Util.isMeta(model, kiRes)));
					ki.setCommunicativeAct(Util.getCommunicativeAct(model, kiRes));
					knowledgeInteractions.add(ki);
					if (!Util.isMeta(model, kiRes)) {
						possibleConnections.add(new Connection().knowledgeBaseId(kbRes.toString()).interactionType(type)
								.matchedKeyword(Util.getGraphPattern(model, kiRes))); // todo argument and result ook
																						// invullen
					}
				}
			}
			return new eu.knowledge.engine.admin.model.SmartConnector().knowledgeBaseId(kbRes.toString())
					.knowledgeBaseName(Util.getName(model, kbRes))
					.knowledgeBaseDescription(Util.getDescription(model, kbRes))
					.knowledgeInteractions(knowledgeInteractions).connections(possibleConnections);
		}).toArray(eu.knowledge.engine.admin.model.SmartConnector[]::new);
	}
}