# Issue #8: diagnostics2 reporter capture and punt tracing

Reporter comment: [6872288929](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6872288929). Raw attachment stays in ignored `local/reporter-diagnostics2/`. SHA256: `8f659e2f5c8d05d91d3fb05064c498a448c7c89fa4bc55bd08cf314bad5a876f` (25522 bytes).

## What the capture establishes

- Build 0.3.5-diagnostics.2, syntheticTest=false, capturePresent=true; 22 events. All four observed processes acknowledge stop, pending=0, minimumObservedLoss=0, incomplete=false. There is no unexpected queue/file loss in the recorded summaries.
- Bounded metadata coalescing is still intentional: 1766 observations, 58 processed, 1708 merged. All 39 turn callbacks were processed without merging. This is not a full response capture.
- Android API31, Google source 17.72.17.ve.arm64/301831138. Clone Google reports301831138; clone Gemini1.0.995038376 reports2147483647. Both original packages are absent according to the corrected inventory.
- The two setter events identify `biyy#d` (title) / `biyy#c` (body), called by `bhej#b → bjfb#w → bclv#h`. The visible title/body resources belong to the native `assistant_robin_punt_disclaimer` banner inside the chat history RecyclerView. This is stronger evidence than an assistant quoting the warning.
- Sampled metadata/turn scans did not match the warning. No structured status/classifier event was recorded. Neither absence establishes that the server sent no warning: the banner has a separate protobuf path.

## Source trace, exact Google17.72.17

Original Store base SHA256 `63721fbf8f688e8aff45af942856d5f23583549306413f156f627aa8a849cdb8`. API30 standalone variant has identical fourteen DEX files. Static class names below are version-specific, not future discovery fingerprints.

1. `bhej.b(Object,int)` reads `bckt.a:ghto`. `bckt` is the native **PuntDisclaimer** item. `ghto.b` and `.c` are passed unchanged to `biyy` title/body setters. The renderer performs no client-version comparison.
2. `ghto` wire fields1/2 are title/body, field4 is behavior: UNKNOWN0, SHOW_THREAD_WITH_PUNT1, HIDE_THREAD_WITH_PUNT2, SHOW_THREAD_WITH_DISCLAIMER3, SHOW_THREAD_WITH_DISCLAIMER_AND_DISABLED_INPUT_PLATE4. Do not strip this state: it may represent a genuinely unsupported conversation.
3. `bdbo.w(aqfo,ghto)` constructs the item. Its thread-level data comes through `bdci.e()` → `aofx.k` → `aozi.z`.
4. **History envelope:** `gffq.d:ghto` (wire field3, presence bit1) is copied into `aozi.z` by `aojo.n(gffq,ZZZ,hiiq)`; DEX offsets238/246/248 read the field and update the state flow. `aojo.k(...)` also applies this field. Prior diagnostics only scanned nested metadata/turns, so neither covered this envelope field.
5. `aoil.b(...)` builds `gffp`, supplies generated metadata/capabilities through `apow`, calls `auji.bv(...)`, then casts the returned value to `gffq`. Record this specific call-site if observed; an arbitrary parsed-value cast is not proof of fresh network provenance.
6. **Generate envelope:** `gfeh.p` (wire field13) is copied to the same flow by `aogo.a` / `aosp.a`; `gfeh.q` (wire field17) maps through `aogl.a` to the per-turn domain model. **History turn:** `gfnl.k` maps through `aoii.a` → `apae.k` → `aqfi.z`. These are distinct sources.

Searching ordinary field accesses finds no nonempty local assignment to `ghto.b` outside protobuf machinery. This supports a parsed/cached-message origin, but does **not** prove a fresh server rejection or establish which request capability/version/conversation property triggered it. Decompiled large coroutines contain errors; field propagation above was checked against DEX references. Private extraction/search files: main Mac `local/issue8/reporter-diagnostics2/`.

