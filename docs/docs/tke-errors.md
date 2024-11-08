---
  sidebar_position: 9
---
# Common Error Messages

On this page you can find a list of common error messages and what they mean.

### `x` is not an unprefixed URI or literal
Whenever you are specifying variable bindings, for example in a binding set when executing an ask, you may encounter this error.
It occurs because all variable bindings need to be either RDF Literals or IRIs.
If you want to identify a specific object or device, you can use something like `<http://www.example.org/x>`.
Note that if the variable binding is for a subject in a triple (?subject ?predicate ?object), then an IRI is always rquired.
For more information on bindings and binding sets, see: [Bindings](https://docs.knowledge-engine.eu/java_developer_api#bindings).