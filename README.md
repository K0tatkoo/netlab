# NetLab

IPv4 subnetting and VLSM trainer for Android. Kotlin + Jetpack Compose, styled
with the same neumorphic design tokens as n3d-store, Strings, Convert, Status
and Sentinel.

Fully bilingual — English and Czech — switchable from inside the app without a
restart.

## What it does

There are two modes and one switch between them. Everything else is a tool
behind an icon.

### Learn

A nine-chapter course written for somebody who has never seen a subnet mask,
read one page at a time. It starts at "what is an IP address" and finishes with
a full VLSM plan, and every worked example prints every operation — including
the ones that feel too small to write down, which are the ones that get skipped
and then get the answer wrong.

1. What are we even doing — addresses, blocks, and why networks get split
2. Binary, the small amount you need — the eight columns, and the powers of two
3. The mask and the prefix — prefix ↔ dotted mask, both directions
4. Inside one block — block size, alignment, the four landmarks, the −2
5. **The recipe: one address → everything** — six steps, then three worked examples
6. The other method: binary AND — what schools often teach, and when to use which
7. VLSM — the five steps, why largest-first, two worked examples
8. Traps and the final check
9. The cheat sheet — what to write in the margin before you start

Pages carry paragraphs, tables, worked calculations set in monospace, numbered
recipes, 32-bit strips, place-value strips, networks drawn to scale, and check
questions with the answer hidden until you commit to one.

**The method taught is the magic number** — `256 − the mask octet`, round down —
because it is the one that works on paper under time pressure and the one exams
are built around. Binary AND gets its own chapter as the proof of why that
works, and as what to write when the homework wants the working shown. Neither
needs a calculator.

### Exercise

The same two exercise kinds as before, but walked one stage at a time. Each
stage asks for exactly one move of the method and is checked on its own, so a
mistake stops where it happened instead of poisoning every field downstream.

- *Split a network (VLSM)* — three stages: **drag the subnets into largest-first
  order**, then a prefix each, then place every block and read its four
  addresses off.
- *Analyse an address* — four stages: mask and block size, network address,
  broadcast, then the hosts.

Every stage has a *How do I do this?* button that shows the rule without giving
the answer, and a *Show me the answer* that fills it in and counts as help
taken. A finished exercise counts as clean only if no stage needed a second go.

Three difficulties. Easy keeps the arithmetic inside the fourth octet; Hard uses
/20–/22 bases so blocks cross octet boundaries. Solved count, current streak and
best streak are kept.

**Step by step** — the full walkthrough, available at the end of an exercise and
on anything typed into the calculator. Each step carries the general rule, prose,
a line of arithmetic, a label/value table and, where it helps, a 32-bit strip
with the prefix boundary drawn on it.

**Calculator** — behind the calculator icon, two modes:

- *Subnet* — address plus a prefix slider, giving mask, wildcard, network,
  broadcast, range, host count, class and scope, with the binary laid out.
- *VLSM plan* — a base network and a slider per subnet, with the plan drawn to
  scale so wasted space is visible. Seeded with the 60/30/14/7 example.

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
ui/        components and the two mode screens plus the calculator and settings
```

`core` has no Android dependencies and is covered by unit tests, including the
worked `10.0.0.0/24 → 60/30/14/7` example and a 900-task sweep asserting every
generated exercise fits and has exactly one right answer. The course is data,
so the tests also assert that the English and Czech chapters have the same
pages carrying the same block types in the same order — a paragraph added to one
language and not the other is a test failure, not something a reader discovers.

## Notes

- No network permission. Everything is computed on the device.
- Language defaults to Czech on a Czech phone, then follows the setting.
- Prefixes may be answered as `/26`, `26` or `255.255.255.192` — all three are
  marked correct.
- `/31` and `/32` answer 0 usable hosts, the textbook convention. RFC 3021 is
  mentioned in the walkthrough rather than assumed by the arithmetic.
- The sorting stage grades the host-count sequence, not the names: two subnets
  asking for the same number of hosts may be dragged into either order.
- `app/src/debug/java/com/n3d/netlab/Previews.kt` holds design previews for
  Android Studio's preview pane. They are not rendered from the command line —
  `com.android.compose.screenshot` discovers zero previews under AGP 8.13.
