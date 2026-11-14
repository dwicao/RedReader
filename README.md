RedReader
=========

An unofficial, open source Android client for Reddit.

[![CI](https://github.com/dwicao/RedReader/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/dwicao/RedReader/actions/workflows/build.yml)
[![Translation status](https://hosted.weblate.org/widgets/redreader/-/svg-badge.svg)](https://hosted.weblate.org/engage/redreader/?utm_source=widget)


A new option under Settings -> Appearance -> Post Layout -> Column View (2/3/4)

Screenshot on Samsung Dex (Z Fold 2)
<img width="1920" height="1080" alt="Screenshot_20260810_134348_RedReader" src="https://github.com/user-attachments/assets/de7edc84-4750-4f11-9939-93c0bee334d8" />


On a Phone it will be like this:
<img width="461" height="913" alt="Screenshot_20260810_134607_RedReader" src="https://github.com/user-attachments/assets/fdd821ea-270c-42c4-8c20-d2fa1bbae6df" />

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

**Features**

* **Grid and column views** — switch a listing between list view and a grid of
    **2, 3 or 4 columns** (*Settings > Appearance > Post layout*). Grid cards are
    built for mixed content: they show the image behind a link post, a YouTube
    thumbnail, a crosspost's parent preview, or a video's poster frame, and size
    themselves to the real aspect ratio of whatever they display.
* **Filter posts by content type** — show only image posts, video posts, text
    posts or links, from any listing, including r/all and multi-subreddit
    listings.
* **Custom subreddit list** — keep your own list of subreddits and open it as a
    single combined listing straight from the main menu. Import a multireddit
    such as `a+b+c+askreddit` (or `/r/a+b+c`, or a full URL), long-press a post
    to subscribe or unsubscribe its subreddit directly, and long-press the menu
    entry to edit the list.
* **Favourite posts** — long-press any post and choose Favourite (it replaces
    the redundant View Comments entry), then open everything favourited from the
    Favourite posts entry below Custom Subreddit on the home screen. The list
    offers list, list-with-thumbnails, and 1–4 column grid views, a content-type
    filter (All, Image, Video, Link, Text), the full post subtitle (flair, age,
    author, subreddit, domain), and upvote/comment buttons; long-press a
    favourite to remove it (with confirmation).
* **Auto theme** — a new Auto (follow system) option switches between the light
    Red theme and the Night theme with the system, and the settings, changelog,
    favourites, and terms screens follow the light/dark theme too instead of
    always rendering dark.
* **Manual update check** — *Settings > About > Check for updates* reuses the
    in-app updater and reports whether the app is up to date.
* **Custom DNS** — *Settings > Network* can route the app's traffic through one
    of several well-known DNS providers or a custom DNS URL.
* **In-app updates** — the app checks `version.json` on GitHub, downloads the
    release APK with a progress bar showing MB downloaded out of total MB,
    verifies size and SHA-256, then installs it. CI only publishes a new
    `version.json` after the release APK is actually downloadable.
* **Reddit player links play in-app** — `reddit.com/.../player` links in
    comments resolve via the API (with HTML fallback, and without logging in)
    and play in the built-in player instead of bouncing to an external browser.
* **Inline images and GIFs in comments** — an opt-in setting (*Settings >
    Images/Video > Inline images in comments*) shows images and GIFs attached to
    a comment in its body instead of as a link, similar to the official site.
    GIFs animate while they're on screen (there's an autoplay setting), the NSFW
    and spoiler preview settings also apply, an optional extra setting loads
    Giphy GIFs directly from Giphy's CDN, and the maximum height of an image
    can be capped (or left unlimited) under *Max image height*.
* **Inline images in posts** — the same treatment for a post's self text
    (*Settings > Images/Video > Inline images in posts*).
* **Comment/post image albums** — tapping an inline image opens every image in
    the post or comment together in the image viewer, so you can swipe through
    them.
* **Save and share from long-press** — the long-press menu on an inline image
    offers Save image and Share image, using the actual image URL (not the
    page link).
* **Keyword filter** — *Settings > Behaviour > Hide posts matching these words*
    hides posts whose title, flair or text contain any of the given words
    (comma or newline separated, case-insensitive), like the RES keyword
    filter. Applies to post listings and the Favourites list.
* **Regular expression rules for blocked subreddits** — *Settings > Behaviour >
    Manage blocked subreddits* lets you add subreddit names and regex rules with
    optional case-insensitive matching; the regex rules apply to the same broad
    listings as the blocklist (front page, /r/all, /r/popular).
* **Hide posts with few comments** — *Settings > Behaviour > Hide posts with
    few comments* (off by default) hides posts with fewer comments than a
    minimum you set.
* **Pinned Subreddits menu entry** — the main menu shows a Pinned Subreddits
    entry (when you have pinned subreddits) that opens them as one combined
    listing, and *Settings > Behaviour > Skip to pinned subreddits* starts the
    app there.

**Performance**

* **Image loading** — images are fetched through Picasso over the app's HTTP
    backend (so they honour the Tor and proxy settings) with a persistent disk
    cache, sized requests, a dedicated connection pool, and two-phase loading
    where the small thumbnail paints first and the full image upgrades it.
* **Correctly sized previews** — Reddit's resolution ladder is now preferred over
    the full resolution original, so a small grid card no longer downloads a
    multi-megabyte image. Hosted videos use the still frame Reddit provides
    instead of downloading the entire video to extract one frame.
* **Faster filtered listings** — filtering by content type asks Reddit for more
    posts per request, so far fewer throttled round trips are needed to fill the
    screen while scrolling.
* **Less work per bind and per scroll frame** — a smaller number of allocations
    and re-parses when rows are bound, a preload window that no longer
    recomputes on every scroll event, a scrollbar that no longer forces a layout
    pass each frame, and comment flair observers that no longer accumulate with
    every bind.
* **A cache that stays out of the way** — cache lookups no longer materialise
    every cached session for a URL, a hit resolves its file once instead of
    searching the cache directories again per stream, and cache pruning no
    longer blocks lookups and downloads while it runs.
* **Faster startup to first post** — downloads run at foreground priority, cache
    hits are served from more threads, and bulk precache work uses a bounded
    pool so user-opened media is not stuck behind background feed work.
* **Quicker video start** — reduced prebuffer, bytes reach the player and image
    decoder as they arrive, and buffers are pre-sized from the response length.

**Fixes**

* **Infinite scroll recovers from failures** — a page load that failed with a
    connection error used to leave the list unable to load anything further, and
    the error offered only "Details". The list now re-arms so scrolling near the
    bottom retries the same page, the error gains a Retry button, and stale
    errors clear on the next successful load.
* **Grid view no longer crashes** — marking posts as read on scroll in a grid
    listing threw a `ClassCastException`, and fixed some images were displayed
    at the wrong size.
* **Media is reliable for signed-in accounts** — posts without preview data
    (common in logged-in r/all and r/popular) no longer stall or render blank,
    and video paths and web pages are never fetched as if they were bitmaps.
* Layout and lint fixes for newer Android versions (for example the unpaired
    `layout_marginLeft` in the grid layout), and CI now uploads the release APK
    as an artifact and guards the release build policy so a release cannot
    silently lose minification.
* **Subreddit search returns results** — searching subreddits used to spin
    forever without showing anything; it now loads and displays results.
* **Stale listings recover** — returning to a listing after a long time could
    end in an End of Cached Data error whose Retry button kept failing; retry
    now reloads the listing correctly.
* **Favourites survive backup/restore** — the preferences backup now includes
    the favourites list (backup format v2; older backups still restore).
* **Post-update dialog is current** — the changelog shipped in the app now
    carries this fork's entries, so the "What's new" dialog after an update
    describes the actual changes instead of old upstream notes.
* **About points at the fork** — the source code entry in *Settings > About*
    links to this repository.


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

To verify a fork build's signature, check that the APK's signing certificate
matches the fork's published debug certificate:

```
SHA1: 2B:BE:A0:4E:4D:73:0A:C7:0E:14:A8:86:DF:B0:7B:02:AC:F8:D1:5E
SHA256: B1:10:8B:4A:31:47:2B:C5:7C:84:47:83:97:A4:53:CB:9B:C7:F5:D7:11:5F:EA:F6:D9:D7:1D:9E:F5:9B:97:48
```

(`apksigner verify --print-certs RedReader-release-signed.apk` prints the
signing certificate; its SHA-256 fingerprint is the same value without the
colons, in lowercase.)

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
