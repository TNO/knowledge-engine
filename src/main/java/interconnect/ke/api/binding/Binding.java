package interconnect.ke.api.binding;

import java.util.Map;
import java.util.HashMap;

/**
 * A binding of a set of variables. Note that not all variables in the graph
 * pattern have to be bound. Even though it is logically a java.util.Map, we
 * wrap it because we want to validate the values.
 */
public class Binding {
	// TODO: Implement/extend (Hash)Map<String, String> for easier iteration?

	private final Map<String, String> map = new HashMap<>();

	public Binding() {
	}

	public void put(String variableName, String value) throws IllegalArgumentException {
		this.validate(value);
		this.map.put(variableName, value);
	}

	public boolean containsKey(String aVariableName) {
		return this.map.containsKey(aVariableName);
	}

	public String get(String variableName) {
		return this.map.get(variableName);
	}

	private void validate(String value) throws IllegalArgumentException {
		// TODO
	}
}
