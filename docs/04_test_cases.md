Test cases
==========

This section contains the test cases that describe the requirements (and tests for bugs) of the Knowledge Engine. They are divided using the [MoSCoW](https://en.wikipedia.org/wiki/MoSCoW_method) rules. In a future version, these Test Cases should be converted to Unit Tests.

Note that the *MUST HAVE* are basically all the features available in the current implementation of Knowledge Engine and the others like *SHOULD HAVE* are the ones missing in the current implementation.   

*M*UST HAVE 
------------
  * publish subscribe: 2 knowledge bases A & B: the first knowledgeIO A the other the counterpart of it knowledgeOI B. A publishes knowledge and B receives it in good fashion.
  * request response: 2 knowledge bases A & B: the first knowledgeIO A and the other the counterpart of it knowledgeIO B. A requests knowledge and B receives the request. B answers the request and A receives the response.
  * publish to new knowledge base: first 2 knowledge bases then 3 knowledge bases: knowledge base A publishes knowledge and knowledge base B receive it. Next, knowledge base C comes online. Knowledgebase A publishes new knowledge and now both B and C receive the knowledge.
  * do not publish to unavailable knowledge base: first 3 knowledge bases then 2 knowledge bases: knowledge base A publishes knowledge and knowledge base B and C receive it. Next, knowledge base C goes offline. Knowledge Base A publishes new knowledge and now only B recieves it.
  * request to new knowledge base: first 2 knowledgebases then 3 knowledge bases. Knowledge Base A requests knowledge and Knowledge Base B answers. Next, Knowledge Base C comes online. Knowledge Base A requests knowledge again and this time both B and C answer.
  * do not request from unavailable knowledge base: first 3 knowledge bases then 2 knowledge bases: knowledge base A requests knowledge and Knowledge Base B & C answer. Next, Knowledge Base C goes offline. Knowledge Base A requests knowledge again and this time only B answers.
  * variables do not matter in matching KnowledgeIOs: 2 KBs A & B have the same knowledgeIOs. KB A has output knowledgeIO with different variables than the input KnowledgeIO of KB B. KB A publishes knowledge and KB B receives it in good fashion.
  * prefixes do not matter in matching KnowledgeIOs: 2 KBs A & B have the same knowledgeIOs. KB A has output knowledgeIO which uses prefixes while KB B has input KnowledgeIO without prefixes. KB A publishes knowledge and KB B receives it in good fashion.   
  * answers are aggregated: 3 KBs A & B & C. KB A requests knowledge that both KB B & C have. KB B answers it and KB C answers it and KB A receives only a single answer which includes both answers from KB B & KB C.
  * askresult does not contain any duplicate bindings.

*S*HOULD HAVE
--------------
  * single question, single request (i.e. backward reasoner should allow multi headed backward rules): 2 knowledge bases: one asks a question (with only a single binding) and the other receives only a single request from the Knowledge Engine to answer this question (and not 6 requests).
  * single publish, single notification: 2 KBs A & B. A publishes knowledge with one-to-many relation (multiple bindings) and B receives all knowledge in a single request (multiple bindings).
  * Aggregate multiple rule applications: 2 KBs A & B. KB A requests knowledge with multiple bindings. KB B receives only a single request for knowledge with those multiple bindings instead of receiving mulitple requests for each binding separately.
  * 2 knowledge bases, propertyC derivable from propertyA propertyB (father)
  * knowledge base A: knows that he is a man
	knowledge base B: knows that he has a child
	knowledge base C: knows that if someone is a man and he has a child that he is a father
	knowledge base D: asks whether he is a father
	[rule: (?p rdf:type :Father) <- (?p rdf:type :Man) (?p rdf:type :Parent)]
  * orchestrate pull: 3 knowledge bases (KBs), KB A requests knowledge that a combination of knowledge base B & C can answer. KB A requests knowledge and the Knowledge Engine requests the similar knowledge from KB B. The result is send to KB C who transforms it into the knowledge KB A actually requested.
  * orchestrate push: 3 knowledge bases, KB B pushes knowledge that KB a wants only after it is transformed by KB C. KB B pushes knowledge and the Knowledge Engine first sends this knowledge to KB C. KB C transforms the knowledge into the knowledge KB A actually is interested in and after this transformation it is received by KB A.
  * multiple filters should propagate (i.e. backward reasoner should allow multi headed backward rules): 2 KBs A & B. Knowledge Base A requests knowledge with bindings for a between filter on dates. KB B receives a single request including both start and end date bindings. KB B only returns knowledge that falls within the date range and KB A receives it correctly.
  * remove knowledge: 2 KBs A & B: KB A publishes knowledge and KB B receives it in good fashion. Now KB A deletes knowledge and KB B receives a notification that the Knowledge has been removed.
  * update knowledge: 2 KBs A & B: KB A publishes knowledge and KB B receives it in good fashion. Now KB A updates the knowledge (i.e. delete and insert in a single translation) and KB B receives the modified knowledge.
  * publish many KBs: 1001 KBs A & B1 to B1000: KB A publishes knowledge and KB B1 to B1000 receive it timely and it good fashion.
  * request many KBs: 1001 KBs A & B1 to B1000: KB A requests knowledge and KB B1 to B1000 answer it. KB A receives their aggregated answers.
	
*C*OULD HAVE
-------------
  * dynamic capabilities: 3 knowledge bases A, B and C: A has a particular KnowledgeIO of which B has the counterpart and C has a slightly changed counterpart. KB A request data and KB B answers as it should. Then KB A dynamically changes its KnowledgeIO and now B should not answer it anymore, but C should answer it.
  * KnowledgeIO checking: knowledge A registers a KnowledgeIO with a small typo in the graph pattern. The Smart Connector should return a warning.
  * KB INSERTS knowledge when Knowledge Engine stores knowledge for KB: 3 KBs A & B & C. Knowledge Base A publishes knowledge and KB B receives this knowledge. Then KB C requests the knowledge and KB A does not need to answer this request, because the knowledge engine stored its knowledge and can return the answer to KB C itself.
  * KB DELETES knowledge when Knowledge Engine stores knowledge for KB: see previous test case. Additional step: KB A removes the knowledge and KB B receives a notification of the removal and afterwards KB C requests the knowledge and the knowledge is no longer returned (and the Knowledge Engine handles this all itself and does not bother KB A).
  * allow multiple requests: 3 KBs A & B & C. KB A requests knowledge and KB B takes a long time to answer this request (it might be a human KB). In the meantime, KB A requests other knowledge and KB C answers to it. Then, KB B also answers it request.
  * allow many multiple requests: 1001 KBs A and B1 to B1000. KB A requests knowledge that only KB B1 has but takes a long time to answer. Then KB A requests knowledge that only KB B2 has but takes a long time to answer, etc to B1000. There are 1000 outstanding requests from KB A to KB B1 to B1000. After a while all of the answers are received correctly and it timely manner (shortly after KB B? finished the work).
  * explain answers: 3 KBs A & B & C. KB A requests knowledge and both KB B & C provide their answer. Now KB A requests an explanation of the answer and receives the info that the answer was a combination of KB B & C's answer.
  * get all knowledge: 3 KBs A & B & C. KB A requests *all* knowledge and KB B & C provide all their knowledge as an answer.
  * get notified by all changes: 3 KBs A & B & C. KB wants to get notified of all knowledge (wildcard graph pattern). KB B & C publish knowledge and KB A receives both of it.
  * graph patterns support one-to-many relations: 2 KBs A & B. A's KnowledgeIO represents a father with one or more children and KB B knowledgeIO matches this. KB requests data and KB B responds.

*W*ON'T HAVE
-------------