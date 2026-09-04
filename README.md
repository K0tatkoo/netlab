# NetLab

IPv4 subnetting and VLSM trainer for Android. Kotlin + Jetpack Compose, styled
with the same neumorphic design tokens as n3d-store, Strings, Convert, Status
and Sentinel.

Fully bilingual — English and Czech — switchable from inside the app without a
restart.

## What it does

**Practice** — generated exercises, two kinds:

- *Split a network (VLSM)* — a base network and a list of subnets with host
  requirements, e.g. `10.0.0.0/24` into A: 60, B: 30, C: 14, D: 7. Fill in the
  network address, prefix, first and last host and broadcast for each; every
  field is marked individually and a wrong one shows the right answer.
- *Analyse an address* — one address and a prefix, find the network, mask,
  range and host count.

Three difficulties. Easy keeps the arithmetic inside the fourth octet; Hard uses
/20–/22 bases so blocks cross octet boundaries. Solved count, current streak and
best streak are kept.

**Step by step** — the walkthrough, available on any exercise and on anything
typed into the calculator. Each step carries prose, a line of arithmetic, a
label/value table and, where it helps, a 32-bit strip with the prefix boundary
drawn on it. Read one step at a time or the whole solution at once.

**Calculator** — two modes:

- *Subnet* — address plus a prefix slider, giving mask, wildcard, network,
  broadcast, range, host count, class and scope, with the binary laid out.
- *VLSM plan* — a base network and a slider per subnet, with the plan drawn to
  scale so wasted space is visible. Seeded with the 60/30/14/7 example.

**Learn** — six lessons: binary and octets, masks and prefixes, the −2, the
magic-number method, VLSM itself, and the six mistakes people actually make.

## Building

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug          # or assembleRelease
./gradlew testDebugUnitTest lintDebug
```

Or open the folder in Android Studio and run it — `local.properties` is
regenerated on import.

`minSdk 29`. The floor is set by `BlurMaskFilter`, which draws the neumorphic
shadows and is only honoured by the hardware-accelerated canvas from API 28.

The release build is signed with the debug key so the APK installs on a phone
without extra setup. Swap in a real keystore before publishing anywhere.

## Layout

```
core/      IPv4 arithmetic, the VLSM allocator, exercise generation and grading
i18n/      every user-visible string, as an interface with En and Cs objects
data/      DataStore-backed settings and score
ui/theme/  the house neumorphism system, shared verbatim with Sentinel
ui/        components and the four screens
```

`core` has no Android dependencies and is covered by unit tests, including the
worked `10.0.0.0/24 → 60/30/14/7` example and a 900-task sweep asserting every
generated exercise fits and has exactly one right answer.

## Notes

- No network permission. Everything is computed on the device.
- Language defaults to Czech on a Czech phone, then follows the setting.
- Prefixes may be answered as `/26`, `26` or `255.255.255.192` — all three are
  marked correct.
- `/31` and `/32` answer 0 usable hosts, the textbook convention. RFC 3021 is
  mentioned in the walkthrough rather than assumed by the arithmetic.
