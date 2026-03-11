package becky.demo.api_log_analysis.controller;

import becky.demo.api_log_analysis.BaseTestClass;
import becky.demo.api_log_analysis.model.LogsError;
import becky.demo.api_log_analysis.service.LogAnalysisService;
import becky.demo.contract_api_log_analysis.models.ErrorResponse;
import becky.demo.contract_api_log_analysis.models.LogReportResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;


@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LogAnalysisApiControllerTest extends BaseTestClass {

    @MockitoBean
    private LogAnalysisService logAnalysisService;

    @Autowired
    private WebTestClient webTestClient;

    @TempDir
    private Path tempDir;

    private static final LogReportResponse MOCK_RESPONSE = LogReportResponse.builder()
            .nbUniqueIpAddresses(3)
            .topThreeUrls(List.of("/hello", "/manage", "/health"))
            .topThreeIPAddresses(List.of("1.1.1.1", "2.2.2.2", "3.3.3.3"))
            .build();

    @BeforeAll
    void setup() {
        webTestClient = webTestClient.mutate()
                .responseTimeout(Duration.ofMillis(30000))
                .build();
    }


    @Test
    void testGetLogReport_onValidInput_expect200ResponseWithBody() throws Exception{
        // Arrange
        String inputFileName = "test.log";
        Path logFile = tempDir.resolve(inputFileName);
        Files.writeString(logFile, "");

        Mockito.when(logAnalysisService.getLogReport(any(Path.class)))
                .thenReturn(Mono.just(MOCK_RESPONSE)
                        .delayElement(Duration.ofMillis(500)));

        // Act
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/reports")
                        .queryParam("fileUrl", logFile.toAbsolutePath().toString())
                        .build())
                .header("x-api-key", "DUMMY_VALUE")
                .exchange()

                // Assert
                .expectStatus().isEqualTo(200)
                .expectBody(LogReportResponse.class)
                .isEqualTo(MOCK_RESPONSE);

        Mockito.verify(logAnalysisService, Mockito.times(1)).getLogReport(any(Path.class));
    }

    @Test
    void getLogReport_onBlankFileUrl_expect400Response() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/reports")
                        .queryParam("fileUrl", "")
                        .build())
                .header("x-api-key", "DUMMY_VALUE")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals("BAD_REQUEST", response.getErrorCode(), "Expected error code to be BAD_REQUEST");
                    assertEquals("Error: fileUrl is an expected parameter", response.getErrorMessage(), "Expected error message to match");
                });
    }

    @Test
    void getLogReport_onNonExistentFile_expect400Response() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/reports")
                        .queryParam("fileUrl", "/nonexistent/path/file.log")
                        .build())
                .header("x-api-key", "DUMMY_VALUE")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals("BAD_REQUEST", response.getErrorCode(), "Expected BAD_REQUEST error code for missing file");
                    assertEquals("File not found: /nonexistent/path/file.log", response.getErrorMessage(), "Expected error message to match");
                });
    }

    @Test
    void getLogReport_onMissingQueryParam_expect400Response() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/reports")
