# DC Inside personal patches

Community-maintained Morphe patch bundle for `com.dcinside.app.android`. This repository contains patch code only. It does not contain the DC Inside APK, decompiled vendor code, account data, or signing keys, and is not affiliated with DC Inside or Morphe.

The baseline is Android app version **5.3.6** (`versionCode 100175`). The bundle has been built and applied to that original APK with Morphe Desktop. Android 11 and 15 emulators loaded the home screen and a post; gallery list and comments were also checked on Android 11. Login success and posting have not been verified with this bundle. Version 5.3.6 and newer versions remain experimental: method names, bytecode, resources and native ABI strings may change. The patch deliberately fails when a required fingerprint or binary anchor is missing.

## Scope

- Stop selected app-owned ad configuration and loader paths, and remove selected ad SDK startup providers and advertising permissions.
- Remove specific ad layout gaps, set six ad-only dimensions to zero, and disable the in-app rating prompt.
- Collapse the remaining gallery ad rows and post footer ad views when their loaders are disabled.
- Keep repeated remote-configuration refresh suppression optional and off by default in the in-app Morphe settings. Enabling it can delay a server setting change until the next allowed refresh; startup and explicitly required refresh paths remain available.
- Reuse previously activated remote-configuration values when a transient fetch fails.
- Retain the vendor's signer allowlist while adding the certificate Morphe actually uses to sign the locally patched APK. A narrowly scoped native digest delegate maps only that installed certificate's public fingerprint to the original public fingerprint. Other digests are delegated unchanged.
- In-app **Morphe settings** entry at the top of the app's native settings screen. It has Korean explanations and switches for six home sections, `Page N` separators, author IDs, IP estimates and repeated configuration refreshes. These controls default to off and can be changed without repatching.
- Author IDs are shown only when supplied by the app. With the IP switch enabled, masked two-octet IPs are matched against AmpleReVanced's bundled community-maintained prefix memo table and labeled **network-range estimates**. They cannot identify a person, exact carrier or region. No IP lookup is sent to an external service.

The [Ample DC Inside patch catalog](https://morphe-patches.software/?app=com.dcinside.app.android#apps) currently targets app 5.3.4. Memo presets, DCCon controls and gallery watch are **not** in this 5.3.6 bundle. Morphe Manager's backup/restore/reset screen is already part of Manager itself. See [ATTRIBUTION.md](ATTRIBUTION.md) for the related GPLv3 project.

This bundle is separate from the manually rebuilt personal APK. See [MAINTAINING.md](MAINTAINING.md) for the feature checklist and gaps that must be verified before calling a newer version supported. Do not combine it with another DC Inside patch source without checking conflicts.

## Build and use

Morphe's official Gradle template requires GitHub Packages authentication with `read:packages`. Set `gpr.user` and `gpr.key` in your **user-level** Gradle properties, as described in the [Morphe setup guide](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_1_setup.md), then run:

```sh
./gradlew buildAndroid
```

The `.mpp` file is written to `patches/build/libs/`. In Morphe Manager, add `https://github.com/Lumi-01/dcinside-personal-patches` as a [patch source](https://github.com/MorpheApp/morphe-manager/blob/main/docs/patch-sources.md). The matching [bundle release](https://github.com/Lumi-01/dcinside-personal-patches/releases/tag/v1.1.0) is also available for a manual Morphe Desktop patch. Review the experimental status before installing the output APK.

Morphe signs the patched APK with its own key. Keep Morphe's signing key and app data if you want later Morphe-patched updates to install over it. This public repository does not distribute a patched APK.

## Limits

No claim of battery-life improvement is made without device measurements. The optional refresh limiter may defer new server settings, so leave it off if immediate settings updates matter. Never disable push messaging, user-selected automatic backups, server configuration, login, or media features solely to save battery. The source may need revision after an app update; there is no safe unconditional patch for all future versions.
