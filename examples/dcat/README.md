# DCAT example
This example demonstrates how the Knowledge Bases (KBs) within a Knowledge Network can be exposed as a [DataService Catalog](https://www.w3.org/TR/vocab-dcat-3/) (DCAT). We reuse the docker compose file from the [multiple runtimes example](../multiple-runtimes/) and add a KB that gathers the metadata about the KBs using DCAT terminology. With specific domain knowledge the KE reasoner is able to automatically transform KE metadata into DCAT catalogue data. The following table shows how Knowledge Engine concepts are mapped to DCAT concepts.

| KE | DCAT |
|----|------|
| ke:KnowledgeBase | dcat:DataService |
| ke:hasName | dcterms:title |
| ke:hasDescription | dcterms:description |

## meta-kb
We use a modified [asking_kb](../common/asking_kb/asking_kb.py) to enable ASKing and printing for DCAT metadata. We use DCAT specific rules derived from the [Knowledge Engine Ontology]{https://github.com/TNO/knowledge-engine/blob/master/smart-connector/src/main/resources/knowledge-engine-ontology.ttl}. We could also use the following (more generic) RFDS rules, but these currently are too slow and require too much memory:

```
// DCAT facts
-> (ke:KnowledgeBase rdfs:subClassOf dcat:DataService ) .
-> (ke:hasName skos:exactMatch dcterms:title ) .
-> (ke:hasDescription skos:exactMatch dcterms:description ) .

// RDFS rules
[exactMatch: (?i ?p1 ?v) (?p1 skos:exactMatch ?p2) -> (?i ?p2 ?v)]
[subClass: (?i rdf:type ?t1) (?t1 rdfs:subClassOf ?t2) -> (?i rdf:type ?t2)]
```

After building & starting the example with `docker compose build` and `docker compose up -d`, you can see the `meta-kb` in action by watching its log: `docker compose logs -f meta-kb`

After a while you should see the following valid DCAT RDF appear in the log:

```
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix dcat: <http://www.w3.org/ns/dcat#> .
@prefix dcterms: <http://purl.org/dc/terms/> .
@prefix ex: <http://example.org/> .

ex:ke_catalog rdf:type dcat:Catalog .
ex:ke_catalog dcterms:title "Knowledge Engine DCAT Catalog" .

<http://example.org/kb2> rdf:type dcat:DataService .
<http://example.org/kb2> dcterms:title "kb2" .
<http://example.org/kb2> dcterms:description "kb2" .
ex:ke_catalog dcat:service <http://example.org/kb2> .

<http://example.org/kb1> rdf:type dcat:DataService .
<http://example.org/kb1> dcterms:title "kb1" .
<http://example.org/kb1> dcterms:description "kb1" .
ex:ke_catalog dcat:service <http://example.org/kb1> .

<http://example.org/kb3> rdf:type dcat:DataService .
<http://example.org/kb3> dcterms:title "kb3" .
<http://example.org/kb3> dcterms:description "kb3" .
ex:ke_catalog dcat:service <http://example.org/kb3> .
```
