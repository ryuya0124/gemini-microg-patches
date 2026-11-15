# Issue #8: preview3-labeled reply contains a preview2 capture

Reporter [comment6883267613](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6883267613) describes the result as preview3: after sending, the update warning returns; selecting one of three generated choices permits another message. The attachment is **not sufficient to verify the preview3 code path**.

Attachment34859989 is56623 bytes, SHA256 `cf7259b442e081dba13adaaeab2945df0b574354b4c8757637f099ba9e494ebd`. Raw data stays ignored in Windows `local/reporter-preview3/` and main Mac `local/issue8/reporter-preview3/`. Do not publish the raw report or APKs.

## Build identity comes before path diagnosis

- Report root `build` is **0.3.6-preview.2**.
- Its `capture_started.diagnosticBuild` is also **0.3.6-preview.2**.
- The two history compatibility records have the preview2 layout, without the preview3 `source`/`observedCount` fields.
- No `stream_update_compatibility` records or corresponding per-process admission counters are present, despite360 observed generate envelopes.
- All three participants acknowledged stop, pending0/loss0/incomplete=false. This concerns the included instrumented capture, not the build the reporter intended to install.

The file is a different capture from the earlier preview2 attachment (different hash/processes/timing), but its internal code identity is still preview2. An older exported file, a rebuild using the older source, an update not taking effect or a mixed-source build are possible explanations; the log cannot distinguish them. Do not state that the reporter definitely failed to install, or that the installed APK is definitely only preview2. Do not treat absence of streaming audit records as evidence of a new blind spot in preview3 without first reconciling the capture version.

The initial user-facing status repeated the reporter's preview3 label before inspecting the attachment. That interpretation is corrected: **preview3 failure is not established by this report**. Its own capture shows the already-known preview2 behavior: history match4797ms/removal4798ms, a stream-thread update punt22367ms, native banner24528ms and visible warning24705ms. There is no new structured server-status/classifier event. No additional server condition or third warning transport is established.

## Published artifact cross-check

Public v0.3.6-preview.3 remains source `cdd4d6f4132cfe18cd20c1f42c4c5ecc65c20424`, MPP245259 bytes, SHA256 `dcaae09a5d189033a14cf9fa95754374eca2e8be58ae0877b062086de128d63b`. GitHub's current public asset digest matches the retained verified release and Mac-tested MPP. The runtime DEX `Schema.BUILD`, manifest version and bundled profile build all equal0.3.6-preview.3; the runtime contains `filterStream`. An updated build verifier now checks those identities together before accepting a bundle. This verifier change does not modify the already published APK runtime or require a new app release.

The previously verified clean native export identifies preview3 and contains both history and stream audit events, with four ACK/loss0/incomplete=false. The safe local report checker rejects this reporter attachment for expected preview3 (exit2) and accepts that verified native preview3 control (exit0). Neither outcome claims a fix on the affected reporter conversation.

```sh
python3 scripts/check-diagnostic-report.py /private/path/report.json --expected-build 0.3.6-preview.3
```

The checker prints only safe build/capture flags, hashes and fixed outcome counts. Exit0 means build identity matches, not that the app or capture is globally correct. Exit2 means the report/header build does not match; resolve that before attributing a regression to the expected build.

## Next action

Prepare a short reply noting that the attached report identifies preview2, with a direct preview3 release link. Ask for a fresh build using only the intended local preview3 source, the same original APK/key/data-preserving update, then a new capture after installation. Keep the old report for provenance. A genuine preview3 report remains needed to investigate its remaining behavior. Do not manufacture preview4, remove unrelated warnings, promote stable or close the Issue based on this file. The next Issue reply still requires user review/explicit authorization; no response has been sent during this analysis.
