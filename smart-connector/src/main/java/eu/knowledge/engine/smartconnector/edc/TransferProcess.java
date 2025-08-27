package eu.knowledge.engine.smartconnector.edc;

import static eu.knowledge.engine.smartconnector.edc.JsonUtil.findByJsonPointerExpression;

public record TransferProcess(String participantId, String counterPartyParticipantId, String contractAgreementId, String responseJson, String transferProcessResponseId) {

  public TransferProcess(String participantId, String counterPartyParticipantId, String contractAgreementId, String responseJson) {
    this(
      participantId,
      counterPartyParticipantId,
      contractAgreementId,
      contractAgreementId,
      findByJsonPointerExpression(responseJson, "/@id")
    );
  }
}