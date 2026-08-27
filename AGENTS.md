# Project Rules & Instructions for AI Agents

## Mandatory Rules on Every Codebase Modification:

1. **Version Bumping (build.gradle.kts)**:
   - Whenever you touch the codebase to fix, improve, or add any features, **YOU MUST** increment the version name and version code in `app/build.gradle.kts` (e.g., `1.1.1` -> `1.1.2`, versionCode calculated accordingly).

2. **Changelog Maintenance (CHANGELOG.md)**:
   - **YOU MUST** describe and document the exact changelog in `CHANGELOG.md` under the newly bumped version header with date and categorized bullet points (e.g. `Added`, `Changed`, `Fixed`).

3. **In-App Updater & Changelog Display**:
   - The app **MUST** automatically fetch the updater from GitHub (`asfakulsiam/TVfyy-Player`) on app open.
   - If a new version is available:
     - The app **MUST** show the Update Dialog containing the exact changelog/release notes from the release.
     - The dialog **MUST** feature a **"Download Now"** button (to directly download the APK) and a **"Remind Me Later"** button (to snooze for 24 hours).
   - The user can also trigger manual update checks anytime from Settings.

4. **Production Keystore & CI/CD Integrity**:
   - Never generate a new production keystore or overwrite the existing `tvfyy-player-release.jks`.
   - Ensure the `.github/workflows/build-and-release.yml` maintains proper Base64 decoding from GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) and automated APK release publication.
