package becky.demo.api_log_analysis.service;

import becky.demo.api_log_analysis.BaseTestClass;
import becky.demo.api_log_analysis.model.LogRecordDto;
import becky.demo.api_log_analysis.model.LogsError;
import becky.demo.api_log_analysis.resource.LogFileResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogAnalysisServiceImplTest extends BaseTestClass {

    @MockitoBean
    private LogFileResource logFileResource;

    @Autowired
    private LogAnalysisService logAnalysisService;

    private static final List<LogRecordDto> SAMPLE_RECORDS = List.of(
            LogRecordDto.builder().ipAddress("1.1.1.1").url("/health").build(),
            LogRecordDto.builder().ipAddress("2.2.2.2").url("/hello").build(),
            LogRecordDto.builder().ipAddress("2.2.2.2").url("/hello").build(),
            LogRecordDto.builder().ipAddress("2.2.2.2").url("/health").build(),
            LogRecordDto.builder().ipAddress("3.3.3.3").url("/health").build(),
            LogRecordDto.builder().ipAddress("3.3.3.3").url("/manage").build(),
            LogRecordDto.builder().ipAddress("3.3.3.3").url("/manage").build(),
            LogRecordDto.builder().ipAddress("4.4.4.4").url("/health").build(),
            LogRecordDto.builder().ipAddress("4.4.4.4").url("/").build()
    );
    @Autowired
    private ServerRequestObservationConvention serverRequestObservationConvention;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getLogReport_onValidInput_expectCorrectUniqueIpCount() {
        // Arrange
        List<String> expectedTopThreeIpAddresses = List.of("2.2.2.2", "3.3.3.3", "4.4.4.4");
        List<String> expectedTopThreeUrls = List.of("/health", "/manage", "/hello");

        when(logFileResource.parseLogFile(any(Path.class)))
                .thenReturn(Mono.just(SAMPLE_RECORDS));

        // Act
        StepVerifier.create(logAnalysisService.getLogReport(Path.of("/some/file.log")))
                .thenConsumeWhile(response -> {
                    // Assert
                    assertEquals(4, response.getNbUniqueIpAddresses(), "Expected 4 unique IP addresses");
                    assertTrue(expectedTopThreeIpAddresses.containsAll(response.getTopThreeIPAddresses()) &&
                                    response.getTopThreeIPAddresses().containsAll(expectedTopThreeIpAddresses),
                            "Expected IP addresses to match");
                    assertTrue(expectedTopThreeUrls.containsAll(response.getTopThreeUrls()) &&
                                    response.getTopThreeUrls().containsAll(expectedTopThreeUrls),
                            "Expected urls to match");
                    return true;
                })
                .verifyComplete();

        Mockito.verify(logFileResource, Mockito.times(1)).parseLogFile(any(Path.class));

    }

    @Test
    void getLogReport_onFewerThanThreeUniqueIps_expectReturnsAllAvailable() {
        List<LogRecordDto> twoIpRecords = List.of(
                LogRecordDto.builder().ipAddress("1.1.1.1").url("/health").build(),
                LogRecordDto.builder().ipAddress("2.2.2.2").url("/manage").build()
        );
        List<String> expectedTopThreeIpAddresses = List.of("1.1.1.1", "2.2.2.2");
        List<String> expectedTopThreeUrls = List.of("/health", "/manage");
        when(logFileResource.parseLogFile(any(Path.class)))
                .thenReturn(Mono.just(twoIpRecords));

        StepVerifier.create(logAnalysisService.getLogReport(Path.of("/some/file.log")))
                .thenConsumeWhile(response -> {
                    assertEquals(2, response.getTopThreeIPAddresses().size(), "Expected only 2 IPs when fewer than 3 unique values exist");
                    assertTrue(expectedTopThreeIpAddresses.containsAll(response.getTopThreeIPAddresses()) &&
                                    response.getTopThreeIPAddresses().containsAll(expectedTopThreeIpAddresses),
                            "Expected IP addresses to match");
                    assertTrue(expectedTopThreeUrls.containsAll(response.getTopThreeUrls()) &&
                                    response.getTopThreeUrls().containsAll(expectedTopThreeUrls),
                            "Expected urls to match");
                    return true;
                })
                .verifyComplete();

        Mockito.verify(logFileResource, Mockito.times(1)).parseLogFile(any(Path.class));
    }

    @Test
    void getLogReport_onEmptyLogFile_expectZeroUniqueIps() {
        when(logFileResource.parseLogFile(any(Path.class))).thenReturn(Mono.just(List.of()));

        StepVerifier.create(logAnalysisService.getLogReport(Path.of("/some/file.log")))
                .thenConsumeWhile(response -> {
                    assertEquals(0, response.getNbUniqueIpAddresses(), "Expected 0 unique IPs for empty log");
                    assertEquals(0, response.getTopThreeIPAddresses().size(), "Expected empty IP list for empty log");
                    assertEquals(0, response.getTopThreeUrls().size(), "Expected empty URL list for empty log");
                    return true;
                })
                .verifyComplete();

        Mockito.verify(logFileResource, Mockito.times(1)).parseLogFile(any(Path.class));
    }

    @Test
    void getLogReport_onResourceError_expectLogsErrorWithInternalServerError() {
        when(logFileResource.parseLogFile(any(Path.class)))
                .thenReturn(Mono.error(new RuntimeException("some error")));

        StepVerifier.create(logAnalysisService.getLogReport(Path.of("/some/file.log")))
                .consumeErrorWith(throwable -> {
                    assertInstanceOf(LogsError.class, throwable, "Expected LogsError wrapping the resource exception");
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ((LogsError) throwable).getHttpStatus(), "Expected INTERNAL_SERVER_ERROR status");
                })
                .verify();

        Mockito.verify(logFileResource, Mockito.times(1)).parseLogFile(any(Path.class));
    }

    @Test
    void getLogReport_onLogsError_expectLogsErrorRethrows() {
        when(logFileResource.parseLogFile(any(Path.class)))
                .thenReturn(Mono.error(new LogsError().builder()
                        .message("Some downstream error")
                        .errorCode("INTERNAL_SERVER_ERROR")
                        .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));

        StepVerifier.create(logAnalysisService.getLogReport(Path.of("/some/file.log")))
                .consumeErrorWith(throwable -> {
                    assertInstanceOf(LogsError.class, throwable, "Expected LogsError wrapping the resource exception");
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ((LogsError) throwable).getHttpStatus(), "Expected INTERNAL_SERVER_ERROR status");
                    assertEquals("Some downstream error", throwable.getMessage(), "Expected error to match");
                })
                .verify();

        Mockito.verify(logFileResource, Mockito.times(1)).parseLogFile(any(Path.class));
    }

    @Test
    void getLogReport_onNullPath_expectLogsErrorWithInternalServerError() {
        when(logFileResource.parseLogFile(nullable(Path.class)))
                .thenReturn(Mono.just(List.of()));


        LogsError exception = assertThrows(LogsError.class, () -> {
            logAnalysisService.getLogReport(null);
        });

        assertEquals("Error: fileUrl is an expected parameter", exception.getMessage(), "Expected matching error message");
        assertEquals("BAD_REQUEST", exception.getErrorCode(), "Expected BAD_REQUEST error code");
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus(), "Expected BAD_REQUEST status");
        Mockito.verify(logFileResource, Mockito.times(0)).parseLogFile(any(Path.class));
    }
}