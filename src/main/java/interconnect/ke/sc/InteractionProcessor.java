package interconnect.ke.sc;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.KnowledgeBase;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;

/**
 * This interface is used to abstract away the details of the matching/reasoning
 * process. Both the matcher and (in the future) the reasoner will implement
 * this interface in a different way which results in different behaviour of the
 * smart connector overall.
 * 
 * It receives a {@link KnowledgeInteraction} with bindings and returns a set of
 * bindings. Depending on the type of {@link KnowledgeInteraction} these
 * bindings are for the single {@link GraphPattern} in the
 * {@link KnowledgeInteraction} or for the argument/result
 * {@link GraphPattern}s.
 */
public abstract class InteractionProcessor {

	private Set<KnowledgeBase> knowledgeBases;

	public InteractionProcessor(Set<KnowledgeBase> someKnowledgeBases) {
		this.knowledgeBases = someKnowledgeBases;
	}

	abstract CompletableFuture<BindingSet> processInteraction(AskKnowledgeInteraction aAKI, BindingSet someBindings);

	abstract CompletableFuture<BindingSet> processInteraction(PostKnowledgeInteraction aPKI,
			BindingSet someArgumentBindings);

}
