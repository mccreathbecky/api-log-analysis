package becky.demo.api_log_analysis.service;

import becky.demo.api_log_analysis.model.LogRecordDto;
import becky.demo.api_log_analysis.model.LogsError;
import becky.demo.api_log_analysis.resource.LogFileResource;
import becky.demo.contract_api_log_analysis.models.LogReportResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
        // Logic Steps:
        // Step 1: (Additional) Validate fileUrl again - theoretically not necessary if done in controller but public method could be called elsewhere in future
        if (fileUrl.isBlank()) {
            throw LogsError.builder()
                    .message("Error: fileUrl is an expected parameter")
                    .errorCode("BAD_REQUEST")
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .build();
        }

        // Call Resource layer to read + parse log data
        return logFileResource.parseLogFile(fileUrl)
                // Either separate methods or together for efficiency, iterate through log data to generate report data
                .map(this::calculateReportValues)
                // TODO: remove once implemented
                .then(Mono.just(LogReportResponse.builder()
                        .nbUniqueIpAddresses(3)
                        .topThreeUrls(List.of("https://abc.com"))
                        .topThreeIPAddresses(List.of("192.156.1.0"))
                        .build()))
                // Handle errors gracefully
                .doOnError(this::handleError);
    }

    private LogReportResponse calculateReportValues(List<LogRecordDto> logRecords) {

        int nbUniqueIpAddresses = 0;
        List<String> topThreeUrls = List.of();
        List<String> topThreeIpAddresses = List.of();

        


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
