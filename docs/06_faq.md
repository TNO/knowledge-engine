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
By the way, the current version (0.1.2) of the KE is a centralized one where all the Smart Connectors (with their reasoner) run on a server hosted by INESC TEC and partners create and access their Smart Connector via the REST Developer API of the generic adapter. A future version of the generic adapter will contain an instance of the Smart Connector (plus reasoner).
Currently, an instance of the Smart Connector is self contained, so no external database is required. The future version will of course need to use the network (ports are not yet decided) to communicate with other Smart Connectors (and the Knowledge Directory).

*Question*: A Knowledge Interaction (KI) is a basic graph pattern. I suppose that this means just a sequence of triple patterns. Does this mean that for example the ‘FILTER’ keyword can’t be used in the KI?
- *Answer*: Indeed, the FILTER keyword (of the SPARQL language) is not available. Although this is a very useful keyword and we would love to support something like that, there needs to be an equivalent of filtering in the reasoner and most of the time this is not there. We do keep this in mind when looking/making for a new reasoner, but I do not expect this to be available anytime soon (there is still research required, I think). Note that a Knowledge Interaction is more than a single Basic Graph Pattern (although it is the most important part of it). It also has a type (Ask/Answer or Post/React) and a Communicative Act (to convey the 'reason' for the interaction). Also, the Post/React KI have two graph patterns attached to them; the argument and the result graph pattern.

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

*Question*: In POST /sc/ki one registers a Knowledge Interaction along with the knowledge interaction type. In POST /sc/ask one queries for some results by referring to a KI and providing an incomplete binding set. The result will be a complete binding set. In your presentation KE for dummies slide 12, you mentioned that one could restrict the question (at the react side). I didn’t find in the rest of the slides on how one can do that, except by having a literal in the registered KI. In your example a person has a name and a email address, but the logic only allows to ask for the email address associated with a person with a certain name, but it does not allow to get the name associated with a specific email address. How do we impose such a restriction, or we can’t do this at this stage?

- *Answer*: If the logic does not allow the inverse, then you should not use an Ask/Answer Knowledge Interactions with a graph pattern like:
	```
	?person :hasUsername ?userName .
	?person :hasEmailaddress ?emailAddress .
	```

	In that case you want to use the Post/React Knowledge Interactions. These have two Graph Patterns and the *argument* graph pattern would look something like:

	```
	?person :hasUsername ?userName .
	```

	and the *result* graph pattern would look something like:

	```
	?person :hasEmailaddress ?emailAddress .
	```

	This tells the Knowledge Engine that you cannot send it an email address and receive the username.

*Question*: Can you explain how to register the argument pattern and the result graph pattern? In the KE API I saw only one graph pattern in the register of a Knowledge interaction, and no parameter to indicate if it is an argument pattern or a result graph pattern.
- *Answer*: In the Java Developer API the constructors of the [PostKnowledgeInteraction](https://gitlab.inesctec.pt/interconnect/knowledge-engine/-/blob/0.1.2/smart-connector/src/main/java/eu/interconnectproject/knowledge_engine/smartconnector/api/PostKnowledgeInteraction.java#L45) and [ReactKnowledgeInteraction](https://gitlab.inesctec.pt/interconnect/knowledge-engine/-/blob/0.1.2/smart-connector/src/main/java/eu/interconnectproject/knowledge_engine/smartconnector/api/ReactKnowledgeInteraction.java#L41) objects require both an argument and a result graph pattern.

	In the JSON body of the [REST Developer API ](https://gitlab.inesctec.pt/interconnect/knowledge-engine/-/blob/0.1.2/rest-api/src/main/resources/openapi-sc.yaml) `POST /sc/ki` operation, you specific the type of the Knowledge Interaction. If you choose the PostKnowledgeInteraction or ReactKnowledgeInteraction `knowledgeInteractionType`, the argument and result graph patterns are also expected (see also the schema of the request body):

	```
	{
	"knowledgeInteractionType": "PostKnowledgeInteraction",
	"argumentGraphPattern": "?s ?p ?o",
	"resultGraphPattern": "?x ?y ?z"
	}
	```

	Note that the result graph pattern is optional.

*Question*: In the context of graph pattern matching, can you explain the subset/superset condition: "when the Graph Pattern" of the sender is a superset of the Graph Pattern of the receiver' ? Is it at definition level or at data level? I found (at ontology level) the following explanation: "Ontology O1 is a subset of ontology O2 if all definitions in O1 are contained in O2 (O2 is the superset of O1)".
1) More concrete: is 'a b c .' graph pattern a subset or a superset of 'a b c . d e f.' ?
2) In case of Post/React knowledge interactions, both argument graph patterns must match and also both result graph patterns must match?
2) In case of Post/React knowledge interactions graph pattern matching, is the POST side regarded as the sender for the argument graph pattern and the REACT side as the sender for the result graph pattern? 
3) Let's assume the REACT side result pattern is like 'a b c . d e f.' and the POST side result pattern is 'a b c .'. Is this allowed?  So it is similar to a 'SELECT' in SQL? The result binding set at the POST side is then also reduced, I assume (not in number of _records_, but _fields_).
- *Answer*: We do not have defined the subset/superset terms within the context of the Knowledge Engine and Graph Patterns, but it would indeed be helpful to do so. Since the idea of the Knowledge Engine is that the *data* is always kept at the source and is only retrieved when necessary for an interaction, it is more suitable to talk about subset/superset at the definition level and not at the data level. This is because all data is simply not available in a single location. The definition level is also the level where currently the *matching* between graph patterns happens. The *matcher* (as opposed to the *reasoner*) does not support subset/superset matching.

	1) I would say graph pattern `a b c` is a subset of the graph pattern `a b c . d e f`. Note that graph pattern typically contain variables like `?a`. Note that graph pattern matching ignores variable names and triple order.
	2) Yes, the argument graph pattern and result graph pattern should both match if two Post/React Knowledge Interactions want to exchange data. Note that this probably changes when the Knowledge Engine uses a reasoner instead of a matcher.
	3) Yes, the PostKnowledgeInteraction sends the argument and the ReactKnowledgeInteraction sends the (optional) result.
	4) Currently, this will not work, because we are using a graph pattern *matcher* instead of a *reasoner*. I expect the reasoner to indeed allow them to interact if the POST side result pattern is a subset of the REACT side result pattern. In that case the result binding set at the POST side should also be a subset (in fields) of the binding set given from the REACT side. So, the results are always given to a Knowledge Base in its own terminology, this already happens by translating the variable names, but should also happen in the way you describe once the reasoner is active.

*Question*: I successfully created smart connector (https://cybergrid.com/kb1") and the knowledge Interaction. When I wanted to execute the ask command with the following body:
```
[
  {
   "deviceName": "device1" 
  }
]
```
I received the following expectation from the knowladge-engine: ```400 Bad Request: ['device1' is not an unprefixed URI or literal.]```
- *Answer*: The reason your request fails is because variable bindings need to be either RDF Literals or IRIs. See also our [documentation](https://gitlab.inesctec.pt/interconnect/knowledge-engine/-/blob/0.1.2/docs/03_java_developer_api.md#bindings).


	If you change your example value from 'device1' to something like '<http://www.example.org/device1>', this particular error should be resolved.

*Question*: Do we need a long polling connection for every Knowledge Interaction? Doesn't that get very complicated?
- *Answer*: No, per Smart Connector (or Knowledge Base) you need a single long polling connection to receive all interactions from the Knowledge Engine. Do remember that this long polling connection is returned with status code 202 every 29 seconds and also needs to be reestablished after you receive data via it.
