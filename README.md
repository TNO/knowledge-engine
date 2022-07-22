# Knowledge Engine

## Components

The Knowledge Engine project consists of the following Maven modules:
- `smart-connector`
  - This is the implementation of the smart connector, with the Java developer API. For instructions on how to use it, refer to [the documentation](./docs/03_java_developer_api.md).
- `smart-connector-api`
	- This module contains interfaces for the smart connector and other classes. It is made as a separate module so that it is easy to use different implementations of the interfaces.
- `smart-connector-rest-server`
	- This module contains the REST API layer that is built on top of the Java Developer API.
- `smart-connector-rest-dist`
  - A distribution of the server that provides the REST API layer for your smart connector(s), and uses the smart connector implementation from the `smart-connector` module. For instructions on how to use it, refer to [the section below](#how-to-use-the-rest-api). For instructions on how to set it up, refer to [this section](#how-to-administer-the-rest-api).
- `examples`
	- A selection of examples of how the Java developer API is used.
- `admin-ui`
	- A (for now) command line tool that can be used as an administration inferface for a knowledge network. It is implemented as a knowledge base that uses metadata of other knowledge bases.

## How to use the REST API
Assuming there is a REST API instance running at a known host, you can use these instructions to help you get started with making a client for it.

To make the client, it needs to talk HTTP, and conform to [our API specification](./smart-connector-rest-server/src/main/resources/openapi-sc.yaml).
With the API specification, you will be able to:

- Register your knowledge base (with its accompanying smart connector) via the `/sc` path.
- Register knowledge interactions for your knowledge base via the `/sc/ki` path.
- Trigger new proactive knowledge requests via the `/sc/ask` and `/sc/post` paths.
- Respond to knowledge requests from the network via the `/sc/handle` path.

In the [`client_example` package](./smart-connector-rest-server/src/main/java/eu/interconnectproject/knowledge_engine/rest/api/client_example), there are several examples of clients written in Java.

## How to administer the REST API
To start a new instance of the REST API knowledge engine version 1.1.2, make sure you have `git checkout 1.1.2` the tag `1.1.2`. Now make sure you run the `mvn clean install` command successfully from the root of the repository.

### Starting the Knowledge Engine in local mode
When no additional configuration parameters are provided, the Knowledge Engine will be default run in local mode. This means you can create multiple Smart Connectors that can communicate with each other through the REST API, but the Knowledge Engine will not connect to a Knowledge Directory and will not be able to connect with Smart Connectors on other machines.

Keep in mind that the `java` commands below assume a *linux* environment. If you are using a *windows* environment, there might be slight differences (like using semi-colons (;) instead of colons (:) to separate classpath entries).

Now, go to the target directory of the `smart-connector-rest-dist` module:

```bash
cd smart-connector-rest-dist/target
```

Finally, start the server (note that you can configure a log file by including the `-Dorg.slf4j.simpleLogger.logFile=ke.log` system property to the JVM):

```bash
java -Dorg.slf4j.simpleLogger.logFile=ke.log -cp "smart-connector-rest-dist-1.1.2.jar:dependency/*" eu.knowledge.engine.rest.Main 8280
```

If you want to run in it in the background, you can use the `nohup` linux command (which does not use the simpleLogger configuration system property, but redirects the standard err/out):

```bash
nohup java -cp "smart-connector-rest-dist-1.1.2.jar:dependency/*" eu.knowledge.engine.rest.Main 8280 > ke.log
```

### Starting the Knowledge Engine in distributed mode
The Knowledge Engine can also start in distributed mode, where it connects with a remote Knowledge Directory and where different instances of the Knowledge Engine (each instance hosting one or more Smart Connectors) can communicate with each other.

First of all, you need to start a Knowledge Directory. The desired port number for the Knowledge Directory can be configured using the command line argument (8080 in the example underneath).

```bash
cd knowledge-directory/target/

java -Dorg.slf4j.simpleLogger.logFile=kd.log -cp "knowledge-directory-1.1.2.jar:dependency/*" eu.knowledge.engine.knowledgedirectory.Main 8080
```

As explained in the local mode section, nohup can be used to run the process in the background. On overview of the registered Knowledge Engine Runtimes can be found on `http://localhost:8080/ker/` (or another host or port if you desire).

Once the Knowledge Directory is up and running, the REST server can be started. It is configured through environment variables. It has the following configuration options:

| Key | Descrption |
| --- | --- |
| KD_URL | URL where the Knowledge Directory can be found |
| KE_RUNTIME_EXPOSED_URL | URL where other Smart Connectors (peers) can contact this Knowledge Engine instance. This allows your Knowledge Engine to be behind a reverse proxy and use TLS. Note that the URL should include the scheme like `http://...` or `https://...`.
| KE_RUNTIME_PORT | Port where where this Knowledge Engine instance will listen for new peer connections |
| KE_RUNTIME_HOSTNAME (deprecated) | Hostname where other Smart Connectors (peers) can contact this Knowledge Engine instance. This variable is superseded by (and conflicts with) KE_RUNTIME_EXPOSED_URL|

Note that the port for the REST API for the Knowledge Bases is still configured through the command line argument.

```bash
cd smart-connector-rest-dist/target

export KD_URL=http://localhost:8080
export KE_RUNTIME_EXPOSED_URL=http://localhost:8081
export KE_RUNTIME_PORT=8081

java -Dorg.slf4j.simpleLogger.logFile=ke.log -cp "smart-connector-rest-dist-1.1.2.jar:dependency/*" eu.knowledge.engine.rest.Main 8280
```

As explained in the local mode section, nohup can be used to run the process in the background.

## Release steps
These are instructions on what to do when we release a new version of the knowledge engine.

1. Update all relevant version references and make sure they are correct and non-SNAPSHOT:
	- all pom.xml files
	- openapi-sc.yaml version
	- this readme.md file
2. Make a commit for the release, and tag it with `git tag {x}.{y}.{z}` in GitLab.
3. `mvn deploy` (for this you need `Deploy-Token` configured in your Maven's `settings.xml`)
4. Build and push the new docker images:
	- `docker build ./smart-connector-rest-dist -t docker-registry.inesctec.pt/interconnect/knowledge-engine/smart-connector-rest-dist:1.1.2`
	- `docker build ./knowledge-directory -t docker-registry.inesctec.pt/interconnect/knowledge-engine/knowledge-directory:1.1.2`
	- `docker build ./admin-ui -t docker-registry.inesctec.pt/interconnect/knowledge-engine/admin-ui:1.1.2`
	- `docker push docker-registry.inesctec.pt/interconnect/knowledge-engine/smart-connector-rest-dist:1.1.2`
	- `docker push docker-registry.inesctec.pt/interconnect/knowledge-engine/knowledge-directory:1.1.2`
	- `docker push docker-registry.inesctec.pt/interconnect/knowledge-engine/admin-ui:1.1.2`
5. Prepare the next SNAPSHOT version and make a commit for that too.

## Running the REST server in Docker

To build and run a Docker image for the REST server, do the following:

```bash
# Build the project, and place the dependencies in the target directories.
# (Skip the tests.)
mvn install -DskipTests

docker-compose up
```
