# PAP API (Java)

Spring Boot implementation of the PAP (Policy Administration Point) back end. It exposes the same routes,
payloads, status codes and error formats as [`pap-api-net`](../pap-api-net), so the React UI
([`pap-ui-react`](../pap-ui-react)) works with either back end unchanged.

## Requirements

- JDK 21+
- Maven is optional: the bundled wrapper (`mvnw` / `mvnw.cmd`) downloads it on first use

## Running

```bash
./mvnw spring-boot:run            # Windows: mvnw.cmd spring-boot:run
```

The API listens on **http://localhost:5200**, the UI's default `VITE_API_URL`. On first start it seeds the
system roles, a default domain and an `admin` / `admin123` account. Change that password before any shared use.

- Swagger UI: http://localhost:5200/swagger
- OpenAPI document: http://localhost:5200/swagger/v1/swagger.json

Build and test:

```bash
./mvnw verify                     # compiles, runs unit + integration tests, builds target/pap-api-java-1.0.0.jar
java -jar target/pap-api-java-1.0.0.jar
```

## Configuration

Settings live in `src/main/resources/application.yml` and can be overridden with environment variables or
`--property=value` arguments.

| Property | Environment variable | Default |
| --- | --- | --- |
| `server.port` | `PORT` | `5200` |
| `pap.jwt.secret` | `PAP_JWT_SECRET` | development key (same as the .NET default); **set a real secret in production** |
| `pap.jwt.access-token-expiry-hours` | | `2` |
| `pap.jwt.refresh-token-expiry-days` | | `7` |
| `pap.data-store.folder` | `PAP_DATA_FOLDER` | `data` (relative to the working directory) |
| `pap.cors.allowed-origin-patterns` | | `http://localhost:5173`, `https://app-pap-ui-*.azurewebsites.net` |

## Compatibility with pap-api-net

- **Routes and payloads:** identical. Enum values are PascalCase (`"Permit"`, `"Equals"`). Rule-combining
  algorithms use their XACML short names (`"deny-overrides"`). Nulls are omitted. Timestamps are ISO-8601 UTC.
- **Errors:** RFC 7807 problem responses with the same `type`, `title` and `errors` keys and messages, for example
  `{"errors": {"Request.Name": ["'Request Name' must not be empty."]}}`. Responses also carry the standard
  `instance` member, which Spring adds automatically.
- **Auth:** HS256 JWTs with the same claims and secret. Tokens issued by one back end are accepted by the other.
  A missing or invalid token gets `401` with an empty body and a `WWW-Authenticate: Bearer` challenge, which
  triggers the UI's token refresh.
- **Data files:** the JSON files in the data folder use the .NET format, including BCrypt password hashes, so
  both back ends can run against the same folder. Writes are atomic (temp file + move) and serialized per file.
- **Policy evaluation:** the same XACML 3.0-style evaluator: targets and nested AND/OR conditions, the
  permit-overrides / deny-overrides / first-applicable rule-combining algorithms, Indeterminate handling, and
  deny-overrides across policies.

Known differences:
- The text of JSON parse errors (`errors["$"]`) is worded differently.
- CORS preflight responses are `200` rather than `204`.

## Project layout

```
src/main/java/com/pap
├── config/        Spring configuration: security, CORS, Jackson, OpenAPI, typed properties
├── domain/model/  entities, value objects and enums
├── json/          Jackson support shared by the API and the data files
├── repository/    JSON file persistence with domain scoping
├── security/      JWT issuing/validation, request filter, current-user access
├── service/       application services (auth, users, policies, test cases, ...)
│   ├── evaluation/  policy evaluator
│   └── export/      XACML XML/JSON export
└── web/           REST controllers, request/response DTOs, global error handling
```
