package eu.knowledge.engine.smartconnector.edc;

import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

import static eu.knowledge.engine.smartconnector.edc.JsonUtil.findByJsonPointerExpression;

public class EdcConnectorClient {

	private final Logger LOG = LoggerFactory.getLogger(EdcConnectorClient.class);
	private final String edcConnectorManagementUrl;
	private final HttpClient httpClient;

	public EdcConnectorClient(String edcConnectorManagementUrl) {
		this.edcConnectorManagementUrl = edcConnectorManagementUrl;
		var executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		this.httpClient = HttpClient.newBuilder().executor(executorService).build();
	}

	/**
	 * Each TKE needs to register a dataplane whenever it wants to expose assets.
	 * This function registers a data plane for HttpProxy/HttpData.
	 */
	public String registerDataPlane(String dataPlaneId, String dataPlaneControlUrl, String dataPlanePublicUrl) {
		var url = getManagementUrl("/v2/dataplanes");
		LOG.info("registerDataPlane at: {}", url);
		var payload = """
				{
				  "@context": {
					"edc": "https://w3id.org/edc/v0.0.1/ns/"
				  },
				  "@id": "%s",
				  "url": "%s",
				  "allowedSourceTypes": [
					"HttpData"
				  ],
				  "allowedDestTypes": [
					"HttpProxy",
					"HttpData"
				  ],
				  "properties": {
					"https://w3id.org/edc/v0.0.1/ns/publicApiUrl": "%s"
				  }
				}
				 """.formatted(dataPlaneId, dataPlaneControlUrl, dataPlanePublicUrl);
		HttpResponse<String> response = httpPost(url, payload);
		LOG.info("Register data plane response: {}", response.body());
		return response.body();
	}

	/**
	 * Register an assert given the provided assetId and tkeUrl.
	 */
	public String registerAsset(String assetId, String tkeUrl, String tkeAssetName) {
		String url = getManagementUrl("/v3/assets");
		String payload = """
					{
						"@context": [
						  	"https://w3id.org/edc/connector/management/v0.0.1"
						],
						"@id": "%s",
						"properties": {
						  	"name": "%s",
						  	"contenttype": "application/json"
						},
						"dataAddress": {
							"type": "HttpData",
							"name": "%s",
							"baseUrl": "%s",
							"proxyMethod": "true",
							"proxyPath": "true",
							"proxyBody": "true"
						}
					}
				""".formatted(assetId, tkeAssetName, tkeAssetName, tkeUrl);
		LOG.info("Registering asset at: {}, Request: {}", url, payload);
		HttpResponse<String> response = httpPost(url, payload);
		LOG.info("Registering asset response: {}", response.body());
		return response.body();
	}

	/**
	 * The policy defines permissions which can be applied to an asset.
	 *
	 * @return
	 */
	public String registerPolicy(String policyId) {
		String url = getManagementUrl("/v3/policydefinitions");
		String payload = """
					{
						"@context": {
							"edc": "https://w3id.org/edc/v0.0.1/ns/",
						  	"odrl": "http://www.w3.org/ns/odrl/2/"
						},
						"@id": "%s",
						"policy": {
							"@context": "http://www.w3.org/ns/odrl.jsonld",
							"@type": "Set",
							"odrl:permission": [],
							"odrl:prohibition": [],
							"odrl:obligation": []
						}
					}
				""".formatted(policyId);
		LOG.info("Registering policy at: {} Request: {}", url, payload);
		HttpResponse<String> response = httpPost(url, payload);
		LOG.info("Registering policy response: {}", response.body());
		return response.body();
	}

	public String registerContractDefinition(String contractDefinitionId, String accessPolicyId,
			String contractPolicyId, String assetId) {
		String url = getManagementUrl("/v3/contractdefinitions");
		String payload = """
					{
						"@context": {
							"edc": "https://w3id.org/edc/v0.0.1/ns/"
						},
						"@id": "%s",
						"accessPolicyId": "%s",
						"contractPolicyId": "%s",
						"assetsSelector": [
							{
								"operandLeft": "id",
								"operator": "=",
								"operandRight": "%s"
							}
						]
					}
				""".formatted(contractDefinitionId, accessPolicyId, contractPolicyId, assetId);
		LOG.info("Registering contract definition at: {} Request: {}", url, payload);
		HttpResponse<String> response = httpPost(url, payload);
		LOG.info("Registering contract definition response: {}", response.body());
		return response.body();
	}

	/**
	 * Catalog requests are sent to ones own connector.
	 *
	 * @param counterPartyAddress
	 * @return
	 */
	public String catalogRequest(String counterPartyAddress, String counterParticipantId) {
		String url = getManagementUrl("/v3/catalog/request");
		String payload = """
					{
						"@context": {
						  	"edc": "https://w3id.org/edc/v0.0.1/ns/"
						},
						"counterPartyAddress": "%s",
						"counterPartyId": "%s",
						"protocol": "dataspace-protocol-http"
					}
				""".formatted(counterPartyAddress, counterParticipantId);
		LOG.info("Requesting catalog at: {}, Request: {}", url, payload);
		HttpResponse<String> postResponse = httpPost(url, payload);
		LOG.info("Requesting Catalog response: {}", postResponse.body());
		return postResponse.body();
	}

