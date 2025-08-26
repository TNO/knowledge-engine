package eu.knowledge.engine.smartconnector.edc;

import static nl.tno.tke.edc.TkeEdcJsonUtil.findByJsonPointerExpression;

public record Token(String tokenJson, String id, String contractId, String authKey, String authCode, String endpoint) {

  public Token(String tokenJson) {
    this(
      tokenJson,
      findByJsonPointerExpression(tokenJson, "/id"),
      findByJsonPointerExpression(tokenJson, "/contractId"),
      findByJsonPointerExpression(tokenJson, "/authKey"),
      findByJsonPointerExpression(tokenJson, "/authCode"),
      findByJsonPointerExpression(tokenJson, "/endpoint")
    );
  }
}