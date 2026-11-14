# Issue #8: incoming server response investigation

**2026-11-13 instrumentation correction:** a later native fixture test found that the older private status/classifier loggers were inserted before existing RETURN instructions. Branches targeting those original returns can skip the observer. Therefore the older runs' lack of RobinStatus/classification events cannot exclude those paths. The separate `diagnostics/issue8-logs` branch (based on frozen v0.3.5) replaces each original return location with the observer and appends the unchanged return; all one parser and six classifier return paths were checked, and a local synthetic native status1086 was captured. This corrects evidence collection only: it does not reproduce or fix the reporter's warning. The user subsequently proposed installing a logging build; a diagnostic-only MPP and explicit local export flow are being verified. Stable publication remains gated on reproduction and verified correction.

Status on 2026-11-13: the reported conversation-update card remains **unreproduced**. No cause or correction is established and no release is authorized by the verification gate. The user's latest instruction is to investigate independently and ask no further questions. The previously authorized issue comment remains; no new comment was posted during this investigation.

## What the original application actually implements

In the verified original Google17.74.18.sa.arm64 /301834567, `azim.a(Throwable):gszm` walks gRPC exceptions and reads the `grpc-status-details-bin` trailer. It selects an Any with type URL `type.googleapis.com/assistant.mobile.robin.RobinStatus` and parses the native status protobuf. The relevant fields are:

| Latest native field | Meaning established from code |
| --- | --- |
| gszm.b | Status category |
| gszm.c | Detailed wire error, mapped by gqkz |
| gszm.d | Server-supplied message |

`gqkz` includes `UNSUPPORTED_CLIENT_TOO_OLD` with wire value1086. `aziq.b(Throwable)` classifies that status; `awnq.d` maps it to the native `awln` error. Its default resource is `assistant_error_app_too_old` /0x7f1504a4, which asks for the latest Google app. That default text differs from the conversation-specific title and explanation in the issue screenshot.

Server text can nevertheless override native error text. `atix.b(awno,gszm)` uses a trimmed nonempty gszm.d when its feature flag is enabled; `awnq.d` contains a similar flag-controlled override. `awno.m(Context)` returns an explicit override instead of its default resource. Therefore a server-originated warning is technically possible. Neither the enum's presence nor the differing default text identifies the reporter's actual response.

The equivalent17.72.17 parser/classifier is `atuc.a(Throwable):ggmf` /`atuf.b(Throwable)`, with `geec` holding the same1086 wire value. Resolve descriptors from each APK; do not reuse the latest obfuscated class names on older versions.

## Transport direction and successful-response schemas

The latest ProcessQuery **incoming** stream is `guvm`. `atti` consumes it and extracts extensions including `guuy` and `guvt`. `guvj` is the corresponding **outgoing** message: `atup` constructs it to send query/operation information. The first private observer mistakenly selected guvj/guvg as response roots; that observer is insufficient evidence about received stream contents. The second observer corrects the direction and instruments the actual guvm consumer entry.

Other useful response/data types are `grrs` (GetConversationMetadata), `grru` (GetConversationTurn), `gsap` (conversation metadata) and `gsav` (turn). `baaq.invokeSuspend` is a generic unary-response coroutine. Parsed metadata can also come from the local store; a cast alone does not prove a new network response. In the completed runs, GetConversationMetadata/GetConversationTurn wrappers were not observed, while the actual ProcessQuery consumer did execute during fresh replies.

Extendable protobufs inherit their extension field set through `hglx.X`; examining only declared fields misses extensions. The revised observer scans bounded native fields and extension values, and also checks serialized protobuf bytes for the known warning text without retaining the bytes or printing text. Serialization is limited to2MiB per value and ordinary event logging to80 per schema per process. These bounds and the selected entry points mean this is **not a complete network capture**.

## Emulator observations

Used only the authenticated official Play Store Android12/API31 AVD `GeminiPatch_PlayStore_API31` /emulator-5584, ordinary shell UID2000. Stock Google17.74.18 /Gemini1.0.995038376 and same-version clones were installed; both clone installed version codes were2147483647. Only the two existing, explicitly created image/Canvas regression conversations were accessed.

The observer changes logging only, preserves method register counts and return values, uses the existing signing key, and never records message contents, accounts, conversation IDs, URLs, raw protobuf bytes or exception text. Its reprocessed APK is restricted to the4KiB-page diagnostic AVD and must not be distributed.

| Run | Native outcome | Incoming observations |
| --- | --- | --- |
| SERVER_RESPONSE | Fresh image/Canvas acknowledgements, no update card |17 stream-consumer entries;228 serialized-value scans; guvm/guuy/guvt plus gsap/gsav observed |
| Image edit + AFTER_SERVER_IMAGE_EDIT | Image-edit acknowledgement and image visible; both subsequent conversation replies succeeded; no update card |33 stream-consumer entries;232 serialized-value scans; same incoming schemas observed |

Neither run logged a RobinStatus detail, a classified client error, a known warning-text match or a read-only-input activation at the instrumented sites. This describes those completed cases only. It does not rule out a different conversation, model, feature rollout or server account condition. The image-edit harness checks visible generated-image UI and a unique acknowledgement; it does not compare image pixels to establish the quality of the requested edit.

The initial v1 run exhausted its160-event global budget on gsap alone and is retained as limited evidence, not a complete response comparison. Revised v2 avoids one schema consuming the entire global budget. Navigation and fixture results are separate from the receiver log.

## Reduced-capability negative control

