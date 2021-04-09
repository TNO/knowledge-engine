FAQ
===

*Question*: may be a question of dummies, what is the time of validity of the information? is it specified in the graph pattern?
*Answer*: The validity of the information is not available by default, but you can include it into the graph pattern if you need it for your use case.


*Question*: There is a limited and defined number of KI. How do we proceed to "generate" all possible graph patterns and so associated KI ?
*Answer*: The KIs used should match and the KIs and their Graph Patterns are the end result of the SAREFization process for services. Also, if you need pilot specific KIs, you can agree on them within your pilot using SAREF or a pilot specific ontology.

*Question*: what about ACKs of the requests?
*Answer*: There are no explicit ACKs. The Ask receives an answer of one or more KBs and the Post receives the BindingSet for the optional Result Graph Pattern. If you need an explicit ACK (for example with the turn on the light example in the presentation) you can use the result Graph Pattern to contain the ACK.

*Question*: Is there any specific requirements such as RAM, CPU and disk space for deploying the KE ? Is there aditional components to take into account such as an external DB or it is all inclusive ? Is there OS specific configuration such as network or ports ?
*Answer*: We do not have minimal requirements for the KE yet. Locally I am running multiple Smart Connectors just fine on my Intel Core i7-8650 CPU @ 1.9GHz with 16 Gb RAM, but it also depends on the amount of traffic of course.
By the way, the current version (0.1.0) of the KE is a centralized one where all the Smart Connectors (with their reasoner) run on a server hosted by INESC TEC and partners create and access their Smart Connector via the REST Developer API of the generic adapter. A future version of the generic adapter will contain an instance of the Smart Connector (plus reasoner).
Currently, an instance of the Smart Connector is self contained, so no external database is required. The future version will of course need to use the network (ports are not yet decided) to communicate with other Smart Connectors (and the Knowledge Directory).
