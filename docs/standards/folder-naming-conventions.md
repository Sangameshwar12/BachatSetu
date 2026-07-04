# Folder Naming Conventions

Folder names should be predictable across backend, mobile, admin, infrastructure, contracts, and documentation.

## General Rules

- Use lowercase names for repository-level folders.
- Use kebab-case for multi-word repository folders.
- Use technology-native conventions inside each project.
- Avoid abbreviations unless they are widely understood.
- Prefer domain names over technical layer names for feature folders.
- Do not use spaces in folder names.

## Top-Level Repository Folders

Approved top-level folder names:

```text
backend
mobile
admin-portal
infrastructure
database
contracts
docs
scripts
```

## Documentation Folders

Use lowercase kebab-case:

```text
architecture
operations
roadmap
standards
tooling
workflow
```

Future documentation folders:

```text
product
compliance
runbooks
release-notes
threat-models
```

## Backend Folders

Use Java and Maven conventions:

```text
src/main/java
src/main/resources
src/test/java
src/test/resources
```

Inside the Java package, use lowercase package names:

```text
shared
modules
identity
tenant
bhishi
ledger
payment
notification
audit
reporting
```

## Flutter Folders

Use Dart and Flutter conventions with snake_case where needed:

```text
lib
test
integration_test
assets
features
shared
core
```

Feature folders should be lowercase snake_case only when multi-word names are needed.

## React TypeScript Folders

Use lowercase or kebab-case:

```text
src
routes
components
features
services
hooks
types
styles
tests
```

Component files can use PascalCase, but folders should remain lowercase or kebab-case.

## Infrastructure Folders

Use provider and environment names:

```text
aws
terraform
docker
environments
dev
staging
production
```

Do not use ambiguous environment names such as `prod2`, `new-prod`, or `test-final`.

