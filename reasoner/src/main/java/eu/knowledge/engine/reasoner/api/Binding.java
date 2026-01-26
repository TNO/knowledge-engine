package eu.knowledge.engine.reasoner.api;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.util.FmtUtils;

public class Binding extends HashMap<Var, Node> {

	private static final long serialVersionUID = 2381462612239850018L;

	public Binding() {
		super();
	}

	public Binding(QuerySolution qs) {
		Iterator<String> vars = qs.varNames();
		while (vars.hasNext()) {
			String var = vars.next();
			super.put(Var.alloc(var), qs.get(var).asNode());
		}
	}

	public Binding(Var variable, Node lit) {
		super();
		if (!lit.isConcrete())
			throw new IllegalArgumentException(
					"Binding values should be concrete nodes (either RDF literals or RDF URIs).");
		this.put(variable, lit);
	}

	public Binding(String variable, String val) {
		this(Var.alloc(variable), (Node) SSE.parseNode(val));
	}

	public Binding(Binding b) {
		super(b);
	}

	public Binding(Map<String, String> map) {
		this();
		this.putMap(map);
	}

	public boolean containsKey(String variable) {
		return this.containsKey(Var.alloc(variable));
	}

	public Node get(String variable) {
		return this.get(Var.alloc(variable));
	}

	public Node put(String variable, String val) {
		Node n = SSE.parseNode(val);
		if (!n.isConcrete())
			throw new IllegalArgumentException(
					"Binding values should be concrete nodes (either RDF literals or RDF URIs).");
		return this.put(Var.alloc(variable), n);
	}

	public Map<String, String> toMap() {

		Map<String, String> result = new HashMap<String, String>();
		for (Map.Entry<Var, Node> entry : this.entrySet()) {
			// TODO: Util function that does the stringForNode without prefixes
			result.put(entry.getKey().getName(), FmtUtils.stringForNode(entry.getValue(), new PrefixMappingZero()));
		}

		return result;
	}

	public void putMap(Map<String, String> map) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			this.put(entry.getKey(), entry.getValue());
		}

	}

	public boolean isSubBindingOf(Binding other) {
		if (!other.keySet().containsAll(this.keySet())) {
			return false;
		}

		return other.entrySet().stream().allMatch(b -> {
			Var variable = b.getKey();
			Node value = b.getValue();
			return !this.containsKey(variable) || value.equals(this.get(variable));
		});
	}

	public Binding keepOnly(Set<Var> variables) {
		var b = new Binding();
		for (var a : variables) {
			b.put(a, this.get(a));
		}
		return b;
	}

}
