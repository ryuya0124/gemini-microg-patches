# Displayed reply archive — development record

Feature branch `feature/preserve-displayed-replies`, based on `b39888d`. Separate from the unresolved Issue #8 warning investigation. Stable feeds and release workflows are unchanged. Experimental bundle version: `0.3.6-preview.4`.

## Owning source and update anchors

`patches/PreserveDisplayedRepliesPatch.kt` is opt-in and accepts only Google17.72.17.ve.arm64 /301831138. The feature runtime is a separate `extensions/displayed-replies.bin`; an unselected patch must not add its classes or hooks to the target APK. No archive text/media is passed to the conversation diagnostics runtime. The diagnostic patch excludes the archive runtime from its TextView setter discovery.

The Activity attachment observes native visible views while the window is focused. `MainActivity.onCreate(Bundle)` receives an entry hook using the existing p0 register; no register expansion is permitted. Resource entry names identify assistant text and generated media, not arbitrary user images, input fields or progress labels:

| Kind | Resource entry / structure |
| --- | --- |
| Text | `assistant_robin_text`, native TextView, excluding EditText |
| Image | `assistant_robin_image_generation_image_view`, `assistant_robin_generated_image_lightbox_image`, loaded ImageView drawable |
| Video | `assistant_robin_toucan_native_player_view`, exact `androidx.media3.ui.PlayerView` |

Native video source: `PlayerView.f:Lnfm;` → `nfm.h():Lneu;` → `neu.c:Lneo;` → `neo.i:Uri`. In the original app, `bprr.c(String,List,hiiq)` creates the media item and installs headers obtained from `bprr.b(hiiq):Object`. The latter has **one shared DEX RETURN_OBJECT**, despite multiple return expressions in the decompiled Java. Its return location is replaced by the observer invocation followed by the original return, preserving branch labels, registers and native results. The coroutine marker is ignored; only completed header maps are considered.

For a future APK, find the resource IDs in resources first; inspect their native binding sites and the player/data-source setup. Re-identify structures and shared DEX return locations, then add a new exact version profile. Do not reuse obfuscated class names by resemblance or declare all versions supported. `VerifyRetention.kt` compares the original and patched return/control-flow locations and checks opt-in payload isolation.

## Storage and concurrency

`ReplyStore` keeps bounded text versions, extending a streaming prefix in place and retaining longer content when the original TextView shrinks, empties or is replaced. It does not intercept server decisions. Native view identities are weak and detached on background; content persists independently.

`SavedArchive` serializes writes on one scheduled worker, with one separate media worker. Text saves are coalesced at150ms and flushed on Activity stop/destroy. A process killed before a queued save completes can lose that latest content. Completed archives are reloaded on next startup. This implementation assumes one native MainActivity UI process; multiple simultaneous archive writers across processes are not supported.

Text and media metadata share an encrypted index; media bytes have separate random filenames. AES-GCM keys stay in AndroidKeyStore. Files use random96-bit IVs, filename AAD and ciphertext-only AtomicFile writes under `noBackupFilesDir/geminipatch-saved-replies`. Failed authentication/read does not overwrite an existing unreadable archive. The UI reports storage failures. Manual clear increments a generation, pauses saving and invalidates in-flight media writes before removing files. Pause/video flags persist privately. Sign-out is not deletion; the viewer explicitly explains this.

Videos default off. Only a current native player URI is eligible: app-private files, content URIs or an allowlisted Google HTTPS host, with bounded bytes/time/redirects and MP4/WebM header checks. A Google-provided video bearer header can remain in RAM for five minutes and is cleared on background/opt-out/clear. It is never logged, persisted or forwarded across host redirects. URL/auth strings never enter the saved index. No generated content, URLs or headers enter diagnostic reports.

Image/view identities and media digests suppress repeats. Images preserve the loaded drawable up to2,048px, rather than claiming original full-resolution files. Videos persist complete files up to32MiB; inaccessible remote links, streaming manifests and larger videos are not supported. Local decoder tests do not prove production Google download/auth behavior.

## Verification workflow

`scripts/build-diagnostics.sh` runs bounded text-model tests and verifies the MPP inventory. `scripts/build-retention-tests.sh PATCHED_GOOGLE_APK [OUT]` creates a **private** fixture APK and same-key instrumentation probe; it requires the existing `KEYSTORE_PATH`, `KEY_ALIAS`, private `GEMINIPATCH_STORE_PASS`, ffmpeg, JDK21 and platform36. Start from a clean signed Google APK with Preserve displayed replies explicitly selected. Do not distribute test APKs or include fixture classes/assets in the MPP.

Morphe Desktop enable flags belong to the immediately preceding patch bundle: `-p EXPERIMENTAL.mpp -e 'Preserve displayed replies' -p OFFICIAL.mpp -e 'Disable Play Store updates'`. Assert that `appliedPatches` contains the requested optional patch; a successful overall command does not prove optional selection.

The probe uses a controlled synthetic assistant answer, a red generated-image drawable and a benign2-second green video. Hidden/user/progress canaries must be excluded. Setup removes native views and the source video, then opens the retained image/video. Reload starts a fresh target process with no source views and checks restored text/image/video; it clears through the viewer. A third fresh process verifies empty/paused persistence. Ciphertext is inspected for canary exclusion, and tampering must fail authentication. The test activities and instrumentation run in `:search`, matching the real MainActivity; a fourth phase starts that non-exported native Activity from the same UID/process and checks its archive viewer attachment. No Google account or generated-video credits are needed.

`scripts/run-retention-tests.py` executes these phases only on an explicitly selected, account-free API31 emulator. It requires `--allow-reset-test-clone`, backs up the prior APK set under ignored `local/`, and restores and hashes it in `finally`. Its dedicated test clone data is reset; never point it at a personal AVD. Supply signed resource splits using repeated `--split` arguments when the input is a Store split base. The probe, fixture and splits must share the existing signing certificate. Example:

```sh
python3 scripts/run-retention-tests.py \
  --serial emulator-5582 --expected-avd GeminiPatch_Issue8_API31 \
  --fixture build/retention-tests/google.apk --probe build/retention-tests/probe.apk \
  --split /private/path/to/matching-resource-split.apk \
  --output local/retention-test-unique-run --allow-reset-test-clone

java -Xmx4g -cp build/diagnostics/verify.jar:tools/morphe-desktop.jar VerifyRetentionKt \
  /private/path/to/original.apk /private/path/to/selected.apk /private/path/to/default-selection.apk
```

Actual results and artifact hashes are recorded in [the emulator report](../docs/testing/displayed-replies-emulator.md) when checks complete. Synthetic replacement is not evidence that every Google refusal/media path has been reproduced. Production remote-video download behavior remains separately unverified unless a real capture explicitly establishes it.
