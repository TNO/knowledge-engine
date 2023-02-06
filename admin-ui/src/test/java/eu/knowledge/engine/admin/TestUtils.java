package eu.knowledge.engine.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;

public class TestUtils {

  @Test
  public void testGetPatterns() {
    Model model = ModelFactory.createDefaultModel();
    var stream = TestUtils.class.getResourceAsStream("/testmodel.ttl");
    model.read(stream, "", "ttl");
    
    var reactKiRes = model.getResource("https://example.org/example-knowledge-base/interaction/thing-react");
    var arg = Util.getArgument(model, reactKiRes);
    var res = Util.getResult(model, reactKiRes);
    assertEquals("?a <http://example.org/becomesRelatedTo> ?b .", arg);
    assertEquals(null, res);
    
    var answerKiRes = model.getResource("https://example.org/example-knowledge-base/interaction/thing-answer");
    var pattern = Util.getGraphPattern(model, answerKiRes);
    assertEquals("?a <http://example.org/isRelatedTo> ?b .", pattern);
  }
}
