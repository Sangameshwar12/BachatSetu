# Security Process

Version: 1.0
Effective date: 2026-07-05
Status: Authoritative

## Scope

This document defines the internal process for vulnerability intake, triage, remediation, dependency risk, secret exposure, disclosure, and closure. Public reporting instructions remain in [SECURITY.md](../../SECURITY.md); platform controls remain in [Security Standards](../architecture/security-standards.md).

## Intake

Security reports must enter a private channel, preferably GitHub private vulnerability reporting. The receiver must restrict access to the minimum response group and preserve the original report and timestamps.

Create a private tracking record containing:

- Reporter and secure contact method
- Affected revision, component, environment, and data class
- Reproduction evidence
- Suspected impact and exploit prerequisites
- Disclosure status
- Assigned security owner

Never copy exploit details, secrets, personal data, or customer identifiers into public issues or ordinary logs.

## Triage

Validate safely and classify severity using exploitability, affected users/tenants, privilege gained, confidentiality, financial integrity, availability, persistence, and active exploitation.

| Severity | Examples | Initial action |
| --- | --- | --- |
| Critical | Active exploitation, authentication bypass, financial or broad sensitive-data compromise | Immediate incident response and release block |
| High | Practical privilege escalation, cross-tenant access, material payment or PII exposure | Expedited remediation and security-owned release |
| Medium | Limited exposure or defense-in-depth failure requiring meaningful prerequisites | Prioritized sprint remediation |
| Low | Minimal impact, hardening, or informational weakness | Backlog with owner and rationale |

If evidence suggests active compromise, transition immediately to incident response, preserve evidence, restrict access, rotate affected credentials, and involve the repository owner.

## Remediation

1. Reproduce in an isolated environment with synthetic data.
2. Define containment and compensating controls.
3. Implement the smallest complete correction on a private branch or advisory fork.
4. Add regression, authorization, boundary, and abuse-case tests appropriate to the weakness.
5. Review for variants in adjacent modules and environments.
6. Run all quality, dependency, secret, architecture, and release gates.
7. Obtain security-owner and affected code-owner approval.
8. Release through the hotfix or planned release process according to severity.

Risk acceptance requires a documented owner, expiry, business justification, compensating controls, and remediation issue. Critical active risk cannot be accepted as routine backlog.

## Dependency Vulnerabilities

Dependabot proposes Maven and GitHub Actions updates weekly. Dependency Review blocks newly introduced high-severity vulnerable dependencies.

For a reported dependency vulnerability:

- Confirm whether the affected code path and version are present.
- Prefer a supported stable upgrade.
- Test compatibility and transitive dependency changes.
- If no patch exists, remove or isolate the dependency, disable the vulnerable capability, or apply a documented compensating control.
- Record false positives and temporary exceptions with evidence and expiry.

## Secret Exposure

Treat every committed or logged secret as compromised even if quickly deleted.

1. Revoke or rotate the credential immediately.
2. Assess usage logs and affected environments.
3. Contain access and preserve evidence.
4. Remove the secret from current files and history using an approved coordinated procedure.
5. Update dependent systems with the replacement secret through approved secret management.
6. Verify secret scanning and document root cause and preventive action.

History cleanup does not replace rotation.

## Disclosure

Coordinate disclosure with the reporter after affected users and environments are protected. Release notes must provide actionable impact and upgrade guidance without exposing exploit details prematurely. Credit is granted only with reporter consent.

Legal, regulatory, contractual, customer, and law-enforcement notification decisions belong to authorized leadership and counsel.

## Closure

A security issue closes only when remediation is deployed to supported versions, regression tests pass, affected secrets are rotated, monitoring shows no continuing exploitation, documentation and changelog are updated, the reporter is informed where appropriate, and follow-up actions have owners and dates.

Significant findings require a blameless review covering detection, response, control gaps, user impact, timeline, and prevention.
