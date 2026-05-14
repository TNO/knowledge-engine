# Knowledge-driven Data Exchange and Validation Framework for Networked Smart Buildings

This is a demo combining a knowledge validator with the knowledge engine. We want to have a scenario that demonstrates/uses the following:
- using a (saref + custom) ontology to reach semantic interoperability.
- using context data (i.e. building data) about sensors to improve knowledge validator performance.
- using a converter knowledge base to convert from Fahrenheit to Celsius and improve the knowledge validator performance.

The example consists of 8 knowledge bases:

- `knowledge-validator-kb`: A knowledge base that contains the knowledge validator which only looks at `building1`.
  - It reacts to sensor measurements by printing the results.
- `dashboard-kb`: A knowledge base that subscribes to anomaly reports published by the `knowledge-validator-kb`.
  - It reacts to validation reports by logging them to the standard output.
- `sensor1-kb`: A knowledge base that publishes celsius temperature measurements from a Dutch sensor type.
  - It makes available the data in `KB_DATA`, using the pattern in `GRAPH_PATTERN`
  - This sensor should have the appropriate reasoner level and load RDFS rules and SAREF.
- `sensor2-kb`: A knowledge base that publishes fahrenheit measurements from a United States sensor type.
  - It makes available the data in `KB_DATA`, using the pattern in `GRAPH_PATTERN`
  - This sensor should have the appropriate reasoner level and load RDFS rules and a custom ontology.
- `sensor3-kb`: A knowledge base that publishes celsius measurements from a European sensor type but is contained in building 2.
  - It makes available the data in `KB_DATA`, using the pattern in `GRAPH_PATTERN`
  - This sensor should have the appropriate reasoner level and load RDFS rules and a custom ontology.
  - The idea is that this sensor's data is not received by the knowledge validator.
- `sensor4-kb`: A knowledge base that publishes fahrenheit measurements from a United States sensor type but is contained in building 2.
  - It makes available the data in `KB_DATA`, using the pattern in `GRAPH_PATTERN`
  - This sensor should have the appropriate reasoner level and load RDFS rules and a custom ontology.
  - The idea is that this sensor's data is not received by the knowledge validator.
- `converter-kb`: A knowledge base that converts fahrenheit into celsius measurements.
  - It converts bindings in the `ARGUMENT_PATTERN` form into bindings in the `RESULT_PATTERN` form, using the Python function `react` defined in `REACT_FUNCTION_DEF`.
- `building-kb`: A knowledge base that contains static building information to provide more context information to the knowledge validator.

## Running

When running the project, and showing the logs of the `knowledge-validator-kb` service:

```
docker compose up -d
docker compose logs -f dashboard-kb
```

After the knowledge validator's learning period is over, you will see that the `dashboard-kb` receives the validation reports:

```
dashboard-kb-1  | INFO:dashboard-kb:REACT KI is handling a request...
dashboard-kb-1  | INFO:dashboard-kb:Reacting with empty bindingset to [ ... ]...
dashboard-kb-1  | INFO:dashboard-kb:REACT KI is handling a request...
dashboard-kb-1  | INFO:dashboard-kb:Reacting with empty bindingset to [ ... ]...
```

## Requirement verification instructions

To run and verify the demo, follow the instructions below. Note that they require console access. Only the last step for running the requirements verification script requires a \*nix system, while the other steps work on both Windows and *nix systems:
<!--
- extract `demo.zip` (note that it will extract into a folder called 'demo'): `unzip demo.zip`
- navigate to the `demo` folder
- make sure docker is installed and started: `docker --version`
- remove any previous images with the same names: `docker image rm directory:latest knowledge-engine:latest knowledge-validator:latest`
- load the included docker images:
  - `docker image load -i directory.tar`
  - `docker image load -i knowledge-validator.tar`
  - `docker image load -i knowledge-engine.tar`
-->
- build the docker compose project: `docker compose build`
- run the docker compose project: `docker compose up -d`
- wait until knowledge-validator is finished learned: `docker compose logs -f dashboard-kb`
- Wait for text `Reacting with empty bindingset to …` to appear
- Exit the dashboard logs using `CTRL+C`
- Run the requirements verification script: `./requirements_verification.sh`
- Wait for the script to finish with all tests <span style="color:green">`[PASSED]`</span>

This demo verifies the requirements below.

Knowledge Engine requirements:
- RQ.KE-1: Process node registration requests
- RQ.KE-2: Process publication and subscription requests
- RQ.KE-3: Accept data from registered publishers
- RQ.KE-4: Send data to registered subscribers
- RQ.KE-5: Forward data from registered publishers to registered subscribers 
 
Knowledge Validator requirements:
- RQ.KV-1: Real-time monitoring of data streams
- RQ.KV-2: Learn patterns of nominal behaviour
- RQ.KV-3: Update patterns of nominal behaviour
- RQ.KV-4: Detect deviations from nominal behaviour
- RQ.KV-5: Report on detected deviations
- RQ.KV-6: Provide explanations for detected deviations

## Ontology
We use the following simple custom ontology based on SAREF:

```
@prefix ex: <http://example.org/> .

ex:EUTemperatureSensor rdfs:subClassOf ex:Sensor .
ex:USTemperatureSensor rdfs:subClassOf ex:Sensor .

```

## RDFS
We use (part of) W3C's [RDFS rules](https://www.w3.org/TR/rdf11-mt/#patterns-of-rdfs-entailment-informative).

## Building info
We use the following static building information as context information to the knowledge validator.

```
<http://example.org/sensor1> ex:isContainedIn <http://example.org/building1> .

<http://example.org/sensor2> ex:isContainedIn <http://example.org/building1> .

<http://example.org/sensor3> ex:isContainedIn <http://example.org/building2> .

<http://example.org/sensor4> ex:isContainedIn <http://example.org/building2> .

```

# Remarks
Following questions/remarks:
- we could use `owl:inverseOf` property and [related rule](https://github.com/apache/jena/blob/main/jena-core/src/main/resources/etc/owl.rules) to go from `s4bldg:isSpaceOf` to `s4bldg:hasSpace`.
- we cannot use the `@include <RDFS>` directive, because it contains all kinds of builtins (such as `-> tableAll()`) which we do not support.
- When we copy all the RDFS axioms/rules from [here](../../reasoner/src/test/resources/rdfs.rules) and add them to the `dk.rules`, the example slows down considerably and uses most of the available resources on the computer.
