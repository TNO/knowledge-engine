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
By the way, the current version (0.1.6) of the KE is a centralized one where all the Smart Connectors (with their reasoner) run on a server hosted by INESC TEC and partners create and access their Smart Connector via the REST Developer API of the generic adapter. A future version of the generic adapter will contain an instance of the Smart Connector (plus reasoner).
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
- *Answer*: In the Java Developer API the constructors of the [PostKnowledgeInteraction](https://gitlab.inesctec.pt/interconnect/knowledge-engine/-/blob/0.1.6/smart-connector/src/main/java/eu/interconnectproject/knowledge_engine/smartconnector/api/PostKnowledgeInteraction.java#L45) and [ReactKnowledgeInteraction](https://gitlab.inesctec.pt/interconnect/knowledge-engine/-/blob/0.1.6/smart-connector/src/main/java/eu/interconnectproject/knowledge_engine/smartconnector/api/ReactKnowledgeInteraction.java#L41) objects require both an argument and a result graph pattern.

	In the JSON body of the [REST Developer API ](https://gitlab.inesctec.pt/interconnect/knowledge-engine/-/blob/0.1.6/rest-api/src/main/resources/openapi-sc.yaml) `POST /sc/ki` operation, you specific the type of the Knowledge Interaction. If you choose the PostKnowledgeInteraction or ReactKnowledgeInteraction `knowledgeInteractionType`, the argument and result graph patterns are also expected (see also the schema of the request body):

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
3) In case of Post/React knowledge interactions graph pattern matching, is the POST side regarded as the sender for the argument graph pattern and the REACT side as the sender for the result graph pattern? 
4) Let's assume the REACT side result pattern is like 'a b c . d e f.' and the POST side result pattern is 'a b c .'. Is this allowed?  So it is similar to a 'SELECT' in SQL? The result binding set at the POST side is then also reduced, I assume (not in number of _records_, but _fields_).
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
- *Answer*: The reason your request fails is because variable bindings need to be either RDF Literals or IRIs. See also our [documentation](https://gitlab.inesctec.pt/interconnect/knowledge-engine/-/blob/0.1.6/docs/03_java_developer_api.md#bindings).


	If you change your example value from 'device1' to something like '<http://www.example.org/device1>', this particular error should be resolved.

*Question*: Do we need a long polling connection for every Knowledge Interaction? Doesn't that get very complicated?
- *Answer*: No, per Smart Connector (or Knowledge Base) you need a single long polling connection to receive all interactions from the Knowledge Engine. Do remember that this long polling connection is returned with status code 202 every 29 seconds and also needs to be reestablished after you receive data via it.

*Question*: I’m trying to understand how timeseries as part of a larger graph pattern are expressed in a binding set.

For instance:
Let's say we have some graph pattern like:

```
?timeseries rdf:type ex:Timeseries .
?timeseries ex:hasMeasurement ?measurement .
?measurement rdf:type saref:Measurement .
?measurement saref:hasFeatureOfInterest ?room .
?room rdf:type saref:Room .
?measurement saref:observedProperty saref:Temperature .
?measurement saref:hasSimpleResult ?temperature .
?measurement ex:hasTimestamp ?ts .
```

And the timeseries returns an array of temperature values and timestamp for each value.

In the ANWSER Knowledge interaction will the binding set be something like:
```json
[
	{
		"timeseries": "<https://www.example.org/timeseries-sensora-b-23>",
		"measurement": "<https://www.example.org/measurement-42>",
		"room": "<https://www.example.org/kitchen>",
		"temperature": "\"21.2\"^^<http://www.w3.org/2001/XMLSchema#float>",
		"ts": "\"2020-10-16T22:00Z\"^^some_timestamp_type"
	},
	{
		"timeseries": "<https://www.example.org/timeseries-sensora-b-23>",
		"measurement": "<https://www.example.org/measurement-43>",
		"room": "<https://www.example.org/kitchen>",
		"temperature": "\"21.4\"^^<http://www.w3.org/2001/XMLSchema#float>",
		"ts": "\"2020-10-16T23:00Z\"^^some_timestamp_type"
	},
	{
		"timeseries": "<https://www.example.org/timeseries-sensora-b-23>",
		"measurement": "<https://www.example.org/measurement-44>",
		"room": "<https://www.example.org/kitchen>",
		"temperature": "\"21.6\"^^<http://www.w3.org/2001/XMLSchema#float>",
		"ts": "\"2020-10-16T24:00Z\"^^some_timestamp_type"
	}
]
```

Is the following statement right: the IRI is filled in by the service specific adapter?
Can this IRI then be used to for example ask more info about <https://www.example.org/measurement-43> ? That would mean that the service specific adapter should use an ID that is stored, and not some temporal/volatile ID for the IRI. Because that means that you can give a reference to an object in the answers. Of course you have to provide then a graph pattern to allow the retrieval of this object  resulting in another binding set.
- *Answer*: You are correct with respect to `<https://www.example.org/measurement-43>`. Ideally you should be able to retrieve more information about it and the service should not randomly generate it, but use a stored id.


*Question*: I'm wondering if the reactive Knowledge Interactions function the same way that a publish/subscribe broker would work. Is it possible for one service to POST something to the KE, while multiple other services listen to the interaction. This way they all receive the data they need whenever it is available. 

If I understand correctly, a REACT KI can also send data back upon receiving it. If publish/subscribe is possible, how would this communication work? Would the Knowledge Base on the POST side receive all responses?
- *Answer*: Yes, it functions similarly to a publish/subscribe broker. The React Knowledge Interaction can be seen as a *subscribe* and the Post Knowledge Interaction can be seen as a *publish*. All *matching* React Knowledge Interactions will receive the Post Knowledge Interaction's data. 
	For a more functional interaction pattern, the Post/React Knowledge Interaction also support a (optional) result graph pattern that describes the data that will be sent back (the results) after receiving data (the arguments). The result will indeed be aggregated and returned to POST side.

*Question*: It's not clear for me how the interaction between the service store and the KE should be seen. In our case we have a system acting as a knowledgebase requesting information for other partners. This information will then be combined and using some specific application logic new information will be exposed. Both will be implemented through the knowledge engine. By looking at the examples I managed to implement a small application which is working fine but still I have some questions:

The registration of the KB and KIs should only be done once I suppose?
Let's say we have a ASK-ANSWER (our KB will create answer) and a POST-REACT KI (our KB will react). Will we need to restart the connection with the KE? I thought something was mentioned during the sync call that an interaction is only active for 30 mins.
Is there a complete example available where the KE and the service store is combined?
- *Answer*: In general, the exact link between service store and knowledge engine is still evolving and under discussion. Currently, as far as I know, this link is that the service store keeps a list of services with metadata that have a generic adapters that also provide access to the Interoperability layer (i.e. Knowledge Engine).

  - The registration of the KB and KIs should only be done once I suppose?
    - Typically, the Generic Adapter (and Smart Connector) should be available as long as your system (Knowledge Base) is available and in that case you only need to register your KIs once. KIs are, however, dynamic and can be added and removed if this is useful for the use case.
  - Let's say we have a ASK-ANSWER (our KB will create answer) and a POST-REACT KI (our KB will react). Will we need to restart the connection with the KE? I thought something was mentioned during the sync call that an interaction is only active for 30 mins.
    - The Knowledge Engine REST Developer API uses long-polling to notify you when your KB needs to react. This long-polling connection will automatically return every *29 seconds* with status code 202 to prevent certain proxies from blocking it. So, you need to reestablish this long-polling connection when you receive a 202. This does not affect the Knowledge Base and Knowledge Interactions.
  - Is there a complete example available where the KE and the service store is combined?
    - I think @aleksandar.tomcic.vizlore.com is working on examples that use the generic-adapter which maintains the link between the Service Store and the Knowledge Engine. For the Knowledge Engine only, we do have an very simple Python example available here: https://gitlab.inesctec.pt/interconnect/ke-python-examples

*Question*: I use a very generic graph pattern like `?s ?p ?o` for my PostKnowledgeInteraction, but my other KB does not get a request. Or, you do get a request, but your post is not returning with the results.
- *Answer*: We noticed multiple knowledge bases that register graph patterns like "?s ?p ?o" (i.e. from the examples we provided). If this is the case, it might occur that you ask a question or post some data and there are multiple KBs available that can answer or want to react to that type of data (i.e. they use a matching graph pattern). This means that you may not receive a request for data on your KB until one of the others has answered or reacted, or you might get a request, but you do not see the expected reaction in your other KB, because the Interoperability layer is waiting for the other matching KBs to answer/react.
We have an issue #95 which would allow you to instruct the Knowledge Engine to not wait indefinitely for an answer, but this is still on our todo list. Until then, we recommend using more specific graph patterns for testing. For example:
{
  "knowledgeInteractionType": "ReactKnowledgeInteraction",
  "argumentGraphPattern": "?s <http://inetum.world/hasValue> ?o ."
}

*Question*: In a Ask/Answer interaction the bindingSet is a list of items. The items should stay in the order there are inserted on the Answer side. Currently it is not the case.
- *Answer*:  The Knowledge Engine cannot guarantee the ordering of the Bindings in a  BindingSet due to several reasons. From a Semantic Technology perspective, the bindings are stored in a binding set in which the ordering is not fixed. Also, due to the matchers (and future reasoners) that mediate between different smart connectors and the fact that the response can be a union from BindingSets received from different knowledge bases, it is difficult to guarantee any ordering. Ideally, the ordering of the bindings can be derived from data, for example an ordering value or timestamps. This would allow the data to be sorted after receiving it from the interoperability layer.

*Question*: Some partners use a query like API request whereby they can specify via an input field what kind of results they expect back, together with the device id (temperature sensor, smart plug, meter,...)
For instance one can select the list of parameters (status, temperature, power, voltage, door_state, humidity, luminosity etc.) that should be returned.
In the result below is only the power_total returned, but additional fields could be selected/set in the input field (depending on the device type it can return more than one value)
This is a quite generic approach. So for this one API call there will be a lot of KIs, correct or can this realized with one KI?
Is the best approach to create a KI graph pattern per device type that returns all parameters?
What if I'm only interested in one parameter (not possible now because an exact match is required in this version, but possible in a next version)?
Result:

```
{
    "results": [
        {
            "statement_id": 0,
            "series": [
                {
                    "name": "mqtt_consumer",
                    "columns": [
                        "time",
                        "power_total"
                    ],
                    "values": [
                        [
                            "2021-05-12T08:53:30.052924161Z",
                            241.63
                        ],
                        [
                            "2021-05-12T09:03:00.050337173Z",
                            119.67
                        ]
                    ]
                }
            ]
        }
    ]
}
```

- *Answer*: This is indeed quite a generic approach that, unfortunately, cannot be done with the current version of the KE (as you already correctly mention: because of exact matching). You could in theory register a lot of Knowledge Interactions, although I am not sure that is the best approach. If there is a limited set of fields that are always available, I would recommend providing a single large knowledge interaction. This would, however, mean that the asker registers this large knowledge interaction as well.
	An alternative approach, which maybe mimics the generic behaviour of the API, could be to provide a measurement graph pattern like:

	```
	?deviceId <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/core/Device> .
	?deviceId <https://saref.etsi.org/core/makesMeasurement> ?m .
	?m <https://saref.etsi.org/core/relatesToProperty> ?p . 
	?p <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?fieldType .
	?m <https://saref.etsi.org/core/hasTimestamp> ?ts .
	?m <https://saref.etsi.org/core/hasValue> ?val .
	```

	This would allow the asking side to provide a binding set with a particular deviceId and 'fieldTypes':

	```
	[
		{
			"deviceId": "<https://www.example.org/device1>",
			"fieldType": "<https://saref.etsi.org/saref4ener/PowerMax>"
		},
		{
			"deviceId": "<https://www.example.org/device1>",
			"fieldType": "<https://saref.etsi.org/saref4ener/PowerMin>"
		}
	]
	```

	The answer side would need to parse this correctly (which is not trivial) and fill the bindingset correctly.
	When we use a reasoner instead of a matcher, the ask side would not need to use the large graph pattern, but only those fieldTypes that it is interested in. The reasoner would still call the answer side, but limit the results to what the ask side needs.
	So, there are several ways to handle this and each approach has advantages and disadvantages. Unfortunately, there is not a single best practice to solve this.

*Question*: Have there been any discussions about availability and scalability of knowledge engine? Seeing as a knowledge base can only have a single polling connection, it does not seem possible to spin up multiple instances of a reactive adapter to the same knowledge base. This would limit the scalability potential by quite a lot. Are there any workarounds around this perhaps?
	- *Answer*: Although scalability and availability have been mentioned several times now, there has not been any thorough discussions about them. One reason for this is that other things have been our priority in the last couple of months. This also means that the Knowledge Engine has not been designed to handle enormous amounts of data (although it is event-based and multi-threaded), but we expect it to be good enough for most use cases. The exact limitations with respect to throughput and latency will probably become clearer in the comings months and since we have not been really concerned with performance I expect there is also still room for improvement.

	Regarding multiple instances of a reactive adapter, we advice a single smart connector per knowledge base and each knowledge base is indeed limited to a single long polling connection. There are several ways to circumvent this limitation:
	1) consider using the _Java_ Developer API (not sure if the generic adapter will provide this in a future version). It uses handlers and is multi threaded, so it is much better scalable than the _REST_ Developer API.
	2) divide Knowledge Interactions over multiple Smart Connectors. This would allow you to have a single long polling connection per Knowledge Interaction.

	B.T.W.: We did not choose HTTP as the protocol in order to support web-server like scalability, but because most partners were familiar with it and the tooling and specification is very accessible.

