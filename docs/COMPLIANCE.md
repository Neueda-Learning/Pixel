# Compliance & Non-Functional Roadmap (US Market)

Target market: **United States**. This document tracks the regulatory, security, and
non-functional requirements relevant to a US-based personal portfolio/finance application, and
what's currently implemented vs. planned for future sprints. It is a planning doc, not a legal
compliance certification — consult counsel before any production launch handling real financial
data.

## Current State (as of this doc)

- No authentication/authorization layer (`pom.xml` has no `spring-security` dependency) — all API
  endpoints are open.
- No encryption at rest for the MySQL database (`docker-compose.yml` uses a plain named volume).
- No TLS/HTTPS termination configured anywhere (nginx `frontend/nginx.conf` serves plain HTTP;
  backend listens on plain HTTP `8080`).
- No audit logging of who viewed/changed transaction data.
- No data retention/deletion policy or user data export (right-to-delete/right-to-access).
- Secrets (DB password, Finnhub/Twelve Data API keys) are passed via plain environment variables
  (`.env`), not a secrets manager.

## Applicable US Regulatory Frameworks (Future Consideration)

> None of these are implemented today. Listed here so they're evaluated before any real user data
> or real money is handled.

| Framework | Relevance | Status |
|---|---|---|
| **GLBA** (Gramm-Leach-Bliley Act) — safeguards for nonpublic personal financial information | Applies if this app is offered as a financial institution service handling real customer financial data | Not implemented — future release |
| **SEC Reg S-P / S-ID** — safeguarding customer records, identity-theft red flags | Applies only if the app acts as a broker-dealer/investment adviser; currently out of scope as a personal tracker | Not implemented — future release, re-evaluate if scope changes |
| **CCPA/CPRA** (California) + other state privacy laws | Applies to any US consumer-facing app collecting personal data, regardless of financial-institution status | Not implemented — future release |
| **SOC 2 Type II** | Common enterprise/customer expectation for SaaS handling financial data; not a law but a widely requested attestation | Not implemented — future release |
| **ADA / WCAG 2.1 AA** | Accessibility compliance for US-facing public web apps | Not implemented — future release |

## Encryption Plan (Future Release)

- **In transit:** Terminate TLS at the nginx/frontend layer (or a reverse proxy/load balancer in
  front of it) and enforce HTTPS-only; enable TLS between the frontend and backend, and between
  the backend and MySQL, in production deployments.
- **At rest:** Enable MySQL data-at-rest encryption (e.g. encrypted EBS/volume backing the
  `mysqldata` volume, or MySQL's own tablespace encryption) for production.
- **Secrets management:** Move `FINNHUB_API_KEY`, `TWELVEDATA_API_KEY`, and DB credentials out of
  plain `.env` files into a managed secrets store (e.g. AWS Secrets Manager / Vault) for any
  non-local environment.
- **Application-level:** Evaluate field-level encryption or hashing for any future PII fields
  (currently the schema has no user/PII tables — `instruments`, `price_history`, `transaction`
  only).

## Non-Functional Compliance Targets (Future Sprints)

- [ ] Add authentication/authorization (e.g. Spring Security + OAuth2/OIDC) — currently no user
      identity model exists at all.
- [ ] Add per-request audit logging for transaction create/edit/delete actions.
- [ ] Define a data retention and deletion policy, plus a user data export mechanism.
- [ ] Enforce HTTPS end-to-end in all non-local environments.
- [ ] Add automated dependency/vulnerability scanning to CI (e.g. `mvn dependency-check`,
      `npm audit`) — not currently part of `.github/workflows/ci.yml`.
- [ ] Add rate limiting / abuse protection on public API endpoints.
- [ ] Define a backup and disaster-recovery policy for the MySQL volume.
- [ ] Accessibility audit against WCAG 2.1 AA for the frontend.

## Out of Scope for Now

This project currently functions as a personal/demo portfolio tracker with no user accounts, no
real brokerage integration, and no PII storage — so GLBA/SEC obligations don't yet apply. This
section should be re-reviewed the moment any of the following change: real user accounts are
added, real money movement/trading is introduced, or the app is offered to external customers.
