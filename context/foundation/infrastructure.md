---
project: "Pay Raise Module"
researched_at: 2026-11-20
recommended_platform: Railway
runner_up: Render
context_type: mvp
tech_stack:
  language: java
  framework: spring-boot
  runtime: jvm-21-maven
---

## Recommendation

**Deploy on Railway.** It scored highest of the three platforms that can actually run a long-lived JVM/Spring Boot process (Cloudflare Workers, Vercel, and Netlify were eliminated outright — none can host a standard Spring Boot process), with auto-detected Java/Maven builds, a co-located managed Postgres plugin, and the cheapest realistic monthly cost (~$10–25/mo) for this app's low-QPS, single-region, co-located-database profile. This matches the interview priorities directly: cost-minimization (P2) and co-located Postgres (P5) both point to Railway over Fly.io, whose new Managed Postgres pushes the cost floor to ~$50–60/mo.

## Platform Comparison

Hard filter applied first: the tech stack requires a long-lived JVM process (Spring Boot's embedded Tomcat). Cloudflare Workers (V8 isolate / Wasm-only, no real JVM), Vercel (serverless functions only, Dockerfile route still function-shaped), and Netlify (Node-only Functions, Java unsupported) were removed from scoring — none can host the backend at all. Only the frontend (Vite+React static build) would fit any of them, which is out of scope for this decision since the frontend has no hosting constraint driving platform choice here.

| Platform | CLI-first | Managed/Serverless | Agent-readable docs | Stable deploy API | MCP/integration |
|---|---|---|---|---|---|
| **Railway** | Partial | Pass | Pass | Pass | Pass |
| Fly.io | Partial | Partial | Pass | Pass | Pass |
| Render | Partial | Partial | Partial | Partial | Pass |

### Shortlisted Platforms

#### 1. Railway (Recommended)

Highest score (4 Pass / 1 Partial). Auto-detects Java/Maven on push, runs genuine long-lived containers (no serverless cold-start mismatch with Spring's embedded server), ships a co-located Postgres plugin on the same private network, and bills usage-based by the second with no separate database subscription fee — the cheapest path at this traffic level (~$10–25/mo all-in). Docs live in a public, MIT-licensed GitHub repo (markdown, PR-friendly), and an official Remote MCP server (`mcp.railway.com`) is GA, if young (~7 months old as of this research). The one real gap: no dedicated CLI `rollback` verb (`railway redeploy` / dashboard "Redeploy" are the workarounds) and Java version isn't auto-pinned (needs `.system.properties` or a Dockerfile).

#### 2. Render

Second: cheap (~$13–26/mo) with first-party managed Postgres and native WebSocket support, but the actual day-to-day loop is dashboard/git-push-centric rather than CLI-first — the CLI exists but rollback has no CLI verb at all (dashboard-only), and docs are available only page-by-page as markdown, not as a full repo. Free tier's 30-day hard Postgres deletion is a real trap for an MVP timeline spanning more than a month. Would be the safer "boring and predictable pricing" pick if Railway's usage-based billing feels too open-ended.

#### 3. Fly.io

Technically the strongest CLI/MCP story (official hosted MCP server with scoped read/write/admin permissions, long-standing Machines-as-VMs model, genuine persistent processes) but the most expensive by far: the free tier is gone (removed Oct 2024) and the current "Managed Postgres" product starts at $38/month alone, pushing the realistic floor to ~$50–60/month — directly in tension with this project's stated cost-minimization priority. Also carries a legacy/deprecated unmanaged "Fly Postgres" product that should NOT be used for a new project (the research explicitly flagged this as winding down).

## Anti-Bias Cross-Check: Railway

### Devil's Advocate — Weaknesses

1. Railway bills RAM/CPU per second with no hard cap by default — a misconfigured health check or a JVM memory leak causing restart loops can run up cost unexpectedly, unlike Render/Fly's flatter pricing.
2. No native `rollback` command — `railway redeploy` / dashboard "Redeploy" are workarounds, not a deterministic rollback primitive; a bad prod deploy costs more manual time than on a platform with a real rollback verb.
3. Java/Maven auto-detection does not pin the JDK version — without a `.system.properties` (or a Dockerfile), the build may silently use a different JDK than the `java.version` in `pom.xml` (21), surfacing as a runtime-only bug.
4. The official Railway MCP server is only ~7 months old (launched 2026-04) — new enough that behavior or scope could still shift, a consideration before giving an agent unattended write access to a production database.
5. The Nixpacks → Railpack builder migration means older guides and Stack Overflow answers may reference stale config syntax, costing time during the tight 3-week MVP window.

### Pre-Mortem — How This Could Fail

The team deployed Spring Boot + Postgres on Railway in December, right before the hard deadline. The developer assumed "Railway detects Java and it just works" and skipped adding `.system.properties`, so the build silently used a JDK slightly different from the `java.version` pinned in `pom.xml`; the app started fine, but one endpoint threw an exception only at runtime — invisible in local testing. Late in the month, the bill came in higher than expected: a health-check-triggered restart loop (caused by that same bug) burned through the entire Hobby plan's $5 included credit within a week, discovered only via an overage email. Rolling back to the last known-good version turned out harder than assumed — there's no single `rollback` command, so the developer had to manually hunt down the right prior deployment in the dashboard under time pressure before a demo for HR. The team had also assumed "no persistent connections needed" meant no long-process risk at all, but Spring Boot's embedded Tomcat and connection pool are a persistent process regardless — so the restart loop cost far more than it would have on a genuinely stateless serverless function.

### Unknown Unknowns

- Services deployed from GitHub are **not publicly accessible by default** — a domain must be generated manually under Networking, or the first deploy silently has no reachable URL.
- Railway meters RAM and CPU separately, per second — a different mental model than a flat monthly VM price; easy to under-budget a JVM that reserves heap memory upfront regardless of actual load.
- During proxy node drain/rebalance, all WebSocket/SSE connections on that node are severed simultaneously (TCP RST) — doesn't matter for this app's lack of WS requirement, but it does apply to any long HTTP request (e.g., a slow report-generation endpoint) riding through the same proxy layer during a deploy.
- One-click templates (e.g., the Postgres plugin) are recommended to be "ejected" into your own repo — skip that and a template update can silently overwrite your configuration.
- The Free plan's $1 credit covers a few hours of real traffic, not a month of development — in practice you need Hobby ($5/mo) from day one, which the "Free plan" marketing doesn't make obvious upfront.

## Operational Story

- **Preview deploys**: Railway deploys per-branch/PR when connected to GitHub; each gets its own service URL. No mention of required Access-style protection for internal previews at this scale.
- **Secrets**: environment variables are set per-service in the Railway dashboard or via `railway variables set` (CLI); shared across the private network within a project, not exposed outside it. No built-in rotation flow — rotate manually and redeploy.
- **Rollback**: no dedicated CLI verb. Use `railway redeploy` or the dashboard's "Redeploy" on a prior deployment. Database migrations applied by a bad deploy do not auto-revert — plan migrations to be backward-compatible for at least one release.
- **Approval**: an agent may safely run `railway up` / `railway logs` / `railway status` unattended for iteration. A human should approve any `railway variables set` touching production secrets, any Postgres plugin deletion, and the first production domain cutover.
- **Logs**: `railway logs` (tail, `--build` for build logs, `-n 100` for last N lines) gives read-only visibility; the official Remote MCP server (`mcp.railway.com`) exposes the same read operations as structured tool calls for an agent, in addition to write operations like redeploy (reserve those for human approval per the line above).

## Risk Register

| Risk | Source | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| Silent JDK version mismatch (build uses wrong Java version) | Devil's advocate | M | M | Add `.system.properties` with `java.runtime.version=21` (or a Dockerfile pinning `eclipse-temurin:21-jre`) on first setup, before writing domain code. |
| Unbounded usage-based cost from a restart loop | Devil's advocate / Pre-mortem | M | M | Set a budget alert in Railway's dashboard; configure Spring Boot Actuator health checks conservatively so a transient failure doesn't trigger a restart storm. |
| No deterministic rollback primitive | Devil's advocate / Pre-mortem | M | H | Keep every deploy tagged with a git SHA; document the manual "redeploy prior deployment" steps in the README before the first production incident, not during one. |
| Deploy silently has no public URL | Unknown unknowns | H | L | Generate the Railway domain under Networking immediately after first successful deploy, and verify it with a smoke-test `curl` before considering setup "done." |
| Official MCP server is young (~7 months) | Devil's advocate | L | M | Scope the MCP server's agent access to read-only operations (logs, status) for now; keep write operations (redeploy, variable changes) to the CLI/dashboard under human control. |
| Template drift overwriting Postgres plugin config | Unknown unknowns | L | M | Eject the Postgres template into the project repo early, per Railway's own recommendation, rather than leaving it as a managed template. |

## Getting Started

Superseded by the phased execution plan at `@context/deployment/deploy-plan.md` (written after this research, once the actual repo state was checked against it — e.g. the repo already has a root `Dockerfile` pinning `eclipse-temurin:21`, so the plan forces Railway's `DOCKERFILE` builder via `railway.json` instead of relying on `.system.properties` + Railpack auto-detection). That file tracks phase-by-phase progress with checkboxes and a running known-good-deployment log; this section is kept only as a historical pointer.

## Out of Scope

The following were not evaluated in this research:
- Docker image configuration
- CI/CD pipeline setup
- Production-scale architecture (multi-region, HA, DR)
