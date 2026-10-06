# 1.0.1 (2026-10-07)

### Changes

* **DC Inside:** Collapse ad gaps in gallery lists and post body/comments; add spacing above the recommendation controls.
* **DC Inside:** Zero six ad-only dimensions, adapted from the Ample 5.3.4 patch and verified against 5.3.6 resources.
* **DC Inside:** Add optional home-section and post-list page-label hiding choices.
* **DC Inside:** Suppress analytics startup and use activated settings on transient remote-config fetch failure.
* **DC Inside:** Make redundant remote-config refresh limiting opt-in and disabled by default to avoid delaying server settings.
* **DC Inside:** Preserve the Realm configuration register when disabling Crashlytics, fixing post opening.

Android 11 and 15 emulator checks covered home and a post; Android 11 also covered gallery list and comments. Login, posting, ad-request counts, and device battery impact are unverified with this bundle.

# 1.0.0 (2026-10-06)

### Features

* **DC Inside:** Disable selected ad loaders and ad spaces.
* **DC Inside:** Remove the in-app rating prompt.
* **DC Inside:** Add local signing compatibility for Morphe-patched APKs.

This release is experimental. The bundle has been applied to the original 5.3.6 APK with Morphe Desktop; full feature and battery testing on a device remains in progress.
