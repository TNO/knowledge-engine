package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

public class AltNode {

	public AltNode(Set<Triple> aGoal) {
		this.goal = aGoal;
	}

	/**
	 * Empty list if no more goals.
	 */
	private Set<Triple> goal;
	private Map<Match, AltNode> children;

}
