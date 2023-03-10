# Examples

## REST API

[`./rest-api/`](./rest-api/) is an example Docker Compose project with 3 knowledge bases that publish, store, and present sensor data through a single Knowledge Engine runtime.
This example covers all four knowledge interaction types, but does not cover all features the REST API provides.

## Java API

[`./java-api/` module](./java-api/) contains a Maven module where the Java API is used to share bindings through a POST knowledge interaction as they appear on an MQTT queue.
Another knowledge base receives those bindings through a REACT knowledge interaction an prints them to the console.

## Multiple runtimes

[`./multiple-runtimes/`](./multiple-runtimes/) contains a minimal Docker Compose project that shows how to configure multiple Knowledge Engine runtimes in distributed mode with a Knowledge Directory.
