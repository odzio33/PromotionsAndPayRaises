# Plan Brief: Minimal auth scaffold

Full plan: [`plan.md`](./plan.md)

## Overview

Today, every backend endpoint trusts whatever `employeeId`/`managerId` is in the URL — there is no login at all. This change adds a minimal, session-based login for the two MVP roles (Employee, Manager), moves every endpoint to derive identity from the authenticated session instead of the URL, and ships a bare-functional login screen in the frontend. It is roadmap Foundation **F-01**, and unlocks every user-facing slice (S-01, S-02, S-03) in `context/foundation/roadmap.md`.

## Starting Point

- `Employee` has `email`/`fullName`/`role`/`manager`/`position` already, but no password field — there's no credential to check today.
- `RaiseRequestController` explicitly comments that it reads IDs from the URL "because authentication is not wired up yet" — the gap is named in the code itself.
- The authorization logic that matters ("is this manager actually assigned to this employee?") already exists and is already correct, in `RaiseRequestService.requireManagedBy` — this change doesn't touch it, only fixes where its input comes from.
- Zero existing tests authenticate; all 5 FR-001...FR-008 endpoint tests call bare URLs.
- CORS is already configured for the deployed Railway frontend/backend split, but without `allowCredentials(true)` — needed for session cookies to survive cross-origin.

## Key Decisions

| Decision | Choice | Why |
|---|---|---|
| Auth mechanism | Session (cookie), not JWT or HTTP Basic | Standard Spring Boot pattern; works naturally with a browser SPA via `credentials: 'include'` |
| Credential storage | New `passwordHash` field directly on `Employee` | `Employee` already anchors identity (email, role) — no new entity/table/migration needed for a minimal foundation |
| Authorization enforcement | Keep the existing service-layer checks (`requireManagedBy`); only change where the ID comes from | The correct logic already exists — don't add a second authorization mechanism on top of it |
| URL shape | Remove `{employeeId}`/`{managerId}` from every URL; always derive from the session | Eliminates a whole class of "URL says X, session says Y" bugs; cleaner API contract |
| Login UI scope | **Included in F-01** (overriding the roadmap's default "Foundations have no user-visible output") | User explicitly wants a way to manually verify login in the browser now, not wait for S-01 |
| Credential seeding | Owned by roadmap Foundation **F-02**, not this change | Keeps this change's automated tests self-contained (they seed their own test passwords); production/demo accounts are F-02's job |
| Test strategy | Existing tests log in via the real `/api/login` endpoint (not `@WithMockUser`) | Proves the actual login mechanism works, not just that business logic works assuming auth happened |
| Session timeout | Spring Boot default (~30 min) | No stated need for anything longer on an internal MVP tool |

## Phases

| # | Phase | Key files |
|---|---|---|
| 1 | Backend auth infrastructure | `pom.xml`, `Employee.java`, new `security/` package, new `SecurityConfig.java`, `CorsConfig.java`, new `AuthController.java` |
| 2 | Move identity to session | `RaiseRequestController.java` (new URL shapes, `@PreAuthorize`) |
| 3 | Update and add tests | `RaiseRequestControllerIntegrationTest.java`, new `AuthControllerIntegrationTest.java` |
| 4 | Minimal frontend login screen | `frontend/src/App.tsx` |

**Prerequisites**: None blocking — this is roadmap Foundation F-01, with `Prerequisites: —`. **Note**: Phase 1 intentionally breaks `./mvnw test` (constructor signature change) until Phase 3 lands; `package -DskipTests` is the correct Phase 1/2 gate.

## What's Explicitly Out of Scope

Self-service registration, password reset, HR/CEO roles, OAuth/SSO, stateless JWT, and login-screen styling — see `plan.md`'s "What We're NOT Doing" for the full list and rationale.

## Open Risk

Manually verifying the login screen (Phases 1 and 4) needs at least one real account before roadmap Foundation F-02's seed script lands — use the local H2 console (already enabled) or a one-off insert in the meantime.

## References

- Roadmap: `context/foundation/roadmap.md` (Foundation F-01)
- PRD: `context/foundation/prd.md` (`## Access Control`, `## Non-Functional Requirements`)
- Full plan: `context/changes/minimal-auth-scaffold/plan.md`
