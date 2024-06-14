---
 sidebar_position: 100
---

# Glossary

## Knowledge Base
An independent producer and/or consumer of information.

## Knowledge Network
A set of Knowledge Bases that securely exchange knowledge about a clearly defined domain.

## Knowledge Engine Runtime
A single instance that is part of a Knowledge Network.
It takes care of the communication between one (or more!) Knowledge Base(s) and the rest of the Knowledge Network.

## Knowledge Directory
A registry that keeps track of all Knowledge Bases (as represented by Smart Connectors), and specifically their knowledge needs and desires.

## Smart Connector
An entity (currently in the form of a Java object) that enables a Knowledge Base to connect to a Knowledge Network and exchange knowledge.

## Knowledge Interaction
A specification of knowledge that a Knowledge Base processes.
A Knowledge Interaction is always one of the following four types: ASK, ANSWER, POST, REACT.
### ASK
A request for knowledge.

### ANSWER
Answer to a request for knowledge

### POST
A publication of knowledge.

### REACT
Reaction to the publication of knowledge


## Graph Pattern
A set of triples, separated by a dot (.), that describe the Knowledge that is processed with a Knowledge Interaction.
Each triple consists of a subject, predicate, and object.
Each of these can be either a variable (using a question mark `?var` prefix), a URI (using the `<https://...>`) or a literal (using quotes `"hello"`)

### On the Expressibility of Graph Patterns
Currently, the Knowledge Engine only supports Basic Graph Patterns.
It does not yet support features such as the FILTER from SPARQL.

## Binding Set
A result of a Knowledge Interaction can have more than 1 match. 
These matches are collected in a `BindingSet`, which is simply a set of bindings.

### Binding
Describes a 'match' of a graph pattern.
Essentially it maps (free) variables in a graph pattern to actual values.
Variables can be identified in a graph pattern as they are always prefixed with `?`.

Two important things should be noted:
1. The keys of the bindings MUST correspond to the variable names in the graph pattern, and they must be complete (all variables must have a value bound to them). (This last restriction does not apply to the bindings given with ASK requests; they can be partial of even empty.)
2. The values of the bindings MUST be valid IRIs (https://www.w3.org/TR/turtle/#sec-iri) (for now without prefixes, so full IRIs) or valid literals (https://www.w3.org/TR/turtle/#literals).
