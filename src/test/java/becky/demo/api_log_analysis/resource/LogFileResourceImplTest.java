package becky.demo.api_log_analysis.resource;

import becky.demo.api_log_analysis.BaseTestClass;
import becky.demo.api_log_analysis.model.LogsError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class LogFileResourceImplTest extends BaseTestClass {

    @TempDir
    private Path tempDir;

    @Autowired
    private LogFileResource resource;

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
    void parseLogFile_onLargeFile_expectHandlesEfficiently() throws Exception {
        Path logFile = tempDir.resolve("large.log");
        StringBuilder content = new StringBuilder();
        int numLines = 10000;
        for (int i = 0; i < numLines; i++) {
            content.append("177.71.128.21 - - [10/Jul/2018:22:21:28 +0200] \"GET /index HTTP/1.1\" 200 3574 \"-\" \"Mozilla/5.0\"\n");
        }
        Files.writeString(logFile, content.toString());

        StepVerifier.create(resource.parseLogFile(logFile))
                .thenConsumeWhile(records -> {
                    assertEquals(numLines, records.size(), "Expected all lines to be parsed");
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
                    LogsError logsError = (LogsError) throwable;
                    assertTrue(logsError.getMessage().startsWith("File not found:"), "Expected message to start File not found");
                    assertEquals("BAD_REQUEST", logsError.getErrorCode(), "Expected BAD_REQUEST error code");
                    assertEquals(HttpStatus.BAD_REQUEST, logsError.getHttpStatus(), "Expected BAD_REQUEST status");

                })
                .verify();
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

    @Test
    void parseLogFile_onMalformedTimestamp_expectSkipsLine() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile,
                "177.71.128.21 - - [INVALID_DATE] \"GET /index HTTP/1.1\" 200 3574 \"-\" \"Mozilla/5.0\"\n" +
                        "168.41.191.40 - - [09/Jul/2018:10:11:30 +0200] \"GET /faq HTTP/1.1\" 200 1234 \"-\" \"Mozilla/5.0\"\n"
        );

        StepVerifier.create(resource.parseLogFile(logFile))
                .thenConsumeWhile(records -> {
                    assertEquals(1, records.size(), "Expected invalid timestamp line to be skipped");
                    assertEquals("168.41.191.40", records.get(0).getIpAddress(), "Expected IP address from valid line");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void parseLogFile_onLineWithDifferentStatusCodes_expectAllParsedCorrectly() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile,
                "192.168.1.1 - - [10/Jul/2018:22:21:28 +0200] \"GET /home HTTP/1.1\" 200 1024 \"-\" \"Mozilla/5.0\"\n" +
                        "10.0.0.1 - - [10/Jul/2018:22:21:29 +0200] \"GET /api HTTP/1.1\" 404 256 \"-\" \"curl\"\n" +
                        "172.16.0.1 - - [10/Jul/2018:22:21:30 +0200] \"POST /submit HTTP/1.1\" 500 512 \"-\" \"curl\"\n"
        );

        StepVerifier.create(resource.parseLogFile(logFile))
                .thenConsumeWhile(records -> {
                    assertEquals(3, records.size(), "Expected 3 valid lines");
                    assertEquals(200, records.get(0).getHttpStatus(), "Expected first line to have 200 status");
                    assertEquals(404, records.get(1).getHttpStatus(), "Expected second line to have 404 status");
                    assertEquals(500, records.get(2).getHttpStatus(), "Expected third line to have 500 status");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void parseLogFile_onLineWithSpecialCharactersInUrl_expectParseCorrectly() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile,
                "192.168.1.1 - - [10/Jul/2018:22:21:28 +0200] \"GET /path?query=value&other=test HTTP/1.1\" 200 1024 \"-\" \"Mozilla/5.0\"\n" +
                        "192.168.1.1 - - [10/Jul/2018:22:21:29 +0200] \"GET /path#anchor HTTP/1.1\" 200 512 \"-\" \"Mozilla/5.0\"\n"
        );

        StepVerifier.create(resource.parseLogFile(logFile))
                .thenConsumeWhile(records -> {
                    assertEquals(2, records.size(), "Expected 2 records to be returned");
                    assertTrue(records.get(0).getUrl().contains("query=value"), "Expected query param in url");
                    assertTrue(records.get(1).getUrl().contains("anchor"), "Expected anchor param in url");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void parseLogFile_onLineWithMissingStatusCode_expectHandlesGracefully() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile,
                "192.168.1.1 - - [10/Jul/2018:22:21:28 +0200] \"GET /home HTTP/1.1\" - 1024 \"-\" \"Mozilla/5.0\"\n"
        );

        StepVerifier.create(resource.parseLogFile(logFile))
                .thenConsumeWhile(records -> {
                    // Should either skip line or handle gracefully
                    assertEquals(-1, records.get(0).getHttpStatus(), "Expected default status code value");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void parseLogFile_onLineWithDifferentHttpMethods_expectAllCaptured() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile,
                "192.168.1.1 - - [10/Jul/2018:22:21:28 +0200] \"GET /home HTTP/1.1\" 200 1024 \"-\" \"Mozilla/5.0\"\n" +
                        "192.168.1.1 - - [10/Jul/2018:22:21:29 +0200] \"POST /api HTTP/1.1\" 201 512 \"-\" \"curl\"\n" +
                        "192.168.1.1 - - [10/Jul/2018:22:21:30 +0200] \"DELETE /resource HTTP/1.1\" 204 0 \"-\" \"curl\"\n" +
                        "192.168.1.1 - - [10/Jul/2018:22:21:31 +0200] \"PUT /update HTTP/1.1\" 200 256 \"-\" \"curl\"\n"
        );

        StepVerifier.create(resource.parseLogFile(logFile))
                .thenConsumeWhile(records -> {
                    assertEquals(4, records.size(), "Expected 4 records");
                    return true;
                })
                .verifyComplete();
    }
}