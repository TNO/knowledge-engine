package interconnect.ke.api;

import java.util.Set;

public interface ReactHandler {

	public ReactResult react(ReactKnowledgeInteraction aReactKnowledgeInteraction, Set<Bindings> argument);

}
