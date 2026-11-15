# Versioned diagnostics0.3.7

Prepared after reporter6892191127 supplied another internally preview2 capture. This does not establish why the reporter's expected update was absent. Version0.3.7 is an experimental distribution with the same selective history/reply warning handling as preview3, clearer installed-version confirmation, stronger capture identities and the separately opt-in displayed-reply archive. The affected conversation has not been verified with the new distribution. Do not claim a new server-side fix or accuse the reporter of an installation mistake.

## New evidence and limits

- The diagnostic launcher, Activity title, status and suggested export filename show0.3.7. Documentation explicitly distinguishes importing the MPP from installing the newly generated Google APK with the existing key/data.
- A capture stores its initiating build separately from the exporter build and timestamps. Re-exporting an old capture does not turn it into a fresh0.3.7 capture. The UI tells the user to start a new capture when its initiating build differs.
- Each observed process records its runtime build, profile SHA256, fixed main/search/other role, package installation time/version and compatibility helper/manifest identities. No APK path, certificate, account or conversation data is added.
- The compatibility patch writes a versioned manifest marker only when selected. Its runtime helper has independently inlined build/profile identities. Method presence and hook-entry counts are separate: a helper being present does not prove a native callback ran. Counts are since process initialization, not guaranteed capture-local counts; existing capture-local admission/processed/outcome counters remain available.
- The exporter reports consistent, mismatch, unknown or no_capture identities. Old/missing process summaries remain unknown; a build/profile disagreement becomes mismatch. This covers observed processes only, not code paths or processes that never participate.
- Fixed timestamps and identities can help distinguish old exports, mixed code and missing compatibility selection. They do not alone identify the cause of a failed installation or Morphe source cache/selection behavior.

`VerifyDiagnostics.kt` checks the MPP version, generated profile, profile fingerprint and executable/inlined capture/export/helper identities. This prevents a newly labeled MPP from containing an older capture/export build literal. `scripts/check-diagnostic-report.py REPORT --expected-build 0.3.7` independently checks the exporter/process runtime and fingerprint fields; exit2 means build/header mismatch, exit3 means current build with incomplete/mismatched identities. Exit0 is identity agreement, not proof of a fix or complete capture.

The bundled profile serialization is normalized before hashing and packaging, so Windows CRLF and Unix LF inputs produce the same fingerprint/runtime payload. The initial cross-platform artifact comparison caught this difference before publication; the corrected package was rebuilt and all three emulator probes rerun before acceptance.

The API31 instrumentation manifest now also exposes a private `BuildIdentityProbe` and the existing selective-copy `UpdateCompatibilityProbe`. Build using `scripts/build-diagnostic-probes.sh` with the same private signing key as the target. Test APKs and test-only classes are excluded from the MPP. Use only a dedicated account-free emulator, back up its prior APKs and restore them after testing. Never uninstall or reset a reporter's app to run this procedure.

## Release isolation

Version0.3.7 is published as a GitHub prerelease. Its experimental feed remains `diagnostics-bundle.json`; the stable feed/version are unchanged. The normal tag-release job checks its tag against the stable feed before publishing, so a plain numeric experimental tag cannot accidentally enter the stable publisher. Release notes contain user-visible changes only. Issue replies require user authorization; the user explicitly requested an update announcement for this distribution.

Verification evidence: [Android12 report](../docs/testing/versioned-diagnostics-0.3.7.md).
