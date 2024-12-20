package eu.knowledge.engine.admin.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.admin.AdminUI;
import eu.knowledge.engine.admin.MetaKB;
import eu.knowledge.engine.admin.Util;
import eu.knowledge.engine.admin.model.AnswerKnowledgeInteraction;
import eu.knowledge.engine.admin.model.AskKnowledgeInteraction;
import eu.knowledge.engine.admin.model.Connection;
import eu.knowledge.engine.admin.model.KnowledgeInteractionBase;
import eu.knowledge.engine.admin.model.PostKnowledgeInteraction;
import eu.knowledge.engine.admin.model.ReactKnowledgeInteraction;
import eu.knowledge.engine.admin.model.SmartConnector;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.rulenode.RuleNode;
import eu.knowledge.engine.smartconnector.api.AnswerExchangeInfo;
import eu.knowledge.engine.smartconnector.api.AnswerHandler;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.ReactExchangeInfo;
import eu.knowledge.engine.smartconnector.api.ReactHandler;
import eu.knowledge.engine.smartconnector.impl.KnowledgeInteractionInfo;
import eu.knowledge.engine.smartconnector.impl.MessageRouter;
import eu.knowledge.engine.smartconnector.impl.MyKnowledgeInteractionInfo;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

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
	@Operation(summary = "Get all smart connectors in the network.", description = "", tags = { "admin UI API", })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "A list of smart connectors.", content = @Content(schema = @Schema(implementation = SmartConnector.class))),
			@ApiResponse(responseCode = "500", description = "If a problem occurred.", content = @Content(schema = @Schema(implementation = String.class))) })
	public void getSCOverview(
			@Parameter(description = "Include Meta-Knowledge-Interactions.", schema = @Schema(defaultValue = "true")) @PathParam("include-meta") boolean includeMeta,
			@Suspended final AsyncResponse asyncResponse, @Context SecurityContext securityContext)
			throws NotFoundException {
		admin = AdminUI.newInstance(false);
		model = this.admin.getModel(); // todo: needs locking for multi-threading? Read while write is busy.
		if (model != null && !model.isEmpty()) {
			Set<Resource> kbs = Util.getKnowledgeBaseURIs(model);
			SmartConnector[] responses = findAndAddConnections(convertToModel(kbs, model, includeMeta));
			asyncResponse.resume(Response.ok().entity(responses).build());
		} else {
			asyncResponse.resume(Response.ok().entity(new ArrayList<SmartConnector>()).build());
		}
	}

	@GET
	@Path("/reload")
	@Operation(summary = "Manually reload the admin-ui's smart connectors within the network. This is sometimes necessary when the initial load did not pick up all SCs correctly.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "If the SC were reloaded."),
			@ApiResponse(responseCode = "500", description = "If a problem occurred.") })
	public void reloadSCs() {
		AdminUI.newInstance(false).fetchInitialData();
	}

	private eu.knowledge.engine.admin.model.SmartConnector[] findAndAddConnections(SmartConnector[] smartConnectors) {

		Set<KnowledgeInteractionInfo> allRelevantKnowledgeInteractions = new HashSet<>();
		KnowledgeInteractionInfo kii;
		for (SmartConnector sc : smartConnectors) {
			for (KnowledgeInteractionBase ki : sc.getKnowledgeInteractions()) {
				if (!Boolean.valueOf(ki.getIsMeta())) {
					kii = createKnowledgeInteractionInfoObject(sc.getKnowledgeBaseId(), ki);
					allRelevantKnowledgeInteractions.add(kii);
				}
			}
		}

		LOG.debug("Number of relevant Knowledge Interactions: {}", allRelevantKnowledgeInteractions.size());

		// TODO first filter on communicative act!
		List<Connection> identifiedConnections = null;
		for (SmartConnector sc : smartConnectors) {

			for (KnowledgeInteractionBase ki : sc.getKnowledgeInteractions()) {
				if (!Boolean.valueOf(ki.getIsMeta())) {
					ReasonerProcessor rp = new ReasonerProcessor(allRelevantKnowledgeInteractions, (MessageRouter) null,
							new HashSet<Rule>());

					RuleNode rn = null;
					if (ki.getKnowledgeInteractionType().equalsIgnoreCase("AskKnowledgeInteraction")) {
						rp.planAskInteraction(createKnowledgeInteractionInfoObject(sc.getKnowledgeBaseId(), ki));
						rn = rp.getReasonerPlan().getStartNode();
					} else if (ki.getKnowledgeInteractionType().equalsIgnoreCase("PostKnowledgeInteraction")) {
						rp.planPostInteraction(createKnowledgeInteractionInfoObject(sc.getKnowledgeBaseId(), ki));
						rn = rp.getReasonerPlan().getStartNode();
					}
					if (rn != null)
						identifiedConnections = Util.createConnectionObjects(rn);
					else
						identifiedConnections = new ArrayList<>();
					ki.connections(identifiedConnections);
				}

			}
		}

		return smartConnectors;

	}

	/**
	 * We return them as MyKnowledgeInteractionInfo, but most of the time we only
	 * need the KnowledgeInteractionInfo signatures.
	 * 
	 * @param incomingKI
	 * @return
	 */
	private MyKnowledgeInteractionInfo createKnowledgeInteractionInfoObject(String kbId,
			KnowledgeInteractionBase incomingKI) {
		URI id = null;
		URI knowledgeBaseId = null;
		KnowledgeInteraction newKI = null;
		MyKnowledgeInteractionInfo myKnowledgeInteractionInfo;

		ReactHandler rHandler = new ReactHandler() {
			@Override
			public BindingSet react(eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction anRKI,
					ReactExchangeInfo aReactExchangeInfo) {
				return null;
			}
		};

		AnswerHandler aHandler = new AnswerHandler() {
			@Override
			public BindingSet answer(eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction anAKI,
					AnswerExchangeInfo anAnswerExchangeInfo) {
				return null;
			}
		};

		try {
			id = new URI(incomingKI.getKnowledgeInteractionId());
			knowledgeBaseId = new URI(kbId);
			if (incomingKI instanceof AskKnowledgeInteraction) {

				newKI = new eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction(
						new CommunicativeAct(transformPurpose(incomingKI.getCommunicativeAct().getRequiredPurposes()),
								transformPurpose(incomingKI.getCommunicativeAct().getSatisfiedPurposes())),
						new GraphPattern(((AskKnowledgeInteraction) incomingKI).getGraphPattern()),
						Boolean.valueOf(incomingKI.getIsMeta()), Boolean.valueOf(incomingKI.getIsMeta()));
				myKnowledgeInteractionInfo = new MyKnowledgeInteractionInfo(id, knowledgeBaseId, newKI, null, null);
			} else if (incomingKI instanceof AnswerKnowledgeInteraction) {
				newKI = new eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction(
						new CommunicativeAct(transformPurpose(incomingKI.getCommunicativeAct().getRequiredPurposes()),
								transformPurpose(incomingKI.getCommunicativeAct().getSatisfiedPurposes())),
						new GraphPattern(((AnswerKnowledgeInteraction) incomingKI).getGraphPattern()),
						Boolean.valueOf(incomingKI.getIsMeta()), Boolean.valueOf(incomingKI.getIsMeta()));
				myKnowledgeInteractionInfo = new MyKnowledgeInteractionInfo(id, knowledgeBaseId, newKI, aHandler, null);
			} else if (incomingKI instanceof PostKnowledgeInteraction) {
				String resultGraphPattern = ((PostKnowledgeInteraction) incomingKI).getResultGraphPattern();
				newKI = new eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction(
						new CommunicativeAct(transformPurpose(incomingKI.getCommunicativeAct().getRequiredPurposes()),
								transformPurpose(incomingKI.getCommunicativeAct().getSatisfiedPurposes())),
						new GraphPattern(((PostKnowledgeInteraction) incomingKI).getArgumentGraphPattern()),
						resultGraphPattern != null ? new GraphPattern(resultGraphPattern) : null,
						Boolean.valueOf(incomingKI.getIsMeta()), Boolean.valueOf(incomingKI.getIsMeta()));
				myKnowledgeInteractionInfo = new MyKnowledgeInteractionInfo(id, knowledgeBaseId, newKI, null, null);
			} else if (incomingKI instanceof ReactKnowledgeInteraction) {
				String resultGraphPattern = ((ReactKnowledgeInteraction) incomingKI).getResultGraphPattern();
				newKI = new eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction(
						new CommunicativeAct(transformPurpose(incomingKI.getCommunicativeAct().getRequiredPurposes()),
								transformPurpose(incomingKI.getCommunicativeAct().getSatisfiedPurposes())),
						new GraphPattern(((ReactKnowledgeInteraction) incomingKI).getArgumentGraphPattern()),
						resultGraphPattern != null ? new GraphPattern(resultGraphPattern) : null,
						Boolean.valueOf(incomingKI.getIsMeta()), Boolean.valueOf(incomingKI.getIsMeta()));
				myKnowledgeInteractionInfo = new MyKnowledgeInteractionInfo(id, knowledgeBaseId, newKI, null, rHandler);
			} else {
				assert false : "Should either be Ask/Answer/Post/React Knowledge Interaction and not: "
						+ incomingKI.getClass();
				myKnowledgeInteractionInfo = null;
			}

		} catch (URISyntaxException e) {
			myKnowledgeInteractionInfo = null;
			LOG.error("An error occurred.");
		}

		return myKnowledgeInteractionInfo;

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
			for (Resource kiRes : kiResources) {
				List<Connection> possibleConnections = new ArrayList<>();
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
					ki.connections(possibleConnections);
				}
			}
			return new eu.knowledge.engine.admin.model.SmartConnector().knowledgeBaseId(kbRes.toString())
					.knowledgeBaseName(Util.getName(model, kbRes))
					.knowledgeBaseDescription(Util.getDescription(model, kbRes))
					.knowledgeInteractions(knowledgeInteractions);
		}).toArray(eu.knowledge.engine.admin.model.SmartConnector[]::new);
	}
}