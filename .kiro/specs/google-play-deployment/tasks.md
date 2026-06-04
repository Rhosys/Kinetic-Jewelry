# Implementation Plan: Google Play Deployment

## Overview

Convert the Kinetic-Jewelry Android app from its current `com.rhosys.kineticjewelry` identity to `ch.rhosys.lyra`, set up signing infrastructure, GitLab CI/CD pipeline, Play Store deployment, developer tooling, and documentation. Most artifacts are adapted from the cycle-tracker reference project at `/home/warren/git/claude/cycle-tracker`.

## Tasks

- [x] 1. Application identity and source package rename
  - [x] 1.1 Update `app/build.gradle.kts` — change `namespace`, `applicationId` to `ch.rhosys.lyra`
    - Change `namespace = "com.rhosys.kineticjewelry"` → `namespace = "ch.rhosys.lyra"`
    - Change `applicationId = "com.rhosys.kineticjewelry"` → `applicationId = "ch.rhosys.lyra"`
    - Keep `.debug` suffix for debug builds, `versionCode = 1`, `versionName = "1.0.0"` unchanged
    - _Requirements: 1.1, 1.2, 1.3, 10.1, 10.2, 10.3_

  - [x] 1.2 Move all source files from `com/rhosys/kineticjewelry/` to `ch/rhosys/lyra/`
    - Move `app/src/main/java/com/rhosys/kineticjewelry/` → `app/src/main/java/ch/rhosys/lyra/`
    - Move `app/src/debug/java/com/rhosys/kineticjewelry/` → `app/src/debug/java/ch/rhosys/lyra/`
    - Move `app/src/test/java/com/rhosys/kineticjewelry/` → `app/src/test/java/ch/rhosys/lyra/`
    - Delete empty `com/rhosys/kineticjewelry/` directory trees after move
    - _Requirements: 2.1, 2.2_

  - [x] 1.3 Update all `package` declarations and `import` statements
    - Replace `package com.rhosys.kineticjewelry` → `package ch.rhosys.lyra` (and all subpackages)
    - Replace `import com.rhosys.kineticjewelry` → `import ch.rhosys.lyra` (and all subpackages)
    - Replace `import com.rhosys.kineticjewelry.R` → `import ch.rhosys.lyra.R`
    - Do NOT modify AndroidManifest.xml relative class references (`.KineticJewelryApp`, `.MainActivity`, etc.)
    - _Requirements: 2.3, 2.4, 2.5, 2.7_

  - [x] 1.4 Update ProGuard/R8 rules in `app/proguard-rules.pro`
    - Add keep rules referencing `ch.rhosys.lyra.data.local.db.**`
    - Add keep rules for Room, Hilt, and type converters
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

  - [x] 1.5 Verify compilation succeeds with `./gradlew compileDebugKotlin`
    - Run compilation to confirm the rename is complete and correct
    - _Requirements: 2.6_

- [x] 2. Checkpoint — Ensure compilation passes after rename
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Signing infrastructure and .gitignore
  - [x] 3.1 Add signing-related patterns to `.gitignore`
    - Append `*.jks`, `*.p12`, `*.key`, `*.mobileprovision`, `*.keystore` patterns
    - Preserve all existing entries
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5, 16.6, 16.7_

  - [x] 3.2 Create `deployment/android-upload-signing.json` placeholder with instructions
    - Create `deployment/` directory
    - Add a README or comment file explaining the keystore must be generated with `generate-android-keystore --alias lyra` from the `_tools` repo
    - Document the expected JSON structure (`keystore` + `passwordCiphertext` fields)
    - _Requirements: 3.1, 3.2, 3.3, 3.9_

