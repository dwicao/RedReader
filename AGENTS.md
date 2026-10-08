# AGENTS.md

## Project overview

RedReader is an open-source Android Reddit client (GPLv3), written in a mix of **Java (majority) and Kotlin**, targeting API 21–36. This repo is a **fork**: `origin` = `dwicao/RedReader`, `upstream` = `QuantumBadger/RedReader` (branch `master`). Local history includes merges from upstream plus fork-specific changes (release build policy, retry features) — check `git log` before assuming local divergence.

Dependencies are pinned in a Gradle version catalog: `gradle/libs.versions.toml` (AGP, Kotlin, SDK/NDK versions all live there).

## Essential commands

```sh
./gradlew assembleDebug          # debug APK (CI uploads build/outputs/apk/debug/RedReader-debug.apk)
./gradlew assembleRelease        # unsigned release APK (minify enabled, see policy below)
./gradlew installDebug           # build + install on connected device

# Everything CI runs — must pass before submitting a PR:
./gradlew pmd checkstyle lint test
# (same thing via justfile:)
just precommit

./gradlew connectedCheck         # instrumentation tests (needs device/emulator)
./config/scripts/run_emulator_tests.sh <system-image>   # boots a headless emulator, disables animations, runs check + connectedCheck with retries
```

- Run a single unit test: `./gradlew test --tests '*PostFilterTest*'`
- CI workflow: `.github/workflows/build.yml` (Java 21, runs release+debug builds, pmd, checkstyle, lint, test, in that order).
- `org.gradle.daemon=false` and an 8 GB heap are set in `gradle.properties` — builds are slow by design; don't try to "fix" it by re-enabling anything locally.
- Compilation targets **Java 11** (version catalog), CI *runs* on Java 21. Kotlin 2.2.x, Compose compiler plugin applied at root.

### Local build setup (this device: Termux, aarch64 Android)

- Android SDK lives at `~/android-sdk` with `local.properties` pointing at it (gitignored — recreate if missing: `sdk.dir=/data/data/com.termux/files/home/android-sdk`). Installed: platform-36, build-tools 36.0.0+36.1.0, platform-tools, cmdline-tools 23.
- **`cmdline-tools/bin/sdkmanager` and `bin/android` are x86_64 ELFs and will NOT run on this aarch64 device.** Use the JVM entry point instead (works because the real logic is in `lib/**.jar`):
  ```sh
  SDK=~/android-sdk
  CP=$(find $SDK/cmdline-tools/latest/lib -name '*.jar' | tr '\n' ':')
  yes | java -cp "$CP" com.android.sdklib.tool.sdkmanager.SdkManagerCli \
      --sdk_root=$SDK --install "platforms;android-36" "build-tools;36.0.0"
  ```
- **AGP's Maven-downloaded aapt2 is also x86_64.** `~/.gradle/gradle.properties` (user-level, outside the repo) contains `android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` (Termux's native aapt2 2.20) — without it, `processDebugResources` fails with "Daemon startup failed".
- **All Robolectric tests fail here** with `The Robolectric native runtime is not supported on Linux (aarch64)` — the loader runs unconditionally in `AndroidTestEnvironment.setUpApplicationState` (no opt-out property; `@GraphicsMode(LEGACY)` does not bypass it, and plain JUnit is no alternative because `android.database.Observable.<init>` is stripped from the mockable android.jar, which breaks `RecyclerView.Adapter` construction). Affected: `EdgeToEdgeInsetsTest`, `ScaledBitmapDecoderTest`, `MPDParserTest`, `GroupedRecyclerViewAdapterIndexTest`, `RedditChangeDataManagerTest` (33 methods as of the round-2 perf work) — environmental only; they pass in CI (x86_64 glibc). Don't chase these as regressions; compare against a clean `git stash` run if in doubt.
- Known third-party aarch64 blockers: none remaining for `pmd checkstyle lint test`; `assembleRelease` R8 should work (pure JVM) but hasn't been verified here.

## Release build policy (CI-enforced, do not break)

`.github/workflows/build.yml` greps `build.gradle.kts` and `proguard-rules.pro` and **fails the build** if:
- release `isMinifyEnabled` is not `true`
- release `isShrinkResources` is not `false` (resource shrinking causes crashes; deliberately off)
- `proguard-rules.pro` loses `-dontobfuscate` (obfuscation deliberately off)

`versionCode`/`versionName` are hardcoded in `build.gradle.kts` `defaultConfig` and bumped manually per release.

## Repo layout

- Root project = the Android app; sources at `src/main/java/org/quantumbadger/redreader/` (non-standard: no `app/` module).
- `libs/redreader-common` — pure-JVM Kotlin module (time/date helpers). No Android deps.
- `libs/redreader-datamodel` — **empty placeholder module** (no sources, just build file). Included in `settings.gradle.kts`.
- `src/test/` — JUnit 4 + Robolectric unit tests (package `org.quantumbadger.redreader.test.*`).
- `src/androidTest/` — Espresso UI tests.
- `config/` — static-analysis configs (`lint/`, `checkstyle/`, `pmd/`) and helper scripts (`scripts/`).
- `fastlane/metadata/` — store listing text/images (do not edit English wording, see Contribution rules).

