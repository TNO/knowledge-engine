package eu.knowledge.engine.rest.api;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

@Provider
public class EofExceptionInterceptor implements WriterInterceptor {

	private static final Logger LOG = LoggerFactory.getLogger(EofExceptionInterceptor.class);

	@Override
	public void aroundWriteTo(WriterInterceptorContext context) throws IOException {

		try {
			context.proceed();
		} catch (Exception e) {
			if (isClientDisconnect(e)) {
				LOG.warn("Client disconnected while writing response: {}", findMessage(e));
				LOG.trace("The following exception occurred.", e);
				return;
			}

			throw e;
		}
	}

	private static String findMessage(Throwable t) {
		while (t != null) {
			if (t.getMessage() != null && !t.getMessage().isBlank()) {
				return t.getMessage();
			}
			t = t.getCause();
		}
		return "<no message>";
	}

	private static boolean isClientDisconnect(Throwable t) {
		while (t != null) {
			if (t instanceof IOException) {
				String msg = t.getMessage();
				if (msg != null) {
					msg = msg.toLowerCase();

					if (msg.contains("broken pipe") || msg.contains("connection reset by peer")) {
						return true;
					}
				}
			}

			t = t.getCause();
		}
		return false;
	}
}