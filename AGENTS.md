# KidLoop Agent Mandate (AGENTS.md)

This document is the authoritative instruction manual and operational mandate for all AI agents (Antigravity, Claude Code, Cursor, Copilot, Windsurf, etc.) working on the **KidLoop** codebase.

---

## 1. Prime Directives & Non-Negotiables

Every agent operating on this repository **MUST** adhere to the following core tenets:

1. **Child Safety & Screen Lockdown First**:
   - The primary purpose of KidLoop is to prevent toddlers from leaving the app, accessing unintended content, or altering device settings.
   - Never remove or weaken `LockManager`, `TouchLockOverlay`, `HiddenUnlockCorner`, or `PinLockoutManager` logic.
   - Screen Pinning (`startLockTask()`) and sticky immersive mode must remain rock-solid.
2. **Strict YouTube ToS Compliance**:
   - Videos must play **only** via the official YouTube IFrame Player API loaded inside a sandboxed `WebView` (`YouTubeHtmlBuilder.kt`, `YouTubePlayerView.kt`).
   - **NEVER** introduce video scrapers, downloaders, background audio extractors, or unofficial YouTube APIs.
   - Respect creator embedding permissions: gracefully handle error codes (101/150) by auto-skipping.
3. **100% Privacy & Zero Tracking**:
   - No third-party analytics, tracking SDKs, ad networks, or telemetry are permitted.
   - All configurations, playlist items, and usage statistics are strictly local on-device via Jetpack DataStore.
4. **Offline Resilience**:
   - Network interruptions must never crash the player. The app must display a calm countdown screen and resume automatically when connectivity returns.

---

## 2. Technical Stack & Architectural Boundaries

- **Language**: 100% Kotlin with strict Gradle Kotlin DSL (`.gradle.kts`).
- **UI Framework**: Modern Jetpack Compose with Material 3. Single-Activity architecture (`MainActivity.kt`).
- **Architecture**: Clean MVVM with `ViewModel` + `StateFlow`. UI components must only observe state and emit parent-delegated events.
- **Dependency Philosophy**:
  - Keep the app lean (~1.3 MB APK size).
  - Use official AndroidX and Jetpack Compose libraries only.
  - **No Heavy DI Frameworks**: Do NOT add Dagger, Hilt, or Koin. Use manual dependency injection via `KidLoopViewModelFactory`.
  - All dependency additions or version bumps **MUST** be made in `gradle/libs.versions.toml` (Gradle Version Catalog).
- **Target Specifications**:
  - `minSdk = 26` (Android 8.0 Oreo)
  - `targetSdk = 34` (Android 14)
  - `compileSdk = 34`

---

## 3. Security & Sensitive Data Mandate

- **Salted SHA-256 PIN Verification**:
  - Parent PINs must always be hashed with random cryptographic salt and verified using constant-time comparison (`SecurityUtils.kt`). Never store or log PINs in plain text.
- **Brute-Force Lockout**:
  - 5 consecutive failed PIN attempts must trigger a 60-second lockout (`PinLockoutManager.kt`).
  - Unlock dialog must auto-dismiss after 15 seconds of inactivity.
- **Secrets Protection & Version Control**:
  - **NEVER** commit `keystore.properties`, `*.jks`, `*.keystore`, or `local.properties`.
  - Always verify `.gitignore` before adding new file patterns.
  - Production signing secrets must only be managed via GitHub Actions Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, etc.).

---

## 4. Coding Standards & Conventions

1. **Zero Hardcoded Strings**:
   - All user-facing strings, buttons, errors, dialog titles, and accessibility content descriptions **MUST** reside in `app/src/main/res/values/strings.xml`.
2. **Theme Consistency**:
   - Use theme tokens from `ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`). Never hardcode hex color literals in Compose UI screens.
3. **Immutability & Reactive Flow**:
   - Keep models in `data/model/` immutable (`data class` with `val`).
   - State flows must be exposed as read-only `StateFlow<T>` via `.asStateFlow()`.
4. **Resource Management**:
   - WebViews and coroutine jobs must be cleanly cancelled and disposed in lifecycle callbacks (`onDestroy`, `DisposableEffect`) to prevent memory leaks.

---

## 5. Verification & Testing Protocol

Before proposing, committing, or pushing any code changes, agents **MUST** execute and verify:

1. **Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```
   All tests in `app/src/test/` must pass without failures.
2. **Release Build Assembly**:
   ```bash
   ./gradlew assembleRelease
   ```
   Must compile cleanly with ProGuard/R8 shrinking enabled.
3. **New Features Require Tests**:
   - Any new calculation, URL parsing pattern, or security routine requires an accompanying unit test in `app/src/test/`.
   - UI gesture interactions should be covered in `app/src/androidTest/`.

---

## 6. Token Efficiency & Tooling Mandates

### RTK - Rust Token Killer
Execute CLI commands using `rtk` proxy when possible for 60–90% token savings:
- Use `rtk git status`, `rtk git diff`, etc.
- Meta commands: `rtk gain`, `rtk discover`.

### MCP Tools: code-review-graph
When analyzing relationships or reviewing changes, prioritize `code-review-graph` MCP tools over raw Grep/Glob scans:
- **Code exploration**: `semantic_search_nodes` or `query_graph`.
- **Change blast radius**: `get_impact_radius` or `get_affected_flows`.
- **Code review**: `detect_changes` + `get_review_context`.

---

## 7. Release & Deployment Protocol

Releases are fully automated via GitHub Actions (`.github/workflows/release.yml`).

When authorized to publish a new release:
1. Update `versionCode` (integer increment) and `versionName` (semantic version string) in `app/build.gradle.kts`.
2. Commit changes with a conventional commit message:
   ```bash
   git commit -am "chore(release): bump version to vX.Y.Z"
   ```
3. Create an annotated git tag and push:
   ```bash
   git tag -a vX.Y.Z -m "Release vX.Y.Z"
   git push origin main --tags
   ```
4. GitHub Actions will automatically compile the signed APK + AAB and publish the GitHub Release with generated release notes.
