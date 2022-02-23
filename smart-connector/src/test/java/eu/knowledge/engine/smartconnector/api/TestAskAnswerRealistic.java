package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAskAnswerRealistic {

    private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswerRealistic.class);

	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;
	private static MockedKnowledgeBase kb3;

    @BeforeAll
    public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {
        
    }

    @Test
    public void testAskAnswer() throws InterruptedException {

        var kn = new KnowledgeNetwork();

        kb1 = new MockedKnowledgeBase("kb1");
        kb1.setReasonerEnabled(true);
        kb2 = new MockedKnowledgeBase("kb2");
        kb2.setReasonerEnabled(true);
        kb3 = new MockedKnowledgeBase("kb3");
        kb3.setReasonerEnabled(true);

        kn.addKB(kb1);
        kn.addKB(kb2);
        kn.addKB(kb3);

        LOG.info("Waiting for everyone to be ready...");
        kn.startAndWaitForReady();
        LOG.info("Everyone is ready!");

        GraphPattern gp = new GraphPattern("?building <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ." 
         + "?building <http://ontology.tno.nl/building#LocatedIn> ?spatialThing ." 
         + "?spatialThing <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing> ." 
         + "?spatialThing <http://www.geonames.org/ontology#postalCode> ?zipCode . " 
         + "?building <http://ontology.tno.nl/building#hasEnergyClass> ?energyClass ." 
         + "?building <http://ontology.tno.nl/building#energyProvider> ?energyProvider ." 
         + "?building <http://ontology.tno.nl/building#flexibilityManager> ?flexibilityManager ." 
         + "?building <http://ontology.tno.nl/building#communityID> ?communityID ." 
         + "?building <https://saref.etsi.org/saref4bldg/hasSpace> ?buildingSpace ." 
         + "?buildingSpace <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/BuildingSpace> ." 
         + "?buildingSpace <https://saref.etsi.org/saref4bldg/contains> ?buildingDevice ." 
         + "?buildingDevice <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/BuildingDevice> ." 
         + "?buildingDevice <http://tno.io/PowerLimit#hasContractualPowerLimit> ?powerSubscribed .");

        // Register KnowledgeInteractions

        // Put bindings

        // Actual test/s
    }

    @AfterAll
    public static void cleanup() {
        LOG.info("Clean up: {}", TestAskAnswerRealistic.class.getSimpleName());
        if (kb1 != null) {
            kb1.stop();
        } else {
            fail("KB1 should not be null!");
        }

        if (kb2 != null) {
            kb2.stop();
        } else {
            fail("KB2 should not be null!");
        }

        if (kb3 != null) {
            kb3.stop();
        } else {
            fail("KB# should not be null!");
        }
    }
    
}
