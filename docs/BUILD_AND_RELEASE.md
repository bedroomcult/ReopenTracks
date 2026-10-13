# Build & Release Guide

Everything needed to build, sign, and publish ReopenTracks. Written so a fresh
AI agent (or human) with no prior context can do it correctly.

- **Repo**: `github.com/bedroomcult/ReopenTracks` (public, fork, Apache-2.0)
- **Default branch**: `main`
- **App id**: `com.reopentracks`
- **Platform**: Android, Java 17, Gradle 9.0.0, AGP 8.11.1, compileSdk 36, minSdk 26

Related docs: [VERSIONING.md](VERSIONING.md) for version/changelog rules.

---

## 1. Environment

| Tool | Requirement | Where |
|---|---|---|
| JDK | 17 (exact) | `C:/Users/Graphic/scoop/apps/openjdk17/current` |
| Android SDK | API 36 + build-tools 36.0.0 | `C:/Users/Graphic/AppData/Local/Android/Sdk` |
| Gradle | use the wrapper (`./gradlew`), never a system Gradle | `gradle/wrapper/` |

There is **no `local.properties`** in the repo and none is needed — the SDK
location comes from the `ANDROID_HOME` environment variable.

```bash
export ANDROID_HOME="C:/Users/Graphic/AppData/Local/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
```

## 2. Build flavours

Three product flavours exist (`build.gradle` → `productFlavors`):

| Flavour | applicationId | Purpose |
|---|---|---|
| `nightly` | `com.reopentracks.nightly` | CI/nightly builds; version auto-derived from git |
| `irreproducible` | `com.reopentracks` | main dev/release flavour (F-Droid style) |
| `reproducible` | `com.reopentracks.playstore` | signed with the release key; store builds |

Build types: `debug` (adds `.debug` suffix) and `release`.

### Common commands

```bash
# Debug APK (unsigned, installable)
./gradlew assembleIrreproducibleDebug
#   -> build/outputs/apk/irreproducible/debug/com.reopentracks.debug_<versionCode>.apk

# Release APK (unsigned unless signing properties are passed — see §4)
./gradlew assembleIrreproducibleRelease
#   -> build/outputs/apk/irreproducible/release/com.reopentracks_<versionCode>.apk

# Android Lint (must pass — CI enforces it)
./gradlew lintIrreproducibleDebug

# Compile the instrumentation tests without a device (CI does this)
./gradlew assembleIrreproducibleDebugAndroidTest
```

### What NOT to run

- **Do not run `connectedAndroidTest` / `connectedCheck`** unless a physical
  device is attached. All 55 tests live in `src/androidTest` (there is no
  `src/test`), so they need a device or emulator. CI deliberately does not run
  them.
- Do not add a system Gradle or a different JDK.

## 3. CI (GitHub Actions)

`.github/workflows/ci.yml` runs two jobs on every push to `main` and every PR:

1. **build** — `assembleIrreproducibleDebug assembleIrreproducibleDebugAndroidTest`, uploads the debug APK as an artifact
2. **lint** — `lintIrreproducibleDebug`, uploads the lint report on failure

Notes that have already bitten us:

- `fetch-depth: 0` is **required** — `build.gradle` derives `versionName` from
  `git describe --tags` and `versionCode` from `git rev-list HEAD --count`.
  A shallow checkout breaks both.
- There is **no** `android-actions/setup-android` step. That action fails on
  current runners (retired Node 16 runtime). `ubuntu-latest` already ships the
  Android SDK.
- `sdkmanager` is **not on `PATH`** — call it as
  `"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"`.
- **Lint errors fail the build.** `lint.xml` promotes several checks to
  `error` severity. A common trap: calling a Java 21+ API such as
  `StringBuilder.isEmpty()` (API 35) when `minSdk` is 26 — use
  `length() == 0` instead.

Always confirm CI is green for the exact commit you intend to release:

```bash
curl -s "https://api.github.com/repos/bedroomcult/ReopenTracks/actions/runs?per_page=5&branch=main"
# every recent run must show "conclusion": "success"
```

## 4. Signing

The release keystore lives **outside** the repo:

```
C:/Users/Graphic/.reopentracks-keys/reopen-tracks-release.jks
alias: reopentracks
```

> **The store/key password is deliberately NOT recorded in this repository
> (it is public).** Retrieve it from the owner's password manager, or ask the
> owner. Never commit it, never paste it into a tracked file, and never echo
> it into a log.

`build.gradle` reads signing material from **system properties**, not from a
properties file:

```
-Drelease_store_file=<path> -Drelease_store_password=<pw>
-Drelease_key_alias=<alias> -Drelease_key_password=<pw>
```

Because those are absent by default, `assembleIrreproducibleRelease` produces an
**unsigned** APK. Sign it manually after the build:

```bash
BT="$ANDROID_HOME/build-tools/36.0.0"
KS="C:/Users/Graphic/.reopentracks-keys/reopen-tracks-release.jks"
REL="build/outputs/apk/irreproducible/release"

# 1. align
"$BT/zipalign.exe" -f -p 4 "$REL/com.reopentracks_<versionCode>.apk" "$REL/aligned.apk"

# 2. sign
"$BT/apksigner.bat" sign --ks "$KS" --ks-key-alias reopentracks \
  --ks-pass "pass:<PW>" --key-pass "pass:<PW>" \
  --out "$REL/ReopenTracks-<versionName>-release.apk" "$REL/aligned.apk"

# 3. verify (must print the ReopenTracks certificate)
"$BT/apksigner.bat" verify --print-certs "$REL/ReopenTracks-<versionName>-release.apk"
```

Expected certificate: `CN=ReopenTracks, OU=ReopenTracks, O=ReopenTracks, ...`
SHA-256 `54ca5f898a4cf712ae8dfdfe2cad3faf2d5ff8a7a96a226df99d2ec7d532596d`.

> **Back up the keystore.** It is the app's permanent identity. Lose it and you
> can never update an installed app again. Leak it and anyone can publish
> updates as you.

## 5. Publishing a release

There is **no `gh` CLI installed**. Use the GitHub REST API with a token taken
from the git credential manager (never print the token):

```bash
TOKEN=$(printf "protocol=https\nhost=github.com\n\n" | git credential fill 2>/dev/null \
        | sed -n 's/^password=//p')
REPO="bedroomcult/ReopenTracks"
```

1. **Create the release** (`POST /repos/$REPO/releases`) with
   `{"tag_name":"vX.Y.Z","name":"ReopenTracks vX.Y.Z","body":"<changelog>","draft":false,"prerelease":false}`
2. **Upload the APK**
   (`POST https://uploads.github.com/repos/$REPO/releases/<releaseId>/assets?name=ReopenTracks-vX.Y.Z.apk`,
   `Content-Type: application/vnd.android.package-archive`, `--data-binary @<apk>`)
3. **Confirm** — `GET /repos/$REPO/releases/tags/vX.Y.Z` must list the asset
   with `"state": "uploaded"`.

To **replace** an asset (e.g. you fixed the icon and re-cut the same tag):
`DELETE /repos/$REPO/releases/assets/<assetId>` first, then upload again.

### Shell notes (Windows / git-bash)

- Long multi-line commands and heredocs get mangled by this shell. **Write a
  `.sh` file under `build/` (gitignored), run it with `bash file.sh`, then
  delete it** — especially anything containing the keystore password.
- Use full paths for `apksigner.bat`, `zipalign.exe`, `aapt2.exe`, `keytool`.
- `/tmp` does not exist for native Windows tools; use a path under `build/`.

## 6. Full release checklist

```
[ ] 1.  All work merged to main; working tree clean
[ ] 2.  CI green for that exact commit (build + lint)
[ ] 3.  versionCode + versionName bumped in build.gradle   (see VERSIONING.md)
[ ] 4.  Changelog written: fastlane/metadata/android/en-US/changelogs/<versionCode>.txt
[ ] 5.  Commit "Release: vX.Y.Z"; tag vX.Y.Z; push main + tag
[ ] 6.  ./gradlew assembleIrreproducibleRelease
[ ] 7.  zipalign + apksigner (verify the cert prints)
[ ] 8.  aapt2 dump badging -> confirm package/version
[ ] 9.  Create GitHub release + upload APK
[ ] 10. Confirm the asset shows "uploaded"
[ ] 11. Install on a device and smoke-test
```

### Re-tagging an existing version

If the tag already points at an older commit (e.g. you fixed something after
tagging), the release keeps the stale commit unless you move it:

```bash
git tag -d vX.Y.Z
git push origin :refs/tags/vX.Y.Z     # delete remote
git tag -a vX.Y.Z -m "ReopenTracks vX.Y.Z" <commit>
git push origin vX.Y.Z
```

## 7. Android icon rules (learned the hard way)

Adaptive icons (`res/drawable/ic_launcher.xml`) crop the foreground to a mask.
The guaranteed-visible safe zone is the **inner 66dp of the 108dp canvas**
(radius 33dp).

- Keep all foreground artwork within **33dp radius** of centre. The current
  ring uses radius 27.5dp with a 7.5dp stroke → outer edge 31.25dp.
- The first version of the ReopenTracks icon used a 41.4dp outer radius and was
  visibly clipped by circular launcher masks. Do not exceed the safe zone.
- When changing the icon, update **all** of:
  - `src/main/res/drawable/ic_logo_color_108dp.xml` (adaptive foreground, colour)
  - `src/main/res/drawable/ic_logo_108dp.xml` (monochrome)
  - `src/main/res/drawable/ic_logo_color_24dp.xml`, `ic_logo_24dp.xml` (small)
  - `fastlane/metadata/android/en-US/icon.png` (512×512 store icon)
- Verify by masking your design to a circle and a squircle before committing.

## 8. Repo conventions

- Java 17, no Kotlin. Match surrounding code style; Android Studio formatter.
- Do not add dependencies casually (see `CONTRIBUTING.md`) — Apache-2.0
  compatible only.
- Commit messages: short imperative, e.g.
  `KML export: add LineString fallback per segment`.
- `plans/` holds advisor-generated implementation plans — not app source.
