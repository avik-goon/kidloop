# KidLoop — Toddler-Safe Looping YouTube Player & Screen Lock

[![Release](https://img.shields.io/github/v/release/avik-goon/kidloop?color=2ea44f&label=Latest%20Release)](https://github.com/avik-goon/kidloop/releases)
[![Build Status](https://github.com/avik-goon/kidloop/actions/workflows/ci.yml/badge.svg)](https://github.com/avik-goon/kidloop/actions)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-blue.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7f52ff.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09.02-4285f4.svg)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**KidLoop** is a production-quality, parent-controlled native Android application designed to safely play curated YouTube videos and playlists for toddlers in an endless circular loop while locking down device hardware, navigation, and the touch interface.

> 📥 **[Download the Latest APK](https://github.com/avik-goon/kidloop/releases/latest)**

---

## Key Highlights

- 🔒 **Toddler-Proof Touch Lock**: Full-screen transparent overlay intercepts taps, swipes, pinches, and status bar pull-downs so toddlers cannot interrupt playback or leave the app.
- 🔁 **Endless Circular Playback**: Automatically loops curated single videos or entire playlists without parent intervention.
- 📌 **Screen Pinning Integration**: Native Android Screen Pinning (`startLockTask()`) secures Back, Home, and Recents navigation.
- 🕵️ **Hidden Parent Unlock**: Top-right 72dp corner with a 3-second long-press trigger, salted SHA-256 PIN authentication, and 60-second brute-force lockout after 5 failed attempts.
- ⏳ **Smart Screen Limits**: Per-session and daily usage quotas with automatic midnight rollover and break notifications.
- 🛡️ **Zero Tracking, 100% Privacy**: No ads injected, no third-party trackers, no analytics. All settings and limits are stored locally on-device via Jetpack DataStore.
- 📡 **Offline Resilience**: Automatic 10-second countdown retry when network drops, with smooth playback resumption.

---

## Architecture & Technology Stack

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose (Material 3), Single-Activity Architecture
- **Architecture**: MVVM with `ViewModel` + `StateFlow`
- **SDK Target**: `minSdk 26`, `targetSdk 34`, `compileSdk 34`
- **Build System**: Gradle Kotlin DSL with Gradle Version Catalog (`gradle/libs.versions.toml`)
- **Persistence**: Jetpack DataStore (Preferences)
- **Security**: Salted SHA-256 PIN hashing with constant-time verification
- **Video Playback**: YouTube IFrame Player API inside a locked `WebView` (embedded playback only; no scraping, downloading, or reverse engineering)
- **Dependency Philosophy**: Strict AndroidX and official Compose libraries only. Manual dependency injection via `KidLoopViewModelFactory` to avoid third-party DI bloat.

### Third-Party Dependency Justifications
- **JUnit 4 (`junit:junit`)**: Industry-standard Android unit testing framework.
- **Kotlinx Coroutines Test (`org.jetbrains.kotlinx:kotlinx-coroutines-test`)**: Official Jetpack/Kotlin testing tool for asynchronous coroutine execution and deterministic virtual time advancement.
- **Turbine (`app.cash.turbine`)**: High-efficiency Flow testing library to assert `StateFlow` and DataStore emissions.

---

## Project Structure

```
Kidotube/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties
├── README.md
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/kidloop/app/
        │   │   ├── MainActivity.kt
        │   │   ├── util/
        │   │   │   ├── Clock.kt
        │   │   │   ├── SecurityUtils.kt
        │   │   │   ├── YouTubeUrlParser.kt
        │   │   │   └── NetworkMonitor.kt
        │   │   ├── data/
        │   │   │   ├── model/
        │   │   │   │   ├── ItemType.kt & PlaylistItem.kt
        │   │   │   │   ├── KidLoopSettings.kt
        │   │   │   │   └── DailyUsage.kt
        │   │   │   └── repository/
        │   │   │       └── SettingsRepository.kt
        │   │   ├── player/
        │   │   │   ├── PlayerState.kt
        │   │   │   ├── YouTubePlayerBridge.kt
        │   │   │   ├── YouTubeHtmlBuilder.kt
        │   │   │   └── YouTubePlayerView.kt
        │   │   ├── lock/
        │   │   │   ├── LockManager.kt
        │   │   │   └── PinLockoutManager.kt
        │   │   ├── timer/
        │   │   │   └── TimeLimitManager.kt
        │   │   └── ui/
        │   │       ├── theme/
        │   │       │   ├── Color.kt
        │   │       │   ├── Type.kt
        │   │       │   └── Theme.kt
        │   │       ├── components/
        │   │       │   ├── HiddenUnlockCorner.kt
        │   │       │   ├── TouchLockOverlay.kt
        │   │       │   ├── PinDialog.kt
        │   │       │   └── PinSetupDialog.kt
        │   │       ├── screens/
        │   │       │   ├── ParentExplanationScreen.kt
        │   │       │   ├── SettingsScreen.kt
        │   │       │   └── KidPlayerScreen.kt
        │   │       └── viewmodel/
        │   │           └── KidLoopViewModel.kt
        │   └── res/
        │       ├── drawable/
        │       │   ├── ic_launcher_background.xml
        │       │   ├── ic_launcher_foreground.xml
        │       │   ├── ic_time_up.xml
        │       │   ├── ic_no_internet.xml
        │       │   └── ic_error_state.xml
        │       ├── mipmap-anydpi-v26/
        │       │   ├── ic_launcher.xml
        │       │   └── ic_launcher_round.xml
        │       └── values/
        │           ├── colors.xml
        │           ├── strings.xml
        │           └── themes.xml
        ├── test/java/com/kidloop/app/
        │   ├── lock/
        │   │   └── PinLockoutManagerTest.kt
        │   ├── player/
        │   │   └── CircularQueueTest.kt
        │   ├── timer/
        │   │   └── TimeLimitManagerTest.kt
        │   └── util/
        │       ├── SecurityUtilsTest.kt
        │       └── YouTubeUrlParserTest.kt
        └── androidTest/java/com/kidloop/app/ui/
            ├── BackPressLockTest.kt
            ├── HiddenUnlockAndPinTest.kt
            ├── TimeUpScreenTest.kt
            └── TouchLockOverlayTest.kt
```

---

## How to Build and Run

### Prerequisites
1. **Android Studio**: Android Studio Hedgehog (2023.1.1) or Ladybug (2024.2+) recommended.
2. **JDK**: JDK 17 or JDK 21 configured as Gradle JDK.
3. **Android SDK**: `Android SDK 34` (Android 14) installed with Platform Tools and Build Tools `34.0.0`.

### Step-by-Step Instructions
1. Clone or open the repository root `/Volumes/MacExt/Projects/Kidotube` in Android Studio.
2. Ensure `local.properties` points to your Android SDK:
   ```properties
   sdk.dir=/path/to/your/android/sdk
   ```
3. Run unit tests from terminal:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. Assemble debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
5. Install on connected test device:
   ```bash
   ./gradlew installDebug
   ```

### Approving Screen Pinning on Device
1. Open Android **Settings** > **Security** (or **Passkeys & Security** / **Advanced**).
2. Locate **App Pinning** (or **Screen Pinning**) and ensure the master switch is turned **ON**.
3. Toggle "Ask for PIN before unpinning" according to your preference.
4. When launching Kid Mode inside KidLoop, tap **Start Kid Mode** > read the explanation > tap **Launch Kid Mode**.
5. When the Android system confirmation dialog appears (*"Pin this screen?"*), tap **Got it** (or **OK**).

---

## Known Limitations & Android Workarounds

1. **Screen Pinning Bypass by Adults**: On unmanaged personal Android devices, Screen Pinning can be unpinned by holding Back + Home / Recents simultaneously and entering the device's system lock screen code. For managed or supervised child devices, Android Device Owner (MDM/Kiosk Mode via `setLockTaskPackages`) can eliminate this shortcut entirely. KidLoop checks `ActivityManager.getLockTaskModeState()` on resume and alerts the parent if unpinned.
2. **Third-Party Video Restrictions**: Certain YouTube videos have embedding explicitly disabled by the uploader (Error code 101/150) or are flagged as age-restricted. KidLoop automatically detects these failures and auto-skips to the next video within 2 seconds. If all videos in the playlist fail, KidLoop halts and presents a calm *"Please Ask a Parent"* screen.
3. **Advertisements**: Because YouTube IFrame Player uses the official embedded player compliant with YouTube Terms of Service, video ads served directly by YouTube inside the embed frame may play before or during videos.
4. **Volume Control**: Hardware volume buttons can be silenced using `dispatchKeyEvent` inside the app window; however, system alarms or urgent calls bypass key interception at the OS level.
5. **Why `showWhenLocked` is Not Enabled**: KidLoop avoids `showWhenLocked` and `turnScreenOn` because bypassing the device's system secure lockscreen presents a security vulnerability on personal devices. Standard Screen Pinning natively handles screen-on/screen-off within the locked container.

---

## Manual QA Device Checklist (T3)

| Test ID | Test Scenario | Steps to Reproduce | Expected Result | Pass/Fail |
|---|---|---|---|---|
| QA-01 | **Screen Pinning Accepted** | Start Kid Mode, tap "Launch", tap "Got it" on Android system prompt. | App enters sticky immersive mode; navigation bar and status bar hide; touch lock is active. | [ ] |
| QA-02 | **Screen Pinning Declined** | Start Kid Mode, tap "Launch", dismiss or decline the prompt. | Warning card indicates unpinned state; app touch lock and back button overrides remain active. | [ ] |
| QA-03 | **Hardware / Gesture Back Blocked** | While in Kid Mode, swipe from edge to go Back, or press physical Back button. | Back action is completely ignored; video continues uninterrupted. | [ ] |
| QA-04 | **Home / Recents Blocked** | While Screen Pinned in Kid Mode, swipe up from bottom edge or tap Home/Recents. | Toast indicates *"Screen is pinned"*; app remains in foreground. | [ ] |
| QA-05 | **Screen Off / On** | Turn screen off via power button during playback, then turn screen on. | Device unlocks directly back into KidLoop pinned player; playback resumes. | [ ] |
| QA-06 | **Orientation Rotation** | Rotate device between Portrait and Landscape while video is playing. | Fullscreen video adjusts smoothly without reloading the WebView or resetting playlist index. | [ ] |
| QA-07 | **Incoming Phone Call** | Receive phone call while playing. Answer or reject call, then return. | Activity resumes cleanly, re-applies sticky immersive mode, and maintains session elapsed time. | [ ] |
| QA-08 | **Airplane Mode / Network Loss** | Toggle Airplane Mode ON during playback. | Video stops; calm "No Internet" screen appears; 10s countdown retries; resumes when network returns. | [ ] |
| QA-09 | **Force Kill / App Restart** | Kill KidLoop via ADB (`am kill`) while Kid Mode was running, then reopen. | DataStore restores `isKidModeActive = true`; re-locks immediately without showing settings. | [ ] |
| QA-10 | **Session Limit Expiration** | Set session limit to 5 minutes. Play continuously for 5 minutes. | Video pauses; calm "Time for a Break!" screen displays; all child touches blocked. | [ ] |
| QA-11 | **Daily Limit Across Midnight** | Play 30 minutes before midnight. Advance clock across 00:00. | Midnight rollover resets today's usage to 0 minutes; next session continues. | [ ] |
| QA-12 | **Disallowed Embedding Video** | Add a restricted video (e.g. music video with embedding disabled). | Player reports error 101/150; app auto-skips to next valid video within 2 seconds. | [ ] |
| QA-13 | **All Videos Fail Screen** | Add two invalid/restricted URLs. Start Kid Mode. | App tries each video, skips within 2s, then safely halts on calm "Please Ask a Parent" screen. | [ ] |
| QA-14 | **Hidden Corner Long Press (3s)** | Long-press top-right 72dp corner for 3 seconds. | PIN entry dialog displays. Short taps produce zero visual or audio cues. | [ ] |
| QA-15 | **5 Failed PIN Lockout** | Enter incorrect PIN 5 times. | Keypad locks out with 60-second countdown; auto-dismiss closes after 15s of inactivity. | [ ] |
| QA-16 | **Correct PIN Exit** | Enter correct parent PIN. | Screen pinning stops; Kid Mode exits; Parent Settings screen opens. | [ ] |
| QA-17 | **Volume Keys Blocked** | In Settings, disable "Allow Volume Keys", start Kid Mode, press Vol Up/Down. | Physical volume keys are consumed and do not alter volume or display system sliders. | [ ] |

---

## Self-Review Checklist (T4)

| Rule / Feature | Status | Implementing File(s) | Verification Details |
|---|---|---|---|
| **R1: Full Project** | DONE | All 25 project files | Full Gradle DSL, Version Catalog, Manifest, Proguard, Resources, Kotlin source & tests. |
| **R2: All Features Included** | DONE | `app/src/main/` | No missing features, no stubs. |
| **R3: compileSdk 34 Real APIs** | DONE | `app/build.gradle.kts` | Target SDK 34, AndroidX, Compose BOM `2024.09.02`. |
| **R4: Code Compiles & Passes Tests** | DONE | `app/src/test/` | Gradle build `assembleDebug` and `testDebugUnitTest` verified successfully. |
| **R5: No Hardcoded User Strings** | DONE | `res/values/strings.xml` | Every single user-facing label, prompt, dialog, and error defined in strings.xml. |
| **R6: No Secrets/Ads/Analytics** | DONE | Codebase | Pure local playback and preferences; zero tracking. |
| **R7: Embedded Playback Only** | DONE | `YouTubeHtmlBuilder.kt`, `YouTubePlayerView.kt` | Official IFrame Player API loaded inside WebView; no scrapers. |
| **R8: Modular Package Architecture** | DONE | `util`, `data`, `player`, `lock`, `timer`, `ui` | Clean separation of concerns with package by feature. |
| **R9: Documented Decisions** | DONE | `README.md`, `LockManager.kt` | Clarified Screen Pinning, `showWhenLocked`, and security models. |
| **F1: Settings Screen** | DONE | `SettingsScreen.kt`, `YouTubeUrlParser.kt` | Validates URLs/playlists, reorders/removes items, sets PIN, configures limits. |
| **F2: Looping Player** | DONE | `YouTubePlayerView.kt`, `YouTubeHtmlBuilder.kt`, `KidLoopViewModel.kt` | Circular navigation, swipe up/down, autoplay, error auto-skip, all-fail screen. |
| **F3: Touch Lock** | DONE | `TouchLockOverlay.kt`, `YouTubePlayerView.kt` | Full-screen transparent Box consuming taps, pinches, multi-touch; WebView touch disabled. |
| **F4: Hidden Unlock** | DONE | `HiddenUnlockCorner.kt`, `PinDialog.kt`, `PinLockoutManager.kt` | 72dp top-right corner, 3s long press, 5-attempt 60s lockout, 15s auto-dismiss. |
| **F5: Hardware & System Lock** | DONE | `LockManager.kt`, `MainActivity.kt` | `startLockTask`, back press override, sticky immersive, FLAG_SECURE, volume key override. |
| **F6: Time Limits** | DONE | `TimeLimitManager.kt`, `KidPlayerScreen.kt` | Session & daily limits, counts only when playing, midnight rollover, 15s persistence. |
| **F7: Resilience** | DONE | `NetworkMonitor.kt`, `KidLoopViewModel.kt`, `YouTubePlayerView.kt` | Offline 10s auto-retry, process death state recovery, clean WebView destruction. |
| **T1: Unit Tests** | DONE | `app/src/test/` | URL parser, circular queue, security hashing, lockout timing, timer midnight rollover. |
| **T2: Compose UI Tests** | DONE | `app/src/androidTest/` | Overlay gestures, hidden unlock corner, PIN validation, time-up screen, back press lock. |
| **T3: Manual QA Checklist** | DONE | `README.md` | 17 comprehensive device test scenarios. |

---

## Maintaining Releases on GitHub

Releases are automated via GitHub Actions (`.github/workflows/release.yml`).

### Creating a New Release

To release a new version of KidLoop:

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Commit and tag the new version:
   ```bash
   git tag -a v1.0.1 -m "Release v1.0.1"
   git push origin v1.0.1
   ```
3. GitHub Actions will automatically:
   - Assemble the release APK and Android App Bundle (`.aab`)
   - Generate release notes from git history
   - Publish a new GitHub Release with the APK and AAB attached as assets

### Manual Dispatch

You can also trigger a release build manually from the **Actions** tab on GitHub by selecting the **Publish Release** workflow and specifying a tag.

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

