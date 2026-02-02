package eu.knowledge.engine.smartconnector.edc;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.SmartConnectorConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;

import static eu.knowledge.engine.smartconnector.edc.JsonUtil.findByJsonPointerExpression;

/**
 * The EdcConnectorService can manage all the configuration of and
 * interactions between the specified EDC connectors in the
 * TkeEdcConnectorConfiguration.
 */
public class EdcConnectorService {

	private final Logger LOG = LoggerFactory.getLogger(EdcConnectorService.class);

	private final HttpClient httpClient;
	private Map<URI, ParticipantProperties> participants = new HashMap<>();

	private final URI managementUrl; 
	private final ParticipantProperties myProperties;
	
	// KER properties
	private URI assetUrl; 
	private String assetName = "TNO Knowledge Engine Runtime API";

	public EdcConnectorService(URI assetUrl) {
		Config config = ConfigProvider.getConfig();
		
		this.managementUrl = toURI(config.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_EDC_MANAGEMENT_URL).getValue());
		URI configParticipantId = toURI(config.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_EDC_PARTICIPANT_ID).getValue());
		URI configProtocolUrl = toURI(config.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_EDC_PROTOCOL_URL).getValue());
		URI configDataPlanePublicUrl = toURI(config.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_EDC_DATAPLANE_PUBLIC_URL).getValue());
		this.myProperties = new ParticipantProperties(configParticipantId, configProtocolUrl, configDataPlanePublicUrl); 
		this.assetUrl = assetUrl;

		LOG.info("Adding connector for [participant id: {}] with [management url: {}]",
				this.myProperties.participantId(), this.managementUrl);
		var executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		this.httpClient = HttpClient.newBuilder().executor(executorService).build();
		this.participants.put(this.myProperties.participantId(),
				new ParticipantProperties(this.myProperties.participantId(), this.myProperties.protocolUrl(), this.myProperties.dataPlanePublicUrl()));
	}

	public void registerParticipant(ParticipantProperties participant) {
		LOG.info("Registering EDC participant with participant id {}", participant.participantId());
		participants.put(participant.participantId(), participant);
	}

	/**
	 * Configure the connector.
	 * @return map with all the responses
	 */
	public void configureConnector() {
		String assetId = UUID.randomUUID().toString(); 
		String policyId = UUID.randomUUID().toString();
		String contractId = UUID.randomUUID().toString();

		LOG.info("Registering KER API asset");
		this.registerAsset(assetId, this.assetUrl, this.assetName);
		LOG.info("Registering Policy");
		this.registerPolicy(policyId);
		LOG.info("Registering Contract Definition");
		this.registerContractDefinition(contractId, policyId, policyId, assetId);
	}

	
	/**
	 * Create and start a transfer process with another participant by
	 * requesting their catalog, setting up a contract agreement and starting
	 * the actual transfer process.
	 * 
	 * @param counterPartyParticipantId	the participant ID of the counter party
	 * 
	 * @return the started transfer process
	 */
	public TransferProcess createTransferProcess(URI counterPartyParticipantId) {
		ParticipantProperties counterParty = participants.get(counterPartyParticipantId);
		String catalogJson = catalogRequest(counterParty);
		String assetId = findByJsonPointerExpression(catalogJson, "/dcat:dataset/@id");
		String policyId = findByJsonPointerExpression(catalogJson, "/dcat:dataset/odrl:hasPolicy/@id");
		String contractAgreementId = negotiateContract(counterParty, assetId, policyId);

		String transferId = transferProcess(counterParty, contractAgreementId);
		String edrsJson = getEndpointDataReference(transferId);

		String authToken = findByJsonPointerExpression(edrsJson, "/authorization");
		String counterPartyDataPlaneUrl = findByJsonPointerExpression(edrsJson, "/endpoint");
		LOG.info("EDC Data Transfer with Remote KER {} started with Contract Agreement Id: {} and Transfer Id: {}",
				counterPartyParticipantId, contractAgreementId, transferId);
		return new TransferProcess(this.myProperties.participantId(), counterPartyParticipantId, contractAgreementId, counterPartyDataPlaneUrl, authToken);
	}


	/**
	 * Negotiate a contract between two connectors for the provided asset
	 * identifier.
	 *
	 * @param counterParty	participant to whom the request should be made
	 * @param assetId       describes what asset the participant wants to use
	 * @param policyId		describes the policy assigned to the counterparty's asset
	 *  
	 * @return contract agreement ID 
	 */
	private String negotiateContract(ParticipantProperties counterParty, String assetId, String policyId) {
		// Send contract agreement request
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
				""".formatted(counterParty.protocolUrl(), policyId, counterParty.participantId(), assetId);

		LOG.debug("Negotiate contract at: {}, Request: {}", url, payload);
		HttpResponse<String> postResponse = httpPost(url, payload);

		LOG.debug("Negotiate contract response: {}", postResponse.body());

		String contractRequestId = findByJsonPointerExpression(postResponse.body(), "/@id");
		
		// Poll for contract agreement finalization and return 
		String url_with_id = getManagementUrl("/v3/contractnegotiations/" + contractRequestId);
		final List<String> responses = new ArrayList<>();
		Awaitility.await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10)).until(() -> {
			HttpResponse<String> response = httpGet(url_with_id);
			responses.add(response.body());
			String state = findByJsonPointerExpression(response.body(), "/state");
			LOG.error(state);
			return Objects.equals(state, "FINALIZED");
		});
		
		return findByJsonPointerExpression(responses.get(responses.size()-1), "/contractAgreementId");
	}

	private String registerAsset(String assetId, URI tkeUrl, String tkeAssetName) {
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
		LOG.debug("Registering asset at: {}, Request: {}", url, payload);
		HttpResponse<String> response = httpPost(url, payload);
		LOG.debug("Registering asset response: {}", response.body());
		return response.body();
	}

	private String registerPolicy(String policyId) {
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
		LOG.debug("Registering policy at: {} Request: {}", url, payload);
		HttpResponse<String> response = httpPost(url, payload);
		LOG.debug("Registering policy response: {}", response.body());
		return response.body();
	}

	private String registerContractDefinition(String contractDefinitionId, String accessPolicyId,
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
		LOG.debug("Registering contract definition at: {} Request: {}", url, payload);
		HttpResponse<String> response = httpPost(url, payload);
		LOG.debug("Registering contract definition response: {}", response.body());
		return response.body();
	}

	/**
	 * Catalog request is always done from one connector (your own) to another
	 * party's connector (counterparty). Using a catalog request one can figure out
	 * what assets are provided by a connector. Asset identifiers can later be used
	 * to negotiate contracts between parties.
	 *
	 * @param counterPartyParticipantId to whom the request should be make
	 * @return response
	 */
	private String catalogRequest(ParticipantProperties counterParty) {
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
				""".formatted(counterParty.protocolUrl(), counterParty.participantId());
		LOG.debug("Requesting catalog at: {}, Request: {}", url, payload);
		HttpResponse<String> postResponse = httpPost(url, payload);
		LOG.debug("Requesting Catalog response: {}", postResponse.body());
		return postResponse.body();
	}
	

	private String transferProcess(ParticipantProperties counterParty, String contractAgreementId) {
		LOG.info("transferProcess for participantId: {}, counterPartyParticipantId: {}, contractAgreementId: {}",
				this.myProperties.participantId(), counterParty.participantId(), contractAgreementId);

		String url = getManagementUrl("/v3/transferprocesses");
		String payload = """
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
		""".formatted(counterParty.protocolUrl(), contractAgreementId);
		LOG.debug("Start transfer process at: {}, Request: {}", url, payload);
		HttpResponse<String> postResponse = httpPost(url, payload);
		LOG.debug("Start transfer process response: {}", postResponse.body());
		
		String transferId = findByJsonPointerExpression(postResponse.body(), "/@id");

		String url_with_id = getManagementUrl("/v3/transferprocesses/" + transferId);
		final List<String> responses = new ArrayList<>();

		Awaitility.await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10)).until(() -> {
			HttpResponse<String> response = httpGet(url_with_id);
			responses.add(response.body());
			String state = findByJsonPointerExpression(response.body(), "/state");
			LOG.error(state);
			return Objects.equals(state, "STARTED");
		});

		return transferId;
	}

	private String getEndpointDataReference(String transferId) {
		String url = getManagementUrl("/v3/edrs/" + transferId + "/dataaddress");
		LOG.debug("Get endpoint data reference");
		HttpResponse<String> response = httpGet(url);
		return response.body();
	}

	/**
	 * Contract the EDC connector URL for the /management endpoint.
	 */
	private String getManagementUrl(String suffix) {
		return this.managementUrl + suffix;
	}

	private HttpResponse<String> httpGet(String url) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(toURI(url))
				.headers("Accept", "application/json")
				.GET()
				.build();

		return sendRequest(request);
	}

	private HttpResponse<String> httpPost(String url, String payload) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(toURI(url))
				.headers("Content-Type", "application/json")
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

	public ParticipantProperties getMyProperties() {
		return this.myProperties;
	}
}