# FINCORE — Bounded Contexts

FINCORE is divided into seven bounded contexts.

## 1. Edge Context

Service:

- edge-service

Responsibilities:

- API Gateway
- routing
- authentication enforcement
- authorization
- rate limiting
- correlation ID
- resilience
- identity propagation

The Edge Context contains no financial business logic.

---

## 2. Identity Context

Service:

- identity-service

Responsibilities:

- users
- roles
- permissions
- authentication
- JWT
- refresh tokens
- account locking
- security events

The Identity Context owns the identity lifecycle.

It does not own:

- portfolios
- accounts
- orders
- risk calculations

---

## 3. Wealth Context

Service:

- wealth-service

Responsibilities:

- customers
- investor profiles
- portfolios
- positions
- allocation
- performance
- net worth

The Wealth Context owns the investment relationship with the customer.

---

## 4. Ledger Context

Service:

- ledger-service

Responsibilities:

- financial accounts
- ledger
- ledger entries
- financial transactions
- balances
- settlements
- double-entry accounting
- idempotency

The Ledger Context is the source of truth for financial movements.

---

## 5. Execution Context

Service:

- execution-service

Responsibilities:

- assets
- orders
- executions
- execution reports
- order lifecycle
- market sessions

The Execution Context owns the trading order lifecycle.

---

## 6. Quant Context

Service:

- quant-service

Responsibilities:

- risk
- VaR
- Expected Shortfall
- volatility
- Sharpe ratio
- beta
- correlation
- drawdown
- stress testing
- Monte Carlo
- portfolio optimization

---

## 7. Intelligence Context

Service:

- intelligence-service

Responsibilities:

- financial reports
- compliance
- historical analytics
- regulatory exports
- batch processing

The Intelligence Context consumes information from other contexts but does not become the owner of their business data.