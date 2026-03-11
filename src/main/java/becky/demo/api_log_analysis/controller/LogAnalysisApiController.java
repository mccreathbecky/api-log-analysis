package becky.demo.api_log_analysis.controller;

import becky.demo.api_log_analysis.service.LogAnalysisService;
import becky.demo.contract_api_log_analysis.interfaces.LogAnalysisApi;
import becky.demo.contract_api_log_analysis.models.LogReportResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class LogAnalysisApiController implements LogAnalysisApi {

    @Autowired
    private LogAnalysisService logAnalysisService;

    @Override
    public Mono<ResponseEntity<LogReportResponse>> getLogReport(String fileUrl, String logPatternLayout, @Nullable String xTrackingId, ServerWebExchange exchange) {

        // TODO: Implement Logic

        // Step 1: Validate fileUrl and logPatternLayout

        // Step 2: ? Convert logPatternLayout to a Pattern?

        // Step 3: Call Service

        // Step 4: Format into 200 OK response

        // Also, handle errors
        return logAnalysisService.getLogReport(fileUrl, logPatternLayout)
                .map(ResponseEntity::ok);
    }
}
