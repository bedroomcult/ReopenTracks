# Versioning & Changelog Rules

How ReopenTracks numbers releases and writes changelogs. Follow these exactly —
other tooling (F-Droid, the release script, CI) depends on the format.

See [BUILD_AND_RELEASE.md](BUILD_AND_RELEASE.md) for the build/sign/publish steps.

---

## 1. Two version numbers

`build.gradle` carries both:

```gradle
defaultConfig {
    applicationId 'com.reopentracks'
    versionCode 6289          // integer, MUST be strictly increasing
    versionName "v5.0.0"      // human string, MUST start with 'v'
}
```

| | `versionCode` | `versionName` |
|---|---|---|
| Type | integer | string, leading `v` |
| Used by | Android/F-Droid to decide "is this an update?" | humans, release title |
| Rule | **never reuse, never decrease** | semantic version, `vMAJOR.MINOR.PATCH` |

### versionCode = number of commits on main

This repo derives it mechanically. The canonical value is:

```bash
git rev-list HEAD --count main
```

The release script (`RELEASE.sh`) writes exactly that into `build.gradle`. Do
not invent a number by hand — a value lower than a previously published build
makes the update undeliverable.

> Caveat: `versionCode` counts commits, so it moves with *any* commit, not just
> releases. That is fine and expected here. Just always recompute it at release
> time rather than trusting the value already in the file.

### versionName = semantic version, with a leading `v`

Format: **`vMAJOR.MINOR.PATCH`** (e.g. `v5.0.0`, `v4.21.1`).

| Bump | When |
|---|---|
| **MAJOR** | Breaking change for users — new app id, removed feature, incompatible data/export format, renamed app |
| **MINOR** | Backwards-compatible new functionality (new feature, new export field, new setting) |
| **PATCH** | Backwards-compatible bug fixes only |

Pre-release identifiers are not used here; every published build is a stable
release. The `nightly` flavour derives its own version from git
(`git describe --tags`) and is not part of this scheme.

**Do not** put the flavour suffix in the changelog filename or the version name.
`versionNameSuffix` (`irreproducible`, `-debug`) is applied by Gradle per
flavour — it is not part of the release version.

### Worked examples

| Change | Bump |
|---|---|
| Fixed KML tracks not rendering in OsmAnd | PATCH → `v4.22.1` |
| Added CSV import | MINOR → `v4.23.0` |
| Renamed app to ReopenTracks, changed app id | MAJOR → `v5.0.0` |
| Updated a translation only | PATCH → `v4.22.1` |

---

## 2. Changelog files

### Location and name

```
fastlane/metadata/android/en-US/changelogs/<versionCode>.txt
```

The filename **is the versionCode**. Example: versionCode `6289` →
`6289.txt`. F-Droid and Google Play both read this directory, so the name is
not cosmetic.

### Template

`fastlane/metadata/android/en-US/changelogs/next_release.txt` is the starting
point. The release script copies it to `<versionCode>.txt` and substitutes
`(versionName)`:

```
vX.Y.Z: ReopenTracks

Changes:
-

Bugfix:
-

Developer:
-
```

### Rules

1. **First line is the title**: `vX.Y.Z: ReopenTracks`. The release script
   substitutes the version into the `(versionName)` placeholder — keep that
   placeholder if you use the script, or write the literal version if you write
   the file by hand.
2. **Three sections, in this order**: `Changes:`, `Bugfix:`, `Developer:`.
3. **Omit any section that has no entries.** Do not leave a dangling `-`.
   (See `6237.txt` below: bugfix-only, so only `Bugfix:` appears.)
4. **One entry per line**, each starting with `- `. No nested bullets, no
   markdown tables, no headings.
5. **Write for a user, not a developer.** Describe the effect, not the
   implementation. "Tracks now render in OsmAnd" — not "added LineString
   fallback in KMLTrackExporter".
6. **English only.** Translations are handled separately (Weblate). Do not
   translate this file.
7. **No emoji, no marketing copy, no exclamation marks.** Plain, factual,
   past-tense or present-tense declarative.
8. **Never edit a changelog after its release is published.** The file is a
   historical record; fix forward with a new PATCH release.

### What goes in which section

| Section | Contents |
|---|---|
| `Changes:` | user-visible additions or behaviour changes |
| `Bugfix:` | user-visible fixes; name the previous version if it introduced the bug |
| `Developer:` | build system, CI, dependencies, internal refactors, docs — things users cannot see |

### Reference examples (real, from this repo)

**Feature + fix (`6217.txt`):**
```
v4.20.0: OpenTracks

Changes:
- Allow to change timezone offset of tracks
- Simplified Marker UI

Bugfix:
- Edge-2-edge: react to virtual keyboard
```

**Bugfix only, no empty sections (`6237.txt`):**
```
v4.21.1: OpenTracks

Bugfix:
- Fixed crash on export/import UI (introduced in v4.21.0)
```

**Major/rename release (`6289.txt`):**
```
v5.0.0: ReopenTracks

Changes:
- Renamed OpenTracks to ReopenTracks (new app id com.reopentracks; installs side-by-side with legacy OpenTracks, no data migration)
- KML/KMZ exports now include LineString geometry, so tracks render in OsmAnd, OpenLayers, SportsTracker and other KML 2.2 readers

Bugfix:
- Fixed KML/KMZ track invisible in third-party apps (geometry was KML 2.3 <Track>/<coord> only)
```

---

## 3. Release commit and tag

- Commit message: **`Release: vX.Y.Z`** (exactly this shape).
- Tag: **`vX.Y.Z`** — annotated, on the release commit.
- Both the `versionCode`/`versionName` bump and the changelog file go in that
  one commit.

```bash
# recompute versionCode, write the changelog, then:
git add build.gradle fastlane/metadata/android/en-US/changelogs/<versionCode>.txt
git commit -m "Release: v5.0.1"
git tag -a v5.0.1 -m "ReopenTracks v5.0.1"
git push origin main
git push origin v5.0.1
```

If a tag must be moved (you fixed something after tagging), delete it locally
**and** remotely before recreating — see BUILD_AND_RELEASE.md §6.

---

## 4. Quick checklist

```
[ ] Pick MAJOR / MINOR / PATCH per the table above
[ ] versionName = "vX.Y.Z"
[ ] versionCode = git rev-list HEAD --count main
[ ] Write fastlane/.../changelogs/<versionCode>.txt
[ ] Title line: "vX.Y.Z: ReopenTracks"
[ ] Sections present only if they have entries; every line starts with "- "
[ ] Entries describe user-visible effects, not internals
[ ] Commit "Release: vX.Y.Z", tag vX.Y.Z, push both
```

## 5. Version history

| Version | versionCode | Notes |
|---|---|---|
| v5.0.0 | 6289 | Renamed to ReopenTracks, app id `com.reopentracks`, KML LineString fix, new icon |
| v4.22.0 | 6284 | Upstream OpenTracks; migration to Codeberg |
| v4.21.1 | 6237 | Crash fix on export/import UI |
| v4.21.0 | 6233 | Swipe-to-delete, reimplemented themes |
| v4.20.0 | 6217 | Timezone offset editing, simplified marker UI |
