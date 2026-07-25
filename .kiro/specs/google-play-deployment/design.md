# Design Document: Google Play Deployment

## Overview

This design covers the complete pipeline for building, signing, and deploying the Kinetic-Jewelry Android app to the Google Play Store. The work spans eight areas:

1. **Identity change** — Migrate applicationId, namespace, and all source packages from `com.rhosys.kineticjewelry` to `ch.rhosys.lyra`
2. **Signing infrastructure** — Generate a KMS-encrypted PKCS12 keystore, commit the JSON artifact to `deployment/android-upload-signing.json`, and configure CI to decrypt at build time via OIDC role assumption
3. **CI pipeline** — GitLab CI jobs for compile, lint, and test on every push
4. **Build pipeline** — GitLab CI jobs producing debug APKs on MRs (manual) and signed release AABs on main (manual), using OIDC + KMS decryption to obtain signing credentials
5. **Deploy pipeline** — GitLab CI job that automatically uploads the signed AAB to the Play Store Internal Testing track via GCP Workload Identity Federation after a successful release build
6. **GCP WIF infrastructure** — Terraform resource in `_rhosys-apps-infra/gcp/main.tf` binding the `gitlab-play-store` service account to the `rhosys/rapid/kinetic-jewelry` project path
7. **Documentation** — SIGNING.md, CI_CD.md, and SETUP.md covering the full process
8. **Developer tooling** — Setup script, emulator management scripts, unified check command, git hooks, and local build documentation

The app uses AGP 8.5.0, Kotlin 2.0.0, JDK 17, Hilt, Room, and Jetpack Compose. Signing uses `android.injected.signing` Gradle properties (not a `signingConfigs` block), matching the pattern established in the cycle-tracker reference project.

## Architecture

```mermaid
flowchart TD
    subgraph "Developer Machine"
        A[generate-android-keystore --alias lyra] --> B[deployment/android-upload-signing.json]
    end

    subgraph "Repository"
        B --> REPO[deployment/android-upload-signing.json<br/>keystore: base64 PKCS12<br/>passwordCiphertext: KMS ciphertext]
    end

    subgraph "GitLab CI (.gitlab-ci.yml)"
        subgraph "validate + test stages"
            CI1[compileDebugKotlin]
            CI2[lintDebug]
            CI3[testDebugUnitTest]
        end

        subgraph "build stage"
            PR[MR → build-debug (manual) → debug APK artifact]
            subgraph "build-release (manual, main only)"
                OIDC[Write GITLAB_OIDC_TOKEN → aws.jwt<br/>AWS_ROLE_ARN = GitLabRunnerRole]
                OIDC --> DECODE[jq .keystore → base64 --decode<br/>→ android/app/android-upload-signing.keystore]
                OIDC --> DECRYPT[jq .passwordCiphertext → base64 --decode<br/>→ aws kms decrypt → STORE_PASSWORD]
                DECODE --> GRADLE[./gradlew bundleRelease<br/>-Pandroid.injected.signing.store.file=...<br/>-Pandroid.injected.signing.store.password=...<br/>-Pandroid.injected.signing.key.alias=lyra<br/>-Pandroid.injected.signing.key.password=...]
                DECRYPT --> GRADLE
            end
        end

        subgraph "deploy stage"
            GRADLE --> DEPLOY[deploy-release (automatic)<br/>GCP WIF → gitlab-play-store SA<br/>deployment/deploy-play-store.ts<br/>→ Play Store internal track]
        end

        REPO --> DECODE
        REPO --> DECRYPT
    end

    subgraph "AWS (eu-west-1)"
        KMS[KMS key: alias/deployment-encryption-key]
        KMS --> DECRYPT
    end

    subgraph "GCP (rhosys-apps)"
        WIF[Workload Identity Pool: gitlab-oidc<br/>Provider: gitlab-com<br/>SA: gitlab-play-store]
        WIF --> DEPLOY
    end

    subgraph "Google Play Console"
        DEPLOY --> GP[Internal Testing track<br/>ch.rhosys.lyra]
    end
```

