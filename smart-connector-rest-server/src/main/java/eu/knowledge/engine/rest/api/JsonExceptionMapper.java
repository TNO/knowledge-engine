package eu.knowledge.engine.rest.api;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Since apparently Jersey gives a status 500 response when it encounters
 * unexpected input in request bodies, this exception mapper maps those
 * exceptions to a 400, since it is a bad request.
 */
@Provider
public class JsonExceptionMapper implements ExceptionMapper<JsonProcessingException> {
	@Override
	public Response toResponse(JsonProcessingException exception) {
		return Response
			.status(Response.Status.BAD_REQUEST)
			.entity(exception.getOriginalMessage())
			.type(MediaType.TEXT_PLAIN)
			.build();
	}
}
