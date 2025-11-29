package eu.knowledge.engine.smartconnector.edc;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.SmartConnectorConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The EdcConnectorService can manage all the configuration of and
 * interactions between the specified EDC connectors in the
 * TkeEdcConnectorConfiguration.
 */
@Named
public class EdcConnectorService {

	private final Logger LOG = LoggerFactory.getLogger(EdcConnectorService.class);

	private EdcConnectorClient edcClient;
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

	// these are all static for every connector
	public final static String DATA_PLANE_ID = "tke-dataplane";

	@Inject
	public EdcConnectorService(URI assetUrl) {
		loadConfig(assetUrl);
		LOG.info("Adding connector for [participant id: {}] with [management url: {}]",
				this.participantId, this.managementUrl);
		this.edcClient = new EdcConnectorClient(this.managementUrl.toString());
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
	public HashMap<String, String> configureConnector() {
		LOG.info("configuring connector of {}", this.participantId);
		// String existingAssetId =
		// getAssetIdFromCatalogForAssetName(participantId(),
		// participantId());
		// if (existingAssetId != null) {
		// LOG.info("Connector already configured and TKE asset present for
		// participantId: {}", participantId);
		// throw new RuntimeException("Connector already configured and TKE asset
		// present.");
		// }

		// properties needed when creating a data plane.
		var dataPlaneId = EdcConnectorService.DATA_PLANE_ID;
		var dataPlaneControlUrl = this.dataPlaneControlUrl;
		var dataPlanePublicUrl = this.dataPlanePublicUrl;
		// properties needed when creating an asset.
		var assetId = UUID.randomUUID().toString(); // generate an unique assetId
		var tkeAssetUrl = this.tkeAssetUrl;
		var tkeAssetName = this.tkeAssetName;
		// properties needed when creating a policy and contract definition.
		var policyId = UUID.randomUUID().toString();
		var contractId = UUID.randomUUID().toString();

		// Create the mandatory edc resources.
		var map = new HashMap<String, String>();
		// map.put("registerDataPlane", connector.registerDataPlane(dataPlaneId,
		// dataPlaneControlUrl, dataPlanePublicUrl));
		LOG.info("Registering KER API asset");
		map.put("registerAsset", this.edcClient.registerAsset(assetId, tkeAssetUrl.toString(), tkeAssetName));
		LOG.info("Registering Policy");
		map.put("registerPolicy", this.edcClient.registerPolicy(policyId));
		LOG.info("Registering Contract Definition");
		map.put("registerContractDefinition",
				this.edcClient.registerContractDefinition(contractId, policyId, policyId, assetId));
		return map;
	}

	public String getAssetIdFromCatalogForAssetName(String counterPartyParticipantId) {
		String response = catalogRequest(counterPartyParticipantId);
		return JsonUtil.findByJsonPointerExpression(response, "/dcat:dataset/@id");
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
		LOG.info("participant: {}", counterParty);
		LOG.info("participant_id: {}", counterParty.participantId());
		return this.edcClient.catalogRequest(counterParty.protocolUrl(), counterPartyParticipantId);
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

		// note that the counterparty protocol url could also be extract from the
		// catalog request
		var counterPartyAddress = counterParty.protocolUrl(); // dsp protocol address of the
																		// counterparty/provider
		var consumerId = this.participantId;
		var providerId = counterParty.participantId();

		// The consumer will negotiate a contract using its own connector, the
		// counterPartyAddress
		// is the party which we need to negotiate the contract with.
		String negotiateContract = this.edcClient.negotiateContract(providerId, counterPartyAddress, policyId, assetId);
		return this.edcClient.contractAgreement(negotiateContract);
	}

	public String transferProcess(String counterPartyParticipantId, String contractAgreementId) {
		LOG.info(
				"transferProcess for participantId: {}, counterPartyParticipantId: {}, contractAgreementId: {}",
				this.participantId, counterPartyParticipantId, contractAgreementId);
		ParticipantProperties counterParty = participants.get(counterPartyParticipantId);

		var counterPartyAddress = counterParty.protocolUrl(); // dsp protocol address of the

		return this.edcClient.transferProcess(counterPartyAddress, contractAgreementId);
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

	public String getTransferProcessStatus(String transferId) {
		return this.edcClient.getTransferProcessStatus(transferId);
	}

	public String getEndpointDataReference(String transferId) {
		return this.edcClient.getEndpointDataReference(transferId);
	}
}