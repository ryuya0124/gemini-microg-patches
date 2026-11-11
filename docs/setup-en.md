# Supported setup (English)

Gemini is a launcher. Its native assistant/chat implementation is in the **Google app**. These patches redirect the launcher to a separately installed Google clone. They do not move the Google implementation into the Gemini APK.

`Gemini Standalone Support` means **installable without Gemini's resource splits**, not “independent of the Google app.” Keep the original system Google app installed. Do not remove it to work around an installation error.

## Supported input pair

| Component | Current local build input / tested version | Minimum Android |
| --- | --- | --- |
| Google app | `17.72.17.ve.arm64` / versionCode `301831138` | API 32 (Android 12L) |
| Gemini launcher | `1.0.995038376` / versionCode `345` | API 32 (Android 12L) |
| microG RE | `7.3.0`, package `app.revanced.android.gms`, previously verified on the Samsung device | Check microG's own requirements |

**Android 12 / API 31 cannot install this current APK pair.** Root does not change that SDK requirement. Historical hook records do not prove that a different input pair works on API 31. Provide the exact APK versions and `adb shell getprop ro.build.version.sdk` when reporting compatibility issues.

No root operation or de-Googled ROM is required by the install procedure. The Issue #8 regression was tested on an API 36 Google APIs emulator with its original Google system app present, using shell UID 2000 and without `adb root`/`su`. This is not verification on a physical OnePlus 8 / Android 12.

## Morphe Manager configuration

Use the matching source APKs, not an already patched APK. The Morphe versions used by the local pipeline are Desktop 1.15.0 and official patches 1.41.0.

For **both** Google and Gemini:

1. Select the compatible default patches from this repository.
2. Add the official Morphe source and select **Clone app** (Expert mode if needed).
3. Set these official Clone app options:

| Option | Required value | Result |
| --- | --- | --- |
| `packageName` | `Default` | Original package name plus `.morphe` |
| `updatePermissions` | `true` | Avoid custom-permission ownership collisions |
| `updateProviders` | `true` | Avoid provider authority collisions |

Expected installed packages:

- Google: `com.google.android.googlequicksearchbox.morphe`
- Gemini: `com.google.android.apps.bard.morphe`
- microG: `app.revanced.android.gms`

Custom clone names such as `com.quicksearch.bond` are **not supported**. DEX references and launcher visibility target the fixed `.morphe` packages.

Install the Google base and its repackaged resource split, Gemini, and microG in the same Android user/profile. Use the same signing key for the Google base, its split and Gemini. The local scripts handle the xxhdpi split; Manager-only installation of the base is not the complete verified resource setup.

Never enable these historical experiments:

- `Account ID Fallback for Clone`
- `Account Switch Lock Bypass`
- `Account Sync Binder Loop Fix`

They previously broke synchronization or token acquisition. They are disabled by default, and the patcher now rejects them before modifying DEX. “Select every patch” is not a supported configuration.

The GmsCore redirect targets `app.revanced.android.gms`. The old title containing `7.0.0` was historical wording, not a hard version check; the title was clarified. The known working microG version is 7.3.0. Please provide the exact version rather than the ambiguous spelling “7.30”.

## Verified local build

Follow the repository quick start, then:

```sh
scripts/build-all.sh
scripts/sign-apks.sh
ADB_SERIAL=your_device_serial scripts/install-device.sh
```

The pipeline explicitly sets the Clone app options and verifies final package names, isolated permission declarations, unchanged protection levels, manifest/DEX permission references and noncolliding provider authorities. It also checks fresh patch-result JSON so a failed patch cannot be mistaken for an old successful APK.

For an APK exported from Manager, export it **before installation** and run:

```sh
scripts/setup-tools.sh
scripts/compile-patches.sh
scripts/verify-clone-contract.sh original-google-base.apk manager-google-output.apk
scripts/verify-clone-contract.sh original-gemini-base.apk manager-gemini-output.apk
```

The CLI scripts enforce this output validation. This is not a claim that Manager itself reads the repository's validation scripts or automatically forces external-source options.

If the duplicate-permission error persists, provide your exact Google/Gemini/patch/Morphe versions, final package name, selected patches and Clone app option values. Do not publish signing keys, account tokens or full authentication logs.
