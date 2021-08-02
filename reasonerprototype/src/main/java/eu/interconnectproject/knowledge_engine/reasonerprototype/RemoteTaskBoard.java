package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.ki.KnowledgeInteraction;

public class RemoteTaskBoard {

	/** Tasks that are ready for execution */
	public Map<URI, List<RemoteRuleReasoningNode>> remoteTasks = new HashMap<>();
	private final KeReasoner keReasoner;

	public RemoteTaskBoard(KeReasoner keReasoner) {
		this.keReasoner = keReasoner;
	}

	public void addRemoteTask(RemoteRuleReasoningNode node) {
		if (!remoteTasks.containsKey(node.getKnowledgeInteractionId())) {
			remoteTasks.put(node.getKnowledgeInteractionId(), new ArrayList<>());
		}
		remoteTasks.get(node.getKnowledgeInteractionId()).add(node);
	}

	public boolean hasMoreTasks() {
		return !remoteTasks.isEmpty();
	}

	public void executeTasksForSingleKnowledgeInteraction() {
		assert hasMoreTasks();
		URI knowledgeInteractionId = remoteTasks.keySet().iterator().next();
		KnowledgeInteraction ki = keReasoner.getKnowledgeInteraction(knowledgeInteractionId);
		Map<BindingSet, BindingSet> requestCache = new HashMap<>();
		// TODO Come up with something more clever in order to avoid unnecessary
		// requests
		List<RemoteRuleReasoningNode> taskList = remoteTasks.get(knowledgeInteractionId);
		for (RemoteRuleReasoningNode node : taskList) {
			BindingSet request = node.getKnowledgeInteractionBinding();
			if (requestCache.containsKey(request)) {
				System.out.println(
						"Not requesting KI " + ki.getId() + " with BindingSet " + request + ", answer was in cache");
				node.processKnowledgeInteractionResponse(requestCache.get(request));
			} else {
				System.out.println("Requesting KI " + ki.getId() + " with BindingSet " + request);
				BindingSet reponse = ki.processRequest(node.getKnowledgeInteractionBinding());
				requestCache.put(request, reponse);
				node.processKnowledgeInteractionResponse(reponse);
			}
		}

		remoteTasks.remove(knowledgeInteractionId);
	}

	@Override
	public String toString() {
		return "RemoteTaskBoard [remoteTasks=" + remoteTasks + "]";
	}

}
