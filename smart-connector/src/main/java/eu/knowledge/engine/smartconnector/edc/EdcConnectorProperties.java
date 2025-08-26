package eu.knowledge.engine.smartconnector.edc;

public record EdcConnectorProperties(String participantId, String protocolUrl, String managementUrl, String dataPlaneId,
        String dataPlaneControlUrl, String dataPlanePublicUrl, String tkeAssetUrl, String tkeAssetName) {
}
