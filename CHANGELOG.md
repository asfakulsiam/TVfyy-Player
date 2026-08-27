# Changelog

All notable changes to the **TVfyy Player** project are documented in this file.

---

## [1.1.1] - 2026-08-27

### Added
- **Automated GitHub Release APK & AAB Deployment**: Configured `.github/workflows/build-and-release.yml` to automatically extract, package, and publish signed release APKs and AABs directly to GitHub Releases.
- **In-App Auto-Updater**: Automatic update check on app launch that fetches the latest release from the configured GitHub repository (`asfakulsiam/TVfyy-Player`).
- **Release Changelog Dialog**: The update prompt displays the release title, new version tag, APK download size, and the full "What's New" changelog from GitHub release notes.
- **Update Action Controls**: Integrated direct "Download Now" (one-tap APK download) and "Remind Me Later" (24-hour snooze interval) options.
- **Persistent Agent Project Rules**: Created `AGENTS.md` and `GEMINI.md` mandating continuous version bumping and changelog tracking for every codebase modification.

### Fixed
- **GitHub Actions Keystore Preparation**: Resolved the CI/CD failure where shell expansion broke the Base64 keystore decoding. Replaced with robust Python-based decoder with padding auto-correction and keytool validation.
- **Gradle CI Cache Resilience**: Updated to `gradle/actions/setup-gradle@v4` with cache resilience to prevent GitHub Actions infrastructure 400/503 cache outages from halting builds.

---

## [1.1.0] - 2026-08-27

### Added
- **Production Keystore Signing Pipeline**: Configured release signing using `tvfyy-player-release.jks` with alias `tvfyy_player_release`.
- **Multi-Format Media Engine**: ExoPlayer / Media3 streaming engine supporting HLS (`.m3u8`), DASH (`.mpd`), MP4, MKV, MP3, and TS streams.
- **Subtitle System**: Embedded and external subtitle parser supporting SRT, VTT, and ASS formats.
- **Channel & Playlist Manager**: Custom M3U playlist parsing, channel group categorization, and favorites.
- **URL & Stream Authentication Profiles**: User-Agent headers, Referer injection, and custom token authentication.
