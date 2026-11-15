# Displayed replies — Secure Folder deployment

On 2026-11-15, at the user's request, the emulator-tested optional archive build was installed on the Galaxy SM-S948Q, Secure Folder Android user150. Only the Google clone was updated; the Gemini launcher and microG were unchanged.

Source: `aa0b20fabf6535c92c540f525e430bf0cc975f08`, experimental `0.3.6-preview.4`. The installed Google base SHA256 is `b919e246486243380a62a13a3d41d215aa44737711a071b6389d43ddf7e8be28`, with matching xxhdpi and Lens feature splits. All three installed APK hashes match the supplied files. The existing signing certificate was checked before deployment and reused; no new signing key was generated.

The Google base and both splits were committed in one replacement PackageInstaller session, explicitly for user150. Before/after checks verified:

- Existing Google application ID and CE/DE data inodes preserved.
- Google remained installed for user150 only, with no installation added to users0/10.
- Gemini and microG APKs, versions, application IDs and profile/data identities unchanged.
- Boot ID, system_server, SystemUI and SELinux Enforcing unchanged.
- Prior Google APKs backed up privately; temporary APK staging on the device removed.

Google still reports versionName `17.72.17.ve.arm64`. Its Android versionCode is now `2147483647`, as deliberately set by the selected Play Store update-blocking patch; the original input code is301831138. A first post-install assertion incorrectly expected the original code. The expected value was checked against the tested APK manifest, then read-only verification completed successfully; no second install or data reset was performed.

The **Preserve displayed replies** patch is included, so the archive defaults to saving text/images when the native chat screen runs. Video saving defaults off and is enabled through the archive viewer's video switch. See [usage](../displayed-replies.md).

This deployment verifies installation, exact APKs and data/profile preservation. It does not claim a physical-device chat, archive restoration or real Google video download was exercised. The [Android12 emulator checks](displayed-replies-emulator.md) remain the behavior verification for the feature. No account credentials or conversation contents were collected, and no app data was cleared or uninstalled.

Private Windows evidence is under ignored `local/device-preview4/` in the feature worktree: `before.json`, `after.json`, `result.json`, prior APKs and staged signed APKs. Keep these private and retain the existing signing key.
