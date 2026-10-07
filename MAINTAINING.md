# Maintaining the DC Inside patch

## App-version checklist

1. Obtain the updated original APK yourself. Record its SHA-256, package name, version name/code, minimum Android version, and native ABIs.
2. Decompile with JADX and Apktool. Compare the methods in `DcInsidePatch.kt` with the new APK. Replace brittle names with verified fingerprints when the app changes; never bypass a failed fingerprint by patching an arbitrary first match.
3. Check `lib/*/libnative-lib.so` for the two `java/security/MessageDigest` references. Their replacement must preserve byte length. Review native app-identity behavior before adding new ABI paths.
4. Confirm every edited XML ID exists and every edited method has the expected signature and semantics. Recheck `PostReadHeaderView.Y`, the shared author span builder `span/g.t`, and `read/V.b` before carrying author or auto-image hooks forward. The metadata hook belongs at the start of `Y`: inserting it just before its final return is skipped by a branch targeting that return.
5. Build `.mpp` with JDK 21 and Morphe's `read:packages` prerequisite. Test it against the original APK with Morphe Desktop or Manager. Verify the output APK's signature and install on Android 11+ and 15+.
6. Check startup, login, login persistence, gallery lists, search, post body, images/video, comments, post/comment composition, push, automatic backup, and remote config online/offline. Do not post to a public gallery merely for regression testing unless explicitly authorized.
7. Add a newly tested `AppTarget` and record results. Keep unknown releases experimental and fail closed when anchors change.

## Patch layout

- `patches/src/main/kotlin/dev/lumi/dcinside/DcInsidePatch.kt`: package/version compatibility, binary and XML edits, method fingerprints and bytecode edits.
- `patches/src/main/kotlin/dev/lumi/dcinside/DisplayOptionsPatch.kt`: optional home-section and page-indicator changes. Each hard-coded item type and method must be rechecked for a new app version.
- `extensions/extension/src/main/java/local/privacy/HomeFilter.java`: filters home adapter items according to current in-app Morphe choices; on a changed item class it leaves the item visible.
- `extensions/extension/src/main/java/local/privacy/PageIndicator.java`: hides the verified page separator views.
- `extensions/extension/src/main/java/local/privacy/AutoImageFilter.java`: optional leading auto-image heuristic, off by default. Recheck the actual post HTML marker on an updated app; ordinary upload images must remain visible.
- `extensions/extension/src/main/java/local/privacy/AuthorInfo.java` and `IpInfo.java`: optional local author IDs and all matching community IP-prefix labels. The post header shows the ID beside the nickname and expands the existing IP/range text in place; do not add an external lookup.
- `extensions/extension/src/main/java/local/privacy/WebResourceBlocker.java`: Naver analytics source toggle and narrow WebView fallback for known analytics/ad hosts. Recheck the constructor of `wv/l` and its script-insertion guard after each app update. Never block all Naver hosts.
- `extensions/extension/src/main/java/local/privacy/SignerCompat.java`: runtime certificate lookup and narrowly scoped compatibility.
- `extensions/extension/src/main/java/local/privacy/MessageDigest.java`: JNI-facing digest delegate.
- `patches/build/libs/*.mpp`: generated bundle, never handwritten.

The official app, its decompiled original, account data, and signing keys belong outside this public repository. A separate private maintenance repository stores the full decompiled baseline and the manually rebuilt APK.

## Feature-parity status

The manually rebuilt version includes additional native and UI adjustments. This Morphe bundle now includes the Firebase analytics flags, remote-config fallback, and an optional redundant-refresh limiter, but must be independently verified for feature parity. The limiter defaults to off because suppressing refresh can defer changed server settings. Preserve login and content APIs when extending it.

The 1.0.1 bundle was applied to the 5.3.6 original APK and installed on Android 11 and 15 emulators. Home and post body loaded on both; gallery list and comments loaded on Android 11, and the recent-gallery hide option removed that home section. An earlier build changed a boolean register used later by the app's Realm configuration and broke post opening; replacing the Crashlytics enable call with `nop` preserved the register and fixed the same post on both emulators. The login screen opened earlier; account authentication and post/comment submission were not performed with this bundle. Keep 5.3.6 experimental until those and real-device behavior are checked.

For 1.1.2, the final APK was installed on Android 11 and 15 emulators. Android 11 rendered a supplied account ID and a masked IP with its offline range label on separate post-header lines. Android 15 displayed the first Morphe setting without clipping. Automatic-image detection on a known auto-image post, account login, and post/comment submission were not reverified for this bundle.

For 1.1.3, `collapse_long_ip_info` defaults on. Android 11 verified that the masked IP and the beginning of the offline range description stay on the nickname line; the right-side button expands and collapses that same text. A registered author's ID appeared beside the nickname without an `아이디` label. On Android 11, temporary host-only logging showed requests to `wcs.naver.net` and `wcs.naver.com` when the Naver toggle was off, and no such requests on a loaded post when it was on. The final bundle omits this diagnostic logging. The patch suppresses both script tags in `wv/l` before WebView loading and keeps a narrow intercept fallback. The default is on. The residual web ad switch is also on by default, but no residual ad host was observed in that test; do not claim complete network-level ad removal. The final APK was installed on Android 11 and a fresh Android 15 emulator. On Android 15, the top Morphe setting was visible and a post body loaded without an app crash. Neither emulator test quantified network latency or battery life.

For 1.1.1, retest the optional auto-image switch against both an actual auto-image post and an ordinary first image. The heuristic requires a DC Inside `viewimage.php` URL, a long hex `alt` marker, and no upload-file marker on the first image. If the server changes the markup, leave the image visible until a reliable new marker is confirmed. IP prefix labels may be numerous; the app's one-line list layout may ellipsize the visible text even though all known labels are appended.

For performance and battery, source suppression of the two Naver scripts avoids their WebView work and follow-on analytics requests. The auto-image HTML scan avoids copying and lowercasing the entire post body. The repeated remote-config refresh limiter remains opt-in because it can delay server changes. Intermittent network stalls were not reproducibly isolated in the emulator; measure on a real device before making broad background-work changes or claiming a quantified battery gain.

The [Ample bundle](https://github.com/AmpleReVanced/revanced-patches) currently marks DC Inside 5.3.4 as its target. This repository ports only the ad-dimension idea, home-section choices, and page-label hiding after 5.3.6 verification. Its gallery watch, DCCon loading, memo, author identifier, and history-filter features need separate runtime and layout checks before adoption. Gallery watch can increase refresh activity and battery use, so do not enable it as a battery optimization.

## Release maintenance

For a manual release, update `version` in `gradle.properties`, build with `buildAndroid generatePatchesList`, upload the resulting `patches-<version>.mpp` to the matching `v<version>` GitHub release, and update `patches-bundle.json` with its URL, version and UTC creation time. Check the released asset hash against the locally built bundle, then confirm Morphe Manager can read the repository as a patch source. The optional release workflow is manual-only (`workflow_dispatch`); it must not be used on top of a release already created by hand.
