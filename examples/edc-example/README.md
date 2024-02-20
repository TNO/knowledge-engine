# TKE-EDC Example
This example uses EDC-IDS Connectors for communication between two Knowledge Engine Runtimes (KERs).
All messages that are sent contain an authentication code.
If a message is received, the authentication code is validated unless it is a meta Knowledge Interaction.

Currently, each KER has a properties file that sets the authentication code and validation endpoint.

## Running the example
This example requires two EDC connectors to be started and the use of the tke-edc-manager.
Execute the following steps to run the example combined with them:
1. In the tke-edc-manager, run `docker compose up` to start two EDC connectors
2. In the tke-edc-manager, run `./mvnw clean install` followed by `./mvnw spring-boot:run` to start the Spring Boot application that can be used to negotiate contracts.
3. Open your browser and go to `localhost:8080/swagger-ui/index.html`
4. In your browser, exexcute a `/configure-connector` request with participant-id: `consumer`
5. In your browser, execute a `/configure-connector` request with participant-id: `provider`
6.  In your browser, execute a `/negotiate-contract` request with participant-id: `consumer` and counter-party-participant-id: `provider`.
7. Copy the `contractAgreementId` in the response from the `/negotiate-contract` request
8. In your browser, execute a `/transfer-process` request with participant-id: `consumer`, counter-party-participant-id: `provider` and the contract-agreement-id set to the id copied in the previous step.
9. Repeat steps 6-8 with the `provider` as participant-id, and `consumer` as counter-party-participant-id
10. In your browser, execute a `/tokens` request where participant-id: `consumer` and counter-party-participant-id: `provider`
11. There should be two entries in the returned array. Each entry contains an `authCode` which should be copied to the properties file of the appropriate KER.
12. Set the validationEndpoint token in the properties file of each KER.
13. In the edc-example directory in this project, execute `docker compose up`

If you have run the example previously, execute `docker compose down` and `docker compose build` before `docker compose up`.