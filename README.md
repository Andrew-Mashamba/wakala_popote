# Quick Cash Backend

Spring Boot backend for the Quick Cash (visa_agent) app. Aligns with [PROJECT.md](../PROJECT.md) and [PROJECT_BOLT.md](../PROJECT_BOLT.md).

## Requirements

- **Java 17**
- **Gradle 8.x** (or use the wrapper: `./gradlew`)

## Run locally

```bash
cd backend
./gradlew bootRun
```

Or with Gradle installed globally:

```bash
gradle bootRun
```

If the wrapper is missing, generate it first:

```bash
gradle wrapper
```

- API base: **http://localhost:8080**
- H2 console: **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:mem:quickcash`, user: `sa`, password: empty)

## API endpoints (current)

Compatible with the existing Flutter app:

| Method | Path | Description |
|--------|------|-------------|
| POST | `/users/createUser` | Register/update user (JSON body). Returns internal user ID (UUID string). |
| POST | `/users/updateUserLocation` | Update user location and address |
| POST | `/users/updateToken` | Update FCM token |
| POST | `/cash/requestCash` | Create cash delivery request |
| GET | `/health` | Health check |
| GET | `/` | Service info |

## Configuration

- **Default:** H2 in-memory DB, port 8080. See `src/main/resources/application.yml`.
- **Production:** Use profile `prod` and set DB env vars. See `application-prod.yml`.

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=quickcash
export DB_USERNAME=quickcash
export DB_PASSWORD=secret
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## Project structure

```
src/main/java/com/quickcash/
├── QuickCashApplication.java
├── config/          # CORS, security, etc.
├── controller/      # REST controllers
├── domain/          # JPA entities (User, CashRequest)
├── dto/             # Request/response DTOs
├── exception/       # Global exception handler
├── repository/      # JPA repositories
└── service/         # Business logic
```

## Build

```bash
./gradlew build
```

Tests:

```bash
./gradlew test
```

## Next steps (from PROJECT.md)

- [ ] Firebase token verification for auth
- [ ] Selcom integration for payments
- [ ] FCM to notify agents on new cash request
- [ ] Bolt Partner API integration (PROJECT_BOLT.md)
- [ ] `/api/v1/*` versions of endpoints with proper auth
