@AGENTS.md

# CLAUDE.md — Agent Guidelines & Commands

Refer to [AGENTS.md](AGENTS.md) for full project architecture, security rules, and development mandates.

## Quick CLI Reference (RTK Token Optimization)

```bash
# Testing & Verification
./gradlew testDebugUnitTest       # Run all unit tests
./gradlew assembleRelease         # Verify production release build
./gradlew assembleDebug           # Build debug APK

# Git & RTK Proxy
rtk git status                    # Token-optimized status check
rtk git diff                      # Token-optimized diff
rtk gain                          # Check token savings
```
