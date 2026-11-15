# Conversation update warning: diagnostic build

This experimental preview includes a targeted compatibility change and optional diagnostics for [Issue #8](https://github.com/ryuya0124/gemini-microg-patches/issues/8). See the [preview instructions](history-update-preview.md). The affected reporter conversation is not yet verified with the change. The normal stable source remains unchanged.

## Install without losing the affected conversation

The diagnostic patch supports **Google `17.72.17.ve.arm64` (301831138) only**. Keep your existing original APK versions, Gemini installation, Morphe signing key and update-blocking settings. Do not switch app versions for this capture.

**The normal repository update source still serves stable0.3.5. Checking source updates alone does not fetch experimental0.3.7.** Download the0.3.7 MPP below and replace your local GeminiPatch source explicitly.

1. Download the `.mpp` from [the diagnostic prerelease](https://github.com/ryuya0124/gemini-microg-patches/releases/tag/v0.3.7).
2. In Morphe Manager, open Sources → Add → Select patch source files and import it locally. Select **0.3.7** for this build and disable older GeminiPatch sources. Importing the MPP alone does **not** update the installed Google app.
   If you already have a local **Gemini microG patches** source, expand its card and tap its **Update** action, then select the downloaded0.3.7 MPP. The source card must change to **0.3.7**. This file-selection update differs from checking remote source updates.
3. Select the **original, unpatched** Google APK above. Keep this source's default selection, including **Conversation diagnostics**, and your existing optional update-blocking selection. Gemini does not need repatching for this capture.
4. Finish patching, then **install the generated Google (Morphe) APK over your existing Google (Morphe) app**, using the same Morphe signing key. Open **Gemini diagnostics 0.3.7** in the same Android profile and check that its screen says **Installed diagnostics: 0.3.7**. This confirms the app update took effect. **If Android rejects the update, stop. Do not uninstall, clear data, reset microG or replace the signing key.** Those actions can lose the state needed to investigate the warning.
   The completed patching screen should list **Source: Gemini microG patches0.3.7**. Tap **Install**, complete Android's **Update** confirmation and wait for **Installed** before opening diagnostics. If Android asks to allow installations from Morphe, complete that permission step and return to finish the installation; opening that settings page alone does not install the APK.

## Record the warning

1. Open **Gemini diagnostics 0.3.7** in the same Android profile as patched Google/Gemini. Its screen must show **Installed diagnostics: 0.3.7**.
2. Tap **Start capture**, then **Open Gemini**. Open the affected conversation and try two consecutive messages without restarting the app. If the warning blocks the second message, stop there; do not discard the report.
3. Return to Gemini diagnostics. If the warning appeared, tap **Mark warning shown**. This is a manual marker, not automatic confirmation of the error.
4. Tap **Stop capture**, then **Save report**. Choose a location in Android's document picker.
5. Attach the newly saved `GeminiPatch-diagnostics-0.3.7-….json` to Issue #8. GitHub may require renaming it to `.txt` or putting it in a ZIP. No raw logcat is needed.

Capture is off until you start it and stops after 15 minutes. Starting a new capture replaces the previous report. **Delete report** removes the app's local capture; delete any exported copies separately. Nothing uploads automatically.

## What the report contains

For analysis, check the report's own `build` and the capture-start build against the version being evaluated before attributing a result to that version. A source version shown in Morphe or a reply label alone does not identify the exported capture. Maintainers can use `scripts/check-diagnostic-report.py REPORT --expected-build 0.3.7`; it summarizes safe fields and rejects mismatched builds without printing report contents. Keep unmatched exports as evidence and obtain a fresh capture from the intended installed build.

It contains relevant Google/Gemini package versions, Android API level, numeric capability IDs, client version strings, parsed response-schema counters, structured error codes and booleans indicating whether known warning text was detected. Fixed app code/view identifiers help locate the warning. Warning submessage presence, behavior enums and known-warning booleans distinguish history loading, generated replies and the native banner. Process numbers, elapsed milliseconds and per-process capture summaries help correlate events and expose missing observations.

Preview2 also records each history compatibility outcome: no punt, retained, matched, removed or failed. Match/removal events share a local per-process call number. The report includes a fixed failure stage, bounded behavior enum, text-comparison flags and before/after punt presence. These observations are not merged or sampled; queue/file limits still count losses in the report. Capture must be started before opening the affected chat. Warning compatibility is selected by default in Morphe.

Preview3 adds the corresponding reply-stream outcomes, distinguishing thread and turn warnings. Present/recognized/removed/failed warnings remain unmerged and unsampled. Empty stream envelopes are counted, with only the first4 and each128th emitted to keep the report bounded. That sampling is separate from unexpected loss accounting. No raw reply, warning text or conversation identifier is added.

Version0.3.7 additionally identifies the exporter runtime, capture-start build, every observed process runtime and the installed compatibility-patch marker/helper build. A profile fingerprint detects mismatched definitions. Hook-entry counters distinguish a loaded helper from history/reply callbacks that actually ran. Installation/capture/export timestamps help identify an older saved capture. These are technical identities and counters, not user contents. The report marks matching, mixed or unknown identities explicitly; missing process identities remain unknown. Exporting an older capture does not relabel it as a fresh0.3.7 capture.

It does **not save** conversation text, titles, account names, email addresses, conversation IDs, URLs, credentials, raw server responses or exception text. The app inspects bounded response data locally to calculate warning booleans; that data is not included in the exported report.

This is a limited observer, not a complete network recording. Repeated history/frame snapshots may be combined; the report counts these separately from unexpected losses. The report also indicates whether every observed process finished saving. An empty error section does not establish that the server sent no error. If the warning disappears after updating, save that report too; do not label that a verified fix.

## Return to stable

Delete the local capture if desired. Import the current stable `.mpp` locally again, repatch the **same original Google APK** using the same signing key and prior settings, and install as an update. The extra diagnostics icon will disappear. Keep existing data; no microG reset is necessary.
