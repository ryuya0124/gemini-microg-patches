# Issue #8: diagnostics3 reporter capture

Reporter reply: [6877407950](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6877407950), posted2026-11-14 at12:41:10 UTC. It quotes the diagnostics3 instructions and attaches a new report, without additional reproduction details. No response has been posted by us to this reply; user review is required first.

Attachment SHA256 `6d2bedb8601140827894c7fc9598bb3c9a17f07d555ff227c596aaf5e3b28c34`,28518 bytes. Raw report remains ignored in Windows diagnostic worktree `local/reporter-diagnostics3/` and main Mac `local/issue8/reporter-diagnostics3/`. Do not publish raw reports, account data or APKs.

## What is now observed

Build0.3.5-diagnostics.3, schema2, syntheticTest=false, capturePresent=true. All three observed processes acknowledge stop with pending0/observerLost0; minimumObservedLoss0 and incomplete=false. This describes the instrumented capture, not all app activity or all network traffic. Metadata coalescing remains intentional:1766 observations,51 processed,1715 merged. All39 turn callbacks,12 full-history callbacks and the single banner callback are processed without merging or unexpected loss.

| Elapsed time | Observed path | Warning state |
| --- | --- | --- |
|10052ms | `atze#a → hggx#a → goif#l → goid#run` receive callback | `gffq.d` present; behavior1; known title/body match |
|10054ms | `aoil#b → aoik#invokeSuspend` history RPC return | Same history-level warning flags |
|10054ms | `aojo#k → aojf#invokeSuspend` history application | Same history-level warning flags |
|10055–10057ms | Historical turns through `aojo#w` / `aojo#k` | Per-turn `gfnl.k` punt absent |
|10815ms | `bhej#b` native banner | Present; behavior1; known title/body match |
|10815ms | `biyy#d` / `biyy#c` native text setters | Known warning matched |
|11349–11350ms | Native title/body views | Warning visible in `assistant_robin_punt_disclaimer` |

This fills the previously unobserved **history-envelope punt** path. The warning is already in the response object before the history mapper and UI renderer; it is not first constructed by the renderer's local version comparison. The logger records flags, not exact wording, so avoid claiming byte-for-byte response identity or a warning/conversation identifier that was not collected.

## Exact API and owning source

Source Google17.72.17.ve.arm64/301831138, original Store base SHA256 `63721fbf8f688e8aff45af942856d5f23583549306413f156f627aa8a849cdb8`. The standalone API30 variant has identical fourteen DEX files.

- `aoil.b` constructs `gffp`, puts generated `ggjl` client metadata into `.g`, and awaits `auji.bv(...)`. It then casts the result to `gffq`.
- `aune` implements `auji`; its `bv` delegates to `auld`. `auld.invokeSuspend` constructs a unary method descriptor for **`assistant.mobile.robin.RobinService/ListConversationTurns`**, request `gffp`, response `gffq`.
- `aune.cy` delegates through the normal call controller to `aukg`; `aukg.invokeSuspend` invokes `hgzh.e` with the native channel/method/request. `hggx` is a forwarding native response listener. `goif.l` delivers queued inbound response objects to that listener. `atze.a` intercepts native `onMessage`, reads response experiment metadata and forwards the object; its `gffq` branch does not construct the warning.
- The observed response therefore belongs to the native history RPC delivery path. It supports a service-response origin for the warning. No raw packets/server implementation were captured: do not invent the backend condition, rule out every transport/cache layer, or describe this as a captured send rejection.
- `gffq.d` is wire field3 containing `ghto`. Behavior1 is `SHOW_THREAD_WITH_PUNT`. The source path into `aozi.z → aofx.k → bdci.e → bdbo.w → bckt.a → bhej.b` is recorded in [reporter2 analysis](reporter-log2-2026-11-14.md).

Private source extractions: `history-transport-java`, `grpc-delivery-java`, `history-rpc-java`, `rpc-client-java`, `rpc-request-java`; read-only implementation search `FindHistoryRpc.kt` / `history-rpc-impl.txt`. These stay under the main Mac ignored evidence directory. Decompiled code is a navigation aid; use original DEX when changing hooks.

## Conditions and remaining uncertainty

Android API31; original Google and Gemini absent. Clone Google17.72.17 reports301831138; clone Gemini1.0.995038376 reports2147483647. Constructed client-version observation is17.72.17.ve.arm64. This does not by itself prove every final outgoing header/metadata value.

The report has28 events: six punts, two setters, two visible-warning events, eight sampled parsed-message events, four read-only-input observations, three capability variants and capture/version controls. Read-only input is false in all four observations. No stream/status/classifier event is present. Those absences do not establish that sending was attempted, that every error path was visited, or that the server accepted a message.

Capability snapshots have405/406/406 IDs. The successful authenticated diagnostics3 Web-origin control had424/425/425/424. The22 IDs present in every successful-control snapshot but absent from every reporter snapshot are:

| IDs | Native enum meaning |
| --- | --- |
|9 | BARD_APP_USER |
|13,14,15,16,22 | Update alarm/timer, snooze/remove/stop alarm |
|139,142,147,161,178 | Open alarm-volume/auto-rotate/call-volume/lock-screen/cast settings |
|193,196,201,215,232 | Corresponding settings action cards |
|383,386,439 | Start/open stopwatch and clock lookup |
|724,726,820 | REMY_AGENT, ACTION_CARD_REMY_CONFIRMATION_CARD, REMY_TOOL_IN_LIVE |

