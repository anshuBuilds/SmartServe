# SmartServe Restaurant Order Workflow Requirements

Last updated: 2026-07-10

## 1. Purpose

SmartServe should support a reliable restaurant ordering workflow from guest menu browsing through kitchen preparation, service handoff, payment, notifications, and reporting. The system must work for QR table ordering, staff-created orders, kitchen display workflows, and manager oversight across restaurant branches.

This document defines production-grade requirements for actors, permissions, order states, edge cases, and a phased delivery roadmap. It uses the current SmartServe implementation as the baseline and extends it toward a more complete restaurant operations platform.

## 2. Research Notes

Modern restaurant systems separate the commercial order record from fulfillment progress. Square's Orders API models an order as the full purchase record, including line items, tenders, refunds, and returns, and exposes order states such as `OPEN`, `COMPLETED`, `CANCELED`, and `DRAFT`. Square also models fulfillment separately with states such as `PROPOSED`, `RESERVED`, `PREPARED`, `COMPLETED`, `CANCELED`, and `FAILED`.

Restaurant POS products commonly include menu management, online/QR ordering, kitchen display system integration, table management, offline behavior, analytics, and payment/refund handling. Recent POS comparisons highlight table management, inventory, reporting, online ordering, and kitchen display integration as expected restaurant features, not extras.

Self-ordering research also warns that kiosk or QR flows can become manipulative or confusing when they hide pricing, add unnecessary steps, or use aggressive upselling. SmartServe should keep guest ordering clear: visible prices, explicit modifiers, transparent totals, understandable availability, and no dark-pattern defaults.

References:

- Square Order object: https://developer.squareup.com/reference/square/objects/Order
- Square OrderState enum: https://developer.squareup.com/reference/square/enums/OrderState
- Square FulfillmentState enum: https://developer.squareup.com/reference/square/enums/FulfillmentState
- TechRadar restaurant POS guide, 2026: https://www.techradar.com/news/the-best-pos-system-for-restaurants
- "Deception by Design" self-ordering kiosk audit, 2026: https://arxiv.org/abs/2603.03218

## 3. Current SmartServe Baseline

SmartServe currently contains these domain areas:

- Restaurant hierarchy: restaurants, branches, and restaurant tables.
- Users and roles: `ADMIN`, `MANAGER`, `WAITER`, `KITCHEN`.
- Menu: categories, items, price, availability, food type, spice level, preparation time.
- Guest QR flow: table session, available menu, guest order creation, tracking token.
- Orders: `DINE_IN` and `TAKEAWAY`.
- Order states: `PENDING`, `PREPARING`, `READY`, `SERVED`, `CANCELLED`.
- Payment states: `NOT_REQUIRED`, `PENDING`, `PAID`, `FAILED`.
- Table states: `AVAILABLE`, `OCCUPIED`, `RESERVED`, `OUT_OF_SERVICE`.
- Kitchen queue: active tickets are `PENDING`, `PREPARING`, `READY`.
- Notifications: order created, ready, served, cancelled.
- Analytics: sales summary, top items, order status counts, table performance.

Current workflow:

1. Guest or staff creates an order.
2. Dine-in orders require an available table; takeaway orders require a customer phone.
3. Table becomes `OCCUPIED` when a dine-in order is created.
4. Kitchen starts a `PENDING` order and moves it to `PREPARING`.
5. Kitchen marks a `PREPARING` order as `READY`.
6. Waiter or manager marks a `READY` order as `SERVED`.
7. Table is released to `AVAILABLE` when the order is `SERVED` or `CANCELLED`.

## 4. Goals

SmartServe must:

- Reduce ordering mistakes by validating table, menu, modifier, availability, and payment rules before kitchen work starts.
- Give the kitchen a clear, low-friction queue with item-level preparation visibility.
- Give waiters a reliable handoff flow for ready food, served orders, and table release.
- Give managers controlled override powers with full audit history.
- Give guests transparent ordering, status tracking, and notifications.
- Support multi-branch operations without leaking data across branches.
- Preserve a trustworthy source of truth for revenue, refunds, cancellations, and analytics.

