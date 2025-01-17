---
sidebar_position: 4
---
# Starting a Knowledge Directory
This page describes how to setup a Knowledge Directory.

You can start the Knowledge Directory on ports 8080 with the available JAR:
```bash
cd knowledge-directory/target/

java -Dorg.slf4j.simpleLogger.logFile=kd.log -cp "knowledge-directory-1.3.0.jar:dependency/*" eu.knowledge.engine.knowledgedirectory.Main 8080
```
You can of course run the Knowledge Directory on another port by replacing 8080 by your preferred port number.
