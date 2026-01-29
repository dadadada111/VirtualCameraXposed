# GitHub CI/CD Build Process Guide

This document outlines the automated build and deployment process for the **VirtualCameraXposed** project using GitHub Actions. It is designed to be easily readable by both developers and AI assistants to understand how to trigger builds, troubleshoot issues, and retrieve artifacts.

## 1. Workflow Overview

The project uses **GitHub Actions** to automatically compile the Android APK whenever code is pushed to the repository.

*   **Workflow File**: `.github/workflows/android.yml`
*   **Trigger**: Push to `main` or `master` branches.
*   **Environment**: Ubuntu (latest)
*   **Java Version**: JDK 17
*   **Gradle Version**: 8.0.2
*   **Artifact**: `app-debug.apk` (Signed with debug keystore)

## 2. Triggering a Build

To trigger a new build and APK generation, simply push any changes to the remote repository.

### Command Line
```bash
# 1. Add modified files
git add .

# 2. Commit with a descriptive message
git commit -m "Your feature or fix description"

# 3. Push to GitHub (This triggers the Action)
git push origin main
```

### Force Rebuild (Without Code Changes)
If you need to rebuild without changing code (e.g., to retry a failed network request), you can create an empty commit:
```bash
git commit --allow-empty -m "Trigger rebuild"
git push origin main
```

## 3. Build Steps (Internal Logic)

The automated workflow performs the following steps sequentially:

1.  **Checkout Code**: Pulls the latest code from the repository (`actions/checkout@v4`).
2.  **Setup Java**: Installs JDK 17 (`actions/setup-java@v4`).
3.  **Setup Gradle**: Configures Gradle 8.0.2 (`gradle/actions/setup-gradle@v3`).
4.  **Clean Build**: Runs `gradle clean` to remove cached build artifacts and ensure a fresh compilation.
5.  **Assemble Debug**: Runs `gradle assembleDebug --no-daemon --stacktrace --info`.
    *   *Note*: We use `assembleDebug` instead of `assembleRelease` to utilize the default Android debug keystore for automatic signing.
    *   `--stacktrace --info`: Enables detailed logging for debugging build failures.
6.  **Upload Artifact**: Uploads the generated APK (`app/build/outputs/apk/debug/app-debug.apk`) as a download artifact named `app-debug`.

## 4. Retrieving the APK

After the build completes (usually 2-5 minutes):

1.  Go to the **Actions** tab in the GitHub repository.
2.  Click on the latest workflow run (top of the list).
3.  Scroll down to the **Artifacts** section.
4.  Click on **app-debug** to download the ZIP file.
5.  Extract the ZIP to get the `app-debug.apk`.

## 5. Troubleshooting Common Issues

| Issue | Symptom | Solution |
|-------|---------|----------|
| **Build Failed (Gradle Error)** | Red ❌ in Actions, log shows `Gradle version X does not exist` | Check `android.yml` and ensure `gradle-version` is set to a valid full version (e.g., `8.0.2` not `8.0`). |
| **Lint Error** | Build fails with `Lint found errors` | `build.gradle` is configured with `abortOnError false` to ignore this, but check logs if new strict rules appeared. |
| **Resource Missing** | `Aapt2Exception: resource not found` | Ensure all referenced resources (like `@mipmap/ic_launcher`) exist in `src/main/res`. Run `git add` to ensure new files are tracked. |
| **APK Not Signed** | "PackageInfo is null" on install | Ensure workflow runs `assembleDebug` (auto-signed) not `assembleRelease` (unsigned). |
| **UI Not Updating** | APK installs but shows old UI | The workflow now includes `gradle clean`. Ensure you are downloading the *latest* artifact, not an old one. |

## 6. Project Structure for AI Context

When asking an AI to modify the project, refer to these key files:

*   **Manifest**: `app/src/main/AndroidManifest.xml` (Permissions, Activity registration)
*   **UI Layout**: `app/src/main/res/layout/activity_main.xml`
*   **Main Logic**: `app/src/main/java/com/example/virtualcamera/MainActivity.java`
*   **Xposed Module**: `app/src/main/java/com/example/virtualcamera/VirtualCameraModule.java`
*   **Build Config**: `app/build.gradle` & `.github/workflows/android.yml`