## Components and Interfaces

### 1. Gradle Build Configuration (`app/build.gradle.kts`)

**Responsibilities:**
- Define `applicationId = "ch.rhosys.lyra"` and `namespace = "ch.rhosys.lyra"`
- Apply `.debug` suffix for debug builds
- Set `versionCode = 1`, `versionName = "1.0.0"`
- Enable R8 minification for release builds
- Reference proguard rules

**No signingConfigs block.** Signing is handled entirely via `-Pandroid.injected.signing.*` command-line properties passed by the CI job. This keeps the build file clean and avoids any conditional logic for missing credentials — Gradle simply produces an unsigned build when the properties aren't provided.

### 2. Source Package Structure

**Before:**
```
app/src/main/java/com/rhosys/kineticjewelry/
app/src/debug/java/com/rhosys/kineticjewelry/
```

**After:**
```
app/src/main/java/ch/rhosys/lyra/
app/src/debug/java/ch/rhosys/lyra/
```

All `package` declarations change from `com.rhosys.kineticjewelry.*` to `ch.rhosys.lyra.*`. All internal imports update accordingly. AndroidManifest.xml relative class references (`.KineticJewelryApp`, `.MainActivity`, `.service.KineticNotificationListenerService`) remain unchanged — they resolve relative to the namespace.

### 3. ProGuard/R8 Rules (`app/proguard-rules.pro`)

Updated to reference `ch.rhosys.lyra`:

```proguard
-keep class ch.rhosys.lyra.data.local.db.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep @com.google.dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
```

### 4. Keystore JSON (`deployment/android-upload-signing.json`)

Generated by `generate-android-keystore --alias lyra`. Structure:

```json
{
  "keystore": "<base64-encoded PKCS12 keystore>",
  "passwordCiphertext": "<base64-encoded KMS ciphertext>"
}
```

- Keystore: RSA 4096-bit, 10950-day validity, PKCS12 format
- DN: `CN=rhosys.ch, O=Rhosys AG, OU=Mobile, L=Unknown, ST=Unknown, C=CH`
- Password: 32 random bytes, base64url-encoded (no `+`, `/`, `=`)
- KMS key: `alias/deployment-encryption-key` in `eu-west-1`

This file is committed to the repo. The keystore itself is password-protected, and the password is KMS-encrypted — so the file is safe to store in version control.

### 5. Signing Configuration (CI Decryption)

The `build-release` job decrypts signing credentials inline — no stored CI variables for secrets, no signingConfigs block in Gradle. The CI runner assumes an OIDC role with `kms:Decrypt` permission, then:

```bash
# 1. Write OIDC token for AWS web identity
echo "${GITLAB_OIDC_TOKEN}" > "${AWS_WEB_IDENTITY_TOKEN_FILE}"

# 2. Extract and decode the keystore
jq -r '.keystore' deployment/android-upload-signing.json | base64 --decode > android/app/android-upload-signing.keystore

# 3. Decrypt the password (plaintext exists only in memory/shell variable)
STORE_PASSWORD=$(jq -r '.passwordCiphertext' deployment/android-upload-signing.json \
  | base64 --decode \
  | aws kms decrypt --ciphertext-blob fileb:///dev/stdin --region eu-west-1 --output text --query Plaintext \
  | base64 --decode)

# 4. Build with signing properties
./gradlew bundleRelease \
  -Pandroid.injected.signing.store.file="$CI_PROJECT_DIR/android/app/android-upload-signing.keystore" \
  -Pandroid.injected.signing.store.password="$STORE_PASSWORD" \
  -Pandroid.injected.signing.key.alias=lyra \
  -Pandroid.injected.signing.key.password="$STORE_PASSWORD"
```