Reporter-only common IDs164/218/492 mean NFC setting, its action card, and ROBIN_APP_FUNCTIONS_ON_LIVE. Device/rollout differences can legitimately change these lists. They are **constructed capability snapshots**, not proof of the exact final request or of causation. Static enum names are verified against this source's `gfcb`.

For capability9, `apoj` checks presence of original `com.google.android.apps.bard`. Its absence was already tested independently on the same owned Web-origin conversation, which still continued successfully; do not call that absence alone the cause. Other missing IDs mostly describe device operations, and the reporter's affected conversation contents/actual server rule are not in the privacy-preserving report. Our Web model/native UI also have not been established identical to the reporter's Auto mode.

## Emulator comparisons completed

A private extension to the already tested diagnostics3 APK removes only the22 IDs absent from all reporter snapshots. It copies `gfcc`, retains its other fields, and replaces the value at the two native `ggjl.l` construction sites without changing registers. Runtime logs confirm removal22 (21 when9 was already absent), with no reducer-unavailable event. It adds no capability. This is a diagnostic negative control, not a release patch and not an exact copy of the reporter's device/configuration.

Authenticated API31, matching Google17.72.17/Gemini1.0.995038376 pair and the same owned Web-origin text conversation:

| Case | Original Google/Gemini | Constructed capability counts | Actual result | Diagnostic export |
| --- | --- | --- | --- | --- |
| WEB_REPORTER_SUBSET | Both present |402/403/403/402 | Loaded previous owned reply; fresh WEB_REPORTER_SUBSET_PLAIN_OK; no update warning |43117 bytes; history response observed without warning; unexpected loss0; incomplete=true because not every process acknowledged stop |
| WEB_REPORTER_BOTH_ABSENT | Both absent for user0, data retained |403/403/402 | Loaded the subset reply; fresh WEB_REPORTER_BOTH_ABSENT_PLAIN_OK; no update warning |36829 bytes; both originals absent in inventory; history response observed without warning; all processes acknowledged; unexpected loss0; incomplete=false |

Both captures contain25 safe punt observations and incoming stream events, with no matched warning at history receipt or renderer and no observer-unavailable event. Counts alone are not final wire-request proof. The successful replies and UI checks are actual service interactions, not injected response fixtures. These controls show that the tested capability omissions, including their combination with both originals absent, are not sufficient to produce the warning **in this owned conversation**. They do not reproduce the reporter's conversation contents, Auto mode, hardware, account rollout or full client metadata.

## Test environment and restoration details

Preflight found a retained clone-Gemini package setting/UID/data directory with no queryable APK path. It was repaired from the exact five retained ordinary APKs, retaining UID10183/CE data inode and then verifying all four ordinary APK sets and the matching stock/microG account. No comparison ran before that guard passed.

The first private APK build failed16KiB verification because it started from an already signed fixture: removing stale v1 signature entries during signing shifted a native library. The private build now strips only the old signature entries before alignment/signing; verification then passes. This was a private harness build problem, not a released MPP defect.

An initial original-app absence setup checked the first user block in `dumpsys`, which is ambiguous for updated system Google plus its disabled base record. That attempt was inconclusive, restored before retry, and not counted as an absence comparison. The final attempt verifies normal user0 package visibility and the actual exported inventory instead. After reinstalling retained original APKs, `install-existing` re-enables their retained user setting; this is not used as a substitute for checking actual APK paths/hashes.

The small test data partition also caused INSTALL_FAILED_INSUFFICIENT_STORAGE during restoration. Test caches were trimmed; original apps were restored from retained exact APKs with matching UIDs, CE data inodes and enabled states. The private Google clone's code was removed with `pm uninstall -k --user0` to avoid the simultaneous old/new APK storage peak, then replaced with its exact ordinary APK. Its UID10182, CE data inode and recorded runtime permission states were verified retained. No app-data clear, account reset or signing-key replacement was used. Exact original/clone APK sets and matching stock/microG account were checked again. Treat the failed automatic restoration attempts as harness failures, not success; the subsequent explicit restoration verification is the completion evidence. Physical Galaxy was untouched.

Private evidence: main Mac `local/issue8/reporter-diagnostics3/caps-control/{subset-summary.json,both-absent-summary.json,preflight-repair.json,absence-restoration.json,restoration.json,final-state.json}` plus private source/build/run logs and the owned Web fixture case folders. These files and the experimental APK are not for distribution.

## Next work and release boundary

The backend trigger remains unresolved: affected conversation/model state and the history request's final metadata selection are not established by these controls. At the user's request a narrowly targeted warning filter was implemented and tested afterward; see [the compatibility experiment](experiments/history-update-compat/README.md). It preserves history and clears only the recognized update punt. Synthetic warning controls demonstrate restoration of the input UI and a real reply on an owned conversation; that is not a natural reproduction of the reporter's affected state.

Diagnostics3 already locates the warning from history response through the native banner. The new0.3.6-preview.1 MPP is an experimental compatibility candidate, not merely another observer. The reported condition has not naturally reproduced on our emulator and the candidate has not been verified on the affected reporter conversation. Do not close Issue #8 or publish a stable warning fix. Do not send an acknowledgment, question or trial request without user review.
