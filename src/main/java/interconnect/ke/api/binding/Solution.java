package interconnect.ke.api.binding;

import java.util.HashSet;

/**
 * A set of bindings. Note that although this is called a 'solution' it could
 * also be a partial solution. I.e. it does not contain a value for every
 * variable in a graph pattern.
 * 
 * @author nouwtb
 *
 */
public class Solution extends HashSet<Binding> {

	private static final long serialVersionUID = 1L;

}
