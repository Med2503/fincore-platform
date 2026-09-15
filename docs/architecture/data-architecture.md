# FINCORE — Data Architecture

FINCORE follows the Database-per-Service pattern.

## Databases

- identity_db
- wealth_db
- ledger_db
- execution_db
- quant_db
- intelligence_db

edge-service does not own a business database.

## Rules

A service owns its database.

No service directly accesses another service's database.

No foreign key crosses a service boundary.

Cross-service relationships use logical identifiers.

Example:

wealth-service:

customer.identityId

The identityId references an identity concept but is not a PostgreSQL foreign key to identity_db.

## Transactions

Transactions are local to a service.

FINCORE does not use distributed @Transactional transactions across microservices.

Cross-service workflows use:

- domain events
- transactional outbox
- idempotent consumers
- Saga-style coordination when required.

## Financial integrity

ledger-service is the authoritative source for financial movements.

Double-entry accounting must preserve:

sum(debits) = sum(credits)