package interconnect.ke.sc;

import java.net.URI;
import java.util.Set;

import interconnect.ke.api.AnswerHandler;
import interconnect.ke.api.KnowledgeBase;
import interconnect.ke.api.ReactHandler;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;

/**
 * The MyKnowledgeBaseStore keeps tracks of the {@link KnowledgeInteraction}s
 * that are currently registered by the {@link KnowledgeBase} associated with
 * this {@link SmartConnectorImpl}.
 */
public interface KnowledgeBaseStore {

	/**
	 * @return Globally unique identifier for this knowledge base.
	 */
	URI getKnowledgeBaseId();

	/**
	 * @return Human-friendly name of this knowledge base.
	 */
	String getKnowledgeBaseName();

	/**
	 * @return A short description of this knowledge base.
	 */
	String getKnowledgeBaseDescription();

	KnowledgeInteractionInfo getKnowledgeInteractionById(URI id);

	MyKnowledgeInteractionInfo getKnowledgeInteractionByObject(KnowledgeInteraction ki);

	Set<MyKnowledgeInteractionInfo> getKnowledgeInteractions();

	Set<MyKnowledgeInteractionInfo> getAskKnowledgeInteractions();

	Set<MyKnowledgeInteractionInfo> getAnswerKnowledgeInteractions();

	Set<MyKnowledgeInteractionInfo> getPostKnowledgeInteractions();

	Set<MyKnowledgeInteractionInfo> getReactKnowledgeInteractions();

	AnswerHandler getAnswerHandler(URI anAnswerKiId);

	ReactHandler getReactHandler(URI anReactKiId);

	void addListener(KnowledgeBaseStoreListener listener);

	void removeListener(KnowledgeBaseStoreListener listener);

	URI register(AskKnowledgeInteraction anAskKI);

	void unregister(AskKnowledgeInteraction anAskKI);

	URI register(AnswerKnowledgeInteraction anAnswerKI, AnswerHandler aAnswerHandler);

	void unregister(AnswerKnowledgeInteraction anAnswerKI);

	URI register(PostKnowledgeInteraction aPostKI);

	void unregister(PostKnowledgeInteraction aPostKI);

	URI register(ReactKnowledgeInteraction anReactKI, ReactHandler aReactHandler);

	void unregister(ReactKnowledgeInteraction anReactKI);

	void register(MyKnowledgeInteractionInfo aKI);

}
