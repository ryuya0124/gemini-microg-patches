# Gemini microG patches

[日本語](#日本語) · [English](#english) · [GitHub](https://github.com/ryuya0124/gemini-microg-patches) · [GitLab mirror](https://gitlab.com/ryuya0124/gemini-microg-patches)

## 日本語

microGを使い、Androidの通常領域やSamsungセキュアフォルダでGeminiにログインするためのMorpheパッチ集です。Geminiランチャー、パッチ済みGoogleアプリ、microGを同じプロファイルにインストールします。

### インストール

1. **Morphe Manager 2.7.1以降**を用意し、安定版のパッチソースを追加します。[GitHubからMorpheに追加](https://morphe.software/add-source?github=ryuya0124/gemini-microg-patches) · [GitLabの登録用JSON](https://gitlab.com/ryuya0124/gemini-microg-patches/-/raw/mirror-feed/patches-bundle.json)
2. Google **17.72.17.ve.arm64** と Gemini **1.0.995038376** の元APKを用意し、両方を **標準のパッチ選択** で処理します。Android 12では、GoogleはAndroid 11以降向け、GeminiはAndroid 10以降向けのバリアントを選びます。[対応APKとバリアント](docs/setup-en.md#input-versions-and-variants)
3. 端末標準のGoogleアプリを残した状態で、使用するプロファイルにmicroG、パッチ済みGoogle、パッチ済みGeminiをインストールします。セキュアフォルダで使う場合は、3つともフォルダ内にインストールします。microGにアカウントを追加してからGemini (Morphe)を開き、案内に沿ってアカウントへのアクセスを許可します。

GitLabから追加する場合は、上のJSONのリンク先を **Sources → Remote** に入力してください。MPPを手動で追加する場合は、リリースから `.mpp` を取得して **Sources → Local** で開けます。[GitHubの安定版リリース](https://github.com/ryuya0124/gemini-microg-patches/releases/latest) · [GitLabのリリース一覧](https://gitlab.com/ryuya0124/gemini-microg-patches/-/releases)

更新時は前回と同じ署名キーを使ってください。起動・ログイン・インストールで困ったときは、詳細手順を参照してください。[セットアップ詳細（英語）](docs/setup-en.md) · [トラブルシューティング](docs/troubleshooting.md)

## English

Morphe patches for signing in to Gemini with microG on Android, including Samsung Secure Folder. Install the Gemini launcher, the patched Google app, and microG in the same Android profile.

### Installation

1. Use **Morphe Manager 2.7.1 or newer** and add the stable patch source. [Add the GitHub source in Morphe](https://morphe.software/add-source?github=ryuya0124/gemini-microg-patches) · [GitLab source JSON](https://gitlab.com/ryuya0124/gemini-microg-patches/-/raw/mirror-feed/patches-bundle.json)
2. Get the original APKs for Google **17.72.17.ve.arm64** and Gemini **1.0.995038376**. Patch both using the **default patch selection**. On Android 12, choose the Google variant for Android 11+ and the Gemini variant for Android 10+. [Supported APK variants](docs/setup-en.md#input-versions-and-variants)
3. Keep the system Google app installed. Install microG and both patched apps in the profile you want to use. For Secure Folder, install all three inside the folder. Add your account to microG, open Gemini (Morphe), and follow the prompts to grant account access.

To add the GitLab source, paste the JSON link above into **Sources → Remote**. To add an MPP manually, download the `.mpp` from a release and open it via **Sources → Local**. [Stable release on GitHub](https://github.com/ryuya0124/gemini-microg-patches/releases/latest) · [GitLab releases](https://gitlab.com/ryuya0124/gemini-microg-patches/-/releases)

Keep the same signing key when updating an existing installation. See the setup guide for help with APK variants, account permissions, and optional Play Store update blocking. [Detailed setup guide](docs/setup-en.md)

## 開発 / Development

[開発ガイド / Developer guide](docs/development.md)

## Credits

[Morphe Desktop](https://github.com/MorpheApp/morphe-desktop) · [Morphe patches](https://github.com/MorpheApp/morphe-patches) · [microG](https://github.com/microg/GmsCore)
