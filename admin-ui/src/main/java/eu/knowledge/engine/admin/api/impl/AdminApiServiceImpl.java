package eu.knowledge.engine.admin.api.impl;

import eu.knowledge.engine.admin.AdminUI;
import eu.knowledge.engine.admin.Util;
import eu.knowledge.engine.admin.model.*;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/admin")
public class AdminApiServiceImpl {

	private static final Logger LOG = LoggerFactory.getLogger(AdminApiServiceImpl.class);

	private AdminUI admin;

	private Model model;

	private static final String[] FILTERED_SYNTAX = {"rdf", "owl"};
	private static final List FILTERED_SYNTAX_TOKENS = Arrays.asList(".", "a", ";", "", " ");

	//todo: Add TKE runtimes + Smart connectors per runtime in JSON response - get from knowledge directory?!
	//todo: add active=true|false (show historical SCs, even after lease is expired. Missing or lost SCs can also be valuable information.)
	//todo: add actual knowledge interaction instances -> make "shadow copies" of KI's? (with meta flag for filtering out in front-end admin UI's)
	//todo: add log with timestamps of all instances of knowledge interactions (in client/GUI we can show time since last interaction)
	//todo: make route which only provides updated information since <timestamp> (for longpolling)
	//todo: remove async response. See e.g., scKiGet(String knowledgeBaseId, SecurityContext

	@GET
	@Path("/sc/all/{include-meta}")
	@Produces({"application/json; charset=UTF-8",  "text/plain; charset=UTF-8"})
	@io.swagger.annotations.ApiOperation(value = "Get all smart connectors in the network.", notes = "", response = SmartConnector.class, responseContainer = "List", tags = {"admin UI API",})
	@io.swagger.annotations.ApiResponses(value = {
			@io.swagger.annotations.ApiResponse(code = 200, message = "A list of smart connectors.", response = SmartConnector.class, responseContainer = "List"),
			@io.swagger.annotations.ApiResponse(code = 500, message = "If a problem occurred.", response = String.class)
	})
	public void getSCOverview(
			@ApiParam(value = "Include Meta-Knowledge-Interactions.", defaultValue = "true") @PathParam("include-meta") boolean includeMeta,
			@Suspended final AsyncResponse asyncResponse,
			@Context SecurityContext securityContext
	) throws NotFoundException {
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

	private eu.knowledge.engine.admin.model.SmartConnector[] findAndAddConnections(SmartConnector[] smartConnectors) {
		HashSet<Connection> allPossibleConnections = new HashSet<Connection>();
		boolean stop;
		for (SmartConnector sc : smartConnectors) {
			allPossibleConnections.addAll(sc.getConnections());
		}

		for (SmartConnector sc : smartConnectors) {
			List<Connection> identifiedConnections = new ArrayList<>();
			for (Connection currentPossibleConnection : sc.getConnections()) {
				String[] tokens = StringUtils.splitPreserveAllTokens(currentPossibleConnection.getMatchedKeyword(), " ");
				for(int i =0; i < tokens.length; i++) {
					stop = false;
					//ignore tokens with rdf/owl syntax
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
								//try to match subject, predicate and object constructs or variables between graphpatterns
								if (!sc.getKnowledgeBaseId().equals(allPossibleConnection.getKnowledgeBaseId()) &&
										allPossibleConnection.getMatchedKeyword().contains(tokens[i])) {
									identifiedConnections.add(new Connection()
											.knowledgeBaseId(allPossibleConnection.getKnowledgeBaseId())
											.connectionType("outgoing")
											.interactionType(currentPossibleConnection.getInteractionType())
											.matchedKeyword(tokens[i]));
								}
							}
						}
					}
				}
			}
			sc.connections(identifiedConnections); //overwrite temporaryConnections with identified connections via graph pattern token match
		}

		return smartConnectors;
	}

	private eu.knowledge.engine.admin.model.SmartConnector[] convertToModel(
			Set<Resource> kbs, Model model, boolean includeMeta) {
		return kbs.stream().map((kbRes) -> {
			Set<Resource> kiResources = Util.getKnowledgeInteractionURIs(model, kbRes);
			List<KnowledgeInteractionBase> knowledgeInteractions = new ArrayList<>();
			List<Connection> possibleConnections = new ArrayList<>();
			for (Resource kiRes : kiResources) {
				if (includeMeta || !Util.isMeta(model, kiRes)) {
					String type = Util.getKnowledgeInteractionType(model, kiRes);
					KnowledgeInteractionBase ki = new KnowledgeInteractionBase();
					if (type.equals("AskKnowledgeInteraction")) {
						var aki = (AskKnowledgeInteraction) new AskKnowledgeInteraction().knowledgeInteractionType(type);
						aki.setGraphPattern(Util.getGraphPattern(model, kiRes));
						ki = aki;
					} else if (type.equals("AnswerKnowledgeInteraction")) {
						var aki = (AnswerKnowledgeInteraction) new AnswerKnowledgeInteraction().knowledgeInteractionType(type);
						aki.setGraphPattern(Util.getGraphPattern(model, kiRes));
						ki = aki;
					} else if (type.equals("PostKnowledgeInteraction")) {
						var pki = (PostKnowledgeInteraction) new PostKnowledgeInteraction().knowledgeInteractionType(type);
						pki.setArgumentGraphPattern(Util.getArgument(model, kiRes));
						pki.setResultGraphPattern(Util.getResult(model, kiRes));
						ki = pki;
					} else if (type.equals("ReactKnowledgeInteraction")) {
						var rki = (ReactKnowledgeInteraction) new ReactKnowledgeInteraction().knowledgeInteractionType(type);
						rki.setArgumentGraphPattern(Util.getArgument(model, kiRes));
						rki.setResultGraphPattern(Util.getResult(model, kiRes));
						ki = rki;
					}
					ki.setKnowledgeInteractionId(kiRes.getURI());
					ki.setIsMeta(String.valueOf(Util.isMeta(model, kiRes)));
					ki.setCommunicativeAct(Util.getCommunicativeAct(model, kiRes));
					knowledgeInteractions.add(ki);
					if (!Util.isMeta(model, kiRes)) {
						possibleConnections.add(new Connection()
								.knowledgeBaseId(kbRes.toString())
								.interactionType(type)
								.matchedKeyword(Util.getGraphPattern(model, kiRes))); //todo argument and result ook invullen
					}
				}
			}
			return new eu.knowledge.engine.admin.model.SmartConnector()
					.knowledgeBaseId(kbRes.toString())
					.knowledgeBaseName(Util.getName(model, kbRes))
					.knowledgeBaseDescription(Util.getDescription(model, kbRes))
					.knowledgeInteractions(knowledgeInteractions)
					.connections(possibleConnections);
		}).toArray(eu.knowledge.engine.admin.model.SmartConnector[]::new);
	}
}