package eu.knowledge.engine.smartconnector.edc;

public record EdcConnectorProperties(String participantId, String protocolUrl, String managementUrl, String dataPlaneId,
        String dataPlaneControlUrl, String dataPlanePublicUrl, String tokenValidationEndpoint, String tkeAssetUrl, String tkeAssetName) {

    public EdcConnectorProperties(String participantId, String protocolUrl) {
        this(
            participantId, 
            protocolUrl, 
            "", 
            "tke-dataplane", 
            "", 
            "",
            "",
            "https://www.knowledge-engine.eu/", 
            "TNO Knowledge Engine Runtime"
        );
    }
}