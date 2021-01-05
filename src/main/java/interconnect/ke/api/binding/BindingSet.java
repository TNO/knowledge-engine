package interconnect.ke.api.binding;

import java.util.HashSet;

/**
 * A set of bindings. Note that the multiple bindings in this set have an 'or' relation (instead of an 'and' relation) with
 * each other.
 */
public class BindingSet extends HashSet<Binding> {
	private static final long serialVersionUID = 1L;
}
