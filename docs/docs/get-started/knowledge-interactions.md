---
---
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Using Knowledge Interactions
This page describes how to register and execute Knowledge Interactions.

## How to instantiate a Knowledge Interaction?
<Tabs groupId="tke-usage">
<TabItem value="java" label="Java">

```java
// ASK:
AskKnowledgeInteraction askInteraction = new AskKnowledgeInteraction(communicativeAct, graphPattern);
smartConnector.register(askInteraction);

// ANSWER:
AnswerKnowledgeInteraction answerInteraction = new AnswerKnowledgeInteraction(communicativeAct, graphPattern);
smartConnector.register(answerInteraction);

// POST:
PostKnowledgeInteraction postInteraction = new PostKnowledgeInteraction(communicativeAct, argumentGraphPattern, resultGraphPattern);
smartConnector.register(postInteraction);

// REACT:
ReactKnowledgeInteraction reactInteraction = new ReactKnowledgeInteraction(communicativeAct, argumentGraphPattern, resultGraphPattern);
smartConnector.register(reactInteraction);
```

You can also provide a name for your interaction, for example:
```java
AskKnowledgeInteraction askInteraction = new AskKnowledgeInteraction(communicativeAct, graphPattern, name); 
```

If you want to use prefixes in your graph pattern, these can be defined in the `graphPattern`:
```java
GraphPattern graphPattern = new GraphPattern(prefixes, pattern);
```

</TabItem>
<TabItem value="JSON" label="Rest API">
To instantiate a Knowledge Interaction, you need to send a POST to `/sc/ki` with a body that contains the type of knowledge interaction that you want to make, and any required graph patterns.
ASK and ANSWER require one graph pattern, like so:

```json
{
  "knowledgeInteractionType": "AskKnowledgeInteraction",
  "graphPattern": "?s ?p ?o"
}
```

POST and REACT require an `argumentGraphPattern`, and optionally use a `resultGraphPattern`. For example:

```json
{
  "knowledgeInteractionType": "PostKnowledgeInteraction",
  "argumentGraphPattern": "?s ?p ?o",
  "resultGraphPattern": "?x ?y ?z"
}
```

You can also provide a name for your interaction and define prefixes for your graph patterns:

```json
{
  "knowledgeInteractionType": "AskKnowledgeInteraction",
  "knowledgeInteractionName": "my-ask",
  "graphPattern": "?a rdf:type ex:Book",
  "prefixes": {
    "rdf": "https://www.w3.org/1999/02/22-rdf-syntax-ns/"
  }
}
```

</TabItem>
</Tabs>

## How to add a knowledge interaction?
<Tabs groupId="tke-usage">
<TabItem value="java" label="Java">

```java
AskKnowledgeInteraction askInteraction = new AskKnowledgeInteraction(graphPattern);
        sc.register(askInteraction);
```

</TabItem>
</Tabs>

## How to execute a Knowledge Interaction?
<Tabs groupId="tke-usage">
<TabItem value="java" label="Java">

```java
AskResult interactionResult = sc.ask(askInteraction, queryBindings).get();
```

</TabItem>
<TabItem value="JSON" label="Rest API">
Send a POST to `/sc/ask` to execute an Ask Knowledge Interaction.
To execute a Post Knowledge Interaction, send a POST to `/sc/post`.

Triggering an interaction requires you to provide two parameters:
* `Knowledge-Base-Id`: specifies the Knowledge Base Id for which to execute the ask
* `Knowledge-Interaction-Id`: specifies the Ask Knowledge Interaction that should be executed

In the body you can also specify a binding set, or a recipient selector *and* binding set.
The recipient selector can be used to select a single Knowledge Base Id which should be contacted.
The binding set specifies values that you are interested in. These must correspond to the variables in the graph pattern of the knowledge interaction.
```json
{
  "recipientSelector": {
    "knowledgeBases": []
  },
  "bindingSet": [
    {}
  ]
}
```
If you leave the array for `knowledgeBases` empty, then it will simply ask all relevant KBs.

</TabItem>
</Tabs>

## How to get the result of a Knowledge Interaction?
After executing a Knowledge Interaction, you can access the bindings from its result.
These bindings will provide actual values for the variables in the graph pattern of the interaction.

<Tabs groupId="tke-usage">
<TabItem value="java" label="Java">

```java
BindingSet resultBindings = interactionResult.getBindings();
```

</TabItem>
</Tabs>

## How to remove a knowledge interaction?
