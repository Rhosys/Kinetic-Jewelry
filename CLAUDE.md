# KineticJewel — Claude Code guidelines

Rules and conventions that must be followed in every session.

---

## CI/CD — never use `sudo` to run application code

**Rule:** `sudo` is only permitted in CI/CD for infrastructure setup steps
(package installation, kernel module loading, service start, writing config
files owned by root). It must never be used to invoke application code such as
`cargo`, `gradle`, `npm`, `python`, or any test runner.

**Why it breaks:** CI runners (GitHub Actions `ubuntu-latest`, GitLab shared
runners) install language toolchains under the non-root user's home directory.
`sudo` switches to root, which has a different `$HOME`, a different `PATH`, and
no access to the user's cargo registry, gradle cache, or npm store. The command
silently uses the wrong toolchain or fails outright.

**Correct pattern — grant access instead of escalating:**

| Problem | Wrong | Right |
|:--------|:------|:------|
| Process needs D-Bus / BlueZ | `sudo -E cargo test` | Write a D-Bus policy file granting the runner user access; run `cargo test` as normal user |
| Process needs a device file | `sudo myapp` | `sudo chmod a+rw /dev/ttyX` in setup step; run `myapp` as normal user |
| Process needs a privileged port | `sudo server` | `sudo setcap cap_net_bind_service+ep ./server` in setup; run `server` as normal user |
| Process needs group membership | `sudo myapp` | `sudo usermod -a -G groupname $USER` + `newgrp groupname` or re-run step in a new shell |

**The D-Bus / BlueZ specific fix** (used in `device-ble-integration` job):
```yaml
- name: Grant CI user BlueZ D-Bus access
  run: |
    sudo tee /etc/dbus-1/system.d/bluetooth-ci.conf > /dev/null << POLICY
    <busconfig>
      <policy user="$(id -un)">
        <allow send_destination="org.bluez"/>
        <!-- add specific interfaces as needed -->
      </policy>
    </busconfig>
    POLICY
    sudo systemctl reload dbus

- name: Run tests          # no sudo here
  run: cargo test --test ble_integration
```

---

## Local development — run on the emulator

```bash
npm run start            # debug variant: boots emulator, builds, installs, launches, streams crash logs
npm run start:release    # release variant: same loop on the R8/ProGuard build — catches stripping crashes
```

`scripts/dev.sh` is the single orchestrator: runs `setup.sh` if the SDK is missing,
creates the shared `WorkspaceAVD` (android-35, pixel_7) if absent, boots it, then
gradle install + launch. All three workspace Android apps share one `WorkspaceAVD`
and one system image — do not give this app its own AVD name.

Emulator-only helpers: `npm run setup`, `npm run emulator:create|start|delete`.
KVM is required (Linux). Troubleshooting lives in `scripts/setup.sh` (Java 17, SDK,
KVM, ktlint).

---

## Repository layout quick-reference

```
firmware/protocol/src/lib.rs          ← only file to edit when protocol changes
firmware/protocol/tests/fixtures/
  test-vectors.json                   ← shared source of truth for Rust + Kotlin CI
firmware/device/src/ble.rs            ← BLE UUIDs (update if protocol UUIDs change)
firmware/device-host/                 ← native Linux clone for CI Suite A tests
.github/workflows/firmware.yml       ← CI — see todo.md for job status
```

Full runbooks: `todo.md`.