## Diagnostic follow-up

Diagnostics3 adds owning-profile `gffq`/`gfeh` observations and an explicit history-consumer entry, plus observation of the exact native banner field read. It reads **only the punt submessage** of full envelopes, saving presence, bounded behavior enum, known-warning booleans and fixed class/method locations. It does not serialize full history/generate envelopes, copy their text into reports, modify responses, change capabilities or suppress the warning.

Native protobuf fixture tests must cover history field3, generate field13, absent punts, banner observation, privacy canaries and retention during a5000-metadata burst. A synthetic response containing the warning proves diagnostics, not reproduction of the reporter's bug. Real authenticated regression and actual Manager application remain release checks for this diagnostic build. Record results separately when completed.

## Diagnostics3 verification completed

- MPP233997 bytes, SHA256 `e4521891ea77059a1373e74decc4f04b96e0850bc1fbcfc007146ecf1dc97030`. All31 JVM classes appear in Android DEX. Profile/site assertions pass:438 casts (new gffq17/gfeh33), history entry1, banner field observation1 and1685 native text setters. Both Desktop and actual Manager APKs pass the static object-register/return-path checks.
- Same-key native probe on authenticated API31 passes native protobuf warning/absence fixtures,5000 metadata burst, all-process acknowledgments, unexpected loss0, existing1086 parser/classifier and native text setter, canary exclusion, capture-off default and deletion. These are synthetic diagnostics checks, not a server rejection.
- Real owned Web-origin conversation loads `DIAG_SECOND_FINAL_PLAIN_OK` and produces a fresh `WEB_DIAG_THIRD_FINAL_PLAIN_OK`, without the update warning. Android document-picker export44564 bytes records four acknowledged processes, loss0, incomplete=false. It contains25 punt events: absent history warning observed at `aoil#b`, `aojo#k` and the transport consumer, plus absent turn/generate punts. No chat marker is exported. Thus the formerly missing top-level history path is now observed during real loading.
- The initial live harness tried Open Gemini while the Start feedback dialog remained open. That navigation was refused. A later UI dump process exited137 before the Send action; there was no verified send or app-warning reproduction from that attempt. After stopping the simultaneous account-free AVD, the fresh guarded WEB_DIAG_THIRD_FINAL run above passed. These harness attempts are retained privately and not counted as application failures.
- Both authenticated clones were restored to their exact ordinary APK sets/UIDs; all four stock/clone APK sets and matching stock/microG test account were checked. Physical Galaxy was untouched.
- Actual Android Morphe2.5.0/Patcher2.3.0 imported the exact MPP and applied all12 source-default Google patches to the311733479-byte API30 standalone original. Its signed314715721-byte APK SHA256 `ad3629bbdbc071fc2ebdaae558c3ac271c2c4d2ecd2b8ff15b40cff03b99ad7e` installs/launches on account-free API31, starts capture off and exports a2119-byte real document-picker report with all four inventory keys, three acknowledged processes, unexpected loss0. The test contains only Start/Stop, no artificial warning marker. The exact prior account-free APK set is restored afterward; only that clone's test data is reset. Manager signing material is retained.

Private evidence: diagnostic worktree `local/verification/d3-build/`, `d3-verified/`, `manager-install3/`, `static3.log`, `static-manager3.log`. Account-free test AVD and signed-in AVD are stopped after restoration, retaining their data.

## Issue communication constraint

The user now requires reviewing **every Issue reply before posting**. The interim receipt/status comment6874790622 was posted before that instruction, then explicitly deleted at the user's request; absence from Issue #8 was verified. Do not post another acknowledgment, capture request or status reply without review. Diagnostic code/tests can continue independently. Keep raw reports/private artifacts out of Git.

**Stable warning fix remains unverified and unpublished.** Cause, natural emulator reproduction and verified correction are still required. Keep Issue #8 open; no additional environment questionnaire.
