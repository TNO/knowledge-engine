---
sidebar_position: 99
---
# Implementing a Knowledge Base
This page describes how to implement your own Knowledge Base given a data source.

There are three approaches to implement your own Knowledge Base:
1. Java
2. REST Developer API
3. Knowledge Mapper (based on a Python client)
4. JavaScript client

The Knowledge Mapper is a tool we have built to easily connect to several common data sources (SQL, RDF, APIs).
If you're interested in using the Knowledge Mapper or JavaScript client, please reach out to us as they are not yet open source.

## Implementing your own Knowledge Interaction
When you receive a request for data via an ANSWER or REACT Knowledge Interaction, you should return the expected results, e.g. by retrieving data from an API.

If you do not have a response, then you should send an empty binding set.
Also, when your REACT Knowledge Interaction has no result graph pattern, you should always return an empty binding set to the Knowledge Engine.

If an error occurs while responding, you can either return an empty BindingSet (although this does not give any information about the error occurring) or call the (in case you are using the asynchronous handler methods of the Java Developer API) `future.completeExceptionally(...)` method.
