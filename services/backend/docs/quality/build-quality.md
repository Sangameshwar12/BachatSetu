# Build Quality Gates

## Policy

`mvn clean verify` is the single local and CI quality command. A pull request is not releasable unless compilation, tests, static analysis, coverage, and dependency policy all pass.

## Gates

| Gate | Build phase | Policy |
| --- | --- | --- |
| Maven Enforcer | validate | Java 21, Maven 3.9+, dependency convergence, no duplicate dependency declarations, and banned legacy dependencies. |
| Checkstyle | verify | Deterministic source hygiene, imports, modifiers, statement layout, 160-character limit, and no unresolved work markers. |
| PMD | verify | High-signal correctness, unused code, null-check, and resource-lifecycle rules. |
| SpotBugs | verify | Medium-or-higher bytecode defect findings fail the build at maximum analysis effort. |
| JaCoCo | verify | At least 80% line coverage for invariant-bearing core domain classes. |

## Coverage Scope

The initial 80% gate covers the `BaseAggregateRoot`, `Money`, `SavingsGroup`, `Payment`, `PaymentAttempt`, `Draw`, and `AuctionBid` families. The prefix-based instrumentation also includes their directly named enum and value types, so the current report contains 14 compiled classes. These classes contain current financial and lifecycle invariants. Configuration, generated MapStruct code, JPA mapping boilerplate, and unrelated data-only records are not used to dilute or inflate this gate.

Coverage scope must expand as additional aggregates gain executable business behavior. Reducing the threshold or excluding an invariant-bearing class requires an architecture review and documentation update.

## Static Analysis Configuration

Configuration is versioned under `config/quality`:

- `checkstyle.xml`
- `pmd-ruleset.xml`
- `spotbugs-exclude.xml`

Suppressions must be narrow, documented, and tied to a framework-generated or framework-mandated pattern. Whole-module suppressions are not acceptable.

## CI Reports

GitHub Actions runs the same `mvn clean verify` command and uploads Checkstyle, PMD, SpotBugs, and JaCoCo reports even when a gate fails. This keeps failures diagnosable without reproducing CI locally.
