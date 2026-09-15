# FINCORE — Event Catalog

## Identity

- UserRegistered
- UserAuthenticated
- UserLocked
- RoleAssigned
- RefreshTokenRevoked

## Wealth

- CustomerCreated
- InvestorProfileUpdated
- PortfolioCreated
- PositionUpdated
- PortfolioRevaluated

## Ledger

- AccountOpened
- MoneyDeposited
- MoneyWithdrawn
- TransferCompleted
- FinancialTransactionCompleted
- LedgerEntryCreated

## Execution

- OrderCreated
- OrderValidated
- OrderRiskApproved
- OrderRiskRejected
- OrderExecuted
- OrderPartiallyExecuted
- OrderCancelled
- SettlementCompleted

## Quant

- RiskAssessmentRequested
- RiskAssessmentCompleted
- RiskLimitBreached
- StressTestCompleted
- PortfolioRiskRecalculated

## Intelligence

- ReportGenerated
- ComplianceAlertRaised
- BatchCompleted

## Event rules

Events represent facts.

They are not commands.

Every event should contain:

- eventId
- eventType
- version
- occurredAt
- correlationId
- causationId
- producer
- aggregateId
- payload

Consumers must be idempotent.