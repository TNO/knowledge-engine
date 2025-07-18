*This software is in development ([TRL 6](https://en.wikipedia.org/wiki/Technology_readiness_level)).*

# Knowledge Engine

[![DOI](https://zenodo.org/badge/502097185.svg)](https://zenodo.org/badge/latestdoi/502097185)

[![Knowledge Engine logo](docs/static/img/ke_text_white_backdrop.png)](https://www.knowledge-engine.eu/)

Welcome to the source code repository of the Knowledge Engine.
This README should help you understand what the Knowledge Engine is, and how to use it.

In short, the Knowledge Engine is an interoperability solution that wants to make it easier for different knowledge bases to communicate.

*Knowledge base* is a term to describe "the system that connects with the interoperability platform". It can be anything. Examples include, databases, sensors, or even GUIs.

Communication is made easier by using concepts from an ontology: a common domain-specific model that the knowledge bases will have to agree upon.

For example, if one knowledge base is interested in some kind of data (e.g., names of dogs: `?dog a ex:Dog . ?dog ex:hasName ?dogName .`), and several other knowledge bases provide such data, the data is automatically gathered and merged when such a query is executed.

The Knowledge Engine consists of a number of components:

- A *smart connector* is used by knowledge bases to exchange data with other knowledge bases. The smart connector gives the knowledge base a way to (1) register the kind of knowledge they want to exchange and in which way, and (2) do the actual knowledge exchange.
- A *Knowledge Engine runtime* is a runtime in which one or more smart connectors can exist. These can exchange data with eachother, and, **if configured correctly**, also with smart connectors in other Knowledge Engine runtimes.
- (optional) A *knowledge directory* is used as a simple discovery mechanism for several Knowledge Engine runtimes to become aware of eachother's existence.

To quickly see how to use the Knowledge Engine through examples, see [`./examples/`](./examples/).

Detailed documentation can be found [here](https://docs.knowledge-engine.eu/).

The rest of this README is structured as follows:

- [Knowledge Engine](#knowledge-engine)
- [Demonstration videos and tutorials](#demonstration-videos-and-tutorials)
- [Starting a Knowledge Engine runtime](#starting-a-knowledge-engine-runtime)
    - [Running with Docker](#running-with-docker)
    - [Running with Java](#running-with-java)
      - [Advanced administration](#advanced-administration)
    - [Running with Java without the REST API](#running-with-java-without-the-rest-api)
- [Integrating with the Knowledge Engine](#integrating-with-the-knowledge-engine)
  - [Using the REST API](#using-the-rest-api)
  - [Using the Java API](#using-the-java-api)
- [Performance benchmark](#performance-benchmark)
- [Developer information](#developer-information)
  - [Components](#components)
  - [Release steps](#release-steps)
  - [Code conventions](#code-conventions)
  - [(advanced) Administering a Knowledge Engine runtime](#advanced-administering-a-knowledge-engine-runtime)
    - [Starting the Knowledge Engine in local mode](#starting-the-knowledge-engine-in-local-mode)
    - [Starting the Knowledge Engine in distributed mode](#starting-the-knowledge-engine-in-distributed-mode)
    - [Configuration](#configuration)

# Demonstration videos and tutorials

[This video](https://youtu.be/Kj66N0U2dzg) gives a high-level introduction to the Knowledge Engine, with a simple demonstration of how we aimed to use it in the [InterConnect project](https://interconnectproject.eu).

Following the previous video, [this video shows the realisation](https://youtu.be/fYoak5EI3FY), and explains more about a pilot in the Netherlands that uses the Knowledge Engine to control energy flexibility in a 22-story building with 160 appartments.

[This video tutorial](https://youtu.be/QVGmrOBJVkg) gives technical details about how to develop knowledge bases and connect them with the Knowledge Engine.

# Starting a Knowledge Engine runtime

Starting a Knowledge Engine runtime can be done in several ways, [with Docker](#running-with-docker), [with Java](#running-with-java), and [in a more minimal way with Java](#running-with-java-without-the-rest-api).

|                | Remote runtimes<sup>*</sup> | REST API | Java API |
|----------------|-----------------------------|----------|----------|
| Docker         | ✅                           | ✅        | ❌        |
| Java           | ✅                           | ✅        | ✅        |
| Java (minimal) | ✅                           | ❌        | ✅        |

<sup>*</sup> Requires additional configuration

### Running with Docker
The easiest way to start a Knowledge Engine runtime is with Docker:

```bash
docker run \
	-p 8280:8280 \
	ghcr.io/tno/knowledge-engine/smart-connector:1.3.2
```

The Knowledge Engine runtime is now available to use via the REST API at base URL `http://localhost:8280/rest` on your host machine.
For usage instructions, see [the section on using the REST API](#using-the-rest-api).

However, running it in the above way **does not support** data exchange with remote runtimes; it can only be used to exchange data with other smart connectors in the same runtime.

To interact with other runtimes, it needs additional configuration:

- An additional port mapping for the socket that listens for communication from other runtimes.
- `ke.runtime.exposed.url`: The URL via which the above socket is available for communication from the other runtime(s). You need to make sure the traffic is correctly routed to the container's port 8081 with a reverse proxy.
- `kd.url`: The URL on which to find the knowledge directory (to discover peers).

The configuration can be set as follows (note that the configuration properties below use [underscores and capital letters](#configuration))

```bash
docker run \
  -p 8280:8280 \
  -p 8081:8081 \
  -e KD_URL=https://knowledge-directory.example.org \
  -e KE_RUNTIME_EXPOSED_URL=https://your-domain.example.org:8081 \
  ghcr.io/tno/knowledge-engine/smart-connector:1.3.2
```

### Running with Java
If you prefer not to use Docker, you can also use the JAR to run it directly in your JVM:

```bash
# Where the knowledge directory is located
export KD_URL=https://knowledge-directory.example.org
# Port where the runtime will listen for connections from other
# smart connectors.
export KE_RUNTIME_PORT=8081
# URL where the runtime will be available from other runtimes.
export KE_RUNTIME_EXPOSED_URL=https://your-domain.example.org:8081

# Start it. The argument (8280) denotes the port number at which it
# will listen for connections to the Knowledge Engine REST API.
java -jar -Dorg.slf4j.simpleLogger.logFile=smart-connector.log \
  smart-connector-rest-dist-1.3.2-with-dependencies.jar 8280
```

The JAR can be retrieved by compiling the project:

```bash
mvn clean package -DskipTests
# the relevant JAR will be in `./smart-connector-rest-dist/target/`
```

#### Advanced administration

For further advanced instructions on administering a Knowledge Engine runtime, refer to [the advanced section of this document](#advanced-administering-a-knowledge-engine-runtime).

### Running with Java without the REST API
In constrained environments, it may be preferable to use the Java API directly, and not use the REST API.

For this, you need to be in a Java project, import our packages, implement the `KnowledgeBase` interface, and start a smart connector with a `SmartConnectorBuilder`.
An example of this can be found in the [`./examples/`](./examples/) folder.

# Integrating with the Knowledge Engine

Again, there are several options here: use the REST API, the Java API or the Knowledge Mapper<sup>*</sup>.

|                              | Available in JVM | Available whenever HTTP is possible | Easy configuration for selected data sources |
|------------------------------|------------------|-------------------------------------|----------------------------------------------|
| REST API                     | ✅                | ✅                                   | ❌                                            |
| Java API                     | ✅                | ❌                                   | ❌                                            |
| Knowledge Mapper<sup>*</sup> | ❌                | ❌                                   | ✅                                            |

<sup>*</sup> Under development, and currently not available as open-source software.

## Using the REST API
Assuming there is a REST API instance running at a known host, you can use these instructions to help you get started with making a client for it.

To make the client, it needs to talk HTTP, and conform to [our API specification](./smart-connector-rest-server/src/main/resources/openapi-sc.yaml).
With the API specification, you will be able to:

- Register your knowledge base via the `/sc` path.
- Register knowledge interactions for your knowledge base via the `/sc/ki` path.
- Trigger new proactive knowledge requests via the `/sc/ask` and `/sc/post` paths.
- Long-poll (`GET`) and respond to (`POST`) knowledge requests from the network via the `/sc/handle` path.

In the [`examples/rest-api`](./examples/rest-api) folder, there is an example Docker Compose project with 3 knowledge bases that publish, store, and present sensor data through a single Knowledge Engine runtime.
This example covers all four knowledge interaction types, but does not cover all features the REST API provides.

## Using the Java API

In the [`Java API Example` module](./examples/java-api), the Java API is used to share bindings through a POST knowledge interaction as they appear on an MQTT queue.
Another knowledge base receives those bindings through a REACT knowledge interaction an prints them to the console.

# Performance benchmark
A preliminary performence benchmark of the Knowledge Engine is available in [this repository](https://github.com/faclc4/YCSB-KE/tree/master).

Based on this benchmark, the minimum requirements for the Knowledge Engine are:
- i3-3040 Intel processor at 3.4 GHz
- 8 Gbit RAM 
- 250Gb SSD drives

Of course, it highly depends on how you want to use the Knowledge Engine, because the reasoner, for example, increases these requirements.


# Developer information

This section gives more detailed information about the project's structure, and is targeted towards developers who contribute code to the project.

## Components

The Knowledge Engine project consists of the following Maven modules:
- `smart-connector`
	- This is the implementation of the smart connector, with the Java developer API. For instructions on how to use it, refer to [the documentation](./docs/docs/getting_started.md). The high-level design of the smart connector can be found in [the wiki](https://github.com/TNO/knowledge-engine/wiki/High%E2%80%90level-Design).
- `smart-connector-api`
	- This module contains interfaces for the smart connector and other classes. It is made as a separate module so that it is easy to use different implementations of the interfaces.
- `smart-connector-rest-server`
	- This module contains the REST API layer that is built on top of the Java Developer API.
- `smart-connector-rest-dist`
	- A distribution of the server that provides the REST API layer for your smart connector(s), and uses the smart connector implementation from the `smart-connector` module. For instructions on how to use it, refer to [the section below](#using-the-rest-api). For instructions on how to set it up, refer to [this section](#advanced-administering-a-knowledge-engine-runtime).
- `admin-ui`
	- A REST API which provides meta-data about smart connectors in a knowledge network. Can be used in an administration inferface for a knowledge network. It is implemented as a knowledge base that uses metadata of other knowledge bases.
- `reasoner`
	- This module contains the reasoner specifically designed for the distributive nature of the knowledge engine.
- `knowledge-directory`
	- This module contains the Knowledge Directory which is used to find other knowledge engine runtimes.

## Release steps
These are instructions on what to do when we release a new version of the knowledge engine.

1. Update all relevant version references and make sure they are correct and non-SNAPSHOT:
	- this `README.md` file
	- all `pom.xml` files
	- `openapi-sc.yaml` version
	- Docker image tags in the Docker Compose examples.
2. Make a commit and push it to a new branch for the release.
3. Make a Pull Request and merge it into master after tests have succeeded.
4. Go to Actions on GitHub and trigger the workflow "Make draft release" with the new version as input.
5. When the workflow is finished, go to the newly generated draft release. Check the release notes, make any changes if necessary, and publish the release.
6. Prepare the next SNAPSHOT version and make a commit for that in a PR and merge into master.
   1. `openapi-sc.yaml`
   2. `pom.xml`
   3. Leave the non-SNAPSHOT version in this README, and in the Docker Compose examples.
7. Inform mailing list(s) (and [the blog](https://www.knowledge-engine.eu/blog/)) about the new release.

## Code conventions
The code conventions of the knowledge-engine can be found in the `/ide` folder in the Eclipse IDE format. The format can often also be imported in other Java IDEs like IntelliJ, VSCode or Netbeans.

## (advanced) Administering a Knowledge Engine runtime
To start a new instance of the REST API knowledge engine version 1.3.2, make sure you have `git checkout 1.3.2` the tag `1.3.2`. Now make sure you run the `mvn clean install` command successfully from the root of the repository.

### Starting the Knowledge Engine in local mode
When no additional configuration parameters are provided, the Knowledge Engine will by default run in local mode. This means you can create multiple smart connectors that can communicate with each other through the REST API, but the Knowledge Engine will not connect to a knowledge directory and will not be able to connect with smart connectors running in other runtimes.

Now, go to the target directory of the `smart-connector-rest-dist` module:

```bash
cd smart-connector-rest-dist/target
```

Finally, start the server (note that you can configure a log file by including the `-Dorg.slf4j.simpleLogger.logFile=ke.log` system property to the JVM):

```bash
java -Dorg.slf4j.simpleLogger.logFile=ke.log -cp "smart-connector-rest-dist-1.3.2.jar:dependency/*" eu.knowledge.engine.rest.Main 8280
```

If you want to run in it in the background, you can use the `nohup` linux command (which does not use the simpleLogger configuration system property, but redirects the standard err/out):

```bash
nohup java -cp "smart-connector-rest-dist-1.3.2.jar:dependency/*" eu.knowledge.engine.rest.Main 8280 > ke.log
```

### Starting the Knowledge Engine in distributed mode
The Knowledge Engine can also start in distributed mode, where it connects with a remote knowledge directory and where different instances of the Knowledge Engine (each instance hosting one or more smart connectors) can communicate with each other. More information about starting the Knowledge Engine in distributed mode can be found in the [documentation](docs/docs/distributed_mode.md).

### Configuration
TNO Knowledge Engine uses the [MicroProfile Config 3.1](https://microprofile.io/specifications/config/) specification to configure its behaviour and we use [SmallRye](https://smallrye.io/smallrye-config/) as the implementation of this specification. The default configuration values can be found in the [microprofile-config.properties](./smart-connector/src/main/resources/META-INF/microprofile-config.properties) configuration file. And, as described in the specification, these configuration values can be overridden by [environment variables and system properties](https://download.eclipse.org/microprofile/microprofile-config-3.1/microprofile-config-spec-3.1.html#default_configsources). Note that environment variables can use underscores and capital letters to adhere to their naming conventions and the MicroProfile Config automatically maps those to corresponding configuration properties using [specific rules](https://download.eclipse.org/microprofile/microprofile-config-3.1/microprofile-config-spec-3.1.html#default_configsources.env.mapping).

A description of all configuration properties can be found in the [`SmartConnectorConfig`](./smart-connector-api/src/main/java/eu/knowledge/engine/smartconnector/api/SmartConnectorConfig.java) class. The rest of this section highlights some of these configuration properties.

*Increasing the wait time for other KBs to respond*

By default, a Smart Connector waits `10` seconds max for a reply from another Smart Connector when sending an ASK/POST message. This time is configurable via the `ke.kb.wait.timeout` property and setting it to `0` means the Smart Connector will wait indefinitely (this can be useful when dealing with Human KBs).

*Increasing the HTTP timeouts*

By default, a KER waits `5` seconds max for a HTTP connection response from another KER when sending a message via the inter-KER protocol. The time is configurable via the `ke.http.timeout` property.

*Configure the reasoner level*

By default, the reasoner level is set to `2`, but can be overridden as described above. The reasoner level (1-5) determines how advanced the reasoner mechanism will be. Every Smart Connector within the Knowledge Engine Runtime will use the configured reasoning level unless specified otherwise. The level can be configured via the `ke.reasoner.level` property.

*Configure default domain knowledge*

By default, the smart connectors created within a KE Runtime do not load any domain knowledge (i.e. rules and ontology facts) that are used during reasoning. Using the `ke.domain.knowledge.path` configuration property sets the (absolute) path to additional domain knowledge that will be loaded by default for every smart connector being started in this runtime. The syntax for this domain knowledge is defined by the [Apache Jena Rules specification](https://jena.apache.org/documentation/inference/index.html#RULEsyntax). Example domain knowledge about [RDFS](https://www.w3.org/TR/rdf-schema/) can be found in [rdfs.rules](reasoner/src/test/resources/rdfs.rules).