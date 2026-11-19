---
project: "Pay Raise Module"
context_type: greenfield
created: 2026-10-10
updated: 2026-10-10
timeline_budget:
  mvp_weeks: 3
  hard_deadline: 2026-12-06
  after_hours_only: true
product_type: web-app
target_scale:
  users: medium
  qps: low
  data_volume: small
checkpoint:
  current_phase: 8
  phases_completed: [1, 2, 3, 4, 5, 6, 7]
  gray_areas_resolved:
    - topic: "pain category"
      decision: "Combination of workflow friction, decision paralysis, coordination overhead, and data trapped across tools (email/conversations)."
    - topic: "insight"
      decision: "Lack of visibility into decision history and salary bands causes different managers to treat similar raise requests inconsistently."
    - topic: "primary persona scope"
      decision: "Line manager (przełożony) within a single organization, managing raise/promotion requests from their direct reports."
    - topic: "auth model"
      decision: "Login (email + password / OAuth), role-based: Employee, Manager (przełożony), HR, CEO (prezes)."
    - topic: "approval flow reversibility"
      decision: "A rejection at any stage can instead be sent back to the employee with a comment (e.g. to add completed goals) rather than only terminating the request."
    - topic: "MVP scope cut"
      decision: "Full approval chain (manager → HR → CEO) was too large for a 3-week after-hours MVP. First version is scoped to the employee → manager leg only; HR and CEO stages are deferred."
  frs_drafted: 9
  quality_check_status: accepted
---

## Vision & Problem Statement

A line manager (przełożony) evaluating an employee's raise or promotion request today has no structured way to see the request's justification alongside the employee's prior raise history and the salary band for their position — everything lives in scattered emails and conversations, and the request must still pass through a multi-step approval chain (manager → HR → CEO) coordinated manually. This costs the manager time piecing together context and creates risk of inconsistent decisions across similar cases.

The insight: inconsistent raise decisions aren't usually a judgment problem — they're a visibility problem. Without a shared, structured view of salary bands and each employee's request history, different managers end up treating comparable cases differently, and nobody notices until it becomes a fairness complaint.

At 100x the current scale, the approval-routing rule would need to account for multiple departments and different approval paths per department — a single fixed manager→HR→CEO sequence stops being sufficient.

## User & Persona

**Primary persona**: Line manager (przełożony) in a single organization, responsible for a team of direct reports. They reach for this product when an employee submits a raise/promotion request, and need to review the employee's justification, compare it against the position's salary band, see the employee's prior request history, and move the request forward to HR and ultimately the CEO for final approval.

## Access Control

Login required (email + password or OAuth). Role-based access with four roles:

- **Employee** — submits a raise/promotion request with justification (achievements, completed projects); sees the status and history of their own requests.
- **Manager (przełożony)** — sees requests from their direct reports, the salary band for each report's position, and each report's prior request history; approves, rejects, or sends the request back to the employee with a comment as the first approval stage.
- **HR** — sees requests approved by the manager; approves, rejects, or sends back as the second approval stage; owns the salary bands per position.
- **CEO (prezes)** — sees requests approved by HR; gives the final approval, rejects, or sends back.

A rejection at any stage can instead be a "send back" to the employee (or previous stage) with a comment — e.g. asking the employee to document additional completed goals — rather than only ending the request outright.

## Success Criteria

### Primary
- An employee can submit a raise request (justification + requested amount) and their manager can see it alongside the position's salary band, decide whether it fits within the band or requires a promotion to a different band, record their own justification, and approve, reject, or send the request back — end to end, for the employee → manager leg.

### Secondary
- Users (employee and manager) receive a notification when a request's status changes (submitted, decided, sent back).

### Guardrails
- Compensation data (salary bands, requested amounts, approval history) must remain visible only to the roles authorized to see it — a privacy breach of pay data is a regression even if the approval flow otherwise works.

## Functional Requirements

### Request submission
- FR-001: Employee can submit a raise/promotion request with a written justification and a requested amount. Priority: must-have
  > Socratic: Counter-argument considered: "a stated amount could anchor the manager's evaluation instead of it resting on the salary band and merit." Resolution: kept as written — the manager sees the band alongside the request, so the anchor is visible and checkable, not hidden.
- FR-002: System limits an employee to at most one *completed* (approved or finally rejected) raise request per year; a request sent back for revision does not consume the yearly limit. Priority: must-have
  > Socratic: Counter-argument considered: "a rigid yearly cooldown could block urgent legitimate cases, and counting every submission — including rejected/sent-back attempts — unfairly penalizes the employee." Resolution: changed — only a completed (approved or finally rejected) request counts against the yearly limit; a send-back does not.
