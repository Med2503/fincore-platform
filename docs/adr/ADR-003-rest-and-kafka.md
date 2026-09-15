# ADR-003 — REST and Kafka

## Status

Accepted

## Decision

Use REST for synchronous request/response interactions.

Use Kafka for asynchronous domain events.

## REST

Appropriate for:

- queries requiring current state
- authentication
- immediate validation
- synchronous commands

## Kafka

Appropriate for:

- domain events
- integration events
- asynchronous workflows
- decoupling services

## Consequence

FINCORE must explicitly handle:

- retries
- idempotency
- ordering
- dead-letter queues
- schema evolution
- eventual consistency