## Static analysis (the trial-and-error minefield)

All four checks are CI-blocking. There is **no ktlint/detekt**; Kotlin is only covered by checkstyle's header check + Android lint.

### Checkstyle (`config/checkstyle/checkstyle.xml`, `./gradlew checkstyle`)
Applies to **`.java` and `.kt`** under `src/main/java/org/quantumbadger`. Notable rules:
- **GPLv3 header required verbatim** on every new `.java` and `.kt` file (copy `config/checkstyle/copyright.java.txt`). XML files also carry it by convention (CONTRIBUTING).
- Lines ≤ 100 chars (`.java` only; URLs/imports exempt).
- Indentation must be **tabs** — a tab followed by a space at line start fails (`^[\t]* `).
- **`FIXME` and `RRDEBUG` literals fail the build** (even in comments; case-insensitive).
- No star imports, **no static imports**, braces required for if/for/while/do/else, `EmptyCatchBlock` only allows catch vars named `expected` or `ignore`.
- `FinalLocalVariable` on `VARIABLE_DEF` and `PARAMETER_DEF`: locals/params that are never reassigned **must** be declared `final`.
- The task is registered as `"Checkstyle"` but invoked as `./gradlew checkstyle` (this is what CONTRIBUTING and CI use).

### PMD (`config/pmd/rules.xml`, `./gradlew pmd`)
- **Java only** (`src/main/java/org/quantumbadger/**/*.java`); Kotlin files are not scanned.
- The `pmd` task `dependsOn assembleDebug` — it's slow because it builds first.
- Custom ruleset (no default ruleset). Common trip-ups: `SystemPrintln`, `UnusedLocalVariable`, `UnusedAssignment`, `PreserveStackTrace` (always rethrow with the cause), `UseTryWithResources`, `MissingOverride`, `AvoidDollarSigns`.
- Suppress inline with `@SuppressWarnings("PMD.RuleName")` — see existing usage in `views/RedditPostView.java`.

### Android Lint (`./gradlew lint`)
- `warningsAsErrors = true`, `abortOnError = true`, `DefaultLocale` escalated to error.
- Suppressions live in `config/lint/lint.xml`; `config/lint/lint-baseline.xml` is **empty** (nothing baselined — keep it that way).
- `checkReleaseBuilds = false`, so lint only runs on demand / in CI.
- CI prints `severity="Error"` entries from `lint-results*.xml` on failure.

### Style (also CONTRIBUTING.md)
- Java: tabs, 100-char lines, match surrounding style. XML: match the file you're editing.
- Formatting-only PRs and rewording existing English strings are explicitly rejected upstream.

## Testing

- Unit tests: JUnit 4 (`org.junit.Test`), **no Mockito** — tests construct real objects. Robolectric for anything touching Android classes; runner configured per class (`@RunWith(RobolectricTestRunner::class)`), `src/test/resources/robolectric.properties` sets the application.
- Test fixtures: JSON/MPD files under `src/test/resources/` (e.g. `reddit/subreddit_rules_askreddit.json`).
- Android instrumentation tests (`src/androidTest/`) are Espresso-based; the emulator scripts wrap runs in retry loops (`config/scripts/retry.sh`, max 5 attempts) because they're flaky — a failure once doesn't necessarily mean broken code, but fix real assertion failures.
- New unit tests are welcomed (CONTRIBUTING); `test` runs as part of the CI gate.

## Architecture

### Data flow: Reddit JSON → screen
1. **Raw models** — `reddit/kthings/*.kt` (kotlinx.serialization `@Serializable`: `RedditPost`, `RedditComment`, `RedditThing`…) parsed via `kthings/JsonUtils.kt` (`ignoreUnknownKeys`, lenient). Legacy `reddit/things/` + `jsonwrap/` (Jackson streaming + reflection) still used for subreddits/users — prefer kthings for new listing types.
2. **Parsed** — `reddit/prepared/RedditParsedPost.kt` / `RedditParsedComment`: derived fields; self-text HTML → `bodytext/BodyElement` tree via `html/HtmlReader` + `prepared/markdown/MarkdownParser`.
3. **Prepared** — `reddit/prepared/RedditPreparedPost.java`: per-user state (read/vote/save/hide, thumbnails, a11y strings), binds to the view; `RedditRenderableComment` = flattened collapse-aware comment row.
4. **View** — `views/RedditPostView.java` (row) inside `adapters/GroupedRecyclerViewAdapter`.

