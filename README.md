Smart Connector
===============

Components
----------

The Knowledge Engine Maven project consists of the following Maven modules:
* admin-ui
* examples
* rest-api
* smart-connector


If you run the smart-connector, you can configure a log file by including the following system property to the JVM: `org.slf4j.simpleLogger.logFile=sc.log`. Typically, you add them using the `-D` handle, so:

```
java ... org.slf4j.simpleLogger.logFile=sc.log ...
```



Release steps
-------------

- update all relevant versions and make sure they are correct and non-SNAPSHOT:
	- all pom.xml files
	- openapi-sc.yaml version