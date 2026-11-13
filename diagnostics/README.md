# Diagnostic development notes — 2026-11-13

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

The first Android Manager patch attempt was killed with the 2 GiB emulator configuration. At 4 GiB, Manager 2.5.0/Patcher 2.3.0 exposed a JSON ABI mismatch: a call compiled against `Json.Default.parseToJsonElement` was absent in its newer serialization dependency. Use an explicitly typed `Json` instance instead of the Default receiver. The diagnostic source is being rebuilt/rechecked after this packaging-side correction; the runtime observers are unchanged.

## Remaining before diagnostic publication

- Verify default patching in Android Morphe Manager with the standalone original and the final MPP; export/install the Manager-produced APK and confirm its diagnostic screen.
- Run repository/profile checks and CI; compare artifact hash against the tested MPP.
- Publish only a diagnostic prerelease, retain stable v0.3.5 and its feed, and post a short capture request in Issue #8. Do not close the issue.
