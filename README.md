# api-log-analysis

This API is part of a demo that parses a log file at a provided location and returns a report on most common elements.
It is intended as a sample design, and areas of future enhancement/assumptions are documented.

## Assumptions
1. Log file format will be a fixed file format per the sample provided
2. Users will query via a HTTP request
3. Each request will allow consideration of a single file. In the future if files would be reused, caching could be implemented.
4. Log files don't contain sensitive data and no masking is required
5. When multiple records 'tie' for the same count (e.g. each appear only once), the default sort order is used
6. The list of top values returned has no guaranteed order in the array response and is best-effort
7. Any additional custom characters expected in individual log components will be provided (e.g. where only decimals or alphabetical characters are considered)

## Methodology
Key elements of the approach taken:
- First, use contract driven principles by developing a basic contract in contract-api-log-analysis
- Use reactive spring boot framework to accelerate HTTP REST non-blocking development
- Use a controller/service/resource setup with the business logic in the service layer
- Interface/Implementation patterns to enable multiple implementations to be developed in future enhancements / toggle between mock implementations
- DTO models of downstream data to avoid exposing downstream data structure to upstream
- Unit tests by layer to verify positive and negative cases for each. Try to cover all branches where practical and achieve at least 80% coverage
- Error handlers at each layer to try and prevent throwing system errors up to the user where not necessary


## AI Usage
AI tools were used to assist with the following:
- Assisted in developing individual functions e.g. base regex for file parsing, which I then fixed up with regex101
- Debugging individual functions e.g. OffsetDateTime conversion in log parsing
- Providing commentary on time/space complexity and highlighting more efficient algorithms e.g. min heap
- Generating additional unit tests following a provided template

## Dependencies
- Java 21
- Maven
- Contract API Log Analysis: https://github.com/mccreathbecky/contract-api-log-analysis

## Run Locally
Ensure you have locally installed `contract-api-log-analysis`.

Clone the project
```bash
  git clone https://github.com/mccreathbecky/api-log-analysis.git
```
Go to the project directory
```bash
  cd api-log-analysis
```
Compile the code
```bash
  mvn clean compile package -U
```
Start the server, specifying the environment
```bash
mvn spring-boot:run -Dactive.spring.profiles=<your-env-here e.g. local> -U
```

Running Tests
```bash
  mvn clean verify test
```

Test API Endpoints
```bash
curl --location 'http://localhost:8080/api-log-analysis/v1/reports?fileUrl=abc' \
--header 'x-tracking-id: becky123' \
--header 'x-api-key: DUMMY_VALUE'
```

## Pending Enhancements
Given more time, the following would be implemented:
- API Key security validation (including encrypted secure key properties)
- A more comprehensive resource implementation rather than a mock for a static file
- Proper logging using tracking ID with appropriate error/warn/info/debug levels
- More detailed unit tests, including more exhaustive null checking
- Circuit Breakers and Timeouts
- Full JavaDocs on every function
- More consistency on error messages e.g. error code formats