### Listings UI pattern
- Activities extend `RefreshableActivity` (`doRefresh(...)`, `requestRefresh()`); they parse the Intent URI via `reddit/url/RedditURLParser` and own a `listingcontrollers/PostListingController` (or `CommentListingController`) which holds `URL + session UUID + PostFilter` and news up the fragment.
- **`fragments/RRFragment.java` is NOT an androidx Fragment** — it's a custom class that pushes its views directly into the activity (`setBaseActivityContent`). No FragmentManager except for dialogs.
- `adapters/RedditListingManager` (abstract) drives a `GroupedRecyclerViewAdapter` with fixed groups (header, items, load-more, loading, footer errors); position math handles `isHidden()` items and a "preload window" (visible ±5) for releasing expensive resources. `getViewType()` returns a **Class** used as the view-type key.
- List rendering/scroll: `views/ScrollbarRecyclerViewManager`.

### Networking/caching (everything goes through here)
- Build a `cache/CacheRequest` (url, account, `Priority`, `DownloadStrategy`, `DownloadQueueType`, callbacks) → `CacheManager.getInstance(ctx).makeRequest(...)`.
- Single handler thread consults the SQLite cache index (`CacheDbManager`, keyed by url+user+session) and the `DownloadStrategy` (always / if-not-cached / timestamp-bounds / never) → cache hit streams from disk, miss queues a download.
- **`DownloadQueueType.REDDIT_API` is throttled to one request per 1200 ms** (Reddit rate limit) inside `PrioritisedDownloadQueue` — don't bypass it.
- OAuth: `reddit/api/RedditOAuth.kt` (auth-code flow, redirect `redreader://rr_oauth_redir`, caught by `LinkDispatchActivity`). Token fetch/refresh happens inside `cache/CacheDownload` per request, not in `RedditAPI`. Client ID: build-time `src/main/assets/reddit_auth.txt` (gitignored; copy from `reddit_auth.placeholder.txt`) or runtime `Settings > Network > Reddit client ID override`. **Never commit the client ID file.**
- HTTP backend: `http/HTTPBackend.kt` → `http/okhttp/OKHTTPBackend.kt` (OkHttp).

### Sessions
A "session" = `UUID` per downloaded snapshot of a URL. Refresh selects a new session; `SessionListDialog` lets users view older cached copies. Listing activities implement `SessionChangeListener` and are re-driven on `onSessionChanged`.

### Routing & actions
- All link clicks go through `common/LinkHandler.kt` → `RedditURLParser.parse(Uri)` → typed `RedditURL` subclasses (`SubredditPostListURL`, `PostCommentListingURL`, …) → activity dispatch.
- Vote/save/hide: `reddit/api/RedditPostActions.kt` applies an **optimistic** update via `reddit/prepared/RedditChangeDataManager` (per-account singleton, persisted to `rr_change_data.dat` by `io/RedditChangeDataIO`), posts via `RedditAPI`, and reverts on failure.

### Preferences
- `common/PrefsUtility.java` — static facade, one getter per setting; `isReLayoutRequired/isRefreshRequired/isRestartRequired(key)` tell activities how to react to changes.
- Compose reads the same SharedPreferences via `compose/prefs/ComposePrefs.kt`.

### Compose vs Views
Compose is a **parallel, incremental layer**, not a rewrite. Only two screens are Compose so far: `activities/AlbumListingActivity.kt` (`compose/ui/AlbumScreen.kt`) and `fragments/ReportDialog.kt` (`compose/ui/ReportScreen.kt`). Everything else (listings, comments, settings, image viewer) is legacy Views. `compose/net/NetWrapper.kt` bridges Compose to the legacy `CacheManager`/`CacheRequest` stack — reuse it for new Compose networking instead of calling OkHttp directly. Context/theme via `compose/ctx/RRComposeContext.kt`.

## Gotchas

- **Manifest**: almost every activity declares `configChanges="orientation|screenSize|keyboardHidden"` — activities/fragments handle config changes manually. `LinkDispatchActivity` handles all deep links *and* the OAuth redirect.
- Application class `RedReader.kt` does heavy init (prefs, OAuth, cache prune, change-data read) on startup — order matters, don't casually reorder.
- `libs/redreader-datamodel` has no source files; adding files there is fine but don't expect anything to exist.
- `src/main/.../compose/ctx/RRComposeContextTest.kt` sits in **main**, not test — odd but intentional-looking; don't "fix" without checking.
- Alpha builds (`config/scripts/build_alpha.sh`) rewrite the package name to `...redreaderalpha` via sed — those scripts assume the maintainer's build machine; not runnable locally.
- Translations: **never edit `res/values-*/strings.xml` or reword English strings** — translations go through Weblate; wording changes are rejected in PRs (CONTRIBUTING).
- Lint ignores are deliberate and documented in `config/lint/lint.xml` (e.g. `GestureBackNavigation` — app uses legacy `onBackPressed()`, predictive back not opted in).
- `-Xlint:deprecation`/`-Xlint:unchecked` are enabled for javac; keep new Java code free of raw types/unchecked warnings.

## Contribution rules (from CONTRIBUTING.md)

Accepted: new features, bug fixes, significant perf improvements, new unit tests.
Rejected: translations via PR, English rewording, formatting-only changes, micro-optimisations without benchmarks.
Before submitting: `./gradlew pmd checkstyle lint test` must pass.
