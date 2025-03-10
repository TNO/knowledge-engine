# Anomaly detection

This is an example of combining anomaly detection with the knowledge engine. We want to have a scenario that demonstrates/uses the following:
- using a (saref + custom) ontology to reach semantic interoperability.
- using context data (i.e. building data) about sensors to improve anomaly detection performance.
- using a converter knowledge base to convert from fahrenheit to celsius and improve the anomaly detection performance.

The example consists of 6 knowledge bases:

- `anomaly-detection-kb`: A knowledge base that represents an anomaly detection algorithm which only looks at `building1`.
  - It reacts to sensor measurements by printing the results.
- `sensor1-kb`: A knowledge base that publishes celsius temperature measurements from a Dutch sensor type.
  - It makes available the data in `KB_DATA`, using the pattern in `GRAPH_PATTERN`
  - This sensor should have the appropriate reasoner level and load RDFS rules and SAREF.
- `sensor2-kb`: A knowledge base that publishes fahrenheit measurements from a US sensor type.
  - It makes available the data in `KB_DATA`, using the pattern in `GRAPH_PATTERN`
  - This sensor should have the appropriate reasoner level and load RDFS rules and a custom ontology.
- `sensor3-kb`: A knowledge base that publishes celsius measurements from a Dutch sensor type but is contained in a different building than the other two sensors.
  - It makes available the data in `KB_DATA`, using the pattern in `GRAPH_PATTERN`
  - This sensor should have the appropriate reasoner level and load RDFS rules and a custom ontology.
  - The idea is that this sensor's data is not received by the anomaly detector.
- `converter-kb`: A knowledge base that converts fahrenheit into celsius measurements.
  - It converts bindings in the `ARGUMENT_PATTERN` form into bindings in the `RESULT_PATTERN` form, using the Python function `react` defined in `REACT_FUNCTION_DEF`.
- `building-kb`: A knowledge base that contains static building information to provide more context information to the anomaly detector.

When running the project, and showing the logs of the `anomaly-detection-kb` service:

```
docker compose up -d
docker compose logs -f anomaly-detection-kb
```

You see that it receives the measurements in celsius and from both the Dutch and US sensor type and ignores the sensor from the other building.

```
anomaly-detection-kb-1  | INFO:anomaly-detection-kb:REACT KI is handling a request...
anomaly-detection-kb-1  | INFO:anomaly-detection-kb:Reacting with empty bindingset to [{'celsius': '"20.11111111111111"^^<http://www.w3.org/2001/XMLSchema#decimal>', 'sensor': '<http://example.org/sensor2>', 'm': '<http://example.org/sensor2/measurement>'}]...
anomaly-detection-kb-1  | INFO:anomaly-detection-kb:REACT KI is handling a request...
anomaly-detection-kb-1  | INFO:anomaly-detection-kb:Reacting with empty bindingset to [{'celsius': '"15.3"^^<http://www.w3.org/2001/XMLSchema#decimal>', 'sensor': '<http://example.org/sensor1>', 'm': '<http://example.org/sensor1/measurement>'}]...
```

## Ontology
We use the following simple custom ontology based on SAREF:

```
@prefix ex: <http://example.org/> .

ex:DutchTemperatureSensor rdfs:subClassOf ex:Sensor .
ex:USTemperatureSensor rdfs:subClassOf ex:Sensor .

```

## RDFS
We use the RDFS rules that can be found [here](../../reasoner/src/test/resources/rdfs.rules).

## Building info
We use the following static building information as context information to the anomaly detector.

```
<http://example.org/sensor1> ex:isContainedIn <http://example.org/building1> .

<http://example.org/sensor2> ex:isContainedIn <http://example.org/building1> .

<http://example.org/sensor3> ex:isContainedIn <http://example.org/buildingX> .

```


#Remarks
Following questions/remarks:
- we could use `owl:inverseOf` property and [related rule](https://github.com/apache/jena/blob/main/jena-core/src/main/resources/etc/owl.rules) to go from `s4bldg:isSpaceOf` to `s4bldg:hasSpace`.
- we cannot use the `@include <RDFS>` directive, because it contains all kinds of builtins (such as `-> tableAll()`) which we do not support.
- When we copy all the RDFS axioms/rules from [here](../../reasoner/src/test/resources/rdfs.rules) and add them to the `dk.rules`, the example slows down considerably and uses most of the available resources on the computer.