Key points:
- `KEY_ALIAS` is hardcoded as `lyra` — not a secret
- `STORE_PASSWORD` and `KEY_PASSWORD` are the same value (PKCS12 uses one password)
- Plaintext exists only in memory/temp file for the duration of the job
- The IAM role needs `kms:Decrypt` on `alias/deployment-encryption-key` in `eu-west-1`
- Only one CI/CD variable required: `AWS_ACCOUNT_ID` = `<ACCOUNT_ID>`

### 6. GitLab CI Pipeline (`.gitlab-ci.yml`)

**Workflow rules:** Prevent double-running when a branch has an open MR.

```yaml
workflow:
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH'
```

**Stages:** `validate`, `test`, `build`, `deploy`

**Default id_tokens:**
```yaml
default:
  id_tokens:
    GITLAB_OIDC_TOKEN:
      aud: https://gitlab.com
```

**Image:** A lean Android SDK image with JDK 17 (e.g., `cimg/android:2024.01` or equivalent) for build and validate/test jobs. The `deploy-release` job uses `node:24` since it only needs Node.js.

**Validate + Test jobs:**

| Job | Stage | Command | Purpose |
|-----|-------|---------|---------|
| compile | validate | `./gradlew compileDebugKotlin` | Catch compilation errors |
| lint | validate | `./gradlew lintDebug` | Android lint checks |
| test | test | `./gradlew testDebugUnitTest` | Unit test suite |

All validate/test jobs use branch-keyed Gradle cache.

**Build jobs:**

| Job | Stage | Trigger | Image |
|-----|-------|---------|-------|
| `build-debug` | build | Manual on MRs | `cimg/android:2024.01` (JDK 17 + Android SDK) |
| `build-release` | build | Manual on main | `cimg/android:2024.01` (JDK 17 + Android SDK) |

**Deploy job:**

| Job | Stage | Trigger | Image |
|-----|-------|---------|-------|
| `deploy-release` | deploy | Automatic after `build-release` on main | `node:24` |

### 7. Build Jobs Detail

**`build-debug`** (manual, MR only):
1. `./gradlew assembleDebug`
2. Upload debug APK artifact (7-day retention)

**`build-release`** (manual, main only):
1. Write `GITLAB_OIDC_TOKEN` to `$AWS_WEB_IDENTITY_TOKEN_FILE`
2. Install AWS CLI
3. Decode keystore from `deployment/android-upload-signing.json`
4. Decrypt password via `aws kms decrypt`
5. `./gradlew bundleRelease` with `-Pandroid.injected.signing.*` properties
6. Upload signed AAB artifact (30-day retention)

**Variables for `build-release`:**
```yaml
variables:
  AWS_WEB_IDENTITY_TOKEN_FILE: "${CI_PROJECT_DIR}/aws.jwt"
  AWS_DEFAULT_REGION: "eu-west-1"
  AWS_ROLE_ARN: "arn:aws:iam::${AWS_ACCOUNT_ID}:role/GitLabRunnerRole"
```

### 8. Deploy Job Detail (`deploy-release`)

Runs automatically after `build-release` succeeds on main. Uses GCP Workload Identity Federation to authenticate to the Play Store API without stored credentials.

**Authentication flow:**
```yaml
variables:
  GOOGLE_APPLICATION_CREDENTIALS: "/tmp/gcp-workload-identity.json"
script:
  - echo "${GITLAB_OIDC_TOKEN}" > /tmp/gcp-oidc.jwt
  - echo '{"type":"external_account","audience":"//iam.googleapis.com/projects/454629444494/locations/global/workloadIdentityPools/gitlab-oidc/providers/gitlab-com","subject_token_type":"urn:ietf:params:oauth:token-type:id_token","token_url":"https://sts.googleapis.com/v1/token","credential_source":{"file":"/tmp/gcp-oidc.jwt"},"service_account_impersonation_url":"https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/gitlab-play-store@rhosys-apps.iam.gserviceaccount.com:generateAccessToken"}' > "$GOOGLE_APPLICATION_CREDENTIALS"
  - npm run deploy:play-store
```

