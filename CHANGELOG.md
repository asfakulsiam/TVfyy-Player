# Changelog

All notable changes to the **TVfyy Player** project are documented in this file.

---

## [1.1.5] - 2026-08-27

### Added
- **Top Events Live Carousel & Multi-Server Selector**: Added live sports and special broadcast events carousel on the Home dashboard with multi-server stream switching modal (`EventServerSheet`), auto-fetching from GitHub (`top-events.xml`) with local asset fallback.
- **Automated GitHub Playlist Sync & Change Detection**: Default `playlist.m3u` is automatically initialized from GitHub with version tracking. When updates are detected on GitHub, a prominent sync badge appears on the Home header.
- **Sync Confirmation & Favorite Reset Warning Dialog**: Created `SyncPlaylistDialog` informing users that syncing will refresh the channel database and reset old favorite markers.
- **Bento Quick Actions Grid**: Added 4 quick action cards on Home (Local File, Direct URL Stream, Playlists Hub, Starred Favorites).
- **Categorized Live Channels Explorer**: Integrated real-time live channel browsing by genre (Sports, News, Bangla, Hindi, English, Kids, etc.) with instant search filtering, channel logos, live indicators, and favorite toggles.

### Fixed
- **Status Bar & System Navigation Bar Overlap**: Applied edge-to-edge `statusBarsPadding()` and `navigationBarsPadding()` across all player overlay controls, floating unlock buttons, top header bars, and destination screens.
- **Gesture Conflict on Active Video Player**: Resolved touch event consumption between playback swipe gestures (brightness/volume/seek) and interactive player controls.
- **Navigation Bar Ergonomics**: Streamlined bottom navigation bar to 4 clean primary tabs (Home, Playlists, Stream, Settings), moving advanced settings to the Settings hub.

---

## [1.1.4] - 2026-08-27

### Added
- **In-App Downloader Modal**: Replaced external browser redirection with an internal background downloader modal that streams the update APK directly inside the app.
- **Live Download Progress & Telemetry**: Displays real-time download progress with an animated linear progress bar, byte indicators (e.g. `8.2 MB / 17.5 MB`), percentage, and live download speed (e.g. `2.8 MB/s`).
- **Immediate In-App APK Installation**: Once the download completes, the modal transitions to the installation screen featuring prominent **"Install Now"** and **"Cancel"** action buttons.
- **Unknown Sources Permission Flow**: Integrated detection for Android 8.0+ `REQUEST_INSTALL_PACKAGES` permission (`canRequestPackageInstalls()`). If permission is required, displays an informative prompt with a direct shortcut to open Android's "Install unknown apps" settings.
- **FileProvider Integration**: Configured secure `FileProvider` authorities (`${applicationId}.fileprovider`) and `file_paths.xml` for package installer intent dispatching.
- **Download Resilience & Browser Fallback**: Added cancel, retry, and secondary "Download via Browser" fallback capabilities in case of connectivity errors.

---

## [1.1.3] - 2026-08-27

### Fixed
- **In-App Auto-Updater Detection**: Resolved update detection failure caused by tag/release version discrepancy (`v1.0.10` vs `1.1.2`).
- **Semantic Version Intelligence**: Enhanced remote version parser to extract and compare semantic versions across GitHub release tags, titles, and changelog headers (`tag_name`, `name`, `body`).
- **CI/CD GitHub Release Tag Alignment**: Updated `.github/workflows/build-and-release.yml` to automatically extract the real version name from Gradle and tag GitHub releases with the exact version (e.g. `v1.1.3`).
- **Auto-Popup Display on App Launch**: Ensured that the update popup dialog appears immediately after splash screen completion whenever an update is available.

---

## [1.1.2] - 2026-08-27

### Added
- **Automated GitHub Release Notes Publishing**: CI/CD pipeline now automatically extracts and formats the full "What's New" release notes from `CHANGELOG.md` directly onto the GitHub Releases page, ensuring clean release notes without cluttering Git commit messages.
- **Direct In-App Release Notes Integration**: In-app updater dynamically pulls release notes directly from GitHub Releases to show users a changelog popup when an update is available.

### Changed
- **Streamlined Settings UI**: Removed the manual "Connected Repository" configuration UI. Repository tracking is now seamlessly pre-configured to `asfakulsiam/TVfyy-Player` with dynamic launch-time auto-checking and manual check triggers.

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
