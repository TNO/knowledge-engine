package eu.knowledge.engine.rest.api.client_example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.knowledge.engine.rest.model.AnswerKnowledgeInteraction;
import eu.knowledge.engine.rest.model.AskKnowledgeInteraction;
import eu.knowledge.engine.rest.model.AskResult;
import eu.knowledge.engine.rest.model.CommunicativeAct;
import eu.knowledge.engine.rest.model.HandleRequest;
import eu.knowledge.engine.rest.model.HandleResponse;
import eu.knowledge.engine.rest.model.KnowledgeInteractionBase;
import eu.knowledge.engine.rest.model.PostKnowledgeInteraction;
import eu.knowledge.engine.rest.model.PostResult;
import eu.knowledge.engine.rest.model.ReactKnowledgeInteraction;
import eu.knowledge.engine.rest.model.SmartConnector;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class KnowledgeEngineRestApiClient {
	private static final Logger LOG = LoggerFactory.getLogger(KnowledgeEngineRestApiClient.class);

	private static final boolean DEBUG = false;

	public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	private static final String SC = "/sc";
	private static final String KI = "/sc/ki";
	private static final String HANDLE = "/sc/handle";
	private static final String POST = "/sc/post";
	private static final String ASK = "/sc/ask";

	private OkHttpClient okClient;
	private final String keEndpoint;
	private final ObjectMapper mapper = new ObjectMapper();

	private String kbId;
	private String kbName;
	private String kbDesc;
	private final Map<String, KnowledgeHandler> knowledgeHandlers = new HashMap<>();

	public KnowledgeEngineRestApiClient(String keEndpoint, String kbId, String kbName, String kbDesc) {
		this.keEndpoint = keEndpoint;
		if (DEBUG) {
			this.okClient = new OkHttpClient.Builder().connectTimeout(5000, TimeUnit.SECONDS)
					.writeTimeout(5000, TimeUnit.SECONDS).readTimeout(5000, TimeUnit.SECONDS)
					.callTimeout(5000, TimeUnit.SECONDS).build();
		} else {
			this.okClient = new OkHttpClient();
		}

		this.kbId = kbId;
		this.kbName = kbName;
		this.kbDesc = kbDesc;

		this.registerSc();
	}

	private void registerSc() {
		// First, check if it's not already there.
		var scs = this.getScs();
		var alreadyExisting = scs.stream().filter(sc -> sc.getKnowledgeBaseId().equals(this.kbId)).findAny();
		if (alreadyExisting.isPresent()) {
			// If so, log a warning, but continue.
			LOG.warn("Reusing existing smart connector with same ID. It may have a different name/description.");
			return;
		}

		var ilo = new SmartConnector().knowledgeBaseId(this.kbId).knowledgeBaseName(this.kbName).knowledgeBaseDescription(this.kbDesc);
		RequestBody body;
		try {
			body = RequestBody.create(mapper.writeValueAsString(ilo), JSON);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Could not serialize as JSON.");
		}
		Request request = new Request.Builder().url(this.keEndpoint + SC).post(body).build();
		try {
			this.okClient.newCall(request).execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<SmartConnector> getScs() {
		Request request = new Request.Builder().url(this.keEndpoint + SC).get().build();
		Response response;
		try {
			response = this.okClient.newCall(request).execute();
			return this.mapper.readValue(response.body().string(), new TypeReference<List<SmartConnector>>() {
			});
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not get SCs.");
		}
	}

	public void close() {
		LOG.info("Deleting knowledge base {}", this.kbId);
		Request request = new Request.Builder().url(this.keEndpoint + SC).delete().header("Knowledge-Base-Id", this.kbId)
				.build();
		try {
			this.okClient.newCall(request).execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String postKi(String type, String graphPattern, String argumentPattern, String resultPattern,
			List<String> requires, List<String> satisfies) {

		KnowledgeInteractionBase ki = null;
		if (type.equals("AskKnowledgeInteraction")) {
			var aki = (AskKnowledgeInteraction) new AskKnowledgeInteraction().knowledgeInteractionType(type);
			aki.setGraphPattern(graphPattern);
			ki = aki;
		} else if (type.equals("AnswerKnowledgeInteraction")) {
			var aki = (AnswerKnowledgeInteraction) new AnswerKnowledgeInteraction().knowledgeInteractionType(type);
			aki.setGraphPattern(graphPattern);
			ki = aki;
		} else if (type.equals("PostKnowledgeInteraction")) {
			var pki = (PostKnowledgeInteraction) new PostKnowledgeInteraction().knowledgeInteractionType(type);
			pki.setArgumentGraphPattern(argumentPattern);
			pki.setResultGraphPattern(resultPattern);
			ki = pki;
		} else if (type.equals("ReactKnowledgeInteraction")) {
			var rki = (ReactKnowledgeInteraction) new ReactKnowledgeInteraction().knowledgeInteractionType(type);
			rki.setArgumentGraphPattern(argumentPattern);
			rki.setResultGraphPattern(resultPattern);
			ki = rki;
		}

		if (requires != null && satisfies != null) {
			ki.setCommunicativeAct(
					new CommunicativeAct().requiredPurposes(requires).satisfiedPurposes(satisfies));
		}

		RequestBody body;
		if (ki != null) {
			try {
				body = RequestBody.create(mapper.writeValueAsString(ki), JSON);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Could not serialize as JSON.");
			}
			Request request = new Request.Builder().url(this.keEndpoint + KI).post(body).header("Knowledge-Base-Id", this.kbId)
					.build();
			try {
				var response = this.okClient.newCall(request).execute();
				return response.body().string();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not POST the new KI.");
			}
		} else {
			return null;
		}
	}

	public String registerAsk(String graphPattern, List<String> requires, List<String> satisfies) {
		return this.postKi("AskKnowledgeInteraction", graphPattern, null, null, requires, satisfies);
	}

	public String registerAsk(String graphPattern) {
		return this.registerAsk(graphPattern, null, null);
	}

	public String registerAnswer(String graphPattern, List<String> requires, List<String> satisfies,
			KnowledgeHandler handler) {
		String kiId = this.postKi("AnswerKnowledgeInteraction", graphPattern, null, null, requires, satisfies);
		this.knowledgeHandlers.put(kiId, handler);
		return kiId;
	}

	public String registerAnswer(String graphPattern, KnowledgeHandler handler) {
		return this.registerAnswer(graphPattern, null, null, handler);
	}

	public String registerPost(String argumentPattern, String resultPattern, List<String> requires,
			List<String> satisfies) {
		return this.postKi("PostKnowledgeInteraction", null, argumentPattern, resultPattern, requires, satisfies);
	}

	public String registerPost(String argumentPattern, String resultPattern) {
		return this.registerPost(argumentPattern, resultPattern, null, null);
	}

	public String registerReact(String argumentPattern, String resultPattern, List<String> requires,
			List<String> satisfies, KnowledgeHandler handler) {
		String kiId = this.postKi("ReactKnowledgeInteraction", null, argumentPattern, resultPattern, requires,
				satisfies);
		this.knowledgeHandlers.put(kiId, handler);
		return kiId;
	}

	public String registerReact(String argumentPattern, String resultPattern, KnowledgeHandler handler) {
		return this.registerReact(argumentPattern, resultPattern, null, null, handler);
	}

	public void startLongPoll() {
		Request request = new Request.Builder().url(this.keEndpoint + HANDLE).get().header("Knowledge-Base-Id", this.kbId)
				.build();

		// Do the request asynchronously, and schedule a callback.
		this.okClient
				// Rebuild the okhttp client to configure the timeout.
				.newBuilder().readTimeout(500, TimeUnit.SECONDS).build().newCall(request).enqueue(new Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						LOG.error("Something went wrong during a handle call. Not repolling.", e);
					}

					@Override
					public void onResponse(Call call, Response response) throws IOException {
						if (response.code() == 202) {
							LOG.info("Received 202 from GET /sc/handle. Repolling.");
							// Do another call to wait for a new task.
							startLongPoll();
						} else if (response.code() == 200) {
							LOG.info("Received 200 from GET /sc/handle. Sending response and then repolling.");

							HandleRequest handleRequest = mapper.readValue(response.body().string(),
									new TypeReference<HandleRequest>() {
									});
							String kiId = handleRequest.getKnowledgeInteractionId();
							var handler = knowledgeHandlers.get(kiId);
							var handleResult = handler.handle(handleRequest);
							postKnowledgeResponse(kiId, handleResult);
							startLongPoll();
						}
					}
				});
	}

	public void postKnowledgeResponse(String kiId, HandleResponse handleResult) {
		RequestBody body;
		try {
			body = RequestBody.create(mapper.writeValueAsString(handleResult), JSON);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Could not serialize as JSON.");
		}
		Request request = new Request.Builder().url(this.keEndpoint + HANDLE).post(body).header("Knowledge-Base-Id", this.kbId)
				.header("Knowledge-Interaction-Id", kiId).build();
		try {
			var response = this.okClient.newCall(request).execute();
			if (!response.isSuccessful()) {
				throw new RuntimeException(
						"Failure while posting knowledge response. Message: " + response.body().string());
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not POST the knowledge response.");
		}
	}

	public PostResult postPost(String kiId, List<Map<String, String>> bindings) {
		RequestBody body;
		try {
			body = RequestBody.create(mapper.writeValueAsString(bindings), JSON);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Could not serialize as JSON.");
		}
		Request request = new Request.Builder().url(this.keEndpoint + POST).post(body).header("Knowledge-Base-Id", this.kbId)
				.header("Knowledge-Interaction-Id", kiId).build();
		try {
			var response = this.okClient.newCall(request).execute();
			if (response.isSuccessful()) {
				return mapper.readValue(response.body().string(), new TypeReference<PostResult>() {
				});
			} else {
				throw new RuntimeException("Failed request. Message: " + response.body().string());
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not POST the POST message.");
		}
	}

	public AskResult postAsk(String kiId) {
		return this.postAsk(kiId, new ArrayList<Map<String, String>>());
	}

	public AskResult postAsk(String kiId, List<Map<String, String>> bindings) {
		RequestBody body;
		try {
			body = RequestBody.create(mapper.writeValueAsString(bindings), JSON);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Could not serialize as JSON.");
		}
		Request request = new Request.Builder().url(this.keEndpoint + ASK).post(body).header("Knowledge-Base-Id", this.kbId)
				.header("Knowledge-Interaction-Id", kiId).build();
		try {
			var response = this.okClient.newCall(request).execute();
			if (response.isSuccessful()) {
				return mapper.readValue(response.body().string(), new TypeReference<AskResult>() {
				});
			} else {
				throw new RuntimeException("Failed request. Message: " + response.body().string());
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not POST the ASK message.");
		}
	}
}
