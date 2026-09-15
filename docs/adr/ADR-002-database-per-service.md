# ADR-002 — Database per Service

## Status

Accepted

## Decision

Each business microservice owns an independent PostgreSQL database.

## Rules

No cross-service database access.

No cross-service foreign keys.

No shared business tables.

## Reason

Database ownership must follow bounded-context ownership.

This prevents hidden coupling between services.

## Consequence

Cross-service data requires:

- REST
- events
- local projections
- snapshots

depending on the use case.