*Question*: I can hardly figure how data are exchanged between services in the Interworking layer architecure. When a service A ask for data form another service (B) through the KE (for example give me the temperature setpoint for this household) it is not clear if the final data (ie the temprature setpoint) is sent directly from Servica A Endpoint to Service B Endpoint or if it transit through the Interworking layer infrastructure.

	- *Answer*: If two services A and B want to exchange data in an interoperable manner, they both should use interoperability layer. They first register their capabilities (using the Ask and Answer Knowledge Interactions) and then the actual data exchange can happen. This data exchange is orchestrated by the interoperability layer and, so, the data transits through the interoperability layer. Service A asks the temperature set point to its Smart Connector and the interoperability layer will contact the Smart Connector of Service B to retrieve the answer from its Service B and sends the result back.

	For more information about the Knowledge Engine and Knowledge Interactions, see the recorded workshop on our shared drive: https://drive.inesctec.pt/f/16182787

*Quesiton*: We noticed that we cannot get consistent results when testing a POST/REACT exchange, both locally and with a partner. So to summarize:
* we can send a POST request
* the REACT sides don't receive 100% of the time (more like 25%), whether it is locally running or from our partner's platform
* When receiving the POST, the REACT side sends the confirmation properly
* The POST side rarely receives the confirmation from REACT (around 5% of the time)
	
