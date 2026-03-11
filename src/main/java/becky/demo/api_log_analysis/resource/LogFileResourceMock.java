package becky.demo.api_log_analysis.resource;

import becky.demo.api_log_analysis.model.LogRecordDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class LogFileResourceMock implements LogFileResource {

    private Pattern logFilePattern;

    public LogFileResourceMock() {

        // this is used in the log parser - it's more efficient to initialise it at construction
        // Apache Common Log Format: IP logname user [timestamp] "METHOD path HTTP/version" status size "referrer" "user-agent"
        this.logFilePattern = Pattern.compile(
                "^(?<ipAddress>[\\d.]+) " +
                    "(?<logname>[\\w.-]*) " +
                    "(?<user>[\\w.-]*) " +
                    "\\[(?<timestamp>[^]]*?)] " +
                    "\"(?<httpMethod>\\w+) (?<urlPath>[^\\s]+)\\s+[^\"]*\" " +
                    "(?<httpStatus>\\d{3}) " +
                    "(?<responseSizeBytes>\\d+) " +
                    "\"(?<referrer>[^\"]*)\" " +
                    "\"(?<userAgent>[^\"]*)\""  // if there are extra characters after this, we don't care
        );
    }

    /**
     * Parse the provided file and return a list of LogRecordDto objects representing the parsed log data.
     * @param fileUrl The absolute file path to the log file to parse
     * @return A Mono containing a list of LogRecordDto objects representing the parsed log data
     */
    @Override
    public Mono<List<LogRecordDto>> parseLogFile(String fileUrl) {
        // TODO: replace with actual data retrieval logic

        return Mono.just(readStaticFile());
    }


    /**
     * Helper method to read a static file from the classpath and parse it into a list of LogRecordDto objects using the log pattern regex.
     * @return A list of LogRecordDto objects representing the parsed log data from the static file
     */
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

    /**
     * Parse a string using a given regex pattern matcher for a specific log format
     * @param line The single line of input
     * @return A parsed LogRecordDto object containing all the fields, including the date extracted into DateTime format
     */
    private LogRecordDto parseLogLine(String line) {
        try {
            Matcher matcher = logFilePattern.matcher(line);
            if (!matcher.find()) {
                System.err.println("Failed to parse log line as valid log: " + line);
                return null;
            }
            // Extract all the log fields
            // Note: At the current state of implementation, all these fields aren't actually needed
            String ipAddress = matcher.group("ipAddress");
            String logName = matcher.group("logname");
            String user = matcher.group("user");
            String timestamp = matcher.group("timestamp");
            String method = matcher.group("httpMethod");
            String path = matcher.group("urlPath");
            String status = matcher.group("httpStatus");
            String size = matcher.group("responseSizeBytes");
            String referrer = matcher.group("referrer");
            String userAgent = matcher.group("userAgent");

            // Parse timestamp: "10/Jul/2018:22:21:28 +0200"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
            OffsetDateTime dateTime = OffsetDateTime.parse(timestamp, formatter);

            return LogRecordDto.builder()
                    .ipAddress(ipAddress)
                    .logName(logName)
                    .user(user)
                    .timeStamp(dateTime)
                    .url(path)
                    .httpStatus(Integer.parseInt(status))
                    .responseSizeBytes(Integer.parseInt(size))
                    .referrerHeader(referrer)
                    .userAgent(userAgent)
                    .build();
        } catch (Exception e) {
            System.err.println("Failed to parse log line: " + line + ", Error: " + e.getMessage());
            return null;
        }
    }
}
