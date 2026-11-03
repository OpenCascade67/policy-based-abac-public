# PDP API (Java)

Spring Boot Policy Decision Point (PDP). It answers authorization requests from applications (PEPs) using
the policies administered in the PAP ([`pap-api-java`](../pap-api-java) or [`pap-api-net`](../pap-api-net)).
It uses the same evaluation engine as the PAP's test cases, so a request gets the same decision in a PAP
test case and from the PDP.

## How it works

- **Policy source:** the PDP reads `policies.json`, `shared-rules.json` and `domains.json` from the PAP's data
  folder. It never writes to that folder. It re-checks the files every `refresh-interval` (default 2s) and
  reloads them when they change, so policies saved in the PAP go live without a restart. If a file can't be
  read, for example because the PAP is mid-write, the last good data keeps being served.
- **Domains:** every request names an administration domain, and only that domain's policies and shared rules
  apply. Policies created before domains existed belong to the default domain, as they do in the PAP.
- **Combining:** rules combine with each policy's algorithm (permit-overrides, deny-overrides or
  first-applicable). The domain's policies are then combined with **deny-overrides**.
- **Clients:** PEP applications authenticate with an API key in the `X-API-Key` header. A client can be limited
  to specific domains.

## Requirements

- JDK 21+
- Maven is optional: the bundled wrapper (`mvnw` / `mvnw.cmd`) downloads it on first use

## Running

```bash
./mvnw spring-boot:run            # Windows: mvnw.cmd spring-boot:run
```

The PDP listens on **http://localhost:5300**. By default it reads `../pap-api-java/data`, which is where
`pap-api-java` keeps its data when both services are started from their project folders.

- Swagger UI: http://localhost:5300/swagger (OpenAPI document at `/swagger/v1/swagger.json`)
- Health (public): http://localhost:5300/actuator/health, including what the policy store has loaded

```bash
./mvnw verify                     # unit + integration tests, builds target/pdp-api-java-1.0.0.jar
```

## API

All endpoints are `POST`, need `X-API-Key`, and return errors as RFC 7807 problems (`application/problem+json`).

### Native format: `POST /api/v1/domains/{domainId}/decisions`

The body uses the same JSON as the PAP's test-case input:

```json
{
  "subject":     { "role": "admin", "department": "finance" },
  "resource":    { "type": "report" },
  "action":      { "id": "read" },
  "environment": { "time": "2026-11-03T09:00:00Z" }
}
```

```json
{
  "decision": "Permit",
  "matchedPolicies": ["Finance Read"],
  "obligations": [{ "obligationId": "audit-log", "fulfillOn": "Permit", "attributeAssignments": [] }]
}
```

- `decision` is `Permit`, `Deny`, `NotApplicable` or `Indeterminate`. `Indeterminate` means a policy couldn't be
  evaluated, for example because of an invalid regex or values that can't be compared.
- `?policyId={id}` evaluates a single policy instead of the whole domain.
- `POST .../decisions/batch` takes `{"requests": [ ... ]}` and returns `{"results": [ ... ]}` in the same order.
  Every request in a batch is evaluated against the same policy snapshot.

### XACML 3.0 JSON Profile: `POST /api/v1/domains/{domainId}/xacml/decisions`

This endpoint accepts and returns `application/xacml+json` (plain `application/json` is also accepted).

```json
{
  "Request": {
    "ReturnPolicyIdList": true,
    "AccessSubject": { "Attribute": [ { "AttributeId": "role", "Value": "admin", "IncludeInResult": true } ] },
    "Action":        { "Attribute": [ { "AttributeId": "urn:oasis:names:tc:xacml:1.0:action:action-id", "Value": "read" } ] }
  }
}
```

