package eu.knowledge.engine.admin.api;

import com.fasterxml.jackson.core.JsonProcessingException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
