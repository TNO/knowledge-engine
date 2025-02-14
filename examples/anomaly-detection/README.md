# Anomaly detection

This is an example of combining anomaly detection with the knowledge engine. We want to have a scenario that demonstrates/uses the following:
- using a (saref + custom) ontology to reach semantic interoperability.
- using context data about sensors to improve anomaly detection performance.
- using a converter knowledge base to convert from fahrenheit to celsius and improve the anomaly detection performance.

The example consists of three knowledge bases:

- `anomaly-detection-kb`: A knowledge base that represents an anomaly detection algorithm
  - It reacts to sensor measurements by printing the results
- `sensor1-kb`: A knowledge base that publishes celsius temperature measurements from a Dutch sensor type.
  - It makes available the data in `KB_DATA`, using the pattern in `GRAPH_PATTERN`
  - This sensor should have the appropriate reasoner level and load RDFS rules and SAREF.
- `sensor2-kb`: A knowledge base that publishes fahrenheit measurements from a US sensor type.
  - It makes available the data in `KB_DATA`, using the pattern in `GRAPH_PATTERN`
  - This sensor should have the appropriate reasoner level and load RDFS rules and a custom ontology.
- `sensor3-kb`: A knowledge base that publishes celsius measurements from a Dutch sensor type but is contained in a different building than the otehr two sensors.
  - It makes available the data in `KB_DATA`, using the pattern in `GRAPH_PATTERN`
  - This sensor should have the appropriate reasoner level and load RDFS rules and a custom ontology.
  - The idea is that this sensor's data is not received by the anomaly detector.
- `converter-kb`: A knowledge base that converts fahrenheit into celsius measurements.
  - It converts bindings in the `ARGUMENT_PATTERN` form into bindings in the `RESULT_PATTERN` form, using the Python function `react` defined in `REACT_FUNCTION_DEF`.
- `building-kb`: A knowledge base that contains static building information to provide more context information to the anomaly detector.

When running the project, and showing the logs of the `anomaly-detector-kb` service:

```
docker compose up -d
docker compose logs -f kb1
```

You see that it receives the measurements in celsius and from both the Dutch and US sensor type.

```
Not yet available.
```

## Ontology
We use the following simple custom ontology based on SAREF:

```
<https://www.example.org/anomaly/DutchTemperatureSensor> <http://www.w3.org/2000/01/rdf-schema#subClassOf> <https://saref.etsi.org/core/Sensor> .
<https://www.example.org/anomaly/USTemperatureSensor> <http://www.w3.org/2000/01/rdf-schema#subClassOf> <https://saref.etsi.org/core/Sensor> .


```

## RDFS
We use the RDFS rules that can be found [here](../../reasoner/src/test/resources/rdfs.rules).

## Building info
We use the following static building information as context information to the anomaly detector.

```
<sensor1> s4bldg:isContainedIn <room1> .
<sensor2> s4bldg:isContainedIn <room2> .
<sensor3> s4bldg:isContainedIn <roomX> .
<room1> s4bldg:isSpaceOf <floor1> .
<room2> s4bldg:isSpaceOf <floor1> .
<roomX> s4bldg:isSpaceOf <floorX> .
<floor1> s4bldg:isSpaceOf <building1> .
<floorX> s4bldg:isSpaceOf <buildingX> .
```


#Remarks
Following questions/remarks:
- we could use `owl:inverseOf` property and [related rule](https://github.com/apache/jena/blob/main/jena-core/src/main/resources/etc/owl.rules) to go from `s4bldg:isSpaceOf` to `s4bldg:hasSpace`.