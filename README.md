# Goat Desk

Farm animal logging app for goat management. Logs weight, vitals, medications, hoof condition, observations, births, and activities. Syncs to farmOS.

## Install the App

1. Go to the [Releases page](../../releases)
2. Download the latest `app-release.apk`
3. On your Android phone: Settings → Security → Allow unknown sources (or "Install unknown apps" for Chrome)
4. Open the downloaded APK and install it
5. Open Goat Desk, go to **FarmOS Sync**, enter your farmOS URL and credentials

## First Time Setup (GitHub Actions — one time only)

You only need to do this once to enable automatic APK builds.

### 1. Add repository secrets

Go to your repo → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

Add these four secrets:

| Secret name | Value |
|---|---|
| `KEYSTORE_BASE64` | *(see keystore file provided separately)* |
| `KEYSTORE_PASSWORD` | `goatdesk2024` |
| `KEY_ALIAS` | `goatdesk` |
| `KEY_PASSWORD` | `goatdesk2024` |

### 2. Trigger a build

After adding secrets, go to **Actions** → **Build Goat Desk APK** → **Run workflow**.

The APK will appear in Releases within about 5 minutes.

## Updating the App

1. Replace `app/src/main/assets/goatdesk.html` with your new version
2. Commit and push to `main`
3. GitHub automatically builds a new APK
4. Download from Releases and install over the old version
