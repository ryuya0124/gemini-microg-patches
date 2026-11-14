# Issue #8: preview2 reporter follow-up

Reporter [comment6881821764](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6881821764) says opening the affected chat now permits one message, after which the update warning returns. Relaunching permits one message again. This does not state that every first message received a successful reply.

The attached capture is60308 bytes, SHA256 `28502663b7fffb8601849ed7fc3f32ee178e244becf9ca54d8c15ebc4e298c36`. Raw evidence stays ignored in Windows `local/reporter-preview2/` and main Mac `local/issue8/reporter-preview2/`. Do not publish the raw file or conversation data.

Build0.3.6-preview.2/schema2/syntheticTest=false. All three processes acknowledge stop, pending0 and observerLost0; minimumObservedLoss0/incomplete=false. Metadata/frame coalescing is separately counted. All330 generate-envelope observations,14 history observations,2 compatibility events and the banner callback are processed without merging or unexpected observer loss. This is evidence about instrumented paths, not a full network recording.

| Elapsed | Evidence |
| --- | --- |
|10584ms | History compatibility call1 matches the English warning, behavior1 |
|10586ms | Same call records `removed`; original punt present, corrected punt absent |
|10591–10593ms | Native receive/history RPC mapper now observes the absent history punt |
|13767ms onward | Incoming streaming frames observed |
|21113ms | Initial generate-response `stream_thread`/`stream_turn` punts absent |
|28808ms | Later generate-response `stream_thread` punt present, behavior1, known title/body match; propagated through aomg/aovf/aotn/aovo/aost/aosp |
|30457–30458ms | Native banner plus both title/body setters receive that warning |
|31160–31161ms | Native PuntDisclaimer title/body visible |

This confirms preview2's history filter actually works in the reporter's affected state. The remaining regression is a separate **generated-reply thread punt**, which preview2 does not filter. No stream-status/classifier error event is present; do not invent a gRPC rejection or infer that the reply was otherwise complete.

## Exact source and correction

Original Google17.72.17.ve.arm64/301831138 only. `aomg.b(gigv,hiiq)` extracts the `gigh` extension; its oneof case1 contains `gfeh`, which becomes `new aopx(gfeh)` before the downstream reducers. `aopx.<init>(gfeh)` simply stores the immutable response in `.a`. Source and original DEX agree on the hook boundary.

`gfeh.p` is the thread punt, wire13, `b` presence bit9/mask512; `.q` is a distinct turn punt, wire17, bit10/mask1024. The owning profile records these separately from history `gffq.d`/mask1. The correction copies the response and clears each field **only** if the same behavior1 and exact English title/body phrase match. It preserves any other warning, reply fields, unknown fields, the original message and all other presence bits. The constructor parameter hook adds no registers. Runtime audit distinguishes history, stream-thread and stream-turn match/removal/failure events. Empty stream responses are counted and sampled first4/every128; present/matched/removed/failed warnings are not merged or sampled.

The backend condition that originally attaches this warning remains unconfirmed. Handling the returned warning does not implement unsupported server features or turn a genuine send failure into success. Preview3 verification results are recorded with the compatibility experiment. No new Issue reply has been authorized by this follow-up notification.
