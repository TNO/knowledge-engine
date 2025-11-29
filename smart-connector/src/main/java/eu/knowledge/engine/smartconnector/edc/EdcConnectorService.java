package eu.knowledge.engine.smartconnector.edc;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.SmartConnectorConfig;

import java.net.URI;
import java.net.URISyntaxException;
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
@Named
public class EdcConnectorService {

	private final Logger LOG = LoggerFactory.getLogger(EdcConnectorService.class);

	private final HttpClient httpClient;
	private Map<String, ParticipantProperties> participants = new HashMap<>();

	// EDC connectors properties
	// Control plane:
	private URI participantId; 
	private URI protocolUrl; 
	private URI managementUrl;
	// Data plane: 
	private URI dataPlaneId;
	private URI dataPlaneControlUrl; 
	private URI dataPlanePublicUrl; 
	private URI tokenValidationEndpoint; 
	
	// KER properties
	private URI tkeAssetUrl; 
	private String tkeAssetName = "TNO Knowledge Engine Runtime API";

	@Inject
	public EdcConnectorService(URI assetUrl) {
		loadConfig(assetUrl);
		LOG.info("Adding connector for [participant id: {}] with [management url: {}]",
				this.participantId, this.managementUrl);
		var executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		this.httpClient = HttpClient.newBuilder().executor(executorService).build();
		this.participants.put(this.participantId.toString(),
				new ParticipantProperties(this.participantId.toString(), this.protocolUrl.toString(), this.dataPlanePublicUrl.toString()));
	}
	
