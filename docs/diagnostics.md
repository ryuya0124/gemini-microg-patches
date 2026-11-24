# Conversation update warning: diagnostic build

This experimental prerelease is the normal **0.3.8** patches plus an opt-in diagnostics screen for [Issue #8](https://github.com/ryuya0124/gemini-microg-patches/issues/8). The stable update source stays on 0.3.8.

It supports **Google `17.72.17.ve.arm64` (301831138) only**.

## Update the installed app

Keep your original APKs, Gemini installation, Morphe signing key and settings. Do not uninstall anything or clear app data.

1. Download `gemini-microg-patches-0.3.9.mpp` from [the prerelease](https://github.com/ryuya0124/gemini-microg-patches/releases/tag/v0.3.9).
2. In Morphe Manager, open **Sources**, expand **Gemini microG patches**, tap **Update** and choose the downloaded file. The card must show **0.3.9**.
3. Open **Google**, tap **Patch** and keep the default selection, including **Conversation diagnostics**.
4. Tap **Install** and confirm Android's **Update**. Importing the file alone does **not** update the app.

## Record the warning

1. Open **Gemini diagnostics** from the app list.
2. Tap **Start capture**, then **Open Gemini**. Open the affected chat and send two messages without restarting the app.
3. Return to Gemini diagnostics. If the warning appeared, tap **Mark warning shown**.
4. Tap **Stop capture**, then **Save report**, and choose where to save it.
5. Attach the new `GeminiPatch-diagnostics-….json` to Issue #8. GitHub may need it renamed to `.txt` or zipped.

Recording stops after 15 minutes. Starting again replaces the previous report. **Delete report** removes it from the app. Nothing is uploaded automatically.

## What the report contains

App versions (Google, Gemini, microG, Play services, Play Store), Android version and phone model, numeric feature IDs, client version strings, response and error codes, app code and view identifiers, where a warning appears in a response (field path only) and whether it was still present after the 0.3.8 warning handling, and booleans for whether known warning text was detected.

It does **not** save chat text, titles, account names, email addresses, conversation IDs, URLs, credentials or raw responses.

## For maintainers

The report identifies the installed diagnostic build only by an opaque code (`r`, `cr` and each process `identity.r`). `python3 diagnostics/package.py token build/diagnostics 0.3.9` prints the code of a version; `scripts/check-diagnostic-report.py REPORT --expected-build 0.3.9` checks a report against it without printing its contents.

## Return to stable

Update the source back to the stable 0.3.8 file (or the remote source), repatch the same original Google APK with the same signing key, and install as an update. The diagnostics icon disappears; app data is kept.
