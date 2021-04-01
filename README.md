# Knowledge Engine

## Components

The Knowledge Engine project consists of the following Maven modules:
- `smart-connector`
  - This is the implementation of the smart connector, with the Java developer API. For instructions on how to use it, refer to [the documentation](./docs/03_java_developer_api.md).
- `rest-api`
  - A server that provides a REST API layer on top of the Java developer API. For instructions on how to use it, refer to [the section below](#how-to-use-the-rest-api). For instructions on how to set it up, refer to [this section](#how-to-administer-the-rest-api).
- `examples`
	- A selection of examples of how the Java developer API is used.
- `admin-ui`
	- A (for now) command line tool that can be used as an administration inferface for a knowledge network. It is implemented as a knowledge base that uses metadata of other knowledge bases.

## How to use the REST API
Assuming there is a REST API instance running at a known host, you can use these instructions to help you get started with making a client for it.

To make the client, it needs to talk HTTP, and conform to [our API specification](./rest-api/src/main/resources/openapi-sc.yaml).
With the API specification, you will be able to:

- Register your knowledge base (with its accompanying smart connector) via the `/sc` path.
- Register knowledge interactions for your knowledge base via the `/sc/ki` path.
- Trigger new proactive knowledge requests via the `/sc/ask` and `/sc/post` paths.
- Respond to knowledge requests from the network via the `/sc/handle` path.

In the [`client_example` package](./rest-api/src/main/java/eu/interconnectproject/knowledge_engine/rest/api/client_example), there are several examples of clients written in Java.

## How to administer the REST API
To start a new instance of the REST API knowledge engine, use the following command (note that you can configure a log file by including the `-Dorg.slf4j.simpleLogger.logFile=ke.log` system property to the JVM):

```bash
java -Dorg.slf4j.simpleLogger.logFile=ke.log -cp "rest-api-0.0.1-SNAPSHOT.jar:lib/*" eu.interconnectproject.knowledge_engine.rest.api.RestServer 8280
```

This assumes all the required dependencies are located in a folder called `lib`. You can let maven collect all the required dependencies in the target/dependency folder using the following command:

```bash
mvn dependency:copy-dependencies
```

If you want to run in it in the background, you can use the `nohup` linux command (which does not use the simpleLogger configuration system property, but redirects the standard err/out):

```bash
nohup java -cp "rest-api-0.0.1-SNAPSHOT.jar:lib/*" eu.interconnectproject.knowledge_engine.rest.api.RestServer 8280 > ke.log
```

## Release steps
These are instructions on what to do when we release a new version of the knowledge engine.

1. Update all relevant version definintions and make sure they are correct and non-SNAPSHOT:
	- all pom.xml files
	- openapi-sc.yaml version
2. Make a commit for the release, and tag it with `git tag v{x}.{y}.{z}`.
3. Prepare the next SNAPSHOT version and make a commit for that too.
