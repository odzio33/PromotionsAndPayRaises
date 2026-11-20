# Lessons Learned

> Append-only register of recurring rules and patterns. Re-read at start by /10x-frame, /10x-research, /10x-plan, /10x-plan-review, /10x-implement, /10x-impl-review.

## Add a formatter/linter at bootstrap time, before writing domain code

- **Context**: Bootstrapping a new backend (Java/Maven) — formatter/linter setup.
- **Problem**: The Spring starter (curl to start.spring.io) added no formatter/linter. Adding Spotless only after the full domain/controllers/tests were already written forced one large reformatting commit across 21 files instead of each file arriving in its final style from the start.
- **Rule**: Before writing any domain code after bootstrapping a new backend, add and run the formatter/linter (e.g. Spotless) first, so every file is written in its final style from the start.
- **Applies to**: bootstrapper, implement

## Check railway logs for the actual bound port before setting a domain's --port

- **Context**: Configuring a public domain on Railway (`railway domain`), for any service.
- **Problem**: Both services returned 502. We assumed Railway would use the Dockerfile's port (`EXPOSE 8081` / `ENV PORT 5173`) and set `railway domain --port` accordingly. Railway injected its own dynamic `PORT=8080` for both services instead, so the domain pointed at a port nothing was listening on — 502 on both backend and frontend until we checked `railway logs` and fixed the domain's port.
- **Rule**: Never assume the Dockerfile's `EXPOSE`/`ENV PORT` default is the port the app actually binds to on Railway — Railway injects its own dynamic `PORT`. Always check `railway logs` for the actually-bound port before (or immediately after) running `railway domain --port`, and fix the domain's target port to match.
- **Applies to**: implement
