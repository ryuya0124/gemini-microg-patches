# Issue #8: Web-origin conversation comparison

## New reporter information

In [comment6847166384](https://github.com/ryuya0124/gemini-microg-patches/issues/8#issuecomment-6847166384), the reporter answered:

- The model/app description is "Gemini auto". This does not give an exact Gemini APK version; do not infer one.
- The affected conversation started on the Web and later received some prompts in the Android app.
- No Gem, Canvas or Deep Research was used.
- A new Android chat works, and the affected conversation can still continue on the Web.

Google17.72.17 was already specified in the earlier reply. This new information changes the next comparison: previous image/Canvas fixtures originated in the official Android app, so their successful continuation did not cover Web origin. The latest instruction still forbids further questions. No new issue question or reply was posted for this phase.

## Setup and model limits

Used only the existing signed-in API31 Play Store AVD5584, not the physical Galaxy. Initialized its existing Chrome, verified that the selected account and the Web account label match the single stock/microG test account, and declined Chrome history sync. Chrome inserts bidirectional-format characters in account labels; the private account checker removes Unicode Cf formatting before comparing and prints booleans only. No credentials or identities are recorded in this document.

Created one new ordinary Web conversation through the real Gemini Web UI, received WEB_ORIGIN_PLAIN_OK and renamed only that owned fixture to **GeminiPatch Issue8 web plain regression**. Its private URL was read from Chrome's focused address bar and stored locally; it is not published. No unrelated conversation was opened.

The Web model menu on this account showed4 Hydrogen selected, with low thinking selected, and offered3.5 Flash-Lite,3.1 Pro,4 Argon and4 Carbon. There was no option named Auto in that menu. The native conversation heading in completed comparisons was **Gemini Everyday help**. These labels are observed account/UI state, not proof of equivalence to the reporter's "Gemini auto". No model spoof, forced feature enablement, Gem, Canvas or Deep Research was applied.

## Completed comparisons

1. With the ordinary local latest candidate (Google17.74.18 /Gemini1.0.995038376, both clone codes2147483647), the native app loaded the Web-created reply and returned a fresh WEB_LATEST_PLAIN_OK. The Web then explicitly loaded that native reply and returned WEB_CONTINUE_PLAIN_OK. No reported update card appeared.
2. Temporarily installed the frozen public0.3.5 reproduction pair from the actual Store inputs: Google17.72.17 /Gemini1.0.995038376, source defaults plus update blocking, all three Google APKs and all five Gemini APKs. Preflight checked clone package names/MAX codes, the existing signing certificate and16KiB alignment. The Google base SHA256 is970f2dd5fa5316f97917fce1e0dd16c6e744b8b71ed47e9811703a770d82b258; Gemini base isa6f6c84b350b7b2c56f16bb4a8dda53864e70f0b2261d51b48d057fab491c829. Same-key updates retained both clone UIDs and account access.
3. That17.72.17 pair loaded the newer Web reply and returned WEB_REPORTED_PLAIN_OK. A following run returned fresh WEB_REPORTED_FOLLOWUP_PLAIN_OK and WEB_REPORTED_FINAL_PLAIN_OK in consecutive app interactions, each checking the immediately preceding marker. No update card appeared. The first attempt at the second step required an older, now offscreen marker and stopped as inconclusive before sending; it is not an app failure.
4. Returned to the same Web conversation and requested Android/ADB Google-app documentation with numbered steps, a Markdown table and a shell code block, without executing any commands. The Web loaded the preceding native reply and returned WEB_CONTINUE_RICH_OK. Accessibility observations confirmed the acknowledgement, adb shell text, a copy-code control and a table widget. This approaches the rich ordinary response in the reporter's screenshot while retaining a Web-origin conversation.
5. After correcting the diagnostic content check described below, the latest candidate loaded that rich Web reply and returned WEB_RICH_CURRENT_PLAIN_OK. The public17.72.17 pair then loaded that new native marker and returned fresh WEB_RICH_BASELINE_PLAIN_OK and WEB_RICH_BASELINE_NEXT_PLAIN_OK in two consecutive interactions, with no update card. The wrapper restored both exact latest candidates. The Web subsequently loaded the second old-app reply and returned WEB_CONTINUE_FINAL_OK, still in the same fixture.
6. The restored latest native pair then loaded WEB_CONTINUE_FINAL_OK and returned WEB_FINAL_RESTORED_PLAIN_OK. The final checker again confirmed every original/clone installed APK hash set, retained UIDs, expected versions and one matching stock/microG account pair. There is no diagnostic observer/reduced-capability APK installed. Native Gemini is foregrounded on the owned fixture.

The stock original Google remains17.74.18 and stock Gemini remains1.0.995038376 throughout. The17.72.17 comparison changes the two **clones**, not the stock pair; do not describe it as a full downgrade of all originals. Exact reporter model/APK/account/conversation content remains unmatched. Positive results for this owned fixture do not prove that the reporter's conversation is compatible.

## Fixture navigation and restoration

The native history RecyclerView exposes only the currently materialized messages. A missing old marker after reopening can be an offscreen/virtualized message, so the harness must not call it a product failure. Native Markdown rendering can also merge Web paragraph/inline spans: the rich marker was present inside a151-character assistant TextView, but the first harness required an exact standalone line. The rich first/retry/latest attempts consequently stopped as INCONCLUSIVE before sending; they did not demonstrate an app failure. The revised harness scrolls the owned history toward the latest turn within its observed bounds and checks the marker **inside assistant_robin_text content only**, never in the user's instruction. It rejects an acknowledgement already present before sending. Every completed case uses a unique reply token; use new tokens/output directories for future repeats.

With that corrected content check, the ordinary latest candidate loaded WEB_CONTINUE_RICH_OK immediately (zero history scrolls) and returned fresh WEB_RICH_CURRENT_PLAIN_OK. This correction is to the diagnostic harness, not to the application or server.

Each old-pair wrapper restores the exact ordinary latest Google and Gemini APK sets in a finally block, including when an assertion is inconclusive. Restoration asserts installed hashes, version names, MAX codes and both UIDs against the pre-test state. No authenticated clone was uninstalled or cleared. The physical Galaxy, signing material and public v0.3.5 bundle are not modified.

Private evidence and helper source are under `/Users/ryuya/Documents/GeminiPatch/local/issue8/web-origin/`: account-check booleans, owned fixture URL, sanitized results, original/retry wrapper inputs and restoration JSON, and private UI XML. Keep authenticated XML/URLs/APKs untracked. Initial failed harness runs are retained rather than overwritten as passing tests.

Completed native results are WEB_LATEST/, WEB_REPORTED/, WEB_REPORTED_FOLLOWUP/, WEB_REPORTED_FINAL/, WEB_RICH_CURRENT/, WEB_RICH_BASELINE/, WEB_RICH_BASELINE_NEXT/ and WEB_FINAL_RESTORED/. Web results are web-continue-PLAIN.json, web-continue-RICH.json and web-continue-FINAL.json. The final corrected old-pair runs are reported-pair-followup/ and reported-rich-final/ (nativeFixtureTestFailed=false and exact restoration asserted). Final installed-state/account checks are final-state.json. Other initial/inconclusive folders must not be counted as product failures or completed continuation passes.

The reported warning remains unreproduced and has no established correction. The publication gate is unchanged: establish the actual triggering condition, reproduce on an emulator and verify that the same failing conversation genuinely continues after the correction. Ask no additional questions under the user's current instruction.
