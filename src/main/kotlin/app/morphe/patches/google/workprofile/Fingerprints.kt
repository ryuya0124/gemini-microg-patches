package app.morphe.patches.google.workprofile

import app.morphe.patcher.Fingerprint

/**
 * Google アプリ（AGSA）内でセキュアフォルダ（Work Profile）検知時に
 * ブラウザ版へリダイレクト（トランポリン）する Deeplink ハンドラーを特定するプライマリ Fingerprint
 */
object WorkProfileTrampolineFingerprint : Fingerprint(
    strings = listOf(
        "Trampolining to web app for work profile. %s"
    )
)

/**
 * トランポリンをスキップする旨のログ文字列を特定するセカンダリ Fingerprint
 * 将来のバージョンでフォーマット指定子が外れた場合などのフォールバック用
 */
object WorkProfileSkipFingerprint : Fingerprint(
    strings = listOf(
        "Skipping trampoline to web app for work profile."
    )
)
