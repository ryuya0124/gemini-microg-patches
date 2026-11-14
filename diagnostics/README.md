# Diagnostic development notes — 2026-11-13

**2026-11-14 / diagnostics2:** see [loss correction and further investigation](logging-v2-2026-11-14.md) for the reproduced diagnostics1 admission defect, fair bounded buffering, per-process stop acknowledgments, Manager inventory correction and the Web-origin/original-Gemini-absence comparison. Sections below describe the original diagnostics1 implementation and its historical test artifact; use the newer record for current behavior.

This branch is isolated from the ongoing latest-version/provider investigation. Base: public `v0.3.5`, commit `cf964a6f4c2239015b678e9ad1b5f0d6e53ec5b0`. Its patch algorithms are retained; their compatibility metadata now lists the exact versions from the base hook registry. No provider candidate or new version profile is included.

The reported conversation warning is **unreproduced**. This is an opt-in evidence collection build, not a fix. A stable release still requires cause, emulator reproduction and verified correction. See [capture instructions](../docs/diagnostics.md).

## Build and boundaries

Run `scripts/setup-tools.sh`, then `scripts/build-diagnostics.sh` with JDK 21 and Android platform 36. Build output is isolated under `build/diagnostics/`; neither the root stable feed nor the normal release directory is written. The diagnostic workflow builds this branch and uploads only its MPP, separate feed and checksums. The normal release job skips prerelease tag names.

`diagnostics/package.py` generates the runtime schema from the version profile, starts with fresh generated class/DEX folders, combines the baseline patches and the diagnostic add-on, includes both JVM classes and Android DEX, and uses deterministic archive metadata. `VerifyDiagnostics.kt` checks exact compatibility and runtime entry points in addition to the normal MPP verifier.

The release contains no original or signed Google/Gemini APK, signing key or emulator probe. Reporter updates must retain their own Morphe signing key and app data.

## Hook map and pitfalls

`profiles/17.72.17.ve.arm64.json` is the owning record. Only exact Google 17.72.17.ve.arm64 /301831138 is accepted. Methods and field expectations must be reanalysed for another APK; never reuse obfuscated descriptors by version resemblance.

- `Laomg;->b(Lgigv;Lhiiq;)Ljava/lang/Object;` is the incoming stream consumer. Outgoing protocol types are not equivalent evidence.
- `Latuc;->a(Ljava/lang/Throwable;)Lggmf;` parses native RobinStatus; `Latuf;->b(Ljava/lang/Throwable;)Ljava/lang/String;` classifies it. Log numeric/category/enum values and message-presence/warning booleans, never the message itself.
- Replace each existing `RETURN_OBJECT` location with the observer invocation, then append the original return. Inserting before the return alone lets existing branch labels skip the logger. Static verification checks all one parser and six classifier return paths, including direct branch/switch/handler targets.
- Preserve registers and native results. Application initialization is injected into the recorded ancestor `wry.onCreate`, with activity attachment at MainActivity onCreate entry while p0 still holds the Activity.
- Expected sites: requestVersion 5, version 1, init 1, stream 1, capabilities 2, status 1, classified 6, attach 1, readOnly 1. Observed incoming casts: gfnf 215, gfnl 82, gigv 23, giha 51, gigj 3, gihc 14 (388 total); all six roots must remain present.
- Original package literals are constructed at runtime for the version inventory. Clone-support rewriting otherwise silently replaces exact original package strings with clone names. Four manifest queries retain relevant package visibility.

## Collection limits and privacy

Capture defaults off, expires after 15 minutes, and uses shared private files across Google processes. A FIFO background worker places Stop/Save after prior events; normal and important observer queues are separately bounded. A generation token prevents stale queued observations entering a new capture. Control writes use same-directory atomic rename and event writes use a file lock.

Normal event bytes stop at 192 KiB, with important-event reserve up to 256 KiB. Incoming serialization is limited to 1 MiB per selected value. Normal schemas are sampled; capability lists are numeric/bounded/deduplicated. Overflow can drop observations. Exported pretty JSON can exceed the internal event-byte cap. Selected parsed values can originate in cache; a cast is not proof of a fresh network response. Known-text matching does not identify the warning's cause and can also match an assistant quoting it. The manual warning marker is explicitly user-supplied.

No raw packets, account identifiers, chat text, titles, URLs, exception messages or stack traces are written. Review every new event field against that rule. All authenticated UI dumps, APKs and raw test artifacts remain private under ignored `local/`.

