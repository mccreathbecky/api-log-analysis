package becky.demo.api_log_analysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import lombok.RequiredArgsConstructor;

@SpringBootTest(classes = ApiLogAnalysisApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@ActiveProfiles("test")
public class BaseTestClass {
    
}
