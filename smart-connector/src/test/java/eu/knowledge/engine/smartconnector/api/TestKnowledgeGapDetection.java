package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.ReasonerPlan;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;

public class TestKnowledgeGapDetection {

    private static final Logger LOG = LoggerFactory.getLogger(TestKnowledgeGapDetection.class);

    private KnowledgeBaseImpl kbEggObserver;
    private KnowledgeBaseImpl kbImperialEggSearcher;
    private KnowledgeNetwork kn;
    private PrefixMappingMem prefixes;
    private AskKnowledgeInteraction askKI;
    private Set<Rule> ruleSet;

    @BeforeEach
    public void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {
        prefixes = new PrefixMappingMem();
        prefixes.setNsPrefixes(PrefixMapping.Standard);
        prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");
        addDomainKnowledge();

        instantiateImperialEggSearcherKB();
        instantiateObserverKB();

        kn = new KnowledgeNetwork();
        kn.addKB(kbEggObserver);
        kn.addKB(kbImperialEggSearcher);
        kn.sync();
    }

    private void addDomainKnowledge() {
        this.ruleSet = new HashSet<>();
        HashSet<TriplePattern> consequent1 = new HashSet<>();
        consequent1.add(new TriplePattern(
                "?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/example/ImperialFaberge>"));
        HashSet<TriplePattern> antecedent1 = new HashSet<>();
        antecedent1.add(new TriplePattern(
                "?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/example/FabergeEgg>"));
        antecedent1.add(new TriplePattern("?id <https://www.tno.nl/example/commissionedBy> \"Alexander III\""));
        antecedent1.add(new TriplePattern("?id <https://www.tno.nl/example/madeIn> \"Russia\""));
        antecedent1.add(new TriplePattern("?id <https://www.tno.nl/example/madeBy> \"House of Fabergé\""));
        this.ruleSet.add(new Rule("Domain knowledge", antecedent1, consequent1, new Rule.AntecedentToConsequentBindingSetHandler(antecedent1)));
    }

    @Test
    public void testKnowledgeGap() throws InterruptedException, ExecutionException {
        AskPlan plan = kbImperialEggSearcher.planAsk(askKI, new RecipientSelector());
        ReasonerPlan rn = plan.getReasonerPlan();
        rn.getStore().printGraphVizCode(rn);
        AskResult result = plan.execute(new BindingSet()).get();
        Set<KnowledgeGap> gaps = result.getKnowledgeGaps();

        LOG.info("Found gaps: " + gaps);

        assertEquals(1, gaps.size());

        Set<TriplePattern> expectedGap = new HashSet<>();
        expectedGap.add(new TriplePattern(prefixes, "?id ex:commissionedBy \"Alexander III\""));
        expectedGap.add(new TriplePattern(prefixes, "?id ex:madeIn \"Russia\""));
        expectedGap.add(new TriplePattern(prefixes, "?id ex:madeBy \"House of Fabergé\""));

        assertEquals(expectedGap, gaps.toArray()[0]);
    }


    @Test
    public void testKnowledgeGapNoMatchingVars() throws InterruptedException, ExecutionException {
        GraphPattern gp = new GraphPattern(prefixes,
                "?iq rdf:type <https://www.tno.nl/example/ImperialFaberge> . ?iq <https://www.tno.nl/example/madeBy> ?company . ?iq <https://www.tno.nl/example/madeIn> ?country . ?iq <https://www.tno.nl/example/hasImage> ?image .");
        this.askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp, "askImperialEggsNonMatching", false, false, true, MatchStrategy.SUPREME_LEVEL);
        kbImperialEggSearcher.register(this.askKI);
        kn.sync();

        AskPlan plan = kbImperialEggSearcher.planAsk(askKI, new RecipientSelector());
        ReasonerPlan rn = plan.getReasonerPlan();
        rn.getStore().printGraphVizCode(rn);

        AskResult result = plan.execute(new BindingSet()).get();

        Set<KnowledgeGap> gaps = result.getKnowledgeGaps();
        LOG.info("Found gaps: " + gaps);

        assertEquals(1, gaps.size());

        Set<TriplePattern> expectedGap = new HashSet<>();
        expectedGap.add(new TriplePattern(prefixes, "?id ex:madeBy \"House of Fabergé\""));
        expectedGap.add(new TriplePattern(prefixes, "?id ex:madeIn \"Russia\""));
        expectedGap.add(new TriplePattern(prefixes, "?id ex:commissionedBy \"Alexander III\""));

