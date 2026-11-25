package app.morphe.patches.google.common.versions

import app.morphe.patches.google.common.HookId

private val previous = google177217.hooks

val google177620 = google177217.copy(
    versionName = "17.76.20.ve.arm64",
    versionCode = "301838242",
    inputSha256 = mapOf(
        "google-base.apk" to "4e36a04c15fac7fbcbac19bb01783a37391c324498e937cc78b876c0d12b54ae",
        "google-xxhdpi.apk" to "579c60128a50b59deaa47e32cfcdf48f1cdb599503cc0aa40eff242968ae5a28"
    ),
    validation = listOf(
        "2026-11-26: exact Play Store split variant extracted from a Google-signed installation; default MPP applied",
        "2026-11-26: API 37 emulator installed the base plus four splits; Gemini launched to sign-in",
        "2026-11-26: AndroidX switch deduplication, haptic delegation, suppression and process restart exercised in the APK",
        "Authenticated chat, Secure Folder and the conversation warning hook remain unverified for this version"
    ),
    hooks = previous - HookId.CONVERSATION_UPDATE_WARNING + mapOf(
        HookId.DEVICE_COMPLIANCE_FEATURE to previous.getValue(HookId.DEVICE_COMPLIANCE_FEATURE).copy(
            className = "Ldvde;", methodName = "c", methodDescriptor = "()Z",
            anchorStrings = listOf("Feature is disabled!"),
            integers = mapOf("branchDistance" to 2),
            description = "Feature-disabled logging and the negated flag moved into c(); verify the XOR and branch before editing."
        ),
        HookId.DEVICE_COMPLIANCE_REDIRECT to previous.getValue(HookId.DEVICE_COMPLIANCE_REDIRECT).copy(
            className = "Ldvdc;"
        ),
        HookId.WORK_PROFILE_BYPASS to previous.getValue(HookId.WORK_PROFILE_BYPASS).copy(
            className = "Lahoz;", methodName = "u",
            methodDescriptor = "(Lcom/google/apps/tiktok/account/AccountId;Lhist;)Ljava/lang/Object;"
        ),
        HookId.WORK_PROFILE_ELIGIBILITY to previous.getValue(HookId.WORK_PROFILE_ELIGIBILITY).copy(
            className = "Laozi;", methodName = "l", methodDescriptor = "(Lhist;)Ljava/lang/Object;"
        ),
        HookId.MICROG_RUNTIME_PERMISSIONS to previous.getValue(HookId.MICROG_RUNTIME_PERMISSIONS).copy(
            description = "Insert after final IPUT_BOOLEAN o:Z (code unit 818); v0 aliases Activity."
        ),
        HookId.PROCESS_NAME_REDIRECT to previous.getValue(HookId.PROCESS_NAME_REDIRECT).copy(
            className = "Lelhw;", methodName = "b", methodDescriptor = "()Ljava/lang/String;"
        ),
        HookId.MAIN_PROCESS_CHECK to previous.getValue(HookId.MAIN_PROCESS_CHECK).copy(
            className = "Lelhs;", methodName = "b",
            description = "Anchor is in elhs.a; the provider call inside elhs.b is the main-process check."
        ),
        HookId.GMS_SIGNATURE_BYPASS to previous.getValue(HookId.GMS_SIGNATURE_BYPASS).copy(className = "Ldhsf;"),
        HookId.GMS_CERTIFICATE_VERIFIER to previous.getValue(HookId.GMS_CERTIFICATE_VERIFIER).copy(className = "Ldhsg;"),
        HookId.PACKAGE_CLONE_REDIRECT to previous.getValue(HookId.PACKAGE_CLONE_REDIRECT).copy(
            description = "Rewrite exact package references in the Google application."
        ),
        HookId.GEMINI_HAPTICS to previous.getValue(HookId.GEMINI_HAPTICS).copy(
            className = "Lcafg;",
            methodDescriptor = "(Lavyc;Landroidx/preference/PreferenceScreen;Lhist;)Ljava/lang/Object;",
            methodSignatures = listOf(
                "Lbtmy;->a(Ljava/lang/Object;)Ljava/lang/Object;",
                "Lbtow;->a(Ljava/lang/Object;)Ljava/lang/Object;"
            ),
            values = mapOf("findPreference" to "ae", "setKey" to "I", "setTitle" to "K",
                "defaultField" to "x", "addPreference" to "ab")
        ),
        HookId.GEMINI_HAPTICS_COMPOSE to previous.getValue(HookId.GEMINI_HAPTICS_COMPOSE).copy(
            className = "Lcabq;",
            integers = mapOf("registers" to 11, "viewRegister" to 7, "titleRegister" to 8,
                "geminiSettingsTitle" to 0x7f151c4c, "settingsTitle" to 0x7f151c59)
        )
    )
)
