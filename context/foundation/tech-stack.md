---
starter_id: spring
package_manager: maven
project_name: pay-raise-module
hints:
  language_family: java
  team_size: solo
  deployment_target: fly
  ci_provider: github-actions
  ci_default_flow: auto-deploy-on-merge
  bootstrapper_confidence: verified
  path_taken: custom
  quality_override: false
  self_check_answers:
    typed: true
    from_official_starter: true
    conventions: true
    docs_current: true
    can_judge_agent: true
  has_auth: true
  has_payments: false
  has_realtime: false
  has_ai: false
  has_background_jobs: true
---

## Why this stack

A solo Java developer building a medium-scale, 3-week after-hours MVP with login, role-based access (employee/manager), an approval workflow, and status-change notifications is a strong fit for Spring Boot as the backend: it's the verified default for `(web, java)`, bundles DI, Spring Security, and Spring Data JPA (PostgreSQL per stated preference) with strong conventions, and clears all four agent-friendly gates. The frontend is a separate Vite+React TypeScript project — chosen because the user prefers React despite no prior experience with it, and the registry has no single starter combining a Java backend with a React frontend, so this is a custom two-project split rather than the single-starter recommended default. Both starters are individually verified, so scaffolding each should be smooth. `has_auth` and `has_background_jobs` are set (login/roles from Access Control; status-change notifications as a scheduled/queued task); payments, realtime, and AI are out of scope per the PRD's explicit non-goals. Backend deploys to Fly.io (starter default); CI runs on GitHub Actions with auto-deploy-on-merge, appropriate for a solo build against the December hard deadline. The five-point self-check came back clean, so no quality-gate compensation is needed.
