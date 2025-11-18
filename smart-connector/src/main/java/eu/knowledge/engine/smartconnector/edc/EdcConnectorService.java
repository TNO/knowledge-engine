package eu.knowledge.engine.smartconnector.edc;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private EdcConnectorProperties properties;
	private Map<String, ParticipantProperties> participants = new HashMap<>();

	// these are all static for every connector
	public final static String DATA_PLANE_ID = "tke-dataplane";

	@Inject
	public EdcConnectorService(EdcConnectorProperties configuration) {
		LOG.info("Adding connector for [participant id: {}] with [management url: {}]",
				configuration.participantId(), configuration.managementUrl());
		this.edcClient = new EdcConnectorClient(configuration.managementUrl());
		this.properties = configuration;
		this.participants.put(configuration.participantId(),
				new ParticipantProperties(configuration.participantId(), configuration.protocolUrl()));
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
		LOG.info("configuring connector of {}", this.properties.participantId());
		// String existingAssetId =
		// getAssetIdFromCatalogForAssetName(properties.participantId(),
		// properties.participantId());
		// if (existingAssetId != null) {
		// LOG.info("Connector already configured and TKE asset present for
		// participantId: {}", participantId);
		// throw new RuntimeException("Connector already configured and TKE asset
		// present.");
		// }

		// properties needed when creating a data plane.
		var dataPlaneId = EdcConnectorService.DATA_PLANE_ID;
		var dataPlaneControlUrl = this.properties.dataPlaneControlUrl();
		var dataPlanePublicUrl = this.properties.dataPlanePublicUrl();
		// properties needed when creating an asset.
		var assetId = UUID.randomUUID().toString(); // generate an unique assetId
		var tkeAssetUrl = this.properties.tkeAssetUrl();
		var tkeAssetName = this.properties.tkeAssetName();
		// properties needed when creating a policy and contract definition.
		var policyId = UUID.randomUUID().toString();
		var contractId = UUID.randomUUID().toString();

		// Create the mandatory edc resources.
		var map = new HashMap<String, String>();
		// map.put("registerDataPlane", connector.registerDataPlane(dataPlaneId,
		// dataPlaneControlUrl, dataPlanePublicUrl));
		LOG.info("Registering KER API asset");
		map.put("registerAsset", this.edcClient.registerAsset(assetId, tkeAssetUrl, tkeAssetName));
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
	public String negotiateContract(String counterPartyParticipantId, String assetId) {
		LOG.info("negotiateContract for participantId: {}, counterPartyParticipantId: {}, assetId: {}", this.properties.participantId(),
				counterPartyParticipantId, assetId);
		ParticipantProperties counterParty = participants.get(counterPartyParticipantId);

		// note that the counterparty protocol url could also be extract from the
		// catalog request
		var counterPartyAddress = counterParty.protocolUrl(); // dsp protocol address of the
																		// counterparty/provider
		var consumerId = this.properties.participantId();
		var providerId = counterParty.participantId();

		// The consumer will negotiate a contract using its own connector, the
		// counterPartyAddress
		// is the party which we need to negotiate the contract with.
		String negotiateContract = this.edcClient.negotiateContract(consumerId, providerId, counterPartyAddress, assetId);
		return this.edcClient.contractAgreement(negotiateContract);
	}

	public String transferProcess(String counterPartyParticipantId, String contractAgreementId) {
		LOG.info(
				"transferProcess for participantId: {}, counterPartyParticipantId: {}, contractAgreementId: {}",
				this.properties.participantId(), counterPartyParticipantId, contractAgreementId);
		ParticipantProperties counterParty = participants.get(counterPartyParticipantId);

		var counterPartyAddress = counterParty.protocolUrl(); // dsp protocol address of the

		return this.edcClient.transferProcess(counterPartyAddress, contractAgreementId);
	}
}