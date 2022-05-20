# Reasoner

We created our own implementation of a reasoner because, although there is a lot of literature on rule reasoners, we did not find algorithms that make the same assumption that the KE reasoner should make: _all facts are distributed_. This meant we could not simply reuse or implement an existing efficient algorithm, but had to come up with adapted versions of algorithms and implement them ourselves. 

## How it works
In short, the reasoner implementation works like this. 

### Planning phase
During the planning phase of an interaction we basically form a graph where every node is a rule application and every edge is one or more matches between the antecedent of the first rule (i.e. node) and the consequent of the second rule (i.e. node). These rules are derived from the graph patterns of the different Knowledge Interaction types (ASK, ANSWER, POST, REACT). Determining the edges (i.e. find matches between the antecedent graph pattern and consequent graph pattern) is the most costly operation during the planning phase. It takes our current matching algorithm 7 seconds to find all 130921 matches between two graph patterns of 8 triples with only variables. 

### Execution phase
During the execution phase, it basically traverses the graph while sending, receiving and translating Binding Sets to and from the involved Knowledge Bases. Here combining Binding Sets (i.e. multiple mappings from variable names to values) has the highest performance cost, because we need to make sure we do not miss any logically valid conclusions. I see and expect quite some room for improvement in all these implementations.