package becky.demo.api_log_analysis.service;

import becky.demo.api_log_analysis.resource.LogFileResource;
import becky.demo.contract_api_log_analysis.models.LogReportResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class LogAnalysisServiceImpl implements LogAnalysisService {

    @Autowired
    private LogFileResource logFileResource;

    /**
     * @param fileUrl
     * @param logPatternLayout
     * @return
     */
    @Override
    public Mono<LogReportResponse> getLogReport(String fileUrl, String logPatternLayout) {
        // Logic Steps:
        // Step 1: (Additional) Validate fileUrl/logPatternLayout again - theoretically not necessary if done in controller but public method could be called elsewhere in future

        // Step 2: Call Resource layer to read log data

        // Step 3: Parse log data (ideally with streaming) into a standard format allowing easy extract of fields

        // Step 4: either separate methods or together for efficiency, iterate through log data to generate report data

        // Step 5: format into LogReportResponse

        // Also, handle errors

        return logFileResource.parseLogFile(fileUrl)
                .then(Mono.just(LogReportResponse.builder()
                        .nbUniqueIpAddresses(3)
                        .topThreeUrls(List.of("https://abc.com"))
                        .topThreeIPAddresses(List.of("192.156.1.0"))
                        .build()));
    }
}
