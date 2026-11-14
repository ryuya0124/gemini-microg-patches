package app.morphe.patches.google.common.versions

import app.morphe.patches.google.common.AppVersionProfile
import app.morphe.patches.google.common.HookId
import app.morphe.patches.google.common.TargetSpec

/** Verified against these exact inputs. Retain this file when adding another version. */
val google177418 = AppVersionProfile(
    appName = "Google App (AGSA)",
    packageName = "com.google.android.googlequicksearchbox",
    versionName = "17.74.18.sa.arm64",
    versionCode = "301834567",
    inputSha256 = mapOf(
        "google-base.apk" to "dadbf2fa043f5d7b07a6d0ca57a427139e274e69a4331794d18a4d3cc7f9adbc"
    ),
    validation = listOf(
        "2026-11-13: Mac Safari original APK acquired; Google signing lineage, SHA256 and API30 standalone manifest verified",
        "2026-11-13: DexKit semantic discovery and DEX branch/register checks completed; static APK verification and 16KiB alignment passed",
        "2026-11-13: API31 signed-in Play Store AVD: original/clone upgraded with UIDs retained; existing image/Canvas replies, new image generation and post-reboot replies passed",
        "2026-11-13: Android Manager2.5/Patcher2.3, Expert mode off: 11 defaults generated/exported; actual APK static checks, install, native launch and first/cached/seeded process probe passed on account-free API31 AVD; conversation-update card remains unreproduced"
    ),
    hooks = mapOf(
        HookId.PROVIDER_AUTHORITY_REDIRECT to TargetSpec(
            anchorStrings = listOf("content://com.google.android.apps.gsa.voiceinteraction.dump.StateDumpProvider"),
            integers = mapOf("providerCount" to 28),
            description = "Derive authority identities from the original manifest, including the root package authority. Redirect exact DEX strings and content:// URI authorities without changing MIME types or process identity constants."
        ),
        HookId.DEVICE_COMPLIANCE_FEATURE to TargetSpec(
            className = "Leeek;", methodName = "a", methodDescriptor = "(Lhvvp;Lhvvp;)V",
            anchorStrings = listOf("Checking for device compliance...", "Feature is disabled!"), expectedMatches = 1,
            description = "Select existing feature-disabled path, including native component-reset recovery. Server integrity state unchanged."
        ),
        HookId.DEVICE_COMPLIANCE_REDIRECT to TargetSpec(
            className = "Leeei;", methodName = "invoke", methodDescriptor = "()Ljava/lang/Object;",
            anchorStrings = listOf("Redirecting to UncertifiedDeviceActivity."), expectedMatches = 1,
            description = "Skip only the local warning-launch branch; does not certify the device or change server integrity results."
        ),
        HookId.WORK_PROFILE_BYPASS to TargetSpec(
            className = "Laois;", methodName = "u",
            methodDescriptor = "(Lcom/google/apps/tiktok/account/AccountId;Lhvti;)Ljava/lang/Object;",
            anchorStrings = listOf(
                "Trampolining to web app for work profile. %s",
                "Skipping trampoline to web app for work profile."
            ),
            expectedMatches = 1,
            integers = mapOf("lookback" to 60, "branchIndex" to 146, "branchRegister" to 14, "branchTargetCodeUnit" to 392, "observedRegisters" to 15),
            description = "Only the trampoline anchor remains. IF_EQZ v14 at code unit 264 jumps to 392; nearest IF_EQZ v2 is a nullable Boolean check and MUST NOT be patched. Secondary skip anchor is absent."
        ),
        HookId.WORK_PROFILE_ELIGIBILITY to TargetSpec(
            className = "Lawiv;", methodName = "k",
            methodDescriptor = "(Lhvti;)Ljava/lang/Object;",
            fieldName = "x", fieldType = "Z",
            anchorStrings = listOf(
                "Robin eligibility : is disallowed on Work profile",
                "Robin eligibility : Work profile is allowed."
            ),
            expectedMatches = 1,
            integers = mapOf("errorCode" to 19),
            description = "Replace IGET_BOOLEAN with same-width const/16 true; retain other eligibility checks."
        ),
        HookId.MICROG_RUNTIME_PERMISSIONS to TargetSpec(
            className = "Lcom/google/android/apps/search/assistant/surfaces/voice/robin/main/MainActivity;",
            methodName = "onCreate", methodDescriptor = "(Landroid/os/Bundle;)V",
            fieldName = "d", fieldType = "Z", expectedMatches = 1,
            integers = mapOf(
                "minRegisters" to 23, "maxRegisters" to 23, "observedRegisters" to 23,
                "activityAlias" to 0,
                "thisParameterOffset" to 2, "scratchArray" to 6, "scratchIndex" to 7, "scratchString" to 8
            ),
            description = "Insert after final IPUT_BOOLEAN d:Z (code unit 793); v0 aliases Activity. v3/v19 remain live for cleanup, scratch v6/v7/v8 are dead."
        ),
        HookId.PROCESS_NAME_REDIRECT to TargetSpec(
            className = "Leuhx;", methodName = "b", methodDescriptor = "()Ljava/lang/String;",
            anchorStrings = listOf("com.google.android.googlequicksearchbox:googleapp"),
            expectedMatches = 1,
            description = "One shared RETURN_OBJECT at code unit 10, reached by both cached and fresh paths; normalize at that label without changing the one-register allocation."
        ),
        HookId.MAIN_PROCESS_CHECK to TargetSpec(
            className = "Leuht;", methodName = "b", methodDescriptor = "()Z",
            anchorStrings = listOf("More than 1 custom main process specified"),
            expectedMatches = 1,
            description = "Anchor is in euht.a; redirect only euht.b provider read to Application.getProcessName."
        ),
        HookId.GMS_SIGNATURE_BYPASS to TargetSpec(
            className = "Ldpwy;", methodName = "b", methodDescriptor = "(Landroid/content/Context;I)I",
            anchorStrings = listOf(
                " requires Google Play services, but their signature is invalid.",
                "The Google Play services resources were not found. Check your project configuration to ensure that the resources are included.",
                " requires Google Play services, but they are missing."
            ),
            expectedMatches = 1
        ),
        HookId.GMS_CERTIFICATE_VERIFIER to TargetSpec(
            className = "Ldpwz;",
            anchorStrings = listOf("Unable to obtain package certificate history."),
            methodSignatures = listOf("b(Ljava/lang/String;)Z", "c(I)Z", "d(Landroid/content/pm/PackageInfo;Z)Z"),
            expectedMatches = 3
        ),
        HookId.GMS_CORE_REDIRECT to TargetSpec(
            anchorStrings = listOf("com.google.android.gms"),
            values = mapOf("originalGoogleSha1" to "38918a453d07199354f8b19af05ec6562ced5788"),
            description = "microG 7.3.0 app.revanced.android.gms; retain Binder service actions, redirect UI actions. Shared mappings: GmsConstants.kt."
        ),
        HookId.PACKAGE_CLONE_REDIRECT to TargetSpec(
            anchorStrings = listOf("com.google.android.googlequicksearchbox"),
            description = "Redirect exact owned-package identity references; counts are recorded from the actual candidate build."
        ),
        HookId.SPLIT_RESTRICTION_REMOVAL to TargetSpec(description = "The verified API30 nodpi original is standalone. Do not mix it with resource splits from an older version or another variant."),
        HookId.APP_LABEL_UPDATE to TargetSpec(description = "Google (Morphe)"),
        HookId.ACCOUNT_SWITCH_LOCK_BYPASS to TargetSpec(
            enabled = false, anchorStrings = listOf("Acquired account-switch Mutex."),
            description = "Historical rejected approach. Do not enable; normal account synchronization is required."
        ),
        HookId.ACCOUNT_SYNC_LOOP_FIX to TargetSpec(
            enabled = false, anchorStrings = listOf("Binding to service"),
            description = "Historical rejected Binder-success spoof; caused hangs."
        ),
        HookId.ACCOUNT_CONVERT_FALLBACK to TargetSpec(
            enabled = false,
            anchorStrings = listOf("Found google email address as the old primary email address", "Failed to convert AGSA account name to account id."),
            description = "Rejected fixed AccountId experiment; no target registered for this version."
        )
    )
)
