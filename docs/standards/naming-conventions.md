# Naming Conventions

Consistent naming helps the platform scale across engineering teams, product modules, APIs, databases, and infrastructure.

## Product Vocabulary

Use consistent business terms:

| Preferred Term | Avoid |
| --- | --- |
| Bhishi group | Committee group in code |
| Contribution | Payment installment when referring to group obligation |
| Payout | Withdrawal when referring to Bhishi disbursement |
| Member | Customer when referring to group participants |
| Organizer | Admin when referring to group-level manager |
| Tenant | Organization, client, account in SaaS context |

## Repository Folders

Use lowercase kebab-case for top-level folders:

```text
admin-portal
backend
mobile
infrastructure
database
contracts
docs
```

## Java

- Packages: lowercase dot notation
- Classes: PascalCase
- Methods: camelCase
- Variables: camelCase
- Constants: UPPER_SNAKE_CASE
- Exceptions: end with `Exception`
- Controllers: end with `Controller`
- Application services: end with `Service`
- Repositories: end with `Repository`

Base package:

```text
in.bachatsetu
```

## TypeScript

- Components: PascalCase
- Hooks: `useSomething`
- Functions: camelCase
- Variables: camelCase
- Constants: UPPER_SNAKE_CASE
- Files for components: PascalCase
- Utility files: kebab-case

## Flutter and Dart

- Files: snake_case
- Classes: PascalCase
- Methods and variables: lowerCamelCase
- Constants: lowerCamelCase or SCREAMING_SNAKE_CASE depending on team convention selected at implementation
- Widgets: descriptive PascalCase

## APIs

- JSON fields: camelCase
- URL paths: kebab-case
- Query parameters: camelCase
- Headers: Title-Case or standard HTTP header casing
- Error codes: UPPER_SNAKE_CASE

## Database

- Tables: plural snake_case
- Columns: snake_case
- Indexes: `idx_{table}_{columns}`
- Unique constraints: `uk_{table}_{columns}`
- Foreign keys: `fk_{source_table}_{target_table}`

## Git Branches

Branch names should be lowercase kebab-case:

```text
feature/bhishi-group-creation
bugfix/payment-status-retry
hotfix/admin-login-outage
release/2026-08-01
```