As a separate, explicitly artificial test, the private ReducedCaps extension copies native `grpg` messages and empties their repeated capability field c at the two gsws.l construction sites. Shared originals, version metadata, return-value contracts and register counts are preserved. This advertises fewer supported features; it does not advertise unimplemented features or modify server permissions. The native enum is grpf (for example, IMAGE_GENERATION has wire1 and CONVERSATION_TURN_PUNT has312). This is not a proposed product change.

In the text-continuation run,29 reductions executed successfully (observed source lists had429/430 entries). Both existing test conversations still returned fresh acknowledgements. The incoming consumer executed16 times with228 bounded serialized scans; the reported card, observed structured errors and read-only activation remained absent.

In a second run with the same private APK, a new basic image request returned its unique ZERO_CAP_IMAGE_OK acknowledgement but **no generated-image control** appeared within181.6 seconds. The image-assertion harness therefore failed, as recorded, rather than treating the text acknowledgement as image success. There were16 reductions,11 incoming consumer entries and194 bounded scans; still no known warning-text match, observed structured error or read-only activation. The finally block restored the exact ordinary Google APK despite that failed assertion.

After restoration, both existing conversations returned fresh RESTORED_SERVER_CONTROL acknowledgements. Repeating the same basic red-square/white-background image request with the ordinary APK returned a new image and RESTORED_CAP_IMAGE_OK in15.5 seconds, with no update card. This single contrast is not a generation-speed benchmark. It confirms ordinary image behavior remained available after the negative control. Evidence is restored-image/image-edit-results.json and conversation-update/observer/RESTORED_SERVER_CONTROL/chat-results.json.

The request-construction control changed behavior without producing the reported update card. It does not prove that the server received no capability IDs at every protocol layer, or exclude a selective feature/capability mismatch in a different conversation. In particular, removing every capability also removes card/fallback capabilities, so a targeted comparison must preserve those when investigating an individual missing feature. Do not treat the artificial missing-image result as the reporter's defect or as a corrected failure. Private evidence is reduced-caps/ and reduced-image/; their summaries explicitly retain the failed image assertion.

## Isolated synthetic protocol/renderer check

A separate instrumentation APK, `app.geminipatch.serverprobe`, has no INTERNET permission and loads the installed Google clone's code into its **own** process. It constructs synthetic google.rpc.Status/Any/RobinStatus bytes with wire1086 and a fixed test marker, puts the envelope in native gRPC trailers, then invokes the application's actual parser and classifier.

On API31, the native parser extracted1086 and the fixed message; the classifier returned UNSUPPORTED_CLIENT_TOO_OLD. The native awno/awln renderer returned the supplied override and retained1086. Its default text was nonempty and differed from the reported title. All four assertions passed. This is an isolated parser/renderer contract test, **not an actual server rejection, not the reported UI card and not a fix**. The probe was uninstalled afterward.

## Other leads checked

- The latest original's DEX/resources do not contain the exact reported title/explanation. This is a search observation, not proof of a server origin.
- gsap.t/grco maps to DailyBriefContext; it is not an update-error card.
- gsav.m/gsvs maps to AgentScheduledTaskSourceMetadata; gsav.n/gsvr is scheduled-task metaline data. Do not rewrite these unrelated strings.
- BardAdvanced/BardClassic isReadOnly fields include account/plan state. They must not be blanket-disabled to suppress this report.
- The generic READ_ONLY_INPUT reason enum distinguishes NONE/UPSELL/TOS; it has not been linked to the update card.
- A [Google-hosted community report](https://support.google.com/gemini/thread/475647994/update-issue?hl=en-AU) dated2026-10-31 describes the same text on an up-to-date ordinary Android app while the web conversation continues. This is another user's report, not confirmation from Google and not evidence that our patch has the same cause. It is a useful independent reproduction lead.

## Evidence, restoration and next useful work

Private Mac evidence is under `/Users/ryuya/Documents/GeminiPatch/local/issue8/server-response/`. `observer-v2/` and `observer-image/` contain sanitized summary/restoration JSON and logging-only helpers; their received-metadata-only logs contain derived booleans/codes/schema paths only. Authenticated UI XML remains private. `status-probe/` contains the synthetic test source, result and cleanup report. Decompiled excerpts and APKs are private investigation artifacts, not repository assets.

Each observer/negative-control run restored the exact ordinary Google candidate in a finally block, with UID10182, code2147483647 and SHA256 `847704f9b39a6d1b22fcbf971cd9b83df305641493058376e3324961cd5f5550`. The final checker verified all four original/clone installed APK sets and retained UIDs; both stock and microG still have one matching account. The isolated probe is absent. No physical Galaxy state changed. Public v0.3.5 MPP remains182391 bytes /SHA256 `c8fde8dad57baba664fd8a8f948125fdeca77165b20a0cba37ad73b9ba771778`; no public bundle/version/tag/release was changed.

Next, obtain a genuinely failing **own** conversation through a concrete feature/schema mismatch, then relate its received status or successful-response metadata to the actual update-card renderer. Avoid guessing that wire1086 is the card, suppressing read-only checks, or advertising capabilities the client cannot parse. Compare the same conversation before and after the correction on the emulator and require a real successful continuation before publishing. Ask no additional questions under the latest instruction.

The reporter later specified that the affected chat originated on the Web, had no Gem/Canvas/Deep Research, still works on the Web, and that new app chats work. The [Web-origin investigation](issue-8-web-origin.md) adds that previously uncovered condition and records live comparisons with the latest clone and the17.72.17 public baseline. The same triggering server status/card has still not been established.