- *Answer*: We were doing tests with 2 REACT KB that had different result graph patterns. One matched our post but not the other. This prevented the react process to react to the post and so the post never got a response. So to remember to work properly: For the same argument graph pattern, ALL reacts and ALL posts need the same answer graph pattern. Also: the post will receive the answers from the react once they have all answered. The answers will be aggregated into one.

*Question*: I have a question about how the compliance checker and reasoner are supposed to work. **If this is the wrong place for asking this, please direct me to the right people!**

An example: the following graph pattern defines a **device** and the **type** of it’s **property**.

```
?device <https://saref.etsi.org/core/measuresProperty> ?property .
?property a ?propertyType .
```

The only parameters we need here are **device** and **propertyType**. The **property** variable is redundant, but it still needs to be in the graph pattern. What we decided to do, is replace this variable by a placeholder individual : `http://interconnectproject.eu/pilots/greek/property#property`. The resulting pattern would then look like this:

```
?device <https://saref.etsi.org/core/measuresProperty> <http://interconnectproject.eu/pilots/greek/property#property> .
<http://interconnectproject.eu/pilots/greek/property#property> a ?propertyType .
```

I’m wondering if referring to individuals that are not in any ontology, in **subjects** and **objects**, will cause problems with compliance or reasoning?

I guess my question is more specifically: “Will the compliance checker look at **subjects** and **objects** in graph patterns, or only at predicates? And will the reasoner be able to handle these kinds of structures?”

