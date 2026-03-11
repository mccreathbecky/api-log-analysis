package becky.demo.api_log_analysis.resource;

import becky.demo.api_log_analysis.model.LogRecordDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LogFileResourceMock implements LogFileResource {

    /**
     * @param fileUrl
     * @return
     */
    @Override
    public Mono<List<LogRecordDto>> parseLogFile(String fileUrl) {
        // TODO: replace with actual data retrieval logic

        return Mono.just(readStaticFile());
    }


    private List<LogRecordDto> readStaticFile() {
        try {
            // Use Spring's ClassPathResource to load from classpath
            ClassPathResource resource = new ClassPathResource("samples/sample-data-1.log");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            List<LogRecordDto> records = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    LogRecordDto record = parseLogLine(line);
                    if (record != null) {
                        records.add(record);
                    }
                }
            }
            reader.close();

            return records;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read static file", e);
        }
    }

    private LogRecordDto parseLogLine(String line) {
        try {
            return LogRecordDto.builder()
                    .ipAddress(line)
                    .logName("dummyLogName")
                    .user("dummyUser")
                    .timeStamp(OffsetDateTime.now())
                    .url("dummyPath")
                    .httpStatus(Integer.parseInt("200"))
                    .responseSizeBytes(Integer.parseInt("1234"))
                    .referrerHeader("dummyReferrer")
                    .userAgent("dummyUserAgent")
                    .build();
        } catch (Exception e) {
            System.err.println("Failed to parse log line: " + line + ", Error: " + e.getMessage());
            return null;
        }
    }
}
