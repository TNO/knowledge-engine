How is reasoning used
=====================

This section explains how reasoning is used in the knowledge engine. We can distinguish between two types of reasoning as it happens within the knowledge engine: 1) reasoning to infer new data and 2) reasoning for orchestration of data exchange.

Graph Patterns
--------------

Reasoning to infer new data
---------------------------
This type of reasoning is what is usually meant when talking about a reasoning in the context of the Semantic Web. In such a scenario there is a collection of facts and a collection of rules and the reasoner uses the facts and rules to infer new facts. The original facts are called the _asserted facts_ and the derived facts are called the _inferred facts_ (see figure 1).

![alt text](./img/asserted-vs-inferred.png)*Figure 1: the difference between asserted and inferred facts.*

In the figure you see two databases with triples. The left database only contains asserted facts and whenever a user asks a question to this database, it answer will be sought within these asserted facts. In the right database you still see the asserted facts, but this time a collection of inferred facts on top of them is also available. These inferred facts are derived by the reasoner from the asserted facts by applying a set of rules. Now, whenever a user asks a question to the database on the right, the answer is not only sought in the asserted facts, but in both the asserted and inferred facts.

Let's give an example. Imagine a smart home scenario where lights have a state of either 'on' or 'off'. In triples this looks like:

```
:light1 rdf:type :Light .
:light1 :hasState :on .
```

Now imagine all the on/off states of all the lights in the home are maintained as asserted facts in a database. The home owner uses an app to check which lights she left on in her home and this app checks the on/off states using the asserted facts in the database. This all works perfect and the home owner buys and installs a new _dimmable_ light. Unfortunately, the app does not check whether the new dimmable light is on or off, since a dimmable light is not either on/off, but has an light intensity between 0 and 100. So, its triples look like:

```
:light2 rdf:type DimmableLight .
:light2 :hasState "23"^^xsd:integer .
```

Now, an update of the app to also check intensity levels of dimmable lights could solve this problem, but updates do not always happen. By using the reasoning capabilities, this problem can also be solved without any updates to the app. This solution would involve telling the reasoner that a DimmableLight (which is also a Light) is on if its light intensity level is larger than 0. Such a rule (which is typically part of the Knowledge Model) looks like:

```
if
	?light :rdf:type DimmableLight .
	?light :hasState ?state .
	largerThan(?state > 0)
then
	?light :hasState :on .
else
	?light :hasState :off .
end
```

Now, the (legacy) app is able to check the dimmable light's on/off state. This can also be called 'forward compatibility', i.e. a legacy app is compatible with future developments.

Reasoning to orchestrate data exchange
--------------------------------------------
This type of reasoning is less obvious and requires some explanation. It is particularly useful in a scenario where data is scattered amongst heterogeneous knowledge bases. Instead of periodically transforming data from each of those knowledge bases into a uniform format and collecting it in a central database, this orchestration method allows the data to stay at its source and only retrieve those facts whenever they are needed.

