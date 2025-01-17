package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;

@Disabled
public class TestAskAnswerRealistic {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswerRealistic.class);

	private static KnowledgeBaseImpl kb1;
	private static KnowledgeBaseImpl kb2;
	private static KnowledgeBaseImpl kb3;
	private static KnowledgeBaseImpl kb4;
	private static KnowledgeBaseImpl kb5;
	private static KnowledgeBaseImpl kb6;
	private static KnowledgeBaseImpl kb7;
	private static KnowledgeBaseImpl kb8;
	private static KnowledgeBaseImpl kb9;
	private static KnowledgeBaseImpl kb10;

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {

	}

	@Test
	public void testAskAnswer() throws InterruptedException {

		var kn = new KnowledgeNetwork();

		kb1 = new KnowledgeBaseImpl("kb1");
		kb1.setReasonerEnabled(true);
		kb2 = new KnowledgeBaseImpl("kb2");
		kb2.setReasonerEnabled(true);
		kb3 = new KnowledgeBaseImpl("kb3");
		kb3.setReasonerEnabled(true);
		kb4 = new KnowledgeBaseImpl("kb4");
		kb4.setReasonerEnabled(true);
		kb5 = new KnowledgeBaseImpl("kb5");
		kb5.setReasonerEnabled(true);
		kb6 = new KnowledgeBaseImpl("kb6");
		kb6.setReasonerEnabled(true);
		kb7 = new KnowledgeBaseImpl("kb7");
		kb7.setReasonerEnabled(true);
		kb8 = new KnowledgeBaseImpl("kb8");
		kb8.setReasonerEnabled(true);
		kb9 = new KnowledgeBaseImpl("kb9");
		kb9.setReasonerEnabled(true);
		kb10 = new KnowledgeBaseImpl("kb10");
		kb10.setReasonerEnabled(true);

		var rules = new HashSet<Rule>();

		var antecedent = new HashSet<TriplePattern>();
		var consequent = new HashSet<TriplePattern>();
		antecedent.add(new TriplePattern("?a <http://ontology.tno.nl/building#energyProvider> ?b"));
		consequent.add(new TriplePattern("?a <http://ontology.tno.nl/building#energyProviderSYNONYM> ?b"));
		rules.add(new Rule(antecedent, consequent));

		antecedent = new HashSet<TriplePattern>();
		consequent = new HashSet<TriplePattern>();
		antecedent.add(new TriplePattern(
				"?c <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building>"));
		consequent.add(new TriplePattern(
				"?c <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/House>"));
		rules.add(new Rule(antecedent, consequent));

		antecedent = new HashSet<TriplePattern>();
		consequent = new HashSet<TriplePattern>();
		antecedent.add(new TriplePattern("?a <http://ontology.tno.nl/building#communityID> ?d"));
		consequent.add(new TriplePattern("?a <http://ontology.tno.nl/building#communityIDSYNONYM> ?d"));
		rules.add(new Rule(antecedent, consequent));

		antecedent = new HashSet<TriplePattern>();
		consequent = new HashSet<TriplePattern>();
		antecedent.add(new TriplePattern("?e <http://www.geonames.org/ontology#postalCode> ?f"));
		consequent.add(new TriplePattern("?e <http://www.geonames.org/ontology#zipCode> ?f"));
		rules.add(new Rule(antecedent, consequent));

		antecedent = new HashSet<TriplePattern>();
		consequent = new HashSet<TriplePattern>();
		antecedent.add(new TriplePattern("?g <https://saref.etsi.org/saref4bldg/contains> ?h"));
		consequent.add(new TriplePattern("?g <https://saref.etsi.org/saref4bldg/sadrži> ?h"));
		rules.add(new Rule(antecedent, consequent));

		antecedent = new HashSet<TriplePattern>();
		consequent = new HashSet<TriplePattern>();
		antecedent.add(new TriplePattern("?h <http://tno.io/PowerLimit#hasContractualPowerLimit> ?p"));
		consequent.add(new TriplePattern("?h <http://tno.io/PowerLimit#imaOgraničenje> ?p"));
		rules.add(new Rule(antecedent, consequent));

		antecedent = new HashSet<TriplePattern>();
		consequent = new HashSet<TriplePattern>();
		antecedent.add(new TriplePattern("?a <http://ontology.tno.nl/building#flexibilityManager> ?m"));
		consequent.add(new TriplePattern("?a <http://ontology.tno.nl/building#Menadžer> ?m"));
		rules.add(new Rule(antecedent, consequent));

		kn.addKB(kb1);
		kn.addKB(kb2);
		kn.addKB(kb3);
		kn.addKB(kb4);
		kn.addKB(kb5);
		kn.addKB(kb6);
		kn.addKB(kb7);
		kn.addKB(kb8);
		kn.addKB(kb9);
		kn.addKB(kb10);

		LOG.info("Waiting for everyone to be ready...");
		kn.sync();
		LOG.info("Everyone is ready!");

		kb1.setDomainKnowledge(rules);
		kb2.setDomainKnowledge(rules);
		kb3.setDomainKnowledge(rules);
		kb4.setDomainKnowledge(rules);
		kb5.setDomainKnowledge(rules);
		kb6.setDomainKnowledge(rules);
		kb7.setDomainKnowledge(rules);
		kb8.setDomainKnowledge(rules);
		kb9.setDomainKnowledge(rules);
		kb10.setDomainKnowledge(rules);

		GraphPattern gp1 = new GraphPattern(
				"?building <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ."
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
			binding.put("building", "<https://www.tno.nl/example/building1>");
			binding.put("spatialThing", "<https://www.tno.nl/example/spatialThing1>");
			binding.put("zipCode", "<https://www.tno.nl/example/zipCode1>");
			binding.put("energyClass", "<https://www.tno.nl/example/energyClass1>");
			binding.put("energyProvider", "<https://www.tno.nl/example/energyProvider1>");
			binding.put("flexibilityManager", "<https://www.tno.nl/example/flexibilityManager1>");
			binding.put("communityID", "<https://www.tno.nl/example/communityID1>");
			binding.put("buildingSpace", "<https://www.tno.nl/example/buildingSpace1>");
			binding.put("buildingDevice", "<https://www.tno.nl/example/buildingDevice1>");
			binding.put("powerSubscribed", "<https://www.tno.nl/example/powerSubscribed1>");

			bindingSet.add(binding);
			return bindingSet;
		});

		GraphPattern gp2 = new GraphPattern(
				"?construcao <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ."
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
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not having bindings in this bindings set");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("construcao", "<https://www.tno.nl/example/construcao2>");
			binding.put("coisaEspacial", "<https://www.tno.nl/example/coisaEspacial2>");
			binding.put("codigoPostal", "<https://www.tno.nl/example/codigoPostal2>");
			binding.put("classeDeEnergia", "<https://www.tno.nl/example/classeDeEnergia2>");
			binding.put("fornecedorDeEnergia", "<https://www.tno.nl/example/fornecedorDeEnergia2>");
			binding.put("gerenteDeFlexibilidade", "<https://www.tno.nl/example/gerenteDeFlexibilidade2>");
			binding.put("IDdaComunidade", "<https://www.tno.nl/example/IDdaDomunidade2>");
			binding.put("edificioEspaco", "<https://www.tno.nl/example/edificioEspaco2>");
			binding.put("dispositivoDeConstrucao", "<https://www.tno.nl/example/dispositivoDeConstrucao2>");
			binding.put("poderSubscrito", "<https://www.tno.nl/example/poderSubscrito2>");

			bindingSet.add(binding);
			return bindingSet;
		});

		GraphPattern gp4 = new GraphPattern(
				"?zgrada <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ."
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
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not having bindings in this bindings set");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("zgrada", "<https://www.tno.nl/example/zgrada4>");
			binding.put("prostornaStvar", "<https://www.tno.nl/example/prostornaStvar4>");
			binding.put("postanskiBroj", "<https://www.tno.nl/example/postanskiBroj4>");
			binding.put("energetskaKlasa", "<https://www.tno.nl/example/energetskaKlasa4>");
			binding.put("dobavljacEnergije", "<https://www.tno.nl/example/dobavljacEnergije4>");
			binding.put("menadzerFleksibilnosti", "<https://www.tno.nl/example/menadzerFleksibilnosti4>");
			binding.put("zajednica", "<https://www.tno.nl/example/zajednica4>");
			binding.put("zgradniProstor", "<https://www.tno.nl/example/zgradniProstor4>");
			binding.put("zgradniUredjaj", "<https://www.tno.nl/example/zgradniUredjaj4>");

			bindingSet.add(binding);
			return bindingSet;
		});

		GraphPattern gp5 = new GraphPattern(
				"?building <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ."
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

		AnswerKnowledgeInteraction aKI5 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp5);
		kb5.register(aKI5, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this bindings set");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("building", "<https://www.tno.nl/example/house5>");
			binding.put("spatialThing", "<https://www.tno.nl/example/spatialThing5>");
			binding.put("zipCode", "<https://www.tno.nl/example/zipCode5>");
			binding.put("energyClass", "<https://www.tno.nl/example/energyClass5>");
			binding.put("energyProvider", "<https://www.tno.nl/example/energyProvider5>");
			binding.put("flexibilityManager", "<https://www.tno.nl/example/flexibilityManager5>");
			binding.put("communityID", "<https://www.tno.nl/example/communityID5>");
			binding.put("buildingSpace", "<https://www.tno.nl/example/buildingSpace5>");
			binding.put("buildingDevice", "<https://www.tno.nl/example/buildingDevice5>");
			binding.put("powerSubscribed", "<https://www.tno.nl/example/powerSubscribed5>");

			bindingSet.add(binding);
			return bindingSet;
		});

		GraphPattern gp6 = new GraphPattern(
				"?building <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ."
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

		AnswerKnowledgeInteraction aKI6 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp6);
		kb6.register(aKI6, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this bindings set");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("building", "<https://www.tno.nl/example/building6>");
			binding.put("spatialThing", "<https://www.tno.nl/example/spatialThing6>");
			binding.put("zipCode", "<https://www.tno.nl/example/zipCode6>");
			binding.put("energyClass", "<https://www.tno.nl/example/energyClass6>");
			binding.put("energyProvider", "<https://www.tno.nl/example/energyProvider6>");
			binding.put("flexibilityManager", "<https://www.tno.nl/example/flexibilityManager6>");
			binding.put("communityID", "<https://www.tno.nl/example/ID6>");
			binding.put("buildingSpace", "<https://www.tno.nl/example/buildingSpace6>");
			binding.put("buildingDevice", "<https://www.tno.nl/example/buildingDevice6>");
			binding.put("powerSubscribed", "<https://www.tno.nl/example/powerSubscribed6>");

			bindingSet.add(binding);
			return bindingSet;
		});

		GraphPattern gp7 = new GraphPattern(
				"?building <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ."
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

		AnswerKnowledgeInteraction aKI7 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp7);
		kb7.register(aKI7, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this bindings set");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("building", "<https://www.tno.nl/example/building7>");
			binding.put("spatialThing", "<https://www.tno.nl/example/spatialThing7>");
			binding.put("zipCode", "<https://www.tno.nl/example/postCode7>");
			binding.put("energyClass", "<https://www.tno.nl/example/energyClass7>");
			binding.put("energyProvider", "<https://www.tno.nl/example/energyProvider7>");
			binding.put("flexibilityManager", "<https://www.tno.nl/example/flexibilityManager7>");
			binding.put("communityID", "<https://www.tno.nl/example/ID7>");
			binding.put("buildingSpace", "<https://www.tno.nl/example/buildingSpace7>");
			binding.put("buildingDevice", "<https://www.tno.nl/example/buildingDevice7>");
			binding.put("powerSubscribed", "<https://www.tno.nl/example/powerSubscribed7>");

			bindingSet.add(binding);
			return bindingSet;
		});

		GraphPattern gp8 = new GraphPattern(
				"?building <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ."
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

		AnswerKnowledgeInteraction aKI8 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp8);
		kb8.register(aKI8, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this bindings set");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("building", "<https://www.tno.nl/example/building8>");
			binding.put("spatialThing", "<https://www.tno.nl/example/spatialThing8>");
			binding.put("zipCode", "<https://www.tno.nl/example/zipCode8>");
			binding.put("energyClass", "<https://www.tno.nl/example/energyClass8>");
			binding.put("energyProvider", "<https://www.tno.nl/example/energyProvider8>");
			binding.put("flexibilityManager", "<https://www.tno.nl/example/flexibilityManager8>");
			binding.put("communityID", "<https://www.tno.nl/example/ID8>");
			binding.put("buildingSpace", "<https://www.tno.nl/example/buildingSpace8>");
			binding.put("buildingDevice", "<https://www.tno.nl/example/uređaj8>");
			binding.put("powerSubscribed", "<https://www.tno.nl/example/powerSubscribed8>");

			bindingSet.add(binding);
			return bindingSet;
		});

		GraphPattern gp9 = new GraphPattern(
				"?building <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ."
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

		AnswerKnowledgeInteraction aKI9 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp9);
		kb9.register(aKI9, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this bindings set");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("building", "<https://www.tno.nl/example/building9>");
			binding.put("spatialThing", "<https://www.tno.nl/example/spatialThing9>");
			binding.put("zipCode", "<https://www.tno.nl/example/zipCode9>");
			binding.put("energyClass", "<https://www.tno.nl/example/energyClass9>");
			binding.put("energyProvider", "<https://www.tno.nl/example/energyProvider9>");
			binding.put("flexibilityManager", "<https://www.tno.nl/example/flexibilityManager9>");
			binding.put("communityID", "<https://www.tno.nl/example/ID9>");
			binding.put("buildingSpace", "<https://www.tno.nl/example/buildingSpace9>");
			binding.put("buildingDevice", "<https://www.tno.nl/example/buildingDevice9>");
			binding.put("powerSubscribed", "<https://www.tno.nl/example/powerSubscribed9>");

			bindingSet.add(binding);
			return bindingSet;
		});

		GraphPattern gp10 = new GraphPattern(
				"?building <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> ."
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

		AnswerKnowledgeInteraction aKI10 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp10);
		kb10.register(aKI10, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this bindings set");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("building", "<https://www.tno.nl/example/building10>");
			binding.put("spatialThing", "<https://www.tno.nl/example/spatialThing10>");
			binding.put("zipCode", "<https://www.tno.nl/example/zipCode10>");
			binding.put("energyClass", "<https://www.tno.nl/example/energyClass10>");
			binding.put("energyProvider", "<https://www.tno.nl/example/energyProvider10>");
			binding.put("flexibilityManager", "<https://www.tno.nl/example/Menadžer10>");
			binding.put("communityID", "<https://www.tno.nl/example/ID10>");
			binding.put("buildingSpace", "<https://www.tno.nl/example/buildingSpace10>");
			binding.put("buildingDevice", "<https://www.tno.nl/example/buildingDevice10>");
			binding.put("powerSubscribed", "<https://www.tno.nl/example/powerSubscribed10>");

			bindingSet.add(binding);
			return bindingSet;
		});

		GraphPattern gp3 = new GraphPattern(
				"?gebouw <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/House> ."
						+ "?gebouw <http://ontology.tno.nl/building#LocatedIn> ?ruimtelijkDing ."
						+ "?ruimtelijkDing <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing> ."
						+ "?ruimtelijkDing <http://www.geonames.org/ontology#zipCode> ?postcode . "
						+ "?gebouw <http://ontology.tno.nl/building#hasEnergyClass> ?energieklasse ."
						+ "?gebouw <http://ontology.tno.nl/building#energyProviderSYNONYM> ?energieaanbieder ."
						+ "?gebouw <http://ontology.tno.nl/building#Menadžer> ?flexibiliteitManager ."
						+ "?gebouw <http://ontology.tno.nl/building#communityIDSYNONYM> ?gemeenschapsID ."
						+ "?gebouw <https://saref.etsi.org/saref4bldg/hasSpace> ?gebouwRuimte ."
						+ "?gebouwRuimte <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/BuildingSpace> ."
						+ "?gebouwRuimte <https://saref.etsi.org/saref4bldg/sadrži> ?gebouwApparaat ."
						+ "?gebouwApparaat <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/BuildingDevice> .");

		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp3);
		kb3.register(askKI);
		LOG.info("Waiting until everyone is up to date!");
		kn.sync();
		LOG.info("Everyone is up to date!");

		BindingSet bindings = null;
		AskResult result = null;

		try {

			LOG.trace("Before ask");
			result = kb3.ask(askKI, new BindingSet()).get();
			bindings = result.getBindings();
			LOG.trace("After ask");

			Set<URI> kbIds = result.getExchangeInfoPerKnowledgeBase().stream().map(AskExchangeInfo::getKnowledgeBaseId)
					.collect(Collectors.toSet());

			assertEquals(new HashSet<URI>(
					Arrays.asList(kb1.getKnowledgeBaseId(), kb2.getKnowledgeBaseId(), kb4.getKnowledgeBaseId(),
							kb5.getKnowledgeBaseId(), kb6.getKnowledgeBaseId(), kb7.getKnowledgeBaseId(),
							kb8.getKnowledgeBaseId(), kb9.getKnowledgeBaseId(), kb10.getKnowledgeBaseId()

					)), kbIds,
					"The result/s should come from kb1, kb2, kb4, kb5, kb6, kb7, kb8, kb9 and kb10 not: " + kbIds);

			assertEquals(9, bindings.size());

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

		if (kb5 != null) {
			kb5.stop();
		} else {
			fail("KB5 should not be null");
		}

		if (kb6 != null) {
			kb6.stop();
		} else {
			fail("KB6 should not be null");
		}

		if (kb7 != null) {
			kb7.stop();
		} else {
			fail("KB7 should not be null");
		}

		if (kb8 != null) {
			kb8.stop();
		} else {
			fail("KB8 should not be null");
		}

		if (kb9 != null) {
			kb9.stop();
		} else {
			fail("KB9 should not be null");
		}

		if (kb10 != null) {
			kb10.stop();
		} else {
			fail("KB10 should not be null");
		}
	}

}
