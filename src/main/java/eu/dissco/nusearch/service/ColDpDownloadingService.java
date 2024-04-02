package eu.dissco.nusearch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.nusearch.exception.ColAuthenticationException;
import eu.dissco.nusearch.exception.ColExportRequestException;
import eu.dissco.nusearch.property.ColDownloadProperties;
import eu.dissco.nusearch.property.IndexingProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Service
@AllArgsConstructor
public class ColDpDownloadingService {

  private final ObjectMapper mapper;
  private final WebClient webClient;
  private final IndexingProperties properties;
  private final ColDownloadProperties colDownloadProperties;

  public static boolean isRetryableServerError(Throwable throwable, boolean downloadRetry) {
    if (throwable instanceof WebClientResponseException webClientResponseException) {
      return webClientResponseException.getStatusCode().is5xxServerError() || (downloadRetry
          && webClientResponseException.getStatusCode().equals(HttpStatus.NOT_FOUND));
    } else {
      return false;
    }
  }

  public Path downloadColDpDataset() throws Exception {
    var exportId = createExport();
    var downloadUrl = getDownloadUrl(exportId, 0);
    return downloadColDp(downloadUrl);
  }

  private Path downloadColDp(String downloadUrl)
      throws Exception {
    Path path = Paths.get(properties.getTempColDpLocation());
    Flux<DataBuffer> flux = webClient
        .get()
        .uri(downloadUrl)
        .retrieve()
        .bodyToFlux(DataBuffer.class);

    DataBufferUtils.write(flux, path).block();

    log.info("Successfully downloaded the colDp, total fileSize: {}", Files.size(path));
    return path;
  }

  private String getDownloadUrl(String exportId, int retryCount)
      throws ColExportRequestException, ColAuthenticationException, InterruptedException {
    log.info(
        "Trying to retrieve download URL for export: {} May take several minutes/retries if the dataset is new",
        exportId);
    var requestResult = requestExport(HttpMethod.GET, "/export/" + exportId, null, JsonNode.class,
        true);
    var response = getFutureResponse(requestResult);
    if (!response.get("status").asText().equals("finished")) {
      if (retryCount > colDownloadProperties.getExportStatusRetryCount()) {
        log.error("Checklistbank export is not ready after {} retries. Export ID: {}",
            colDownloadProperties.getExportStatusRetryCount(), exportId);
        throw new ColExportRequestException(
            "Failed to retrieve download URL for export: " + exportId
                + ". Export is not ready after " + colDownloadProperties.getExportStatusRetryCount()
                + " retries.");
      } else {
        log.info("Checklistbank export is not ready yet. Retrying in 500ms");
        Thread.sleep(colDownloadProperties.getExportStatusRetryTime());
        retryCount = retryCount + 1;
        return getDownloadUrl(exportId, retryCount);
      }
    } else {
      log.info("Checklistbank successfully created download: {}",
          response.toPrettyString());
      return response.get("download").asText();
    }
  }

  private String createExport() throws ColExportRequestException, ColAuthenticationException {
    log.info("Requesting export from CheckListBand for dataset: {}",
        properties.getColDataset());
    var requestBody = BodyInserters.fromValue(buildRequestBody());
    var uri = "/dataset/" + properties.getColDataset() + "/export";
    var requestResult = requestExport(HttpMethod.POST, uri, requestBody, String.class, false);
    var exportId = getFutureResponse(requestResult);
    log.info("Checklistbank successfully created export. Export ID: {}",
        exportId);
    return exportId.replace("\"", "");
  }

  private String getAuth() {
    return "Basic " + Base64.getEncoder().encodeToString(
        (properties.getColUsername() + ":" + properties.getColPassword()).getBytes());
  }

  private <T> Mono<T> requestExport(HttpMethod method, String uri,
      BodyInserter<JsonNode, ReactiveHttpOutputMessage> requestBody, Class<T> responseType,
      boolean downloadRetry) {
    var request = webClient.method(method)
        .uri(uri);
    if (requestBody != null) {
      request.body(requestBody);
    }
    var retrieve = request.acceptCharset(StandardCharsets.UTF_8)
        .header("Authorization", getAuth())
        .retrieve()
        .onStatus(HttpStatus.UNAUTHORIZED::equals, r -> Mono.error(
            new ColAuthenticationException("Unable to authenticate with Checklistbank.")));
    if (!downloadRetry) {
      retrieve.onStatus(HttpStatusCode::is4xxClientError,
          r -> Mono.error(new ColExportRequestException(
              "Unable to create export of Checklist bank. Response from API: " + r.statusCode())));
    }
    return retrieve.bodyToMono(responseType).retryWhen(
        Retry.fixedDelay(3, Duration.ofSeconds(2))
            .filter(ex -> isRetryableServerError(ex, downloadRetry))
            .onRetryExhaustedThrow(
                (retryBackoffSpec, retrySignal) -> new ColExportRequestException(
                    "External Service failed to process after max retries")));
  }


  private <T> T getFutureResponse(Mono<T> response)
      throws ColExportRequestException, ColAuthenticationException {
    try {
      return response.toFuture().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted exception has occurred.");
      throw new ColExportRequestException(
          "Interrupted execution: A connection error has occurred in creating a handle.");
    } catch (ExecutionException e) {
      if (e.getCause().getClass().equals(ColAuthenticationException.class)) {
        log.error(
            "Unable to authenticated with Checklistbank with the provided credentials.");
        throw new ColAuthenticationException(e.getCause().getMessage());
      }
      log.error(
          "An unexpected exception has occurred while calling the export API of Checklistbank", e);
      throw new ColExportRequestException(e.getCause().getMessage());
    }
  }

  private JsonNode buildRequestBody() {
    var json = mapper.createObjectNode();
    json.put("format", "coldp");
    json.put("synonyms", colDownloadProperties.isSynonyms());
    json.put("extinct", colDownloadProperties.isExtinct());
    json.put("extended", colDownloadProperties.isExtended());
    return json;
  }
}
