# Maintaining the DC Inside patch

## App-version checklist

1. Obtain the updated original APK yourself. Record its SHA-256, package name, version name/code, minimum Android version, and native ABIs.
2. Decompile with JADX and Apktool. Compare the methods in `DcInsidePatch.kt` with the new APK. Replace brittle names with verified fingerprints when the app changes; never bypass a failed fingerprint by patching an arbitrary first match.
3. Check `lib/*/libnative-lib.so` for the two `java/security/MessageDigest` references. Their replacement must preserve byte length. Review native app-identity behavior before adding new ABI paths.
4. Confirm every edited XML ID exists and every edited method has the expected signature and semantics.
5. Build `.mpp` with JDK 21 and Morphe's `read:packages` prerequisite. Test it against the original APK with Morphe Desktop or Manager. Verify the output APK's signature and install on Android 11+ and 15+.
6. Check startup, login, login persistence, gallery lists, search, post body, images/video, comments, post/comment composition, push, automatic backup, and remote config online/offline. Do not post to a public gallery merely for regression testing unless explicitly authorized.
7. Add a newly tested `AppTarget` and record results. Keep unknown releases experimental and fail closed when anchors change.

## Patch layout

- `patches/src/main/kotlin/dev/lumi/dcinside/DcInsidePatch.kt`: package/version compatibility, binary and XML edits, method fingerprints and bytecode edits.
- `extensions/extension/src/main/java/local/privacy/SignerCompat.java`: runtime certificate lookup and narrowly scoped compatibility.
- `extensions/extension/src/main/java/local/privacy/MessageDigest.java`: JNI-facing digest delegate.
- `patches/build/libs/*.mpp`: generated bundle, never handwritten.

The official app, its decompiled original, account data, and signing keys belong outside this public repository. A separate private maintenance repository stores the full decompiled baseline and the manually rebuilt APK.

## Feature-parity status

The manually rebuilt version also includes additional native and UI adjustments, Firebase analytics flags, remote-config fallback, and throttled redundant config refresh. This Morphe bundle must be independently verified for feature parity; do not assume that its current patch set implements every private-build change. Preserve login and content APIs when extending it.

The 1.0.0 bundle passed a Morphe Desktop patch run on the 5.3.6 original APK and static DEX/signature checks. Runtime regression testing remains pending because the available emulator failed to boot. Keep 5.3.6 experimental until startup, login, content loading and post/comment actions are checked on a device.

## Release maintenance

For a manual release, update `version` in `gradle.properties`, build with `buildAndroid generatePatchesList`, upload the resulting `patches-<version>.mpp` to the matching `v<version>` GitHub release, and update `patches-bundle.json` with its URL, version and UTC creation time. Check the released asset hash against the locally built bundle, then confirm Morphe Manager can read the repository as a patch source. The optional release workflow is manual-only (`workflow_dispatch`); it must not be used on top of a release already created by hand.
