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
To start a new instance of the REST API knowledge engine version 0.1.10, make sure you have `git checkout 0.1.10` the tag `0.1.10`. Now make sure you run the `mvn clean install` command successfully from the root of the repository.

Now, go to the target directory of the `smart-connector-rest-dist` module:

```bash
cd smart-connector-rest-dist/target
```

Finally, start the server (note that you can configure a log file by including the `-Dorg.slf4j.simpleLogger.logFile=ke.log` system property to the JVM):

```bash
java -Dorg.slf4j.simpleLogger.logFile=ke.log -cp "smart-connector-rest-dist-0.1.10.jar:dependency/*" eu.interconnectproject.knowledge_engine.rest.Main 8280
```

If you want to run in it in the background, you can use the `nohup` linux command (which does not use the simpleLogger configuration system property, but redirects the standard err/out):

```bash
nohup java -cp "smart-connector-rest-dist-0.1.10.jar:dependency/*" eu.interconnectproject.knowledge_engine.rest.Main 8280 > ke.log
```

## Release steps
These are instructions on what to do when we release a new version of the knowledge engine.

1. Update all relevant version references and make sure they are correct and non-SNAPSHOT:
	- all pom.xml files
	- openapi-sc.yaml version
	- this readme.md file
2. Make a commit for the release, and tag it with `git tag {x}.{y}.{z}` in GitLab.
3. Build and push the new docker image: `docker build ./smart-connector-rest-dist -t docker-registry.inesctec.pt/interconnect/knowledge-engine/smart-connector-rest-dist:0.1.10`
4. Prepare the next SNAPSHOT version and make a commit for that too.

## Running the REST server in Docker

To build and run a Docker image for the REST server, do the following:

```bash
# Build the project, and place the dependencies in the target directories.
# (Skip the tests.)
mvn install -DskipTests

docker-compose up
```
