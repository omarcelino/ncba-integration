# Running and Testing the Application

## Prerequisites

- Java 21+
- Docker & Docker Compose (for MySQL)
- Gradle 7.0+
- curl or Postman (for API testing)

## Build the Application

```bash
# Clean build
./gradlew clean build -x test

# Build with tests
./gradlew build

# Run tests only
./gradlew test
```

## Run Locally

### Option 1: With Docker Compose (Recommended)

Create `docker-compose.yml`:
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: countrydb
    ports:
      - "3306:3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:mysql://mysql:3306/countrydb
      DB_USER: root
      DB_PASSWORD: root
      SOAP_URL: http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso
    depends_on:
      mysql:
        condition: service_healthy
```

Run:
```bash
docker-compose up
```

Access: http://localhost:8080/api/countries

### Option 2: Manual Setup

1. **Start MySQL**:
```bash
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=countrydb \
  -p 3306:3306 \
  mysql:8
```

2. **Run Application**:
```bash
./gradlew bootRun
```

3. **Access**:
```
http://localhost:8080/api/countries
```

## API Testing

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

Expected Response:
```json
{
  "status": "UP"
}
```

### Create Country

```bash
curl -X POST http://localhost:8080/api/countries \
  -H "Content-Type: application/json" \
  -d '{"countryName": "kenya"}'
```

Expected Response:
```json
{
  "id": 1,
  "name": "Kenya",
  "isoCode": "KE",
  "capital": "Nairobi",
  "area": "582646",
  "population": "54027487",
  "continent": "Africa",
  "currencyCode": "KES",
  "currencyName": "Kenyan shilling",
  "phonePrefix": "+254",
  "languages": ["English", "Swahili"],
  "createdAt": "2026-10-09T12:00:00",
  "updatedAt": "2026-10-09T12:00:00"
}
```

### Get All Countries

```bash
curl http://localhost:8080/api/countries
```

### Get Country by ID

```bash
curl http://localhost:8080/api/countries/1
```

### Get Country by ISO Code

```bash
curl http://localhost:8080/api/countries/iso/KE
```

### Update Country

```bash
curl -X PUT http://localhost:8080/api/countries/1 \
  -H "Content-Type: application/json" \
  -d '{"capital": "New Nairobi"}'
```

### Delete Country

```bash
curl -X DELETE http://localhost:8080/api/countries/1
```

## Postman Collection

Import to Postman or create requests with:

**Base URL**: `http://localhost:8080`

| Method | Endpoint | Body |
|--------|----------|------|
| POST | `/api/countries` | `{"countryName": "tanzania"}` |
| GET | `/api/countries` | - |
| GET | `/api/countries/{id}` | - |
| GET | `/api/countries/iso/{code}` | - |
| PUT | `/api/countries/{id}` | `{"capital": "..."}` |
| DELETE | `/api/countries/{id}` | - |

## Monitoring Endpoints

Access at `http://localhost:8080`:

- **Health**: `/actuator/health`
- **Info**: `/actuator/info`
- **Metrics**: `/actuator/prometheus`
- **All Endpoints**: `/actuator`

## Logging

Application logs are structured in JSON format for better debugging:

Example:
```json
{
  "timestamp": "2026-10-09T12:00:00",
  "level": "INFO",
  "thread": "main",
  "logger": "com.ncba.integration.service.CountryService",
  "message": "Creating country: normalized='Kenya'"
}
```

## Running Tests

### Unit Tests Only
```bash
./gradlew test --tests "*NameNormalizer*"
```

### Integration Tests
```bash
./gradlew test --tests "*Integration*"
```

### All Tests with Coverage
```bash
./gradlew test jacocoTestReport
```

Coverage report: `build/reports/jacoco/test/html/index.html`

## Performance Testing

### Load Test with Apache JMeter

1. Create test plan targeting `/api/countries`
2. Configure thread group (e.g., 100 users, 5 minute rampup)
3. Run and analyze results

### Simple Load Test with curl

```bash
#!/bin/bash
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/countries \
    -H "Content-Type: application/json" \
    -d "{\"countryName\": \"country$i\"}" &
done
wait
```

## Troubleshooting

### Database Connection Issues

Check if MySQL is running:
```bash
docker ps | grep mysql
```

Verify credentials in `application.yml`

### SOAP Service Timeout

Test SOAP endpoint:
```bash
curl -X POST http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso \
  -H "Content-Type: text/xml" \
  -d '<?xml version="1.0"?><soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><CountryISOCode xmlns="http://www.oorsprong.org/websamples.countryinfo"><sCountryName>Kenya</sCountryName></CountryISOCode></soap:Body></soap:Envelope>'
```

### High Memory Usage

Adjust JVM settings:
```bash
./gradlew bootRun --args='--spring.jvm.args=-Xmx512m'
```

## Cleanup

```bash
# Stop application
Ctrl+C

# Remove Docker container
docker stop mysql
docker rm mysql

# Remove Docker Compose services
docker-compose down

# Clean build artifacts
./gradlew clean
```

## Continuous Integration

The application includes:
- Unit tests for business logic
- Integration tests with test database (H2)
- Structured logging for debugging
- Health check endpoints for deployment verification

Run full CI locally:
```bash
./gradlew clean test build
```
