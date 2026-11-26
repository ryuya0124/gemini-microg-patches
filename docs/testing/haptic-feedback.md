# Geminiの触覚フィードバック

## v0.3.11-haptics.3候補

パッチ集の対応表を確認し、Google 17.54.18・17.70.11にも「Geminiの振動」を追加した。17.54.18はAndroidXの設定構築、17.70.11はAndroidXとComposeの設定構築を照合した。AndroidXのスイッチは、全版で波形再生と同じ保存値から表示を初期化する。

フローティングボタンの表示切り替え、ガイド付きプロンプト、Gemini内のデバッグ通知にも振動呼び出しがあった。17.76.20と17.78.14は、ホーム画面の候補プロンプト・シート操作と `YourDay` の追加2経路も確認した。プロンプトの処理は、リソース名と呼出元から機能を特定し、各版の命令列・引数・呼び出し個数を照合した。

| Google | 波形再生 | Viewフィードバック | エミュレータ |
| --- | ---: | ---: | --- |
| 17.54.18 | 4 | 23 | Android 12・17 |
| 17.70.11 | 4 | 28 | Android 17 |
| 17.72.17 | 4 | 28 | Android 12・17 |
| 17.76.20 | 4 | 37 | Android 17 |
| 17.78.14 | 4 | 39 | Android 12・17 |

同じMPPの標準選択でGoogle 5版とGeminiランチャー3版を生成し、出力DEXで上表の呼び出し数を照合した。Android 17では5組ともGeminiランチャーからサインイン画面に到達した。Android 12では17.72.17と17.78.14をランチャー1.0.995038376（API 29のバリアント）と組み合わせ、17.54.18はGoogle側の振動設定と抑止を単独で試験した。今回のランチャー1.0.958859967と1.0.1009090390はAPI 32以上を要求する。ランチャー3版と17.76.20の機能splitで対象の振動APIの直接呼び出しは見つからなかった。

エミュレータで振動のオフ、プロセス再起動後の保持、再オンを確認した。Android 17の17.78.14では、送信・回答開始・回答完了のイベントクラスを使ってアプリ本来の波形処理を実行した。Androidの振動サービスへの要求は、旧版オフで計3件、候補版オフで0件、候補版オンで計3件だった。

調査対象は `performHapticFeedback`、`Vibrator`、`VibratorManager`、`VibrationEffect`、音声由来の振動API。確認できたGemini専用の呼び出しを対象にした。Googleの共有機能、キーボード、OS各自の振動はそれぞれの設定に従う。ログイン後の実チャット、Live、画像編集など個々の操作の実通信を伴う試験は、公開前に残っている。

入力のAPKと監査証跡は `~/Documents/GeminiPatch-haptics/local/haptics-route-audit/` と旧版の検証フォルダ。候補のビルド、出力DEXの照合、Android 12・17の実行ログは `~/Documents/GeminiPatch-haptics-followup/local/full-audit/`。同フォルダの `build-audit-apks.sh`、`run-haptics-audit.sh`、`verify-haptics-playback.sh` で再確認できる。

## v0.3.11-haptics.2候補

Issue #8では、スイッチをオフにしても送信・考え中・回答完了に振動する場面が報告された。v0.3.11-haptics.1の3箇所はフローティング表示の `GpiView` 経路だった。各Google APKで通常チャットの入力コントローラと回答状態から波形再生までを追跡した。17.78.14では `submit_elapsed_realtime` のあるメソッドと `QueryAnswerStarted`、`QueryAnswered` が同じプレイヤーを呼ぶ。

| Google | 波形プレイヤーの呼出元 | 置き換えた波形再生 | 置き換えたView呼び出し |
| --- | --- | ---: | ---: |
| 17.72.17.ve.arm64 | `Laupm;->a(Laupj;)V` | 4 | 13 |
| 17.76.20.ve.arm64 | `Lataj;->a(Latag;)V` | 4 | 14 |
| 17.78.14.ve.arm64 | `Latup;->a(Latum;)V` | 4 | 14 |

波形側の4箇所はチャット用の共通プレイヤー、Liveの音声セッション2箇所、Gemini内の問題報告。共通プレイヤーには長押しで話す操作や入力ツールからの呼び出しもある。View側はフローティング表示の状態・全画面化・ドラッグ、画像の長押しと編集。17.76.20・17.78.14には `YourDay` の呼び出しも含む。元のGeminiランチャー345・353のDEXでは、今回対象とした振動APIの直接呼び出しは0件だった。

設定名は「Geminiの振動」。保存時はプロセス共通のアプリ内ファイルをアトミックに置き換え、振動時は最新値を読む。旧版の保存値も初回から引き継ぐ。Androidの `SharedPreferences` は複数プロセスでの使用をサポートしていない。 [Androidの仕様](https://developer.android.com/reference/android/content/SharedPreferences)

Morpheの標準選択で3版を生成し、出力DEXで上表の呼び出し数を照合した。Android 12と17で旧設定の移行、オフ時の画面・波形再生の抑止、プロセス再起動後の保持、再オンを確認した。17.76.20の分割版はAndroid 17で確認した。AndroidXと旧Composeの両経路を試し、17.78.14では設定一覧のリスナーを実行した。

Android 17・Google 17.78.14の比較では、送信・回答開始・回答完了のイベントクラスを使い、アプリ本来の波形再生処理にテスト波形を渡した。`dumpsys vibrator_manager` の要求数は旧版オフで3、候補版オフで0、候補版オンで3だった。再実行用の `verify-haptics-playback.sh` と結果は `local/followup/` に保存した。

ログイン済みの実チャットで3場面を送信し、振動サービスの要求を比較する試験が公開前に残っている。今回のエミュレータはサインイン画面まで。Google検索、Lens、IME、OS由来の振動はGeminiのスイッチの制御範囲外。

### 次版での追跡

`scripts/discover-hooks.sh APK` の `gemini_haptics_waveform_player` と `gemini_haptics_answer_events` でプレイヤーと回答イベントを見つける。入力側の `submit_elapsed_realtime` からプレイヤーへの呼び出しも照合する。`performHapticFeedback`、`Vibrator`、`VibrationEffect` を列挙し、Geminiの画面やサービスに属することを文字列・リソース・呼出元から確認して版別プロファイルを更新する。Macの監査証跡は `~/Documents/GeminiPatch-haptics/local/haptics-route-audit/`、候補のビルドと検証結果は `~/Documents/GeminiPatch-haptics-followup/local/followup/`。

## v0.3.11-haptics.1の記録

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
