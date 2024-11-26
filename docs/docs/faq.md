---
  sidebar_position: 99
---


FAQ
===

### What is the time of validity of the exchanged information?
The validity of the exchanged information is not available by default, but you can include it into the graph pattern if you need it for your use case.

### Are there any technical requirements, e.g. RAM, CPU and disk space, for deploying the Knowledge Engine?
We do not have minimal requirements for the Knowledge Engine yet.
Locally we can run multiple Smart Connectors on an Intel Core i7-8650 CPU @ 1.9GHz with 16 Gb RAM, but the requirements also depend on the amount of data that is exchanged.
An instance of the Smart Connector is self-contained and no external database or storage is required.

For setting up your own Knowledge Network, including a Knowledge Directory, typically the following steps are required:
1. Have a machine or virtual machine ready:
	* Ideally configure the (virtual) machine to be in a DMZ network, separate from other critical/sensitive resources (good security practice)
2. Deploy the Knowledge Engine and Knowledge Directory on the machine, i.e. deploy two Java servers.
3. Configure the firewall to allow external communication to that (virtual) machine.
   * Depending on the local infrastructure, configure a proxy (if it exists) to forward the requests to that (virtual) machine.

A medium range (virtual) machine with the following requirements should be sufficient to set up your own Knowledge Network:
* (Ideally) Linux-based OS
* Latest Java SE installed
* Outside world (inbound) internet access
* Low/medium CPU (2 cores at least)
* 16 GB RAM (nowadays a good minimum for a server, more is better)

It is recommended that someone can access the (virtual) machine to collect any logs and troubleshoot when necessary.

### How does the Knowledge Engine deal with privacy-sensitive information?
The Knowledge Engine (and the Smart Connector) functions as a serving hatch and does not store any data that is being exchanged.
All this data is stored in the Knowledge Bases which are responsible for protecting privacy-sensitive data.
Within the Knowledge Engine we distinguish between *graph patterns* and *binding sets*.
The graph patterns should not contain any privacy-sensitive data since they are part of the meta-data that is being stored as capability descriptions.
This information is needed to orchestrate the data exchange.
These graph patterns might also show up in the logs of the smart connector (at all log levels).
The binding sets, on the other hand, _can_ contain privacy-sensitive data which will not be stored.
Binding Sets are also not stored in the log files of Smart Connectors if these have a log level of INFO or higher.
Keep in mind, though, that the Knowledge Engine functions as an intelligent broker between consumers and producers of knowledge.
This might cause the Knowledge Engine to exchange your data with unexpected parties within the Knowledge Network.
So make sure your knowledge network only contains trusted KBs.

### Can we use SPARQL keywords such as FILTER in Knowledge Interactions?
No, SPARQL keywords are not available.
We do not use SPARQL, because SPARQL is only usable for a question/answer interactions, while we also support publish/subscribe and function call interactions.
Although keywords such as FILTER are very useful keywords, and we would love to support something like that, there need to be equivalent options in the reasoner and most of the time this is not there.
We do keep this in mind when looking for/making a new reasoner, but do not expect this to be available anytime soon (there is still research required).
Note that a Knowledge Interaction is more than a single Basic Graph Pattern (although it is the most important part of it).
It also has a type (Ask/Answer or Post/React) and a Communicative Act (to convey the 'reason' for the interaction).

Take for example the following SPARQL query:
```sparql
SELECT ?sensor WHERE {
?building a saref4bldg:Building.
?building bot:containsElement ?sensor.
?multisensor saref:consistsOf ?motionsensor.
?vibrationSensor a IC:MotionSensor.
?vibrationSensor saref:hasState ?state .
}
```
We can use the Basic Graph Pattern from the WHERE-clause to create a Knowledge Interaction.
We will then also need to specify the type of interaction, e.g. ASK, and the Communicative Act.


###  Why do our two Knowledge Bases not exchange data even though they have matching graph patterns?
In this case, typically the error is in the Knowledge Interactions that you expect to match.
Two Knowledge Interactions match when:
* The types match
* The graph patterns match
* The communicative acts match

In the table below 'yes' means those two types of Knowledge Interactions match, while 'no' means those two types of Knowledge Interactions do not match.

