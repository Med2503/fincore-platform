# FINCORE — Service Communication

FINCORE uses two communication styles.

## Synchronous communication

REST is used when the caller requires an immediate response.

Examples:

- login
- retrieving a portfolio
- retrieving an order
- retrieving an account
- creating a customer

## Asynchronous communication

Kafka is used for domain events and workflows that do not require an immediate response.

Examples:

- OrderCreated
- RiskAssessmentCompleted
- OrderExecuted
- PositionUpdated
- SettlementCompleted
- ReportGenerated

## Principle

REST answers:

"What is the current state?"

Events communicate:

"Something happened."

## Consistency

Strong consistency is required for:

- ledger entries
- financial transaction integrity
- account balance updates
- local order state transitions

Eventual consistency is acceptable for:

- portfolio projections
- risk snapshots
- reporting
- compliance analytics