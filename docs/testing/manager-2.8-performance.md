# Morphe Manager 2.8対応とパッチ適用時間

2026-11-28。Morphe Manager 2.8.0とMorphe Desktop 2.4.0はPatcher 2.6.0を使用する。Manager 2.9.0-dev.3はPatcher 2.6.1-dev.1を使用する。デスクトップの開発用ツールは2.4.0に固定した。 [Manager 2.8.0](https://github.com/MorpheApp/morphe-manager/releases/tag/v2.8.0) ・ [Manager 2.9.0-dev.3](https://github.com/MorpheApp/morphe-manager/releases/tag/v2.9.0-dev.3) ・ [Patcher 2.6.0](https://github.com/MorpheApp/morphe-patcher/releases/tag/v2.6.0)

## Macでの比較

Google `17.78.14.ve.arm64` の単体APKと標準パッチ選択を使った。公開済み `v0.3.11` と候補のMPPを、同じMorphe Desktop 2.4.0、Java 21、最大ヒープ1536 MiB、4コア設定で3回ずつ測定した。

| 指標 | 公開版 | 最適化後 |
| --- | ---: | ---: |
| APK生成の実時間の中央値 | 3.76秒 | 3.24秒 |
| 最大RSSの中央値 | 1600.50 MiB | 1497.47 MiB |

適用コマンドを1回ごとに新しいディレクトリで実行する。

```sh
/usr/bin/time -l java -Xmx1536m -XX:ActiveProcessorCount=4 \
  -jar "$MORPHE" patch "$INPUT_APK" -p "$MPP" \
  --unsigned -o patched.apk -r result.json
```

振動呼び出しの候補はPatcherのインデックスで検索し、選んだ命令だけを置換する。Googleの対応5版で、候補の実行命令、例外処理、クラス定義、バイナリManifestと `resources.arsc` を公開版と照合した。同一DEXで異なったのは物理オフセットとデバッグ情報の位置。Google 17.78.14は旧Patcher 2.5.0でも生成して同じ項目を照合した。

512 MiBを指定したDesktop 2.4.0は、17.78.14の事前検査でリソーステーブルを読み込む段階でメモリ不足になった。表の数値はDesktopでの測定で、Android版の処理時間やRAM使用量は別に測る必要がある。

## Android版Managerでの適用

Macの専用Android 12（API 31）エミュレーターで、公式のManager 2.8.0と2.9.0-dev.3を順に使用した。候補MPPは `0.3.12-test.1` としてローカルに追加した。両方のManagerで、Google `17.78.14.ve.arm64`（13パッチ）とGemini `1.0.995038376`（4パッチ）を標準選択で生成し、インストールした。

Manager 2.8.0では公開版で作ったGoogle 17.72.17を候補で17.78.14へ上書きした。Managerを2.9.0-dev.3へ上書きした後は、両クローンを同じ署名で再生成し、アプリIDの一致と更新時刻の変更を確認した。両方のManagerでGeminiがサインイン画面まで起動した。
