package becky.demo.api_log_analysis.service;

import becky.demo.contract_api_log_analysis.models.LogReportResponse;
import reactor.core.publisher.Mono;

public interface LogAnalysisService {

    Mono<LogReportResponse> getLogReport(String fileUrl, String logPatternLayout);
}
