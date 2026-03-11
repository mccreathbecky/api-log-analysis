package becky.demo.api_log_analysis.resource;

import becky.demo.api_log_analysis.model.LogsError;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LogFileResourceImplTest {

    @TempDir
    private Path tempDir;

    private static LogFileResource resource;

    @BeforeAll
    static void setUp() {
        resource = new LogFileResourceImpl();
    }

    @Test
    void parseLogFile_onValidFile_expectParsesAllLines() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile,
                "177.71.128.21 - - [10/Jul/2018:22:21:28 +0200] \"GET /index HTTP/1.1\" 200 3574 \"-\" \"Mozilla/5.0\"\n" +
                        "168.41.191.40 - - [09/Jul/2018:10:11:30 +0200] \"GET /faq HTTP/1.1\" 200 1234 \"-\" \"Mozilla/5.0\"\n"
        );

        StepVerifier.create(resource.parseLogFile(logFile))
                .thenConsumeWhile(records -> {
                    assertEquals(2, records.size(), "Expected 2 valid lines");
                    assertEquals( "177.71.128.21", records.get(0).getIpAddress(),"IP address did not match");
                    assertEquals( "168.41.191.40", records.get(1).getIpAddress(),"IP address did not match");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void parseLogFile_onEmptyFile_expectReturnsEmptyList() throws Exception {
        Path logFile = tempDir.resolve("empty.log");
        Files.writeString(logFile, "");

        StepVerifier.create(resource.parseLogFile(logFile))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    void parseLogFile_onInvalidFile_expectThrowsBadRequest() throws Exception {
        Path logFile = tempDir.resolve("does-not-exist.log");
//        Files.writeString(logFile, "");

        StepVerifier.create(resource.parseLogFile(logFile))
                .consumeErrorWith(throwable -> {
                    assertInstanceOf(LogsError.class, throwable);
                    LogsError logsError = (LogsError) throwable.getCause();
                    assertEquals("File not found: does-not-exist.log", logsError.getMessage());
                    assertEquals("BAD_REQUEST", logsError.getErrorCode());
                    assertEquals(HttpStatus.BAD_REQUEST, logsError.getHttpStatus());

                });
    }

    @Test
    void parseLogFile_onInvalidLine_expectSkipsAndContinues() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile,
                "177.71.128.21 - - [10/Jul/2018:22:21:28 +0200] \"GET /index HTTP/1.1\" 200 3574 \"-\" \"Mozilla/5.0\" trailing junk\n" +
                        "random junk\n"
        );

        StepVerifier.create(resource.parseLogFile(logFile))
                .thenConsumeWhile(records -> {
                    assertEquals(1, records.size(), "Expected 1 valid line (ignoring trailing junk)");
                    assertEquals( "177.71.128.21", records.get(0).getIpAddress(),"IP address did not match");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void parseLogFile_onInvalidInteger_expectSetsMinusOne() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile,
                "177.71.128.21 - - [10/Jul/2018:22:21:28 +0200] \"GET /index HTTP/1.1\" - - \"-\" \"Mozilla/5.0\"\n"
        );

        StepVerifier.create(resource.parseLogFile(logFile))
                .thenConsumeWhile(records -> {
                    assertEquals(1, records.size(), "Expected 1 valid line");
                    assertEquals( -1, records.get(0).getResponseSizeBytes(),"Response bytes did not match");
                    assertEquals( -1, records.get(0).getHttpStatus(),"HTTP Status did not match");
                    return true;
                })
                .verifyComplete();
    }


}