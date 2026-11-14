# Conversation update warning: diagnostic build

This experimental preview includes a targeted compatibility change and optional diagnostics for [Issue #8](https://github.com/ryuya0124/gemini-microg-patches/issues/8). See the [preview instructions](history-update-preview.md). The affected reporter conversation is not yet verified with the change. The normal stable source remains unchanged.

## Install without losing the affected conversation

The diagnostic patch supports **Google `17.72.17.ve.arm64` (301831138) only**. Keep your existing original APK versions, Gemini installation, Morphe signing key and update-blocking settings. Do not switch app versions for this capture.

1. Download the `.mpp` from [the diagnostic prerelease](https://github.com/ryuya0124/gemini-microg-patches/releases/tag/v0.3.6-preview.2).
2. In Morphe Manager, open Sources → Add → Select patch source files and import it locally. The source version should read `0.3.6-preview.2`.
3. Select the **original, unpatched** Google APK above. Keep this source's default selection, including **Conversation diagnostics**, and your existing optional update-blocking selection. Gemini does not need repatching for this capture.
4. Install as an update using the same Morphe signing key. **If Android rejects the update, stop. Do not uninstall, clear data, reset microG or replace the signing key.** Those actions can lose the state needed to investigate the warning.

## Record the warning

1. Open the new **Gemini diagnostics** icon in the same Android profile as patched Google/Gemini.
2. Tap **Start capture**, then **Open Gemini**. Open the affected conversation and perform the action that shows the warning.
3. Return to Gemini diagnostics. If the warning appeared, tap **Mark warning shown**. This is a manual marker, not automatic confirmation of the error.
4. Tap **Stop capture**, then **Save report**. Choose a location in Android's document picker.
5. Attach `GeminiPatch-diagnostics.json` to Issue #8. GitHub may require renaming it to `.txt` or putting it in a ZIP. No raw logcat is needed.

Capture is off until you start it and stops after 15 minutes. Starting a new capture replaces the previous report. **Delete report** removes the app's local capture; delete any exported copies separately. Nothing uploads automatically.

## What the report contains

It contains relevant Google/Gemini package versions, Android API level, numeric capability IDs, client version strings, parsed response-schema counters, structured error codes and booleans indicating whether known warning text was detected. Fixed app code/view identifiers help locate the warning. Warning submessage presence, behavior enums and known-warning booleans distinguish history loading, generated replies and the native banner. Process numbers, elapsed milliseconds and per-process capture summaries help correlate events and expose missing observations.

Preview2 also records each history compatibility outcome: no punt, retained, matched, removed or failed. Match/removal events share a local per-process call number. The report includes a fixed failure stage, bounded behavior enum, text-comparison flags and before/after punt presence. These observations are not merged or sampled; queue/file limits still count losses in the report. Capture must be started before opening the affected chat. Warning compatibility is selected by default in Morphe.

It does **not save** conversation text, titles, account names, email addresses, conversation IDs, URLs, credentials, raw server responses or exception text. The app inspects bounded response data locally to calculate warning booleans; that data is not included in the exported report.

This is a limited observer, not a complete network recording. Repeated history/frame snapshots may be combined; the report counts these separately from unexpected losses. The report also indicates whether every observed process finished saving. An empty error section does not establish that the server sent no error. If the warning disappears after updating, save that report too; do not label that a verified fix.

## Return to stable

Delete the local capture if desired. Import the current stable `.mpp` locally again, repatch the **same original Google APK** using the same signing key and prior settings, and install as an update. The extra diagnostics icon will disappear. Keep existing data; no microG reset is necessary.
