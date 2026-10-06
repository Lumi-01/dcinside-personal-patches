# DC Inside personal patches

Community-maintained Morphe patch bundle for `com.dcinside.app.android`. This repository contains patch code only. It does not contain the DC Inside APK, decompiled vendor code, account data, or signing keys, and is not affiliated with DC Inside or Morphe.

The baseline is Android app version **5.3.6** (`versionCode 100175`). The bundle has been built and applied to that original APK with Morphe Desktop, and its output signature and DEX files passed structural checks. Device execution could not be tested because the available emulator did not boot; 5.3.6 remains marked experimental until that check succeeds. Newer versions are also experimental: method names, bytecode, resources and native ABI strings may change. The patch deliberately fails when a required fingerprint or binary anchor is missing. A successful build does **not** by itself establish compatibility with a later app release.

## Scope

- Stop selected app-owned ad configuration and loader paths, and remove selected ad SDK startup providers and advertising permissions.
- Remove specific ad layout gaps and the in-app rating prompt.
- Retain the vendor's signer allowlist while adding the certificate Morphe actually uses to sign the locally patched APK. A narrowly scoped native digest delegate maps only that installed certificate's public fingerprint to the original public fingerprint. Other digests are delegated unchanged.

This initial bundle is separate from the manually rebuilt personal APK. See [MAINTAINING.md](MAINTAINING.md) for the feature checklist and gaps that must be verified before calling a newer version supported. Do not combine it with another DC Inside patch source without checking conflicts.

## Build and use

Morphe's official Gradle template requires GitHub Packages authentication with `read:packages`. Set `gpr.user` and `gpr.key` in your **user-level** Gradle properties, as described in the [Morphe setup guide](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_1_setup.md), then run:

```sh
./gradlew buildAndroid
```

The `.mpp` file is written to `patches/build/libs/`. In Morphe Manager, add `https://github.com/Lumi-01/dcinside-personal-patches` as a [patch source](https://github.com/MorpheApp/morphe-manager/blob/main/docs/patch-sources.md). The matching [bundle release](https://github.com/Lumi-01/dcinside-personal-patches/releases/tag/v1.0.0) is also available for a manual Morphe Desktop patch. Review the experimental status before installing the output APK.

Morphe signs the patched APK with its own key. Keep Morphe's signing key and app data if you want later Morphe-patched updates to install over it. This public repository does not distribute a patched APK.

## Limits

No claim of battery-life improvement is made without device measurements. Never disable push messaging, user-selected automatic backups, server configuration, login, or media features solely to save battery. The source may need revision after an app update; there is no safe unconditional patch for all future versions.
