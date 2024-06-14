---
sidebar_position: 6
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Smart Connectors

## How to instantiate a Smart Connector?
<Tabs groupId="tke-usage">
<TabItem value="java" label="Java">

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

java -Dorg.slf4j.simpleLogger.logFile=ke.log -cp "smart-connector-rest-dist-1.2.4.jar:dependency/*" eu.knowledge.engine.rest.Main 8280
```

</TabItem>
</Tabs>


## How to add a Smart Connector?

## How to remove a Smart Connector?

## How to renew the lease of a Smart Connector?