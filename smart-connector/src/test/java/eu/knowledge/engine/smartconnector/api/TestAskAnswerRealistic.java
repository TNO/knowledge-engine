package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutionException;

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
    private static MockedKnowledgeBase kb4;

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
        kb4 = new MockedKnowledgeBase("kb4");
        kb4.setReasonerEnabled(true);

        kn.addKB(kb1);
        kn.addKB(kb2);
        kn.addKB(kb3);
        kn.addKB(kb4);

        LOG.info("Waiting for everyone to be ready...");
        kn.startAndWaitForReady();
        LOG.info("Everyone is ready!");

        GraphPattern gp1 = new GraphPattern("?building <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ." 
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


        AnswerKnowledgeInteraction aKI1 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
        kb1.register(aKI1, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
            assertTrue(
                anAnswerExchangeInfo.getIncomingBindings().isEmpty() 
                    || anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
                    "Should not have bindings in this bindings set");

            BindingSet bindingSet = new BindingSet();
            Binding binding = new Binding();
            binding.put("building", "<https://www.tno.nl/example/building>");
            binding.put("spatialThing", "<https://www.tno.nl/example/spatialThing>");
            binding.put("zipCode", "<https://www.tno.nl/example/zipCode>");
            binding.put("energyClass", "<https://www.tno.nl/example/energyClass>");
            binding.put("energyProvider", "<https://www.tno.nl/example/energyProvider>");
            binding.put("flexibilityManager", "<https://www.tno.nl/example/flexibilityManager>");
            binding.put("communityID", "<https://www.tno.nl/example/communityID>");
            binding.put("buildingSpace", "<https://www.tno.nl/example/buildingSpace>");
            binding.put("buildingDevice", "<https://www.tno.nl/example/buildingDevice>");
            binding.put("powerSubscribed", "<https://www.tno.nl/example/powerSubscribed>");

            bindingSet.add(binding);
            return bindingSet;
        });

        GraphPattern gp2 = new GraphPattern("?construcao <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ." 
         + "?construcao <http://ontology.tno.nl/building#LocatedIn> ?coisaEspacial ." 
         + "?coisaEspacial <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing> ." 
         + "?coisaEspacial <http://www.geonames.org/ontology#postalCode> ?codigoPostal . " 
         + "?construcao <http://ontology.tno.nl/building#hasEnergyClass> ?classeDeEnergia ." 
         + "?construcao <http://ontology.tno.nl/building#energyProvider> ?fornecedorDeEnergia ." 
         + "?construcao <http://ontology.tno.nl/building#flexibilityManager> ?gerenteDeFlexibilidade ." 
         + "?construcao <http://ontology.tno.nl/building#communityID> ?IDdaComunidade ." 
         + "?construcao <https://saref.etsi.org/saref4bldg/hasSpace> ?edificioEspaco ." 
         + "?edificioEspaco <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/BuildingSpace> ." 
         + "?edificioEspaco <https://saref.etsi.org/saref4bldg/contains> ?dispositivoDeConstrucao ." 
         + "?dispositivoDeConstrucao <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/BuildingDevice> ." 
         + "?dispositivoDeConstrucao <http://tno.io/PowerLimit#hasContractualPowerLimit> ?poderSubscrito .");


         AnswerKnowledgeInteraction aKI2 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp2);
         kb2.register(aKI2, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
             assertTrue(
                 anAnswerExchangeInfo.getIncomingBindings().isEmpty() || 
                    anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
                    "Should not having bindings in this bindings set"
             );

            BindingSet bindingSet = new BindingSet();
            Binding binding = new Binding();
            binding.put("construcao", "<https://www.tno.nl/example/construcao>");
            binding.put("coisaEspacial", "<https://www.tno.nl/example/coisaEspacial>");
            binding.put("codigoPostal", "<https://www.tno.nl/example/codigoPostal>");
            binding.put("classeDeEnergia", "<https://www.tno.nl/example/classeDeEnergia>");
            binding.put("fornecedorDeEnergia", "<https://www.tno.nl/example/fornecedorDeEnergia>");
            binding.put("gerenteDeFlexibilidade", "<https://www.tno.nl/example/gerenteDeFlexibilidade>");
            binding.put("IDdaComunidade", "<https://www.tno.nl/example/IDdaDomunidade>");
            binding.put("edificioEspaco", "<https://www.tno.nl/example/edificioEspaco>");
            binding.put("dispositivoDeConstrucao", "<https://www.tno.nl/example/dispositivoDeConstrucao>");
            binding.put("poderSubscrito", "<https://www.tno.nl/example/poderSubscrito>");

            bindingSet.add(binding);
            return bindingSet;
         });

        GraphPattern gp4 = new GraphPattern("?zgrada <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ." 
        + "?zgrada <http://ontology.tno.nl/building#LocatedIn> ?prostornaStvar ." 
        + "?prostornaStvar <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing> ." 
        + "?prostornaStvar <http://www.geonames.org/ontology#postalCode> ?postanskiBroj . " 
        + "?zgrada <http://ontology.tno.nl/building#hasEnergyClass> ?energetskaKlasa ." 
        + "?zgrada <http://ontology.tno.nl/building#energyProvider> ?dobavljacEnergije ." 
        + "?zgrada <http://ontology.tno.nl/building#flexibilityManager> ?menadzerFleksibilnosti ." 
        + "?zgrada <http://ontology.tno.nl/building#communityID> ?zajednica ." 
        + "?zgrada <https://saref.etsi.org/saref4bldg/hasSpace> ?zgradniProstor ." 
        + "?zgradniProstor <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/BuildingSpace> ." 
        + "?zgradniProstor <https://saref.etsi.org/saref4bldg/contains> ?zgradniUredjaj ." 
        + "?zgradniUredjaj <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/BuildingDevice> .");

        AnswerKnowledgeInteraction aKI4 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp4);
         kb4.register(aKI4, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
             assertTrue(
                 anAnswerExchangeInfo.getIncomingBindings().isEmpty() || 
                    anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
                    "Should not having bindings in this bindings set"
             );

            BindingSet bindingSet = new BindingSet();
            Binding binding = new Binding();
            binding.put("zgrada", "<https://www.tno.nl/example/zgrada>");
            binding.put("prostornaStvar", "<https://www.tno.nl/example/prostornaStvar>");
            binding.put("postanskiBroj", "<https://www.tno.nl/example/postanskiBroj>");
            binding.put("energetskaKlasa", "<https://www.tno.nl/example/energetskaKlasa>");
            binding.put("dobavljacEnergije", "<https://www.tno.nl/example/dobavljacEnergije>");
            binding.put("menadzerFleksibilnosti", "<https://www.tno.nl/example/menadzerFleksibilnosti>");
            binding.put("zajednica", "<https://www.tno.nl/example/zajednica>");
            binding.put("zgradniProstor", "<https://www.tno.nl/example/zgradniProstor>");
            binding.put("zgradniUredjaj", "<https://www.tno.nl/example/zgradniUredjaj>");

            bindingSet.add(binding);
            return bindingSet;
         });
        

        GraphPattern gp3 = new GraphPattern("?gebouw <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ." 
         + "?gebouw <http://ontology.tno.nl/building#LocatedIn> ?ruimtelijkDing ." 
         + "?ruimtelijkDing <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing> ." 
         + "?ruimtelijkDing <http://www.geonames.org/ontology#postalCode> ?postcode . " 
         + "?gebouw <http://ontology.tno.nl/building#hasEnergyClass> ?energieklasse ." 
         + "?gebouw <http://ontology.tno.nl/building#energyProvider> ?energieaanbieder ." 
         + "?gebouw <http://ontology.tno.nl/building#flexibilityManager> ?flexibiliteitManager ." 
         + "?gebouw <http://ontology.tno.nl/building#communityID> ?gemeenschapsID ." 
         + "?gebouw <https://saref.etsi.org/saref4bldg/hasSpace> ?gebouwRuimte ." 
         + "?gebouwRuimte <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/BuildingSpace> ." 
         + "?gebouwRuimte <https://saref.etsi.org/saref4bldg/contains> ?gebouwApparaat ." 
         + "?gebouwApparaat <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/BuildingDevice> .");

         AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp3);
         kb3.register(askKI);
         LOG.info("Waiting until everyone is up to date!");
         kn.waitForUpToDate();
         LOG.info("Everyone is up to date!");

         BindingSet bindings = null;
         AskResult result = null;

         try {

             LOG.trace("Before ask");
             result = kb3.ask(askKI, new BindingSet()).get();
             bindings = result.getBindings();
             LOG.trace("After ask");

             Set<URI> kbIds = result.getExchangeInfoPerKnowledgeBase().stream().map(
                 AskExchangeInfo::getKnowledgeBaseId
             ).collect(Collectors.toSet());

             assertEquals(
                 new HashSet<URI>(Arrays.asList(kb1.getKnowledgeBaseId(),
                                                kb2.getKnowledgeBaseId(),
                                                kb4.getKnowledgeBaseId()

                 )), kbIds, "The result/s should come from kb1 and kb2 and not: " + kbIds);

            assertEquals(3, bindings.size());
            
            for (Binding b : bindings) {
                assertTrue(b.containsKey("gebouw"));
                assertTrue(b.containsKey("ruimtelijkDing"));
                assertTrue(b.containsKey("postcode"));
                assertTrue(b.containsKey("energieklasse"));
                assertTrue(b.containsKey("energieaanbieder"));
                assertTrue(b.containsKey("flexibiliteitManager"));
                assertTrue(b.containsKey("gemeenschapsID"));
                assertTrue(b.containsKey("gebouwRuimte"));
                assertTrue(b.containsKey("gebouwApparaat"));
                LOG.info("Bindings: {}", bindings);
            }

         } catch (InterruptedException | ExecutionException e) {
             fail();
         }
        
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
            fail("KB3 should not be null!");
        }

        if (kb4 != null) {
            kb4.stop();
        } else {
            fail("KB4 should not be null!");
        }
    }
    
}
