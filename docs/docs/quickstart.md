---
 sidebar_position: 3
---

# Quickstart

Setting up a Knowledge Network requires 3 steps:
1. Start a Knowledge Directory
2. Start one (or more) Smart Connectors.
3. Register Knowledge Interactions

## Starting the Knowledge Directory
Start the Knowledge Directory on ports 8080:
```bash
cd knowledge-directory/target/

java -Dorg.slf4j.simpleLogger.logFile=kd.log -cp "knowledge-directory-1.2.4.jar:dependency/*" eu.knowledge.engine.knowledgedirectory.Main 8080
```
You can of course run the Knowledge Directory on another port by replacing 8080 by your preferred port number.

## Starting a Smart Connector
After starting a Knowledge Directory, one can start smart connectors to join the network via:
```bash
cd smart-connector-rest-dist/target

export KD_URL=http://localhost:8080
export KE_RUNTIME_EXPOSED_URL=http://localhost:8081
export KE_RUNTIME_PORT=8081

java -Dorg.slf4j.simpleLogger.logFile=ke.log -cp "smart-connector-rest-dist-1.2.4.jar:dependency/*" eu.knowledge.engine.rest.Main 8280
```

## Registering Knowledge Interactions