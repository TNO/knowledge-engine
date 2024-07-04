---
---
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Knowledge Interactions

## How to instantiate a Knowledge Interaction?
<Tabs groupId="tke-usage">
<TabItem value="java" label="Java">

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
