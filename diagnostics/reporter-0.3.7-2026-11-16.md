# Reply after0.3.7 announcement — capture still identifies preview2

[Reporter6898164783](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6898164783) replied on2026-11-16 at02:09 JST, saying they also turned on warning marking, and attached34879289 with filename `GeminiPatch-diagnostics-0.3.7.txt`.

The attachment is87,581 bytes, SHA256 `fe4e92742d19f3ac72cdb9db6f15b4b01222f90064d3e737ba85d0cb2075bce1`. Its contents identify **0.3.6-preview.2**, both at report root and capture start. It has no0.3.7 `captureBuild`, `exporterIdentity`, `buildIdentity`, `exportedAtMs`, `captureStartedAtMs` or per-process identity. The compatibility records use the preview2 layout and contain no stream compatibility outcomes. The safe report checker rejects it for expected0.3.7 with exit2.

This is a distinct capture from34870695: participant IDs and timing differ. It is not the same byte-identical report attached again. That distinction does not establish when it was captured relative to any attempted app update; the legacy report lacks absolute capture/export timestamps.

## Recorded sequence

| Elapsed time | Evidence |
| --- | --- |
|4ms | Capture starts, diagnosticBuild0.3.6-preview.2 |
|1,115ms | Manual `user_marked_warning` event |
|8,428ms | Known history update warning matches |
|8,432ms | History warning removed |
|17,899ms | Known update warning present in reply `stream_thread` |
|18,907ms | Native banner receives that warning |
|18,993ms | Warning text automatically observed on screen |
|37,223ms | Capture stopped |

Two participants acknowledge stop, minimum observed loss0, incomplete=false. There are130 parsed-message events,37 punt observations and one manual warning marker. The manual marker is an annotation, not an update/compatibility switch and not proof of an automatic warning at that early timestamp. The later native banner/text events independently establish warning display. No new structured status/classifier error appears in this capture.

The package inventory reports Google clone17.72.17.ve.arm64 /301831138 and Gemini clone1.0.995038376 /2147483647, with the original Google/Gemini absent. No unsupported Google input version is established by this report.

## Delivery cross-check and interpretation

The public0.3.7 asset was freshly downloaded again. It is283,074 bytes with SHA256 `f0900eeb2846a83c8ee5e7f1fc856a25b580c4d52f51be445414b437e01e93dd`, matching the published GitHub digest, CI and emulator-tested package. Release target remains588859e, with0.3.7 runtime identities verified before release.

An actual0.3.7 exporter adds its own runtime build and identity fields even when re-exporting an older capture; that behavior was tested with a deliberate stale control record. This attachment therefore does not identify a0.3.7 exporter merely reading old capture data. The filename alone cannot establish the runtime used to write it.

An older saved export, an older selected/generated source, an unapplied update, another installed profile/copy or mixed-source output remain possibilities. This file cannot choose among them or prove that the reporter deliberately skipped updating. Do not report an installation mistake as fact, advertise a new fix, diagnose a new warning transport, or blame another observer gap from this attachment. Identifying the actually opened diagnostic screen and the source/build used for the generated APK would distinguish those possibilities, but no additional question or Issue reply was sent during this review.

The latest user's request was to review the new reply. The prior authorization covered the specific0.3.7 announcement; future replies still require review/authorization. No runtime change, new release, emulator launch or physical-device operation was made in this review. Raw attachment and safe summary remain ignored at Windows `local/reporter-20261116/`; do not publish raw logs/APKs/credentials.
