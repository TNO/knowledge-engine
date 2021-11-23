package eu.knowledge.engine.reasoner.api;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.sparql.sse.SSE;

import eu.knowledge.engine.reasoner.api.Binding;

public class Binding extends HashMap<Node_Variable, Node> {

	private static final long serialVersionUID = 2381462612239850018L;

	public Binding() {
		super();
	}

	public Binding(Node_Variable variable, Node lit) {
		super();
		this.put(variable, lit);
	}

	public Binding(String variable, String val) {
		this(new Node_Variable(variable), SSE.parseNode(val));
	}

	public Binding(Binding b) {
		super(b);
	}

	public boolean containsKey(String variable) {
		return this.containsKey(SSE.parseNode(variable));
	}

	public Node get(String variable) {
		return this.get((Node_Variable) SSE.parseNode(variable));
	}

	public Node put(String variable, String val) {
		return this.put(new Node_Variable(variable), SSE.parseNode(val));
	}

	public Map<String, String> toMap() {

		Map<String, String> result = new HashMap<String, String>();
		for (Map.Entry<Node_Variable, Node> entry : this.entrySet()) {
			result.put(entry.getKey().toString(), entry.getValue().toString());
		}

		return result;
	}

	public void putMap(Map<String, String> map) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			this.put(entry.getKey(), entry.getValue());
		}

	}

}