	/**
	 * @return A configuration object with properties for the two connectors.
	 */
	private void loadConfig(URI assetUrl) {
		Config config = ConfigProvider.getConfig();
		try {
			this.participantId = new URI(config.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_EDC_PARTICIPANT_ID).getValue());
			this.protocolUrl = new URI(config.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_EDC_PROTOCOL_URL).getValue());
			this.managementUrl = new URI(config.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_EDC_MANAGEMENT_URL).getValue());
			this.dataPlaneControlUrl = new URI(config.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_EDC_DATAPLANE_CONTROL_URL).getValue());
			this.dataPlanePublicUrl = new URI(config.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_EDC_DATAPLANE_PUBLIC_URL).getValue());
			this.tokenValidationEndpoint = new URI(config.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_EDC_TOKEN_VALIDATION_ENDPOINT).getValue());
		} catch (URISyntaxException e) {
			// TODO: handle this error!
			LOG.error("Invalid URI syntax, see: ".formatted(e.getMessage()));
		}
			this.tkeAssetUrl = assetUrl;
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
		LOG.info("configuring connector of {}", this.participantId);
		String assetId = UUID.randomUUID().toString(); 
		String policyId = UUID.randomUUID().toString();
		String contractId = UUID.randomUUID().toString();

		LOG.info("Registering KER API asset");
		this.registerAsset(assetId, this.tkeAssetUrl.toString(), this.tkeAssetName);
		LOG.info("Registering Policy");
		this.registerPolicy(policyId);
		LOG.info("Registering Contract Definition");
		this.registerContractDefinition(contractId, policyId, policyId, assetId);
	}

	
	/**
	 * Make sure we have a valid authToken to communicate with the remote KER. This
	 * involves fetching their catalog, negotiating a contract, starting a transfer
	 * process and receiving the token.
	 */
	public TransferProcess createTransferProcess(String counterPartyParticipantId) {
		String catalogJson = catalogRequest(counterPartyParticipantId);
		String assetId = findByJsonPointerExpression(catalogJson, "/dcat:dataset/@id");
		String policyId = findByJsonPointerExpression(catalogJson, "/dcat:dataset/odrl:hasPolicy/@id");
		String contractAgreementJson = negotiateContract(counterPartyParticipantId, assetId, policyId);

		String contractAgreementId = findByJsonPointerExpression(contractAgreementJson, "/contractAgreementId");

		String transferJson = transferProcess(counterPartyParticipantId, contractAgreementId);
		String transferId = findByJsonPointerExpression(transferJson, "/@id");
		String _statusJson = getTransferProcessStatus(transferId);
		String edrsJson = getEndpointDataReference(transferId);

		String authToken = findByJsonPointerExpression(edrsJson, "/authorization");
		String counterPartyDataPlaneUrl = findByJsonPointerExpression(edrsJson, "/endpoint");
		LOG.info("EDC Data Transfer with Remote KER {} started with Contract Agreement Id: {} and Transfer Id: {}",
				counterPartyParticipantId, contractAgreementId, transferId);
		return new TransferProcess(this.participantId.toString(), counterPartyParticipantId, contractAgreementId, counterPartyDataPlaneUrl, authToken);
	}


	/**
	 * Negotiate a contract between two connectors for the provided asset
	 * identifier.
	 *
	 * @param counterPartyParticipantId to whom the request should be make
	 * @param assetId                   determines what asset the participant wants
	 *                                  to use
	 * @return response
	 */
	public String negotiateContract(String counterPartyParticipantId, String assetId, String policyId) {
		LOG.info("negotiateContract for participantId: {}, counterPartyParticipantId: {}, assetId: {}", this.participantId,
				counterPartyParticipantId, assetId);
		ParticipantProperties counterParty = participants.get(counterPartyParticipantId);
		String negotiateContract = this.negotiateContract(counterParty.participantId(), counterParty.protocolUrl(), policyId, assetId);
		return this.contractAgreement(negotiateContract);
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
	 * Catalog request is always done from one connector (your own) to another
	 * party's connector (counterparty). Using a catalog request one can figure out
	 * what assets are provided by a connector. Asset identifiers can later be used
	 * to negotiate contracts between parties.
	 *
	 * @param counterPartyParticipantId to whom the request should be make
	 * @return response
	 */
	public String catalogRequest(String counterPartyParticipantId) {
		LOG.info("Catalog request: {}", counterPartyParticipantId);
		ParticipantProperties counterParty = participants.get(counterPartyParticipantId);
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
				""".formatted(counterParty.protocolUrl(), counterPartyParticipantId);
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
			LOG.error(state);
			return Objects.equals(state, "FINALIZED");
		});

		return responses.get(responses.size() - 1);
	}
	
	public String transferProcess(String counterPartyParticipantId, String contractAgreementId) {
		LOG.info("transferProcess for participantId: {}, counterPartyParticipantId: {}, contractAgreementId: {}",
				this.participantId, counterPartyParticipantId, contractAgreementId);
		ParticipantProperties counterParty = participants.get(counterPartyParticipantId);

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
		""".formatted(counterParty.protocolUrl(), contractAgreementId);
		LOG.info("Start transfer process at: {}, Request: {}", url, payload);
		HttpResponse<String> postResponse = httpPost(url, payload);
		LOG.info("Start transfer process response: {}", postResponse.body());
		return postResponse.body();
	}

	public String getTransferProcessStatus(String transferId) {
		String url = getManagementUrl("/v3/transferprocesses/" + transferId);
		final List<String> responses = new ArrayList<>();

		Awaitility.await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10)).until(() -> {
			HttpResponse<String> response = httpGet(url);
			responses.add(response.body());
			String state = findByJsonPointerExpression(response.body(), "/state");
			LOG.error(state);
			return Objects.equals(state, "STARTED");
		});

		return responses.get(responses.size() - 1);
	}

	public String getEndpointDataReference(String transferId) {
		String url = getManagementUrl("/v3/edrs/" + transferId + "/dataaddress");
		LOG.info("Get endpoint data reference");
		HttpResponse<String> response = httpGet(url);
		LOG.info(response.body());
		return response.body();
	}

	/**
	 * Contract the EDC connector URL for the /management endpoint.
	 */
	private String getManagementUrl(String suffix) {
		return this.managementUrl + suffix;
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


	public URI getParticipantId() {
		return this.participantId;
	}

	public URI getControlPlaneProtocolUrl() {
		return this.protocolUrl;
	}

	public URI getDataPlanePublicUrl() {
		return this.dataPlanePublicUrl;
	}
}