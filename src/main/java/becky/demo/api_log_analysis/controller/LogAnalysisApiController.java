package becky.demo.api_log_analysis.controller;

import becky.demo.api_log_analysis.model.LogsError;
import becky.demo.api_log_analysis.service.LogAnalysisService;
import becky.demo.contract_api_log_analysis.interfaces.LogAnalysisApi;
import becky.demo.contract_api_log_analysis.models.ErrorResponse;
import becky.demo.contract_api_log_analysis.models.LogReportResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class LogAnalysisApiController implements LogAnalysisApi {

    @Autowired
    private LogAnalysisService logAnalysisService;

    @Override
    public Mono<ResponseEntity<LogReportResponse>> getLogReport(String fileUrl, @Nullable String xTrackingId, ServerWebExchange exchange) {

        // Validate fileUrl
        if (fileUrl.isBlank()) {
            throw LogsError.builder()
                    .message("Error: fileUrl is an expected parameter")
                    .errorCode("BAD_REQUEST")
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .build();
        }
        // Extract path object from fileUrl and verify it's valid
        Path filePath = Path.of(fileUrl);
        if (!Files.exists(filePath)) {
            throw LogsError.builder()
                    .message("File not found: " + fileUrl)
                    .errorCode("BAD_REQUEST")
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .build();
        }

        // Call Service
        return logAnalysisService.getLogReport(filePath)
                // Format into 200 OK response
                .map(ResponseEntity::ok)
                // Handle any errors to return in expected format
                .doOnError(this::handleError);

    }

    private Mono<ResponseEntity<ErrorResponse>> handleError(Throwable throwable) {
        if (throwable instanceof LogsError logsError) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .errorCode(logsError.getErrorCode())
                    .errorMessage(logsError.getMessage())
                    .build();
            return Mono.just(ResponseEntity.status(logsError.getHttpStatus()).body(errorResponse));
        } else {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .errorCode("Internal Server Error")
                    .errorMessage("An unexpected error occurred: " + throwable.getMessage())
                    .build();
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
        }
    }
}
