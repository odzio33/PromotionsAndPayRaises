# Implementation Plan: Minimal auth scaffold

## Overview

Replace today's path-parameter trust (`employeeId`/`managerId` taken straight from the URL) with a real, minimal session-based login for the two MVP roles (Employee, Manager), plus a bare-functional frontend login screen. This is roadmap Foundation F-01 — it unlocks S-01, S-02, S-03, none of which should ship with the current "anyone can impersonate anyone" gap.

## Current State Analysis

- `Employee` (`src/main/java/com/example/payraisemodule/domain/Employee.java`) has `email` (unique), `fullName`, `role` (`EMPLOYEE`/`MANAGER`), `manager`, `position` — but no credential field at all.
- No Spring Security dependency in `pom.xml`; no filter chain, no login endpoint.
- `RaiseRequestController` (`src/main/java/.../controller/RaiseRequestController.java:23-25`) explicitly comments that `employeeId`/`managerId` are path variables "because authentication ... is not wired up yet."
- `RaiseRequestService.requireManagedBy` (`src/main/java/.../service/RaiseRequestService.java:87-97`) already enforces "is this manager actually assigned to this employee" correctly — only the *source* of the manager ID is wrong today (URL instead of session).
- `CorsConfig` (`src/main/java/.../config/CorsConfig.java`) allows the frontend origin but does not set `allowCredentials(true)` — session cookies will not work cross-origin (frontend and backend are separate Railway services) without this.
- `RaiseRequestControllerIntegrationTest` and `ApplicationTests` call every endpoint unauthenticated today; they construct `Employee` directly via its 5-arg constructor.
- `EmployeeRepository.findByEmail(String)` already exists — the natural lookup for a `UserDetailsService`.

## What We're NOT Doing

