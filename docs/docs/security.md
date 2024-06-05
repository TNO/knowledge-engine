---
  sidebar_position: 11
---

# Security
Within the KE we distinguish between several parts of the security question and explain below how we expect to deal with those:

1) **Connection between Knowledge Base (KB) and Smart Connector (SC)**: When using _Java_ Developer API this is secure by default, however, when using the REST Developer API we need to be more careful. The Generic Adapter and Service Store already provide some solution for this.
2) **Connection between SC and SC** (when in different Java Virtual Machines): By using a configurable Exposed URL, the KE remains flexible with respect to the setup it is used in. This Exposed URL can point directly to the SC or to a proxy that is configured for HTTPS with certificates.
3) **Connection between SC and Knowledge Directory (KD)**: The Knowledge Directory itself exposes a REST API that we recommend to be put behind a HTTPS proxy. SCs have a configuration option to point to the URL of the Knowledge Directory. This can be both HTTP and HTTPS.
4) **Identification/authentication**: Still unclear, but we are thinking about *not* introducing the concept of a user within the KE. The reason is that the only way other KBs can really trust a user, is by having a centralized Identity Provider that all KBs can access which constrasts the distributed nature of the KE. But again, not sure about whether we can maintain this position. Sharing login credentials and JWT tokens through the KE by including them in the domain knowledge is of course possible and this indeed requires the ontologies to contain classes and properties related to login and token information.
5) **Authorization**: In the future we want to support roles and access control policies that are agreed upon by all KBs and it should make up the domain knowledge together with the ontology. We will definitely work on this in the future, but probably not in the scope of InterConnect.


## Sharing credentials

As I mentioned in my previous comment there is no _right_ way to deal with sharing credentials. More discussion and experience is definitely necessary to find the sweet spot for security related issues like these. In your use cases, the easiest and safest way is probably to share the credentials outside of the Knowledge Engine and just use some token (or home id) in the graph pattern, but it might be interesting to see how actually sending the credentials through the KE works out using a login graph pattern. Keep in mind, though, that the KE does not encrypt anything by itself and that the credentials might be visible to others.

## Using Basic Authentication to secure data exchange

The communication in a Knowledge Network can be secured using Basic Authentication and HTTPS. This requires the usage of reverse proxies (like [NGINX](https://www.nginx.org/)) in front of the KD and KE runtimes. In such scenario, this reverse proxy handles all HTTPS and Basis Authentication configuration and forwards traffic over HTTP and without basic authentication to the KD and KE Runtime. To facilitate this, the KE supports conveying user credentials (username and password) within both the KD_URL and KE_RUNTIME_EXPOSED_URL environment variables like this `https://username:password@www.example.org/keruntime`. Whenever these URLs contains user credentials, the KER uses these credentials to connect to the Knowledge Directory or Knowledge Engine Runtime, respectively. An example of using Basic Authentication (without HTTPS) in distributed mode can be found in the within the Knowledge Engine repository at `examples/authentication/`.
