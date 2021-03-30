Smart Connector
===============

Components
----------

The Knowledge Engine Maven project consists of the following Maven modules:
* admin-ui
* examples
* rest-api
* smart-connector


If you run the REST version of the smart-connector. Use the following command (note that you can configure a log file by including the `-Dorg.slf4j.simpleLogger.logFile=sc.log` system property to the JVM):

```
java -Dorg.slf4j.simpleLogger.logFile=ke.log -cp "rest-api-0.0.1-SNAPSHOT.jar:lib/*" eu.interconnectproject.knowledge_engine.rest.api.RestServer 8280
```

This assumes all the required dependencies are located in a folder called `lib`. You can let maven collect all the required dependencies in the target/dependency folder using the following command:

```
mvn dependency:copy-dependencies
```

If you want to run in it in the background, you can use the `nohup` linux command (which does not use the simpleLogger configuration system property, but redirects the standard err/out):

```
nohup java -cp "rest-api-0.0.1-SNAPSHOT.jar:lib/*" eu.interconnectproject.knowledge_engine.rest.api.RestServer 8280 > ke.log
```


Release steps
-------------

- update all relevant versions and make sure they are correct and non-SNAPSHOT:
	- all pom.xml files
	- openapi-sc.yaml version