- Self-service registration / sign-up (accounts are provisioned via seed data — ownership moved to roadmap Foundation F-02 per this change's interview)
- Password reset / forgot-password flow
- HR or CEO roles, or any role beyond Employee/Manager (PRD Non-Goals)
- OAuth/SSO (PRD Access Control allows it long-term; session + password is this MVP's choice)
- Stateless JWT (session-based login was chosen over JWT/HTTP Basic in this change's interview)
- Visual styling of the login screen beyond functional minimum — S-01 owns the real UI

## Phase 1: Backend auth infrastructure

### Changes Required:

#### `pom.xml`
**Intent**: Add Spring Security for session-based authentication, and its test support alongside the existing `spring-boot-starter-webmvc-test`.
**Contract**: Add `spring-boot-starter-security` (compile scope) and `spring-security-test` (test scope).

#### `src/main/java/com/example/payraisemodule/domain/Employee.java`
**Intent**: Store a hashed password per employee so the login endpoint has something to verify against.
**Contract**: Add `@Column(name = "password_hash", nullable = false) private String passwordHash;` with getter/setter, and add `passwordHash` as a 6th constructor parameter. This is a breaking constructor change — every call site is fixed in Phase 3 (tests); Phase 1's automated check is `package -DskipTests`, not `test`.

#### `src/main/java/com/example/payraisemodule/security/EmployeeUserDetails.java` (new)
**Intent**: Adapt `Employee` to Spring Security's `UserDetails` contract so the built-in `DaoAuthenticationProvider`/`AuthenticationManager` can authenticate against it without any custom provider code.
**Contract**: Implements `UserDetails`; `getUsername()` → `employee.getEmail()`, `getPassword()` → `employee.getPasswordHash()`, `getAuthorities()` → single `SimpleGrantedAuthority("ROLE_" + employee.getRole().name())`. Exposes `getEmployee()`/`getEmployeeId()` for controller use.

#### `src/main/java/com/example/payraisemodule/security/EmployeeUserDetailsService.java` (new)
**Intent**: Load a `UserDetails` by email for Spring Security to authenticate against.
**Contract**: Implements `UserDetailsService`; `loadUserByUsername(email)` uses `EmployeeRepository.findByEmail`, wraps in `EmployeeUserDetails`, throws `UsernameNotFoundException` if absent (Spring Security's own class — do not leak "no such email" wording differently from a wrong password, to avoid user enumeration).

#### `src/main/java/com/example/payraisemodule/config/SecurityConfig.java` (new)
**Intent**: Wire a JSON-API-friendly session security chain — not Spring's default HTML `formLogin` flow, which would redirect to a login *page* instead of returning JSON.
**Contract**:
- `@EnableMethodSecurity` on the class (needed by Phase 2's `@PreAuthorize`).
- `@Bean PasswordEncoder` → `BCryptPasswordEncoder`.
- `@Bean AuthenticationManager` → `authenticationConfiguration.getAuthenticationManager()`.
- `@Bean SecurityFilterChain`: `.cors(Customizer.withDefaults())` (reuse the existing `CorsConfig` bean — do not duplicate CORS rules here); `.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))` (cookie must be JS-readable so the SPA can echo it back on state-changing requests); `.authorizeHttpRequests(a -> a.requestMatchers("/api/login", "/actuator/health").permitAll().anyRequest().authenticated())`; `.exceptionHandling(...)` with a custom `AuthenticationEntryPoint` (401 + `{"error":"Authentication required"}`) and `AccessDeniedHandler` (403 + `{"error":"Access denied"}`) so failures return JSON, never an HTML page; `.formLogin(AbstractHttpConfigurer::disable)`, `.httpBasic(AbstractHttpConfigurer::disable)`; `.logout(l -> l.logoutUrl("/api/logout").logoutSuccessHandler((req,res,auth) -> res.setStatus(200)))`.

#### `src/main/java/com/example/payraisemodule/config/CorsConfig.java`
**Intent**: Session cookies cannot cross origins without this — the frontend and backend are separate Railway services/domains.
**Contract**: Add `.allowCredentials(true)` to the existing `CorsRegistry` mapping. (Safe with the existing explicit `CORS_ALLOWED_ORIGINS` list — credentialed CORS cannot use a wildcard origin, and this config never did.)

#### `src/main/java/com/example/payraisemodule/dto/LoginRequest.java` (new)
**Intent**: Validated login request body, matching the existing DTO convention (e.g. `SubmitRaiseRequest`).
**Contract**: `record LoginRequest(@NotBlank String email, @NotBlank String password)`.

#### `src/main/java/com/example/payraisemodule/dto/AuthResponse.java` (new)
**Intent**: One consistent "who is logged in" shape shared by `/api/login` and `/api/me`.
**Contract**: `record AuthResponse(Long id, String email, String fullName, Role role)` with a static `from(Employee)` factory, mirroring `RaiseRequestResponse.from(...)`.

#### `src/main/java/com/example/payraisemodule/controller/AuthController.java` (new)
**Intent**: Expose JSON login/me endpoints a React SPA can call directly (logout is handled entirely by Spring Security's own filter at `/api/logout` — no custom code needed for it).
**Contract**: `POST /api/login` (body `LoginRequest`) — authenticates via `AuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))`, stores the result in `SecurityContextHolder` and persists it to the HTTP session via `HttpSessionSecurityContextRepository`, returns `200 AuthResponse`. A thrown `AuthenticationException` propagates to `ApiExceptionHandler` (below). `GET /api/me` — returns `200 AuthResponse` for the current `@AuthenticationPrincipal EmployeeUserDetails`; unauthenticated calls never reach the method body (the security chain's entry point returns 401 first).

#### `src/main/java/com/example/payraisemodule/controller/ApiExceptionHandler.java`
**Intent**: Keep the existing `{"error": "..."}` shape for authentication failures too, instead of Spring Security's default response.
**Contract**: Add `@ExceptionHandler(org.springframework.security.core.AuthenticationException.class)` → `401 {"error": "Invalid email or password"}` (same message for "no such email" and "wrong password" — avoids leaking which one was wrong).

#### Success Criteria:

##### Automated Verification:
- `./mvnw spotless:apply && ./mvnw spotless:check` passes
- `./mvnw -B package -DskipTests` succeeds (main sources compile with the new security classes and the 6-arg `Employee` constructor)

##### Manual Verification:
- Insert one test `Employee` row with a bcrypt-encoded password via the local H2 console (`spring.h2.console.enabled=true`), then `curl -i -c cookies.txt -X POST http://localhost:8081/api/login -H "Content-Type: application/json" -d '{"email":"...","password":"..."}'` returns `200` + a `Set-Cookie: JSESSIONID=...`.
- The same request with a wrong password returns `401 {"error":"Invalid email or password"}`.
- `curl http://localhost:8081/api/me` with no cookie returns `401` JSON (not an HTML login page).

## Phase 2: Move identity to session

### Changes Required:

#### `src/main/java/com/example/payraisemodule/controller/RaiseRequestController.java`
**Intent**: Endpoints must trust the authenticated session, never a URL-supplied ID, per this change's core goal.
**Contract**: Replace `{employeeId}`/`{managerId}` path variables with `@AuthenticationPrincipal EmployeeUserDetails` on every method, and change the routes:
- `POST /api/employees/{employeeId}/raise-requests` → `POST /api/raise-requests`
- `GET /api/employees/{employeeId}/raise-requests` → `GET /api/raise-requests/mine`
- `GET /api/managers/{managerId}/raise-requests/pending` → `GET /api/raise-requests/pending`
- `POST /api/managers/{managerId}/raise-requests/{requestId}/decision` → `POST /api/raise-requests/{requestId}/decision`
- `POST /api/managers/{managerId}/raise-requests/{requestId}/send-back` → `POST /api/raise-requests/{requestId}/send-back`

Add `@PreAuthorize("hasRole('EMPLOYEE')")` to submit/mine and `@PreAuthorize("hasRole('MANAGER')")` to pending/decision/send-back — a coarse role gate on top of (not instead of) the existing fine-grained `requireManagedBy` check in the service.

#### `src/main/java/com/example/payraisemodule/service/RaiseRequestService.java`
**Intent**: No business-rule change — the yearly-limit check, band decision, and `requireManagedBy` logic are already correct. Only the controller's *source* of `employeeId`/`managerId` changes.
**Contract**: No code change. (Confirms the "continue service pattern" interview decision — authorization logic stays where it already is.)

#### Success Criteria:

##### Automated Verification:
- `./mvnw -B package -DskipTests` succeeds with the new routes and `@PreAuthorize` annotations

##### Manual Verification:
- (Covered by Phase 3's rewritten integration tests — no separate manual step here.)

## Phase 3: Update and add tests

### Changes Required:

#### `src/test/java/com/example/payraisemodule/controller/RaiseRequestControllerIntegrationTest.java`
**Intent**: Keep proving the FR-001...FR-008 flow end-to-end, now through real authenticated sessions against the Phase 2 URL shapes — this is the test that proves auth didn't break the business flow.
**Contract**: In `@BeforeEach`, create `manager`/`employee` with a `PasswordEncoder`-encoded password (autowire the bean from `SecurityConfig`). Before each flow, log in via `mockMvc.perform(post("/api/login")...)` as the relevant user, capture the resulting `MockHttpSession`, and pass it (`.session(session)`) on every subsequent call instead of an ID in the URL. Update all 5 request URLs to the Phase 2 shapes.

#### `src/test/java/com/example/payraisemodule/controller/AuthControllerIntegrationTest.java` (new)
**Intent**: Nothing today directly tests the login contract — this closes that gap.
**Contract**: 4 cases: valid login → `200` + `AuthResponse` body matching the seeded employee; wrong password → `401`; unknown email → `401` with the *same* message as wrong password (enumeration check); `GET /api/me` with no session → `401`; with a valid session → `200` matching the logged-in user.

#### `src/test/java/com/example/payraisemodule/ApplicationTests.java`, `src/test/java/com/example/payraisemodule/domain/PositionTest.java`, `src/test/java/com/example/payraisemodule/domain/RaiseRequestTest.java`
**Intent**: Confirm these still compile/pass once `Employee`'s constructor gains `passwordHash`.
**Contract**: `ApplicationTests` needs no logic change (bare `contextLoads()`) but must be re-run — if the Spring context fails to load due to security bean wiring, fix `SecurityConfig`, not this test. Update `PositionTest`/`RaiseRequestTest` call sites only if they construct `Employee` directly with the old 5-arg signature.

#### Success Criteria:

##### Automated Verification:
- `./mvnw test` — full suite green (0 failures/errors)
- `./mvnw spotless:check` passes

## Phase 4: Minimal frontend login screen

### Changes Required:

#### `frontend/src/App.tsx`
**Intent**: Give a human a way to exercise the login endpoint from a browser — in scope for this Foundation per this change's interview (overriding the roadmap's default assumption that Foundations have no user-visible output). Deliberately minimal; S-01 owns the real UI and routing.
**Contract**: Replace the current shadcn placeholder with a single form (email + password inputs, submit button) that `fetch`es `` `${import.meta.env.VITE_API_URL}/api/login` `` with `method: 'POST'`, `credentials: 'include'`, JSON body. On `200`, render the returned `fullName`/`role`. On `401`, render the error message from the response body.

#### Success Criteria:

##### Automated Verification:
- `cd frontend && npm run build` succeeds

##### Manual Verification:
- Open the deployed frontend URL, log in with a real test account (insert one via the backend's H2 console, or wait for F-02's seed script), confirm the name/role renders; a wrong password shows the error; reloading the page after a successful login does not require logging in again within the session window — this specifically proves Phase 1's `allowCredentials(true)` CORS fix works cross-origin on Railway (frontend and backend are separate deployed services).

## Open Risks & Assumptions

- **Credential seeding ownership**: this change's interview moved production/demo credential seeding to roadmap Foundation F-02, not this change. This change's own automated tests are self-contained (they encode their own test passwords) and do not depend on F-02 landing first. Manual verification (Phases 1 and 4) needs at least one real account in the meantime — use the already-enabled local H2 console, or a one-off SQL insert, until F-02 ships.
- **Roadmap dependency drift**: `context/foundation/roadmap.md` currently lists F-01 and F-02 as `Parallel with` each other (no `Prerequisites` between them). This plan's automated tests don't need that to change, but the deployed/demo environment's *manual* verification of the login screen is more useful once F-02's seed exists. Consider updating F-01's `Prerequisites`/`Parallel with` fields in the roadmap if that ordering turns out to matter in practice — not changed automatically by this plan.
- **No frame.md existed for this change** — this plan was built directly from roadmap Foundation F-01 + PRD `## Access Control`/`## Non-Functional Requirements` + direct codebase research, not a prior `/10x-frame` pass.

## Progress

### Phase 1: Backend auth infrastructure

#### Automated
- [x] 1.1 `./mvnw spotless:apply && spotless:check` passes — f03229f
- [x] 1.2 `./mvnw -B package -DskipTests` succeeds — f03229f

#### Manual
- [x] 1.3 Login with a seeded test account returns 200 + session cookie — f03229f
- [x] 1.4 Login with a wrong password returns 401 `{"error":"Invalid email or password"}` — f03229f
- [x] 1.5 `/api/me` with no cookie returns 401 JSON, not an HTML login page — f03229f

### Phase 2: Move identity to session

#### Automated
- [x] 2.1 `./mvnw -B package -DskipTests` succeeds with the new routes and `@PreAuthorize` annotations — bbf8c1e

### Phase 3: Update and add tests

#### Automated
- [x] 3.1 `./mvnw test` — full suite green (0 failures/errors) — bbf8c1e
- [x] 3.2 `./mvnw spotless:check` passes — bbf8c1e

### Phase 4: Minimal frontend login screen

#### Automated
- [ ] 4.1 `cd frontend && npm run build` succeeds

#### Manual
- [ ] 4.2 Login via the deployed frontend renders name/role; wrong password shows an error; session survives a page reload cross-origin
