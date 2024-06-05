---
---

# Knowledge Interactions

## How to instantiate a Knowledge Interaction?
### Java
```java
// ASK:
AskKnowledgeInteraction askInteraction = new AskKnowledgeInteraction(graphPattern);
smartConnector.register(askInteraction);

// ANSWER:
AnswerKnowledgeInteraction answerInteraction = new AnswerKnowledgeInteraction(graphPattern);
smartConnector.register(answerInteraction);

// POST:
PostKnowledgeInteraction postInteraction = new PostKnowledgeInteraction(graphPattern);
smartConnector.register(postInteraction);

// REACT:
ReactKnowledgeInteraction reactInteraction = new ReactKnowledgeInteraction(graphPattern);
smartConnector.register(reactInteraction);
```

### REST API

## How to add a knowledge interaction?

## How to execute a Knowledge Interaction?
### Java
```java
AskResult interactionResult = sc.ask(askInteraction, queryBindings).get();
```

## How to get the result of a Knowledge Interaction?
After executing a Knowledge Interaction, you can access the bindings from its result.
These bindings will provide actual values for the variables in the graph pattern of the interaction.
```java
BindingSet resultBindings = interactionResult.getBindings();
```

## How to remove a knowledge interaction?