## 5. Non-Goals for the Initial Production Scope

These should not block the core workflow:

- Full third-party delivery marketplace integration.
- Loyalty and coupons beyond basic discounts.
- Complex inventory recipe deduction.
- Split bills and item-level payment allocation.
- Reservations beyond table status support.
- Driver dispatch.
- Offline-first sync conflict resolution.

## 6. Actors

### Guest

The guest scans a table QR code, browses the menu, creates a dine-in order, receives order status updates, and can track the order using a private tracking token.

### Waiter

The waiter creates staff-assisted orders, views orders for assigned or branch tables, marks ready orders as served, handles guest clarifications, and requests manager help for cancellations, voids, discounts, or payment exceptions.

### Kitchen Staff

Kitchen staff view the kitchen queue, start preparation, mark tickets or items ready, flag unavailable items, and add kitchen notes. They should not see unnecessary customer personal data or financial admin controls.

### Manager

The manager oversees branch operations, manages menu availability, updates order status, approves cancellations after preparation starts, handles refunds/voids, reopens exceptional orders, edits table status, and views branch reports.

### Admin

The admin manages restaurants, branches, users, global configuration, cross-branch reporting, and high-risk settings. Admins can perform manager actions across branches.

### System

The system validates state transitions, locks tables during order creation, calculates totals, sends notifications, masks private data, records audit events, and protects branch access.

### Payment Provider

The provider authorizes, captures, fails, refunds, or reverses payments. SmartServe should treat provider callbacks as asynchronous events that may arrive late or be retried.

## 7. Core Order Concepts

### Order

The commercial record: customer, branch, table, items, totals, taxes, discounts, payment status, source, and audit history.

### Ticket/Fulfillment

The kitchen/service progress record: when preparation starts, which station owns the item, when food is ready, who handed it off, and whether fulfillment failed or was cancelled.

### Table Session

The dine-in context created by QR scan or waiter seating. A table may have one active ordering session, but the session may include multiple order rounds if the product supports add-on orders later.

### Payment

The financial record: amount due, amount paid, provider reference, status, failure reason, refunds, and settlement metadata.

## 8. Recommended Production State Model

SmartServe should keep separate state fields for order, kitchen fulfillment, payment, and table. This avoids mixing "food is ready" with "bill is paid" or "table is occupied".

### Order State

| State | Meaning | Terminal |
| --- | --- | --- |
| `DRAFT` | Cart or staff order is being assembled; no kitchen work. | No |
| `PLACED` | Guest/staff submitted the order; validation passed. | No |
| `ACCEPTED` | Restaurant accepted the order and committed it to kitchen workflow. | No |
| `IN_PREPARATION` | At least one item is being prepared. | No |
| `READY_FOR_SERVICE` | All required kitchen items are ready for handoff. | No |
| `SERVED` | Dine-in order reached the guest, or takeaway was handed over. | No |
| `COMPLETED` | Operationally complete and payment obligations are satisfied or not required. | Yes |
| `CANCELLED` | Order was intentionally cancelled before completion. | Yes |
| `FAILED` | Order could not be completed because of system/payment/kitchen failure. | Yes |

Mapping from current SmartServe:

- `PENDING` -> `PLACED` or `ACCEPTED`
- `PREPARING` -> `IN_PREPARATION`
- `READY` -> `READY_FOR_SERVICE`
- `SERVED` -> `SERVED`
- `CANCELLED` -> `CANCELLED`

### Fulfillment State

| State | Meaning |
| --- | --- |
| `QUEUED` | Ticket is visible to kitchen but not started. |
| `STARTED` | Kitchen has started preparation. |
| `PARTIALLY_READY` | Some items are ready, but not the whole ticket. |
| `READY` | Ticket is ready for waiter/customer pickup. |
| `HANDED_OFF` | Food was served or takeaway was collected. |
| `CANCELLED` | Fulfillment was cancelled intentionally. |
| `FAILED` | Fulfillment failed without a clean cancellation. |

### Payment State

