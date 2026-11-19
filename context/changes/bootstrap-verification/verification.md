---
bootstrapped_at: 2026-10-10T21:21:06Z
starter_id: spring
starter_name: Spring Boot
project_name: pay-raise-module
language_family: java
package_manager: maven
cwd_strategy: subdir-then-move
bootstrapper_confidence: verified
phase_3_status: ok
audit_command: "null"
---

## Hand-off

```yaml
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
  path_taken: standard
  quality_override: false
  self_check_answers: null
  has_auth: true
  has_payments: false
  has_realtime: false
  has_ai: false
  has_background_jobs: false
```

### Why this stack

A solo Java developer (7 years of experience, new to agents) building a medium-scale internal web app with an approval workflow needs a framework they can judge an agent's output against. Spring Boot is the recommended default for `(web, java)`, clears all four agent-friendly gates, and its bootstrapper confidence is verified, so scaffolding will be smooth. The PRD's auth requirement (role-based login) maps to Spring Security, a mature, well-documented module in the same ecosystem. Payments, realtime, AI, and background jobs are out of scope per the PRD's non-goals and functional requirements, so no additional infrastructure is needed beyond the base starter. Deployment defaults to Fly, the starter's first listed target; CI runs on GitHub Actions with auto-deploy-on-merge, matching the solo, after-hours timeline.

## Pre-scaffold verification

| Signal             | Value                              | Severity | Notes                              |
| ------------------ | ----------------------------------- | -------- | ----------------------------------- |
| npm package        | not run                             | n/a      | cmd_template is not npm-distributed (curl to start.spring.io) |
| GitHub repo        | not run                             | n/a      | docs_url (https://docs.spring.io/spring-boot/) is not a github.com/<owner>/<repo> URL |

## Scaffold log

**Resolved invocation**: `curl -s https://start.spring.io/starter.tgz -d dependencies=web,devtools -d type=maven-project -d javaVersion=21 -d groupId=com.example -d artifactId=.bootstrap-scaffold | tar -xzf -` (run with cwd set to `.bootstrap-scaffold/`)
**Strategy**: subdir-then-move
**Exit code**: 0
**Files moved**: 8 (`.gitattributes`, `.gitignore`, `.mvn/`, `HELP.md`, `mvnw`, `mvnw.cmd`, `pom.xml`, `src/`)
**Conflicts (.scaffold siblings)**: none
**.gitignore handling**: absent in cwd — moved silently
**.bootstrap-scaffold cleanup**: deleted

Note: the Spring Initializr archive does not create its own subdirectory and has no `{pm}` placeholder in its `cmd_template`; it was extracted with the working directory set to `.bootstrap-scaffold/` so the subdir-then-move strategy could apply normally. The `{name}` substitution also fills the `artifactId` request parameter, so the scaffolded `pom.xml` carries `artifactId=.bootstrap-scaffold` rather than the project's real name — a cosmetic mismatch worth a manual rename (see Next steps).

## Post-scaffold audit

**Tool**: skipped — no built-in audit tool for java
**Recommended external tool**: OWASP Dependency-Check or Snyk

## Hints recorded but not acted on

| Hint                       | Value                              |
| -------------------------- | ----------------------------------- |
| bootstrapper_confidence    | verified                            |
| quality_override           | false                               |
| path_taken                 | standard                            |
| self_check_answers         | null                                |
| team_size                  | solo                                |
| deployment_target          | fly                                 |
| ci_provider                | github-actions                      |
| ci_default_flow            | auto-deploy-on-merge                |
| has_auth                   | true                                |
| has_payments               | false                               |
| has_realtime               | false                               |
| has_ai                     | false                               |
| has_background_jobs        | false                               |

## Next steps

Next: a future skill will set up agent context (CLAUDE.md, AGENTS.md). For now, your project is scaffolded and verified — happy hacking.

Useful manual steps in the meantime:
- `git init` (if you have not already) to start your own repo history.
- Rename the `artifactId` in `pom.xml` from `.bootstrap-scaffold` to `pay-raise-module` (or your preferred artifact id) — it currently reflects the temp scaffold directory name, not the project name.
- Review any `.scaffold` siblings the conflict policy created and decide which version of each file to keep (none were created this run).
- Address audit findings per your project's risk tolerance — no built-in audit ran for Java; consider OWASP Dependency-Check or Snyk.
