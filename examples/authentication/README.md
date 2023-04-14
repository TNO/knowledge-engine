# Multiple runtimes with authentication

Authentication is supported, and should be configured in a reverse proxy.

This Docker Compose project is a simple example that protects the Knowledge Directory and communication between the runtimes with BasicAuth.

The Knowledge Engine receives the credentials for connecting to the Knowledge Directory *in the `KD_URL` env variable*, specifically in the userinfo part.

This example uses the same credentials for all three resources (KD, runtime 1 inter-ker, and runtime 2 inter-ker), but it should be noted that they can (and should) be different for each resource.

__It should be noted that this current solution can be a security risk, because the credentials are shared from the Knowledge Directory to the Knowledge Engine in plain text format.__

## Running this example

To run this example:

```bash
docker compose up
```
