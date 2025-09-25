package eu.knowledge.engine.smartconnector.edc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {

  private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  public static String findByJsonPointerExpression(String json, String jsonPointerExpression) {
    try {
      if (json == null) return null;
      JsonNode jsonNode = objectMapper.readTree(json);
      JsonNode id = jsonNode.at(jsonPointerExpression);

      if (id == null) return null;
      return id.textValue();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
