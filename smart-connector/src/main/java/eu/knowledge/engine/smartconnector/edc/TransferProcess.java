package eu.knowledge.engine.smartconnector.edc;

import java.net.URI;

public record TransferProcess(URI participantId, URI counterPartyParticipantId, String contractAgreementId,
        String counterPartyDataPlaneUrl, String authToken) {

}