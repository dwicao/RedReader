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
package org.quantumbadger.redreader.common

import android.animation.ValueAnimator
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.AdapterView
import android.widget.ListAdapter
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.adapters.NoFilterAdapter
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory

object AndroidCommon {
    @JvmField
	val UI_THREAD_HANDLER = Handler(Looper.getMainLooper())

    @JvmStatic
	fun runOnUiThread(runnable: Runnable) {
        if (General.isThisUIThread) {
            runnable.run()
        } else {
            UI_THREAD_HANDLER.post(runnable)
        }
    }

    @JvmStatic
	fun onTextChanged(
        textBox: TextView,
        action: Runnable
    ) {
        textBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: Editable) {
                action.run()
            }
        })
    }

    @JvmStatic
	fun onSelectedItemChanged(
        view: AdapterView<*>,
        action: Runnable
    ) {
        view.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                action.run()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                action.run()
            }
        }
    }

	@Suppress("DEPRECATION")
	@SuppressLint("PackageManagerGetSignatures")
	fun getPackageInfo(context: Context): PackageInfo {

		val name = context.packageName
		val pInfo = context.packageManager.getPackageInfo(name, PackageManager.GET_SIGNATURES)

		return PackageInfo(
			packageName = name,
			versionCode = pInfo.versionCode,
			versionName = pInfo.versionName!!,
			ids = pInfo.signatures!!.map {
				CertificateFactory.getInstance("X509")
					.generateCertificate(ByteArrayInputStream(it.toByteArray())).encoded
			}
		)
	}

	class PackageInfo(
		val packageName: String,
		val versionCode: Int,
		val versionName: String,
		val ids: List<ByteArray>
	)

	@JvmStatic
	fun setAutoCompleteTextViewItemsNoFilter(
		view: MaterialAutoCompleteTextView,
		items: List<String>
	) {
		view.setSimpleItems(items.toTypedArray())
		view.setAdapter(NoFilterAdapter(view.adapter as ListAdapter, items))
	}

	@JvmStatic
	fun setAutoCompleteTextViewItemsNoFilter(
		view: MaterialAutoCompleteTextView,
		items: Array<String>
	) {
		setAutoCompleteTextViewItemsNoFilter(view, items.toList())
	}

	/**
	 * Prompts the user to grant the notification permission, if it is needed.
	 *
	 * @return true if the prompt was shown, false if no prompt was necessary.
	 */
	@JvmStatic
	fun promptForNotificationPermission(
        activity: BaseActivity,
        onDisabled: Runnable? = null
	): Boolean {
		if (Build.VERSION.SDK_INT < 33) {
			return false
		}

		if (!PrefsUtility.pref_behaviour_notifications()) {
			return false
		}

		if (RedditAccountManager.getInstance(activity).defaultAccount.isAnonymous) {
			return false
		}

		if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
				== PackageManager.PERMISSION_GRANTED) {
			return false
		}

		DialogUtils.showDialogPositiveNegative(
			activity,
			activity.getString(R.string.notification_prompt_title),
			activity.getString(R.string.notification_prompt_message),
			R.string.dialog_yes,
			R.string.dialog_no,
			{
				activity.requestPermissionWithCallback(
					Manifest.permission.POST_NOTIFICATIONS,
					object : BaseActivity.PermissionCallback {
						override fun onPermissionGranted() {
							// All good
						}

						override fun onPermissionDenied() {
							activity.startActivity(Intent(
								Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
								Uri.fromParts("package", activity.packageName, null)
							).apply {
								addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
							})
						}

					})
			},
			{
				PrefsUtility.set_pref_behaviour_notifications(false)
				onDisabled?.run()
			}
		)

		return true
	}

	@JvmStatic
	fun removeClickListeners(view: View) {
		view.apply {
			setOnClickListener(null)
			setOnLongClickListener(null)
			isClickable = false
			isLongClickable = false
		}
	}

	/**
	 * If the "Incognito keyboard" preference is enabled, ask the IME not to learn
	 * what's typed into this text view (IME_FLAG_NO_PERSONALIZED_LEARNING).
	 */
	@SuppressLint("InlinedApi")
	@JvmStatic
	fun applyKeyboardPrivacy(textView: TextView) {
		if(PrefsUtility.pref_behaviour_incognito_keyboard()) {
			textView.imeOptions =
				textView.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
		}
	}

    /**
     * Whether the system's animations are disabled (e.g. battery saver or the accessibility
     * "Remove animations" setting). Checked both via the animator service on API 26+ and the
     * developer options' global animator duration scale, as the two can differ between OEM
     * builds. Works on all supported API levels.
     */
    @JvmStatic
    fun areAnimationsDisabled(context: Context): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()) {
            return true
        }

        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f)

        return scale == 0f
    }

    /**
     * Apply the global line spacing preference to a text view.
     */
	@JvmStatic
	fun applyGlobalLineSpacing(textView: TextView) {
		textView.setLineSpacing(0f, PrefsUtility.appearance_linespacing_global())
	}

	/**
	 * Apply a long-click listener, honouring the long-press delay preference:
	 * if the user chose a delay longer than the system default, the menu only
	 * opens after that much time with the finger held still. With the system
	 * default, the normal framework handling is used unchanged.
	 */
	@SuppressLint("ClickableViewAccessibility")
	@JvmStatic
	fun setExtendedLongClickListener(view: View, listener: View.OnLongClickListener) {

		val delayMs = PrefsUtility.pref_accessibility_long_press_delay().toLong()

		if (delayMs <= 0L) {
			view.setOnLongClickListener(listener)
			return
		}

		var downTime = 0L
		var downX = 0f
		var downY = 0f
		var longFired = false

		val handler = UI_THREAD_HANDLER
		val touchSlop = android.view.ViewConfiguration.get(view.context).scaledTouchSlop

		fun fireLongClick() {
			if (longFired) {
				return
			}

			longFired = true
			// Any pending timer returns early via the longFired guard
			listener.onLongClick(view)
		}

		val timer = Runnable { fireLongClick() }

		view.setOnTouchListener { _, event ->

			when (event.actionMasked) {

				android.view.MotionEvent.ACTION_DOWN -> {
					longFired = false
					downTime = SystemClock.uptimeMillis()
					downX = event.x
					downY = event.y
					handler.postDelayed(timer, delayMs)
					false
				}

				android.view.MotionEvent.ACTION_MOVE -> {
					if (Math.abs(event.x - downX) > touchSlop || Math.abs(event.y - downY) > touchSlop) {
						handler.removeCallbacks(timer)
					}
					false
				}

				android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
					handler.removeCallbacks(timer)
					longFired
				}

				else -> false
			}
		}

		// Accessibility actions go through the framework long-click path;
		// honour the same delay there and never double-fire
		view.setOnLongClickListener { v ->
			if (SystemClock.uptimeMillis() - downTime >= delayMs) {
				fireLongClick()
				true
			} else {
				false
			}
		}

		view.isLongClickable = true
	}

	/** Apply the incognito keyboard flag to every EditText in a view hierarchy. */
	@JvmStatic
	fun applyKeyboardPrivacyToTree(view: View) {

		if(!PrefsUtility.pref_behaviour_incognito_keyboard()) {
			return
		}

		if(view is EditText) {
			applyKeyboardPrivacy(view)
		}

		if(view is ViewGroup) {
			for(i in 0 until view.childCount) {
				applyKeyboardPrivacyToTree(view.getChildAt(i))
			}
		}
	}
}
