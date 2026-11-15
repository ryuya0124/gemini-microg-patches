# Maintainer-side delivery audit — 2026-11-16

The user asked whether our side could explain repeated preview2 captures after0.3.7 was announced. No assumption was made that the reporter ignored the instructions. No new Issue reply or environment question was posted.

## Confirmed shortcomings

**Update-channel guidance:** the normal repository `main/patches-bundle.json` still intentionally advertises stable0.3.5. There is no `dev` branch feed for the0.3.7 experiment. Morphe's normal remote update check therefore does not discover0.3.7, even though it is a newer public GitHub prerelease. Local imported sources also require explicitly choosing a replacement file. During this audit, Manager initially reported sources current while its local GeminiPatch source was stillpreview1. That UI status is not confirmation that the experimental local bundle was updated. Our instructions did not prominently distinguish these update routes. The [instructions](../diagnostics.md) now do.

**Verification gap:** before0.3.7 publication, the new runtime/APK was exercised on Android12 using Desktop-generated APKs. Actual Android Manager generation had been verified for older distributions, but the full0.3.7 Manager upgrade/install/export route was not repeated before that publication. That was a maintainer-side validation gap. It has now been tested as described below. The audit does not retrospectively turn that missing prerelease check into a completed one.

The current public MPP is the expected283,074-byte file, SHA256 `f0900eeb2846a83c8ee5e7f1fc856a25b580c4d52f51be445414b437e01e93dd`. Its executable capture/export/helper identities are0.3.7, not merely its manifest label. No release-asset substitution was found.

## Actual Manager test

Dedicated account-free Android12/API31 AVD `GeminiPatch_Issue8_API31`, normal shell UID2000, Android Morphe Manager2.5.0/Patcher2.3.0:

1. The existing enabled local source was0.3.6-preview.1. Its Update action selected the exact public0.3.7 MPP, with no Manager restart. The card became0.3.7 and19 named patches.
2. Selected the original API30 Google17.72.17 APK, SHA256 `a269697a169ce4420b3873d89c9bd0f6c400d793b5cb9c2e2603a476a6a316ac`. Manager applied13 default compatible Google patches, with no Expert mode or custom patch options. Optional reply retention and Store update blocking were not added.
3. The completed Manager screen identified source0.3.7. Saved its APK through the actual Android document picker. Output314,719,817 bytes, SHA256 `f5f30d2acb4966d47f0bdfeb49e1f61c85da963a60248094411eeff9e027f66e`, using the existing private Manager test signing key. Signature/alignment passed. DEX inspection found0.3.7 runtime/export/helper identities, BuildAudit and the actual reply constructor hook, with no old preview executable build literal.
4. A preliminary install via ADB on the disposable clone showed the0.3.7 diagnostic screen and produced a consistent0.3.7 report through the real document picker. This alone did not test Manager's Install button, so the following separate update test was also run.
5. Backed up the ordinary test clone APKs, installed the retained Manager-signedpreview1 APK on the disposable account-free clone, and created/stopped a legacy capture through its actual diagnostic UI.
6. Returned to Manager's0.3.7 result screen and pressed **Install**. Android initially refused installations from Morphe because its permission was denied in this AVD. Allowed that source for this test, returned to Manager, pressed Install again and completed Android's **Update** confirmation. Manager reached **Installed**. The installed APK hash exactly matched the new0.3.7 output and the old test-clone UID was retained.
7. The updated diagnostic screen showed **Installed diagnostics:0.3.7** and recognized the retained legacy capture. Saving it produced exporter build0.3.7 with a preview1 capture header and identity mismatch, confirming retained app data and correct separation of exporter/capture identities.
8. A fresh UI capture/export reported build/capture0.3.7, matching per-process manifest/helper/profile identities, all participants acknowledged, loss0 and incomplete=false.

This test did not reproduce a source update generating preview2 code, an installer delivering an older cached APK, or a0.3.7 exporter reporting itself as preview2. It uses Manager2.5.0 and an oldpreview1 source/install; it does not establish the reporter's Manager version, source selection, profile, installer result or exactpreview2 upgrade state. No authenticated conversation or reported server warning was exercised.

## Source inspection and cleanup

Manager2.5.0 source tag inspected at commit43d0b641a638cddff84ddfd63115eda18d68f614. Its [remote manifest resolver](https://github.com/MorpheApp/morphe-manager/blob/43d0b641a638cddff84ddfd63115eda18d68f614/app/src/main/java/app/morphe/manager/domain/bundles/RemotePatchBundle.kt) reads main/dev JSON feeds; a GitHub prerelease alone is not that feed. Its [local importer](https://github.com/MorpheApp/morphe-manager/blob/43d0b641a638cddff84ddfd63115eda18d68f614/app/src/main/java/app/morphe/manager/domain/repository/PatchBundleRepository.kt) replaces local source files and reloads source metadata. The [external patch host](https://github.com/MorpheApp/morphe-manager/blob/43d0b641a638cddff84ddfd63115eda18d68f614/patcher-core/src/main/kotlin/app/morphe/manager/patcher/host/HostMain.kt) runs in a process scoped to one patch run and checks preloaded file stamps. These mechanisms were inspected as hypotheses; they do not prove what happened on the reporter device.

The temporary unknown-app-install permission was restored to its initial deny state. Both test wrappers restored and hashed the exact prior ordinary Google clone APK set and checked the AVD remained account-free. Only the dedicated test clone's disposable data was reset around the tests. The local Manager source intentionally remains0.3.7 in that test AVD; no Manager signing key was changed. No physical Galaxy, authenticated emulator or real user account was operated. APKs, raw UI/capture files and private signing material remain excluded from Git.

Private Mac evidence under `/Users/ryuya/Documents/GeminiPatch-v037/local/verification/`: `manager-audit/`, `manager-audit-runtime/`, `manager-audit-update/`. The latter includes exact installed-hash/UID confirmation, legacy/fresh report checks and permission restoration. Do not publish the APKs or raw captures.