| State | Meaning |
| --- | --- |
| `NOT_REQUIRED` | No online payment is needed, for example pay-at-counter mode. |
| `PENDING` | Payment expected but not yet authorized or captured. |
| `AUTHORIZED` | Funds authorized, capture pending. |
| `PAID` | Payment captured. |
| `PARTIALLY_REFUNDED` | Some money returned. |
| `REFUNDED` | Full money returned. |
| `FAILED` | Payment failed. |
| `VOIDED` | Authorization or unpaid order voided. |

### Table State

| State | Meaning |
| --- | --- |
| `AVAILABLE` | Can accept a new dine-in session. |
| `RESERVED` | Held for a future or current booking. |
| `SEATED` | Guests are seated but no order placed yet. |
| `ORDERING` | Active QR/staff order being assembled. |
| `OCCUPIED` | Active order exists. |
| `NEEDS_SERVICE` | Guest or kitchen needs waiter attention. |
| `BILL_REQUESTED` | Guest is done and wants checkout. |
| `CLEANING` | Guest left; table must be reset. |
| `OUT_OF_SERVICE` | Not usable. |

## 9. Allowed State Transitions

### Normal Dine-In Flow

1. Table: `AVAILABLE -> SEATED` when waiter seats guests or guest scans QR.
2. Order: `DRAFT -> PLACED` when guest submits.
3. Order: `PLACED -> ACCEPTED` after validation and restaurant acceptance.
4. Fulfillment: `QUEUED -> STARTED` when kitchen starts.
5. Order: `ACCEPTED -> IN_PREPARATION`.
6. Fulfillment: `STARTED -> READY`; order becomes `READY_FOR_SERVICE`.
7. Order: `READY_FOR_SERVICE -> SERVED` when waiter confirms delivery.
8. Payment: `PENDING/AUTHORIZED -> PAID` when paid.
9. Order: `SERVED -> COMPLETED` after payment is satisfied.
10. Table: `OCCUPIED -> BILL_REQUESTED -> CLEANING -> AVAILABLE`.

### Normal Takeaway Flow

1. Order: `DRAFT -> PLACED`.
2. Payment: optional `PENDING -> PAID` before kitchen acceptance, depending on branch policy.
3. Order: `PLACED -> ACCEPTED -> IN_PREPARATION -> READY_FOR_SERVICE`.
4. Fulfillment: `READY -> HANDED_OFF`.
5. Order: `SERVED -> COMPLETED`.

### Cancellation Rules

- Guests may cancel only while the order is `DRAFT` or `PLACED`, if branch policy allows it.
- Waiters may request cancellation for `PLACED` or `ACCEPTED`.
- Kitchen may reject or flag an order before preparation if an item cannot be made.
- Managers may cancel `PLACED`, `ACCEPTED`, or `IN_PREPARATION`.
- After `READY_FOR_SERVICE`, cancellation requires manager approval and must record reason and compensation action.
- `COMPLETED` orders cannot be cancelled; they require refund/return adjustment records.

### Reopen Rules

- Reopening should be rare and manager-only.
- `READY_FOR_SERVICE -> IN_PREPARATION` is allowed only when kitchen marks a correction or remake.
- `SERVED -> READY_FOR_SERVICE` is allowed only for mistaken service confirmation.
- `COMPLETED` should not reopen; create an adjustment, refund, or replacement order instead.

## 10. Permissions

| Capability | Guest | Waiter | Kitchen | Manager | Admin |
| --- | --- | --- | --- | --- | --- |
| Browse active menu | Yes | Yes | Yes | Yes | Yes |
| Create QR dine-in order | Yes | Yes | No | Yes | Yes |
| Create staff order | No | Yes | No | Yes | Yes |
| Create takeaway order | No | Yes | No | Yes | Yes |
| View own tracked order | Yes | No | No | No | No |
| View branch orders | No | Yes | Kitchen tickets only | Yes | Yes |
| Start preparation | No | No | Yes | Yes | Yes |
| Mark order ready | No | No | Yes | Yes | Yes |
| Mark served/handoff | No | Yes | No | Yes | Yes |
| Cancel before prep | Limited | Request/allowed by policy | No | Yes | Yes |
| Cancel during prep | No | Request | Request | Yes | Yes |
| Refund/void payment | No | No | No | Yes | Yes |
| Edit menu availability | No | No | Item outage only | Yes | Yes |
| Manage restaurants/branches | No | No | No | Branch only | Yes |
| Manage users | No | No | No | Branch users | Yes |
| View analytics | No | Limited | Kitchen metrics | Branch | All |
| Override branch access | No | No | No | No | Yes |

