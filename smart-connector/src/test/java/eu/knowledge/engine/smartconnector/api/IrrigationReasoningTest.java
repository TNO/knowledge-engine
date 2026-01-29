package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.ExecutionException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

public class IrrigationReasoningTest {

	private PrefixMapping prefixes = new PrefixMappingMem();

	private static final Logger LOG = LoggerFactory.getLogger(IrrigationReasoningTest.class);

	private BindingSet irrigationRequestReceived = null;

	@Test
	public void irrigationTest() throws InterruptedException, ExecutionException {

		prefixes.setNsPrefix("ex", "https://www.example.org/");
		prefixes.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		prefixes.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

		KnowledgeNetwork kn = new KnowledgeNetwork();

		var appKb = initAppKb();
		kn.addKB(appKb);
		kn.addKB(initRasterKb());
		kn.addKB(initIrrigationKb());
		kn.addKB(initDroneKb());

		kn.sync();
		LOG.info("EEK");

		// post irrigationrequest
		BindingSet bs = new BindingSet();

		Binding b = new Binding();
		b.put("ir", "<https://www.example.org/irrigationrequest1>");
		b.put("field", "<https://www.example.org/field1>");
		bs.add(b);

		appKb.post(appKb.getPostKnowledgeInteractions().iterator().next(), bs).get();

		// check if irrigation KB received irrigationrequest
		assertNotNull(this.irrigationRequestReceived);

		kn.stop();
	}

	private KnowledgeBaseImpl initAppKb() {

		KnowledgeBaseImpl appKb = new KnowledgeBaseImpl("AppKb");

		GraphPattern gp = new GraphPattern(prefixes, """
					?ir rdf:type ex:IrrigationRequest .
					?ir ex:forField ?field .
				""");

		PostKnowledgeInteraction postKI = new PostKnowledgeInteraction(new CommunicativeAct(), gp, null,
				"irrigation-request");

		appKb.register(postKI);

		appKb.setReasonerLevel(5);

		return appKb;

	}

	private KnowledgeBaseImpl initDroneKb() {
		KnowledgeBaseImpl droneKb = new KnowledgeBaseImpl("DroneKb");

		GraphPattern gp1 = new GraphPattern(prefixes, """
					?ir rdf:type ex:IrrigationRequest .
					?ir ex:forField ?field .
					?ir ex:recentRasterAvailable "false"^^<http://www.w3.org/2001/XMLSchema#boolean> .
				""");

		GraphPattern gp2 = new GraphPattern(prefixes, """
					?ir rdf:type ex:IrrigationRequest .
					?ir ex:forField ?field .
					?ir ex:recentRasterAvailable "true"^^<http://www.w3.org/2001/XMLSchema#boolean> .
				""");

		ReactKnowledgeInteraction reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), gp1, gp2,
				"fly-drone");

		droneKb.register(reactKI, (ki, exchangeInfo) -> {

			for (int i = 0; i < 10; i++) {
				LOG.info("Drone flying...");
				try {
					Thread.sleep(750);
				} catch (InterruptedException ie) {
					LOG.error("Sleeping interrupted!", ie);
				}
			}

			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("ir", "<https://www.example.org/irrigationrequest1>");
			b.put("field", "<https://www.example.org/field1>");
			bs.add(b);
			return bs;
		});

		return droneKb;
	}

	private KnowledgeBaseImpl initRasterKb() {

		KnowledgeBaseImpl rasterKb = new KnowledgeBaseImpl("RasterKb");

		// availability KI
		GraphPattern gp1 = new GraphPattern(prefixes, """
				?ir ex:recentRasterAvailable ?avail .
				""");

		AnswerKnowledgeInteraction answerAvailKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1,
				"raster-available");

		rasterKb.register(answerAvailKI, (ki, answerExchangeInfo) -> {

			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("ir", "<https://www.example.org/irrigationrequest1>");
			b.put("avail", "\"false\"^^<http://www.w3.org/2001/XMLSchema#boolean>");
			bs.add(b);

			return bs;
		});

		// raster location KI
		GraphPattern gp2 = new GraphPattern(prefixes, """
				?field ex:hasRaster ?raster .
				?raster rdf:type ex:IrrigationRaster .
				?raster ex:hasLocation ?location .
				""");

		AnswerKnowledgeInteraction answerRasterKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp2,
				"raster-location");

		rasterKb.register(answerRasterKI, (ki, answerExchangeInfo) -> {

			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("field", "<https://www.example.org/field1>");
			b.put("raster", "<https://www.example.org/raster1>");
			b.put("location", "<https://www.example.org/location1>");
			bs.add(b);

			return bs;
		});

		return rasterKb;

	}

	private KnowledgeBaseImpl initIrrigationKb() {

		KnowledgeBaseImpl irrigationKb = new KnowledgeBaseImpl("IrrigationKb");

		GraphPattern gp = new GraphPattern(prefixes, """
					?ir rdf:type ex:IrrigationRequest .
					?ir ex:recentRasterAvailable "true"^^<http://www.w3.org/2001/XMLSchema#boolean> .
					?ir ex:forField ?field .
					?field ex:hasRaster ?raster .
					?raster rdf:type ex:IrrigationRaster .
					?raster ex:hasLocation ?location .
				""");

		ReactKnowledgeInteraction irrigationKI = new ReactKnowledgeInteraction(new CommunicativeAct(), gp, null,
				"receive-irrigation");

		irrigationKb.register(irrigationKI, (ki, reactExchangeInfo) -> {

			BindingSet bs = reactExchangeInfo.getArgumentBindings();

			LOG.info("BindingSet received: {}", bs);
			IrrigationReasoningTest.this.irrigationRequestReceived = bs;

			return new BindingSet();
		});

		return irrigationKb;
	}

}