- *Answer*: I think it helps if we distinguish between syntax and semantics here. Using a pattern like:

	```
	?device <https://saref.etsi.org/core/measuresProperty> <http://interconnectproject.eu/pilots/greek/property#property> .
	<http://interconnectproject.eu/pilots/greek/property#property> a ?propertyType .
	```

	is syntactically correct and it should be accepted by both the validator and reasoner. The difficulty here is with the semantics that it expresses. Imagine the following bindingset:

	| ?device   | ?propertyType |
	|-----------|---------------|
	| \<sensor1\> | saref:Motion  |
	| \<sensor1\> | saref:Smoke   |
	| \<sensor2\> | saref:Energy  |

	If we combine the above Graph Pattern with the above BindingSet, we get the following RDF triples (I am a bit inconsistent with the prefixes):

	1.	`<sensor1> <https://saref.etsi.org/core/measuresProperty> <http://interconnectproject.eu/pilots/greek/property#property> .`
	2.	`<http://interconnectproject.eu/pilots/greek/property#property> a saref:Motion .`

	3.	`<sensor1> <https://saref.etsi.org/core/measuresProperty> <http://interconnectproject.eu/pilots/greek/property#property> .`
	4.	`<http://interconnectproject.eu/pilots/greek/property#property> a saref:Smoke .`

	5.	`<sensor2> <https://saref.etsi.org/core/measuresProperty> <http://interconnectproject.eu/pilots/greek/property#property> .`
	6.	`<http://interconnectproject.eu/pilots/greek/property#property> a saref:Energy .`

	Now, if we draw these triples as a graph figure, we get something like the following:

	![fixed_iri_in_gp](/uploads/6cbb12403ebb8610271df081961cb368/fixed_iri_in_gp.png)

	Note:
	- that there are 6 triples in the above graph pattern, but only 5 edges in this figure. This is caused by the fact that the 1st and 3rd triple of the graph pattern above are exactly the same and triples can only occur once in a graph. So, if you write them twice, they will still only occur once. 
	- the effect that using a fixed individual greek:property in the graph pattern is causing; it makes it semantically impossible to determine that sensor1 is measuring property Motion and Smoke, while sensor2 is measuring Energy!

	Conclusion: while the fixed property is syntactically correct, it is semantically incorrect. So, using a fixed property in the current version of the KE (with a matcher instead of a reasoner) will probably work fine and the data is exchanged as expected. This is, however, probably not the case with the future version of the KE with reasoner, because the reasoner will actually semantically interpret the graph pattern and bindingset and when you ask something like:

	```
	?device <https://saref.etsi.org/core/measuresProperty> <http://interconnectproject.eu/pilots/greek/property#property> .
	<http://interconnectproject.eu/pilots/greek/property#property> a saref:Energy .
	```

	(note that ?propertyType has been substituted with saref:Energy)

	This graph pattern represents the query: “give me all devices that measure energy” and it will answer with the following bindingset:

	| ?device   |
	|-----------|
	| \<sensor1\> |
	| \<sensor2\> |

	Which is not correct and is caused by the fixed  property. So, there are two solutions here. The practical one, which I think happens quite a lot, is for the ontology to provide an individual per property. So, you would have a `interconnect:motionIndividual`, `interconnect:energyIndividual` and `interconnect:smokeIndividual`. These can be used instead of the greek:property and will make sure that it remains semantically correct. A less practical (and philosophically debatable) one is to have a unique property individual for every property a device measures. So, you would get something like `<sensor1/individual/motion>`, `<sensor1/individual/smoke>` and `<sensor2/individual/energy>` and even more for all other devices.

	And last but not least, a short reaction to Georg’s remark: “I think it makes sense to think about the GP as the body of a query”. It certainly does, although within the context of the Knowledge Engine graph patterns are also used for non-query like interactions.

