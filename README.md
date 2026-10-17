RedReader
=========

An unofficial, open source Android client for Reddit.

[![CI](https://github.com/dwicao/RedReader/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/dwicao/RedReader/actions/workflows/build.yml)
[![Translation status](https://hosted.weblate.org/widgets/redreader/-/svg-badge.svg)](https://hosted.weblate.org/engage/redreader/?utm_source=widget)


This is a fork
--------------

This repository is a fork of the original RedReader project by
[QuantumBadger](https://github.com/QuantumBadger/RedReader), which remains the
upstream project and the canonical home of this app. Everything here is
distributed under the same GPLv3 licence, and credit for the original work goes
to the upstream project and its contributors.

The fork exists to carry a small set of extra features and performance work on
top of regular upstream merges. Upstream changes are merged in regularly, so
this build is normally a little ahead of the last official release rather than a
divergent codebase.

**The official releases are still the upstream ones.** If you want the polished,
store-maintained version of the app, install it from Google Play or F-Droid
(details below). The builds published in this repository are community builds
from this fork's `master` branch, and they are **not** signed with a release
key — see [Fork builds](#fork-builds) before installing one.


Features
--------

Inherited from upstream:

* Free and open source, with no ads or tracking
* Lightweight and fast
* Swipe posts left and right to perform customizable actions, such as
    upvote/downvote, or save/hide
* Advanced cache management - automatically stores past versions of posts and
    comments
* Support for multiple accounts
* Two-column tablet mode (can be used on your phone, if it's big enough)
* Image and comment precaching (optional: always, never, or Wi-Fi only)
* Built-in image viewer, and GIF/video player
* Multiple themes, including night mode, and ultra black for AMOLED displays
* Support for Android 5.0+
* Translations for multiple languages
* Accessibility features and optimization for screen reader use

Added in this fork:

* **Custom subreddit list** — keep your own list of subreddits and open it as a
    single combined listing from the main menu. Import a multireddit such as
    `a+b+c+askreddit`, long-press a post to subscribe or unsubscribe its
    subreddit directly, and long-press the menu entry to edit the list.
* **Faster grid images** — grid and column cards now pick an appropriately sized
    image instead of falling back to the full resolution original, use the still
    frame Reddit provides for hosted videos instead of downloading the whole
    video, and load more images in parallel.
* **Faster filtered listings** — filtering by image, video, text or link asks
    Reddit for more posts per request, so far fewer round trips are needed while
    scrolling.
* **Scrolling and cache work** — less work per bound row and per scroll frame,
    cache pruning no longer blocks cache lookups while it runs, and a growing
    number of duplicate flair observers are no longer accumulated per bind.
* Several bug fixes, including a crash when marking posts as read on scroll in
    grid view, and a dead retry button after a network error while scrolling.


Downloading
-----------

### Official releases

The upstream project is available on the Google Play store:

https://play.google.com/store/apps/details?id=org.quantumbadger.redreader

It is also available on F-Droid:

https://f-droid.org/app/org.quantumbadger.redreader

Google Play APKs from the upstream project are also available here:

https://github.com/QuantumBadger/RedReader/releases

<a href='https://play.google.com/store/apps/details?id=org.quantumbadger.redreader'><img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" alt="Get it on Google Play" height="80"></a>
<a href='https://f-droid.org/packages/org.quantumbadger.redreader/'><img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80"></a>

### Fork builds

APKs built from this fork are published in this repository's releases:

https://github.com/dwicao/RedReader/releases

Before installing one, please note:

* **They are signed with a debug key**, not a release key. Android will refuse
  to install a fork build over a store-installed copy (or over another fork
  build signed with a different key), because the signatures do not match.
  Uninstall the existing app first — you will lose its local data.
* **They are built from `master`**, so they are newer than the last upstream
  release, but they are not the officially tested store builds.
* **They use the same application id** as the upstream app
  (`org.quantumbadger.redreader`), so they are meant for personal sideloading
  and testing rather than redistribution.

To install a fork build:

    adb install -r RedReader-release.apk

If you would like to go back to the official version afterwards, uninstall the
fork build first and reinstall from your store.


Translating
-----------

Please help translate RedReader into new languages, or improve the translations
for existing languages! Translations are managed through the upstream project's
Weblate instance, so please contribute there rather than editing the string
files in this repository:

https://hosted.weblate.org/projects/redreader/strings/

[![Translation status](https://hosted.weblate.org/widgets/redreader/-/287x66-grey.png)](https://hosted.weblate.org/engage/redreader/?utm_source=widget)


Pull Requests
-------------

Bug fixes and small improvements are welcome, and contributions to the upstream
project are especially valuable, since that is where most users get the app from.
Please see the contribution guidelines in
[CONTRIBUTING.md](CONTRIBUTING.md).


Building
--------

RedReader is built using Gradle. On Linux, simply run:

    ./gradlew installDebug

To build an unsigned release APK:

    ./gradlew assembleRelease

A release APK still has to be signed before it can be installed. The APKs
attached to this repository's releases are signed locally with a debug
keystore; they are not suitable for distribution.

The Reddit OAuth client id is read from `src/main/assets/reddit_auth.txt`
(gitignored, copy it from `reddit_auth.placeholder.txt`), or can be set at
runtime under *Settings > Network*.


License
-------

RedReader is licensed under the GPL, version 3. A copy of the license is
included in [LICENSE.txt](LICENSE.txt).

This fork is distributed under the same licence. The original project and its
contributors are the copyright holders of the work they authored; changes made
in this fork are distributed under the same terms.


Thanks
------

A full list of contributors is included in the [changelog](src/main/assets/changelog.txt).
