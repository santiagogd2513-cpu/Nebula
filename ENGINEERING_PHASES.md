# Nebula engineering audit and 18-phase plan

This plan tracks work that can be verified from the repository and GitHub Actions. A phase is complete only after its stated checks pass. Device-only behavior is explicitly marked as requiring a physical Android test.

## Initial audit findings

- GitHub Actions run 11 passed unit tests, debug APK assembly, release publication, and artifact upload for commit 0cda7d3.
- The runtime adapter is intentionally non-functional until MojoLauncher and its native submodules are integrated; the Play button remains disabled.
- Microsoft/Ely.by OAuth and catalog installation are not implemented.
- The Modrinth image loader previously accepted arbitrary URLs and had an unbounded task queue / no response-size limit.
- The project details screen needed a non-empty label for version-only dependencies.
- The project model contained a redundant JSON fallback expression.

## Phases

1. **Audit and network/image hardening** — HTTPS Modrinth image host allowlist, bounded downloads/queue, bitmap sampling/cache, input validation, and dependency fallback labels. Verify with unit tests and APK build.
2. **Catalog model tests** — test search/detail JSON parsing and dependency/gallery handling.
3. **Catalog pagination correctness** — prevent stale page responses and duplicate concurrent pagination; verify edge cases.
4. **Search UX** — submit from keyboard, preserve filters, clear empty/error/loading states.
5. **Project details UX** — author, dependency types, gallery captions, and long descriptions.
6. **Lifecycle safety** — prevent stale asynchronous callbacks from mutating a replaced screen.
7. **Account storage audit** — validate token identifiers and payload boundaries; test malformed/corrupt records where practical.
8. **Local profile persistence** — harden SharedPreferences set handling and add tests for profile identity rules.
9. **Accessibility pass** — content descriptions, focus, touch targets, and scalable text.
10. **Android configuration audit** — manifest, backup policy, network policy, and target SDK behavior.
11. **Catalog error handling** — rate limits, timeouts, malformed JSON, and retry messaging.
12. **Dependency metadata clarity** — represent project, version-only, optional, required, incompatible, and embedded dependencies accurately.
13. **Release pipeline hardening** — verify artifact names, unique versions, and release behavior.
14. **Build reproducibility** — add and validate a Gradle wrapper if the official wrapper artifacts can be obtained safely.
15. **Runtime integration readiness** — pin and document MojoLauncher upstream revision, submodules, native toolchain requirements; no fake launch implementation.
16. **Installation design review** — define hash verification, path traversal prevention, atomic writes, and rollback before enabling installs.
17. **Documentation and privacy review** — update status, data handling, and known limitations.
18. **Final regression pass** — run full unit tests and APK build; verify latest GitHub Release.

## Completion record

- Phase 1: implemented; CI verification pending at commit creation.
- Phases 2–18: not started.
