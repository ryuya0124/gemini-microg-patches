# Issue #8: targeted history-update warning compatibility candidate

The selective compatibility helper copies the `ListConversationTurns` response and clears only the recognized English history-update punt, preserving the rest of the response. The backend reason for returning the warning remains unknown. The current candidate is0.3.6-preview.2; the reporter's affected conversation remains unverified.

Owning preview profile: [17.72.17.ve.arm64.json](../../profiles/17.72.17.ve.arm64.json). Packaged core: [HistoryUpdateCompatibility.java](../../runtime/app/geminipatch/compat/HistoryUpdateCompatibility.java). Packaged receive hook: [HistoryUpdateCompatibilityPatch.kt](../../patches/HistoryUpdateCompatibilityPatch.kt). This directory retains the tested prototype and native instrumentation sources. Private Mac evidence: `/Users/ryuya/Documents/GeminiPatch/local/issue8/reporter-diagnostics3/update-compat/`.

## Behavior

- Exact Google17.72.17.ve.arm64 source only; `atze.a(Object)` native response listener, `gffq.d`/presence bit1, `ghto` behavior1 SHOW_THREAD_WITH_PUNT.
- Requires title `Conversation requires an update` and the body phrase `This conversation was created with a newer version of Gemini`. Case-normalized, bounded English comparison. Other locales are not claimed supported.
- Copies the native response builder; clears only `.d` and its `.b` presence bit. Original response, turns, pagination, other fields and unknown fields are retained. Other punt behaviors/reasons and quoted warning text remain untouched. Reflection/schema failure returns the original response.
- Does not change client versions, capabilities, account state or outgoing requests. Optional diagnostics3 observation records the original warning before the copy; no warning text or response contents are added to the report.

## Verification already completed

- Same-key native instrumentation exercised real `gffq`/`ghto` protobuf parsing and the core helper. Exact serialized comparison proves preservation of turns, pagination and unknown field99 and no original mutation. Behaviors0/2/3/4/99, unrelated messages, title-only matches, no-punt and quoted text remain unchanged.
- A **test-only** fixture injects that known warning into the native response for the owned test conversation. Control mode1, filtering disabled: native PuntDisclaimer banner and both setters observed; UI detects the warning before sending. Mode2, filtering enabled: original warning observed before filtering, absent at the history mapper, no banner/setter warning, and fresh `WEB_UPDATE_CLEARED_PLAIN_OK` received from the service.
- Both fixture captures are explicitly syntheticTest=true and exclude fake email/URL/conversation canaries. Their allStoppedProcessesAcknowledged=false/incomplete=true flags are retained because the instrumentation lifecycle terminates capture participants; minimumObservedLoss0 is not a claim of globally complete capture. Independent UI observations and the actual fresh reply accompany the positive/negative event evidence.
- A separate **core-only candidate APK** is built directly from the clean diagnostics3 fixture. Static verification confirms the exact parameter/register, original instruction sequence and absence of fixture, subset-reduction and instrumentation classes. Its native protobuf unit suite passes again.

## Important limits and current remaining work

These tests establish selective warning removal and native UI/real-message continuation on an owned text fixture under a **synthetically injected warning**. They do not naturally reproduce the reporter's affected conversation, prove the server's reason, or establish that an unsupported conversation format can be handled. A real downstream request rejection would not be converted into a successful reply by deleting a warning field.

The core-only candidate also received a fresh `WEB_UPDATE_CORE_PLAIN_OK` service reply. Exact ordinary APK sets, UIDs and the matching stock/microG account pair were restored, followed by a fresh `WEB_UPDATE_RESTORED_PLAIN_OK` reply.

The full preview MPP was built from original APKs. Its Desktop-generated APK passed the native protobuf suite and received `WEB_PREVIEW_MPP_PLAIN_OK` from the service. Its real document-picker export identifies0.3.6-preview.1; this capture has minimumObservedLoss0 but incomplete=true because not all participants acknowledged stop. Exact ordinary APK sets, UIDs and the matching account pair were restored afterward. This continuation used an ordinary owned conversation, not the reporter's naturally affected state.

