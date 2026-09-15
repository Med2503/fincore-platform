# FINCORE — Service Ownership

| Service | Owns | Does not own |
|---|---|---|
| edge-service | API access | Business data |
| identity-service | Identity and security | Portfolio, account, order |
| wealth-service | Customer and portfolio | Ledger balance |
| ledger-service | Financial movements | Portfolio |
| execution-service | Orders and executions | Account balance |
| quant-service | Risk calculations | Orders |
| intelligence-service | Reports and compliance | Source-of-truth financial data |

## Ownership rule

Each business concept has one authoritative owner.

Other services may maintain:

- local projections
- read models
- snapshots
- cached representations

but they must not modify another service's source of truth.