|               |                                         |     POST                |                                         |     ASK    |
|---------------|-----------------------------------------|-------------------------|-----------------------------------------|------------|
|               |                                         |     *only argument GP*    |     *both argument      and result GP*    |         |
|     <b>REACT</b>     |     *only argument* GP                    |     yes                 |     no                                  |     n/a    |
|               |     *both argument      and result GP*    |     no                  |     yes                                 |     n/a    |
|     <b>ANSWER</b>    |                                      |     n/a                 |     n/a                                 |     yes    |

When your two Knowledge Interaction types have a 'yes', then you can take a look at whether the graph patterns match.

Two graph patterns match when every triple of the first graph pattern is also in the second graph pattern and vice versa.
The ordering and names of variables like `?s` are ignored.
Note that in case of POST and REACT Knowledge Interactions, both the argument graph pattern and the result graph pattern must match.

If you are sure that the graph patterns match (be careful of typos!), check which communicative acts they use.
The CommunicativeAct is meant to indicate the 'reason' for the interaction and in most cases the “InformPurpose” is sufficient, and therefore it is the default communicative act of every registered Knowledge Interaction.
Whenever the Knowledge Engine wants to exchange data it compares the sender Knowledge Interaction’s communicative act with the recipient Knowledge Interaction’s communicative act and if they ‘match’ the data will be exchanged.
If both Knowledge Bases use the REST API to register Knowledge Interactions and do not specify the communicative act, they will be able to exchange data.
However, when they _do_ specify the communicative act when registering a Knowledge Interaction, they should be compatible.


### How do we get all possible graph patterns and their associated Knowledge Interactions that we need to register?
Within a specific use case or setting, the Knowledge Interactions and their graph patterns are the end result of the ontology engineering process.
In this process you need to decide what data you want to exchange, and which ontologies you want to use for this.
You can also build your own ontology, though we recommend reusing ontologies where possible as it's beneficial for the interoperability with other systems.


### Do you send ACKs for all requests?
There are no explicit ACKs.
The Ask receives an answer of one or more KBs and the Post receives the BindingSet for the optional Result Graph Pattern.
If you need an explicit ACK, you can put it in the result graph pattern of a REACT Knowledge Interaction.


### Can we restrict the results of a Knowledge Interaction?
The first way to restrict a result is to limit which Knowledge Bases are contacted.
When executing an interaction, you can specify a single knowledge base.
In this case, it will only contact that specific knowledge base to try and answer your query.

The second way to restrict a Knowledge Interaction is to use literals.
This can be done either in the graph pattern of the registered Knowledge Interaction _or_ in the binding set when executing an interaction.
This way you will only receive data related to that literal.

But what if you want to make this more general?
Take for example a person with a name and an email address.
You may want to be able to request the email address for a person with a certain name, but do not want to provide the name for a specific email address.

In this case, if logic does not allow the inverse, then you should not use an Ask/Answer Knowledge Interactions with a graph pattern like:
```sparql
?person :hasUsername ?userName .
?person :hasEmailaddress ?emailAddress .
```

Instead, you want to use the Post/React Knowledge Interactions.
These have two Graph Patterns and the *argument* graph pattern would look something like:

```sparql
?person :hasUsername ?userName .
```

and the *result* graph pattern would look something like:

```sparql
?person :hasEmailaddress ?emailAddress .
```
This way the Knowledge Engine can send a username and receive the email address, but it cannot do the inverse.


### How do the reactive Knowledge Interactions compare to a publish-subscribe broker?
The Knowledge Engine functions similarly to a publish-subscribe broker.
The React Knowledge Interaction can be seen as a *subscribe* and the Post Knowledge Interaction can be seen as a *publish*.
All *matching* React Knowledge Interactions will receive the Post Knowledge Interaction's data.

Aside from this interaction pattern, POST/REACT can also be used for an interaction pattern similar to function calls.
The POST/REACT Knowledge Interactions support an (optional) result graph pattern that describes what data is sent back (the results) after receiving data (the arguments).
The results from all *matching* REACTs are aggregated before being returned to the POST side.

