package eu.knowledge.engine.smartconnector.edc;

import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

import static eu.knowledge.engine.smartconnector.edc.JsonUtil.findByJsonPointerExpression;

public class EdcConnectorClient {

  private final Logger LOG = LoggerFactory.getLogger(EdcConnectorClient.class);
  private final String edcConnectorManagementUrl;
  private final HttpClient httpClient;

  public EdcConnectorClient(String edcConnectorManagementUrl) {
    this.edcConnectorManagementUrl = edcConnectorManagementUrl;
    var executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    this.httpClient = HttpClient.newBuilder().executor(executorService).build();
  }

  /**
   * Each TKE needs to register a dataplane whenever it wants to expose assets.
   * This function registers a data plane for HttpProxy/HttpData.
   */
  public String registerDataPlane(String dataPlaneId, String dataPlaneControlUrl, String dataPlanePublicUrl) {
    var url = getManagementUrl("/v2/dataplanes");
    LOG.info("registerDataPlane at: {}", url);
    var payload = """
        {
          "@context": {
            "edc": "https://w3id.org/edc/v0.0.1/ns/"
          },
          "@id": "%s",
          "url": "%s",
          "allowedSourceTypes": [
            "HttpData"
          ],
          "allowedDestTypes": [
            "HttpProxy",
            "HttpData"
          ],
          "properties": {
            "https://w3id.org/edc/v0.0.1/ns/publicApiUrl": "%s"
          }
        }
      """.formatted(dataPlaneId, dataPlaneControlUrl, dataPlanePublicUrl);
    HttpResponse<String> response = httpPost(url, payload);
    LOG.info("Register data plane response: {}", response.body());
    return response.body();
  }

  /**
   * Register an assert given the provided assetId and tkeUrl.
   */
  public String registerAsset(String assetId, String tkeUrl, String tkeAssetName) {
    var url = getManagementUrl("/v3/assets");
    LOG.info("registerAsset at: {}", url);
    var payload = """
      {
        "@context": [
          "https://w3id.org/edc/connector/management/v0.0.1"
        ],
        "@id": "%s",
        "properties": {
          "name": "%s",
          "contenttype": "application/json"
        },
        "dataAddress": {
          "type": "HttpData",
          "name": "%s",
          "baseUrl": "%s",
          "proxyMethod": "true",
          "proxyPath": "true",
          "proxyBody": "true"
        }
      }
      """.formatted(assetId, tkeAssetName, tkeAssetName, tkeUrl);
    HttpResponse<String> response = httpPost(url, payload);
    LOG.info("Register asset response: {}", response.body());
    return response.body();
  }

  /**
   * The policy defines permissions which can be applied to an asset.
   *
   * @return
   */
  public String registerPolicy(String policyId) {
    var url = getManagementUrl("/v3/policydefinitions");
    LOG.info("getManagementUrl at: {}", url);
  
    var payload = """
      {
        "@context": {
          "edc": "https://w3id.org/edc/v0.0.1/ns/",
          "odrl": "http://www.w3.org/ns/odrl/2/"
        },
        "@id": "%s",
        "policy": {
          "@context": "http://www.w3.org/ns/odrl.jsonld",
          "@type": "Set",
          "odrl:permission": [],
          "odrl:prohibition": [],
          "odrl:obligation": []
        }
      }
      """.formatted(policyId);
    
    HttpResponse<String> response = httpPost(url, payload);
    LOG.info("Register policy response: {}", response.body());
    return response.body();
  }

  public String registerContractDefinition(String contractDefinitionId, String accessPolicyId, String contractPolicyId, String assetId) {
    var url = getManagementUrl("/v3/contractdefinitions");
    LOG.info("registerContractDefinition at: {}", url);
    var payload = """
      {
        "@context": {
          "edc": "https://w3id.org/edc/v0.0.1/ns/"
        },
        "@id": "%s",
        "accessPolicyId": "%s",
        "contractPolicyId": "%s",
        "assetsSelector": [
          {
            "operandLeft": "id",
            "operator": "=",
            "operandRight": "%s"
          }
        ]
      }
      """.formatted(contractDefinitionId, accessPolicyId, contractPolicyId, assetId);
    HttpResponse<String> response = httpPost(url, payload);
    LOG.info("Register contract definition response: {}", response.body());
    return response.body();
  }

  /**
   * CataLOG requests are sent to ones own connector.
   *
   * @param counterPartyAddress
   * @return
   */
  public String catalogRequest(String counterPartyAddress, String countParticipantId) {
    var url = getManagementUrl("/v3/catalog/request");
    LOG.info("catalogRequest at: {}", url);
    var payload = """
      {
        "@context": {
          "edc": "https://w3id.org/edc/v0.0.1/ns/"
        },
        "counterPartyAddress": "%s",
        "counterPartyId": "%s",
        "protocol": "dataspace-protocol-http"
      }
      """.formatted(counterPartyAddress, countParticipantId);
    HttpResponse<String> postResponse = httpPost(url, payload);
    LOG.info("CataLOG request response: {}", postResponse.body());
    return postResponse.body();
  }

