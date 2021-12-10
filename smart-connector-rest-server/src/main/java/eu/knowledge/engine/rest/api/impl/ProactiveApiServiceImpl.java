package eu.knowledge.engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import eu.knowledge.engine.rest.api.NotFoundException;
import eu.knowledge.engine.rest.model.AskExchangeInfo;
import eu.knowledge.engine.rest.model.AskResult;
import eu.knowledge.engine.rest.model.KnowledgeInteractionWithId;
import eu.knowledge.engine.rest.model.PostExchangeInfo;
import eu.knowledge.engine.rest.model.PostResult;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.ExchangeInfo.Initiator;
import io.swagger.annotations.ApiParam;

@Path("/sc")
public class ProactiveApiServiceImpl {

	private static final Logger LOG = LoggerFactory.getLogger(ProactiveApiServiceImpl.class);

	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	@POST
	@Path("/ask")
	@Consumes({ "application/json; charset=UTF-8" })
	@Produces({ "application/json; charset=UTF-8", "text/plain; charset=UTF-8" })
	public void scAskPost(
			@ApiParam(value = "The Knowledge Base Id for which to execute the ask.", required = true) @HeaderParam("Knowledge-Base-Id") String knowledgeBaseId,
			@ApiParam(value = "The Ask Knowledge Interaction Id to execute.", required = true) @HeaderParam("Knowledge-Interaction-Id") String knowledgeInteractionId,

			@ApiParam(value = "The keys bindings are allowed to be incomplete, but they must correspond to the binding keys that were defined in the knowledge interaction.", required = true) @NotNull @Valid JsonNode recipientAndBindingSet,
			@Suspended final AsyncResponse asyncResponse, @Context SecurityContext securityContext) {

		LOG.debug("scAskPost called for KB {} and KI {} - {}", knowledgeBaseId, knowledgeInteractionId,
				recipientAndBindingSet);

		RecipientAndBindingSet recipientAndBindingSetObject;
		try {
			recipientAndBindingSetObject = new RecipientAndBindingSet(recipientAndBindingSet);
		} catch (IllegalArgumentException e) {
			LOG.debug("", e);
			asyncResponse.resume(Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build());
			return;
		}

		if (knowledgeBaseId == null || knowledgeInteractionId == null) {
			asyncResponse.resume(Response.status(Status.BAD_REQUEST)
					.entity("Both Knowledge-Base-Id and Knowledge-Interaction-Id headers should be non-null.").build());
			return;
		}

		var kb = this.manager.getKB(knowledgeBaseId);
		if (kb == null) {
			if (this.manager.hasSuspendedKB(knowledgeBaseId)) {
				this.manager.removeSuspendedKB(knowledgeBaseId);
				asyncResponse.resume(Response.status(Status.NOT_FOUND).entity(
						"This knowledge base has been suspended due to inactivity. Please reregister the knowledge base and its knowledge interactions.")
					.build());
				return;
			} else {
				asyncResponse.resume(
						Response.status(Status.NOT_FOUND).entity("Smart connector not found, because its ID is unknown.").build());
				return;
			}
		}

		try {
			new URI(knowledgeInteractionId);
		} catch (URISyntaxException e) {
			asyncResponse.resume(Response.status(Status.BAD_REQUEST)
					.entity("Knowledge interaction not found, because its ID must be a valid URI.").build());
			return;
		}

		if (!kb.hasKnowledgeInteraction(knowledgeInteractionId)) {
			asyncResponse.resume(
					Response.status(Status.NOT_FOUND).entity("Knowledge Interaction not found, because its ID is unknown.").build());
			return;
		}

		KnowledgeInteractionWithId ki = kb.getKnowledgeInteraction(knowledgeInteractionId);
		if (!ki.getKnowledgeInteractionType().equals("AskKnowledgeInteraction")) {
			asyncResponse.resume(Response.status(Status.BAD_REQUEST)
					.entity("Given Knowledge Interaction ID should have type AskKnowledgeInteraction and not "
							+ ki.getKnowledgeInteractionType() + ".")
					.build());
			return;
		}

		try {
			var askFuture = kb.ask(knowledgeInteractionId, recipientAndBindingSetObject.recipient,
					recipientAndBindingSetObject.bindingSet);

			askFuture.thenAccept(askResult -> {

				LOG.debug("AskResult received, resuming async response: {}", askResult);
				List<AskExchangeInfo> infos = askResult.getExchangeInfoPerKnowledgeBase().stream()
						.map(aei -> new AskExchangeInfo().bindingSet(this.bindingSetToList(aei.getBindings()))
								.knowledgeBaseId(aei.getKnowledgeBaseId().toString())
								.knowledgeInteractionId(aei.getKnowledgeInteractionId().toString())
								.exchangeStart(Date.from(aei.getExchangeStart()))
								.exchangeStart(Date.from(aei.getExchangeStart()))
								.initiator(toInitiatorEnumAsk(aei.getInitiator()))
								.exchangeEnd(Date.from(aei.getExchangeEnd())).status(aei.getStatus().toString())
								.failedMessage(aei.getFailedMessage()))
						.collect(Collectors.toList());

				AskResult ar = new AskResult().bindingSet(this.bindingSetToList(askResult.getBindings()))
						.exchangeInfo(infos);

				asyncResponse.resume(Response.status(Status.OK).entity(ar).build());
			});

		} catch (URISyntaxException | InterruptedException | ExecutionException e) {
			LOG.trace("", e);
			asyncResponse.resume(Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Something went wrong while sending a POST or while waiting on the REACT.").build());
		} catch (IllegalArgumentException e) {
			LOG.trace("", e);
			asyncResponse.resume(Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build());
		}
	}

	private List<Map<String, String>> bindingSetToList(BindingSet bindings) {
		var listBindings = new ArrayList<Map<String, String>>(bindings.size());
		bindings.forEach((binding) -> {
			Map<String, String> listBinding = new HashMap<>();
			binding.forEach((k, v) -> {
				listBinding.put(k, v);
			});
			listBindings.add(listBinding);
		});
		return listBindings;
	}

	private AskExchangeInfo.InitiatorEnum toInitiatorEnumAsk(Initiator initiator) {
		switch (initiator) {
		case KNOWLEDGEBASE:
			return AskExchangeInfo.InitiatorEnum.KNOWLEDGEBASE;
		case REASONER:
			return AskExchangeInfo.InitiatorEnum.REASONER;
		default:
			assert false;
			return null;
		}
	}

	@POST
	@Path("/post")
	@Consumes({ "application/json; charset=UTF-8" })
	@Produces({ "application/json; charset=UTF-8", "text/plain; charset=UTF-8" })
	public void scPostPost(
			@ApiParam(value = "The Knowledge Base Id for which to execute the ask.", required = true) @HeaderParam("Knowledge-Base-Id") String knowledgeBaseId,
			@ApiParam(value = "The Post Knowledge Interaction Id to execute.", required = true) @HeaderParam("Knowledge-Interaction-Id") String knowledgeInteractionId,
			@ApiParam(value = "The keys bindings must be complete, and they must correspond to the binding keys that were defined in the knowledge interaction.", required = true) @NotNull @Valid JsonNode recipientAndBindingSet,
			@Suspended final AsyncResponse asyncResponse, @Context SecurityContext securityContext)
			throws NotFoundException {

		LOG.debug("scPostPost called for KB {} and KI {} - {}", knowledgeBaseId, knowledgeInteractionId,
				recipientAndBindingSet);

		RecipientAndBindingSet recipientAndBindingSetObject;
		try {
			recipientAndBindingSetObject = new RecipientAndBindingSet(recipientAndBindingSet);
		} catch (IllegalArgumentException e) {
			LOG.trace("", e);
			asyncResponse.resume(Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build());
			return;
		}

		if (knowledgeBaseId == null || knowledgeInteractionId == null) {
			asyncResponse.resume(Response.status(Status.BAD_REQUEST)
					.entity("Both Knowledge-Base-Id and Knowledge-Interaction-Id headers should be non-null.").build());
			return;
		}

		var kb = this.manager.getKB(knowledgeBaseId);
		if (kb == null) {
			if (this.manager.hasSuspendedKB(knowledgeBaseId)) {
				manager.removeSuspendedKB(knowledgeBaseId);
				asyncResponse.resume(Response.status(Status.NOT_FOUND).entity(
						"This knowledge base has been suspended due to inactivity. Please reregister the knowledge base and its knowledge interactions.")
					.build());
				return;
			} else {
				asyncResponse.resume(
					Response.status(Status.NOT_FOUND).entity("Smart connector not found, because its ID is unknown.").build());
				return;
			}
		}

		try {
			new URI(knowledgeInteractionId);
		} catch (URISyntaxException e) {
			LOG.trace("", e);
			asyncResponse.resume(Response.status(Status.BAD_REQUEST)
					.entity("Knowledge interaction not found, because its ID must be a valid URI.").build());
			return;
		}

		if (!kb.hasKnowledgeInteraction(knowledgeInteractionId)) {
			asyncResponse.resume(
					Response.status(404).entity("Knowledge Interaction not found, because its ID is unknown.").build());
			return;
		}

		KnowledgeInteractionWithId ki = kb.getKnowledgeInteraction(knowledgeInteractionId);
		if (!ki.getKnowledgeInteractionType().equals("PostKnowledgeInteraction")) {
			asyncResponse.resume(Response.status(Status.BAD_REQUEST)
					.entity("Given Knowledge Interaction ID should have type PostKnowledgeInteraction and not "
							+ ki.getKnowledgeInteractionType() + ".")
					.build());
			return;
		} else {

			try {
				var postFuture = kb.post(knowledgeInteractionId, recipientAndBindingSetObject.recipient,
						recipientAndBindingSetObject.bindingSet);

				postFuture.thenAccept(postResult -> {

					LOG.debug("PostResult received, resuming async response: {}", postResult);

					List<PostExchangeInfo> infos = postResult.getExchangeInfoPerKnowledgeBase().stream()
							.map(pei -> new PostExchangeInfo()
									.argumentBindingSet(this.bindingSetToList(pei.getArgument()))
									.resultBindingSet(this.bindingSetToList(pei.getResult()))
									.knowledgeBaseId(pei.getKnowledgeBaseId().toString())
									.knowledgeInteractionId(pei.getKnowledgeInteractionId().toString())
									.initiator(toInitiatorEnumPost(pei.getInitiator()))
									.exchangeStart(Date.from(pei.getExchangeStart()))
									.exchangeEnd(Date.from(pei.getExchangeEnd())).status(pei.getStatus().toString())
									.failedMessage(pei.getFailedMessage()))
							.collect(Collectors.toList());

					PostResult pr = new PostResult().resultBindingSet(this.bindingSetToList(postResult.getBindings()))
							.exchangeInfo(infos);

					asyncResponse.resume(pr);

				});

			} catch (URISyntaxException | InterruptedException | ExecutionException e) {
				LOG.trace("", e);
				asyncResponse.resume(Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity("Something went wrong while sending a POST or while waiting on the REACT.").build());
			} catch (IllegalArgumentException e) {
				LOG.trace("", e);
				asyncResponse.resume(Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build());
			}
		}
	}

	private PostExchangeInfo.InitiatorEnum toInitiatorEnumPost(Initiator initiator) {
		switch (initiator) {
		case KNOWLEDGEBASE:
			return PostExchangeInfo.InitiatorEnum.KNOWLEDGEBASE;
		case REASONER:
			return PostExchangeInfo.InitiatorEnum.REASONER;
		default:
			assert false;
			return null;
		}
	}

}
