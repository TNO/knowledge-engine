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

### How does the Knowledge Engine deal with subsets/supersets in graph patterns?
We have not defined the subset/superset terms within the context of the Knowledge Engine but this would indeed be helpful.
For ontologies, the subset/superset definition is sometimes explained as follows:
"Ontology O1 is a subset of ontology O2 if all definitions in O1 are contained in O2 (O2 is the superset of O1)".

Since the idea of the Knowledge Engine is that the *data* is always kept at the source and is only retrieved when necessary for an interaction, it is more suitable to talk about subset/superset at the definition level and not at the data level.
This is because all data is simply not available in a single location.
The definition level is also the level where currently the *matching* between graph patterns happens.
The *matcher* (as opposed to the *reasoner*) does not support subset/superset matching.

We can consider the following concrete questions when dealing with subset/superset:
1. Is `a b c.` a subset or superset of `a b c . d e f .`?
	- I would say graph pattern `a b c` is a subset of the graph pattern `a b c . d e f`.
   		Note that graph pattern typically contain variables like `?a`.
   		Graph pattern _matching_ ignores variable names and triple order.
2. When using POST/REACT, do argument and result graph patterns *both* need to match or can they be a subset/superset?
	- Yes, the argument graph pattern and result graph pattern should both match if two POST/REACT interactions want to exchange data.
   	This may change when you use the reasoner instead of the matcher.
3. Would the following interactions match? A REACT with result graph pattern `a b c. d e f.` and a POST with result graph pattern `a b c.`
	- This will not match if you are using the graph pattern _matcher_ instead of a _reasoner_.
    The reasoner would allow them to interact if the POST result pattern is a subset of the REACT result pattern.
    In that case, the result binding set at the POST side should also be a subset (in fields) of the binding set given by the REACT side.
