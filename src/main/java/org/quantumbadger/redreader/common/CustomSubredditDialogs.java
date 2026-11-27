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

package org.quantumbadger.redreader.common;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.adapters.MainMenuSelectionListener;
import org.quantumbadger.redreader.reddit.url.SubredditPostListURL;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Dialogs for creating, editing, deleting, picking, and assigning subreddits
// to multiple custom subreddit lists.
public final class CustomSubredditDialogs {

	public interface OnEntrySavedListener {
		void onEntrySaved(@NonNull CustomSubreddits.Entry entry);
	}

	private CustomSubredditDialogs() {
	}

	public static void showCreateDialog(
			@NonNull final AppCompatActivity activity,
			@Nullable final String initialSubreddits,
			@Nullable final OnEntrySavedListener listener) {
		showCreateOrEditDialog(activity, null, initialSubreddits, listener);
	}

	public static void showEditDialog(
			@NonNull final AppCompatActivity activity,
			@NonNull final CustomSubreddits.Entry entry,
			@Nullable final OnEntrySavedListener listener) {
		showCreateOrEditDialog(activity, entry, null, listener);
	}

	private static void showCreateOrEditDialog(
			@NonNull final AppCompatActivity activity,
			@Nullable final CustomSubreddits.Entry existing,
			@Nullable final String initialSubreddits,
			@Nullable final OnEntrySavedListener listener) {

		final EditText nameInput = new EditText(activity);
		AndroidCommon.applyKeyboardPrivacy(nameInput);
		nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
		nameInput.setSingleLine(true);
		nameInput.setHint(R.string.custom_subreddits_name_hint);
		if(existing != null) {
			nameInput.setText(existing.name);
		}

		final EditText subredditsInput = new EditText(activity);
		AndroidCommon.applyKeyboardPrivacy(subredditsInput);
		subredditsInput.setInputType(
				InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		subredditsInput.setMinLines(3);
		subredditsInput.setHint(R.string.custom_subreddits_list_hint);
		if(existing != null) {
			subredditsInput.setText(existing.subreddits);
		} else if(initialSubreddits != null) {
			subredditsInput.setText(initialSubreddits);
		}

		final LinearLayout container = new LinearLayout(activity);
		container.setOrientation(LinearLayout.VERTICAL);
		final int hPad = General.dpToPixels(activity, 20);
		final int vPad = General.dpToPixels(activity, 10);
		container.setPadding(hPad, vPad, hPad, vPad);
		container.addView(nameInput);
		General.setLayoutMatchWidthWrapHeight(nameInput);
		container.addView(subredditsInput);
		General.setLayoutMatchWidthWrapHeight(subredditsInput);

		final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
				.setTitle(existing == null
						? R.string.custom_subreddits_add
						: R.string.custom_subreddits_edit)
				.setView(container)
				.setPositiveButton(R.string.action_save, null)
				.setNegativeButton(R.string.dialog_cancel, null);

		if(existing != null) {
			builder.setNeutralButton(
					R.string.action_delete,
					(dialog, which) -> showDeleteConfirmDialog(activity, existing));
		}

		final AlertDialog dialog = builder.create();
		if(dialog.getWindow() != null) {
			dialog.getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
							| WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}
		dialog.show();

		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
			final String raw = subredditsInput.getText().toString();
			if(raw.trim().isEmpty()) {
				General.quickToast(activity, R.string.custom_subreddits_empty_list);
				return;
			}
			final String normalized = CustomSubreddits.normalize(raw);
			if(normalized == null) {
				General.quickToast(activity, R.string.mainmenu_custom_invalid_name);
				return;
			}
			final String name = nameInput.getText().toString();
			final CustomSubreddits.Entry saved;
			if(existing == null) {
				saved = CustomSubreddits.create(activity, name, normalized);
			} else {
				CustomSubreddits.update(activity, existing.id, name, normalized);
				final CustomSubreddits.Entry reloaded
						= CustomSubreddits.getById(activity, existing.id);
				saved = reloaded != null
						? reloaded
						: new CustomSubreddits.Entry(
								existing.id,
								name.trim(),
								normalized);
			}
			dialog.dismiss();
			if(saved != null && listener != null) {
				listener.onEntrySaved(saved);
			}
		});
	}

	public static void showListPickerDialog(
			@NonNull final AppCompatActivity activity,
			@NonNull final MainMenuSelectionListener listener) {
		final List<CustomSubreddits.Entry> entries = CustomSubreddits.getAll(activity);
		if(entries.isEmpty()) {
			showCreateDialog(activity, null, entry -> listener.onSelected(
					SubredditPostListURL.getCombination(entry.subreddits)));
			return;
		}

		final String[] labels = new String[entries.size()];
		for(int i = 0; i < entries.size(); i++) {
			final CustomSubreddits.Entry entry = entries.get(i);
			if(entry.name.isEmpty()) {
				labels[i] = entry.subreddits;
			} else {
				labels[i] = entry.name + " (" + entry.subreddits + ")";
			}
		}

		final AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
				.setTitle(R.string.mainmenu_custom_subreddits)
				.setItems(labels, (d, which) -> listener.onSelected(
						SubredditPostListURL.getCombination(
								entries.get(which).subreddits)))
				.setNeutralButton(
						R.string.custom_subreddits_add,
						(d, which) -> showCreateDialog(activity, null, entry -> listener.onSelected(
								SubredditPostListURL.getCombination(entry.subreddits))))
				.setNegativeButton(R.string.dialog_close, null)
				.create();

		dialog.setOnShowListener(d -> dialog.getListView().setOnItemLongClickListener(
				(parent, view, position, id) -> {
					dialog.dismiss();
					showItemActionMenu(activity, entries.get(position));
					return true;
				}));

		dialog.show();
	}

	public static void showItemActionMenu(
			@NonNull final AppCompatActivity activity,
			@NonNull final CustomSubreddits.Entry entry) {
		final String[] items = {
				activity.getString(R.string.action_edit),
				activity.getString(R.string.action_copy_link),
				activity.getString(R.string.action_share),
				activity.getString(R.string.action_delete)
		};

		final UriString url = Constants.Reddit.getNonAPIUri("/r/" + entry.subreddits);

		final AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
				.setTitle(entry.displayName())
				.setItems(items, (d, which) -> {
					switch(which) {
						case 0:
							showEditDialog(activity, entry, null);
							break;
						case 1: {
							final ClipboardManager clipboardManager
									= (ClipboardManager)activity.getSystemService(
											Context.CLIPBOARD_SERVICE);
							if(clipboardManager != null) {
								clipboardManager.setPrimaryClip(
										ClipData.newPlainText(null, url.value));
								General.quickToast(
										activity.getApplicationContext(),
										R.string.subreddit_link_copied_to_clipboard);
							}
							break;
						}
						case 2:
							LinkHandler.shareText(
									activity,
									entry.displayName(),
									LinkHandler.getPreferredRedditUriString(url)
											.toString());
							break;
						case 3:
							showDeleteConfirmDialog(activity, entry);
							break;
						default:
							break;
					}
				})
				.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}

	private static void showDeleteConfirmDialog(
			@NonNull final AppCompatActivity activity,
			@NonNull final CustomSubreddits.Entry entry) {
		DialogUtils.showDialogPositiveNegative(
				activity,
				activity.getString(R.string.action_delete),
				activity.getString(
						R.string.custom_subreddits_delete_confirm_message,
						entry.displayName()),
				R.string.action_delete,
				R.string.dialog_cancel,
				() -> CustomSubreddits.delete(activity, entry.id),
				() -> {});
	}

	public static void showSubredditMembershipDialog(
			@NonNull final AppCompatActivity activity,
			@NonNull final String subreddit) {
		final List<CustomSubreddits.Entry> entries = CustomSubreddits.getAll(activity);
		final String normalizedSub = CustomSubreddits.normalizeSingle(subreddit);
		if(normalizedSub == null) {
			General.quickToast(activity, R.string.mainmenu_custom_invalid_name);
			return;
		}
		if(entries.isEmpty()) {
			showCreateDialog(activity, normalizedSub, null);
			return;
		}

		final CharSequence[] labels = new CharSequence[entries.size()];
		final boolean[] checked = new boolean[entries.size()];
		for(int i = 0; i < entries.size(); i++) {
			final CustomSubreddits.Entry entry = entries.get(i);
			labels[i] = entry.displayName();
			checked[i] = entry.contains(normalizedSub);
		}

		new MaterialAlertDialogBuilder(activity)
				.setTitle(R.string.mainmenu_custom_subreddits)
				.setMultiChoiceItems(
						labels,
						checked,
						(dialog, which, isChecked) -> checked[which] = isChecked)
				.setPositiveButton(R.string.action_save, (dialog, which) -> {
					final Set<String> targetIds = new HashSet<>();
					for(int i = 0; i < entries.size(); i++) {
						if(checked[i]) {
							targetIds.add(entries.get(i).id);
						}
					}
					CustomSubreddits.setMembership(activity, normalizedSub, targetIds);
					General.quickToast(activity, R.string.custom_subreddits_updated);
				})
				.setNeutralButton(
						R.string.custom_subreddits_add,
						(dialog, which) -> showCreateDialog(activity, normalizedSub, null))
				.setNegativeButton(R.string.dialog_cancel, null)
				.show();
	}
}