*Question*: Which are the technical requirements needed to host the KE at pilot level for the next 22 months? Do you have an estimation about hosting/processing needs? And from the technical support, any idea about the effort that might require? 
- *Answer*: From a technical perspective what needs to be done is:
	1. Have a machine or virtual machine ready (spec discussion separately);
	1.1. Ideally configure the machine/virtual machine to be in a DMZ network, separate from other critical/sensitive resources (for the sake of prevention)
	2. Deploy the KE and Knowledge Directory in the machine i.e., deploy two java servers. 
	3. Configure firewall to allow external communication to that machine/virtual machine
	3.1 Depending on the local infrastructure, configure a proxy (if it exists) to forward the requests to that machine/virtual machine.
	4. Communicate me the public IP address where the KE is now available (so that we can use the project domain to identify that IP). 
	5. If the proxy used uses a wildcard TLS certificate it can protect the KE instance (that is what we are doing in the cloud instance). If the wildcard certificate is not available, we need to acquire one and deploy it together with the KE.

	These are essentially the steps. These steps are standard, so any technical colleague should be able to do it. 

	As for the technical requirements of the machine, we will collect that info and provide it to you. But a medium range machine/virtual machine should do it. 
	Requirements:
	- (ideally) Linux based OS , e.g., latest ubuntu
	- Latest Java SE installed
	- Outside world (inbound) internet access
	- Low/medium CPU (2 core at least) 
	- 16 GB RAM (nowadays a good minimum for a server. if if has more, better).

	Finally someone should be able to access that machine to collect any logs and trouble shoot whenever necessary.
