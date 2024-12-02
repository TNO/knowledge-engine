package eu.knowledge.engine.smartconnector.impl;

import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeGap;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.mergeGaps;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMergeGaps {
    private static final Logger LOG = LoggerFactory.getLogger(TestMergeGaps.class);

    @Test
    public void testMergeWithEmptyList() {
        Set<KnowledgeGap> gaps1 = new HashSet<>();
        TriplePattern tp1 = new TriplePattern("?a rdf:type <https://saref.etsi.org/core/v3.2.1/#saref:Sensor>");
        TriplePattern tp2 = new TriplePattern("?a ex:madeBy \"Sophie\"");
        KnowledgeGap gap = new KnowledgeGap();
        gap.add(tp1);
        gap.add(tp2);
        gaps1.add(gap);

        Set<KnowledgeGap> mergedGaps = mergeGaps(new HashSet<>(), gaps1);
        LOG.info("Merged empty list with knowledge gaps: " + mergedGaps.toString());
        assertEquals(gaps1, mergedGaps);
    }

    @Test
    public void testMergeWithoutAdding() {
        Set<KnowledgeGap> gaps1 = new HashSet<>();
        TriplePattern tp1 = new TriplePattern("?a rdf:type <https://saref.etsi.org/core/v3.2.1/#saref:Sensor>");
        TriplePattern tp2 = new TriplePattern("?a ex:madeBy \"Sophie\"");
        KnowledgeGap gap = new KnowledgeGap();
        gap.add(tp1);
        gap.add(tp2);
        gaps1.add(gap);

        Set<KnowledgeGap> mergedGaps = mergeGaps(gaps1, new HashSet<>());
        LOG.info("Merged knowledge gaps with empty list: " + mergedGaps.toString());
        assertEquals(gaps1, mergedGaps);
    }

    @Test
    public void testMergeWithSingleSubjectMatch() {
        Set<KnowledgeGap> gaps1 = new HashSet<>();
        TriplePattern tp1 = new TriplePattern("?a rdf:type <https://saref.etsi.org/core/v3.2.1/#saref:Sensor>");
        TriplePattern tp2 = new TriplePattern("?a ex:madeBy \"Sophie\"");
        KnowledgeGap gap = new KnowledgeGap();
        gap.add(tp1);
        gap.add(tp2);
        gaps1.add(gap);

        Set<KnowledgeGap> toAdd = new HashSet<>();
        TriplePattern tp = new TriplePattern("\"Sensor1\" rdf:type <https://saref.etsi.org/core/v3.2.1/#saref:Sensor>");
        KnowledgeGap gap2 = new KnowledgeGap();
        gap2.add(tp);
        toAdd.add(gap2);

        Set<KnowledgeGap> mergedGaps = mergeGaps(gaps1, toAdd);

        Set<KnowledgeGap> expectedGaps = new HashSet<>();
        KnowledgeGap expectedGap = new KnowledgeGap();
        expectedGap.add(tp);
        expectedGap.add(tp2);
        expectedGaps.add(expectedGap);
        LOG.info("Merged knowledge gaps with single triple whose subject matches: " + mergedGaps.toString());
        assertEquals(expectedGaps, mergedGaps);
    }

    @Test
    public void testMergeWithSinglePredicateMatch() {
        Set<KnowledgeGap> gaps1 = new HashSet<>();
        TriplePattern tp1 = new TriplePattern("\"Sensor1\" ?p <https://saref.etsi.org/core/v3.2.1/#saref:Sensor>");
        TriplePattern tp2 = new TriplePattern("?a ex:madeBy \"Sophie\"");
        KnowledgeGap gap = new KnowledgeGap();
        gap.add(tp1);
        gap.add(tp2);
        gaps1.add(gap);

        Set<KnowledgeGap> toAdd = new HashSet<>();
        TriplePattern tp = new TriplePattern("\"Sensor1\" rdf:type <https://saref.etsi.org/core/v3.2.1/#saref:Sensor>");
        KnowledgeGap gap2 = new KnowledgeGap();
        gap2.add(tp);
        toAdd.add(gap2);

        Set<KnowledgeGap> mergedGaps = mergeGaps(gaps1, toAdd);

        Set<KnowledgeGap> expectedGaps = new HashSet<>();
        KnowledgeGap expectedGap = new KnowledgeGap();
        expectedGap.add(tp);
        expectedGap.add(tp2);
        expectedGaps.add(expectedGap);
        LOG.info("Merged knowledge gaps with single triple whose predicate matches: " + mergedGaps.toString());
        assertEquals(expectedGaps, mergedGaps);
    }

    @Test
    public void testMergeWithSingleObjectMatch() {
        Set<KnowledgeGap> gaps1 = new HashSet<>();
        TriplePattern tp1 = new TriplePattern("\"Sensor1\" rdf:type ?a");
        TriplePattern tp2 = new TriplePattern("?a ex:madeBy \"Sophie\"");
        KnowledgeGap gap = new KnowledgeGap();
        gap.add(tp1);
        gap.add(tp2);
        gaps1.add(gap);

        Set<KnowledgeGap> toAdd = new HashSet<>();
        TriplePattern tp = new TriplePattern("\"Sensor1\" rdf:type <https://saref.etsi.org/core/v3.2.1/#saref:Sensor>");
        KnowledgeGap gap2 = new KnowledgeGap();
        gap2.add(tp);
        toAdd.add(gap2);

        Set<KnowledgeGap> mergedGaps = mergeGaps(gaps1, toAdd);

        Set<KnowledgeGap> expectedGaps = new HashSet<>();
        KnowledgeGap expectedGap = new KnowledgeGap();
        expectedGap.add(tp);
        expectedGap.add(tp2);
        expectedGaps.add(expectedGap);
        LOG.info("Merged knowledge gaps with single triple whose object matches: " + mergedGaps.toString());
        assertEquals(expectedGaps, mergedGaps);
    }

    @Test
    public void testMergeWithSubjectAndPredicateMatch() {
        Set<KnowledgeGap> gaps1 = new HashSet<>();
        TriplePattern tp1 = new TriplePattern("?a rdf:type <https://saref.etsi.org/core/v3.2.1/#saref:Sensor>");
        TriplePattern tp2 = new TriplePattern("?a ex:madeBy \"Sophie\"");
        KnowledgeGap gap = new KnowledgeGap();
        gap.add(tp1);
        gap.add(tp2);
        gaps1.add(gap);

        Set<KnowledgeGap> toAdd = new HashSet<>();
        TriplePattern tp = new TriplePattern("\"Sensor1\" ?b <https://saref.etsi.org/core/v3.2.1/#saref:Sensor>");
        KnowledgeGap gap2 = new KnowledgeGap();
        gap2.add(tp);
        toAdd.add(gap2);

        Set<KnowledgeGap> mergedGaps = mergeGaps(gaps1, toAdd);

        Set<KnowledgeGap> expectedGaps = new HashSet<>();
        KnowledgeGap expectedGap1 = new KnowledgeGap();
        expectedGap1.add(tp1);
        expectedGap1.add(tp2);
        expectedGaps.add(expectedGap1);
        KnowledgeGap expectedGap2 = new KnowledgeGap();
        expectedGap2.add(tp);
        expectedGap2.add(tp2);
        expectedGaps.add(expectedGap2);
        LOG.info("Merged knowledge gaps with single triple whose subject and predicate match: " + mergedGaps.toString());
        assertEquals(expectedGaps, mergedGaps);
    }

    @Test
    public void testMergeWithSubjectAndObjectMatch() {
        Set<KnowledgeGap> gaps1 = new HashSet<>();
        TriplePattern tp1 = new TriplePattern("?a rdf:type <https://saref.etsi.org/core/v3.2.1/#saref:Sensor>");
        TriplePattern tp2 = new TriplePattern("?a ex:madeBy \"Sophie\"");
        KnowledgeGap gap = new KnowledgeGap();
        gap.add(tp1);
        gap.add(tp2);
        gaps1.add(gap);

        Set<KnowledgeGap> toAdd = new HashSet<>();
        TriplePattern tp = new TriplePattern("\"Sensor1\" rdf:type ?c");
        KnowledgeGap gap2 = new KnowledgeGap();
        gap2.add(tp);
        toAdd.add(gap2);

        Set<KnowledgeGap> mergedGaps = mergeGaps(gaps1, toAdd);

        Set<KnowledgeGap> expectedGaps = new HashSet<>();
        KnowledgeGap expectedGap1 = new KnowledgeGap();
        expectedGap1.add(tp1);
        expectedGap1.add(tp2);
        expectedGaps.add(expectedGap1);
        KnowledgeGap expectedGap2 = new KnowledgeGap();
        expectedGap2.add(tp);
        expectedGap2.add(tp2);
        expectedGaps.add(expectedGap2);
        LOG.info("Merged knowledge gaps with single triple whose subject and object match: " + mergedGaps.toString());
        assertEquals(expectedGaps, mergedGaps);
    }


    @Test
    public void testMergeWithPredicateAndObjectMatch() {
        Set<KnowledgeGap> gaps1 = new HashSet<>();
        TriplePattern tp1 = new TriplePattern("\"Sensor1\" ?b <https://saref.etsi.org/core/v3.2.1/#saref:Sensor>");
        TriplePattern tp2 = new TriplePattern("?a ex:madeBy \"Sophie\"");
        KnowledgeGap gap = new KnowledgeGap();
        gap.add(tp1);
        gap.add(tp2);
        gaps1.add(gap);

        Set<KnowledgeGap> toAdd = new HashSet<>();
        TriplePattern tp = new TriplePattern("\"Sensor1\" rdf:type ?c");
        KnowledgeGap gap2 = new KnowledgeGap();
        gap2.add(tp);
        toAdd.add(gap2);

        Set<KnowledgeGap> mergedGaps = mergeGaps(gaps1, toAdd);

        Set<KnowledgeGap> expectedGaps = new HashSet<>();
        KnowledgeGap expectedGap1 = new KnowledgeGap();
        expectedGap1.add(tp1);
        expectedGap1.add(tp2);
        expectedGaps.add(expectedGap1);
        KnowledgeGap expectedGap2 = new KnowledgeGap();
        expectedGap2.add(tp);
        expectedGap2.add(tp2);
        expectedGaps.add(expectedGap2);
        LOG.info("Merged knowledge gaps with single triple whose predicate and object match: " + mergedGaps.toString());
        assertEquals(expectedGaps, mergedGaps);
    }


    @Test
    public void testMergeWithCompleteTripleMatch() {
        Set<KnowledgeGap> gaps1 = new HashSet<>();
        TriplePattern tp1 = new TriplePattern("\"Sensor1\" ?b <https://saref.etsi.org/core/v3.2.1/#saref:Sensor>");
        TriplePattern tp2 = new TriplePattern("\"Sensor1\" ex:madeBy \"Sophie\"");
        KnowledgeGap gap = new KnowledgeGap();
        gap.add(tp1);
        gap.add(tp2);
        gaps1.add(gap);

        Set<KnowledgeGap> toAdd = new HashSet<>();
        TriplePattern tp = new TriplePattern("?a rdf:type ?c");
        KnowledgeGap gap2 = new KnowledgeGap();
        gap2.add(tp);
        toAdd.add(gap2);

        Set<KnowledgeGap> mergedGaps = mergeGaps(gaps1, toAdd);

        Set<KnowledgeGap> expectedGaps = new HashSet<>();
        KnowledgeGap expectedGap1 = new KnowledgeGap();
        expectedGap1.add(tp1);
        expectedGap1.add(tp2);
        expectedGaps.add(expectedGap1);
        KnowledgeGap expectedGap2 = new KnowledgeGap();
        expectedGap2.add(tp);
        expectedGap2.add(tp2);
        expectedGaps.add(expectedGap2);
        LOG.info("Merged knowledge gaps with single triple whose elements (S-P-O) all match: " + mergedGaps.toString());
        assertEquals(expectedGaps, mergedGaps);
    }
}