- FR-003: Employee can view the full status and history of their own requests, including the manager's written justification. Priority: must-have
  > Socratic: Counter-argument considered: "full history with the manager's comments could expose internal evaluation language not written with the employee as the intended reader." Resolution: kept — the employee sees everything the manager wrote about their own request.

### Manager review and decision
- FR-004: Manager can view a pending request from a direct report along with the salary band for that report's position and where the employee currently sits within that band. Priority: must-have
  > Socratic: Counter-argument considered: "a raw band without context could encourage the manager to anchor low regardless of merit." Resolution: changed — the view also shows the employee's current position within the band, not just the band's min/max.
- FR-005: Manager can view a direct report's full history of prior raise requests. Priority: must-have
  > Socratic: Counter-argument considered: "full history could bias the manager against a repeat requester instead of judging the current request on its own merits." Resolution: kept — visibility into history is the core value proposition; the bias risk is lower than the cost of the blindness this product exists to fix.
- FR-006: Manager can record their own written justification when deciding on a request. Priority: must-have
  > Socratic: Counter-argument considered: "a mandatory justification could become a box-ticking formality (one word) without improving decision quality." Resolution: kept as written — a weak justification still creates an audit trail; enforcing content quality is a culture issue, not something the MVP should police.
- FR-007: Manager can mark whether the request fits within the current position's salary band or requires a promotion to a different position/band. Priority: must-have
  > Socratic: Counter-argument considered: "a manager unilaterally deciding promotion vs. raise oversteps, since there's no HR/CEO check yet in the MVP." Resolution: kept — in the MVP this is only a proposal/label the manager attaches; it is not a final position-change approval (that authority stays out of scope until HR/CEO stages exist).
- FR-008: Manager can approve, reject, or send the request back to the employee with a comment. Priority: must-have
  > Socratic: Counter-argument considered: "without a decision deadline, a request could sit indefinitely in limbo — exactly the visibility problem the product exists to solve." Resolution: kept without an SLA for the MVP; a response-time guarantee is out of scope for now (see Non-Goals).

### Notifications
- FR-009: User (employee and manager) receives a notification when a request's status changes. Priority: nice-to-have
  > Socratic: Counter-argument considered: "for a manually-tested MVP, notifications may be unnecessary overhead since both sides can just check status in the app." Resolution: kept as nice-to-have — built only if the 3-week budget allows; dropping it does not affect the Primary success criterion.

## User Stories

### US-01: Employee requests a raise and manager decides

- **Given** an employee who has not submitted a raise request in the past year
- **When** they submit a request with a justification and requested amount
- **Then** their manager sees the request alongside the position's salary band and the employee's prior request history, and can approve, reject, or send it back with a comment

#### Acceptance Criteria
- A second request within the same year is blocked only if the employee has a completed (approved or finally rejected) request in that period; a request currently sent back for revision does not block a new submission.
- The manager's view shows the salary band for the employee's current position, and where the employee sits within it, before a decision is made.
- A "send back" decision requires a comment and returns the request to the employee in an editable state.
- An approved or rejected decision requires the manager's written justification before it can be submitted.

## Business Logic

Based on the organization's structure, the employee's salary history, and company rules, the application automatically determines who must approve or reject a raise request and in what order, and routes the request through that sequence until a final decision is reached.

The rule consumes user-facing inputs: who the employee reports to (their manager), and the employee's position and its salary band. The output is an ordered sequence of named approval stages the request must pass through, ending in a final decision. The employee (and each approver, when it's their turn) encounters this as a progress bar showing the named stages and which one the request currently sits at.

## Non-Functional Requirements

- Compensation data (salary bands, requested/approved amounts, request history, and decision justifications) is never visible to a role not authorized for that specific request — no role can see pay data outside their defined access scope.

## Non-Goals

- **No AI-assisted drafting of manager justifications.** Idea noted for a later version; the MVP's approval decisions and their written justifications are entirely human-authored.
- **No HR or CEO approval stages.** The MVP covers only the employee → manager leg of the approval chain; routing beyond the manager is deferred to a later version.
- **No SLA or deadline enforcement on manager decisions.** No forced response time, reminders, or escalation if a request sits undecided.
- **No multi-department or configurable approval paths.** A single fixed approval sequence applies to everyone in the MVP; per-department routing variation is out of scope.

## Quality cross-check

All items present: Access Control, Business Logic (one-sentence rule), Project artifacts, Timeline-cost acknowledgment (3-week MVP, scoped down to fit), Non-Goals (4 entries). No gaps to flag.