*Question*: How does the Knowledge Engine deal with privacy sensitive information?
- *Answer*: The Knowledge Engine (and the Smart Connector) functions as a serving hatch and does not store any data that is being exchanged. All this data is stored in the Knowledge Bases and the KB are responsible for protecting privacy sensitive data. Within the Knowledge Engine we distinguish between *graph patterns* and *binding sets*. The graph patterns should not contain any privacy sensitive data since they are part of the meta data that is being stored as capability descriptions. This information is needed to orchestrate the data exchange. These graph patterns might also show up in the logs of the smart connector (at all log levels). The binding sets, on the other hand, _can_ contain privacy sensitive data which will not be stored. Binding Sets are also not stored in the log files of Smart Connectors if these have a log level of INFO or higher. Keep in mind, though, that the Knowledge Engine functions as an intelligent broker between consumers and producers of knowledge. This might cause the Knowledge Engine to exchange your data with unexpected parties within the Knowledge Network. So make sure your knowledge network only contains trusted KBs.

*Question*: Why do our two KBs not exchange data, while they should?
- *Answer*: Assuming we are talking about the Matcher (default) instead of the Reasoner. The first thing to check are the Knowledge Interactions (KIs) of the two KBs that you expect to match. For two KIs to match, both their type needs to match and their graph patterns. In the table below 'yes' means those two types of KIs match, while 'no' means those two types of KIs do not match. When your two KI types have a 'yes', you can take a look at whether the graph patterns match. Two graph patterns match when every triple of the first graph pattern is also in the second graph pattern and vice versa. The ordering and names of variables like `?s` are ignored. Note that in case of POST and REACT KIs, both the argument graph pattern and the result graph pattern must match.

