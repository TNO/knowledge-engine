---
  sidebar_position: 9
---
# Common Error Messages

On this page you can find a list of common error messages and what they mean.

### `x` is not an unprefixed URI or literal
Whenever you are specifying variable bindings, for example in a binding set when executing an ask, you may encounter this error.
It occurs because all variable bindings need to be either RDF Literals or IRIs.
If you want to identify a specific object or device `x`, you can use something like `<http://www.example.org/x>`.
Note that if the variable binding is for a subject in a triple (?subject ?predicate ?object), then an IRI is always required.
For more information on bindings and binding sets, see: [Bindings](https://docs.knowledge-engine.eu/java_developer_api#bindings).

### There are lots of 'HTTP/1.1 header parser received no bytes' errors in the logs. How do I prevent these?
This can be prevented by using a shorter timeout in the Java HTTP Client.
Try using the following Java option: `-Djdk.httpclient.keepalive.timeout=3`.
This can also be set in the docker configuration of a Knowledge Engine Runtime docker container by setting the JAVA_TOOL_OPTIONS as follows: `JAVA_TOOL_OPTIONS: "-Djdk.httpclient.keepalive.timeout=3"`.

### "java.lang.IllegalArgumentException: KB gave outgoing binding Binding [...], but this doesn't have a matching incoming binding!"
When using a REACT Knowledge Interaction that contains both an argument and result graph pattern which share a variable name, the Knowledge Engine expects all values for this variable in the result binding set to also occur as a value of the variable in the argument binding set.
This has to do with how these graph patterns are internally used to form if … then … rules.
If this is not the case, it gives the above error message.
If this shared variable in the argument and result graph patterns is intended to be the same, make sure you align the values of this variable in the result binding set with the values of this variable in the argument binding set.
If the variable is not intended to be the same thing, you can rename one of them to prevent the knowledge engine to expect them to share values.