# Issue #8: first reporter diagnostic report

Source: [comment6865648676](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6865648676), attachment `GeminiPatch-diagnostics.txt`. The reporter also apologized for a delay; a short acknowledgment was posted without further questions.

Downloaded the attachment into ignored `local/reporter-20261114/`; do not commit the raw report. SHA256: `0f4877b619d108db2d110877d637fdab3c4ea9163916ce4fbf3e43e1572c39fe`. Valid schema1 JSON, 16383 bytes, 21 events, build0.3.5-diagnostics.1, syntheticTest=false, capturePresent=true.

## Established observations

- Android12/API31; source Google17.72.17.ve.arm64 /301831138; Gemini clone1.0.995038376, package-manager reported code2147483647. The Google clone reports301831138. These are observer-visible values, not independent proof of the installed manifest codes because patching can scope version lookups.
- At11388 ms, `known_warning_text_rendered` was emitted with role `other` in the native Google process. This is automatic known-text matching, not a manual marker. No `user_marked_warning` event is present. It confirms the detector saw matching warning text outside an explicitly identified user-message view, but does not identify its renderer, origin or cause. Role `other` is not sufficient to rule out every possible quotation.
- The capture ended at18916 ms. Before the warning, the native process recorded conversation metadata and turn observations. Metadata observed counts reached384; eleven sampled parsed-message events were saved in total. All saved scans reported knownWarningText=false and textScanPerformed=true.
- Four saved read-only-input observations had present=false; no server_status, client_error_classified, incoming_stream or observer_unavailable event was saved. This describes this capture only; it cannot exclude unobserved execution or server behavior.
- Two capability snapshots each contained406 numeric IDs. The successful same-source emulator capture had424/425 IDs. Both omissions and additions exist between the sets; the reporter sets also differ from each other. Account/device/feature-rollout differences are plausible, but this comparison establishes no causal capability mismatch. Never blindly copy emulator IDs or enable capabilities from that comparison.

## Material limitations discovered in the real report

The native-process `queueDropped` count reached1356 by the warning. It was already102 on the first saved metadata event. The existing sampling happens inside the background task **after** the normal 24-pending admission limit; heavy history loading can fill that queue before sampling and discard other observations. The 16-slot important queue is separate, but its independent capacity and nonblocking file locks are still bounded. Therefore absence of a status/stream/error event cannot be treated as evidence that no such event occurred.

Stop ran in the diagnostic UI process, where queueDropped=0 and observedCounters={}. Those values are process-local, not a global summary. Do not quote the final zero as a lossless capture. Preserve per-process drop/counter evidence when improving the exporter.

The relevantPackages object contains only three unique keys: Google clone, Gemini original (absent), Gemini clone. The expected original-Google key is missing. The collector normally attempts four identities, so this inventory anomaly also needs investigation (including Android Manager rewriting); it does not establish that original Google is absent and must not be used to diagnose the earlier standalone-app problem.

## Next investigation

1. Fix observer admission so repeated metadata/history observations are sampled or deduplicated before they can starve other schemas. Retain independent bounded capacity for actual status, warning and turn events; use a stress fixture to verify important evidence and cross-process loss reporting. Keep native return values/registers untouched.
2. Establish the warning's native view identity and renderer from static code / privacy-preserving fixed identifiers. The current `other` role is too broad; never collect arbitrary TextView content, account names or exception text.
3. Trace metadata/turn loading and its compatibility/read-only state before ProcessQuery. The timeline makes this worth checking, but does not prove a client-only or server-only cause.
4. Do not publish a stable fix until the actual condition is reproduced on an emulator and the same failing conversation genuinely continues after correction. Existing synthetic1086 and manual-marker tests remain instrumentation tests, not reproduction.

No application patch, version spoof, stable release or physical Galaxy change was made on the basis of this report. The diagnostic prerelease and stablev0.3.5 remain unchanged.
