# ADR-001 — Microservices Architecture

## Status

Accepted

## Context

FINCORE contains several domains with different business responsibilities,
scaling characteristics and consistency requirements.

## Decision

Use seven independently deployable microservices:

- edge-service
- identity-service
- wealth-service
- ledger-service
- execution-service
- quant-service
- intelligence-service

## Why

This allows:

- bounded contexts
- independent deployment
- independent scaling
- isolated persistence
- clearer ownership
- domain-oriented architecture

## Rejected alternative

A modular monolith was considered.

It would simplify deployment but would not demonstrate the distributed
architecture, messaging, resilience and operational concerns targeted by FINCORE.

## Consequences

Positive:

- strong service boundaries
- independent scaling
- failure isolation

Negative:

- distributed complexity
- eventual consistency
- operational complexity
- observability requirements