- [x] 4. Node.js project setup (package.json, husky, lint-staged)
  - [x] 4.1 Create `package.json` with scripts, devDependencies, and lint-staged config
    - Add `prepare`, `deploy:play-store` scripts
    - Add `husky`, `lint-staged`, `tsx`, `@googleapis/androidpublisher`, `google-auth-library` as devDependencies
    - Add `lint-staged` config: `"*.kt": ["ktlint --format"]`
    - _Requirements: 14.4, 15.1, 15.2, 15.3_

  - [x] 4.2 Create `.husky/commit-msg` hook — reject messages shorter than 20 chars
    - Copy pattern from cycle-tracker, add the 20-char length check
    - Print error instructing developer to explain WHY the change was made
    - _Requirements: 14.1, 14.2_

  - [x] 4.3 Create `.husky/pre-commit` hook — run `npx lint-staged`
    - Single line: `npx lint-staged`
    - No Drizzle migration logic (unlike cycle-tracker)
    - _Requirements: 14.3, 14.5, 15.3_

- [x] 5. Deploy script and test
  - [x] 5.1 Create `deployment/deploy-play-store.ts`
    - Copy from cycle-tracker's `deployment/deploy-play-store.ts`
    - Change `PACKAGE_NAME` to `ch.rhosys.lyra`
    - Change `DEFAULT_AAB_PATH` to `app/build/outputs/bundle/release/app-release.aab`
    - Remove `getVersionName()` that reads `app.config.js` — read version from env var or hardcode approach
    - Keep all error handling (draft app, 404, 401/403 diagnostics)
    - _Requirements: 7.4, 7.5, 7.6, 7.7_

  - [x] 5.2 Create `deployment/deploy-play-store.test.ts`
    - Copy verbatim from cycle-tracker (tests the injectable `PublisherClient` interface)
    - Update package name and AAB path constants in assertions
    - _Requirements: 7.5_

- [x] 6. GitLab CI pipeline (`.gitlab-ci.yml`)
  - [x] 6.1 Create `.gitlab-ci.yml` with workflow rules, stages, and validate/test jobs
    - Workflow rules to prevent double-running on MR + branch push
    - Stages: `validate`, `test`, `build`, `deploy`
    - Default `id_tokens` block for `GITLAB_OIDC_TOKEN`
    - Use `cimg/android:2024.01` image (NOT `reactnativecommunity/react-native-android`)
    - `compile` job: `./gradlew compileDebugKotlin`
    - `lint` job: `./gradlew lintDebug`
    - `test` job: `./gradlew testDebugUnitTest`
    - Branch-keyed Gradle cache
    - No `cd android` prefix — Gradle wrapper is at project root
    - No `expo prebuild` — Gradle runs directly
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_

  - [x] 6.2 Add `build-debug` and `build-release` jobs
    - `build-debug`: manual on MRs, `./gradlew assembleDebug`, 7-day artifact retention
    - `build-release`: manual on main, OIDC + KMS decrypt + `./gradlew bundleRelease` with signing properties, 30-day artifact retention
    - Key alias is `lyra`
    - AWS variables: `AWS_WEB_IDENTITY_TOKEN_FILE`, `AWS_DEFAULT_REGION`, `AWS_ROLE_ARN`
    - Install AWS CLI for KMS decryption
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 6.10_

  - [x] 6.3 Add `deploy-release` job
    - Automatic after `build-release` on main (not manual)
    - `node:24` image, `npm ci`, GCP WIF credential JSON, `npm run deploy:play-store`
    - GCP audience: `//iam.googleapis.com/projects/454629444494/locations/global/workloadIdentityPools/gitlab-oidc/providers/gitlab-com`
    - Service account: `gitlab-play-store@rhosys-apps.iam.gserviceaccount.com`
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.8_