  /**
   * In order to request any data, a contract gets negotiated, and an agreement is resulting has to be negotiated between providers and consumers.
   * <p>
   * The consumer now needs to initiate a contract negotiation sequence with the provider. That sequence looks as follows:
   * <p>
   * Consumer sends a contract offer to the provider (currently, this has to be equal to the provider's offer!)
   * Provider validates the received offer against its own offer
   * Provider either sends an agreement or a rejection, depending on the validation result
   * In case of successful validation, provider and consumer store the received agreement for later reference
   *
   * @return
   */
  public String negotiateContract(String consumerParticipantId, String providerParticipantId, String counterPartyAddress, String assetId) {
    var catalogRequest = catalogRequest(counterPartyAddress, providerParticipantId);
    String catalogOfferIdForAsset = findByJsonPointerExpression(catalogRequest, "/dcat:dataset/odrl:hasPolicy/@id");

    var url = getManagementUrl("/v2/contractnegotiations");
    LOG.info("negotiateContract at: {}", url);
    var payload = """
      {
        "@context": {
          "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
        },
        "@type": "NegotiationInitiateRequestDto",
        "consumerId": "%s",
        "connectorId": "%s",
        "providerId": "%s",
        "counterPartyAddress": "%s",
        "protocol": "dataspace-protocol-http",
        "policy": {
          "@context": "http://www.w3.org/ns/odrl.jsonld",
          "@id": "%s",
          "@type": "Set",
          "permission": [],
          "prohibition": [],
          "obligation": [],
          "target": "%s"
        }
      }
      """.formatted(consumerParticipantId, providerParticipantId, providerParticipantId, counterPartyAddress, catalogOfferIdForAsset, assetId);

    HttpResponse<String> postResponse = httpPost(url, payload);
    LOG.info("Negotiate contract response: {}", postResponse.body());
    return postResponse.body();
  }

  public String contractAgreement(String json) {
    String contractAgreementId = findByJsonPointerExpression(json, "/@id");
    var url = getManagementUrl("/v2/contractnegotiations/" + contractAgreementId);
    LOG.info("contractAgreement at: {}", url);
    final List<String> responses = new ArrayList<>();

    Awaitility.await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10)).until(() -> {
      HttpResponse<String> response = httpGet(url);
      responses.add(response.body());
      String state = findByJsonPointerExpression(response.body(), "/state");
      return Objects.equals(state, "FINALIZED");
    });

    return responses.get(responses.size() - 1);
  }

  public String transferProcess(String counterPartyAddress, String counterPartyParticipantId, String contractAgreementId, String assetId) {
    var url = getManagementUrl("/v2/transferprocesses");
    LOG.info("transferProcess at: {}", url);
    var payload = """
        {
          "@context": {
            "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
          },
          "@type": "TransferRequestDto",
          "counterPartyAddress": "%s",
          "connectorId": "%s",
          "contractId": "%s",
          "assetId": "%s",
          "protocol": "dataspace-protocol-http",
          "dataDestination": {
            "type": "HttpProxy"
          }
        }
      """.formatted(counterPartyAddress, counterPartyParticipantId, contractAgreementId, assetId);
    HttpResponse<String> postResponse = httpPost(url, payload);
    LOG.info("Transfer process response: {}", postResponse.body());
    return postResponse.body();
  }

  /**
   * Contract the EDC connector URL for the /management endpoint.
   */
  private String getManagementUrl(String suffix) {
    return edcConnectorManagementUrl + suffix;
  }

  private HttpResponse<String> httpGet(String url) {
    return httpGet(url, "application/json");
  }

  private HttpResponse<String> httpGet(String url, String accept) {
    LOG.info("Calling: {}", url);

    HttpRequest request = HttpRequest.newBuilder()
      .uri(toURI(url))
      .headers("Accept", accept)
      .GET()
      .build();

    return sendRequest(request);
  }

  private HttpResponse<String> httpPost(String url, String payload) {
    return httpPost(url, "application/json", payload);
  }

  private HttpResponse<String> httpPost(String url, String contentType, String payload) {
    LOG.info("Calling: {}, Payload: {}", url, payload);

    HttpRequest request = HttpRequest.newBuilder()
      .uri(toURI(url))
      .headers("Content-Type", contentType)
      .POST(HttpRequest.BodyPublishers.ofString(payload))
      .build();

    return sendRequest(request);
  }

  private HttpResponse<String> sendRequest(HttpRequest request) {
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      int statusCode = response.statusCode();

      if (statusCode >= 200 && statusCode <= 299) {
        return response;
      } else if (response.statusCode() >= 400 && statusCode <= 499) {
        throw new RuntimeException("HttpClient exception, request failed with statusCode: " + statusCode + ", response: " + response.body());
      } else if (response.statusCode() >= 500) {
        throw new RuntimeException("HttpServer exception, request failed with statusCode: " + statusCode + ", response: " + response.body());
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return null;
  }

  private URI toURI(String url) {
    try {
      return new URI(url);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

}