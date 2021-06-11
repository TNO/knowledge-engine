package eu.interconnectproject.knowledge_engine.rest.api;
import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;


public class CORSFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request,
            ContainerResponseContext response) throws IOException {
    	response.getHeaders().add("Access-Control-Allow-Origin", "*");
    	response.getHeaders().add("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, HEAD");
    	response.getHeaders().add("Access-Control-Allow-Headers","Accept, Content-Type, Knowledge-Base-Id, Knowledge-Interaction-Id");
    }
}
