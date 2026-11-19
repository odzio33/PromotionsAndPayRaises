# Lessons Learned

> Append-only register of recurring rules and patterns. Re-read at start by /10x-frame, /10x-research, /10x-plan, /10x-plan-review, /10x-implement, /10x-impl-review.

## Add a formatter/linter at bootstrap time, before writing domain code

- **Context**: Bootstrapping a new backend (Java/Maven) — formatter/linter setup.
- **Problem**: The Spring starter (curl to start.spring.io) added no formatter/linter. Adding Spotless only after the full domain/controllers/tests were already written forced one large reformatting commit across 21 files instead of each file arriving in its final style from the start.
- **Rule**: Before writing any domain code after bootstrapping a new backend, add and run the formatter/linter (e.g. Spotless) first, so every file is written in its final style from the start.
- **Applies to**: bootstrapper, implement