- [x] 7. Checkpoint — Review CI pipeline configuration
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. GCP WIF Terraform binding
  - [x] 8.1 Add `google_service_account_iam_member` resource to `_rhosys-apps-infra/gcp/main.tf`
    - Resource name: `play_store_wif_kinetic_jewelry`
    - Bind `gitlab-play-store` SA to `rhosys/rapid/kinetic-jewelry` project path
    - Role: `roles/iam.workloadIdentityUser`
    - Reuse existing `google_iam_workload_identity_pool.gitlab` and `google_service_account.play_store`
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 9. Developer tooling scripts
  - [x] 9.1 Create `scripts/setup.sh`
    - Copy from cycle-tracker's `scripts/setup.sh`
    - Change marker from `cycle-tracker android sdk` to `lyra android sdk`
    - Remove Node.js version check (not needed for native Kotlin app build)
    - Remove `npm ci` and `expo prebuild` steps
    - Keep Java 17 install, Android SDK install, license acceptance, env var writing, KVM validation
    - Add ktlint binary download and PATH addition
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 12.9_

  - [x] 9.2 Create `scripts/emulator-create.sh`
    - Copy from cycle-tracker, change `AVD_NAME` to `WorkspaceAVD`
    - Change system image to `system-images;android-35;google_apis;x86_64`
    - _Requirements: 13.1, 13.2, 13.3, 13.7_

  - [x] 9.3 Create `scripts/emulator-start.sh`
    - Start `WorkspaceAVD` in foreground with `-no-snapshot-load`
    - Error if AVD doesn't exist, directing user to `scripts/emulator-create.sh`
    - _Requirements: 13.4, 13.5, 13.7_

  - [x] 9.4 Create `scripts/emulator-delete.sh`
    - Copy from cycle-tracker, change `AVD_NAME` to `WorkspaceAVD`
    - _Requirements: 13.6, 13.7_

  - [x] 9.5 Create `scripts/check.sh` — unified check command
    - `./gradlew compileDebugKotlin lintDebug testDebugUnitTest`
    - _Requirements: 17.1, 17.2, 17.3_

- [x] 10. Documentation
  - [x] 10.1 Create `docs/SIGNING.md`
    - Copy structure from cycle-tracker's `docs/SIGNING.md`
    - Change alias to `lyra`, paths to `deployment/`
    - Document keystore generation, JSON structure, KMS decryption pattern, OIDC role config
    - Add key rotation section
    - _Requirements: 11.1, 11.4_

  - [x] 10.2 Create `docs/CI_CD.md`
    - Document pipeline stages/jobs, trigger conditions, artifact retention
    - Document AWS OIDC configuration, GCP WIF configuration
    - Document `deploy-release` automatic deployment to Play Store Internal Testing track
    - _Requirements: 11.2, 11.3_

  - [x] 10.3 Create `docs/SETUP.md`
    - Prerequisites, `scripts/setup.sh` usage, emulator setup
    - Daily workflow, command table, troubleshooting section
    - Document `scripts/check.sh` as pre-push quality gate
    - Document local build commands (`./gradlew assembleDebug`, `./gradlew bundleRelease`)
    - _Requirements: 17.4, 18.1, 18.2, 18.3, 18.4, 19.1, 19.2, 19.3, 19.4, 19.5, 19.6, 19.7_

- [x] 11. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- No property-based tests — this feature is entirely build config, CI/CD, infrastructure, and scripts
- The `deployment/android-upload-signing.json` keystore must be generated separately using `generate-android-keystore --alias lyra` from the `_tools` repo
- The Terraform change (task 8.1) is in a separate repo (`_rhosys-apps-infra`) and should be committed/applied independently
- Only one CI/CD variable needed in GitLab project settings: `AWS_ACCOUNT_ID` = `REDACTED`
- All Gradle commands run from project root (no `cd android` prefix)
- Docker image for CI is `cimg/android:2024.01` (lean Android SDK) — NOT `reactnativecommunity/react-native-android`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "3.1", "4.1"] },
    { "id": 1, "tasks": ["1.2", "3.2", "4.2", "4.3"] },
    { "id": 2, "tasks": ["1.3"] },
    { "id": 3, "tasks": ["1.4", "1.5"] },
    { "id": 4, "tasks": ["5.1", "6.1", "8.1", "9.1", "9.2", "9.3", "9.4", "9.5"] },
    { "id": 5, "tasks": ["5.2", "6.2", "10.1", "10.2", "10.3"] },
    { "id": 6, "tasks": ["6.3"] }
  ]
}
```
