# Related implementation

The home-section and page-indicator behavior, plus the ad-dimension cleanup, were informed by the GPLv3 [AmpleReVanced DC Inside patches](https://github.com/AmpleReVanced/revanced-patches), whose published target is DC Inside 5.3.4. This repository adapts the concepts to verified 5.3.6 classes and resources. It does not bundle the Ample runtime or claim compatibility with all of its features. This repository is distributed under GPLv3; see `LICENSE`.

The bundled `lumi_ip_prefixes.txt` is copied from AmpleReVanced's `user_memo_preset_2.txt` at commit [`30930ba`](https://github.com/AmpleReVanced/revanced-patches/commit/30930ba4981e643b58c38c98dfb53cc83343a3f2). Its entries are community-maintained estimates of a two-octet IP prefix, not verified locations or identities. We append all distinct matching labels after the displayed IP, without a “대역:” prefix, and do not write them into the app's user memo database.
