# Security Policy

Version: 1.0
Last reviewed: 2026-07-05

## Supported Versions

| Version | Supported |
| --- | --- |
| `main` and the current release line | Yes |
| Older or unmaintained release lines | No |

Until the first production release, security fixes target `main` and any active `release/*` branch.

## Responsible Disclosure

Do not report suspected vulnerabilities through public issues, discussions, pull requests, or social media. Use GitHub's private vulnerability reporting for this repository through **Security > Advisories > Report a vulnerability**.

If private reporting is unavailable, contact the repository owner privately through GitHub before sharing technical details. Include only the information necessary to establish a secure communication channel.

## Reporting Process

Provide:

- A concise vulnerability description
- Affected component and revision
- Reproduction steps or proof of concept
- Expected security impact
- Known prerequisites or mitigations
- A safe method for follow-up contact

Do not access data that is not yours, disrupt services, persist access, or publish the vulnerability before coordinated disclosure.

## Response Expectations

The maintainers will acknowledge a valid private report, assess severity, coordinate remediation, and agree on disclosure timing. Response time depends on severity and project availability; active exploitation or risk to financial or personal data receives immediate priority.

## Dependency Updates

- Dependabot checks Maven and GitHub Actions dependencies weekly.
- Dependency Review blocks pull requests that introduce high-severity vulnerable dependencies.
- Critical runtime vulnerabilities require an expedited patch or a documented compensating control.
- Dependency exceptions require owner approval, expiry, and a remediation issue.

## Secret Management

- Never commit credentials, API keys, tokens, private keys, production identifiers, or real customer data.
- Use GitHub encrypted secrets for CI and an approved secret manager for deployed environments.
- Treat an exposed secret as compromised: revoke or rotate it before removing it from history.
- Secret scanning runs on pushes and pull requests; bypassing it requires security-owner approval.

## Security Process

Internal triage, remediation, escalation, and closure requirements are defined in [Security Process](docs/governance/security-process.md). Platform design controls are defined in [Security Standards](docs/architecture/security-standards.md).
