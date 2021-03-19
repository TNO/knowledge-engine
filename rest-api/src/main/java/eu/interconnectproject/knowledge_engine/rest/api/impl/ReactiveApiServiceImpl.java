package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.rest.api.NotFoundException;
import io.swagger.annotations.ApiParam;

@Path("/sc")
public class ReactiveApiServiceImpl {

	private static final Logger LOG = LoggerFactory.getLogger(ReactiveApiServiceImpl.class);

	@GET
	@Path("/handle")
	@Consumes({ "application/json" })
	@Produces({ "application/json", "text/plain" })
	public void scHandleGet(
			@ApiParam(value = "The Knowledge Base Id for which to execute the ask.", required = true) @HeaderParam("Knowledge-Base-Id") String knowledgeBaseId,
			@Suspended final AsyncResponse asyncResponse, @Context SecurityContext securityContext,
			@Context HttpServletRequest servletRequest) throws NotFoundException, IOException {

		assert servletRequest.isAsyncStarted();
		final AsyncContext asyncContext = servletRequest.getAsyncContext();
		final ServletOutputStream s = asyncContext.getResponse().getOutputStream();

		s.setWriteListener(new WriteListener() {
			volatile boolean done = false;

			public void onWritePossible() throws IOException {
				while (s.isReady()) {
					if (done) {
						asyncContext.complete();
						asyncResponse.isCancelled();
						break;
					} else {
						s.write(12);
						done = true;
					}
				}
			}

			@Override
			public void onError(Throwable t) {
				LOG.error("AN error occurred.");

			}
		});
	}

	@POST
	@Path("/handle")
	@Consumes({ "application/json" })
	@Produces({ "application/json", "text/plain" })
	public Response scHandlePost(
			@ApiParam(value = "The Knowledge Base Id for which to execute the ask.", required = true) @HeaderParam("Knowledge-Base-Id") String knowledgeBaseId,
			@ApiParam(value = "The Post Knowledge Interaction Id to execute.", required = true) @HeaderParam("Knowledge-Interaction-Id") String knowledgeInteractionId,
			@ApiParam(value = "") @Valid List<Map<String, String>> requestBody,
			@Context SecurityContext securityContext) throws NotFoundException {

		return Response.serverError().build();
	}

}
