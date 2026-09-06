# ビルド依存

`scripts/setup-tools.sh` が `config/tools.lock.json` に固定された公式配布物を取得し、SHA-256を検証します。

- Morphe Desktop 1.15.0
- Morphe patches 1.41.0
- Kotlin compiler 2.4.10

JDK 21とPython 3は別途用意します。署名にはAndroid SDK Build Tools 36.0.0が必要です。バイナリはGit対象外です。
