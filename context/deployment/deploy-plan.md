---
project: "Pay Raise Module"
platform: Railway
status: code-ready
updated: 2026-11-20
---

# Railway Deployment Plan

Executes the decision in `@context/foundation/infrastructure.md` (Railway, over Fly.io/Render). See that file for the full platform comparison and anti-bias cross-check; this file is the execution plan + running operational log.

**Legend:** `[AGENT]` = Claude Code can do this directly (file edit, local CLI). `[HUMAN]` = requires browser OAuth, dashboard click, or an account/billing decision.

## Phase 0 — Pre-flight

- [X] `[HUMAN]` Confirm a Railway account exists with a payment method attached.
- [X] `[AGENT]` Check for Railway CLI (`railway --version`); install if missing (`npm i -g @railway/cli`). — installed `railway 5.75.0`.
- [X] `[HUMAN]` `railway login` (interactive browser OAuth). — signed in as Piotr Ohde (poodzio0604@gmail.com).
- [X] `[AGENT]` Verify with `railway whoami`. — confirmed.
- [X] `[HUMAN]` Create the Railway project. — created via `railway init --name pay-raise-module` (project ID `9350e47f-96ab-4f19-a14f-161130b866c0`), workspace "Piotr Ohde's Projects".
- [X] `[AGENT]` `railway link` in the repo root once the project exists. — `railway init` links automatically; confirmed via `railway status` (environment `production`, ID `6f3eb2a4-a55b-4574-8775-e7dcd655b553`).

## Phase 1 — Backend code changes ✅ done (2026-11-20)

- [x] `server.port=${PORT:8081}` in `src/main/resources/application.properties`.
- [x] `spring-boot-starter-actuator` added to `pom.xml`.
- [x] `/actuator/health` exposed (`management.endpoints.web.exposure.include=health`, `management.endpoint.health.probes.enabled=true`).
- [x] `CorsConfig` (`src/main/java/com/example/payraisemodule/config/CorsConfig.java`) — reads `app.cors.allowed-origins` (env `CORS_ALLOWED_ORIGINS`), default `http://localhost:5173` (both in properties and in the `@Value` annotation itself, since `src/test/resources/application.properties` fully replaces the main one for tests and has no default otherwise).
- [x] ~~`railway.json` at repo root~~ — superseded, see Phase 3's IaC migration note (now `.railway/railway.ts`).
- [x] `./mvnw spotless:apply && spotless:check` clean; `./mvnw -B package -DskipTests` and `./mvnw test` both pass (20 tests, 0 failures/errors).

## Phase 2 — Frontend code changes ✅ done (2026-11-20)

