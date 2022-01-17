package eu.knowledge.engine.admin.api.impl;

import eu.knowledge.engine.admin.AdminUI;
import eu.knowledge.engine.admin.Util;
import eu.knowledge.engine.admin.model.*;
import io.swagger.annotations.ApiParam;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/admin")
public class AdminApiServiceImpl {

	private static final Logger LOG = LoggerFactory.getLogger(AdminApiServiceImpl.class);

	private AdminUI admin;

	private Model model;

	//todo: Add TKE runtimes + Smart connectors per runtime in JSON response
	//todo: add active=true|false (show historical SCs, even after lease is expired. Missing or lost SCs can also be valuable information.)
	//todo: add registered knowledge interactions
	//todo: add log with timestamps of all instances of knowledge interaction (in client/GUI we can show time since last interaction)
	//todo: Select smart connector or knowledge interactions based on Knowledge-Base-Id (re-use get route in SmartConnectorLifeCycleApiServiceImpl.java)
	//todo: make route which only gets updated info since <timestamp> (for longpolling)
	//todo: test if suitable for long polling
	//TODO: remove async response? See e.g., scKiGet(String knowledgeBaseId, SecurityContext

	@GET
	@Path("/sc/overview")
	@Produces({"application/json; charset=UTF-8",  "text/plain; charset=UTF-8"})
	@io.swagger.annotations.ApiOperation(value = "Get all smart connectors in the network.", notes = "", response = SmartConnector.class, responseContainer = "List", tags = {"admin UI API",})
	@io.swagger.annotations.ApiResponses(value = {
			@io.swagger.annotations.ApiResponse(code = 200, message = "A list of smart connectors.", response = SmartConnector.class, responseContainer = "List"),
			@io.swagger.annotations.ApiResponse(code = 500, message = "If a problem occurred.", response = String.class)
	})
	public void getSCOverview(
			@Suspended final AsyncResponse asyncResponse,
			@Context SecurityContext securityContext
	) throws NotFoundException {
		admin = AdminUI.newInstance(false); //or start when init/start API route is called?
		model = this.admin.getModel(); // todo: needs locking for multi-threading? Read while write is busy.
		if (model != null && !model.isEmpty()) {
			Set<Resource> kbs = Util.getKnowledgeBaseURIs(model);
			SmartConnector[] responses = convertToModel(kbs, model);
			asyncResponse.resume(Response.ok().entity(responses).build());
		} else {
			asyncResponse.resume(Response.ok().entity(new ArrayList<SmartConnector>()).build());
			throw new NotFoundException();
		}
	}

	private eu.knowledge.engine.admin.model.SmartConnector[] convertToModel(
			Set<Resource> kbs, Model model) {
		return kbs.stream().map((kbRes) -> {
			Set<Resource> kiResources = Util.getKnowledgeInteractionURIs(model, kbRes);
			List<KnowledgeInteractionBase> knowledgeInteractions = new ArrayList<>();
			for (Resource kiRes : kiResources) {
				String type = Util.getKnowledgeInteractionType(model, kiRes);
				KnowledgeInteractionBase ki = null;
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
				knowledgeInteractions.add(ki);
				//LOG.info("\t* {}{}", knowledgeInteractionType, (Util.isMeta(model, kiRes) ? " (meta)" : ""));
				//+ add name
			}
			return new eu.knowledge.engine.admin.model.SmartConnector()
					.knowledgeBaseId(kbRes.toString())
					.knowledgeBaseName(Util.getName(model, kbRes))
					.knowledgeBaseDescription(Util.getDescription(model, kbRes))
					.knowledgeInteractions(knowledgeInteractions);
		}).toArray(eu.knowledge.engine.admin.model.SmartConnector[]::new);
	}
}