## Verification completed

- The initial clean JDK 21 rebuild matched the first emulator-tested MPP exactly: 224793 bytes; SHA256 `6cea2bae38cfeb6bffc6e4045ad1bdc71313da5ebab7b43796b072884711336f`. This identifies the pre-Manager-ABI test build, not a release asset.
- Android 12/API31 signed-in Play Store AVD: same-key update of existing clones retained UIDs, login and the owned Web-origin conversation. A fresh native reply succeeded without the reported update warning. This is a regression check, not reproduction of Issue #8.
- Same-key instrumentation exercised the actual native status parser and classifier using local synthetic wire1086. Both callbacks were captured. Fake email, URL, conversation ID and message text were absent from exported JSON. Capture-off default, empty export, Stop preserving pending errors, all four package inventory identities and deletion passed. The synthetic probe was removed afterward.
- Actual Android document-picker export after a live chat: valid 32682-byte JSON, capture start/stop, 23 parsed-message events, four stream events, capability/version/read-only observations, no synthetic event or observer-unavailable event. No warning was manually marked in that successful session.
- The finally block restored the exact normal APK sets for stock/clone Google and Gemini. UIDs and matching stock/microG test account were retained. Physical Galaxy was untouched.

Private Mac evidence: `local/verification/auth-fixed/`, `build-inventory.json`, clean-build.log and the patched-DEX checks. Synthetic results must never be described as an actual server rejection or the reporter's defect. Older private observers' absence of status/classification logs is inconclusive because of the branch-label logging gap described above.

The first Android Manager patch attempt was killed with the 2 GiB emulator configuration. At 4 GiB, Manager 2.5.0/Patcher 2.3.0 exposed that its external JVM host does not provide the desktop JSON implementation: `Json.Default.parseToJsonElement` failed, and changing to an explicit Json builder then failed with missing JsonBuilder. The final implementation derives a Java Properties resource from the owning JSON during packaging and loads it using only the Java standard library. No new JSON dependency is required. The runtime observers are unchanged.

## Final diagnostic verification and handoff

Final MPP: **225524 bytes**, SHA256 `fa31403d1e28a8d5a4caf53d2d89290511f52c8fb578dac98b56d2849b6bffd7`. [CI run 43422753593](https://github.com/ryuya0124/gemini-microg-patches/actions/runs/43422753593) passed on source `f366419abd243cf6be4c02afc0d8acbc5c91b423`; its downloaded MPP matches the Mac test build byte for byte. Repository/profile checks passed. Its runtime extension and owning profile are byte-identical to the earlier authenticated live-chat/document-picker test above.

The final MPP was also applied through **Android Morphe Manager 2.5.0/Patcher 2.3.0**, with the source defaults (12 Google patches), to the 311733479-byte standalone Google17.72.17 original. Its 313216408-byte signed APK was saved through Android's document picker, verified, installed on the **account-free** API31 AVD, and launched successfully. The diagnostic screen starts with capture off. Start, Open Gemini (reaching the native permission explanation), a deliberately artificial manual marker, Stop and real document-picker JSON export passed. The 1382-byte report contains exactly capture_started/user_marked_warning/capture_stopped, not an automatic warning detection. The real reported warning remains unreproduced. Static checks on this Manager-produced APK confirm all parser/classifier return paths retain observers.

For this account-free install test only, the prior newer Google clone was backed up, removed to permit a downgrade, and its exact APK set restored afterward. That clone's test data was reset. No accounts were present, no Manager signing key was reset, and this procedure is **not** a reporter instruction. The signed-in AVD was handled separately with same-key updates and full exact restoration.

A final same-key synthetic native probe on the final MPP passed again: native wire1086/classifier logging, no canary text/identifiers, off-by-default, Stop ordering, all four inventory keys and deletion. Both authenticated clones were restored afterward, with all four stock/clone APK sets, UIDs and matching stock/microG account checked. Evidence: private `local/verification/auth-properties/` and `manager-install/`.

Publish only the diagnostic prerelease, retain stable v0.3.5 and its feed, and post a short capture request in Issue #8. Keep the issue open. When a reporter report arrives, distinguish real server_status/parsed-message warnings from a manual marker or assistant quotation; relate the evidence to the actual native renderer before changing behavior. A missing event is inconclusive. Do not suppress read-only status, invent capabilities or advertise a newer version as an unverified fix. Stable publication still needs cause, emulator reproduction and verified correction.
