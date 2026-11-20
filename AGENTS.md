# Repository Guidelines

Pay Raise Module: a raise/promotion approval tool. Spring Boot (Java 21, Maven) backend at the repo root, a separate Vite + React + TypeScript frontend in `frontend/`, backed by PostgreSQL in Docker / H2 locally.

## Hard Rules

- Never write into `context/` except via the `/10x-*` skills — it holds the project's PRD/tech-stack/shape-notes chain and `10x-bootstrapper` preserves it verbatim on re-scaffold.
- `bustling-binary/` and `data/*.db` are gitignored on purpose (unrelated leftover scaffold, local H2 runtime files) — don't track or reference them.
- Backend and frontend each have their own build/test/lint toolchain; don't add a root-level JS or Java tool that straddles both.

## Project Structure & Module Organization

- `src/main/java/com/example/payraisemodule/` — layered by concern: `domain/`, `dto/`, `repository/`, `service/`, `controller/`. Tests mirror this under `src/test/java/...`.
- `frontend/src/` — React app; `components/ui/` is shadcn-generated (regenerate via `npx shadcn add <name>` rather than hand-editing structure), `lib/utils.ts` holds `cn()`. Path alias `@/*` maps to `frontend/src/*` (see `@frontend/vite.config.ts`).
- `context/foundation/` — PRD, tech-stack hand-off, and shape-notes for this project; read `@context/foundation/prd.md` for domain rules (approval routing, access control) before changing business logic.
- `frontend/.env.example` documents `VITE_API_URL` (read via `import.meta.env.VITE_API_URL`) as the backend API base; no client code consumes it yet.

## Deployment

Railway (`backend` + `frontend` services, plus a Postgres plugin) — see `@context/foundation/infrastructure.md` for the platform decision and `@context/deployment/deploy-plan.md` for the rollout plan/status. `.railway/railway.ts` (Railway's Infrastructure-as-Code format, not the deprecated `railway.json`) pins both services' Dockerfile builder, the `frontend` service's `rootDirectory`, and the backend's `/actuator/health` check. The root `package.json`/`node_modules` exist only to run the `railway` npm package for this IaC file (`railway config plan`/`apply`) — not an app build/lint tool, so it doesn't violate the backend/frontend toolchain-separation rule above.

## Build, Test, and Development Commands

- `./mvnw spring-boot:run` — run the backend on port 8081 (H2 file DB in `data/`).
- `./mvnw test` — run backend unit + integration tests.
- `cd frontend && npm run dev` — Vite dev server on 5173.
- `cd frontend && npm run build` — `tsc -b` then `vite build`.
- `cd frontend && npm run lint` — oxlint.
- `./mvnw spotless:check` — verify backend formatting (`./mvnw spotless:apply` to fix); backend uses Google Java Format via the Spotless Maven plugin (`@pom.xml`).
- `docker compose up` — full stack against Postgres (see `@docker-compose.yml`).

## Coding Style & Naming Conventions

- Backend: field/method order: fields → constructor → public methods → private methods; no blank line at the start of a class body. Formatting is enforced by Spotless/Google Java Format — run `./mvnw spotless:apply` before committing.
- Frontend: components `PascalCase.tsx`, utilities `kebab-case.ts`; see `@frontend/package.json` for the full dependency/script list.

## Testing Guidelines

- Backend: JUnit 5 + Mockito, test classes end in `Test`, mirror the package of the class under test. Integration tests use the H2 in-memory profile in `@src/test/resources/application.properties` (`create-drop`).
- Frontend: no test runner configured yet — verify changes with `npm run build` and manual check.

## Commit & Pull Request Guidelines

No CI configured yet. Commit messages so far: short imperative summary line, optional body explaining *why*, ending with a `Co-Authored-By:` trailer when AI-assisted — follow this shape.
