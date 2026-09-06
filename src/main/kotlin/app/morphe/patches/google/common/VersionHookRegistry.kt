package app.morphe.patches.google.common

/**
 * パッチが適用する各フックの識別子
 */
enum class HookId(val displayName: String) {
    WORK_PROFILE_BYPASS("Work Profile 制限バイパス"),
    GMS_CORE_REDIRECT("GMS (MicroG) 接続先リダイレクト"),
    GMS_SIGNATURE_BYPASS("Google Play Services 署名・可用性検証バイパス"),
    GEMINI_TARGET_REDIRECT("Gemini 連携先 Google アプリリダイレクト"),
    SPLIT_RESTRICTION_REMOVAL("Split APK 制約解除"),
    APP_LABEL_UPDATE("アプリ表示名・エイリアス更新"),
    PROCESS_NAME_REDIRECT("マルチプロセス名リダイレクト"),
    ACCOUNT_SWITCH_LOCK_BYPASS("アカウント切り替えロックバイパス"),
    ACCOUNT_SYNC_LOOP_FIX("アカウント同期バインドループ防止"),
    ACCOUNT_CONVERT_FALLBACK("アカウントID変換フォールバック")
}

/**
 * 特定のフックを適用するためのターゲット仕様
 * バージョン固有のクラス名や、耐性向上のための文字列アンカーを定義
 */
data class TargetSpec(
    val className: String? = null,
    val methodName: String? = null,
    val methodDescriptor: String? = null,
    val anchorStrings: List<String> = emptyList(),
    val description: String = ""
)

/**
 * 特定のアプリ・バージョンにおけるフック定義のプロファイル
 */
data class AppVersionProfile(
    val appName: String,
    val packageName: String,
    val versionPattern: Regex,
    val hooks: Map<HookId, TargetSpec>
)

/**
 * アプリのバージョンごとに Hook ID とターゲット定義を管理するレジストリ
 */
object VersionHookRegistry {
    private val profiles = mutableListOf<AppVersionProfile>()

    init {
        // Google アプリ (AGSA) 17.54.x 向けのプロファイル登録
        register(
            AppVersionProfile(
                appName = "Google App (AGSA)",
                packageName = "com.google.android.googlequicksearchbox",
                versionPattern = Regex("^17\\.54\\..*"),
                hooks = mapOf(
                    HookId.WORK_PROFILE_BYPASS to TargetSpec(
                        className = "Lauby;",
                        methodName = "a",
                        methodDescriptor = "(Lgtij;)Ljava/lang/Object;",
                        anchorStrings = listOf(
                            "Trampolining to web app for work profile. %s",
                            "Skipping trampoline to web app for work profile.",
                            "Trampolining to web app for work profile."
                        ),
                        description = "Google アプリ 17.54.x 用 Work Profile トランポリン回避定義"
                    ),
                    HookId.GMS_CORE_REDIRECT to TargetSpec(
                        anchorStrings = listOf("com.google.android.gms"),
                        description = "GMS パッケージ名の MicroG 置換"
                    ),
                    HookId.APP_LABEL_UPDATE to TargetSpec(
                        description = "表示名 Google (Morphe) への更新"
                    )
                )
            )
        )

        // Google アプリ 汎用・最新バージョン向けフォールバックプロファイル
        register(
            AppVersionProfile(
                appName = "Google App (AGSA) Generic/Future",
                packageName = "com.google.android.googlequicksearchbox",
                versionPattern = Regex(".*"),
                hooks = mapOf(
                    HookId.WORK_PROFILE_BYPASS to TargetSpec(
                        anchorStrings = listOf(
                            "Trampolining to web app for work profile. %s",
                            "Skipping trampoline to web app for work profile.",
                            "Trampolining to web app for work profile."
                        ),
                        description = "汎用文字列アンカーによる動的制御フロー探索"
                    ),
                    HookId.GMS_CORE_REDIRECT to TargetSpec(
                        anchorStrings = listOf("com.google.android.gms"),
                        description = "全DEX走査によるGMS文字列自動置換"
                    )
                )
            )
        )

        // Gemini (com.google.android.apps.bard) 1.0.x 向けのプロファイル登録
        register(
            AppVersionProfile(
                appName = "Gemini",
                packageName = "com.google.android.apps.bard",
                versionPattern = Regex("^1\\.0\\..*"),
                hooks = mapOf(
                    HookId.GEMINI_TARGET_REDIRECT to TargetSpec(
                        anchorStrings = listOf("com.google.android.googlequicksearchbox"),
                        description = "Gemini 1.0.x 用 Google アプリ連携先置換"
                    ),
                    HookId.SPLIT_RESTRICTION_REMOVAL to TargetSpec(
                        description = "Split APK 制約除去"
                    ),
                    HookId.APP_LABEL_UPDATE to TargetSpec(
                        description = "表示名 Gemini (Morphe) への更新"
                    )
                )
            )
        )

        // Gemini 汎用・最新バージョン向けフォールバックプロファイル
        register(
            AppVersionProfile(
                appName = "Gemini Generic/Future",
                packageName = "com.google.android.apps.bard",
                versionPattern = Regex(".*"),
                hooks = mapOf(
                    HookId.GEMINI_TARGET_REDIRECT to TargetSpec(
                        anchorStrings = listOf("com.google.android.googlequicksearchbox"),
                        description = "全DEX走査によるパッケージ名自動置換"
                    )
                )
            )
        )
    }

    /**
     * プロファイルを登録する
     */
    fun register(profile: AppVersionProfile) {
        profiles.add(profile)
    }

    /**
     * パッケージ名とバージョン名から最適なプロファイルを検索
     */
    fun findProfile(packageName: String, versionName: String): AppVersionProfile? {
        // 特定バージョンパターンに厳密にマッチするものを優先
        val matched = profiles.filter { it.packageName == packageName && it.versionPattern.matches(versionName) }
        return if (matched.size > 1) {
            // パターンが ".*" ではない具体的な正規表現を優先
            matched.firstOrNull { it.versionPattern.pattern != ".*" } ?: matched.first()
        } else {
            matched.firstOrNull()
        }
    }

    /**
     * Hook ID に基づくターゲット仕様を取得
     */
    fun getTargetSpec(hookId: HookId, packageName: String, versionName: String): TargetSpec? {
        val profile = findProfile(packageName, versionName)
        return profile?.hooks?.get(hookId)
    }

    /**
     * フック適用のログを出力
     */
    fun logHook(hookId: HookId, tier: String, status: String, detail: String = "") {
        val detailPart = if (detail.isNotEmpty()) " - $detail" else ""
        println("[HookRegistry] [${hookId.displayName}] [$tier] $status$detailPart")
    }
}
