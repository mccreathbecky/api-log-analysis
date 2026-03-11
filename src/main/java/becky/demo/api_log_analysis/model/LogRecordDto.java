package becky.demo.api_log_analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogRecordDto {
    private String ipAddress;
    private String logName;
    private String user;
    private OffsetDateTime timeStamp;
    private String url;
    private int httpStatus;
    private int responseSizeBytes;
    private String referrerHeader;
    private String userAgent;
}
