# Geminiの触覚フィードバック

Googleアプリに `Gemini haptic feedback setting` を適用すると、Geminiの設定に「触覚フィードバック」が追加される。AndroidX版では設定一覧、Compose版では設定のトップ画面下部に表示する。既定はオン。オフにすると、回答に伴うステータス表示の振動が止まる。両方の画面で同じ端末内設定を使う。Geminiランチャーの再パッチが必要になるのは、ランチャー自体を新版にする場合。

| 入力 | 2026-11-26 JSTの確認範囲 |
| --- | --- |
| Google `17.72.17.ve.arm64`（301831138） | Android 11以降のnodpi単体APK。既定のパッチ選択でAPKを生成し、Android 12と17のエミュレータで設定と振動制御を検証。パッチを外したビルドも比較。 |
| Google `17.76.20.ve.arm64`（301838242） | Galaxyの通常領域にPlayストアが配信したbaseと4 split。既定選択で生成し、5 APKを同じ署名でインストール。Android 17でGeminiからGoogleクローンのログイン画面まで起動し、設定と振動制御を検証。 |
| Gemini `1.0.1009090390`（353） | GalaxyのPlayストア版base（API 32以上）。既定選択で単体APKを生成し、Android 17でGoogleクローンに接続。 |

エミュレータ上で生成APKのAndroidX設定一覧を使い、同じ画面にスイッチが重複しないこと、初期オン、オフ、別プロセスでのオフ維持、オンに戻したときの元の振動呼び出しを確認。Compose用の追加行は、元のViewを保持し、AndroidXと同じ設定を変更することを計測用Viewで確認した。振動の測定には `View.performHapticFeedback` を記録するテスト用Viewを使った。APKのDEXには、設定登録2箇所と回答ステータスの振動3箇所に呼び出しがある。パッチ無効時は追加呼び出しが0箇所。

ログイン済みのGemini設定画面、チャット、Secure Folderはこの候補ビルドで未確認。Google 17.76.20にはIssue #8の会話更新警告用の版別フックがない。最新版のGoogle 17.78.14.ve.arm64（301841514）は公開を確認したが、Macのダウンロードがサイトの確認画面で止まった。APKを取得・検証してから個別に追加する。[Google APK掲載ページ](https://www.apkmirror.com/apk/google-inc/google-search/google-app-17-78-14-release/google-app-17-78-14-ve-arm64-android-apk-download/) ・ [Gemini APK掲載ページ](https://www.apkmirror.com/apk/google-inc/google-gemini/google-gemini-1-0-1009090390-release/)

## Googleの新版を追加するとき

1. 通常配布の署名付きAPKからpackageName、versionCode、SDK条件、SHA-256を確認する。分割版は端末から同じインストールの全splitを取得する。
2. `scripts/discover-hooks.sh APK` の `gemini_haptics_settings_class` で設定の候補クラスを探す。候補の呼出元を追い、設定全体の `PreferenceScreen` を受け取る共通メソッドを特定する。
3. `scripts/inspect-hooks.sh APK class 'Landroidx/preference/Preference;'` と `dump` で、AndroidXのキー・タイトル・既定値・親への追加を照合する。設定マネージャーの保存先が `<package>_preferences` か確認する。Compose版は `onCreateView` の戻り値とトップ画面のタイトルを使う。タイトルのリソース名と、戻り点で両方のレジスタが使えることを確認する。
4. `scripts/inspect-hooks.sh APK search 'GpiView;->performHapticFeedback(I)Z'` で回答ステータスの呼び出しと個数を照合する。設定メソッドと振動の呼び出しを新しい版別プロファイルに記録する。
5. `scripts/build-mpp.sh`、`scripts/check-hook-profiles.sh --write`、`scripts/check-hook-profiles.sh` を実行する。Morpheの標準設定で適用し、エミュレータで設定変更、アプリ再起動、ログイン済みの会話を確認してから公開する。