- [x] `frontend/Dockerfile` rewritten as multi-stage: `deps` → `dev` (used by `docker-compose.yml` via `target: dev`, preserves local hot-reload) and `build` → final stage serving `dist/` via `serve` on `$PORT` (used by Railway, no `target` specified so it gets the last/production stage).
- [x] `docker-compose.yml`'s `frontend` service updated to `build: { context: ./frontend, target: dev }` so local `docker compose up` still runs the Vite dev server.
- [x] `frontend/.env.example` with `VITE_API_URL=http://localhost:8081`; convention documented in `@AGENTS.md` (no client code consumes it yet — business UI isn't built).
- [x] ~~`frontend/railway.json`~~ — superseded, see Phase 3's IaC migration note (now `.railway/railway.ts`, `rootDirectory: "frontend"`).
- [x] `npm run build` inside `frontend/` confirmed clean (`dist/` produced, no errors).

## Phase 3 — Railway service setup ✅ done (2026-11-20)

- [X] `[AGENT]` Add the Postgres plugin — `railway add --database postgres` (service ID `477ed03d-d0f2-49a0-aa20-323d4719deb0`). Could be done via CLI non-interactively; didn't need the dashboard.
- [X] `[AGENT]` Create the **backend** service from the GitHub repo — `railway add --repo odzio33/PromotionsAndPayRaises --branch main --service backend` (service ID `32bafc03-fed3-4da9-ac8b-ecb1ec1745af`).
- [X] `[AGENT]` Create the **frontend** service from the same repo — `railway add --repo odzio33/PromotionsAndPayRaises --branch main --service frontend` (service ID `b0cb84d6-b2e8-4953-b479-ff18504f6bba`).
- [X] **Auto-deploy on push**: branch `main` set explicitly via `--branch main` on both `railway add` calls above — this is what wires Railway's native GitHub App integration to trigger a build on every push to `main`, no GHA involved. *(Still worth a visual dashboard confirmation in Phase 5 that "Deploy on Push" is actually ON, since the CLI doesn't expose a way to read that flag back.)*
- [X] `[AGENT]` Confirmed the Postgres plugin's actual variable names via `railway variables --service Postgres --json`: `PGHOST=postgres.railway.internal`, `PGPORT=5432`, `PGDATABASE=railway`, `PGUSER=postgres`, `PGPASSWORD=<redacted>` — matches the assumed names exactly, also noted a `DATABASE_URL` convenience var we don't need (our app reads discrete `DB_*` vars, not a single URL).
- [X] `[AGENT]` Set backend env vars via `railway variable set --service backend`: `SPRING_PROFILES_ACTIVE=docker`, `DB_HOST=${{Postgres.PGHOST}}`, `DB_PORT=${{Postgres.PGPORT}}`, `DB_NAME=${{Postgres.PGDATABASE}}`, `DB_USERNAME=${{Postgres.PGUSER}}`, `DB_PASSWORD=${{Postgres.PGPASSWORD}}` — verified the references resolved correctly (`DB_HOST` shows the real internal hostname, not the literal `${{...}}` string). `CORS_ALLOWED_ORIGINS` deliberately left unset (Phase 4).
- [X] **Config-as-Code → Infrastructure-as-Code migration** (discovered live via CLI, not in the original research): Railway deprecated `railway.json`/`railway.toml` in favor of `.railway/railway.ts`; the old format only works until **2026-12-01** — before this project's 2026-12-06 hard deadline. Done: `npm install railway` at repo root (devtool only — see `@AGENTS.md`'s Deployment section for why this doesn't violate the toolchain-separation rule; `node_modules`/root `package.json` are new, `node_modules` added to `.gitignore`), then `railway config migrate --apply --variables=unmanaged --service backend`, hand-edited `.railway/railway.ts` to add the `frontend` service with `source.rootDirectory: "frontend"` (the migrate command only discovers per-service Config-as-Code, it doesn't know about monorepo subdirectories — that's authored by hand, once, per Railway's own IaC docs), `railway config plan` → `railway config apply --yes` → `railway config migrate cutover --yes` (reported "nothing to do", confirmed via `railway config migrate status` that both services are fully off Config as Code). Deleted the now-dead `railway.json` / `frontend/railway.json`.
- [X] **Windows-only CLI bug hit and worked around**: `railway config plan`/`apply` shell out to the `railway` binary to version-check itself, via Node's `execFileSync` *without* `shell: true` — this cannot launch Windows `.cmd` shims (a well-known Node/Windows limitation), so every `railway config plan` failed with a misleading "CLI too old" error regardless of actual version (`railway --version` was 5.75.0, well above the 5.42.1 minimum). Worked around by patching the installed `node_modules/railway/dist/iac/index.js`'s `execFileSync` call to pass `shell: true` locally (not committed — `node_modules` is gitignored, so this patch doesn't persist across `npm install`; re-apply if `npm install` is re-run on Windows before Railway ships a fix upstream).

## Phase 4 — Domain generation + env wiring (strict order)

- [ ] `[HUMAN]` Generate the **backend** public domain first (Settings → Networking → Generate Domain).
- [ ] `[HUMAN]` Set frontend env var `VITE_API_URL=https://<backend-public-domain>` (Vite bakes this in at build time — redeploy frontend after setting it).
- [ ] `[HUMAN]` Generate the **frontend** public domain.
- [ ] `[HUMAN]` Set backend env var `CORS_ALLOWED_ORIGINS=https://<frontend-public-domain>` (add `http://localhost:5173` too, comma-separated, for local-against-prod testing). **Never use `*.railway.internal`/`RAILWAY_PRIVATE_DOMAIN` here — the browser can't resolve it.**
- [ ] `[HUMAN]` Redeploy both services to pick up the new vars.

## Phase 5 — Validation

- [ ] `curl https://<backend-public-domain>/actuator/health` → expect `{"status":"UP"}`.
- [ ] `[HUMAN]` Confirm each service's build log shows Dockerfile build steps (`maven:3.9-eclipse-temurin-21`, `COPY pom.xml` / `node:22-alpine`, `npm ci`) — not Railpack/Nixpacks auto-detect. See Phase 6 if it shows Railpack anyway.
- [ ] `[HUMAN]` Open the frontend public URL in a browser — expect the current placeholder shadcn page (no business UI/API calls exist yet).
- [ ] `[AGENT]` `railway logs --service backend` / `--service frontend` — confirm no startup errors.
- [ ] `[HUMAN]` Set a usage/budget alert (Project Settings → Usage), e.g. $20/mo threshold.
- [ ] `[HUMAN]` Push a trivial commit to `main` and confirm both services redeploy automatically without any manual trigger — proves the auto-deploy-on-push wiring from Phase 3 actually works end to end.

## Phase 6 — Edge cases / extra support steps

- [ ] **Railpack ignores the Dockerfile builder override**: if Phase 5's log check shows Railpack ran despite `railway.json`, `[HUMAN]` set Builder to "Dockerfile" explicitly in that service's Settings → Build, then redeploy and re-check.
- [ ] **Health check still times out**: `[AGENT]` bump `healthcheckTimeout` in `railway.json` (e.g. to 600); check `railway logs` for slow Hibernate schema validation on cold start; confirm nothing blocks unauthenticated requests to `/actuator/health` (today: trivially true, no Spring Security in `pom.xml`).
- [ ] **Bad deploy needs rollback** (no CLI rollback verb): `[HUMAN]` Deployments tab → find last known-good → "Redeploy". `[AGENT]` log known-good deployment IDs/commit SHAs below as they accumulate.
- [X] **Postgres plugin "eject into repo"**: variable names confirmed in Phase 3 (no file-based export exists for Railway's Postgres plugin — this is documentation/pinning, not infra-as-code). `PGPASSWORD` deliberately omitted here — read it live via `railway variables --service Postgres` if ever needed, never paste it into a committed file:
  - `PGHOST` = `postgres.railway.internal`
  - `PGPORT` = `5432`
  - `PGDATABASE` = `railway`
  - `PGUSER` = `postgres`
- [ ] **MCP server scope**: if `mcp.railway.com` is wired up later, scope it read-only (logs/status) — no write/deploy scope without a separate explicit decision.
- [ ] **Auto-deploy didn't trigger on push**: check the GitHub repo's Settings → Integrations / Installed GitHub Apps for the Railway app and confirm it has access to this repo; re-connect the service's Source if the webhook is missing.

## Known-good deployments (rollback log)

| Date | Service | Commit SHA | Notes |
|---|---|---|---|
| _(fill in after first successful deploy)_ | | | |

## Out of Scope

- Docker image optimization beyond the multi-stage split already in place.
- Production-scale architecture (multi-region, HA, DR).
- GitHub Actions — intentionally not used; Railway's native GitHub integration handles build + auto-deploy on push to `main` directly.