Permission requirements:

- Every staff action must be scoped to the user's branch unless the user is an admin.
- Kitchen users should see customer name only if operationally needed; phone numbers should stay masked.
- Guest tracking tokens must be high-entropy, unguessable, and revocable.
- Manager/admin overrides must write audit events with actor, reason, previous state, new state, and timestamp.

## 11. Functional Requirements

### Guest Ordering

- QR session must validate table token, branch active status, and table availability.
- Guest menu must show only active categories and available items.
- Guest must see item name, price, dietary metadata, spice level, availability, estimated prep time when available, and required modifiers when implemented.
- Guest must provide a name.
- Phone number is required when SMS/WhatsApp notifications are requested or when takeaway is enabled for guest flow.
- Guest must explicitly choose notification consent; consent must not default to yes.
- Guest must see total before submission.
- Submission must be idempotent to prevent duplicate orders on double tap or retry.
- Guest tracking must show status without exposing staff-only data.

### Staff Ordering

- Staff can create dine-in or takeaway orders.
- Dine-in orders require branch and available table.
- Takeaway orders must not include table.
- Staff can add special instructions.
- Staff can see validation errors before order submission.
- Staff can reprint or resend ticket notifications.

### Menu and Availability

- Unavailable items cannot be ordered.
- If an item becomes unavailable while a guest has it in cart, submission must fail with a clear message and updated availability.
- Price must be snapshotted onto the order item at submission time.
- Order totals must use snapshotted line item prices, not live menu prices.
- Future modifier support must snapshot modifier names, prices, and kitchen notes.

### Kitchen Queue

- Kitchen queue must show active tickets ordered by priority and creation time.
- Kitchen can filter by status and station when station support exists.
- Kitchen can start tickets, mark items ready, mark full ticket ready, and flag issues.
- Queue counts must update in near real time.
- Prep start and ready timestamps must be recorded.
- Kitchen history must be paginated and bounded by date range.

### Service Handoff

- Waiters must see ready orders by branch and table.
- Waiter marks served only when food reaches guest or takeaway handoff is complete.
- Served action should capture actor and timestamp.
- Table should not become available immediately if payment or cleaning is still pending in the production model.

### Payment

- Branch must define payment policy: pay-later, pay-before-kitchen, pay-at-counter, or online optional.
- Payment status must be independent from kitchen status.
- Payment callbacks must be idempotent.
- Failed payment must not create duplicate orders.
- Refunds and voids require manager/admin permission and reason.
- Financial reports must use payment records, not only order status.

### Notifications

- Notify kitchen when order is created or accepted.
- Notify guest when order is ready if consent and contact channel exist.
- Notify waiter when order is ready for service.
- Notify manager when cancellation/refund/failed payment needs approval.
- Notifications must be retryable and store delivery status.

### Analytics

- Track order volume by status, branch, channel, and time.
- Track average time from placed to started, started to ready, ready to served, and served to completed.
- Track cancellations by reason and actor.
- Track top menu items and unavailable-item frequency.
- Track table turnover time.
- Track payment success/failure/refund totals.

## 12. Edge Cases

### Duplicate Submission

If a guest taps submit twice or the network retries, SmartServe must return the original order for the same idempotency key instead of creating another kitchen ticket.

### Table Race Condition

If two guests scan the same QR or a waiter creates an order at the same time, table locking must allow only one active dine-in session/order creation path.

### Table Already Occupied

If table status is not orderable, guest session should explain that ordering is unavailable and staff should be contacted.

