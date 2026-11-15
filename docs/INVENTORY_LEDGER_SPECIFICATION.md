# SupplyGraph — Double-Entry Inventory Ledger Specification

**Document ID:** `SG-SPEC-002`  
**Phase:** Phase 2 (Digital Twin Core & Graph Domain Model)  
**Status:** Approved Engineering Specification  

---

## 1. Principles of Double-Entry Inventory Accounting

SupplyGraph implements strict double-entry bookkeeping for physical inventory positions. Direct mutation of inventory balances without an auditable, balanced ledger transaction is impossible.

### 1.1 The Invariant of Conservation
For every transaction $T$ composed of postings $P_1, P_2, \dots, P_k$:
$$\sum_{P \in \text{Debits}} P.\text{quantity} = \sum_{P \in \text{Credits}} P.\text{quantity}$$
Physical inventory cannot be created from nothing or vanish without an explicit offsetting account.

### 1.2 Account Identification
An `InventoryAccount` is uniquely keyed by:
$$\text{AccountId} = \langle \text{NodeId}, \text{MaterialId}, \text{BalanceCategory}, \text{Optional}\langle\text{LotNumber}\rangle \rangle$$

---

## 2. Balance Categories & Valid Transitions

| Category | Description | Permitted Source Transitions |
| :--- | :--- | :--- |
| `ON_HAND_AVAILABLE` | Unrestricted stock ready for production or dispatch. | $\to$ `RESERVED_PRODUCTION`, `RESERVED_SHIPMENT`, `QUARANTINED`, `IN_TRANSIT` |
| `RESERVED_PRODUCTION` | Stock allocated to an approved manufacturing order. | $\to$ `ON_HAND_AVAILABLE` (release), `EXTERNAL_OFFSET` (consumed) |
| `RESERVED_SHIPMENT` | Stock staged for outbound transport loading. | $\to$ `ON_HAND_AVAILABLE` (unstage), `IN_TRANSIT` (dispatched) |
| `IN_TRANSIT` | Stock aboard an active transportation lane. | $\to$ `ON_HAND_AVAILABLE` (received at destination), `QUARANTINED` (damaged in transit) |
| `QUARANTINED` | Stock segregated due to quality excursion, contamination, or expiration. | $\to$ `ON_HAND_AVAILABLE` (cleared by QA), `EXTERNAL_OFFSET` (scrapped) |
| `EXTERNAL_OFFSET` | Balancing account for external receipts (PO receipt) and final consumption. | Offset leg for inbound purchases and external shipments. |

---

## 3. Invariants & Rules

1. **Non-Negative Balance Rule:**
   $$\forall A \notin \{\text{EXTERNAL\_OFFSET}\}, \quad \text{Balance}(A) \ge 0$$
   Any transaction that would cause an internal account to have a negative balance throws `IllegalStateException("Insufficient inventory")` and aborts atomically.
2. **Lot Traceability & Expiration Enforcement:**
   - Every physical lot carries `LotNumber`, `MaterialId`, `manufacturedAt`, and `Optional<ExpirationDate>`.
   - Any transaction attempting to post an expired lot into `ON_HAND_AVAILABLE` is rejected. Expired lots must be directed to `QUARANTINED`.
3. **Explicit Reservation Lifecycles:**
   - Reserving inventory transfers quantity from `ON_HAND_AVAILABLE` to `RESERVED_*`.
   - Releasing inventory transfers quantity back from `RESERVED_*` to `ON_HAND_AVAILABLE`.
   - Fulfilling inventory transfers quantity from `RESERVED_*` to consumption offset.
4. **Idempotent Transaction Posting:**
   - Each transaction carries a unique `TransactionId`.
   - If an already-applied `TransactionId` is submitted again, the ledger returns `false` without applying duplicate postings.

