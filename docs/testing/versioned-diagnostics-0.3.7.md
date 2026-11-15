# Versioned diagnostics0.3.7 — emulator verification

On2026-11-15, the default Google17.72.17.ve.arm64 /301831138 patch selection with optional Store update blocking was built from the experimental0.3.7 MPP. The local reply archive remains an optional patch and was not included in this default APK.

MPP SHA256: `f0900eeb2846a83c8ee5e7f1fc856a25b580c4d52f51be445414b437e01e93dd`.

Tested Google APK SHA256: `8d7054f716b24b4e1a54b4414b80fb5de40dd5d1b34beb0f22168d8dfe864811`.

The APK passed clone-contract, signature, selected-patch and16KiB alignment checks. MPP inventory checks include default compatibility selection, exact supported versions, optional archive selection, exclusion of probes, profile fingerprint and inlined executable build identities.

On dedicated account-free Android12/API31 `GeminiPatch_Issue8_API31`, all three same-key instrumentation probes passed:

1. **Build identity:** actual installed manifest marker/helper/profile agreement; visible diagnostic screen/versioned filename; capture/export and per-process builds; both hook-entry counters; detection of mixed builds, missing legacy identities and wrong profile; real export after an old capture marker retaining its old identity; a fresh capture clearing that stale state.
2. **Selective warning compatibility:** native history pagination, other fields, unknown fields and original messages preserved; unrelated warnings/behaviors unchanged; reply thread/turn warnings selectively removed; actual native stream constructor hook; paired outcome/failure-stage audits; private canary text excluded.
3. **Native diagnostics:** capture off by default; native wire1086 parser/classifier and incoming message/punt observations;5000-snapshot burst with other important kinds retained; original TextView setter behavior preserved; Stop ordering, process/loss summaries, canary exclusion and capture deletion.

The private probes and controlled inputs were not server replies from the affected reporter conversation. The warning handling algorithm is unchanged from preview3, whose controlled reply/history reproduction is documented separately. These tests validate the new diagnostic identities and regressions; they do not establish that the reporter's conversation is fixed.

After testing, the exact prior Google clone APK set was restored and hashed, the probe removed and the emulator verified account-free. Only that dedicated test clone's data was reset. No physical device, authenticated emulator or user conversation was changed. Private evidence: Mac `/Users/ryuya/Documents/GeminiPatch-v037/local/verification/{emulator-final,clean,probes}`. Do not publish APKs, raw capture files or signing credentials.
