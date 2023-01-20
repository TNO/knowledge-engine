package eu.knowledge.engine.smartconnector.impl;

import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.Vocab;

/**
 * An {@link OtherKnowledgeBase} represents a knowledge base in the network that
 * is NOT the knowledge base that this smart connector represents.
 */
public class OtherKnowledgeBase {
	private final URI id;
	private final String name;
	private final String description;
	private final List<KnowledgeInteractionInfo> knowledgeInteractions;
	private final URL endpoint;

	private Model rdf = null;

	private static final Logger LOG = LoggerFactory.getLogger(OtherKnowledgeBase.class);

	public OtherKnowledgeBase(URI anId, String aName, String aDescription,
			List<KnowledgeInteractionInfo> someKnowledgeInteractions, URL anEndpoint) {
		this.id = anId;
		this.name = aName;
		this.description = aDescription;
		this.knowledgeInteractions = someKnowledgeInteractions;
		this.endpoint = anEndpoint;
	}

	public Model getRDF() {

		if (this.rdf == null) {

			// first create a RDF version of this KnowledgeBase
			Model m = ModelFactory.createDefaultModel();

			Resource kb = m.createResource(this.getId().toString());
			m.add(kb, RDF.type, Vocab.KNOWLEDGE_BASE);
			m.add(kb, Vocab.HAS_NAME, m.createLiteral(this.getName()));
			m.add(kb, Vocab.HAS_DESCR, m.createLiteral(this.getDescription()));

			List<KnowledgeInteractionInfo> myKIs = this.getKnowledgeInteractions();

			for (KnowledgeInteractionInfo myKI : myKIs) {
				Resource ki = m.createResource(myKI.getId().toString());
				m.add(kb, Vocab.HAS_KI, ki);
				m.add(ki, Vocab.IS_META, ResourceFactory.createTypedLiteral(myKI.isMeta()));
				Resource act = m.createResource(myKI.getId().toString() + "/act");
				m.add(ki, Vocab.HAS_ACT, act);
				m.add(act, RDF.type, Vocab.COMMUNICATIVE_ACT);
				Resource req = m.createResource(act.toString() + "/req");
				m.add(act, Vocab.HAS_REQ, req);
				Resource sat = m.createResource(act.toString() + "/sat");
				m.add(act, Vocab.HAS_SAT, sat);
				for (Resource r : myKI.getKnowledgeInteraction().getAct().getRequirementPurposes()) {
					m.add(req, RDF.type, r);
				}
				for (Resource r : myKI.getKnowledgeInteraction().getAct().getSatisfactionPurposes()) {
					m.add(sat, RDF.type, r);
				}

				switch (myKI.getType()) {
				case ASK:
					m.add(ki, RDF.type, Vocab.ASK_KI);
					Resource gp = m.createResource(myKI.getId() + "/gp");
					m.add(ki, Vocab.HAS_GP, gp);
					m.add(gp, RDF.type, Vocab.GRAPH_PATTERN);
					m.add(gp, Vocab.HAS_PATTERN, m.createLiteral(this
							.convertToPattern(((AskKnowledgeInteraction) myKI.getKnowledgeInteraction()).getPattern())));
					break;
				case ANSWER:
					m.add(ki, RDF.type, Vocab.ANSWER_KI);
					gp = m.createResource(myKI.getId() + "/gp");
					m.add(ki, Vocab.HAS_GP, gp);
					m.add(gp, RDF.type, Vocab.GRAPH_PATTERN);
					m.add(gp, Vocab.HAS_PATTERN, m.createLiteral(this
							.convertToPattern(((AnswerKnowledgeInteraction) myKI.getKnowledgeInteraction()).getPattern())));
					break;
				case POST:
					m.add(ki, RDF.type, Vocab.POST_KI);
					Resource argGp = m.createResource(myKI.getId() + "/argumentgp");
					m.add(ki, Vocab.HAS_GP, argGp);
					m.add(argGp, RDF.type, Vocab.ARGUMENT_GRAPH_PATTERN);
					GraphPattern argument = ((PostKnowledgeInteraction) myKI.getKnowledgeInteraction()).getArgument();
					if (argument != null)
						m.add(argGp, Vocab.HAS_PATTERN, m.createLiteral(this.convertToPattern(argument)));
	
					// CHECK: If the KI doesn't have a result gp, do we still need to create
					// these resources? Currently, we do.
					Resource resGp = m.createResource(myKI.getId() + "/resultgp");
					m.add(ki, Vocab.HAS_GP, resGp);
					m.add(resGp, RDF.type, Vocab.RESULT_GRAPH_PATTERN);
					GraphPattern result = ((PostKnowledgeInteraction) myKI.getKnowledgeInteraction()).getResult();
					if (result != null)
						m.add(resGp, Vocab.HAS_PATTERN, m.createLiteral(this.convertToPattern(result)));
					break;
				case REACT:
					m.add(ki, RDF.type, Vocab.REACT_KI);
					argGp = m.createResource(myKI.getId() + "/argumentgp");
					m.add(ki, Vocab.HAS_GP, argGp);
					m.add(argGp, RDF.type, Vocab.ARGUMENT_GRAPH_PATTERN);
					argument = ((ReactKnowledgeInteraction) myKI.getKnowledgeInteraction()).getArgument();
					if (argument != null)
						m.add(argGp, Vocab.HAS_PATTERN, m.createLiteral(this.convertToPattern(argument)));
	
					// CHECK: If the KI doesn't have a result gp, do we still need to create
					// these resources? Currently, we do.
					resGp = m.createResource(myKI.getId() + "/resultgp");
					m.add(ki, Vocab.HAS_GP, resGp);
					m.add(resGp, RDF.type, Vocab.RESULT_GRAPH_PATTERN);
					result = ((ReactKnowledgeInteraction) myKI.getKnowledgeInteraction()).getResult();
					if (result != null)
						m.add(resGp, Vocab.HAS_PATTERN, m.createLiteral(this.convertToPattern(result)));
					break;
				default:
					LOG.warn("Ignored currently unsupported knowledge interaction type {}.", myKI.getType());
					assert false;
				}
			}

//			System.out.println("------------------------");
//			m.write(System.out, "turtle");
//			System.out.println("------------------------");

			this.rdf = m;
		}
		return rdf;
	}

	public URI getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public List<KnowledgeInteractionInfo> getKnowledgeInteractions() {
		return this.knowledgeInteractions;
	}

	public URL getEndpoint() {
		return this.endpoint;
	}

	private String convertToPattern(GraphPattern gp) {
		Iterator<TriplePath> iter = gp.getGraphPattern().patternElts();

		StringBuilder sb = new StringBuilder();

		while (iter.hasNext()) {

			TriplePath tp = iter.next();
			sb.append(FmtUtils.stringForTriple(tp.asTriple(), new PrefixMappingMem()));
			sb.append(" . ");
		}

		return sb.toString();
	}
}
