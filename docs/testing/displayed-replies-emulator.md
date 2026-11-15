# Displayed replies/media archive — Android12 verification

Verified on2026-11-15 using dedicated Android12/API31 AVD `GeminiPatch_Issue8_API31` on the Mac. The emulator had no Google/microG account. No physical Galaxy or authenticated emulator was changed.

## Tested artifacts

Experimental MPP: `gemini-microg-patches-0.3.6-preview.4.mpp`, SHA256 `155e493f636bea698002763a0c580c910d047175ac1501421831281b4dfd981e`.

Clean selected Google APK: version17.72.17.ve.arm64 /301831138, SHA256 `b919e246486243380a62a13a3d41d215aa44737711a071b6389d43ddf7e8be28`. Clone contract, existing signing certificate and16KiB alignment passed. Its source is the Store base with matching resource splits; the Google APKs are private and are not distribution assets.

The [sanitized native results](displayed-replies-results.json) identify the private fixture/probe APKs by hash. They contain only fixed test statuses and booleans, without account data or captured user content. The fixture adds test screens to the clean APK; neither its classes nor its MP4 asset enters the MPP.

## Results

All four instrumentation phases passed in the **same `:search` process used by the real MainActivity**, with a force-stop between phases:

| Phase | Verified behavior |
| --- | --- |
| Setup | Visible assistant text saved; user input, hidden replies/images and progress labels excluded. Streaming text extended in place, then retained after replacement and native-view removal. A red generated-image fixture and complete benign2-second video survived source-view/source-file removal. Saved image rendered and saved video decoded through in-memory MediaDataSource. Ciphertext-only storage and authentication-tag tamper rejection passed. Video saving initially off. |
| Reload | A fresh process with no original assistant/media views restored the text, image and video. Both media viewers worked. Cancelling a delayed load did not reopen a viewer. Clearing while an image job was queued removed text/media and did not allow the queued job to restore them. |
| Cleared | A further fresh process remained empty and paused, with video saving disabled. |
| Native | The patched, non-exported real MainActivity launched from same-UID instrumentation and attached its archive viewer. This is an account-free Activity integration check, not an authenticated chat test. |

`VerifyRetention.kt` also passed against original, selected and default-selection APKs: no archive runtime when the optional patch is unselected; correct existing Activity p0 register; unchanged video-header registers, native return and branch destinations; no fixture classes. MPP inventory/default/exact-version checks, bounded text-model tests, repository checks and hook-profile checks passed. Shell syntax was checked locally; ShellCheck was unavailable on the Mac and is left to CI.

After testing, the wrapper restored and hashed the exact prior Google clone APK set and checked the emulator remained account-free. Only that dedicated test clone's data was reset. The probe was uninstalled and the dedicated emulator stopped.

## Limits of this evidence

The native display replacement and media removal were controlled test fixtures. No server safety refusal was solicited or changed. This does not establish every production late-replacement path, every transient frame, authenticated chat behavior or a remote Veo download. Video decoder/local-file persistence is verified; production Google media URL/auth and expiring-link behavior still need separate real-world evidence. The feature does not promise recovery of content that never reached the app or disappeared before capture completed.

Build/test scripts and update anchors: [development record](../../diagnostics/displayed-replies.md). User instructions and limits: [displayed reply archive](../displayed-replies.md).
