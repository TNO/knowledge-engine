package eu.knowledge.engine.smartconnector.edc;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
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
	private final Map<String, EdcConnectorClient> connectors = new HashMap<>();
	private final Map<String, EdcConnectorProperties> configuration = new HashMap<>();

	// these are all static for every connector
	public final static String DATA_PLANE_ID = "tke-dataplane";
	public final static String ASSET_NAME = "TNO Knowledge Engine Runtime";
	public final static String ASSET_URL = "https://www.knowledge-engine.eu/";

	@Inject
	public EdcConnectorService(List<EdcConnectorProperties> configuration) {
		for (EdcConnectorProperties connector : configuration) {
			addConnector(connector);
		}
	}

	public void addConnector(EdcConnectorProperties connector) {
		LOG.info("Adding connector for [participant id: {}] with [management url: {}]",
				connector.participantId(), connector.managementUrl());
		connectors.put(connector.participantId(), new EdcConnectorClient(connector.managementUrl()));
		configuration.put(connector.participantId(), connector);
	}

	/**
	 * Configure the connector with the provided connector's participantId
	 *
	 * @param participantId connector to configure
	 * @return map with all the responses
	 */
	public HashMap<String, String> configureConnector(String participantId) {
		LOG.info("configureConnector for participantId: {}", participantId);
		EdcConnectorProperties properties = configuration.get(participantId);
		EdcConnectorClient connector = connectors.get(participantId);

		// String existingAssetId = getAssetIdFromCatalogForAssetName(properties.participantId(),
		// 		properties.participantId(), properties.tkeAssetName());
		// if (existingAssetId != null) {
		// 	LOG.info("Connector already configured and TKE asset present for participantId: {}", participantId);
		// 	throw new RuntimeException("Connector already configured and TKE asset present.");
		// }

		// properties needed when creating a data plane.
		var dataPlaneId = EdcConnectorService.DATA_PLANE_ID;
		var dataPlaneControlUrl = properties.dataPlaneControlUrl();
		var dataPlanePublicUrl = properties.dataPlanePublicUrl();
		// properties needed when creating an asset.
		var assetId = UUID.randomUUID().toString(); // generate an unique assetId
		var tkeAssetUrl = EdcConnectorService.ASSET_URL;
		var tkeAssetName = EdcConnectorService.ASSET_NAME;
		// properties needed when creating a policy and contract definition.
		var policyId = UUID.randomUUID().toString();
		var contractId = UUID.randomUUID().toString();

		// Create the mandatory edc resources.
		var map = new HashMap<String, String>();
		// map.put("registerDataPlane", connector.registerDataPlane(dataPlaneId, dataPlaneControlUrl, dataPlanePublicUrl));
		LOG.info("Registering Asset");
		map.put("registerAsset", connector.registerAsset(assetId, tkeAssetUrl, tkeAssetName));
		LOG.info("Registering Policy");
		map.put("registerPolicy", connector.registerPolicy(policyId));
		LOG.info("Registering Contract Definition");
		map.put("registerContractDefinition", connector.registerContractDefinition(contractId, policyId, policyId, assetId));
		return map;
	}

	public String getAssetIdFromCatalogForAssetName(String participantId, String counterPartyParticipantId,
			String assetName) {
		String response = catalogRequest(participantId, counterPartyParticipantId);
		return JsonUtil.findByJsonPointerExpression(response, "/dcat:dataset/@id");
	}

	/**
	 * Catalog request is always done from one connector (your own) to another
	 * party's connector (counterparty). Using a catalog request one can figure out
	 * what assets are provided by a connector. Asset identifiers can later be used
	 * to negotiate contracts between parties.
	 *
	 * @param participantId             from which connector the request should be
	 *                                  made
	 * @param counterPartyParticipantId to whom the request should be make
	 * @return response
	 */
	public String catalogRequest(String participantId, String counterPartyParticipantId) {
		LOG.info("catalogRequest for participantId: {}", participantId);
		EdcConnectorClient connector = connectors.get(participantId);
		EdcConnectorProperties counterPartyProperties = configuration.get(
				counterPartyParticipantId);

		var counterPartyProtocolUrl = counterPartyProperties.protocolUrl();
		return connector.catalogRequest(counterPartyProtocolUrl, counterPartyParticipantId);
	}

	/**
	 * Negotiate a contract between two connectors for the provided asset
	 * identifier.
	 *
	 * @param participantId             from which connector the request should be
	 *                                  made
	 * @param counterPartyParticipantId to whom the request should be make
	 * @param assetId                   determines what asset the participant wants
	 *                                  to use
	 * @return response
	 */
	public String negotiateContract(String participantId, String counterPartyParticipantId, String assetId) {
		LOG.info("negotiateContract for participantId: {}, counterPartyParticipantId: {}, assetId: {}", participantId,
				counterPartyParticipantId, assetId);
		EdcConnectorProperties participantProperties = configuration.get(participantId);
		EdcConnectorProperties counterPartyProperties = configuration.get(counterPartyParticipantId);
		EdcConnectorClient connector = connectors.get(participantId);

		// note that the counterparty protocol url could also be extract from the
		// catalog request
		var counterPartyAddress = counterPartyProperties.protocolUrl(); // dsp protocol address of the
																		// counterparty/provider
		var consumerId = participantProperties.participantId();
		var providerId = counterPartyProperties.participantId();

		// The consumer will negotiate a contract using its own connector, the
		// counterPartyAddress
		// is the party which we need to negotiate the contract with.
		String negotiateContract = connector.negotiateContract(consumerId, providerId, counterPartyAddress, assetId);
		return connector.contractAgreement(negotiateContract);
	}

	public String transferProcess(String participantId, String counterPartyParticipantId, String contractAgreementId,
			String assetId) {
		LOG.info(
				"transferProcess for participantId: {}, counterPartyParticipantId: {}, contractAgreementId: {}, assetId: {}",
				participantId, counterPartyParticipantId, contractAgreementId, assetId);
		EdcConnectorProperties counterPartyProperties = configuration.get(counterPartyParticipantId);
		EdcConnectorClient connector = connectors.get(participantId);

		var counterPartyAddress = counterPartyProperties.protocolUrl(); // dsp protocol address of the
																		// counterparty/provider
		var providerId = counterPartyProperties.participantId();

		return connector.transferProcess(counterPartyAddress, providerId, contractAgreementId, assetId);
	}
}