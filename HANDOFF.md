# 引き継ぎメモ

## 2026-11-14 diagnostics2公開・原因調査継続

ユーザーの依頼によりログ不具合を修正し、[v0.3.5-diagnostics.2](https://github.com/ryuya0124/gemini-microg-patches/releases/tag/v0.3.5-diagnostics.2) を診断専用プレリリースとして公開。[Issue返信6871766839](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6871766839) で同じAPK・署名・データを保った更新と再記録を短く依頼。通常修正版ではない。stableは引き続きv0.3.5、root feed変更なし、Issueの警告は未再現・原因未特定。

ソース別branch `diagnostics/issue8-logs` の69040ef8a5dcd5016f3a773aff3be7ad8f640335。Windows work/diagnostics-issue8 /Mac /Users/ryuya/Documents/GeminiPatch-diagnostics は同期・clean。最終CI43556906747成功、通常release job43557118065はprereleaseタグでskip。MPP232359bytes/SHA256 0dc1604ce5c4c6401bb1e8ebe6ec34d7b4867073de31b399df0b680c0fbdb61d、Mac実測版・CI・公開asset一致。公開はMPP/別diagnostics feed/checksumのみ。APK/キー/生ログは公開していない。

Android12で旧版に履歴5000件を集中させ、turn/stream/capabilities/requestVersion欠落を再現。修正版は種類別の有界キューで履歴の繰り返しsnapshotをまとめ、別種の重要イベントを保持。merge回数と予期しない欠落を区別する。background file lock待機、process別のpending/loss/停止ACK、未知/終了processをincompleteとして残す方式へ変更。20000件の並列producerテスト、native疑似1086、native tm.oの実setText経由の呼び出し元記録・表示内容維持・canary非流出、全1685setterのregister確認が成功。元Googleがinventoryから消えたのはMorpheがStringBuilderをliteral化してclone名へ書き換えたためとactual Manager APKで判明し、診断namespaceだけ除外して4キー保持を確認。診断画面の独立task化も確認。

Google17.72.17で同じ自作Web起点会話を元Geminiあり/実際にuser0から削除の両方で継続成功。capability9(BARD_APP_USER)は元Geminiの有無に応じて変化したが、警告は再現しなかった。この差だけを直すパッチやread-only省略は公開しない。2つの初期実会話captureは4process全ACK・予期しないloss0。最終captureはOSがidleな2processをempty #17で終了した記録に対応してincomplete=trueを返し、UIの0を全体の0と誤認しないことも確認。最終版自体も返答成功。actual Morphe Manager2.5/Patcher2.3で標準12patchから生成・署名・16KiB整列・起動・文書選択経由JSON保存が成功、3process全ACK・loss0・4inventoryキー保持。Manager APK314703433bytes/SHA256 b9f9062be95979672b06ec31ad3a0f4b4ed0cef9a75699120bf2b09d868d82c9。

元Geminiの復元はinstall-existingだけではAPK実体を戻せない場合があるため、保持した正確な5つのoriginal APKを再インストールしてpath/hash/UID10153/code345を確認。全stock/clone APKセット・UID・同一stock/microGアカウントは最終復元済み。account-free Managerテストcloneだけdowngrade用に削除し元APKセットへ戻した（そのテストデータのみリセット）。Galaxy未操作。自分で起動したaccount-free AVD5582は終了し、ログイン済み5584は維持。詳しい記録は別branch [diagnostics/logging-v2-2026-11-14.md](https://github.com/ryuya0124/gemini-microg-patches/blob/diagnostics/issue8-logs/diagnostics/logging-v2-2026-11-14.md)。次の報告者JSONでknown_warning_setterの固定class/methodとview/resource/ancestorsを照合し、実際のrenderer経路を特定する。追加の環境質問はしていない。

## 2026-11-14 Issue #8 添付ログの解析

報告者から診断ログを受領。[解析と観測上の制約](https://github.com/ryuya0124/gemini-microg-patches/blob/diagnostics/issue8-logs/diagnostics/reporter-log-2026-11-14.md) を別branchへ記録。警告文の画面表示は自動検出したが、native processでqueueDropped1356があり、status/stream無しを除外根拠にできない。StopのqueueDropped0/observedCounters空はUI processだけの値。原Googleキーもinventoryから欠落し、未インストールとは断定不可。Google17.72.17/API31、Gemini1.0.995038376は確定。受領のお礼と調査中であることを短く返信（コメント6870953180）、追加質問無し。原因特定・自然な再現・修正は未達で、新しいアプリ変更/リリース/実機変更はしていない。次はメタデータ大量ロードでの計測取りこぼし、process別summary、警告を描くnative view/rendererの対応を調べる。raw reportはdiagnostic worktreeのignored local/reporter-20261114に保持。

更新: 2026-11-13（最新の追記は末尾）

## 2026-11-13 診断版の作業中メモ

追記（公開完了）: 診断専用プレリリース [v0.3.5-diagnostics.1](https://github.com/ryuya0124/gemini-microg-patches/releases/tag/v0.3.5-diagnostics.1) を公開し、[Issueコメント6852606011](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6852606011) で導入・JSON添付を短く依頼済み。修正版ではない。タグsourceはe47b11be2087b6be890c274dcb648ff92fc397ea、最終CI43423478830成功。MPP225524bytes/SHA256 fa31403d1e28a8d5a4caf53d2d89290511f52c8fb578dac98b56d2849b6bffd7、Mac検証版・CI生成物・公開asset一致。最新stableは引き続きv0.3.5。プリリリースタグの通常release job43423697728は予定どおりskip。公開MPP・別diagnostics feed・checksumのみでAPK/署名キーは配布していない。ローカル/リモート別branchを揃え、Macの診断worktreeはclean。

Android版Morphe2.5/Patcher2.3の標準12選択でも単体Google17.72.17から生成・署名・起動・文書選択経由JSON保存を確認。外部JVM hostにdesktop用JSON実装が無かったため、ビルド時に元JSONからPropertiesを生成し標準Javaだけで読む方式へ変更した。メモリ2GBのテストAVDはpatcher強制終了したため4GBで再確認。account-freeテストcloneだけ旧版へ入れ直して元APKセット復元（そのテストデータはリセット）、ログイン済みAVDは別途同一署名の更新だけで検証し全stock/clone APK・UID・アカウント復元確認。実機未操作。手動warningマーカーのテスト・native疑似1086を自然な不具合再現と混同しない。報告者のログ待ち。追加の事実質問はしていない。

ユーザーの「ログ追加して入れてもらう」提案を受け、公開v0.3.5を基準に別worktree/branch `diagnostics/issue8-logs` で診断専用MPPを準備中。Windows `work/diagnostics-issue8`、Mac `/Users/ryuya/Documents/GeminiPatch-diagnostics`。通常の最新候補の未コミット変更とは混ぜない。対応はGoogle17.72.17.ve.arm64のみ。明示Start後だけ収集し、会話本文・アカウント・URL等を保存せず、Androidの文書選択画面からJSONを書き出す。実際のIssue警告は未再現で、修正リリースのゲートは維持する。

旧private observerのRETURN前挿入は既存分岐がログを飛ばすと判明。旧ログにstatus/classifiedが無いことからサーバー応答を除外してはいけない。診断版では元RETURN位置をinvokeに置き換えRETURNを後置。全1/6帰還経路とnative疑似1086の実記録、canary非流出、停止直後の保存を確認。Android12のログイン済みAVDでは自作Web会話への返答と実際のJSON保存成功。終了後は4つのstock/clone APKセット・UID・同一アカウントを完全復元。Galaxyは未操作。詳細・残作業は別branch `diagnostics/README.md`。

## リリース本文の方針

利用者から見える変更点・修正点だけを短く書く。内部実装の説明やテスト結果は含めず、必要な開発記録はdocs/testingやこの引き継ぎメモに残す。2026-11-12の利用者の指示で、他のリポジトリにも共通する方針。v0.3.3の公開Release本文と元Markdownをこの形式へ修正済み。

## 現在の状態

- microGログイン修正とセキュアフォルダのエラー(19)回避を実装済み。
- 通常領域: チャット「１＋１」→「2 です。」、「２＋２」→「4」を実機で確認。アプリ終了・再起動後もログインを維持。
- セキュアフォルダ: user 150で `Work profile is allowed`、`Server eligibility response is ok`、`Robin is eligible` を確認し、その後ユーザーが利用成功を確認。
- GitHub管理向けにソース・スクリプト・ドキュメントを整理。旧APK・ログ・個人メモは `local/` に保管。
- ユーザーの明示指示でリポジトリを公開へ変更済み: https://github.com/ryuya0124/gemini-microg-patches
- 署名キーを維持。パスワードはユーザーの指定により `local/SIGNING.md` に明記し、`.env` に設定済み。これらはGit対象外。

## 次に作業するとき

1. [README.md](README.md) と [docs/architecture.md](docs/architecture.md) を読む。
2. `git status --short` で他の作業の差分を確認する。
3. `scripts/check-repo.sh`、`scripts/setup-tools.sh`、`scripts/compile-patches.sh` を実行する。
4. APKに影響する変更では `scripts/build-all.sh`、`scripts/sign-apks.sh` を実行する。
5. 端末を明示して `ADB_SERIAL=... scripts/install-device.sh`。Secure Folder内はユーザーが手動起動する。
6. 実機結果と未確認事項をこのメモに追記する。生ログに含まれるトークンやアカウント情報はコミットしない。

## 変更してはいけない前提

- `Account Switch Lock Bypass`、`Account Sync Binder Loop Fix`、`Account ID Fallback for Clone` は無効のまま。固定AccountIdやBinder成功の偽装はログイン不能・同期ループの原因になった。
- Dagger向けにはプロセス名から `.morphe` を除去するが、アカウントストアのmain-process判定は実際のプロセス名を使う。
- microGのUI actionは転送し、互換Binder service actionは元名を維持する。
- 権限要求の挿入位置はonCreate末尾ではない。末尾ではp0がActivity以外の値に上書きされる。
- エラー19は `appk.k` の `appk.x:Z` を読む1命令だけ変更する。Android全体のmanaged-profile判定は変更しない。
- 元APKのハッシュを検証する。別バージョンに対応するときは、難読化名と制御フローを再調査する。

## 整理後の検証記録

- JDK 21・固定Kotlin 2.4.10でGoogle本体、Geminiランチャー、xxhdpi splitを元APKから再生成。
- MainActivityの分岐先検証成功。
- エラー19修正が1命令で、レジスタ・命令幅・分岐オフセットを維持することを検証。
- 3つのAPKを既存キーで署名し、apksignerによる検証成功。
- 動作確認済みv8と再生成後のGoogle本体（15 DEX）・Geminiランチャー（4 DEX）のDEX内容が完全一致。
- ShellCheckとactionlintでスクリプト・Actions定義の検証成功。
- GitHub CIは2026-09-06の再実行で成功。実行ID 34036990773。スクリプト検査・パッチと検証ツールのコンパイル・Artifact保存まで確認。以前の課金ロックによる停止は今回解消している。課金設定はこちらでは変更していない。
- Macのoriginを上記リポジトリに設定済み。リポジトリ専用Deploy keyでfetch/pushする。秘密鍵はlocal/keys/github-deploy-ed25519、core.sshCommandはこのリポジトリ内だけに設定。アカウント全体の認証情報はコピーしていない。
- 旧履歴はlocal-history-before-organizationブランチとbundleに保持。通常のgit pushはmainのみ。git push --allは実行しない。

## ローカルに保管した旧資料

- `local/archive/before-organization.bundle`: 整理前のGit履歴。
- `local/archive/before-organization.diff`: 整理前の未コミット差分。
- `local/HANDOFF-private-history.md`: 以前の詳細調査メモと環境情報。
- `local/archive/dist-v8-before-reorganization/`: ユーザー確認済みv8のAPK。
- `local/archive/work/`: 途中の検証コード・ログ・ビルド。
- `local/archive/authfix-result.json`: 他作業由来の結果をそのまま保管。

旧Git履歴には署名パスワードを含むため、GitHubには整理後の新しい履歴を使用する。旧履歴をpushしない。

## 未確認・制約

- Gemini Live、音声、端末アシスタント設定、端末全体の再起動、他のGoogleバージョンは未確認。
- Samsungでは `am start --user 150` が拒否される。インストール更新は可能だが、フォルダ内の起動は手動。
- GeminiやGoogleのサーバー仕様変更によって利用できなくなる可能性は残る。

## MPP配布対応（2026-09-06）

- 中間JARだけの配布を修正し、scripts/build-mpp.shでAndroid DEXとManifest情報を含む正式なMPPを生成。
- R8/D8 9.4.17をSHA-256固定。SDK Build Tools 36.0.0内蔵D8はKotlin 2.4のmetadataを処理できなかったため使用しない。
- VerifyMpp.ktが全JVMクラスのDEX収録とManifestを検証し、Morphe Desktopのlist-patchesでも読み込む。
- patches-bundle.jsonでRemote sourceを登録可能。Clone appは公式ソース側で別途有効にする必要がある。
- v0.1.0を公開済み: https://github.com/ryuya0124/gemini-microg-patches/releases/tag/v0.1.0
- CI実行34037445468とRelease実行34037506485が成功。全15 JVMクラスのAndroid DEX収録を検証。公開リポジトリURLをDesktopに渡してMPPをダウンロードし、13パッチの読み込みを確認。
- Manager上での全パッチ適用操作は未確認。APK生成は既存のローカルスクリプトで検証済み。

## バージョン別フック管理（2026-09-06）

- 実行用定義はcommon/versionsのGoogle/Gemini版別Kotlin、閲覧用はdocs/versionsの生成JSON。
- versionNameとversionCodeを完全一致で選択。未登録版の汎用フォールバックは廃止。
- エラー19・権限要求・プロセス判定・GMS可用性/署名検証・転送のアンカーと対象を定義から参照。検証コードも共有。
- 旧Lauby.aの誤記を実DEXのaiwk.fへ修正。Web転送回避の既存2命令挿入は保持。
- 新バージョンは旧ファイルを残して追加し、check-hook-profiles.sh --writeでJSONを生成。詳細はdocs/versions/README.md。
- 定義・入力ハッシュ・JSON整合性と未登録版拒否の検証をCI/Releaseへ追加。
- 新構成でGoogle/Gemini/splitを再ビルド成功。Googleの15 DEX、Geminiの4 DEXと各AndroidManifestが整理前とバイト単位で一致。
- 未登録versionName・異なるversionCodeの拒否、入力ハッシュとJSON整合性、MPP生成・17クラスのDEX収録・Desktop読み込みも検証成功。
- バージョン別フック管理をv0.1.1として公開済み。公開v0.1.0の既存ファイルは変更しない。

## v0.1.1公開（2026-09-06）

- Release: https://github.com/ryuya0124/gemini-microg-patches/releases/tag/v0.1.1
- Release Actions実行34038918051でMPP生成・構造検証・版別定義の検証・公開に成功。
- 配布物はMPP、patches-bundle.json、SHA256SUMS。Remote sourceの更新情報も0.1.1へ更新。

## 新版のフック解析（2026-09-06）

[解析ガイド](docs/hook-analysis.md) と [調査テンプレート](docs/templates/hook-investigation.md) を追加。`scripts/inspect-hooks.sh APK search SUBSTRING` で全DEXの参照を検索し、`dump` で完全記述子に一致するメソッドを表示できる。未知版の解析を許可するが、対応版登録やパッチ適用はしない。現行Google APKでエラー19の文字列からappk.kを再発見できることを確認。配布パッチの挙動は変更していない。

## 2026-11-03更新（最新）

- 読み取り可能なMac作業先: `/Users/Shared/GeminiPatch-20261103/repo`。以前のDocumentsはSSHの権限で読めず、GitHubからcloneして再開した。
- Google 17.70.11.ve.arm64 / code 301827586、Gemini 1.0.995038376 / code 345を端末から抽出して対応。追加のLens/tclib動的feature splitは抽出・保管のみで、配布セットは従来と同じbase+xxhdpi。未検証機能は未対応と判断する。
- microGは7.3.0。user 150でGoogleのUncertifiedDeviceActivity起動を確認し、ローカル転送分岐を1命令変更。端末認定の偽装ではない。
- 古い署名キーが不明なため、ユーザーの指示でGoogle/Geminiクローンだけをアンインストールし、新キーでuser 150へ再インストール。microGと元アプリは維持。
- 新キーの保管先: Windows `B:\Androidアプリ 署名鍵\Gemini microG patches`。`gemini-release.p12`、`password`、`署名情報.txt`、`restore-map.json`。秘密鍵の再インポートとバックアップのSHA一致を確認。今後はこのキーを使う。パスワードは公開資料へ書かない。
- Macの`.env`と`local/keys/`にも設定済み。これらはGit対象外。Bドライブには日付サブフォルダを作らずアプリ単位で保管する既存規則に従った。
- DEX分岐、資格判定の1命令差分、警告分岐の1命令差分、MPP構造を検証。ユーザーがセキュアフォルダで警告消失・内部サービス復旧後のチャット送受信を確認。通常領域・強制停止・端末再起動は新版で未再検証。
- 探索はビルド時のみ。将来DexKitを実行時導入する場合の初回/更新後だけのキャッシュ方針はdocs/hook-analysis.mdに記載。

### 送信失敗の追記

初回の「できた」は起動確認だった。チャット成功の記録を訂正。送信時にUNIMPLEMENTED: InteractorProcessEndpointService bindService returned falseを確認し、user 150のdisabledComponentsにも同サービスが入っていた。警告転送だけの修正では不十分。Checking for device compliance...の既存feature-disabled分岐を選び、元のcomponent-reset復旧処理を通す修正を追加。検証完了までv0.2.0タグを公開しない。

### 送信失敗の解決確認

追加修正版を同じ新キーで上書きし、既存feature-disabled経路の復旧ログ、内部サービスのdisabledComponentsからの消失を確認。ユーザーに改めて送信・返答を依頼し「できた」と確認済み。最初の起動確認と混同しない。通常領域・端末再起動・音声/Liveは今回未再検証。

## 2026-11-10（最新）

- 作業先は `/Users/ryuya/Documents/GeminiPatch`。11月4日にSharedからGit履歴・入力・キー込みで移動済み。
- Google 17.72.17.ve.arm64を端末から抽出して新版定義を追加。Geminiは1.0.995038376のまま。
- Issue #7を調査。投稿者本人がclosed/not_plannedにしていた。コメント・再オープンは行っていない。queriesの変更漏れを修正し、Standalone Supportがsplitの話であることも明記。
- DexKit 2.3.0をホスト解析へ導入。9ルールの候補・参照関係をJSON保存。同じAPK/ルール/コード/ツールはキャッシュ利用。端末には探索コードを追加していない。ネイティブライブラリはtools下でビルドする。初回はscripts/setup-dexkit.sh。
- 新版のtrampoline直前にはnullable Booleanの別条件が増えたため、最寄り分岐を書き換える方法を使わず、検証済みAND_INT_2ADDR直後の条件と分岐先を使う。
- プロセス名探索では定数文字列googleappを返す別ヘルパーを除外。版別完全記述子に一致する本物のプロセス名取得関数だけを選択する。
- キーは既存のlocal/keys/gemini-release.p12を維持。バックアップはBドライブの既存アプリ別フォルダを維持。
- 入力ハッシュ/版定義/DEX分岐・資格判定・認定チェック・trampoline/Manifestを検証。実機更新をuser 150へ実施し、利用者の確認待ち。

## 2026-11-12: Issue #8（最新）

- 公式Clone appの名前・権限/Provider設定を明記し、Google/Geminiの許可参照を宣言済み権限に限って同じクローン名前空間へ補正。宣言そのものは公式Clone appがfinalizeで変更するため、こちらで先に変更して二重.morphe化しない。
- APKの固定packageName、permission宣言とprotectionLevel、Manifest/DEX参照、Provider競合をビルド・署名・インストール前に検証。
- default=falseの失敗実験3パッチは、HookIdのenabled=falseを実行時にも強制し、選択時に理由付きで停止。
- パッチ失敗時にMorpheの終了コードだけを信用しない。fresh出力とresult JSONを検証し、前回成功APKを失敗結果で置き換えない。
- 専用API36 Google APIsエミュレータで、元Googleが存在して権限を所有する条件から重複インストール失敗を再現。ユーザー削除後も失敗することを確認し復元。v0.3.0のLens許可参照不足と修正版の解消、元Google無効時の改造Googleへの起動を確認。adb root/suは未使用。詳細はdocs/testing/issue-8-emulator.md。
- 構成違い3ケースを拒否、失敗実験3つを停止、既存出力を維持するテストが成功。Manager内で外部ソースのオプションを強制する機能ではないため、英語手順を守る必要がある。
- エミュレータはアカウント未登録。チャット・実際のLens処理・OnePlus8実機/API31の動作を確認したという意味ではない。現行入力のminSdk=32は維持。
- 新たな署名キーは作成していない。Bドライブの既存キーとMacの.envを維持。Issueへの返信/クローズはしていない。

### リリース前の追加クラッシュ修正

エミュレータ画面の追加確認でGoogle (Morphe) keeps stoppingを検出。元のプロセス名補正がlabel付きreturnの手前への挿入だったため、キャッシュ済みIF_NEZが補正を飛び越えていた。RETURN_OBJECTを補正の先頭命令に置換し、ラベルを保持して解消。

ProcessNameProbeがインストール済みAPKの実コードを呼び、初回/キャッシュ/seeded cacheの差を検証。旧版の未補正キャッシュとwidget receiverのClassCastExceptionを再現し、修正版の正常化とクラッシュ消失を確認。最終26項目+設定9項目が成功。保護されたAPPWIDGET_UPDATEは偽装せず、同じexported receiverのDI入口へ無害な独自actionを明示送信する。

公開前の最終証拠はlocal/issue8/emulator-verified/。スクリーンショットはログイン待ち画面（アカウント未登録）で、エラーのDownload Google画面でもクラッシュダイアログでもない。チャット/Lensの実処理を検証した意味ではない。

## Android 12での追加確認（2026-11-12）

- ユーザーからAndroid12エミュレータでの検証を指定された。API31イメージrevision11の専用AVD `GeminiPatch_Issue8_API31` を作成し、同じv0.3.1実装で26項目すべて成功。公開前のAPI36確認とは別の、公開後の追加検証。
- 既定入力のminSdk32をバージョン全体の制限のように説明した点を訂正。同じGoogle17.72.17.ve.arm64/301831138にAPI30 nodpi単体APK、同じGemini1.0.995038376/345にAPI29バリアントがある。Mac側Codexへユーザーの許可で取得だけを依頼し、通常ブラウザから原本を取得、ハッシュを検証した。
- 原本Google全14DEX・Gemini全3DEXが既定入力と完全一致。フックの追加変更・新しいMPP・署名キーは不要。最低SDKは原本もパッチ後も30/29を維持。minSdkを書き換えたローカル実験はインストールせず不採用。
- 元Googleあり／ユーザーのみ削除後の重複権限エラーを再現。API31で最初の衝突はC2D_MESSAGE。v0.3.0のLens許可不整合とキャッシュ後のWidget ClassCastExceptionを再現し、修正版で解消。元Google無効時の改造Googleへの起動、独自action受信後のクラッシュ無しを確認。元Googleの状態を復元。
- API31のdumpsysは `mResumedActivity:` を使うため、最初のテストは起動済みにもかかわらずAPI36向けの文字列判定で失敗。実際の両フォーマットを扱うようテストを修正し、全26項目を再実行。専用AVD・SDK・通常shellの制約は維持。
- 再現用ビルドは `scripts/emulator/build-android12-fixtures.sh`。既定の `inputs/` / `dist/` を置き換えない。記録・ハッシュ・コマンドは `docs/testing/issue-8-android12.md`、ローカル証拠は `local/issue8/api31/emulator-verified/`。
- エミュレータはGoogleアカウント・microG未登録。画面はSign in to use Gemini。インストール／権限／プロセス／起動確認であり、アカウント認証・チャット・Lens処理・OnePlus8実機の確認ではない。Galaxyの既存アプリは今回更新していない。
- v0.3.1公開済み。Issue #8へは利用者の指示で短い英語の返信を投稿済み。バージョン質問とAPI31非対応の段落は利用者の指示で削除。Issueは開いたまま。

## Morphe標準設定への対応・実機更新作業（2026-11-12）

- ユーザーが実機の更新と「Morpheの標準設定でインストールが通ること」を指定。v0.3.2としてAutomatic Clone Setupをdefault=trueで追加し、Google/Geminiの必須依存にもした。通常ビルドは独自ソースの既定選択のみで、公式Clone app/Expert mode/-O指定が不要。
- 原本Manifestの権限・Providerを先に記録し、Patcher1.12の名前順実行・逆順finalizeを使って最後に固定.morphe名前空間へ設定。既存のpermissionSupportは参照だけを更新する。旧公式Clone appの既定false/false、従来true/trueでも二重変換なし。任意名は明示的に停止。Automatic Clone Setupの既定選択を維持すること。
- 生成後の権限保護レベル、Manifest/DEX参照、Provider競合、各フックの命令幅/分岐を検証済み。既存キーで署名、公開MPPに26 JVMクラスのAndroid DEXを収録し検証済み。
- 配布MPPからの既定生成と旧設定の互換性/出力保持/実験拒否11項目成功。API31とAPI36で各26項目を再実行し、旧不具合の再現と修正版の解消が成功。詳細はdocs/testing/default-clone.md。
- install-device.shはANDROID_USERを両installへ渡す。未指定時は0、Secure Folderは150を明示。新規キー生成・アンインストール・データ消去は行わない。
- 実機のlease取得・同一署名でのuser150更新・データ領域/端末状態/インストール済みAPKハッシュ確認は、ユーザーが依頼を許可したMac側Codexへ担当を分離。端末の元Google/microGやuser0/10へは変更を加えない。実機チャットは利用者の確認待ちとして扱う。

### 実機への適用結果

- SM-S948Q / Android17のSecure Folder user150にGoogle/Gemini修正版を-r更新済み。Google base+xxhdpiは同一PackageInstallerセッション。インストール済み3APKのSHA256・既存署名を照合し一致。
- LensServiceは `com.google.android.googlequicksearchbox.morphe.permission.LENS_SERVICE` を要求する状態へ修正済み。user150のCE/DE領域を維持し、user0/10は未導入のまま。OS/SystemUI識別・SELinux状態も維持。
- Samsungがshellのcross-userアクセスを拒否したため、このSecure Folderへの自動更新では許可済みroot PackageInstallerを使用。通常ユーザーでのエミュレータインストールはshell UID2000で確認済み。
- 一時APKと所有したleaseを解放。親も `local/issue8/auto-clone/device-update/RESULT.md` と `verified-summary.json` を照合。更新後、利用者がログイン状態の維持とチャット返答成功を「返答できた」と確認。自動照合結果と利用者の動作確認を区別して記録。秘密の端末識別子・生ログはGitへ含めない。

## Issue #8再返信・Android ManagerのVerifyError（2026-11-12）

- 追加報告はインストール後の `ekjg.b(): register index out of range (1 >= 1)`。Android Manager2.5.0（Patcher2.3.0）の実GUIで公開v0.3.2 MPPを原本Google API30へ既定適用し、専用API31 AVDで同じVerifyErrorを再現。
- 原因はProcessNameSanitizePatchのprivate registerCountへのreflection。Patcher2.3.0のMutableMethod.ImplementationはサブクラスのためgetDeclaredFieldが失敗し、例外を握りつぶした旧版は1レジスタにv1/v2を注入した。Desktop1.12生成だけでは見つからなかった。
- v0.3.3では3レジスタ明示のprivate static synthetic helperを追加し、元providerのレジスタを変更しない。returnのラベルはhelper呼び出しへ維持。GMS/無効の旧account実験からも同じreflectionを除去し、既存レジスタ数を検証。旧実験は引き続き実行拒否。
- 実Android Managerで同じ原本からv0.3.3を生成・書き出し、静的レジスタ検査とAPI31での初回/キャッシュ/seeded正常化・Widget入口・Google起動・クラッシュ無しを確認。専用runnerは16項目成功（既存test clone削除を含む）、最後にManager署名のtest cloneだけ削除。実機と元Googleは変更しない。
- Desktop生成のAPI31/API36も各26項目、MPP設定11項目を再実行し成功。MPPは26クラスをAndroid DEXへ収録。今回はアカウント未登録のエミュレータでの検証で、実機チャット確認はv0.3.2のまま。
- 詳細・原本ハッシュ参照・実GUI手順・生成APKハッシュ・コマンドは [docs/testing/issue-8-manager-registers.md](docs/testing/issue-8-manager-registers.md)。ローカル証拠は `local/issue8/register-fix/`。今後Patcher更新ではDesktop生成APKの実行だけで済ませず、実Android Managerでの生成を比較する。
- Issueへ短い原因説明を返信済み。自動closeはしない。リリース後の返信・公開結果は次の追記へ記録する。

### v0.3.3公開・返信完了

- コードcommit `691092193ab07f26faabd216fb1ac0fd646846b0` のCI `43202588258`、Release `43202870466` が成功。タグv0.3.3と [Release](https://github.com/ryuya0124/gemini-microg-patches/releases/tag/v0.3.3) を公開。
- 公開MPPをダウンロードして照合。SHA256 `c7b72128b63f6f4e91b0da79dcb56200dd5369f1380bfb57d14987aebe6a929c`、171035 bytesで、実Android Managerに読み込ませて検証したMPPと完全一致。CI生成物も全29 ZIPエントリが一致。
- Issue #8へ [短い返信](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6821468926) を投稿。Android12で再現と解消を確認済みと伝え、ソース更新・原本Google APKの標準選択での再パッチを案内。Issueは閉じていない。投稿者の実機での結果は未確認。

## Issue #8 権限画面の追加返信・v0.3.4（2026-11-12）

- [追加返信](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6822070486) はv0.3.3の起動成功と、Contacts→unknown actionという2つの権限画面。画像と実microG RE7.3.0を確認し、専用API31/API36 AVDで両画面・拒否後の再要求を再現した。
- unknown actionはmicroG所有EXTENDED_ACCESSにdescriptionがなく、Androidが代替文言を使うため。Googleから宣言を上書きしない。v0.3.4は権限前に用途を説明し、後回し/Back/拒否を尊重、不足分だけ要求する。OSの文言そのものを直したとは説明しない。
- JavaランタイムをD8→MPP内extensions/microg-permissions.bin→extendWithで統合。DialogFragmentを使い、選択はプロファイル別SharedPreferencesへ保存。すでに許可済みなら無表示。microG無効/未導入時は要求せず、有効化/導入後に試せる。
- 最終MPPを実Android Manager2.5.0で原本へ既定10パッチ適用・書き出し。生成APKでAPI31の48/48、API36の52/52権限UI項目が成功。同一署名での上書き更新と既存UID/許可維持、部分許可、拒否、再起動、取り消し、microG再導入、日本語を検証。英日画面も目視確認した。
- 既存回帰はDesktop生成で各API26項目、Managerの旧VerifyError比較16項目、MPP設定11項目が成功。静的検査へVerifyPermissionSetupを追加。MPP内26パッチクラスと2ランタイムクラスを検証し、JDK21/24のランタイムDEX一致も確認。
- 実機にはインストールや設定変更をしていない。アカウント未登録AVDの検証であり、チャット/認証成功や投稿者の実機結果とは区別する。詳細・ハッシュ・コマンド・テストハーネスの修正理由は [権限UI検証記録](docs/testing/issue-8-permission-ui.md)。ローカル証拠はlocal/issue8/permission-ui/。
- リリース本文は利用者の指示どおり変更点/修正点の2項目のみ。内部実装とテスト結果は本文に含めない。公開・返信の結果は次の追記へ記録する。

### v0.3.4公開・返信完了

- コードcommit `19370b98adfd825ba7aa0c45809c06642adaf239` のCI `43225699248` とRelease `43226055432` が成功。[v0.3.4](https://github.com/ryuya0124/gemini-microg-patches/releases/tag/v0.3.4) を公開。
- 公開MPPをダウンロードして検証済みMPPと照合。173574 bytes、SHA256 `3958947638c8ecebe6f9752a4395dd4e24c2a55da88b7c1a1a4de956e0ac0393` で完全一致。公開SHA256SUMSのMPP/更新JSONも一致。本文は変更点2項目だけになっていることを確認。
- [Issue返信](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6824765654) で権限の用途・事前説明・拒否後の再要求抑止を短く案内。システム文言自体は変わらないことを明記。Issueを閉じる操作は行っていない。
- 専用AVD2台は名前を確認して停止。実機接続やADBサーバーは停止せず、実機アプリも変更していない。証拠はMacのlocal/issue8/permission-uiとWindowsのoutputsへ保存。

## Issue #8 更新停止の追加返信・v0.3.5候補（2026-11-12〜13）

- 投稿者が汎用更新停止はGeminiに効かないと報告。短く確認したところ、画像はGemini自身の更新ではなくネイティブのUpdate your Google app画面だった。該当コメント6825468743。利用者は実ストアでのデバッグと、原因特定/再現/修正確認後の公開を明示。
- 公式Play Store入り専用API31 AVD GeminiPatch_PlayStore_API31 / emulator-5584を追加。利用者がWindowsのscrcpy画面から手動ログイン。ログイン中は操作・画面取得を止め、完了後にテストした。ストア54.0.09-31で公式Google/GeminiはOpen、クローンIDはItem not foundを確認。物理端末のアプリ/設定は変更しない。
- 原因を実APKで確定: Google/Gemini両方のversionCodeは2147483647になっていたが、Geminiの汎用ランタイムが他パッケージの値も自分の元コード345へ戻し、Googleを古いと誤判定。画像どおりの警告とStoreへの遷移結果を再現。
- Compatible Play Store Update Blockingをdefault追加し、汎用ランタイムの復元を自分の.morpheだけへ限定。未選択時はNoop。int/long/majorの既存処理を維持、レジスタ数を拡大しない。これは更新停止そのものを自動有効化するパッチではなく、公式の任意パッチとの互換修正。
- 再現中に、Gemini単体APKのエラー画像がdensity split依存で落ちる別件も発見。現行版のBardErrorLogoをframework画像へ変更し、コード1の低バージョンfixture/クローン未導入時も警告が正しく表示されることを確認。警告分岐や最低版チェックは無効化していない。新しいstyleフックを版別Kotlin/JSONへ記録。
- 最終MPPからDesktop+公式1.41で23/23、実Android Manager2.5/Patcher2.3+公式2.2で生成/保存した両APKでも同じ実ストアの23/23を成功。両方/片方の最大値、元のコード、foreign/own/major、同一署名ダウングレード拒否、真の不足/低コード、原本/アカウント維持を確認。
- default回帰API31/API36各26、MPP設定11も成功。追加のUniversalVersionProbeとVerifyUpdateCompatibilityで実APKと分岐/レジスタを検査。MPPは27パッチクラスをDEX収録。検証MPP SHA256 c8fde8dad57baba664fd8a8f948125fdeca77165b20a0cba37ad73b9ba771778。
- 詳細/コマンド/範囲/ハッシュはdocs/testing/issue-8-play-store.md。生UI XMLやAVDにはログイン情報があるためlocal/のまま公開しない。StoreログインとmicroG側のチャット認証は別で、今回は後者を確認したとは扱わない。
- リリース本文は変更/修正2点のみ。CI/配布物照合後に公開・短い返信を行う。署名キーは既存を維持し、公開APK/キー/生ログを添付しない。

### v0.3.5公開・返信完了（2026-11-13）

- コードcommit `cf964a6f4c2239015b678e9ad1b5f0d6e53ec5b0` のCI `43244021175` が成功。CI生成MPPが実APK生成・検証に使用したMPPとバイト一致した後にタグを作成。Release `43244474608` も成功し、[v0.3.5](https://github.com/ryuya0124/gemini-microg-patches/releases/tag/v0.3.5)を公開した。
- 公開MPPを再ダウンロードし、182391 bytes、SHA256 `c8fde8dad57baba664fd8a8f948125fdeca77165b20a0cba37ad73b9ba771778`で一致。SHA256SUMSのMPP/更新JSONも照合済み。mainの更新JSONは0.3.5へ反映。本文は修正点2項目だけ。
- APK入力guard追加後のManager最終マトリクスも23/23成功。証拠はMacのlocal/issue8/play-store/manager-final-results、集計JSONのWindows控えはoutputs/v0.3.5-evidence。認証済UI XMLは公開しない。
- [Issue返信](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6827381066)で原因と、原本両APKをsource既定選択＋任意の更新停止で再パッチする方法を短く案内。Issueは閉じていない。投稿者の実機での解消報告はまだない。
- 専用の旧回帰AVD5580/5582は名前を確認して停止。ユーザーがログインしたPlay Store AVD5584とWindowsのscrcpy画面は維持し、アカウント/公式アプリを消去していない。実機/Tail ADB/ADBサーバーは変更・停止していない。

## Issue #8 会話内の更新要求（2026-11-13、調査中）

- 追加画像はConversation requires an updateという既存会話内の表示。利用者は原因確定・エミュレータ再現・修正確認後の公開を明示。まだ再現/原因確定しておらず、新版や警告抑止を準備しない。
- 公式Play Store AVD5584の昨夜のstockログインは残っていた。利用者の許可で公式Geminiの初回案内を進め、テキスト応答と画像生成を確認。microG RE7.3.0を同じAVDへ追加し、利用者が手動ログイン。改造版の新規チャット応答と、公式版で作成した画像会話の読込も成功した。
- Windowsの旧scrcpy/トンネルは停止済みだったため再表示。タイトルGeminiPatch_microG_Android12。再起動で切断されるため、セッションの最新記録はWindowsのoutputs/playstore-desktop-session.jsonを参照し、再利用前に実在確認する。ログイン中は画面取得を停止した。
- 原本GoogleのenumにUNSUPPORTED_CLIENT_TOO_OLD /1086があるが、現時点で実際にそのエラーが起きた証拠はない。起動版チェックのログだけを根拠にフックしない。投稿者返信6839017500は既存会話で発生・新規は未試行・Google17.72.17。
- 現状と次の検証はdocs/testing/issue-8-conversation-update.md。private証拠はlocal/issue8/conversation-update。実機変更なし。会話互換性やサーバー原因だとまだ断定しない。
- 利用者は確認依頼が先行したことを指摘し、削除を指示。今回の確認依頼2件はGitHub CLIで投稿者/Issue/本文を確認して削除し、不在も検証済み。当時は追加質問を停止。追加デバッグ後の質問許可は後述の最新指示を優先する。
- 実Manager両APKでも既存の画像会話/Canvas会話へ新規送信し、固有のIMAGE_MANAGER_OK/CANVAS_MANAGER_OKを受信した。両方MAX、Google自身の実コード301831138復元と相手MAX維持の実プローブも成功。元Googleだけ無効/元Geminiだけ無効/両方無効の3条件でも既存会話から各固有応答を受信。finallyで元のenabled値・版を完全復元した。削除状態とは区別する。
- 最新指示は「原本の版をそろえた上でアプデ無効パッチ」。公式Storeから入ったGoogle17.72.17.ve.arm64/301831138とGemini1.0.995038376/345を全split付きで取り出した。Google base SHA63721fbf8f688e8aff45af942856d5f23583549306413f156f627aa8a849cdb8、Gemini baseは既知793fdf...と一致。Google14DEXは前のAPI30原本とすべて一致するがManifest/resources/assetsのパッケージングは異なる。
- local/issue8/conversation-update/build-store-pair.shで、当該原本に公開0.3.5MPPの既定＋Disable Play Store updatesを適用・導入済み。全8APK（base2、split6）は同じclone名/MAX/既存署名にそろえ、署名と16KiB整列を事前確認。既存画像会話/Canvas会話でSTORE_ALIGNED_IMAGE_OK/STORE_ALIGNED_CANVAS_OKを受信し、再起動後も警告なし。版をそろえた条件でも報告の警告は未再現。
- 原本Geminiだけuser0から一時削除（データ保持）した比較も実施。実際のpackage判定で原本なし/cloneありを確認した上で既存2会話が新規応答。最後に原本Gemini code345/UID10153/enabled1/installed=trueへ復元済み。Google原本は削除していない。実機変更なし。
- 別件のProvider参照変更漏れを通常UIDで再現。Manifestだけclone authorityとなり、DEXのURI/Provider内チェックが旧名のままでStateDumpProvider.queryが例外。ローカル候補でManifest由来のauthority/URI参照を統一し、同じContentResolver queryがFAIL→PASS。会話警告の原因と判明したわけではない。警告を隠すフックや機能制限解除は追加していない。
- ローカル候補は同じ原本＋更新無効化から作成し、同じ署名で上書きしてclone UID/アカウントを維持。画像/Canvas既存会話からPROVIDER_IMAGE_OK/PROVIDER_CANVAS_OKを新規受信。両アプリ実ランタイムのown/foreign/int/long/major版テストも成功。現時点のAVD5584はこの未公開候補。private証拠はprovider-candidate/。sourceの版別Kotlin/JSONと静的contract検査も更新。
- baselineの公開0.3.5MPPは182391 bytes/SHA c8fde8...1778のまま保持。候補はbuild/gemini-patches.jarでのみ生成し、公開MPPを上書きしていない。Windows branch investigate/issue8-conversation-compat、未commit、push/tag/release/追加返信なし。Macのsourceにも同じ候補を反映済み。失敗したconsole診断でrootは得られておらず、データを消さず通常起動へ戻した。

### Macで最新版取得・対応版明記・追加デバッグ（2026-11-13）

- 利用者は追加デバッグ、Morpheの「全バージョン」表示廃止、Issue報告版/現行版/最新版と今後の追加への対応を要求。全16名前付きパッチのcompatibleWithをVersionHookRegistry.compatibilityFor経由へ変更。未登録版と異なるversionCodeは従来どおり拒否。コンパイル済みの全エクスポートを検査し、null/空/不一致の対応版一覧をCIで拒否する。docs/versions/README.mdの版表も同じ登録内容から生成する。
- Windowsブラウザはツールが未対応として停止。Macの既存Codexチャットへ取得だけ依頼し、SafariからGoogle17.74.18.sa.arm64/301834567/API30+nodpi単体APKを確保。407168160 bytes、SHA256 dadbf2fa043f5d7b07a6d0ca57a427139e274e69a4331794d18a4d3cc7f9adbc。原本未変更。API24–32のGoogle署名f0fd6c...db83とAPI33+の鍵ローテーションを検証。source/Git/鍵/AVDは補助チャットに変更させていない。取得先local/issue8/latest-20261113、download-verification.json参照。
- DexKitの8一意候補+5プロセス名候補から実DEXを確認し、Google_17_74_18.ktを追加。プロセス名Leuhx.bのキャッシュ/初回は実DEXで共通RETURN1か所、トランポリンindex146→392、MainActivity.d:Z/code793/v0/23registers、Provider28件。Jadxの見かけのreturn2つを数に使わない。ローカル候補でstatic7項目+clone契約+署名+16KiB整列を確認。
- 17.72.17に一時的な観測拡張を入れ、Google/Gemini更新停止の全4組合せを同じUID/アプリデータのまま比較。全組合せで画像/Canvas既存会話へ新規返信。観測したconstructedVersionは正しい17.72.17.ve.arm64、capability数/hash集合も全組合せ一致、警告/readonly/error分類なし。全通信のキャプチャではなく、eligibility requestVersionの観測点は発火しなかったことに注意。診断APKは4KiBページAPI31専用で、16KiB非対応の再処理ライブラリを含むため配布禁止。現在の認証済みAVDからは削除済み（通常APKで上書き）。
- Google原本をuser0から一時削除・データ保持した条件も新規返信できた。finallyで元UID/版/enabledを復元。その後、専用認証済みAVD5584の公式Googleを17.74.18へ同UID更新。公式最新版で画像/Canvas会話を更新し、旧17.72.17 cloneが新しい公式ターンを読んだ上で返信、次に17.74.18 cloneでも同じ会話に返信。新規画像生成と通常Android再起動後の返信も成功。同じstock/microG account各1件を保持。
- 5584の現状は公式Google17.74.18.sa.arm64/301834567、公式Gemini1.0.995038376/345、両clone同名版/MAX。Googlecloneは単体APK、Geminicloneはbase+4split。UIDとログインを保持し、observerコード/debuggableは通常候補への上書きで除去。実機への更新・設定変更なし。Windows scrcpyを再表示、outputs/playstore-desktop-session.json（SSH9951/scrcpy15275、再利用前に実在確認）を参照。
- 別のアカウント未登録AVD5582で検証専用MPP0.3.6-dev.20261113をManager2.5へ読込。実画面にGoogle4版/Gemini2版の具体名を確認し、Expert OFFの標準11パッチで最新版Googleを実生成・保存。Manager出力2ae95d...28b8はstatic全項目/署名/16KiB/インストール/ネイティブ起動/実プロセス名3経路を成功。更新停止なし標準出力なのでcode301834567で正しい。認証済み5584のデータ削除とは区別。容量確保で5582 Downloadの古い5重複APKだけを、host保存物とのSHA一致確認後に削除。host証拠は維持。検証終了後にこの5582だけ停止し、認証済み5584/Windowsの画面は保持。
- 配布済み0.3.5MPP c8fde8...1778、patches-bundle.json、公開タグ/リリースは変更なし。追加のprivate MPPはlocal/issue8/latest-20261113/mppに隔離（29class JVM/DEX、inventory.json）。実テスト後のsource更新はvalidationテキスト/説明の追記だけで実行フック対象は不変。会話内の更新要求は未再現・原因未確定なので、この警告を修正したとは扱わず公開しない。この時点では追加質問も停止していたが、次の最新指示で許可された。詳細はdocs/testing/issue-8-conversation-update.md。

### 追加質問を許可・Issueへ投稿（2026-11-13、後に質問停止）

- 利用者が「未再現のため追加の質問を許可する」と明示。以前の質問停止はこの指示で解除。公開条件（原因特定、エミュレータで再現・修正確認）は維持。
- [追加質問6846494726](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6846494726)を短い英語で投稿し、本文/投稿者/保存先を読み戻して確認。Geminiアプリ版とモデル、会話の作成元とGem/Canvas/Deep Researchの使用、新規会話とWebでの継続可否を尋ねた。Google17.72.17は既に回答済みなので再質問していない。
- 返信待ち。Issueを閉じず、修正済みとは述べず、リリース/タグ/公開MPP/アプリの状態は変更していない。返答を次の再現条件に反映する。

### サーバー応答の調査・追加質問停止（最新指示）

- 利用者がサーバー側の応答を疑い、「これ以上の追加質問はせず調べて」と指示。質問許可はここで終了。既存の投稿は消さず、追加質問せず、返信待ちで調査を止めない。
- 受信側の構造化エラー/会話メタデータから警告表示までを調べる。過去のconstructedVersion/capability観測だけでは受信応答は追えていない。enumの存在だけでサーバー原因と断定せず、合成エラーの表示試験と自然発生した不具合を区別。原因確定・実再現・修正確認後の公開条件は維持。
- 原本17.74.18でazim.aがgrpc-status-details-binのRobinStatusを解析し、gszm.c=詳細コード/d=サーバー文面を扱うことを確認。atix.b/awnq.dにはflag付き文面上書き、awno.mにその表示経路がある。1086の標準文面はGoogleアプリ更新要求で、報告の会話カードと異なる。1086が今回の原因とはまだ言えない。
- ProcessQueryの受信はguvm、送信はguvj。最初のobserverは向きを誤り、一覧gsapだけで上限にも達していた。v2は実受信atti入口/guvm拡張を観測し、画像/Canvasへの新規返信時17受信入口・228値スキャン、画像編集＋続行時33入口・232値スキャンを記録。いずれも更新カード/観測点のエラー/readonly/既知警告文面は出なかった。選択箇所とサイズ/件数上限があるため全通信の否定には使わない。
- INTERNET権限のない別UIDの合成プローブで、実APKのgRPC trailer解析→1086分類と、nativeエラー文面上書き表示を成功。自然再現や修正ではない。プローブは終了後削除済み。GSAPのDailyBrief、GSAVの予定タスク、plan由来readonlyなどの別経路も切り分け、抑止フックは追加していない。
- 計測APKは各finallyで通常Google候補SHA847704...5550/UID10182/MAXへ復元。最終checkerで原本/clone全4セットのAPK hash/UID/版、stock/microG各1の同一アカウント保持を確認。実機変更なし、公開0.3.5MPP c8fde8...1778は不変。Issue最新は既存質問6846494726のまま、新規質問/返信/公開なし。
- 詳細と次の解析点はdocs/testing/issue-8-server-response.md。private証拠local/issue8/server-response/{observer-v2,observer-image,status-probe}。合成probeの初回はgRPC定数名の難読化で失敗したが、実fromCodeValueへ修正して最終PASS。アプリの不具合とは区別。会話カードは未再現・未解決であり、公開条件は未達。
- 追加の負の対照で、grpg.cの申告capabilityだけをコピー上で空にするprivate拡張を2送信構築点へ追加。29回実適用した画像/Canvasのテキスト続行は成功。画像新規要求では16回適用・受信11回、固有ackだけ返り181.6秒で画像なしのためharnessはFAIL（成功扱いしない）。どちらも報告カード/観測点のerror/readonlyはなし。全capability削除はfallback対応も除くため、選択的な不足を否定する試験ではない。失敗時もfinallyで通常APK/hash/UIDを復元。証拠reduced-caps/reduced-image、配布禁止。
- 復元後に両会話からRESTORED_SERVER_CONTROLの新規返信、同じ赤四角/白背景の画像要求でRESTORED_CAP_IMAGE_OK＋画像を15.5秒で確認。単発なので速度比較ではない。最終4アプリhash/UID/版とstock/microG同一account維持も再確認。restored-image/とfinal-state.json参照。警告そのものは最後まで未再現。

### 投稿者の追加回答とWeb由来の会話（2026-11-13）

- [返信6847166384](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6847166384)を読み直した。「Gemini auto」、Webで作成後にアプリでも複数prompt、Gem/Canvas/Deep Researchなし、新規chatとWeb継続は可能。Gemini APK正確版は未提示。「Auto」とこちらのモデルUIが同じとは断定しない。追加質問禁止は継続、新規のIssue投稿なし。
- 5584のChromeを既存test accountで初期化し、stock/microGとWeb account表示の一致をローカル比較、履歴syncは拒否。メール表示のUnicode Cfを除いて比較すること（初回の文字列不一致はアカウント違いではなかった）。Webで新規の通常会話を作成・WEB_ORIGIN_PLAIN_OKを受信し、GeminiPatch Issue8 web plain regressionと改名。private URLはUIから取得してlocalのみ保存、他会話は開かない。
- 最新17.74.18 cloneで当該Web返信を読みWEB_LATEST_PLAIN_OK、Webに戻りそのnative返信を読みWEB_CONTINUE_PLAIN_OKを受信。公開0.3.5のStore入力17.72.17/345両clone・全8APKへ同キー上書きした条件でも、当該会話からWEB_REPORTED、FOLLOWUP、FINALの各新規返信が成功。stock原本Googleは最新17.74.18のままなので完全な原本pair降版とは言わない。
- 同じWeb会話にGoogleアプリ無効化の説明（実行しない、番号リスト/表/シェルコード）を要求し、WEB_CONTINUE_RICH_OK・adb shell文字列・コードcopy操作・table widgetを確認。Webのモデルは4 Hydrogen/思考低、native見出しGemini Everyday helpで、Auto同名選択肢はまだ確認できていない。詳細docs/testing/issue-8-web-origin.md、private local/issue8/web-origin。
- 元の古いmarkerが画面外になった連続試験と、長文marker未確認の初回は送信前INCONCLUSIVE。アプリ障害に数えない。harnessは既知の自分の会話内のRecyclerViewを最新側へscrollし、直近markerを確認、既存ackを拒否するよう修正。旧pair wrapperは各finallyで通常候補の全hash/UID/版を復元する。
- 長文の追加切り分けでは、Webの段落/inline要素がnativeで151文字の1 TextViewへ結合され、目印が独立行でなくなっていた。送信前に止まったRich初回/retry/latestは検査側の誤判定で、同じ更新カードはなし。markerはassistant_robin_text内で確認するよう修正（user prompt内の同名markerは除外）。最新版で長文Web markerをscroll0回で確認後、WEB_RICH_CURRENT_PLAIN_OKを新規受信。アプリの修正ではなくharnessの修正。
- その長文を含む同一会話で公開17.72.17 pairからWEB_RICH_BASELINE/NEXTの2新規返信も成功。最新版両cloneの完全復元後、Webが最後の旧native返信を読みWEB_CONTINUE_FINAL_OKを返すところまで成功。正の結果はreported-pair-followup/、reported-rich-final/、各WEB_*結果とweb-continue-{PLAIN,RICH,FINAL}.json。初回INCONCLUSIVE群は成功/アプリ障害のどちらにも数えない。報告の更新カードは依然未再現、Auto同一条件も未確認なので公開条件未達。
- 最後に復元済み最新nativeがWebのFINAL返信を読み、WEB_FINAL_RESTORED_PLAIN_OKを新規受信。原本/clone全4 APKセットhash/UID/版とstock/microG各1の同一account保持をfinal-state.jsonで再確認。現在5584は元の通常候補17.74.18/345の両clone、診断コードなし、作業用の自分の会話をnativeで表示。ChromeのWebログインも維持。新規質問・Issue返信・push・tag・releaseはなし。公開0.3.5MPP/更新JSONは不変。

## Issue #8: diagnostics3 and reporter2 (2026-11-14)

- Reporter comment6872288929 supplied diagnostics2. All4 processes acknowledged stop, minimumObservedLoss=0; metadata coalescing remains intentional. Native setter/view evidence identifies PuntDisclaimer (bhej/biyy), not a quotation in assistant text. Full analysis: diagnostics/reporter-log2-2026-11-14.md.
- Source tracing found top-level gffq.d history warning -> aozi.z -> aofx.k -> bdci.e -> bdbo.w -> bckt -> bhej. gfeh.p/q and gfnl.k are distinct thread/turn sources. These findings establish the renderer/data path, not the cause or a fresh server rejection. Do not suppress punts or invent capabilities.
- Diagnostics3 directly records warning submessage presence/behavior/known-warning flags and static locations; full history/generate envelopes are not serialized. Exact owning profile and register/site assertions retained. Synthetic native protobuf/5000-burst/privacy tests, authenticated Web-origin continuation/document-picker export (4 ACK/loss0), actual Android Manager default12 generation/install/export (3 ACK/loss0) and exact ordinary app restoration passed. Both dedicated AVDs stopped with data retained; Galaxy untouched.
- Tested MPP233997 bytes SHA256 e4521891ea77059a1373e74decc4f04b96e0850bc1fbcfc007146ecf1dc97030. Diagnostic release only; stable v0.3.5/feed unchanged. Cause and natural reproduction/correction remain unverified, so no stable warning fix may be published.
- LATEST USER INSTRUCTION: Every Issue reply requires the user's review BEFORE posting. Interim receipt/status comment6874790622 was explicitly deleted at the user's request; absence verified. Do not post an acknowledgment, capture request or status without review. A new capture request can be prepared for review, not sent automatically.

## Diagnostics3 published (2026-11-14)

- User requested the new diagnostic build. Published v0.3.5-diagnostics.3 as a prerelease: https://github.com/ryuya0124/gemini-microg-patches/releases/tag/v0.3.5-diagnostics.3 . Target91955d6d6f9ca4c15197ffad41eb4bc93d0313be, CI43579120590 success. All three public assets downloaded and matched byte-for-byte with tested CI artifacts; MPP SHA256 e4521891ea77059a1373e74decc4f04b96e0850bc1fbcfc007146ecf1dc97030,233997 bytes.
- Stable latest remains v0.3.5. Diagnostics3 only improves observation; warning trigger/cause and an actual fix remain unverified.
- No Issue announcement/capture request posted. Draft remains outputs/issue8-diagnostics3-reply-draft.md; every new reply requires user review. Authorized explanation-only comment6875041545 is the current reply; deleted receipt/status comment6874790622 remains deleted.