**How it works:**
1. Write the GitLab OIDC token to `/tmp/gcp-oidc.jwt`
2. Write a GCP external account credential JSON that references the OIDC token file, the WIF pool/provider, and the service account to impersonate
3. `GOOGLE_APPLICATION_CREDENTIALS` points to this JSON — the Google Auth library handles the token exchange transparently
4. Execute `deployment/deploy-play-store.ts` which uploads the AAB to the `internal` track for `ch.rhosys.lyra`

### 9. Deploy Script (`deployment/deploy-play-store.ts`)

Adapted from the cycle-tracker deploy script. Uses `@googleapis/androidpublisher` to upload the signed AAB to the Play Store Internal Testing track.

**Key differences from cycle-tracker:**
- Package name: `ch.rhosys.lyra` (not `ch.rhosys.cycletracker`)
- AAB path: `app/build/outputs/bundle/release/app-release.aab` (no `android/` prefix — this is a native Kotlin project, not Expo)
- Version name: read from `app/build.gradle.kts` or passed as env var

**Core flow:**
1. Create an edit for `ch.rhosys.lyra`
2. Upload the AAB bundle
3. Assign the uploaded version to the `internal` track with status `completed`
4. Commit the edit

**Error handling:**
- If the app is in draft state on Play Console, retry the release with `draft` status and print a message indicating manual promotion is required
- If the Play Store API returns 404 (package not found), exit non-zero with instructions for creating the app and uploading the first AAB manually via Play Console
- If 401/403, diagnose whether it's a missing release or permission issue

### 10. GCP Workload Identity Federation Infrastructure

A single Terraform resource added to `_rhosys-apps-infra/gcp/main.tf`, following the same pattern as the existing `play_store_wif_cycle_tracker` binding:

```hcl
resource "google_service_account_iam_member" "play_store_wif_kinetic_jewelry" {
  service_account_id = google_service_account.play_store.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.gitlab.name}/attribute.project_path/rhosys/rapid/kinetic-jewelry"
}
```

This reuses the existing:
- `google_iam_workload_identity_pool.gitlab` — the WIF pool with `gitlab-oidc` ID
- `google_service_account.play_store` — the `gitlab-play-store` SA that has Play Store API access

No new service accounts or pools are needed. The binding grants the `rhosys/rapid/kinetic-jewelry` GitLab project the ability to impersonate the `gitlab-play-store` service account.

### 11. Developer Setup Script (`scripts/setup.sh`)

**Responsibilities:**
- Install all build prerequisites from a fresh clone to a working environment
- Adapted from the cycle-tracker setup script but targeting native Kotlin (no Node.js/npm dependency for the app build itself)

**Steps:**

1. **Verify Java 17** — Check `java -version` for version 17. If missing, install via `sudo apt-get install -y openjdk-17-jdk` on Linux or `brew install openjdk@17` on macOS.
2. **Download Android SDK cmdline-tools** — If `$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager` doesn't exist, download from `https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip`, extract to `$ANDROID_HOME/cmdline-tools/latest/`.
3. **Install SDK components** — `sdkmanager "platform-tools" "build-tools;35.0.0" "platforms;android-35"`
4. **Accept licenses** — `yes | sdkmanager --licenses`
5. **Write ANDROID_HOME to shell profiles** — Append an export block (guarded by a unique marker `# BEGIN lyra android sdk` / `# END lyra android sdk`) to `~/.bashrc`, `~/.zshrc`, and `~/.profile`. Skip if marker already present.
6. **Validate KVM on Linux** — Check `/dev/kvm` exists. If CPU supports VT-x/AMD-V (`grep -Ec '(vmx|svm)' /proc/cpuinfo`) but `/dev/kvm` is missing, install `qemu-kvm`. Warn if CPU lacks virtualisation flags.
7. **Install ktlint** — Download the standalone ktlint binary from GitHub releases, make executable, and add to PATH in the marker block.
8. **Print next steps** — Restart terminal, run `scripts/emulator-create.sh`.

**Environment:**
- `ANDROID_HOME` defaults to `$HOME/Android/Sdk`
- `set -euo pipefail` for fail-fast behavior
- Exit non-zero on any critical failure

