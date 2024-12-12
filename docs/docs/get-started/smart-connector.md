---
sidebar_position: 6
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Connecting to a Knowledge Network
This page describes how to connect to an (existing) Knowledge Network using a Smart Connector.

To connect to a Knowledge Network, you need a Knowledge Engine Runtime (KER).
Every KER in distributed mode consists of two APIs: [Knowledge Engine Developer REST API](https://github.com/TNO/knowledge-engine/blob/1.2.5/smart-connector-rest-server/src/main/resources/openapi-sc.yaml) and the [Inter-Knowledge Engine Runtime API](https://github.com/TNO/knowledge-engine/blob/1.2.5/smart-connector/src/main/resources/openapi-inter-ker.yaml).
The former is started on port `8280` by default, and you use this API to register your Knowledge Base and Knowledge Interactions.
The latter API is meant for internal communication between KERs and you do not need to use it yourself.
However, you do need to make sure this API is reachable for other KERs in the Knowledge Network.
By default, this API is available on port 8081, but sometimes you need to change this port using the `KE_RUNTIME_PORT` environment variable.
Make sure the latter API of your KER is accessible from the internet and configure its URL when starting the KER with the `KE_RUNTIME_EXPOSED_URL` environment variable.
To set this up correctly, you typically install a reverse proxy like NGINX and open the correct ports in the firewall of the server.
For this you need to contact the administrator of the server you are using.
A KER starts in distributed mode when it detects the `KD_URL` environment variable.
This variable points to the Knowledge Directory of the Knowledge Network.
You can configure it using environment variables `KD_URL=<knowledge-direcotry-url>`.
If the Knowledge Directory is protected using Basic Authentication, you can add the credentials to the KD_URL as described [here](https://stackoverflow.com/a/50528734).



To connect to a network, the following steps are required:
* Get access to the Knowledge Engine Runtime (KER) you want to connect to
* Start a Knowledge Engine Runtime (KER) on your computer
* Register your Knowledge Base via the REST Developer API
* [Register your Knowledge Interactions via the REST Developer API](./knowledge-interactions.md)

## Getting access to the Knowledge Engine Runtime
To get access to the Knowledge Engine Runtime you want to connect to, you will need its URL.
You can test whether you have access to its REST Developer API by activating its `GET /sc` operation via the browser with a URL like: `<ker-url>/sc`
If the KER is protected with Basic Authentication, your browser might ask you for credentials. 
This operation returns JSON with all the Knowledge Bases that are registered with that Knowledge Engine Runtime.
An empty list indicates that no Knowledge Bases are registered with this Knowledge Engine Runtime.
If you run a Knowledge Engine Runtime on your own computer, then the URL is typically `http://localhost:8280`.

## How to start a Smart Connector?
> *Before starting a Smart Connector, please ensure that there is a Knowledge Directory available to connect to.*

> Typically, you start a single Smart Connector which should be available as long as your system (Knowledge Base) is available.

<Tabs groupId="tke-usage">
<TabItem value="java" label="Java">

Assuming `this` is your knowledge base, you can make a `SmartConnector` as follows:

```java
SmartConnector sc = SmartConnectorBuilder.newSmartConnector(this).create();
```

</TabItem>
<TabItem value="bash" label="Rest API">

```bash
cd smart-connector-rest-dist/target

export KD_URL=http://localhost:8080
export KE_RUNTIME_EXPOSED_URL=http://localhost:8081
export KE_RUNTIME_PORT=8081

java -Dorg.slf4j.simpleLogger.logFile=ke.log -cp "smart-connector-rest-dist-1.2.5.jar:dependency/*" eu.knowledge.engine.rest.Main 8280
```

</TabItem>
</Tabs>

## How to remove a Smart Connector?

## How to renew the lease of a Smart Connector?

## How to deal with the long polling connection of a Smart Connector?
Each Smart Connector uses a single long polling connection to receive all interactions from the Knowledge Engine.
The Knowledge Engine REST Developer API uses long-polling to notify you when your KB needs to react.
This long-polling connection will automatically return every *29 seconds* with status code 202 to prevent certain proxies from blocking it.
You will need to reestablish this long polling connection when you receive a 202 and after you receive data via it.