### Item Sold Out During Checkout

If an item becomes unavailable after cart load, order submission must reject only the affected item and allow the guest/staff to revise.

### Price Changed During Checkout

If price changes before submission, SmartServe should show the updated total and require confirmation. After submission, order item prices remain fixed.

### Kitchen Cannot Prepare Item

Kitchen flags the ticket; manager or waiter resolves by substituting item, cancelling item, or cancelling order. Guest-facing status should not expose internal blame.

### Partial Readiness

For multi-item orders, kitchen may mark individual items ready. Full order becomes ready only when all required items are ready or manager splits the ticket.

### Guest Requests Add-On Items

Production design should support multiple orders per table session rather than editing an already-preparing order. This keeps kitchen and payment audit clean.

### Order Modification After Preparation Starts

Changes after `IN_PREPARATION` require manager approval or a new add-on order. Never silently mutate a kitchen ticket already being prepared.

### Cancellation After Payment

Cancellation must trigger refund/void workflow if payment exists. Order state and payment state must both be updated.

### Payment Succeeds But App Times Out

Payment callback should reconcile the order by provider reference. Guest/staff retry must not charge twice.

### Payment Fails After Order Placed

Branch policy decides whether the order remains accepted for pay-at-counter or is blocked from kitchen acceptance.

### Notification Failure

Order state must not depend on notification success. Failed notifications should retry and show staff-visible warning if important.

### Branch Disabled Mid-Session

Existing carts may browse but cannot submit. Staff/admin should see a clear branch-disabled reason.

### QR Token Leaked

QR token should identify table session only. Tracking token should be separate. Admin/manager must be able to rotate table QR tokens.

### Out-of-Service Table

Guests cannot order; staff can only override with manager permission.

### Served By Mistake

Manager can correct `SERVED -> READY_FOR_SERVICE` within a short configured window, with audit reason.

### Completed Order Dispute

Use refund/replacement records. Do not mutate completed order lines except through explicit adjustment entities.

## 13. Data Requirements

Minimum production entities or fields:

- Order: source, state, branch, table/session, customer display name, masked contact, totals, tax, discount, notes, idempotency key.
- Order item: menu item reference, snapshotted name, price, quantity, modifiers, item status, kitchen station, prep timestamps.
- Fulfillment/ticket: order reference, status, assigned station/user, priority, timestamps.
- Payment: provider, amount, currency, status, authorization ID, capture ID, failure reason.
- Refund/void: payment reference, amount, reason, actor, timestamp.
- Audit event: entity type, entity ID, actor, action, previous value, new value, reason, timestamp.
- Notification: channel, recipient, consent basis, template, status, attempts, last error.
- Table session: table, status, opened by, opened at, closed at, tracking relationship.

## 14. API Requirements

Recommended order APIs:

- `POST /api/orders` create staff order with idempotency key.
- `GET /api/orders/{id}` get order.
- `GET /api/branches/{branchId}/orders` list branch orders by status/date/type.
- `POST /api/orders/{id}/accept` manager/waiter policy-dependent.
- `POST /api/orders/{id}/cancel` with reason.
- `POST /api/orders/{id}/serve` waiter/manager.
- `POST /api/orders/{id}/complete` system/manager after payment policy is satisfied.

Recommended kitchen APIs:

- `GET /api/branches/{branchId}/kitchen/queue`
- `POST /api/branches/{branchId}/kitchen/orders/{id}/start`
- `POST /api/branches/{branchId}/kitchen/orders/{id}/items/{itemId}/ready`
- `POST /api/branches/{branchId}/kitchen/orders/{id}/ready`
- `POST /api/branches/{branchId}/kitchen/orders/{id}/issue`
- `GET /api/branches/{branchId}/kitchen/history`

Recommended guest APIs:

- `GET /api/guest/session/{qrToken}`
- `GET /api/guest/session/{qrToken}/menu`
- `POST /api/guest/session/{qrToken}/orders`
- `GET /api/guest/orders/{trackingToken}`

Recommended payment APIs:

