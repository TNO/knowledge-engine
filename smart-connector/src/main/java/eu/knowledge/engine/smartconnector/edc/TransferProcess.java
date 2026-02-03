package eu.knowledge.engine.smartconnector.edc;

public record TransferProcess(String participantId, String counterPartyParticipantId, String contractAgreementId,
        String counterPartyDataPlaneUrl, String authToken) {

}