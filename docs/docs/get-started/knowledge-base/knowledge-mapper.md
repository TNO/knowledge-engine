---
sidebar_position: 3
---
# Knowledge Mapper
The Knowledge Mapper makes it easier to connect to common data sources including SQL, SPARQL and Python classes (e.g. to connect to APIs).
It allows your Knowledge Base to be connected to the network using a single configuration file.
The mapper takes care of connecting to the Knowledge Network and helps in registering your Knowledge Base and Knowledge Interactions.

It is openly available at https://github.com/TNO/knowledge-mapper.

## What's included?
- Software to easily connect to SQL, SPARQL and Python classes (e.g. to connect to APIs)
- Python client to connect to a Knowledge Network
- Web interface to initialize a Knowledge Base that loads static JSON data

## How to use it
Examples of how to use the Knowledge Mapper to a SQL/SPARQL/Python data source are available at https://github.com/TNO/knowledge-mapper/tree/main/mapper/examples.

### Typical project setup
In a project that uses the Knowledge Mapper to connect to a network, you'll typically find the following files:
```
my-project
├── .gitignore      
├── Dockerfile      # For easy deployment
├── README.md
└── config.jsonc    # Configuration file
```
When connecting to a Python class, you'll also find either a `src/` directory or a `.py` file.

The configuration file is important as it defines which Knowledge Interactions your connector provides to the network.

### Configuration file
Below you can find what such a configuration file looks like.
Depending on whether you're connecting to SQL/SPARQL/Python, some additional variables need to be defined (see [examples](https://github.com/TNO/knowledge-mapper/tree/main/mapper/examples) for more details).

```json
{
  // The endpoint where a knowledge engine runtime is available.
  "knowledge_engine_endpoint": "http://tke-runtime:8280/rest",
  "knowledge_base": {
    // An URL representing the identity of this knowledge base
    "id": "https://example.org/a-custom-knowledge-base",
    // A name for this knowledge base
    "name": "Some knowledge base",
    // A description for this knowledge base
    "description": "This is just an example."
  },

  // Several knowledge interaction definitions can be placed here
  "knowledge_interactions": [
    {
      // The type of this knowledge interaction. If we have knowledge available that is requestable, the type should be "answer"
      "type": "answer",
      // The graph pattern that expresses the 'shape' of our knowledge
      "pattern": "?tree <https://example.org/hasHeight> ?height . ?tree <https://example.org/hasName> ?name .",
      // An optional name of this knowledge interaction
      "name": "answer-trees-with-heights-and-names"
    }
  ]
}
```