### Inconsistent or missing results with POST/REACT
Users are sometimes surprised that their POST Knowledge Interaction is not returning with results.
This situation typically occurs when there are multiple Knowledge Bases available that can answer or want to react to a Knowledge Interaction.
The Knowledge Engine will aggregate all results before returning and thus delays in answers are possible.
If your Knowledge Base has not received a request for data, it may be waiting until others have answered or reacted.
If your Knowledge Base has received a request but the results are not returned, it is likely because the Knowledge Engine is waiting for the results of other matching Knowledge Bases.
We have an [issue](https://github.com/TNO/knowledge-engine/issues/109) which would allow you to instruct the Knowledge Engine to not wait indefinitely for an answer, but this is still on our todo list.
Until then, we recommend using more specific graph patterns.

We have also seen the following situation:
* POST requests were sent
* The corresponding Knowledge Base with a corresponding REACT did not always receive this
* When receiving a POST, the REACT side sent a confirmation properly
* The Knowledge Base with the POST rarely received the confirmation from the REACT
* There were 2 REACT Knowledge Interactions with the same argument graph pattern but different result graph patterns.

In this setting, there were 2 REACT Knowledge Interactions with the same argument graph pattern but different result graph patterns.
One result graph pattern matched the POST, but the other did not.
This prevented the REACT interaction to react to the POST and thus the POST never got a response.
So if you have the same argument graph pattern for several interactions, be careful that *all* REACTs and *all* POSTs use the same result graph pattern


### We see a spike in memory usage whenever an ASK or POST is executed. What is happening?
Most likely you have enabled the reasoner when you created a Smart Connector for your Knowledge Base.
When an ASK or POST is executed, the Knowledge Engine will use the reasoner to infer new data and orchestrate the data exchange (for more details see [Reasoning](./reasoning.md)).
When you have large graph patterns and/or many bindings, the reasoner's time and memory consumption can be quite large.
If you have no need for this reasoning capability, you can limit its resource usage by disabling the reasoner.
When using the REST Developer API, you can disable the reasoner by setting the JSON property `reasonerEnabled` to `false` or leave the property out altogether because by default the reasoner is disabled.


### How to deal with a query-like Knowledge Base where you can specify what properties you want to be returned?
Some users use a data source where you can specify what kind of results you expect back via an input field.
A common example we have seen is a query-like API request where you can specify what properties should be returned, e.g. for a device:

```json
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
                        ]
                    ]
                }
            ]
        }
    ]
}
```
Naturally, in this case you want to put other data in a Knowledge Interaction depending on which fields are selected.
The difficulty is, however, that a Knowledge Interaction is predefined and not dynamic.

There are several ways to tackle this.
The first option is to register a lot of Knowledge Interactions that cover all the possibilities.
The second option is to register one single graph pattern that covers all properties and to enable the reasoner.
Alternatively, you can instantiate an ASK with a generalized graph pattern like:
```sparql
?deviceId <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/core/Device> .
?deviceId <https://saref.etsi.org/core/makesMeasurement> ?m .
?m <https://saref.etsi.org/core/relatesToProperty> ?p . 
?p <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?fieldType .
?m <https://saref.etsi.org/core/hasTimestamp> ?ts .
?m <https://saref.etsi.org/core/hasValue> ?val .
```
This allows the asking side to provide a binding set that specifies which properties should be returned (in this case 'fieldType' for a specific 'device').
```json
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
The ANSWER side will need to parse this correctly (which is not trivial) and fill the binding set correctly.

### Any thoughts on the scalability of the single long polling connection used by each Smart Connector?
Currently, each Smart Connector uses a single long polling connection to receive all interactions from the Knowledge Engine.
The Knowledge Engine is event-based and multithreaded, and while not designed to handle enormous amounts of data, this has not been a limiting factor in our use cases so far.

If the current setup, a Smart Connector with a single long polling connection, is limiting for you, there are several ways to circumvent this:
1) Use the _Java_ Developer API.
   It uses handlers and is multithreaded, so it is scales better than the _REST_ Developer API.
2) Divide Knowledge Interactions over multiple Smart Connectors.
   This allows you to have a single long polling connection per Knowledge Interaction.


*Question*: I have a question about how the compliance checker and reasoner are supposed to work. **If this is the wrong place for asking this, please direct me to the right people!**

An example: the following graph pattern defines a **device** and the **type** of it’s **property**.

```sparql
?device <https://saref.etsi.org/core/measuresProperty> ?property .
?property a ?propertyType .
```

The only parameters we need here are **device** and **propertyType**. The **property** variable is redundant, but it still needs to be in the graph pattern. What we decided to do, is replace this variable by a placeholder individual : `http://interconnectproject.eu/pilots/greek/property#property`. The resulting pattern would then look like this:

```sparql
?device <https://saref.etsi.org/core/measuresProperty> <http://interconnectproject.eu/pilots/greek/property#property> .
<http://interconnectproject.eu/pilots/greek/property#property> a ?propertyType .
```

I’m wondering if referring to individuals that are not in any ontology, in **subjects** and **objects**, will cause problems with compliance or reasoning?

I guess my question is more specifically: “Will the compliance checker look at **subjects** and **objects** in graph patterns, or only at predicates? And will the reasoner be able to handle these kinds of structures?”

- *Answer*: I think it helps if we distinguish between syntax and semantics here. Using a pattern like:

	```sparql
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
	[Image no longer available]

	Note:
	- that there are 6 triples in the above graph pattern, but only 5 edges in this figure. This is caused by the fact that the 1st and 3rd triple of the graph pattern above are exactly the same and triples can only occur once in a graph. So, if you write them twice, they will still only occur once. 
	- the effect that using a fixed individual greek:property in the graph pattern is causing; it makes it semantically impossible to determine that sensor1 is measuring property Motion and Smoke, while sensor2 is measuring Energy!

	Conclusion: while the fixed property is syntactically correct, it is semantically incorrect. So, using a fixed property in the current version of the KE (with a matcher instead of a reasoner) will probably work fine and the data is exchanged as expected. This is, however, probably not the case with the future version of the KE with reasoner, because the reasoner will actually semantically interpret the graph pattern and bindingset and when you ask something like:

	```sparql
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

- *Question*: In the context of graph pattern matching, can you explain the subset/superset condition: "when the Graph Pattern" of the sender is a superset of the Graph Pattern of the receiver' ? Is it at definition level or at data level? I found (at ontology level) the following explanation: "Ontology O1 is a subset of ontology O2 if all definitions in O1 are contained in O2 (O2 is the superset of O1)".
1) More concrete: is 'a b c .' graph pattern a subset or a superset of 'a b c . d e f.' ?
2) In case of Post/React knowledge interactions, both argument graph patterns must match and also both result graph patterns must match?
3) In case of Post/React knowledge interactions graph pattern matching, is the POST side regarded as the sender for the argument graph pattern and the REACT side as the sender for the result graph pattern?
4) Let's assume the REACT side result pattern is like 'a b c . d e f.' and the POST side result pattern is 'a b c .'. Is this allowed?  So it is similar to a 'SELECT' in SQL? The result binding set at the POST side is then also reduced, I assume (not in number of _records_, but _fields_).
- *Answer*: We do not have defined the subset/superset terms within the context of the Knowledge Engine and Graph Patterns, but it would indeed be helpful to do so. Since the idea of the Knowledge Engine is that the *data* is always kept at the source and is only retrieved when necessary for an interaction, it is more suitable to talk about subset/superset at the definition level and not at the data level. This is because all data is simply not available in a single location. The definition level is also the level where currently the *matching* between graph patterns happens. The *matcher* (as opposed to the *reasoner*) does not support subset/superset matching.

	1) I would say graph pattern `a b c` is a subset of the graph pattern `a b c . d e f`. Note that graph pattern typically contain variables like `?a`. Note that graph pattern matching ignores variable names and triple order.
	2) Yes, the argument graph pattern and result graph pattern should both match if two Post/React Knowledge Interactions want to exchange data. Note that this probably changes when the Knowledge Engine uses a reasoner instead of a matcher.
	3) Yes, the PostKnowledgeInteraction sends the argument and the ReactKnowledgeInteraction sends the (optional) result.
	4) Currently, this will not work, because we are using a graph pattern *matcher* instead of a *reasoner*. I expect the reasoner to indeed allow them to interact if the POST side result pattern is a subset of the REACT side result pattern. In that case the result binding set at the POST side should also be a subset (in fields) of the binding set given from the REACT side. So, the results are always given to a Knowledge Base in its own terminology, this already happens by translating the variable names, but should also happen in the way you describe once the reasoner is active.

*Question*: I’m trying to understand how timeseries as part of a larger graph pattern are expressed in a binding set.

For instance:
Let's say we have some graph pattern like:

```sparql
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

In the ANSWER Knowledge interaction will the binding set be something like:
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
Can this IRI then be used to for example ask more info about `<https://www.example.org/measurement-43>` ? That would mean that the service specific adapter should use an ID that is stored, and not some temporal/volatile ID for the IRI. Because that means that you can give a reference to an object in the answers. Of course you have to provide then a graph pattern to allow the retrieval of this object  resulting in another binding set.
- *Answer*: You are correct with respect to `<https://www.example.org/measurement-43>`. Ideally you should be able to retrieve more information about it and the service should not randomly generate it, but use a stored id.