        assertEquals(new KnowledgeGap(expectedGap), gaps.toArray()[0]);
    }

    @Test
    public void testNoKnowledgeGap() throws InterruptedException, ExecutionException {
        GraphPattern gp = new GraphPattern(prefixes,
                "?iq rdf:type <https://www.tno.nl/example/FabergeEgg> . ?iq <https://www.tno.nl/example/hasImage> ?image .");
        this.askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp, "askImperialEggNoGap", false, false, true, MatchStrategy.SUPREME_LEVEL);
        kbImperialEggSearcher.register(this.askKI);
        kn.sync();

        AskPlan plan = kbImperialEggSearcher.planAsk(askKI, new RecipientSelector());
        ReasonerPlan rn = plan.getReasonerPlan();
        rn.getStore().printGraphVizCode(rn);

        AskResult result = plan.execute(new BindingSet()).get();

        Set<KnowledgeGap> gaps = result.getKnowledgeGaps();
        LOG.info("Found gaps: " + gaps);

        assertEquals(0, gaps.size());
    }

    @Test
    public void testKnowledgeGapWithoutPrefixes() throws InterruptedException, ExecutionException {
        GraphPattern gp = new GraphPattern("?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/example/ImperialFaberge> . ?id <https://www.tno.nl/example/hasImage> ?image .");
        this.askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp, "askImperialEggNoGap", false, false, true, MatchStrategy.SUPREME_LEVEL);
        kbImperialEggSearcher.register(this.askKI);
        kn.sync();

        AskPlan plan = kbImperialEggSearcher.planAsk(askKI, new RecipientSelector());
        ReasonerPlan rn = plan.getReasonerPlan();
        rn.getStore().printGraphVizCode(rn);

        AskResult result = plan.execute(new BindingSet()).get();
        Set<KnowledgeGap> gaps = result.getKnowledgeGaps();
        LOG.info("Found gaps: " + gaps);

        assertEquals(1, gaps.size());
        Set<TriplePattern> expectedGap = new HashSet<>();
        expectedGap.add(new TriplePattern(prefixes, "?id ex:commissionedBy \"Alexander III\""));
        expectedGap.add(new TriplePattern(prefixes, "?id ex:madeIn \"Russia\""));
        expectedGap.add(new TriplePattern(prefixes, "?id ex:madeBy \"House of Fabergé\""));
        assertEquals(expectedGap, gaps.toArray()[0]);
    }

    public void instantiateImperialEggSearcherKB() {
        kbImperialEggSearcher = new KnowledgeBaseImpl("ImperialEggSearcher");
        kbImperialEggSearcher.setReasonerEnabled(true);

        GraphPattern gp2 = new GraphPattern(prefixes,
                "?id rdf:type <https://www.tno.nl/example/ImperialFaberge> . ?id <https://www.tno.nl/example/madeBy> ?company . ?id <https://www.tno.nl/example/madeIn> ?country . ?id <https://www.tno.nl/example/hasImage> ?image .");
        this.askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2, "askImperialEggs", false, false, true, MatchStrategy.SUPREME_LEVEL);
        kbImperialEggSearcher.register(this.askKI);
        kbImperialEggSearcher.setDomainKnowledge(this.ruleSet);
    }

    public void instantiateObserverKB() {
        kbEggObserver = new KnowledgeBaseImpl("EggObserver");
        kbEggObserver.setReasonerEnabled(true);

        GraphPattern gp1 = new GraphPattern(prefixes, "?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/example/FabergeEgg> . ?id <https://www.tno.nl/example/hasImage> ?image .");
        AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1, "answerEggs");
        kbEggObserver.register(aKI, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
            assertTrue(
                    anAnswerExchangeInfo.getIncomingBindings().isEmpty()
                            || anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
                    "Should not have bindings in this binding set.");
            BindingSet bindingSet = new BindingSet();
            Binding binding1 = new Binding();
            binding1.put("id", "<https://www.tno.nl/example/Hen>");
            binding1.put("image", "\"Picture Of Hen Fabergé Egg\"^^<http://www.w3.org/2001/XMLSchema#string>");
            bindingSet.add(binding1);
            Binding binding2 = new Binding();
            binding2.put("id", "<https://www.tno.nl/example/ThirdImperial>");
            binding2.put("image", "\"Picture of Third Imperial Fabergé Egg\"");
            bindingSet.add(binding2);

            return bindingSet;
        });
    }

    @AfterEach
    public void cleanup() throws InterruptedException, ExecutionException {
        LOG.info("Clean up: {}", TestKnowledgeGapDetection.class.getSimpleName());
        kn.stop().get();
    }
}