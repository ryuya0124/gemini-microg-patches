# Geminiの触覚フィードバック

Googleアプリに `Gemini haptic feedback setting` を適用すると、Geminiの設定から回答時の振動を切り替えられる。Google 17.78.14では「メディアの透かしを表示する」の直前に「回答時の振動」を表示する。AndroidX版とCompose版は同じ端末内設定を使う。17.72.17・17.76.20の表示名は「触覚フィードバック」で、Compose版では設定画面下部に表示する。既定はオン。オフにすると回答に伴うステータス表示の振動が止まる。Geminiランチャーの再パッチが必要になるのは、ランチャー自体を新版にする場合。

| 入力 | 2026-11-26 JSTの確認範囲 |
| --- | --- |
| Google `17.72.17.ve.arm64`（301831138） | Android 11以降のnodpi単体APK。既定のパッチ選択でAPKを生成し、Android 12と17のエミュレータで設定と振動制御を検証。パッチを外したビルドも比較。 |
| Google `17.76.20.ve.arm64`（301838242） | Galaxyの通常領域にPlayストアが配信したbaseと4 split。既定選択で生成し、5 APKを同じ署名でインストール。Android 17でGeminiからGoogleクローンのログイン画面まで起動し、設定と振動制御を検証。 |
| Google `17.78.14.ve.arm64`（301841514） | Google署名のnodpi単体APK（API 30以上）。Morpheの既定選択でAPKを生成。Android 12と17で旧版から同一署名で上書きして、オフ設定の継承、切り替えと再起動後の保持を確認。 |
| Gemini `1.0.1009090390`（353） | GalaxyのPlayストア版base（API 32以上）。既定選択で単体APKを生成し、Android 17でGoogleクローンに接続。 |

エミュレータ上で生成APKのAndroidX設定一覧を使い、同じ画面にスイッチが重複しないこと、初期オン、オフ、別プロセスでのオフ維持、オンに戻したときの元の振動呼び出しを確認。17.72.17・17.76.20のCompose版では元のViewを保持し、AndroidXと同じ設定を変更することを計測用Viewで確認した。振動の測定には `View.performHapticFeedback` を記録するテスト用Viewを使った。APKのDEXには、設定登録2箇所と回答ステータスの振動3箇所に呼び出しがある。パッチ無効時は追加呼び出しが0箇所。

17.78.14の一覧内表示はAndroid 12・17上で確認した。アプリ自身のAndroidX設定グループとComposeが読む設定グループを使い、透かしの直前にスイッチが1つだけ並ぶことを確認した。両経路の切り替え処理を実行し、状態の保存、再起動後の読み出し、回答の振動制御を照合した。

Google 17.78.14の組み合わせは、Android 12でGemini `1.0.995038376` のAPI 29バリアント、Android 17で `1.0.1009090390` のAPI 32バリアントを使用し、両方でログイン画面に到達した。ログイン済みの設定画面、チャット、Secure Folderは未確認。Google 17.76.20と17.78.14にはIssue #8の会話更新警告用の版別フックがない。[Google APK掲載ページ](https://www.apkmirror.com/apk/google-inc/google-search/google-app-17-78-14-release/google-app-17-78-14-ve-arm64-android-apk-download/) ・ [Gemini APK掲載ページ](https://www.apkmirror.com/apk/google-inc/google-gemini/google-gemini-1-0-1009090390-release/)

## Googleの新版を追加するとき

1. 通常配布の署名付きAPKからpackageName、versionCode、SDK条件、SHA-256を確認する。分割版は端末から同じインストールの全splitを取得する。
2. `scripts/discover-hooks.sh APK` の `gemini_haptics_settings_class` で設定の候補クラスを探す。候補の呼出元を追い、設定全体の `PreferenceScreen` を受け取る共通メソッドを特定する。
3. `scripts/inspect-hooks.sh APK class 'Landroidx/preference/Preference;'` と `dump` で、AndroidXのキー・タイトル・既定値・親への追加を照合する。17.78.14では `watermark setting switch listener` を含む2つの行生成メソッドを確認する。Compose版のグループがどのリストを使うかと、既存スイッチの変更リスナーを追う。両方の設定画面で保存先が `<package>_preferences` になることを照合する。
4. `scripts/inspect-hooks.sh APK search 'GpiView;->performHapticFeedback(I)Z'` で回答ステータスの呼び出しと個数を照合する。設定メソッドと振動の呼び出しを新しい版別プロファイルに記録する。
5. `scripts/build-mpp.sh`、`scripts/check-hook-profiles.sh --write`、`scripts/check-hook-profiles.sh` を実行する。Morpheの標準設定で適用し、エミュレータで設定変更、アプリ再起動、ログイン済みの会話を確認してから公開する。
