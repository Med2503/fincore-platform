# ADR-005 — Transactional Outbox

## Status

Accepted

## Problem

A service may successfully commit a database transaction while
Kafka publishing fails.

Example:

1. save Order
2. commit PostgreSQL
3. Kafka unavailable
4. OrderCreated is lost

## Decision

Use the Transactional Outbox pattern.

Business data and the outbox record are persisted in the same
local database transaction.

## Flow

Business operation
↓
DB transaction
├── business data
└── outbox_events
↓
commit
↓
Outbox publisher
↓
Kafka

## Consequence

The publisher must support:

- retries
- failure recovery
- idempotency
- published status
- retry counters

Consumers must also be idempotent.