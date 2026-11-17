/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.test.general;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Locks down the mapping from a preference key to the action an activity must
 * take when it changes (re-layout, refresh, restart).
 *
 * <p>These lists used to be hand-written chains of {@code equals()} calls with
 * the appearance family matched by a bare string literal. They are now declared
 * as tables of string resource ids, and all {@code pref_appearance_*} keys are
 * matched by prefix. This test pins the resulting behaviour so a future edit
 * cannot silently stop a setting from taking effect.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class PrefReactionTest {

	private static Context context() {
		return RuntimeEnvironment.getApplication();
	}

	private static String key(final int stringResource) {
		return context().getString(stringResource);
	}

	@Test
	public void appearanceKeysAlwaysRequireRefresh() {
		final Context context = context();

		// The appearance family is matched by prefix, so every member must
		// refresh -- including the ones that used to be listed individually.
		final int[] appearanceKeys = {
				R.string.pref_appearance_theme_key,
				R.string.pref_appearance_twopane_key,
				R.string.pref_appearance_hide_headertoolbar_commentlist_key,
				R.string.pref_appearance_hide_headertoolbar_postlist_key,
				R.string.pref_appearance_hide_comments_from_blocked_users_key,
				R.string.pref_appearance_indentlines_colorful_key,
				R.string.pref_appearance_highlight_own_username_key,
				R.string.pref_appearance_post_hide_subreddit_header_key,
		};

		for(final int appearanceKey : appearanceKeys) {
			Assert.assertTrue(
					"appearance key should refresh: " + context.getString(appearanceKey),
					PrefsUtility.isRefreshRequired(context, context.getString(appearanceKey)));
		}
	}

	@Test
	public void nonAppearanceRefreshKeysStillRefresh() {
		final Context context = context();

		final int[] refreshKeys = {
				R.string.pref_behaviour_nsfw_key,
				R.string.pref_behaviour_keyword_filter_key,
				R.string.pref_behaviour_domain_filter_key,
				R.string.pref_images_thumbnail_size_key,
				R.string.pref_images_high_res_thumbnails_key,
				R.string.pref_accessibility_concise_mode_key,
				R.string.pref_reddit_client_id_override_key,
		};

		for(final int refreshKey : refreshKeys) {
			Assert.assertTrue(
					"key should refresh: " + context.getString(refreshKey),
					PrefsUtility.isRefreshRequired(context, context.getString(refreshKey)));
		}
	}

	@Test
	public void relayoutKeysRequireRelayout() {
		final Context context = context();

		Assert.assertTrue(PrefsUtility.isReLayoutRequired(
				context, key(R.string.pref_appearance_theme_key)));
		Assert.assertTrue(PrefsUtility.isReLayoutRequired(
				context, key(R.string.pref_menus_mainmenu_useritems_key)));
		Assert.assertTrue(PrefsUtility.isReLayoutRequired(
				context, key(R.string.pref_menus_mainmenu_shortcutitems_key)));

		Assert.assertFalse(PrefsUtility.isReLayoutRequired(
				context, key(R.string.pref_behaviour_nsfw_key)));
	}

	@Test
	public void restartKeysRequireRestart() {
		final Context context = context();

		Assert.assertTrue(PrefsUtility.isRestartRequired(
				context, key(R.string.pref_appearance_twopane_key)));
		Assert.assertTrue(PrefsUtility.isRestartRequired(
				context, key(R.string.pref_appearance_navbar_color_key)));
		Assert.assertTrue(PrefsUtility.isRestartRequired(
				context, key(R.string.pref_behaviour_keep_screen_awake_key)));

		Assert.assertFalse(PrefsUtility.isRestartRequired(
				context, key(R.string.pref_behaviour_nsfw_key)));
	}

	@Test
	public void aKeyCanRequireMoreThanOneReaction() {
		final Context context = context();
		final String themeKey = key(R.string.pref_appearance_theme_key);

		// The theme affects layout, needs a refresh, and only fully applies after
		// a restart -- this is the reason the three lists cannot be exclusive.
		Assert.assertTrue(PrefsUtility.isReLayoutRequired(context, themeKey));
		Assert.assertTrue(PrefsUtility.isRefreshRequired(context, themeKey));
		Assert.assertTrue(PrefsUtility.isRestartRequired(context, themeKey));
	}

	@Test
	public void unknownKeyRequiresNothing() {
		final Context context = context();
		final String unknown = "pref_not_a_real_key";

		Assert.assertFalse(PrefsUtility.isReLayoutRequired(context, unknown));
		Assert.assertFalse(PrefsUtility.isRefreshRequired(context, unknown));
		Assert.assertFalse(PrefsUtility.isRestartRequired(context, unknown));
	}
}
