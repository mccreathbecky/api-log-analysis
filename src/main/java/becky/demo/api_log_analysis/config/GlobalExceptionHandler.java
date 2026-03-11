package becky.demo.api_log_analysis.config;

import becky.demo.api_log_analysis.model.LogsError;
import becky.demo.contract_api_log_analysis.models.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    /**
     * Catch-all handler for any exceptions
     * Attempts to map them to ErrorResponse format
     */
    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
            Throwable ex,
            ServerWebExchange request) {
        
        String trackingId = getOrGenerateTrackingId(request);
        
        // Log the full exception for debugging
        logger.error("Exception occurred: type={}, message={}, trackingId={}", 
                ex.getClass().getSimpleName(), ex.getMessage(), trackingId, ex);
        
        // Map known exception types to specific error codes
        HttpStatus status;
        String errorCode;
        String errorMessage;
        
        if (ex instanceof LogsError logsError) {
            // This case should be handled by the specific handler, but we include it here for completeness
            status = logsError.getHttpStatus();
            errorCode = logsError.getErrorCode();
            errorMessage = logsError.getMessage();
        } else if (ex instanceof MissingRequestValueException) {
            status = HttpStatus.BAD_REQUEST;
            errorCode = "BAD_REQUEST";
            errorMessage = ex.getMessage() != null ? ex.getMessage() 
                    : "The request is missing a required parameter.";
        } else if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            errorCode = "BAD_REQUEST";
            errorMessage = ex.getMessage() != null ? ex.getMessage() 
                    : "Invalid request parameters.";
        } else if (ex instanceof NullPointerException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = "INTERNAL_SERVER_ERROR";
            errorMessage = "An unexpected error occurred. Please contact support if the issue persists.";
        } else {
            // Default for truly unexpected exceptions
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = "INTERNAL_SERVER_ERROR";
            errorMessage = "An unexpected error occurred. Please contact support if the issue persists.";
        }
        
        ErrorResponse error = new ErrorResponse()
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .trackingId(trackingId);
        
        return Mono.just(ResponseEntity.status(status).body(error));
    }

    /**
     * Extract tracking ID from request header or generate a new one
     */
    private String getOrGenerateTrackingId(ServerWebExchange request) {
        String trackingId = request.getRequest().getHeaders().getFirst("x-tracking-id");
        return (trackingId != null && !trackingId.isBlank()) 
                ? trackingId 
                : UUID.randomUUID().toString();
    }
}