- `POST /api/orders/{id}/payments`
- `POST /api/payments/webhooks/{provider}`
- `POST /api/orders/{id}/refunds`

## 15. Non-Functional Requirements

- Security: JWT staff auth, branch scoping, role checks, masked personal data, high-entropy guest tokens.
- Reliability: transactional order creation, pessimistic locking for tables, idempotency for order creation and payment callbacks.
- Performance: kitchen queue under 500 ms for active branch load; guest menu under 1 second for normal branch catalog.
- Auditability: all high-risk actions must be traceable.
- Privacy: store only required customer contact data; honor consent choices.
- Accessibility: guest QR menu must support keyboard navigation, readable contrast, clear error messages, and non-manipulative totals.
- Observability: log transition failures, payment mismatches, notification failures, and queue latency.
- Scalability: branch-level data partitioning in queries; avoid loading cross-branch order history by default.

## 16. Acceptance Criteria

Core workflow is production-ready when:

- A guest can scan, browse, order, track, and receive a ready notification.
- A waiter can create staff orders and mark ready orders served.
- Kitchen can process a queue without seeing unrelated branch tickets.
- Manager can cancel/refund with reason and audit history.
- Table locking prevents duplicate active dine-in orders for the same table.
- Sold-out and price-change scenarios are handled before order submission.
- Payment failure, duplicate callbacks, and notification failure do not corrupt order state.
- Analytics match order/payment records for a selected branch and date range.
- Automated tests cover normal flow, invalid transitions, branch access, duplicate submission, and cancellation/payment edge cases.

## 17. Phased Roadmap

### Phase 0: Stabilize Current Workflow

- Document current state transitions and permissions.
- Fix any guest tracking inefficiencies and simplify tracking token retrieval.
- Add idempotency key support to guest and staff order creation.
- Add audit events for status transitions.
- Add tests for table locking, duplicate order prevention, and branch isolation.

### Phase 1: Production Kitchen and Service Flow

- Split order state from fulfillment state internally.
- Add item-level readiness.
- Add waiter ready-order handoff view.
- Add cancellation reasons and manager approval rules.
- Improve kitchen queue real-time refresh.
- Add preparation SLA metrics.

### Phase 2: Payments and Financial Integrity

- Add payment entity and provider references.
- Implement online payment lifecycle: pending, authorized, paid, failed.
- Add refund and void workflows with manager approval.
- Reconcile payment webhooks idempotently.
- Update analytics to use payment records.

### Phase 3: Table Sessions and Multi-Round Dining

- Add table session entity.
- Allow multiple orders under one active table session.
- Add bill requested and cleaning table states.
- Add session-level guest tracking.
- Support add-on orders without mutating active kitchen tickets.

### Phase 4: Manager Operations and Analytics

- Add manager dashboard for exceptions, cancellations, refunds, and queue delays.
- Add branch-level configuration for payment policy, cancellation windows, and notification channels.
- Add deeper analytics for table turnover, prep times, revenue, refunds, and item availability.
- Add exportable reports.

### Phase 5: Advanced Restaurant Features

- Modifiers and combos.
- Station routing.
- Inventory alerts.
- Reservations/waitlist.
- Promotions and loyalty.
- Third-party delivery integration.
- Offline-tolerant staff ordering.

## 18. Beginner Pitfalls to Avoid

- Do not use one status field for everything. Kitchen progress, payment, and table occupancy are different lifecycles.
- Do not edit submitted order prices when menu prices change. Snapshot prices on the order.
- Do not trust the frontend to prevent duplicate orders. Use server-side idempotency.
- Do not make payment success the only source of truth. Payment providers send late and repeated events.
- Do not release a table just because food was served if payment or cleaning still matters.
- Do not let manager overrides happen silently. Audit them.
- Do not expose phone numbers or tracking credentials in broad staff views.
- Do not mutate completed orders. Use refunds, adjustments, or replacement orders.
- Do not build add-on dining by reopening old kitchen tickets. Create separate order rounds under the same table session.
- Do not let branch users query data across branches accidentally.
