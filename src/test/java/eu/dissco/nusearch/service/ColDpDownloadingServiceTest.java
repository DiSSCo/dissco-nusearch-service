package eu.dissco.nusearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.BDDMockito.given;

import eu.dissco.nusearch.exception.ColAuthenticationException;
import eu.dissco.nusearch.exception.ColExportRequestException;
import eu.dissco.nusearch.property.IndexingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.nusearch.service.ColDpDownloadingService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(MockitoExtension.class)
class ColDpDownloadingServiceTest {

  private static MockWebServer mockHandleServer;
  private final ObjectMapper mapper = new ObjectMapper();
  @Mock
  private IndexingProperties properties;
  private ColDpDownloadingService service;

  @BeforeAll
  static void init() throws IOException {
    mockHandleServer = new MockWebServer();
    mockHandleServer.start();
  }

  @AfterAll
  static void destroy() throws IOException {
    mockHandleServer.shutdown();
  }

  @BeforeEach
  void setup() {
    WebClient webClient = WebClient.create(
        String.format("http://%s:%s", mockHandleServer.getHostName(), mockHandleServer.getPort()));
    service = new ColDpDownloadingService(mapper, webClient, properties);
  }

  @AfterEach
  void cleanup() throws IOException {
    var path = Path.of("src/test/resources/download.zip");
    if (Files.exists(path)){
      Files.deleteIfExists(path);
    }
  }

  @Test
  void testPostHandle() throws Exception {
    // Given
    var pathLocation = "src/test/resources/download.zip";
    var responseBody = givenDownloadResponseBody();
    mockHandleServer.enqueue(new MockResponse()
        .setResponseCode(HttpStatus.CREATED.value())
        .setBody("\"6877d4ef-cc87-42f0-b922-f54133185840\""));
    mockHandleServer.enqueue(new MockResponse()
        .setResponseCode(HttpStatus.OK.value())
        .setBody(mapper.writeValueAsString(responseBody))
        .addHeader("Content-Type", "application/json"));
    mockHandleServer.enqueue(new MockResponse()
        .setResponseCode(HttpStatus.OK.value()));

    given(properties.getTempColdpLocation()).willReturn(pathLocation);

    // When
    var response = service.downloadColDpDataset();

    // Then
    assertThat(response).isEqualTo(Path.of(pathLocation));
  }

  @Test
  void testAuthenticationException() {
    // Given
    mockHandleServer.enqueue(new MockResponse()
        .setResponseCode(HttpStatus.UNAUTHORIZED.value()));

    // When/Then
    assertThrows(ColAuthenticationException.class, () -> service.downloadColDpDataset());
  }

  @Test
  void test4XXException() {
    // Given
    mockHandleServer.enqueue(new MockResponse()
        .setResponseCode(HttpStatus.BAD_REQUEST.value()));

    // When/Then
    assertThrows(ColExportRequestException.class, () -> service.downloadColDpDataset());
  }

  @Test
  void test5XXException() {
    // Given
    for (int i = 0; i < 4; i++) {
      mockHandleServer.enqueue(new MockResponse()
          .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }


    // When/Then
    assertThrows(ColExportRequestException.class, () -> service.downloadColDpDataset());
  }

  private JsonNode givenDownloadResponseBody() {
    return mapper.createObjectNode().put("download",
        String.format("http://%s:%s/%s", mockHandleServer.getHostName(), mockHandleServer.getPort(),
            "job/68/6877d4ef-cc87-42f0-b922-f54133185840.zip"));
  }
}
