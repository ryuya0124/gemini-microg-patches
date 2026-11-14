# Conversation update warning: experimental preview

This preview adds **Conversation update warning compatibility**. It ignores the recognized English “Conversation requires an update” warning when loading existing chats and keeps the conversation history. The affected reporter's conversation has not yet been verified with this change. This is an experimental preview, not a confirmed resolution of Issue #8.

The compatibility patch supports **original Google17.72.17.ve.arm64 /301831138 only**. Keep your existing Gemini installation. Do not downgrade another working Google version to try this preview.

## Install while keeping your data

1. Import the supplied `gemini-microg-patches-0.3.6-preview.2.mpp` locally in Morphe Manager. Check that the source version is0.3.6-preview.2.
2. Select the **original, unpatched Google17.72.17 APK**. Keep the source defaults, including **Conversation update warning compatibility** and **Conversation diagnostics**, plus your previous update-blocking choice.
3. Build and install as an update with the **same Morphe signing key**. If Android rejects it, stop; do not uninstall, clear app data or reset microG.
4. Open the affected existing conversation and try sending a short message. A warning disappearing is separate from receiving a reply.

The included optional diagnostics screen can record what happens using the [capture steps](diagnostics.md#record-the-warning). Capture stays off until you start it; nothing uploads automatically. If the warning disappears but a message still fails, keep that report as evidence of the remaining problem. Do not post account credentials or raw logcat.

Warning compatibility is **enabled by default**. With capture started, the report also records whether the history warning matched, was removed, was retained, or the filter failed. A local per-process call number links the match and removal events. Failure stages and before/after presence flags are fixed technical values; no conversation contents or identifiers are saved. Existing stream/status observations remain available for a subsequent send failure.

## Restore the stable patch

Import the stable source, repatch the same original Google APK with the same signing key and previous options, and install as an update. No data reset is needed.

## Development record

The selective response filter and its version-specific analysis live under [the experiment notes](../diagnostics/experiments/history-update-compat/README.md). The package contains the core compatibility helper; test-only warning injection and instrumentation are excluded. Stable release notes should describe user-visible changes only; detailed test results remain in development records.