### 12. Emulator Scripts

Three scripts for managing the development AVD:

**`scripts/emulator-create.sh`:**
- Download system image `system-images;android-35;google_apis;x86_64` via `sdkmanager`
- Create AVD named `WorkspaceAVD` using the Pixel 7 device profile
- If AVD already exists, print message and exit 0 (idempotent)

**`scripts/emulator-start.sh`:**
- Start `WorkspaceAVD` emulator in foreground with `-no-snapshot-load`
- If AVD doesn't exist, print error directing user to `scripts/emulator-create.sh` and exit 1

**`scripts/emulator-delete.sh`:**
- Delete AVD `WorkspaceAVD` via `avdmanager delete avd --name WorkspaceAVD`

All scripts validate `ANDROID_HOME` is set and the SDK is installed before proceeding.

### 13. Git Hooks (Husky)

**Minimal `package.json`** (repo root — for git hooks and deploy script):
```json
{
  "private": true,
  "scripts": {
    "prepare": "husky",
    "deploy:play-store": "tsx deployment/deploy-play-store.ts"
  },
  "devDependencies": {
    "husky": "^9.1.7",
    "lint-staged": "^15.3.0",
    "tsx": "^4.0.0",
    "@googleapis/androidpublisher": "^22.0.0",
    "google-auth-library": "^9.0.0"
  },
  "lint-staged": {
    "*.kt": ["ktlint --format"]
  }
}
```

**`.husky/commit-msg`:**
```bash
#!/bin/sh
MSG=$(cat "$1")
if [ ${#MSG} -lt 20 ]; then
  echo "❌ Commit message too short. Explain WHY this change was made (min 20 chars)."
  exit 1
fi
```

**`.husky/pre-commit`:**
```bash
#!/bin/sh
npx lint-staged
```

### 14. Lint-Staged + ktlint

**lint-staged configuration** (in `package.json`):
```json
{
  "lint-staged": {
    "*.kt": ["ktlint --format"]
  }
}
```

**Behavior:**
- On `git commit`, the pre-commit hook runs `npx lint-staged`
- lint-staged identifies staged `*.kt` files
- For each staged file, runs `ktlint --format` which auto-fixes formatting issues
- If ktlint finds unfixable errors, it exits non-zero and the commit is rejected
- Fixed files are automatically re-staged by lint-staged

### 15. .gitignore Additions

Append to the existing `.gitignore` (preserving all current entries):

```gitignore
# Signing keys — never commit plaintext keystores
*.jks
*.p12
*.key
*.mobileprovision
*.keystore
```

The existing `.gitignore` already contains `/local.properties`, `*.apk`, and `*.aab`.

### 16. Unified Check (`scripts/check.sh`)

```bash
#!/usr/bin/env bash
set -euo pipefail
./gradlew compileDebugKotlin lintDebug testDebugUnitTest
```

Single Gradle invocation that runs compilation, lint, and unit tests in sequence. If any task fails, Gradle exits non-zero and the script propagates the failure. This mirrors what CI runs, giving developers a local pre-push quality gate.

### 17. Documentation

| File | Content |
|------|---------|
| `docs/SIGNING.md` | Keystore generation, `deployment/android-upload-signing.json` structure, KMS decryption pattern, OIDC role configuration, key rotation |
| `docs/CI_CD.md` | GitLab CI pipeline stages/jobs, `build-release` trigger (manual, main), `deploy-release` (automatic after build-release), artifact retention, AWS OIDC config, GCP WIF config for Play Store deployment |
| `docs/SETUP.md` | Prerequisites, `scripts/setup.sh`, emulator setup, daily workflow, command table, troubleshooting |

## Data Models

### Keystore JSON Schema

```typescript
interface KeystoreJson {
  /** Base64-encoded PKCS12 keystore file (password-protected) */
  keystore: string;
  /** Base64-encoded KMS ciphertext of the keystore password */
  passwordCiphertext: string;
}
```

