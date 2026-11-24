# Issue #9: Morphe Patcher 2.5 migration and memory use

[Issue #9](https://github.com/ryuya0124/gemini-microg-patches/issues/9) reported that the bundle was built for Morphe Patcher 1.12.0 while current Morphe uses Patcher 2.x, and that patching used a lot of memory until Morphe crashed.

## Causes

1. **Old bundle version forced legacy resource decoding.** The MPP manifest declared `Patcher-Version: 1.12.0`. Patcher 2.x keeps the 1.x behavior "decode all resources before patches execute" whenever any loaded bundle declares a version below 2.0.0 ([PatcherBehavior](https://github.com/MorpheApp/morphe-patcher/blob/v2.5.0/src/main/kotlin/app/morphe/patcher/PatcherBehavior.kt), adoption `EVERY_BUNDLE`). With this bundle loaded, Google was always fully decoded (11,265 files), also for the official patches loaded alongside it.
2. **String rewriting made more classes mutable than needed and compiled smali per instruction.** The package/GMS/permission redirects made every candidate class mutable before checking whether it had a string to change, and rewrote each `const-string` from smali text. Patcher 2.5 adds `replaceStrings()` for this: one indexed pass, and a class becomes mutable only if a string in it is replaced.
3. **Source no longer compiled with Patcher 2.5.** `compatibleWith(String)`, `getAllClassesWithString()` and `addInstructions(String)` without an index are hidden in 2.5. Desktop 2.x also renamed the result JSON field `patchingSteps` to `stepResults`, and ARSCLib renamed `valueAsString`/`setValueAsString`/`writeApk`.

## Changes

- MPP declares `Patcher-Version: 2.5.0`; tools pinned to Morphe Desktop 2.3.0 (Patcher 2.5.0, same as Android Manager 2.7.1) and official patches 2.3.0 for comparison.
- Compatibility is declared with `Compatibility` objects listing the exact registered versions from `VersionHookRegistry`. Unregistered versions are still refused at patch time.
- Gemini target, Google package and GMS redirects and clone permission references use `replaceStrings()`. The GMS replacement keeps the previous per-string rules unchanged.
- Read-only searches (process-name provider, signature verifier, eligibility, main-process check) inspect the original classes and make only the class they change mutable.
- Bytecode patches that use the instruction indexes depend on a read-only resource step, so Patcher 2's on-request decoder loads the 87 MB Google resource table before the indexes are built instead of on top of them. The manifest is not changed.

## Verification (2026-11-24, macOS, Desktop 2.3.0)

Inputs: Google 17.72.17.ve.arm64 base (`0ecc808f…1252`), xxhdpi split (`b8867397…cf7d`), Gemini 1.0.995038376 base (`af62293b…be`).

**Output equivalence with the published v0.3.5 MPP** (both generated with Desktop 2.3.0, default selection):

| | Google | Gemini |
|---|---|---|
| DEX classes compared (canonical instructions, registers, try blocks, fields) | 158,074 identical | 5,298 identical |
| `aapt2 dump resources` / `strings` / manifest `xmltree` | identical | identical |

`resources.arsc` bytes differ only because the on-request encoder drops two unused key strings and trailing empty `anim` entries; every resource, value and ID is the same. The xxhdpi split from `PatchSplit` matches the previous build exactly.

**Repository checks:** `check-repo.sh`, `build-all.sh` (Google, Gemini, split) with `verify-apks.sh` (DEX branches, work-profile eligibility, compliance redirect/feature gate, trampoline, clone contract, process name, permission setup, update compatibility), Gemini manifest and clone contract, `check-hook-profiles.sh`, MPP structure (28 JVM classes in Android DEX), and compilation of every `scripts/kotlin/*.kt` all pass.

**Memory and time, Google base, `java -Xmx… -jar morphe-desktop.jar patch`:**

| Max heap | Bundles | v0.3.5 | This change |
|---|---|---|---|
| 6 GB | this bundle | 3,771 MB RSS, 6.3 s | 2,057 MB RSS, 2.7 s |
| 6 GB | + official 2.3.0 | 4,505 MB RSS, 6.8 s | 2,012 MB RSS, 3.1 s |
| 1 GB | this bundle | ok, 7.0 s | ok, 5.5 s |
| 1 GB | + official 2.3.0 | ok, 7.2 s | ok, 5.9 s |
| 992 MB | this bundle | out of memory | out of memory |

Mutable classes: 350 → 348 of 158,074. Below about 1 GB of heap both versions fail while Patcher itself loads the 87 MB resource table and 14 DEX files; that floor comes from the Google APK, not from these patches. Without the resource preload step the new bundle needed about 1.15 GB, because the table was loaded on top of the instruction indexes.

## Not verified

- Generation in the Android Morphe Manager 2.7.1 GUI and runtime on a device or AVD. The dedicated Issue #8 AVDs are on the Windows host, not this Mac. The generated DEX and resources are content-identical to the runtime-verified v0.3.5 output, but HANDOFF requires a real Manager comparison after a Patcher update before release.
- Phones with less memory than the ~1 GB Java heap the Google APK needs can still fail; that is outside these patches.

## 500 MB heap on a phone

Target from the maintainer: Morphe on a phone has about 500 MB of Java heap for patching. Measured with Desktop 2.3.0 (Patcher 2.5.0), same Google base APK:

| Selection | 256 MB | 512 MB | 768 MB | 896 MB | 1024 MB |
|---|---|---|---|---|---|
| No patch selected | – | ok (384 MB ok) | ok | – | – |
| Bytecode patch only (instruction indexes, no resources) | – | ok (384 MB ok) | ok | – | – |
| Any resource patch, e.g. a read-only manifest edit | – | OOM | OOM | – | – |
| Official `Clone app` alone | OOM | OOM | OOM | ok | ok |
| Official `Disable Play Store updates` alone | OOM | OOM | OOM | OOM | OOM |
| This bundle, default selection | – | OOM | OOM | OOM | ok |
| This bundle on **Gemini** | ok | ok | ok | – | – |

As soon as any resource patch is selected, Patcher's `prepareDecodingOnRequest` parses the whole 87 MB `resources.arsc` of Google with ARSCLib before any patch runs. That alone does not fit in 768 MB, and it happens with official patches too, including editing the manifest in binary form with `resourceXml()`. A Google clone needs manifest changes (package, permissions, providers), so Google cannot be patched within 500 MB with Patcher 2.5 whatever these patches do. Gemini fits even in 256 MB.

The remaining gap between official `Clone app` (fits in 896 MB) and this bundle (about 1 GB) is the instruction index built for the string searches. Removing it would not reach 500 MB either.
