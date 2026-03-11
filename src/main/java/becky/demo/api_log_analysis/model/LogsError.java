package becky.demo.api_log_analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogsError extends RuntimeException {
    private String message;
    private String errorCode;
    private Throwable throwable;
    private HttpStatus httpStatus;
}
