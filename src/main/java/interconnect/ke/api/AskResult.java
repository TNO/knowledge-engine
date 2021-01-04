package interconnect.ke.api;

import interconnect.ke.api.binding.BindingSet;

/**
 * An AskResult contains the result of the ask knowledge interaction, of course
 * including the bindings, but (in the future) also information on how the
 * result is formed (which knowledge bases contributed etc.)
 */
public class AskResult {
  private final BindingSet bindings;

  public AskResult(BindingSet someBindings) {
    this.bindings = someBindings;
  }

  public BindingSet getBindings() {
    return this.bindings;
  }
}