The tested MPP is242327 bytes, SHA256 `d09b0819ccb975e20c90b7cc709c1381df6727b671c3581dba1fdd020f155eb0`. Actual Android Morphe Manager2.5.0/Patcher2.3.0 generated it successfully with13 default Google patches from the API30 original Google17.72.17 APK. Static checks verify the receive hook and diagnostic parser/setter registers. The actual Manager APK installs on account-free Android12/API31. A real Start/Stop/document-picker export has schema2/build0.3.6-preview.1, all four inventory keys, three acknowledged processes, incomplete=false and loss0. This smoke capture contains only start/stop and does not claim an authenticated chat test through Manager. The wrapper subsequently restored the exact prior account-free clone APK set; its test clone data was reset for this downgrade comparison. The authenticated AVD's account/data and the physical Galaxy were untouched by this Manager test.

No stable warning fix is published. Any candidate distribution must use only the core helper, never `HistoryUpdateFixture`, its control file or the instrumentation APK. Those classes are explicitly rejected by the build verifier.

## Updating the hook for another version

Start from the version's original DEX, locate the `RobinService/ListConversationTurns` method descriptor, then follow its response type through native onMessage delivery and the history mapper into PuntDisclaimer. Confirm the warning field number, presence bit and behavior enum independently; an obfuscated class-name match is insufficient. Preserve native builder/unknown-field behavior and response identity for unrelated cases. The runtime helper currently uses this exact version's schema; changing the profile alone does not port it. Update the helper together with the exact compatible-version declaration, repeat the native negative controls, the synthetic warning UI comparison, an authenticated real reply and actual Manager generation/install. Never claim another locale based only on the English match.

The shell files under `tests/` preserve the private Mac harness recipes and its explicit evidence paths. Stage their accompanying source files at those ignored paths before running them; they are not part of the distributed patch build. `scripts/build-diagnostics.sh` builds only the packaged runtime and patch sources.

## Preview2 compatibility audit

Warning compatibility remains selected by default, and the build verifier rejects a bundle with it disabled by default. Capture remains opt-in. The compatibility callback records `history_update_compatibility` without retaining response objects or strings. These events are neither merged nor sampled; existing queue/file limits and loss accounting still apply.

| Result | Meaning |
| --- | --- |
| `no_punt` | The history receive hook ran, with no present top-level punt |
| `retained` | A different behavior, unmatched warning or bounded-text comparison was retained |
| `matched` | The exact English warning matched, before copying the response |
| `removed` | The selective copy passed validation and its punt is absent |
| `failed` | The filter returned the original response; `stage` identifies schema/match/copy/clear/validate |

Match and completion share a per-process local call number. This is not a conversation identifier. Bounded behavior, original/filtered presence and comparison booleans accompany each outcome. A match without completion is not evidence of success: inspect process acknowledgments/loss, downstream history/banner events and stream/status observations. `no_punt` proves the hook ran for that callback; it does not prove every response or send path was observed. No raw warning, conversation text, exception details or credentials are added.

Native audit instrumentation exercises all five outcomes and pairs successful match/removal calls. The failure-event case is explicitly synthetic and tests only safe logging, not a naturally failing filter. Existing protobuf preservation and unrelated-warning controls run with it. See private `preview2-runtime` evidence for actual completed runs.

The preview2 native audit suite passed, including unchanged original/turns/pagination/unknown fields, unrelated-warning controls and canary exclusion. The complete MPP generated an APK that passes the clone contract, existing parser/setter/register checks, signature and16KiB alignment. On authenticated Android12/API31 it loaded the owned Web-origin conversation and received fresh `WEB_PREVIEW_AUDIT_PLAIN_OK`. Its actual document-picker export has four acknowledged processes, loss0/incomplete=false and a `no_punt` compatibility event from the real receive hook, plus history and incoming-stream observations. This ordinary conversation has no naturally returned update warning; the match/removal path is established by the separate synthetic/native tests, not by this live run.

Preview2 MPP SHA256: `339415d49c80573de911b32eaaaeb1f20d0bd7aef18269a8167ed93dd88e7a61`. Private evidence: main Mac `local/issue8/reporter-diagnostics3/preview2-runtime/`; generated APK build inventory: isolated Mac `local/verification/preview2-build/`. The wrapper restored the exact four ordinary APK sets, UIDs and matching stock/microG account pair. At boot the retained Gemini clone setting again had no queryable APK code; preflight reinstalled its exact five retained APKs, preserving UID/CE data inode, and verified the full baseline before this comparison. This was an emulator-preflight repair, not a compatibility patch result. Physical Galaxy was untouched. Actual Manager generation/install was completed for preview1; preview2 changes only runtime audit/reporting and verifies the default flag in the packaged patch inventory.

Issue replies require user approval unless the current instruction explicitly authorizes the particular announcement.
