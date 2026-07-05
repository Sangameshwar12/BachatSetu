# BachatSetu AI Context

Version: 1.0

---

## Purpose

This directory contains the permanent AI knowledge base for the BachatSetu project.

Every AI coding assistant (Codex, ChatGPT, Claude Code, GitHub Copilot, Cursor, etc.) MUST read these documents before generating, modifying, reviewing, or refactoring code.

The objective is to ensure:

- Consistent architecture
- Consistent coding standards
- Correct business rules
- Production-quality implementations
- No architectural violations

---

## Project Overview

BachatSetu is an enterprise-grade fintech platform that digitizes traditional Indian Bhishi (ROSCA/Chit Fund) management.

The project is designed using:

- Domain Driven Design (DDD)
- Hexagonal Architecture
- Clean Architecture
- Modular Monolith
- SOLID Principles

The system must be secure, scalable, maintainable, and production-ready.

---

## AI Workflow

Before performing any task:

1. Read all required AI context documents.
2. Understand the requested sprint.
3. Generate code ONLY within sprint scope.
4. Never modify unrelated modules.
5. Keep the build green.
6. Update documentation if architecture changes.
7. Generate tests whenever production code changes.

---

## Required Reading Order

Every AI assistant must read the following files in order:

1. codex-system-prompt.md
2. engineering-rules.md
3. coding-style.md
4. architecture-principles.md
5. business-rules.md
6. sprint-definition.md
7. project-roadmap.md
8. glossary.md

---

## AI Responsibilities

AI is responsible for:

- Writing production-quality code
- Following engineering standards
- Respecting architecture boundaries
- Creating tests
- Updating documentation

AI must never:

- Guess business rules
- Violate architecture
- Generate placeholder code
- Ignore existing standards
- Introduce unnecessary dependencies

---

## Folder Structure

.ai/

README.md

codex-system-prompt.md

engineering-rules.md

coding-style.md

architecture-principles.md

business-rules.md

sprint-definition.md

project-roadmap.md

glossary.md

prompts/

---

## Versioning

Current Version:

AI Knowledge Base v1.0

Any modification to these files must be reviewed before merging.