### Version Configuration

```kotlin
// In app/build.gradle.kts defaultConfig
versionCode = 1          // Increment by 1 per Play Store upload
versionName = "1.0.0"   // Semantic versioning: MAJOR.MINOR.PATCH
```

### OIDC / WIF Configuration

| Property | Value |
|----------|-------|
| AWS Role ARN | `arn:aws:iam::${AWS_ACCOUNT_ID}:role/GitLabRunnerRole` |
| AWS Region | `eu-west-1` |
| AWS Permission | `kms:Decrypt` on `alias/deployment-encryption-key` |
| AWS Auth method | GitLab OIDC → `GITLAB_OIDC_TOKEN` written to `$AWS_WEB_IDENTITY_TOKEN_FILE` |
| CI Variable | `AWS_ACCOUNT_ID` = `<ACCOUNT_ID>` |
| GCP WIF Pool | `gitlab-oidc` in project `rhosys-apps` (project number `454629444494`) |
| GCP WIF Provider | `gitlab-com` |
| GCP Service Account | `gitlab-play-store@rhosys-apps.iam.gserviceaccount.com` |
| GCP Binding | `attribute.project_path/rhosys/rapid/kinetic-jewelry` |

### Deploy Script Configuration

| Property | Value |
|----------|-------|
| Package name | `ch.rhosys.lyra` |
| AAB path | `app/build/outputs/bundle/release/app-release.aab` |
| Track | `internal` |
| Script path | `deployment/deploy-play-store.ts` |
| npm script | `deploy:play-store` |

## Error Handling

### Build Failures