```json
{
  "Response": [{
    "Decision": "Permit",
    "Status": { "StatusCode": { "Value": "urn:oasis:names:tc:xacml:1.0:status:ok" } },
    "Obligations": [{ "Id": "audit-log", "AttributeAssignment": [ { "AttributeId": "level", "Value": 1,
                      "DataType": "http://www.w3.org/2001/XMLSchema#integer" } ] }],
    "Category": [{ "CategoryId": "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject",
                   "Attribute": [ { "AttributeId": "role", "Value": "admin", "IncludeInResult": true } ] }],
    "PolicyIdentifierList": { "PolicyIdReference": [ { "Id": "…", "Version": "3.0" } ] }
  }]
}
```

- **Categories:** both the shorthand members (`AccessSubject`, `Resource`, `Action`, `Environment`,
  `RecipientSubject`, `IntermediarySubject`, `Codebase`, `RequestingMachine`) and the generic `Category` array
  are supported. They map to the policy categories `subject`, `resource`, `action` and `environment`.
- **Attribute ids:** each `AttributeId` is matched as written. A URN id is also matched by its last segment
  (`…:action:action-id` → `action-id`), unless the request sends that short name explicitly. Repeated
  attributes and array values form bags.
- **Multiple decisions:** repeat a category (each combination is decided) or use `MultiRequests` /
  `RequestReference`. One request may expand to at most `max-batch-size` decisions.
- **Errors:** a body that is JSON but not a valid XACML request returns one `Indeterminate` result with status
  `syntax-error`. Evaluation errors return `processing-error`.
- **Not supported:** `CombinedDecision` (it returns `Indeterminate` / `processing-error`), and `Content`, XPath and
  advice.

### HTTP status codes

| Status | When |
| --- | --- |
| 200 | A decision was made; this includes `Deny`, `NotApplicable` and `Indeterminate` |
| 400 | The body isn't JSON or isn't a request object, a batch is too large, or an id is malformed |
| 401 | `X-API-Key` is missing or wrong |
| 403 | The client may not use this domain, or the domain is inactive |
| 404 | Unknown domain, or unknown `policyId` |

A PEP should treat any non-200 response, and any decision other than `Permit`, as a denial.

## Configuration

Settings live in `src/main/resources/application.yml` and can be overridden with environment variables or
`--property=value` arguments.

| Property | Environment variable | Default |
| --- | --- | --- |
| `server.port` | `PORT` | `5300` |
| `pdp.policy-store.folder` | `PDP_POLICY_FOLDER` | `../pap-api-java/data` |
| `pdp.policy-store.refresh-interval` | | `2s` (`0s` checks on every request) |
| `pdp.max-batch-size` | | `100` |
| `pdp.clients[n].name` / `.api-key` / `.domains` | `PDP_API_KEY` (first client's key) | one `development` client with a development key; **replace it in production** |
| `pdp.cors.allowed-origin-patterns` | | none (PEPs are usually servers) |

API keys must be at least 16 characters, and each must be unique. Only their SHA-256 digests are kept in
memory, and they are compared in constant time. Each decision is logged by the `com.pdp.audit` logger (client,
domain, decision, matched policies, latency).

```yaml
pdp:
  clients:
    - name: orders-service
      api-key: ${ORDERS_PDP_KEY}
      domains: [00000000-0000-0000-0000-000000000001]
```

## Project layout

```
src/main/java/com/pdp
├── config/       Spring configuration: security, CORS, Jackson, OpenAPI, typed properties
├── domain/model/ read-only policy model (the PAP file format)
├── evaluation/   policy evaluator, the same engine as the PAP's
├── json/         Jackson support for enums and the PAP data files
├── security/     API-key authentication
├── service/      decision service (domain scoping, batching, audit log)
├── store/        file-backed policy store with change detection, plus its health indicator
├── xacml/        XACML 3.0 JSON Profile parsing and responses
└── web/          REST controllers, DTOs, global error handling
```

The evaluator and policy model are copied from `pap-api-java`, and both copies have the same unit-test
scenarios. If PAP evaluation changes, update both, or move them into a shared library module.
