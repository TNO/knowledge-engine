# TKE-EDC Example
This example uses EDC-IDS Connectors for communication between two Knowledge Engine Runtimes (KERs).
All messages that are sent contain an authentication code.
If a message is received, the authentication code is validated unless it is a meta Knowledge Interaction.

## Introduction to the Knowledge Engine
The Knowledge Engine is a system for seamlessly connecting data sources.
Each data source, which can provide and/or consume information, is also called a Knowledge Base.
Multiple Knowledge Bases that communicate together form a network.
To communicate with such a network, each Knowledge Base uses a component called a Smart Connector.
This Smart Connector takes care of all communication between a Knowledge Base and others in the network.
The Smart Connectors are part of the Knowledge Engine solution and should not be confused with the EDC-IDS Connectors.


![A single Knowledge Base communicates with a network through a Smart Connector.](./single-kb.png)

Within a network, each Knowledge Base announces what information it wants to _receive_, and what information it can _provide_ through its Smart Connector.
The Knowledge Engine will determine who to contact for each information request.

There are 4 types of information requests, also called Knowledge Interactions, in the Knowledge Engine: Ask, Answer, Post, React.
Ask is to request information. An answer provides an Answer to a request for information, i.e. Ask.
Post is to announce information. 
A React gives you the ability to subscribe to information and thus react to information that is announced through a Post.

These Knowledge Interactions are first registered at the Smart Connector. 
After they have been registered, they can be executed.

For more information on the Knowledge Engine, check out the [documentation](../../docs/00_home.md).

## About the Integration with EDC-IDS
The current integration between the Knowledge Engine and EDC-IDS focuses on the authentication of messages.
All messages that are sent contain an authentication code.
This authentication code is validated whenever the message is received.
This way we can be sure that the message was sent by the correct party, thus it establishes trust within the network.

We currently use the standard EDC-IDS Connector without any modifications.
We use the Connector to establish and check the identity of all parties in the network.
The communication between KERs is still direct, meaning that messages that are sent do not go through the Connector.

The authentication tokens are valid for a limited amount of time.
You can set the duration of validity of authentication tokens in the EDC Connector properties file (`edc.transfer.proxy.token.validity.seconds`).
While tokens can expire in the current implementation, there is not yet a mechanism to renew them.
That's why we currently advise you to set it to a high number.


## Running the TKE-EDC example
This example uses 3 knowledge bases as depicted below.

![Picture with 3 knowledge bases. Each knowledge base uses a Smart Connector to communicate with the other knowledge bases.](./illustration-example-situation.png)

One knowledge base asks for information and the other two provide an answer to the question.

### Executing the example
Execute the following steps to run the example:
1. In this project, execute a `mvn clean install`.
2. In the `knowledge-directory` directory in this project, execute `docker build . -t testkd:1.2.5-SNAPSHOT`.
3. In the `smart-connector-rest-dist` directory in this project, execute `docker build . -t testsc:1.2.5-SNAPSHOT`.
4. In the `examples/edc-example` directory in this project, execute `docker compose build`. 
5. In the `examples/edc-example` directory in this project, execute `docker compose up -d tke-edc-one tke-edc-two tke-edc-three`. This starts three EDC-IDS Connectors.
6. Wait around 10 seconds to give the EDC Connectors time to finish setting up. Then, execute `docker compose up -d` to start three KERs, three linked Knowledge Bases and a Knowledge Directory.

You can inspect the logs with `docker compose logs -f`.
After a moment (+-30 seconds), the logs will stabilise when the connectors have finished initiating the various data flows.
You can then see that one KER (`runtime-1`) asks for information, a second KER (`runtime-2`) answers with `http://example.org/Math, http://example.org/Science` and the third (`runtime-3`) answers with `http://example.org/Magazines, http://example.org/Books`.

To stop the example, execute `docker compose down`.

## Adding another participant to the network
For each additional KER with an EDC-IDS Connector, we need the following files in the `examples/edc-example` directory:
- `connector/configuration/ker-configuration.properties` contains settings for the EDC-IDS Connector
- `connector/configuration/ker-vault.properties` contains a public key
- A new directory for this specific KER. Currently, they are named `ker1`, `ker2`, ..., containing:
  - `edc.properties` file containing several properties that are used in the communication between KERs
  - `Dockerfile` containing a Docker script that starts the KER. Note that this contains a link to the `edc.properties` file and thus is unique for each KER.

The `docker-compose.yml` in `examples/edc-example/` should also be modified to include:
- An additional KER (currently named `runtime-1`, `runtime-2`, ...)
  - The `build` setting refers to the directory for the new KER.
  - The `depends_on` setting refers to the Docker component for the EDC-IDS Connector
  - The `KE_RUNTIME_EXPOSED_URL` is a unique URL for the new KER.
- An additional EDC-IDS Connector (currently named `tke-edc-one`, `tke-edc-two`, ...)
  - Requires 4 ports to be forwarded
  - The `command` used to start this connector refers to the previously mentioned configuration files and thus the names of those files should be modified if you copy the command from another EDC-IDS Connector.
  - The `hostname` is used in the properties files to refer to this entity
- An additional knowledge base (`kb1`, `kb2`, ...)
  - The `KE_URL` refers to the `KE_RUNTIME_EXPOSED_URL` of the KER Docker component (`runtime-1`, `runtime-2`, ...)