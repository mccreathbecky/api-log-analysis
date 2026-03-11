package becky.demo.api_log_analysis.service;

import becky.demo.api_log_analysis.model.LogRecordDto;
import becky.demo.api_log_analysis.model.LogsError;
import becky.demo.api_log_analysis.resource.LogFileResource;
import becky.demo.contract_api_log_analysis.models.LogReportResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class LogAnalysisServiceImpl implements LogAnalysisService {

    @Autowired
    private LogFileResource logFileResource;

    /**
     * Parse the provided file and analyse commonly appearing values into a report
     * @param fileUrl The absolute file path to the log file to parse
     * @return A LogReportResponse containing the results of the analysis e.g. number of unique IP addresses, top 3 URLs, top 3 IP addresses
     */
    @Override
    public Mono<LogReportResponse> getLogReport(String fileUrl) {
        // Validate fileUrl again - theoretically not necessary if done in controller but public method could be called elsewhere in future
        if (fileUrl.isBlank()) {
            throw LogsError.builder()
                    .message("Error: fileUrl is an expected parameter")
                    .errorCode("BAD_REQUEST")
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .build();
        }

        // Call Resource layer to read + parse log data
        return logFileResource.parseLogFile(fileUrl)
                // Iterate through log data to generate report data, formatting into expected response
                .map(this::calculateReportValues)
                // Handle errors gracefully
                .doOnError(this::handleError);
    }

    /**
     * Helper method to calculate report values from the list of log records. 
     * This method iterates through the log records to count unique IP addresses and URLs, then determines the top 3 most frequent IP addresses and URLs.
     * @param logRecords - A list of LogRecordDto objects representing the parsed log data
     * @return A LogReportResponse object containing the calculated report values such as number of unique IP addresses, top 3 URLs, and top 3 IP addresses
     */
    /*
        Developer Notes:
        Time Complexity
            Overall: O(n log n) where n = number of log records, but typically better as m (unique IPs/URLs) is often much smaller than n
            Parsing log file: O(n)
            Building HashMaps: O(n)
            Sorting for top 3: O(m log m) where m = unique IPs/URLs (typically m << n)
            Bottleneck: The two .sorted() operations on HashMap entries
        Space Complexity
            O(m) where m = number of unique IPs + unique URLs
            Two HashMaps store unique values
            Acceptable for most datasets
    
    
    */
    private LogReportResponse calculateReportValues(List<LogRecordDto> logRecords) {
        HashMap<String, Integer> ipAddresses = new HashMap<>();
        HashMap<String, Integer> urls = new HashMap<>();

        // Iterate through all the records
            // Maintain following data structures:
            // ipAddresses: Map < String ipAddress , int count >
            // urls: Map < String url , int count >
        for (LogRecordDto logRecord : logRecords) {
            String ipAddress = logRecord.getIpAddress();
            String url = logRecord.getUrl();    
            
            ipAddresses.put(ipAddress, ipAddresses.getOrDefault(ipAddress, 0) + 1);
            urls.put(url, urls.getOrDefault(url, 0) + 1);
        }

        // then calculate nbUniqueIpAddresses as size(ipAddresses)
        int nbUniqueIpAddresses = ipAddresses.size();

        // then sort by count descending for ipAddresses, urls and take top 3
        List<String> topThreeUrls = new ArrayList<>();
        List<String> topThreeIpAddresses = new ArrayList<>();

        ipAddresses.entrySet().stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                .limit(3)
                .forEach(entry -> topThreeIpAddresses.add(entry.getKey()));

        urls.entrySet().stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                .limit(3)
                .forEach(entry -> topThreeUrls.add(entry.getKey()));

        // Format into LogReportResponse
        return LogReportResponse.builder()
                .nbUniqueIpAddresses(nbUniqueIpAddresses)
                .topThreeUrls(topThreeUrls)
                .topThreeIPAddresses(topThreeIpAddresses)
                .build();
    }


    /**
     * Helper method to handle errors and create a consistent error response structure.
     * @param throwable - The exception that occurred during processing
     * @return A LogsError object containing error details to be returned in the response
     */
    private Throwable handleError(Throwable throwable) {
        if (throwable instanceof LogsError) {
            throw (LogsError) throwable;
        } else {
            throw LogsError.builder()
                    .message("Failed to process log data: " + throwable.getMessage())
                    .errorCode("Internal Server Error")
                    .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .throwable(throwable)
                    .build();

        }
    }
}
