package eu.interconnectproject.knowledge_engine.rest.api.client_example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.rest.model.AskResult;
import eu.interconnectproject.knowledge_engine.rest.model.CommunicativeAct;
import eu.interconnectproject.knowledge_engine.rest.model.HandleRequest;
import eu.interconnectproject.knowledge_engine.rest.model.HandleResponse;
import eu.interconnectproject.knowledge_engine.rest.model.SmartConnector;
import eu.interconnectproject.knowledge_engine.rest.model.KnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.rest.model.PostResult;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestApiClient {
	private static final Logger LOG = LoggerFactory.getLogger(RestApiClient.class);

	private static final boolean DEBUG = false;

	public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	private static final String SC = "/sc";
	private static final String KI = "/sc/ki";
	private static final String HANDLE = "/sc/handle";
	private static final String POST = "/sc/post";
	private static final String ASK = "/sc/ask";
	
	private OkHttpClient okClient;
	private final String baseUrl;
	private final ObjectMapper mapper = new ObjectMapper();
	private final Map<String, KnowledgeHandler> knowledgeHandlers = new HashMap<>();

	public RestApiClient(String aBaseUrl) {
		this.baseUrl = aBaseUrl;
		if (DEBUG) {
			this.okClient = new OkHttpClient.Builder()
				.connectTimeout(5000, TimeUnit.SECONDS)
				.writeTimeout(5000, TimeUnit.SECONDS)
				.readTimeout(5000, TimeUnit.SECONDS)
				.callTimeout(5000, TimeUnit.SECONDS)
				.build();
		} else {
			this.okClient = new OkHttpClient();
		}
	}

	public void flushAll() {
		var scs = this.getScs();
		scs.forEach(sc -> {
			this.deleteSc(sc.getKnowledgeBaseId());
		});
	}

	public void postSc(String kbId, String kbName, String kbDesc) {
		var ilo = new SmartConnector().knowledgeBaseId(kbId).knowledgeBaseName(kbName).knowledgeBaseDescription(kbDesc);
		RequestBody body;
		try {
			body = RequestBody.create(mapper.writeValueAsString(ilo), JSON);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Could not serialize as JSON.");
		}
		Request request = new Request.Builder()
				.url(this.baseUrl + SC)
				.post(body)
				.build();
		try {
			this.okClient.newCall(request).execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<SmartConnector> getScs() {
		Request request = new Request.Builder()
				.url(this.baseUrl + SC)
				.get()
				.build();
		Response response;
		try {
			response = this.okClient.newCall(request).execute();
			return this.mapper.readValue(response.body().string(), new TypeReference<List<SmartConnector>>(){});
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not get SCs.");
		}
	}

	public void deleteSc(String kbId) {
		Request request = new Request.Builder()
				.url(this.baseUrl + SC)
				.delete()
				.header("Knowledge-Base-Id", kbId)
				.build();
		try {
			this.okClient.newCall(request).execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String postKi(String kbId, String type, String graphPattern, String argumentPattern, String resultPattern, List<String> requires, List<String> satisfies) {
		var workaround = new KnowledgeInteraction()
			.knowledgeInteractionType(type);
		if (requires != null && satisfies != null) {
			workaround.setCommunicativeAct(new CommunicativeAct()
				.requiredPurposes(requires)
				.satisfiedPurposes(satisfies)
			);
		}
		if (graphPattern != null) {
			workaround.setGraphPattern(graphPattern);
		}
		if (argumentPattern != null) {
			workaround.setArgumentGraphPattern(argumentPattern);
		}
		if (resultPattern != null) {
			workaround.setResultGraphPattern(resultPattern);
		}

		RequestBody body;
		try {
			body = RequestBody.create(mapper.writeValueAsString(workaround), JSON);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Could not serialize as JSON.");
		}
		Request request = new Request.Builder()
				.url(this.baseUrl + KI)
				.post(body)
				.header("Knowledge-Base-Id", kbId)
				.build();
		try {
			var response = this.okClient.newCall(request).execute();
			return response.body().string();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not POST the new KI.");
		}
	}

	public String postKiAsk(String kbId, String graphPattern, List<String> requires, List<String> satisfies) {
		return this.postKi(kbId, "AskKnowledgeInteraction", graphPattern, null, null, requires, satisfies);
	}

	public String postKiAsk(String kbId, String graphPattern) {
		return this.postKiAsk(kbId, graphPattern, null, null);
	}

	public String postKiAnswer(String kbId, String graphPattern, List<String> requires, List<String> satisfies, KnowledgeHandler handler) {
		String kiId = this.postKi(kbId, "AnswerKnowledgeInteraction", graphPattern, null, null, requires, satisfies);
		this.knowledgeHandlers.put(kiId, handler);
		return kiId;
	}

	public String postKiAnswer(String kbId, String graphPattern, KnowledgeHandler handler) {
		return this.postKiAnswer(kbId, graphPattern, null, null, handler);
	}

	public String postKiPost(String kbId, String argumentPattern, String resultPattern, List<String> requires, List<String> satisfies) {
		return this.postKi(kbId, "PostKnowledgeInteraction", null, argumentPattern, resultPattern, requires, satisfies);
	}

	public String postKiPost(String kbId, String argumentPattern, String resultPattern) {
		return this.postKiPost(kbId, argumentPattern, resultPattern, null, null);
	}

	public String postKiReact(String kbId, String argumentPattern, String resultPattern, List<String> requires, List<String> satisfies, KnowledgeHandler handler) {
		String kiId = this.postKi(kbId, "ReactKnowledgeInteraction", null, argumentPattern, resultPattern, requires, satisfies);
		this.knowledgeHandlers.put(kiId, handler);
		return kiId;
	}

	public String postKiReact(String kbId, String argumentPattern, String resultPattern, KnowledgeHandler handler) {
		return this.postKiReact(kbId, argumentPattern, resultPattern, null, null, handler);
	}

	public void startLongPoll(String kbId) {
		Request request = new Request.Builder()
			.url(this.baseUrl + HANDLE)
			.get()
			.header("Knowledge-Base-Id", kbId)
			.build();

		// Do the request asynchronously, and schedule a callback.
		this.okClient
			// Rebuild the okhttp client to configure the timeout.
			.newBuilder().readTimeout(500, TimeUnit.SECONDS).build()
			.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				LOG.error("Something went wrong during a handle call. Not repolling.", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (response.code() ==  202) {
					LOG.info("Received 202 from GET /sc/handle. Repolling.");
					// Do another call to wait for a new task.
					startLongPoll(kbId);
				} else if (response.code() == 200) {
					LOG.info("Received 200 from GET /sc/handle. Sending response and then repolling.");
					
					HandleRequest handleRequest = mapper.readValue(response.body().string(), new TypeReference<HandleRequest>(){});
					String kiId = handleRequest.getKnowledgeInteractionId();
					var handler = knowledgeHandlers.get(kiId);
					var handleResult = handler.handle(handleRequest);
					postKnowledgeResponse(kbId, kiId, handleResult);
					startLongPoll(kbId);
				}
			}
		});
	}

	public void postKnowledgeResponse(String kbId, String kiId, HandleResponse handleResult) {
		RequestBody body;
		try {
			body = RequestBody.create(mapper.writeValueAsString(handleResult), JSON);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Could not serialize as JSON.");
		}
		Request request = new Request.Builder()
				.url(this.baseUrl + HANDLE)
				.post(body)
				.header("Knowledge-Base-Id", kbId)
				.header("Knowledge-Interaction-Id", kiId)
				.build();
		try {
			this.okClient.newCall(request).execute();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not POST the knowledge response.");
		}
	}

	public PostResult postPost(String kbId, String kiId, List<Map<String, String>> bindings) {
		RequestBody body;
		try {
			body = RequestBody.create(mapper.writeValueAsString(bindings), JSON);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Could not serialize as JSON.");
		}
		Request request = new Request.Builder()
				.url(this.baseUrl + POST)
				.post(body)
				.header("Knowledge-Base-Id", kbId)
				.header("Knowledge-Interaction-Id", kiId)
				.build();
		try {
			var response = this.okClient.newCall(request).execute();
			return mapper.readValue(response.body().string(), new TypeReference<PostResult>(){});
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not POST the POST message.");
		}
	}

	public AskResult postAsk(String kbId, String kiId) {
		return this.postAsk(kbId, kiId, new ArrayList<Map<String, String>>());
	}

	public AskResult postAsk(String kbId, String kiId, List<Map<String, String>> bindings) {
		RequestBody body;
		try {
			body = RequestBody.create(mapper.writeValueAsString(bindings), JSON);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Could not serialize as JSON.");
		}
		Request request = new Request.Builder()
				.url(this.baseUrl + ASK)
				.post(body)
				.header("Knowledge-Base-Id", kbId)
				.header("Knowledge-Interaction-Id", kiId)
				.build();
		try {
			var response = this.okClient.newCall(request).execute();
			return mapper.readValue(response.body().string(), new TypeReference<AskResult>(){});
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not POST the ASK message.");
		}
	}
}
