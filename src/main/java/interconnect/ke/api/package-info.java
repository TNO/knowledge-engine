/**
 * This package contains the classes that make up the Java Developer API of
 * Knowledge Engine. This is a middleware solution for (among other things)
 * exchanging data in an interoperable manner using Semantic Web technologies.
 * 
 * The {@link interconnect.ke.api.SmartConnector} represents the gateway that
 * allows the {@link interconnect.ke.api.KnowledgeBase} to connect to the
 * Knowledge Engine and exchange data. The app/service/platform or database that
 * wants to become interoperable needs to implement the
 * {@link interconnect.ke.api.KnowledgeBase} interface and make an instance of
 * the {@link interconnect.ke.api.SmartConnector} class. It should also register
 * {@link interconnect.ke.api.interaction.KnowledgeInteraction}s that let the
 * Knowledge Engine (and the other {@link interconnect.ke.api.SmartConnector}s
 * in the network know what its capabilities are.
 */
package interconnect.ke.api;