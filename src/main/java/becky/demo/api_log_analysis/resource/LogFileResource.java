package becky.demo.api_log_analysis.resource;

import becky.demo.api_log_analysis.model.LogRecordDto;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LogFileResource {
    Mono<List<LogRecordDto>> parseLogFile(String fileUrl);
}
