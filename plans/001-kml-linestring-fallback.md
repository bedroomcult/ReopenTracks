# Plan 001: KML export gains LineString fallback + complete Schema declarations

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 3c23a9f54..HEAD -- src/main/java/de/dennisguse/opentracks/io/file/exporter/KMLTrackExporter.java src/main/res/values/strings.xml`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug
- **Planned at**: commit `3c23a9f54`, 2026-10-13

## Why this matters

KML/KMZ exports contain the full track (verified: 1357 `<when>`/`<coord>` pairs
in a real user file) but render as empty in OsmAnd, OpenLayers, SportsTracker,
ExifTool, and Golden Cheetah. Only Wanderer reads them (see README compat
table). Root cause: geometry is emitted exclusively as KML 2.3
`<MultiTrack>/<Track>/<coord>` (space-separated, no `gx:` namespace), which
those apps do not parse — they look for `<LineString>/<coordinates>`
(comma-separated) or KML 2.2 `gx:Track`. A secondary defect: `<Schema>`
declares only 4 `SimpleArrayField` entries while `<SimpleArrayData>` emits up to
9 names, so strict validators see undeclared fields. This plan adds a
`<LineString>` fallback per segment (existing `<Track>` output untouched) and
declares every emitted field. KMZ inherits the fix automatically because
`KMZTrackExporter` zips the KML output as `doc.kml`.

## Current state

The relevant files, each with one line on its role:

- `src/main/java/de/dennisguse/opentracks/io/file/exporter/KMLTrackExporter.java` — sole KML writer; owns `<Track>`/`<coord>`/`<Schema>` output (lines 1–524)
- `src/main/java/de/dennisguse/opentracks/io/file/exporter/KMZTrackExporter.java` — zips KML output as `doc.kml`; needs no change (verified lines 30–40)
- `src/main/java/de/dennisguse/opentracks/io/file/importer/KMLTrackImporter.java` — already parses both `coord`/`gx:coord`, `Track`/`gx:Track`; needs no change (verified lines 33–48)
- `src/main/res/values/strings.xml` — display-name strings for Schema fields; 4 of ~9 needed exist (verified lines 161–185)

Key excerpts as they exist today (with `file:line` markers):

Segment lifecycle (`KMLTrackExporter.java:317-369`): `writeOpenSegment()`
prints `<Track>` and clears 9 sensor lists; `writeTrackPoint()` prints
`<when>` + `<coord>lon lat alt</coord>` (space-separated via
`getCoordinates(position, " ")`, line 377) and appends to the sensor lists;
`writeCloseSegment()` prints `<ExtendedData>/<SchemaData>`, then
`</Track>`. There is deliberately no `<LineString>` anywhere in the file
(verified: zero matches).

Coordinate helper (`KMLTrackExporter.java:468-474`):

```java
private static String getCoordinates(Position position, String separator) {
    String result = position.longitude() + separator + position.latitude();
    if (position.hasAltitude()) {
        result += separator + position.altitude().toM();
    }
    return result;
}
```

`Position` is a Java record with nullable `Double latitude/longitude`
(`Position.java:16-24`); `.longitude()`/`.latitude()` return `Double`,
`.hasAltitude()` guards altitude. `TrackPoint.hasLocation()` delegates to
`position.hasLocation()` (`TrackPoint.java:146-147`), which requires both
lat and lon non-null (`Position.java:55-57`).

Schema header (`KMLTrackExporter.java:248-258`): declares exactly 4 fields —
speed, power, cadence, heartrate:

```java
writeSimpleArrayStyle(EXTENDED_DATA_TYPE_SPEED, context.getString(R.string.description_speed_ms));
writeSimpleArrayStyle(EXTENDED_DATA_TYPE_POWER, context.getString(R.string.description_sensor_power));
writeSimpleArrayStyle(EXTENDED_DATA_TYPE_CADENCE, context.getString(R.string.description_sensor_cadence));
writeSimpleArrayStyle(EXTENDED_DATA_TYPE_HEARTRATE, context.getString(R.string.description_sensor_heart_rate));
```

But `writeCloseSegment()` (`KMLTrackExporter.java:332-369`) emits up to 9
`SimpleArrayData` names: `trackpoint_type`, speed, distance, power, cadence,
heartrate, `elevation_gain`, `elevation_loss`, `accuracy_horizontal`,
`accuracy_vertical`. Five are undeclared: distance, trackpoint_type,
elevation_gain, elevation_loss, accuracy_horizontal, accuracy_vertical
(trackpoint_type is a string enum, the rest float).

Existing display strings (`strings.xml`): `description_speed_ms` (174),
`description_sensor_cadence` (175), `description_sensor_heart_rate` (176),
`description_sensor_power` (177), `description_altitude_gain` /
`description_altitude_loss` (161–162, formatted `%1$d m (%2$d ft)` — NOT
suitable as a Schema displayName; use plain literals instead, see Step 2).
No strings exist for distance, trackpoint_type, or accuracies — again use
plain literals, do not add translatable strings for machine-readable schema
names (matches existing pattern where `writeSimpleArrayStyle` takes a
display label, not the field name).

Repo conventions that apply here, with exemplar: string output via
`printWriter.println` with CDATA for user text (`StringUtils.formatCData`,
e.g. `KMLTrackExporter.java:300`); sensor values formatted with
`SENSOR_DATA_FORMAT` (US locale, 1 fraction digit, no grouping,
`KMLTrackExporter.java:60-65`). Match both. Test pattern: round-trip
`ExportImportTest.kmz_with_trackdetail_and_sensordata()` in
`src/androidTest/java/de/dennisguse/opentracks/io/file/importer/ExportImportTest.java`
— device test, NOT runnable in CI (build+lint only per owner instruction).

## Commands you will need

| Purpose   | Command                  | Expected on success |
|-----------|--------------------------|---------------------|
| Typecheck (via lint job) | `./gradlew lintIrreproducibleDebug --stacktrace` on CI | exit 0 |
| Build | `./gradlew assembleIrreproducibleDebug assembleIrreproducibleDebugAndroidTest --stacktrace` on CI | exit 0, APK artifact uploaded |
| Local XML check | `python3 -c "import xml.dom.minidom,sys; xml.dom.minidom.parse(sys.argv[1])" <exported.kml>` | no exception (well-formed) |

No local Gradle run is expected (owner: no local runs; CI is build+lint only,
no emulator). Verification of XML semantics is a Python well-formedness +
tag-count check on a real export file, done by the owner on-device after merge.

## Scope

**In scope** (the only files you should modify):

- `src/main/java/de/dennisguse/opentracks/io/file/exporter/KMLTrackExporter.java`
- `plans/README.md` (status row update only)

**Out of scope** (do NOT touch, even though they look related):

- `src/main/java/de/dennisguse/opentracks/io/file/exporter/KMZTrackExporter.java` — inherits fix via `doc.kml`; no change needed.
- `src/main/java/de/dennisguse/opentracks/io/file/importer/KMLTrackImporter.java` — already parses both dialects; no change needed.
- `src/main/res/values/strings.xml` — no new translatable strings; use plain English literals for new Schema displayNames.
- `src/main/java/de/dennisguse/opentracks/io/file/exporter/GPXTrackExporter.java` and `CSVTrackExporter.java` — different formats, unaffected.
- Any change to the `<Track>`/`<coord>`/`<when>` output shape — third-party round-trip (OpenTracks re-import) depends on it.

## Git workflow

- Branch: `advisor/001-kml-linestring-fallback`
- Commit per step; message style: short imperative, e.g. `KML export: add LineString fallback per segment`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Add LineString coordinate accumulator to the segment lifecycle

What to do, precisely. In `KMLTrackExporter.java`:

1. Add a field next to the other per-segment lists (after `accuracyVertical`,
   ~line 92):
   ```java
   private final List<Position> lineStringPositions = new ArrayList<>();
   ```
   `Position` is already imported (line 48). No new imports needed.
2. In `writeOpenSegment()` (~line 317), add `lineStringPositions.clear();`
   alongside the other `.clear()` calls.
3. In `writeTrackPoint()` (~line 371), after the existing
   `if (trackPoint.hasLocation()) { ... } else { ... }` coord block, add:
   ```java
   if (trackPoint.hasLocation()) {
       lineStringPositions.add(trackPoint.getPosition());
   }
   ```
   Do NOT add positions for location-less points (sensor-only points have no
   coordinates; a `<coordinates>` entry without lon/lat would be invalid).
   `trackPoint.getPosition()` returns non-null `Position`
   (`TrackPoint.java:151-154`); `hasLocation()` guarantees lon/lat present.

**Verify**: `grep -n "lineStringPositions" src/main/java/de/dennisguse/opentracks/io/file/exporter/KMLTrackExporter.java` → 3 matches (declaration, clear, add). No other output changed yet.

### Step 2: Emit `<LineString>` fallback in `writeCloseSegment()` + declare all Schema fields

What to do, precisely. Still in `KMLTrackExporter.java`:

1. Placement (locked): ONE track-level `<LineString>` emitted in
   `writeEndTrack()` between `</MultiTrack>` and `</Placemark>` — a sibling
   of `<MultiTrack>`, still inside `<Placemark>`. This is the only valid
   KML placement: inside `<Track>` is illegal, and per-segment siblings of
   `<MultiTrack>` would require restructuring the writer. Segments therefore
   accumulate into one line (segment breaks survive only in `<Track>`).
   Implement exactly:
     - Field declaration (from Step 1, keep as-is).
     - In `writeBeginTrack()` (~line 296), after
       `printWriter.println("<interpolate>1</interpolate>");` add
       `lineStringPositions.clear();`.
     - REMOVE the `lineStringPositions.clear();` from `writeOpenSegment()`
       added in Step 1 (positions accumulate across segments).
     - Keep the `lineStringPositions.add(...)` in `writeTrackPoint()` from
       Step 1 unchanged.
     - In `writeEndTrack()` (~line 311), between
       `printWriter.println("</MultiTrack>");` and
       `printWriter.println("</Placemark>");`, add
       `writeLineStringFallback();`.
   - Add the new method after `writeCloseSegment()` (use this form —
     `StringBuilder.isEmpty()` needs Java 15+; repo is Java 17):
   ```java
   private void writeLineStringFallback() {
       if (lineStringPositions.isEmpty()) {
           return;
       }
       printWriter.println("<LineString>");
       printWriter.println("<altitudeMode>absolute</altitudeMode>");
       StringBuilder coordinates = new StringBuilder();
       for (Position position : lineStringPositions) {
           if (!coordinates.isEmpty()) {
               coordinates.append(" ");
           }
           coordinates.append(getCoordinates(position, ","));
       }
       printWriter.println("<coordinates>" + coordinates + "</coordinates>");
       printWriter.println("</LineString>");
   }
   ```
   Use the second form. `getCoordinates(position, ",")` reuses the existing
   helper (comma-separated `lon,lat[,alt]`, ~line 468) — the exact format
   2.2-era parsers expect. `StringBuilder.isEmpty()` requires Java 15+;
   repo targets Java 17 (`build.gradle: sourceCompatibility JavaVersion.VERSION_17`),
   so it is available. Positions with altitude emit `lon,lat,alt`; without,
   `lon,lat` — both valid per KML spec.
2. In `writeHeader()` (~line 248), after the 4 existing
   `writeSimpleArrayStyle(...)` lines and before
   `printWriter.println("</Schema>");`, add declarations for every name
   `writeCloseSegment()` can emit:
   ```java
   writeSimpleArrayStyle(EXTENDED_DATA_TYPE_TRACKPOINT, "Trackpoint type");
   writeSimpleArrayStyle(EXTENDED_DATA_TYPE_DISTANCE, "Distance (m)");
   writeSimpleArrayStyle(EXTENDED_DATA_TYPE_ALTITUDE_GAIN, "Elevation gain (m)");
   writeSimpleArrayStyle(EXTENDED_DATA_TYPE_ALTITUDE_LOSS, "Elevation loss (m)");
   writeSimpleArrayStyle(EXTENDED_DATA_TYPE_ACCURACY_HORIZONTAL, "Horizontal accuracy (m)");
   writeSimpleArrayStyle(EXTENDED_DATA_TYPE_ACCURACY_VERTICAL, "Vertical accuracy (m)");
   ```
   Plain English literals (not translatable resources) — machine-readable
   schema labels; matches the decision to avoid new `strings.xml` entries.
   Note: `writeSimpleArrayStyle` declares `type="float"`; `trackpoint_type`
   values are enum names (strings). KML Schema `type` for string arrays
   should be `xsd:string`, but changing `writeSimpleArrayStyle`'s signature
   risks the 4 existing fields. Keep `type="float"` for the 5 float fields;
   for `trackpoint_type` this is technically imprecise but matches the status
   quo ante (it was already emitted without any declaration). Do NOT invent
   a new typed variant — out of scope.

**Verify**:
- `grep -n "writeLineStringFallback\|LineString" src/main/java/de/dennisguse/opentracks/io/file/exporter/KMLTrackExporter.java` → declaration + 1 call site in `writeEndTrack` + 4 tag lines in method body.
- `grep -c "writeSimpleArrayStyle" src/main/java/de/dennisguse/opentracks/io/file/exporter/KMLTrackExporter.java` → 11 (1 method def + 4 old calls + 6 new calls).
- Confirm `writeOpenSegment()` contains NO `lineStringPositions.clear()` (accumulates across segments), and `writeBeginTrack()` DOES clear it.

### Step 3: Verify on CI + validate output shape against the real user file

What to do, precisely:

1. Commit both changes on branch `advisor/001-kml-linestring-fallback`.
2. Push and watch the existing CI (`.github/workflows/ci.yml`): `build` job
   (`assembleIrreproducibleDebug assembleIrreproducibleDebugAndroidTest`) and
   `lint` job (`lintIrreproducibleDebug`) must both be green. No emulator
   exists in CI; device tests do not run.
3. After merge, owner exports one track on-device (KML, not KMZ — faster to
   inspect) and runs locally:
   ```bash
   python3 -c "import zipfile,pathlib,sys,re; d=pathlib.Path(sys.argv[1]).read_bytes().decode('utf-8',errors='replace'); print('LineString:',d.count('<LineString'),'/coordinates:',d.count('<coordinates>'),'/Track:',d.count('<Track>'))" <exported.kml>
   ```
   Expected: `LineString: 1`, `<coordinates>: >=1`, `<Track>: >=1` (both
   present). Then open the same file in OsmAnd or any KML 2.2 reader and
   confirm the track line renders.

**Verify**: CI both jobs green; owner confirms `LineString: 1` and visible
track in third-party app.

## Test plan

- No new automated tests (device-only suite; CI cannot run it; owner scoped
  no local runs). Existing round-trip test
  `ExportImportTest.kmz_with_trackdetail_and_sensordata()` must still compile
  (`assembleIrreproducibleDebugAndroidTest` on CI) — it asserts KML re-import
  equality, and this plan does not alter `<Track>`/`<coord>` output so its
  expectations hold. The importer's `onMarkerLocationEnd`/`onCoordEnded` paths
  are untouched; `<LineString>` is ignored by `KMLTrackImporter` (no
  `TAG_LINE_STRING` case), so re-import is unaffected.
- Owner-side manual check (post-merge, on device): export KML, count tags per
  Step 3, open in OsmAnd.
- Which existing test to use as structural pattern: none — no test change.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] CI `build` job green on the PR (`assembleIrreproducibleDebug assembleIrreproducibleDebugAndroidTest` exit 0)
- [ ] CI `lint` job green on the PR (`lintIrreproducibleDebug` exit 0)
- [ ] `grep -c "<LineString>"` on a fresh on-device KML export returns >= 1
- [ ] `grep -c "<Track>"` on the same export returns >= 1 (legacy output preserved)
- [ ] Every `SimpleArrayData name="X"` in the export has a matching `SimpleArrayField name="X"` (check with `python3 -c` set comparison; empty diff)
- [ ] `KMLTrackImporter.java`, `KMZTrackExporter.java`, `strings.xml`, `GPXTrackExporter.java`, `CSVTrackExporter.java` unmodified (`git status`)
- [ ] `plans/README.md` status row updated to DONE

## STOP conditions

Stop and report back (do not improvise) if:

- The code at `KMLTrackExporter.java:317-369` (segment lifecycle) or
  `:468-474` (`getCoordinates`) doesn't match the excerpts (codebase drifted
  since commit `3c23a9f54`).
- `Position` is no longer a record with `longitude()`/`latitude()` returning
  `Double` and `hasAltitude()` guard (Step 2's `getCoordinates` reuse breaks).
- `StringBuilder.isEmpty()` is unavailable (toolchain below Java 15 —
  contradicts `build.gradle` Java 17; stop, do not polyfill silently).
- CI `lint` fails on the touched file for a reason unrelated to this change
  (e.g. pre-existing lint error surfaced by recompile) — report, do not fix
  unrelated lint.
- The fix appears to require touching `KMLTrackImporter` (e.g. importer
  chokes on `<LineString>`) — that contradicts the verified claim that the
  importer ignores unknown tags; stop and report the observed failure.

## Maintenance notes

For the human/agent who owns this code after the change lands:

- If per-segment styling or per-segment visibility is ever needed, the single
  track-level `<LineString>` must be split back into per-segment lines (the
  accumulator intentionally merges segments; segment breaks survive only in
  `<Track>`).
- `trackpoint_type` Schema field is declared `type="float"` but holds enum
  names — imprecise but harmless; fix by adding a typed `writeSimpleArrayField`
  variant only if a validator complains.
- The `temperature` `SimpleArrayField` seen in the user's exported file does
  not exist in repo source at `3c23a9f54` — it comes from a newer/older app
  build than this checkout. If temperature export lands in source later, its
  declaration must be added in `writeHeader()` the same way.
- What a reviewer should scrutinize: that `<LineString>` sits between
  `</MultiTrack>` and `</Placemark>` (sibling, not nested in `<Track>`), and
  that sensor-only points (no location) are excluded from coordinates.
