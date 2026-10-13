# ReopenTracks — Developer Documentation

Start here. These docs are written for an AI agent or human with **no prior
context** on this repository.

| Document | Read it when |
|---|---|
| [BUILD_AND_RELEASE.md](BUILD_AND_RELEASE.md) | Building, signing, or publishing a release; CI questions; icon rules |
| [VERSIONING.md](VERSIONING.md) | Choosing a version number or writing a changelog |

## Orientation

| Fact | Value |
|---|---|
| Repo | `github.com/bedroomcult/ReopenTracks` (public, Apache-2.0, fork of OpenTracks) |
| Default branch | `main` |
| App id | `com.reopentracks` |
| Language | Java 17 (no Kotlin) |
| Build | Gradle 9.0.0 via `./gradlew`, AGP 8.11.1, compileSdk 36, minSdk 26 |
| Tests | 55 files, **all** in `src/androidTest` (device required); `src/test` does not exist |
| CI | `.github/workflows/ci.yml` — build + lint, no emulator |
| Releases | GitHub Releases only (no F-Droid/Play Store listing) |

### Layout

```
build.gradle                       app id, versions, flavours, signing config
.github/workflows/ci.yml           build + lint on push/PR
docs/                              this documentation
fastlane/metadata/android/en-US/   store metadata + changelogs/ (one file per versionCode)
plans/                             advisor-generated implementation plans
src/main/java/com/reopentracks/    app source
src/androidTest/java/com/reopentracks/   instrumented tests
src/main/res/drawable/             icons (ic_launcher.xml + ic_logo_* variants)
```

### The three things that most often trip people up

1. **`versionCode` is `git rev-list HEAD --count main`** — recompute it at
   release time; never hand-write it. See VERSIONING.md.
2. **CI needs `fetch-depth: 0`** — the build derives the version from git tags
   and commit count; a shallow checkout produces a broken version.
3. **Adaptive icons are masked to a 66dp safe zone** — keep artwork within a
   33dp radius of centre or it gets clipped. See BUILD_AND_RELEASE.md §7.
