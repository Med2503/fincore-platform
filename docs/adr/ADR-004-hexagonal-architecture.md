# ADR-004 — Hexagonal Architecture

## Status

Accepted

## Decision

Business domains use Hexagonal Architecture combined with DDD
and Clean Architecture principles.

## Dependency direction

API
↓
Application
↓
Domain

Infrastructure depends on the domain/application contracts.

The domain must not depend on:

- Spring
- JPA
- PostgreSQL
- Kafka
- HTTP

## Goal

Keep business rules independent from technical infrastructure.

## Consequence

More initial code is required.

However, the architecture improves:

- testability
- maintainability
- replaceability
- separation of concerns