	/**
	 * In order to request any data, a contract gets negotiated, and an agreement is
	 * resulting has to be negotiated between providers and consumers.
	 * <p>
	 * The consumer now needs to initiate a contract negotiation sequence with the
	 * provider. That sequence looks as follows:
	 * <p>
	 * Consumer sends a contract offer to the provider (currently, this has to be
	 * equal to the provider's offer!)
	 * Provider validates the received offer against its own offer
	 * Provider either sends an agreement or a rejection, depending on the
	 * validation result
	 * In case of successful validation, provider and consumer store the received
	 * agreement for later reference
	 *
	 * @return
	 */
	public String negotiateContract(String counterPartyId, String counterPartyAddress, String policyId,
			String assetId) {
		String url = getManagementUrl("/v3/contractnegotiations");
		String payload = """
					{
						"@context": {
							"edc": "https://w3id.org/edc/v0.0.1/ns/",
							"odrl": "http://www.w3.org/ns/odrl/2/"
						},
						"@type": "ContractRequest",
						"counterPartyAddress": "%s",
						"protocol": "dataspace-protocol-http",
						"policy": {
							"@context": "http://www.w3.org/ns/odrl.jsonld",
							"@id": "%s",
							"@type": "Offer",
							"assigner": "%s",
							"target": "%s"
						}
					}
				""".formatted(counterPartyAddress, policyId, counterPartyId, assetId);

		LOG.info("Negotiate contract at: {}, Request: {}", url, payload);
		HttpResponse<String> postResponse = httpPost(url, payload);
		LOG.info("Negotiate contract response: {}", postResponse.body());
		return postResponse.body();
	}

	public String contractAgreement(String json) {
		String contractAgreementId = findByJsonPointerExpression(json, "/@id");
		var url = getManagementUrl("/v3/contractnegotiations/" + contractAgreementId);
		LOG.info("contractAgreement at: {}", url);
		final List<String> responses = new ArrayList<>();

		Awaitility.await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10)).until(() -> {
			HttpResponse<String> response = httpGet(url);
			responses.add(response.body());
			String state = findByJsonPointerExpression(response.body(), "/state");
			return Objects.equals(state, "FINALIZED");
		});

		return responses.get(responses.size() - 1);
	}

	public String transferProcess(String counterPartyAddress, String contractAgreementId) {
		var url = getManagementUrl("/v3/transferprocesses");
		var payload = """
			{
				"@context": {
					"@vocab": "https://w3id.org/edc/v0.0.1/ns/"
				},
				"counterPartyAddress": "%s",
				"contractId": "%s",
				"protocol": "dataspace-protocol-http",
				"transferType": "HttpData-PULL",
				"dataDestination": {
					"type": "HttpProxy"
				}
			}
		""".formatted(counterPartyAddress, contractAgreementId);
		LOG.info("Start transfer process at: {}, Request: {}", url, payload);
		HttpResponse<String> postResponse = httpPost(url, payload);
		LOG.info("Start transfer process response: {}", postResponse.body());
		return postResponse.body();
	}

	/**
	 * Contract the EDC connector URL for the /management endpoint.
	 */
	private String getManagementUrl(String suffix) {
		return edcConnectorManagementUrl + suffix;
	}

	private HttpResponse<String> httpGet(String url) {
		return httpGet(url, "application/json");
	}

	private HttpResponse<String> httpGet(String url, String accept) {
		LOG.info("Calling: {}", url);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(toURI(url))
				.headers("Accept", accept)
				.GET()
				.build();

		return sendRequest(request);
	}

	private HttpResponse<String> httpPost(String url, String payload) {
		return httpPost(url, "application/json", payload);
	}

	private HttpResponse<String> httpPost(String url, String contentType, String payload) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(toURI(url))
				.headers("Content-Type", contentType)
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.build();

		return sendRequest(request);
	}

	private HttpResponse<String> sendRequest(HttpRequest request) {
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			int statusCode = response.statusCode();

			if (statusCode >= 200 && statusCode <= 299) {
				return response;
			} else if (response.statusCode() >= 400 && statusCode <= 499) {
				throw new RuntimeException("HttpClient exception, request failed with statusCode: " + statusCode
						+ ", response: " + response.body());
			} else if (response.statusCode() >= 500) {
				throw new RuntimeException("HttpServer exception, request failed with statusCode: " + statusCode
						+ ", response: " + response.body());
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return null;
	}

	private URI toURI(String url) {
		try {
			return new URI(url);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}