For the reasoner to orchestrate this, it requires an overview of all the currently available knowledge bases and their capabilities. These capabilities are called KnowledgeIOs (see also [Conceptual framework](#conceptual-framework)) and each knowledge base typically has multiple of them. Each KnowledgeIO represents a single capability of a knowledge base and describes this capability using only the concepts and relations defined in the common Knowledge Model. Our assumption is that every capability of a possible knowledge base (i.e. a machine learning model, user app, database or service) can be described in such a way. For this a KnowledgeIO defines either an input knowledge or an output knowledge or both. For example, a user app that presents knowledge to its user in a table would define a KnowledgeIO in which only the input knowledge is defined. This should be read as: the app needs input data for it to function properly.

With this overview of the available capabilities, the reasoner is able to answer questions about knowledge that is scattered over multiple knowledge bases. This works as follows. For every KnowledgeIO of every available Knowledge Base, the Smart Connector determines whether it is relevant and if so, it updates its state accordingly. This state consists of a collection of rules that represent the available capabilities and whenever the reasoner applies such a rule to answer a certain question (i.e. using backward reasoning), during the execution of the rule the relevant Knowledge Base is contacted and the data is retrieved on the fly.

For example, there are three Knowledge Bases called App, Measurements and Temperature Converter. The scenario is that the App gives the user access to all available measurements in degrees Fahrenheit. However, the Measurements Knowledge Base only stores measurements in degrees Celcius. The idea is that the reasoner is able to use the Temperature Converter Knowledge Base to convert the availble measurements in degrees Celcius into the requested measurements in degrees Fahrenheit. The KnowledgeIOs of the different Knowledge Bases are of the following:

```

App KnowledgeIO:
  * input: ?meas rdf:type saref:Measurement . ?meas saref:tempInFahrenheit ?temp .
  * output: <empty>

Measurements KnowledgeIO:
  * input: <empty>
  * output: ?m rdf:type saref:Measurement . ?m saref:tempInCelcius ?t .
  * requestable: true
  * subscribable: false

Temperature Converter KnowledgeIO:
  * input: ?mm rdf:type saref:Measurement . ?mm saref:tempInFahrenheit ?tf .
  * output:  ?mm rdf:type saref:Measurement . ?mm saref:tempInCelcius ?tc . 

```

Now, these KnowledgeIOs will result in the following backward rules (see also [Conceptual framework](#conceptual-framework)) in the App's Smart Connector:

```
if
	?m rdf:type saref:Measurement .
	?m saref:tempInCelcius ?t .
then
	retrieveDataFromKnowledgeBase(Measurements)
end

if
	?mm rdf:type saref:Measurement .
	?mm saref:tempInFahrenheit ?tf .
then
	?mm rdf:type saref:Measurement .
	?mm saref:tempInCelcius ?tc .
	retrieveDataFromKnowledgeBase(Temperature Converter) 
end

```

The App asks its SmartConnector for measurements in degrees Fahrenheit, but the Measurements Knowledge Base (KB) only contains measurements in degrees Celcius. The reasoner will therefore apply the backward rule of the Temperature Converter to every measurement that the Measurements KB returns. So, the Measurements KB returns the following RDF:

```
:m1 rdf:type saref:Measurement .
:m1 saref:tempInCelcius "21" .

:m2 rdf:type saref:Measurement .
:m2 saref:tempInCelcius "18" .

:m3 rdf:type saref:measurement .
:m3 saref:tempInCelcius "24" .
```

The Temperature Converter KB is able to convert this into:

```
:m1 rdf:type saref:Measurement .
:m1 saref:tempInCelcius "69.8" .

:m2 rdf:type saref:Measurement .
:m2 saref:tempInCelcius "64.4" .

:m3 rdf:type saref:measurement .
:m3 saref:tempInCelcius "75.2" .
```

Which is the data that can be returned by the Smart Connector to the App KB as the answer to its query. But data exchange does not only involve asking questions and getting answers (i.e. pulling data), it often also entails publishing data to subscribers (i.e. pushing data). If we modify the Measurements KB of the above example into a Temperature sensor that periodically publishes the latest measurement in degrees Celcius. The KnowledgeIOs of the App and Temperature Converter KBs remain the same and the Temperature Sensor KB has the following KnowledgeiO:

```
Temperature Sensor KnowledgeIO:
  * input: <empty>
  * output: ?m rdf:type saref:Measurement . ?m saref:tempInCelcius ?t .
  * requestable: false
  * subscribable: true
```

Note that the only difference with the KnowledgeIO of the Measurements KB is that the output knowledge is no longer requestable, but only subscribable. So, the `requestable` and `subscribable` attribute of a KnowledgeIO indicates whether the output data of the KB can be pulled by other KBs and/or whether it is automatically pushed to other KBs. In the case of the Temperature Sensor it is automatically pushed. This Temperature Sensor KnowledgeIO results in the following *forward* rule in the Smart Connector of the Temperature Sensor:

```
if
	?m rdf:type saref:Measurement .
	?m saref:tempInCelcius ?t .
then
	sendDataToOtherKnowledgeBases()
end
```

This means that whenever the Temperature Sensor publishes a new measurement, it will get pushed to subscribed KBs (in this case the App KB). The Smart Connector of the App KB will receive this measurement where its reasoner works with the following *forward* rules:

```

if
	?mm rdf:type saref:Measurement .
	?mm saref:tempInCelcius ?tc .
then
	retrieveDataFromKnowledgeBase(Temperature Converter)
	?mm rdf:type saref:Measurement .
	?mm saref:tempInFahrenheit ?tf .
end

if
	?m rdf:type saref:Measurement .
	?m saref:tempInFahrenheit ?t .
then
	sendDataToKnowledgeBase()
end

```

Now, upon receiving the new measurement the rule for the Temperature Converter will trigger and convert the measurement into Fahrenheit. Once this is done, the new Measurement in degrees Fahrenheit will be send tot the App KB by the other rule. The App can now update its GUI with the latest measured temperature in degrees Fahrenheit.

From these example it becomes clear that a reasoner can also be used to orchestrate data exchange between different Knowledge Bases. 
