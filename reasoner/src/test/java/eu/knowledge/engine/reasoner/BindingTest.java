package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import eu.knowledge.engine.reasoner.api.Binding;

public class BindingTest {
  @Test
  public void testBindingEqual() {
    Binding a = new Binding();
    Binding b = new Binding();

    assertEquals(a, b);
    
    a.put("x", "<some>");
    a.put("y", "<some>");
    b.put("x", "<some>");
    b.put("y", "<some>");

    assertEquals(a, b);
  }
}
