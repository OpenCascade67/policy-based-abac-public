# policy-based-abac
Solution contains PAP and PDP components.

- `pap-ui-react` - React UI for policy administration and testing
- `pap-api-net` - .NET 8 PAP back end
- `pap-api-java` - Java 21 / Spring Boot PAP back end with the same API as `pap-api-net` (see its README)
- `pdp-api-java` - Java 21 / Spring Boot Policy Decision Point: evaluates the PAP's policies for applications,
  in the PAP's native JSON format or XACML 3.0 JSON Profile (see its README)
- `infra` - deployment infrastructure

## Running locally

| Component | Start (from its folder) | Address |
| --- | --- | --- |
| PAP API (Java) | `mvnw spring-boot:run` | http://localhost:5200 |
| PDP API | `mvnw spring-boot:run` | http://localhost:5300 (reads `../pap-api-java/data`) |
| UI | `npm i && npm run dev` | http://localhost:5173 |

Policies saved in the PAP take effect in the PDP within a couple of seconds, without a restart.
