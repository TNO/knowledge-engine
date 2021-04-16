FAQ
===

*Question*: may be a question of dummies, what is the time of validity of the information? is it specified in the graph pattern?
- *Answer*: The validity of the information is not available by default, but you can include it into the graph pattern if you need it for your use case.


*Question*: There is a limited and defined number of KI. How do we proceed to "generate" all possible graph patterns and so associated KI ?
- *Answer*: The KIs used should match and the KIs and their Graph Patterns are the end result of the SAREFization process for services. Also, if you need pilot specific KIs, you can agree on them within your pilot using SAREF or a pilot specific ontology.

*Question*: what about ACKs of the requests?
- *Answer*: There are no explicit ACKs. The Ask receives an answer of one or more KBs and the Post receives the BindingSet for the optional Result Graph Pattern. If you need an explicit ACK (for example with the turn on the light example in the presentation) you can use the result Graph Pattern to contain the ACK.

*Question*: Is there any specific requirements such as RAM, CPU and disk space for deploying the KE ? Is there aditional components to take into account such as an external DB or it is all inclusive ? Is there OS specific configuration such as network or ports ?
- *Answer*: We do not have minimal requirements for the KE yet. Locally I am running multiple Smart Connectors just fine on my Intel Core i7-8650 CPU @ 1.9GHz with 16 Gb RAM, but it also depends on the amount of traffic of course.
By the way, the current version (0.1.0) of the KE is a centralized one where all the Smart Connectors (with their reasoner) run on a server hosted by INESC TEC and partners create and access their Smart Connector via the REST Developer API of the generic adapter. A future version of the generic adapter will contain an instance of the Smart Connector (plus reasoner).
Currently, an instance of the Smart Connector is self contained, so no external database is required. The future version will of course need to use the network (ports are not yet decided) to communicate with other Smart Connectors (and the Knowledge Directory).
*Question*: A Knowledge Interaction (KI) is a basic graph pattern. I suppose that this means just a sequence of triple patterns. Does this mean that for example the ‘FILTER’ keyword can’t be used in the KI?
- *Answer*: Indeed, the FILTER keyword (of the SPARQL language) is not available. Although this is a very useful keyword and we would love to support something like that, there needs to be an equivalent of filtering in the reasoner and most of the time this is not there. We do keep this in mind when looking/making for a new reasoner, but I do not expect this to be available anytime soon (there is still research required, I think). Note that a Knowledge Interaction is more than a single Basic Graph Pattern (although it is the most important part of it). It also has a type (Ask/Answer or Post/React) and a Communicative Act (to convey the 'reason' for the interaction). Also, the Post/React KI have to graph patterns attached to them; the argument and the result graph pattern.
*Question*: It means also that a SparQL query like you see below is a SparQL query and is not something that can be used at the KE REST API interface. Only the part within the brackets is a basic graph pattern. Is that right?
```
SELECT ?sensor WHERE {
?building a saref4bldg:Building.
?building bot:containsElement ?sensor.
?multisensor saref:consistsOf ?motionsensor.
?vibrationSensor a IC:MotionSensor.
?vibrationSensor saref:hasState ?state .
}
```
- *Answer*: Exactly, the WHERE part contains the Basic Graph Patterns and those are used to create the Knowledge Interactions. We do not use SPARQL, because SPARQL is only usable for a question/answer interactions, while the Interoperability layer should also support publish/subscribe and function call interactions.