|               |                                         |     POST                |                                         |     ASK    |
|---------------|-----------------------------------------|-------------------------|-----------------------------------------|------------|
|               |                                         |     *only argument GP*    |     *both argument      and result GP*    |         |
|     <b>REACT</b>     |     *only argument* GP                    |     yes                 |     no                                  |     n/a    |
|               |     *both argument      and result GP*    |     no                  |     yes                                 |     n/a    |
|     <b>ANSWER</b>    |                                      |     n/a                 |     n/a                                 |     yes    |

*Question*: When making the ASK request, the binding set is a list of datapoints where the timestamps are increasing from the first to the last element. When the Answer service receives a response, the order of the datapoints inside the binding set is completely changed.
- *Answer*: You are correct to observe that the ordering of bindings in a binding set is not guaranteed by the Knowledge Engine. The reason why we call it a binding set is, because the elements in a set are unordered. Since JSON does not support sets and we are using JSON for our REST API, we put the bindings in a JSON list ([ … ]). I can imagine this is a bit misleading and you would expect the result to keep the ordering. However, due to the nature of RDF, the ordering of the bindings in a binding set cannot be used to encode any information when exchanging data. The ordering should, thus, be encoded explicitly using numbers or (as in your case) timestamps and the receiving end (ANSWER) should use this information to put the information in the correct order.

*Question*: My KB has an ASK knowledge interaction with the same graph pattern as the ANSWER knowledge interaction of the Whirlpool KB with whom I want to exchange data, but the data exchange is not happing. What is going wrong?
- *Answer*: Double check the communicative acts that both knowledge interactions use. The CommunicativeAct is meant to indicate the 'reason' for the interaction and in most cases the “InformPurpose” is sufficient and therefore it is the default communicative act of every registered Knowledge Interaction. Whenever the KE wants to exchange data it compares the sender KI’s communicative act with the recipient KI’s communicative act and if they ‘match’ the data will be exchanged. If both KB use the REST API to register KIs and do not specify the communicative act, they will be able to exchange data. However, when they _do_ specify the communicative act when registering a KI, they should be compatible. In the case of Whirlpool the problem might be that Whirlpool explicitly configures the old default https://www.tno.nl/energy/ontology/interconnect#InformPurpose URI. This default was changed to https://w3id.org/knowledge-engine/InformPurpose in KE version `1.1.2` and sometimes prevents two knowledge bases from exchanging data.

*Question*: What should our KB do When we receive a request for data (either via an ANSWER or REACT Knowledge Interaction), but we do not have a response?
- *Answer*: You should send an empty binding set when you do not have a response. Also when your REACT Knowledge Interaction has no result graph pattern, you should always return an empty binding set to the Knowledge Engine. If an error occurs while responding, you can either return an empty BindingSet (although this does not give any information about the error occurring) or call the (in case you are using the asynchronous handler methods of the Java Developer API) `future.completeExceptionally(...)` method.

*Question*: There are lots of 'HTTP/1.1 header parser received no bytes' errors in the logs. How do I prevent these?
- *Answer*: This can be prevented by using a shorter timeout in the Java HTTP Client. Try using the following Java option: `-Djdk.httpclient.keepalive.timeout=3`. This can also be set in the docker configuration of a KER docker container by setting the JAVA_TOOL_OPTIONS as follows: `JAVA_TOOL_OPTIONS: "-Djdk.httpclient.keepalive.timeout=3"`.