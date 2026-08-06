# Security & OWASP Top 10 (2021) Threat Map

Companion doc to [COMPLIANCE.md](./COMPLIANCE.md). Maps this codebase against the
[OWASP Top 10 (2021)](https://owasp.org/Top10/), defines what PRs must call out from a security
standpoint, and lists non-functional security requirements for future sprints.

## OWASP Top 10 Threat Map

| # | Category | Applies here? | Current state | Future work |
|---|---|---|---|---|
| A01 | Broken Access Control | Yes | No auth/authz exists at all — every `/api/**` endpoint (`InstrumentController`, `TransactionController`, `PortfolioController`, `RiskController`, `MarketController`) is unauthenticated and unauthorized. Any caller can create/edit/delete transactions. | Add authentication (Spring Security + OAuth2/OIDC) and per-endpoint authorization. |
| A02 | Cryptographic Failures | Yes | No TLS anywhere (backend on plain HTTP `8080`, nginx serves plain HTTP); no encryption at rest for MySQL; DB/API-key secrets passed as plain env vars. | See encryption plan in [COMPLIANCE.md](./COMPLIANCE.md). |
| A03 | Injection | Partially mitigated | JPA repositories use derived queries and parameterized `@Query` JPQL (`PriceHistoryRepository`, `TransactionRepository`) — no raw string-concatenated SQL found. Symbol input is uppercased and validated against `instrumentRepository.existsById` before use. | Keep enforcing parameterized queries in code review; add static analysis (e.g. `spotbugs` + `find-sec-bugs`) to CI. |
| A04 | Insecure Design | Yes | No rate limiting, no request size limits, no abuse protection on any endpoint; no threat model has been formally reviewed for this app. | Add rate limiting/throttling; do a design-level threat modeling pass before adding auth. |
| A05 | Security Misconfiguration | Yes | CORS (`CorsConfig`) allows all headers (`allowedHeaders("*")`) though origins are restricted via `CORS_ALLOWED_ORIGINS`; Spring Boot Actuator is on the classpath (`spring-boot-starter-actuator`) with no visibility into which endpoints are exposed/secured; `ddl-auto: update` is used for schema management (dev convenience, risky if ever pointed at prod). | Restrict CORS headers to what's actually needed; explicitly enable/secure only required actuator endpoints; move to versioned migrations (Flyway/Liquibase) before production. |
| A06 | Vulnerable and Outdated Components | Yes | No automated dependency/vulnerability scanning in `.github/workflows/ci.yml` (no `npm audit`, no `mvn dependency-check`, no Dependabot config found). | Add dependency scanning to CI (`npm audit --audit-level=high`, OWASP Dependency-Check Maven plugin, or GitHub Dependabot). |
| A07 | Identification and Authentication Failures | Yes | No identity model exists — there are no users, sessions, tokens, or passwords anywhere in the codebase. | Same as A01 — must be designed together with an auth solution. |
| A08 | Software and Data Integrity Failures | Partially applicable | CI builds/tests run on every PR (`ci.yml`), but there's no artifact signing/verification, and Docker base images (`maven:3.9-eclipse-temurin-17`, `eclipse-temurin:17-jdk`, `mysql:8.4`) aren't pinned to digests. | Pin Docker base images by digest; add CI supply-chain checks. |
| A09 | Security Logging and Monitoring Failures | Yes | `GlobalExceptionHandler` logs unhandled exceptions (`log.error(...)`), but there's no audit trail for who created/edited/deleted a transaction, and no centralized log aggregation/alerting. | Add audit logging for transaction mutations; ship logs to a monitored aggregator in non-local environments. |
| A10 | Server-Side Request Forgery (SSRF) | Low risk today, worth watching | `MarketService`/`TwelveDataHistoricalService` build outbound URLs using a fixed `base-url` plus a user-supplied `symbol` path segment — symbol is validated against the `instruments` table before use in price-history lookups, limiting attacker-controlled URL construction. | Re-review if any future feature lets a user supply an arbitrary external URL/host. |

## Security Implications Section for PRs

Starting with this documentation, every PR that touches the backend, frontend API layer, CI/CD
config, or Docker/infra files must include a short **"Security implications"** note in the PR
description, answering:

1. Does this change touch authentication, authorization, or any data-access boundary?
2. Does it introduce a new external call, new user input, or new file/DB write path?
3. Does it change CORS, headers, secrets handling, or dependency versions?
4. If yes to any of the above — which OWASP Top 10 category (see table above) does it relate to,
   and what mitigation was added (or is deferred to a future release)?

If the answer to all of the above is "no", the PR can simply state: `Security implications: none —
no new input/auth/dependency/config surface touched.`

> Note: none of the PRs merged so far include a "Security implications" note, since this
> requirement is being introduced only now, after that work was already done. It applies to all
> PRs opened from this point forward.

## Non-Functional Security Requirements (Future Sprints)

- [ ] Authentication + authorization for all `/api/**` endpoints (currently fully open).
- [ ] TLS enforced end-to-end in non-local environments (see [COMPLIANCE.md](./COMPLIANCE.md)).
- [ ] Dependency/vulnerability scanning wired into `.github/workflows/ci.yml`.
- [ ] Rate limiting on public API endpoints.
- [ ] Audit logging for all transaction create/update/delete operations.
- [ ] Restrict CORS `allowedHeaders` to an explicit list instead of `*`.
- [ ] Pin Docker base images to digests instead of floating tags.
- [ ] Replace `ddl-auto: update` with versioned migrations (Flyway/Liquibase) before any production
      deployment.
- [ ] Secure/disable unused Spring Boot Actuator endpoints.

## Out of Scope for Now

As with [COMPLIANCE.md](./COMPLIANCE.md), this app currently has no user accounts and no real
money movement, so several items above (especially A01/A07 auth) are flagged as required before
any production launch rather than immediate blockers for the current demo/learning scope.