//                        .queryParam("fileUrl", "/nonexistent/path/file.log")
                        .build())
                .header("x-api-key", "DUMMY_VALUE")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals("BAD_REQUEST", response.getErrorCode(), "Expected BAD_REQUEST error code for missing file");
                    assertEquals("400 BAD_REQUEST \"Required query parameter 'fileUrl' is not present.\"", response.getErrorMessage(), "Expected error message to match");
                });
    }

    @Test
    void getLogReport_onEmptyLogReportResponse_expect200WithZeroValues() throws Exception {
        Path logFile = tempDir.resolve("empty.log");
        Files.writeString(logFile, "");

        LogReportResponse emptyResponse = LogReportResponse.builder().build();

        Mockito.when(logAnalysisService.getLogReport(any(Path.class)))
                .thenReturn(Mono.just(emptyResponse));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/v1/reports")
                        .queryParam("fileUrl", logFile.toAbsolutePath().toString()).build())
                .header("x-api-key", "DUMMY_VALUE")
                .exchange()
                .expectStatus().isOk()
                .expectBody(LogReportResponse.class)
                .value(response -> {
                    assertEquals(null, response.getNbUniqueIpAddresses());
                    assertTrue(response.getTopThreeIPAddresses().isEmpty());
                });
    }

    @Test
    void getLogReport_onServiceError_expect500Response() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile, "");

        Mockito.when(logAnalysisService.getLogReport(any(Path.class)))
                .thenReturn(Mono.error(new RuntimeException("unexpected failure")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/reports")
                        .queryParam("fileUrl", logFile.toAbsolutePath().toString())
                        .build())
                .header("x-api-key", "DUMMY_VALUE")
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals("INTERNAL_SERVER_ERROR", response.getErrorCode(), "Expected INTERNAL_SERVER_ERROR code for unhandled exception");
                    assertEquals("An unexpected error occurred. Please contact support if the issue persists.", response.getErrorMessage(), "Expected error message to contain generic value");
                });
    }

    @Test
    void getLogReport_onKnownLogsError_expectMappedErrorResponse() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile, "");

        LogsError logsError = LogsError.builder()
                .message("Something domain-specific went wrong")
                .errorCode("NOT_FOUND")
                .httpStatus(HttpStatus.NOT_FOUND)
                .build();

        Mockito.when(logAnalysisService.getLogReport(any(Path.class)))
                .thenReturn(Mono.error(logsError));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/reports")
                        .queryParam("fileUrl", logFile.toAbsolutePath().toString())
                        .build())
                .header("x-api-key", "DUMMY_VALUE")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals("NOT_FOUND", response.getErrorCode(), "Expected error code to match LogsError");
                    assertEquals("Something domain-specific went wrong", response.getErrorMessage(), "Expected error message to match LogsError");
                });
    }


    @Test
    void getLogReport_onServiceError_expectTrackingIdInResponse() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile, "");
        String trackingId = "test-tracking-id-123";

        Mockito.when(logAnalysisService.getLogReport(any(Path.class)))
                .thenReturn(Mono.error(new RuntimeException("unexpected failure")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/reports")
                        .queryParam("fileUrl", logFile.toAbsolutePath().toString())
                        .build())
                .header("x-api-key", "DUMMY_VALUE")
                .header("x-tracking-id", trackingId)
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(trackingId, response.getTrackingId(), "Expected tracking ID from request header to be echoed in response");
                });
    }

    @Test
    void getLogReport_onServiceError_expectGeneratedTrackingIdWhenNoneProvided() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile, "");

        Mockito.when(logAnalysisService.getLogReport(any(Path.class)))
                .thenReturn(Mono.error(new RuntimeException("unexpected failure")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/reports")
                        .queryParam("fileUrl", logFile.toAbsolutePath().toString())
                        .build())
                .header("x-api-key", "DUMMY_VALUE")
                // no x-tracking-id header
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertNotNull(response.getTrackingId(), "Expected a generated tracking ID when none was provided");
                    assertFalse(response.getTrackingId().isBlank(), "Expected generated tracking ID to not be blank");
                });
    }

    @Test
    void getLogReport_onNullPointerException_expect500Response() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile, "");

        Mockito.when(logAnalysisService.getLogReport(any(Path.class)))
                .thenReturn(Mono.error(new NullPointerException()));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/reports")
                        .queryParam("fileUrl", logFile.toAbsolutePath().toString())
                        .build())
                .header("x-api-key", "DUMMY_VALUE")
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals("INTERNAL_SERVER_ERROR", response.getErrorCode(), "Expected INTERNAL_SERVER_ERROR for NullPointerException");
                    assertEquals("An unexpected error occurred. Please contact support if the issue persists.",
                            response.getErrorMessage(), "Expected generic safe message for NullPointerException");
                });
    }

    @Test
    void getLogReport_onIllegalArgumentException_expect400Response() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile, "");

        Mockito.when(logAnalysisService.getLogReport(any(Path.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("invalid argument supplied")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/reports")
                        .queryParam("fileUrl", logFile.toAbsolutePath().toString())
                        .build())
                .header("x-api-key", "DUMMY_VALUE")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals("BAD_REQUEST", response.getErrorCode(), "Expected BAD_REQUEST for IllegalArgumentException");
                    assertEquals("invalid argument supplied", response.getErrorMessage(), "Expected exception message to be propagated");
                });
    }

    @Test
    void getLogReport_onIllegalArgumentExceptionWithNullMessage_expectDefaultErrorMessage() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile, "");

        Mockito.when(logAnalysisService.getLogReport(any(Path.class)))
                .thenReturn(Mono.error(new IllegalArgumentException((String) null)));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/reports")
                        .queryParam("fileUrl", logFile.toAbsolutePath().toString())
                        .build())
                .header("x-api-key", "DUMMY_VALUE")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals("Invalid request parameters.", response.getErrorMessage(),
                            "Expected default message when IllegalArgumentException has null message");
                });
    }
}