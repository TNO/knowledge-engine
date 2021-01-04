package interconnect.ke.api;

import interconnect.ke.api.binding.BindingSet;

/**
 * A PostResult contains the result of the post knowledge interaction, of course
 * including the bindings, but (in the future) also information on how the
 * result is formed (which knowledge bases contributed etc.)
 */
public class PostResult {
  private final BindingSet bindings;

  public PostResult(BindingSet someBindings) {
    this.bindings = someBindings;
  }

  public BindingSet getBindings() {
    return this.bindings;
  }
}
