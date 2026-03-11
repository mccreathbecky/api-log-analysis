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

    private final Pattern logFilePattern;
    private final DateTimeFormatter dateTimeFormatter;

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

        // Parse timestamp: "10/Jul/2018:22:21:28 +0200"
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
    }

    /**
     * Parse the provided file and return a list of LogRecordDto objects representing the parsed log data.
     * @param fileUrl The absolute file path to the log file to parse
     * @return A Mono containing a list of LogRecordDto objects representing the parsed log data
     */
    /*
        Developer Notes
        Time Complexity
            O(n*L) - n = lines in file, L = line length
        Space Complexity
            O(n)
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
    /*
        Developer Notes
        Time Complexity
            O(n)
        Space Complexity
            O(n)
     */
    private List<LogRecordDto> readStaticFile() {
        BufferedReader reader = null;
        try {
            // Use Spring's ClassPathResource to load from classpath
            ClassPathResource resource = new ClassPathResource("samples/sample-data-1.log");
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
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
            // Try to close the reader in an error scenario
            try {
                if (reader != null && reader.ready()) {
                    reader.close();
                }
            } catch (Exception throwaway) {
                System.err.println("Error during exception handler attempting to close reader");
            }
            throw new RuntimeException("Failed to read static file", e);
        }
    }

    /**
     * Parse a string using a given regex pattern matcher for a specific log format
     * @param line The single line of input
     * @return A parsed LogRecordDto object containing all the fields, including the date extracted into DateTime format
     */
    /*
        Developer Notes
        Time Complexity
            O(L), L = line length, regex match is linear in input size
        Space Complexity
            O(n)
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
            OffsetDateTime dateTime = OffsetDateTime.parse(timestamp, dateTimeFormatter);
            int httpStatusCode = (status.isBlank() || "-".equals(status)) ? -1: Integer.parseInt(status);
            int responseSizeBytes = (size.isBlank() || "-".equals(size)) ? -1: Integer.parseInt(size);

            return LogRecordDto.builder()
                    .ipAddress(ipAddress)
                    .logName(logName)
                    .user(user)
                    .timeStamp(dateTime)
                    .url(path)
                    .httpStatus(httpStatusCode)
                    .responseSizeBytes(responseSizeBytes)
                    .referrerHeader(referrer)
                    .userAgent(userAgent)
                    .build();
        } catch (Exception e) {
            System.err.println("Failed to parse log line: " + line + ", Error: " + e.getMessage());
            return null;
        }
    }
}