| Scenario | Behavior |
|----------|----------|
| Missing signing properties | Gradle produces unsigned build (no error) — this is by design for local dev |
| Invalid keystore or wrong password | `bundleRelease` fails with non-zero exit, error message from jarsigner |
| R8 strips required class | Build succeeds but app crashes at runtime — mitigated by comprehensive keep rules |
| Invalid versionName format | Build proceeds (Gradle doesn't validate format) — enforced by code review |

### CI/CD Failures

| Scenario | Behavior |
|----------|----------|
| Compilation failure | `compile` job fails, MR blocked by pipeline status |
| Lint errors | `lint` job fails, MR blocked |
| Test failure | `test` job fails, MR blocked |
| OIDC role assumption failure | `build-release` fails when writing the OIDC token or when AWS CLI attempts to use it — indicates trust policy mismatch or missing OIDC provider |
| KMS decryption failure | `build-release` fails at `aws kms decrypt` with AccessDeniedException or InvalidCiphertextException — role lacks `kms:Decrypt` permission or ciphertext is corrupted |
| Keystore base64 decode failure | `build-release` fails at `base64 --decode` — `keystore` field in JSON is malformed |

### Deploy Failures

| Scenario | Behavior |
|----------|----------|
| GCP WIF token exchange failure | `deploy-release` fails when Google Auth library attempts to exchange the OIDC token — WIF binding missing or misconfigured |
| Play Store 404 (package not found) | Deploy script exits non-zero with instructions to create the app and upload first AAB manually via Play Console |
| Play Store draft app error | Deploy script retries with `draft` status and prints message indicating manual promotion required |
| Play Store 401/403 | Deploy script diagnoses whether it's a missing release or permission issue and prints actionable instructions |

### Keystore Generation Failures

| Scenario | Behavior |
|----------|----------|
| Missing `--alias` argument | Exit code 1, usage message to stderr |
| KMS encryption failure | Exit code 1, error message, temp keystore deleted |
| keytool failure | Exit code 1, error message, temp files cleaned up |

### Setup Script Failures

| Scenario | Behavior |
|----------|----------|
| Java 17 install fails (no apt-get or brew) | Exit code 1, error with manual install instructions |
| Android SDK download fails (network error) | Exit code 1, curl fails with non-zero |
| sdkmanager component install fails | Exit code 1, error from sdkmanager propagated |
| KVM not available (no VT-x/AMD-V) | Warning printed, script continues (emulator will be slow) |
| Shell profile already contains marker block | Skip writing, print info message |

### Emulator Script Failures

| Scenario | Behavior |
|----------|----------|
| `ANDROID_HOME` not set or SDK missing | Exit code 1, error directing user to run `scripts/setup.sh` |
| AVD already exists (create) | Print message, exit 0 (idempotent) |
| AVD doesn't exist (start) | Exit code 1, error directing user to run `scripts/emulator-create.sh` |
| System image download fails | Exit code 1, sdkmanager error propagated |

### Git Hook Failures

| Scenario | Behavior |
|----------|----------|
| Commit message < 20 chars | Commit rejected, error message printed |
| ktlint finds unfixable errors | Commit rejected, lint errors displayed |
| ktlint not on PATH | lint-staged fails, commit rejected with "command not found" |
| npm/npx not available | Pre-commit hook fails, commit rejected |

## Testing Strategy

**PBT is not applicable to this feature.** The work is entirely build configuration (Gradle DSL), CI/CD pipeline setup (GitLab CI YAML), infrastructure as code (Terraform), file restructuring, shell scripts, and documentation. There are no pure functions with meaningful input variation to test with property-based testing.

### Verification Approach

| Area | Test Method | Command/Check |
|------|-------------|---------------|
| Package rename compiles | Smoke test | `./gradlew compileDebugKotlin` |
| Lint passes | Smoke test | `./gradlew lintDebug` |
| Unit tests pass | Regression | `./gradlew testDebugUnitTest` |
| Release build with R8 | Smoke test | `./gradlew assembleRelease` (unsigned, verifies R8 rules) |
| Signed AAB valid | Integration | `jarsigner -verify app/build/outputs/bundle/release/app-release.aab` |
| CI pipeline syntax | Validation | GitLab CI lint (`/ci/lint` API endpoint) |
| Keystore JSON structure | Manual | Verify JSON has `keystore` and `passwordCiphertext` fields |
| KMS decrypt works | Manual | `aws kms decrypt --ciphertext-blob ...` returns plaintext |
| OIDC role assumption | Manual | Trigger `build-release` on main, verify AWS credentials work |
| GCP WIF binding | Manual | Trigger `deploy-release`, verify token exchange succeeds |
| Play Store upload | Manual | Verify AAB appears on Internal Testing track in Play Console |
| Deploy script | Manual | Run `npm run deploy:play-store` after a successful build |
| Terraform plan | Manual | `tofu plan` in `_rhosys-apps-infra/gcp/` shows the new binding |
| Setup script installs Java | Manual | Run `scripts/setup.sh` on clean machine, verify `java -version` shows 17 |
| Setup script installs SDK | Manual | Run `scripts/setup.sh`, verify `sdkmanager --list` works |
| Emulator create | Manual | Run `scripts/emulator-create.sh`, verify `avdmanager list avd` shows WorkspaceAVD |
| Emulator start | Manual | Run `scripts/emulator-start.sh`, verify emulator boots |
| Commit-msg hook rejects short | Manual | Attempt commit with < 20 char message, verify rejection |
| Pre-commit hook runs ktlint | Manual | Stage a malformatted `.kt` file, commit, verify ktlint runs |
| Unified check | Smoke test | `scripts/check.sh` exits 0 on clean project |
| .gitignore patterns | Manual | Verify `git status` ignores `*.jks`, `*.p12`, etc. |

### What Gets Tested in CI

The CI pipeline itself serves as the primary test harness:
- **compile job** validates the package rename didn't break anything
- **lint job** validates no dead imports or references to old package
- **test job** validates existing unit tests still pass after rename

### Manual Verification Checklist

Before first Play Store upload:
1. `./gradlew bundleRelease` with signing properties produces a valid AAB
2. `jarsigner -verify` confirms the AAB is properly signed
3. Google Play Console accepts the AAB upload without errors
4. App installs and runs correctly from Play Store internal testing track
5. `deploy-release` job completes successfully and AAB appears on internal track
