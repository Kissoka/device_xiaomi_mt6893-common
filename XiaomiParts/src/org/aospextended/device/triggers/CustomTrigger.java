package org.aospextended.device.triggers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.KeyEvent;
import android.widget.Switch;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;
import com.android.internal.os.DeviceKeyHandler;

import org.aospextended.device.util.Action;
import org.aospextended.device.util.Utils;
import org.aospextended.device.KeyHandler;

import org.aospextended.device.R;
import org.aospextended.device.util.ShortcutPickerHelper;

public class CustomTrigger extends PreferenceFragment implements
        OnPreferenceChangeListener, OnMainSwitchChangeListener,
	OnPreferenceClickListener, ShortcutPickerHelper.OnPickListener {

    private static final String TAG = "CustomTrigger";

    private static final String SETTINGS_METADATA_NAME = "com.android.settings";

    public static final String PREF_CUSTOM_TRIGGER_ENABLE = "custom_trigger_enable";
    public static final String PREF_LEFT_TRIGGER = "custom_left_trigger";
    public static final String PREF_RIGHT_TRIGGER = "custom_right_trigger";
    public static final String KEY_TRIGGER_HAPTIC_FEEDBACK = "custom_trigger_haptic_feedback";

    private static final int DLG_SHOW_ACTION_DIALOG  = 0;
    private static final int DLG_RESET_TO_DEFAULT    = 1;

    private static final int MENU_RESET = Menu.FIRST;

    private MainSwitchPreference mEnableCustomTrigger;

    private Preference mLeftTrigger;
    private Preference mRightTrigger;

    private SwitchPreference mHapticFeedback;

    private boolean mCheckPreferences;
    private SharedPreferences mPrefs;

    private ShortcutPickerHelper mPicker;
    private String mPendingkey;

    private String[] mActionEntries;
    private String[] mActionValues;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        mPicker = new ShortcutPickerHelper(getActivity(), this);

        mPrefs = Utils.getSharedPreferences(getActivity());

        mActionValues = getResources().getStringArray(R.array.action_screen_off_values);
        mActionEntries = getResources().getStringArray(R.array.action_screen_off_entries);

        initPrefs();

        setHasOptionsMenu(true);
    }

    private PreferenceScreen initPrefs() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        addPreferencesFromResource(R.xml.custom_trigger);

        prefs = getPreferenceScreen();

	boolean enableCustomTrigger = mPrefs.getBoolean(PREF_CUSTOM_TRIGGER_ENABLE, true);

	mEnableCustomTrigger = (MainSwitchPreference) findPreference(PREF_CUSTOM_TRIGGER_ENABLE);
        mEnableCustomTrigger.addOnSwitchChangeListener(this);
        mEnableCustomTrigger.setChecked(enableCustomTrigger);

	mLeftTrigger = (Preference) prefs.findPreference(PREF_LEFT_TRIGGER);
	mRightTrigger = (Preference) prefs.findPreference(PREF_RIGHT_TRIGGER);

        PreferenceCategory haptic = (PreferenceCategory) prefs.findPreference("haptic");
        mHapticFeedback = (SwitchPreference) findPreference(KEY_TRIGGER_HAPTIC_FEEDBACK);
	mHapticFeedback.setEnabled(enableCustomTrigger);
        mHapticFeedback.setChecked(mPrefs.getInt(Utils.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, 1) != 0);
        mHapticFeedback.setOnPreferenceChangeListener(this);

	setPref(mLeftTrigger, mPrefs.getString(PREF_LEFT_TRIGGER,
                Action.ACTION_WAKE_DEVICE));
	setPref(mRightTrigger, mPrefs.getString(PREF_RIGHT_TRIGGER,
		Action.ACTION_WAKE_DEVICE));

        return prefs;
    }

    private void setPref(Preference preference, String action) {
        if (preference == null || action == null) {
            return;
        }
        preference.setSummary(getDescription(action));
        preference.setOnPreferenceClickListener(this);
    }

    private String getDescription(String action) {
        if (action == null) {
            return null;
        }
        int i = 0;
        for (String val : mActionValues) {
            if (action.equals(val)) {
                return mActionEntries[i];
            }
            i++;
        }
        return null;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = null;
        int title = 0;
	if (preference == mLeftTrigger) {
            key = PREF_LEFT_TRIGGER;
            title = R.string.custom_left_trigger_title;
        } else if (preference == mRightTrigger) {
            key = PREF_RIGHT_TRIGGER;
            title = R.string.custom_right_trigger_title;
        }
	if (key != null) {
            showDialogInner(DLG_SHOW_ACTION_DIALOG, key, title);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (KEY_TRIGGER_HAPTIC_FEEDBACK.equals(key)) {
                final boolean value = (boolean) newValue;
                mPrefs.edit().putInt(Utils.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, value ? 1 : 0).commit();
                return true;
        }
        return false;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        mEnableCustomTrigger.setChecked(isChecked);
    }

/*  TODO:
    * Implement an method to detect doubleclick from trigger buttons.
    * Then Assign the given task to action.
    * Guard this functionality while game in use.

    public boolean onKeyDown(int keyCode, KeyEvent event) {

	switch (keyCode){
	case 131:
		// Assigning temp action for test
		action = mPrefs.getString(PREF_LEFT_TRIGGER,
				Action.ACTION_TORCH);
            break;

        case 132:
		// Assigning temp action for test
		action = mPrefs.getString(PREF_RIGHT_TRIGGER,
				Action.ACTION_VIB_SILENT);
	    break;
	}

	if (action == null || action.equals(Action.ACTION_NULL)) return false;

	if (action.equals(Action.ACTION_CAMERA)) {
		Action.processAction(mContext, Action.ACTION_WAKE_DEVICE, false);
	}
	Action.processAction(mContext, action, false);

	return true;
    } */

    // Reset all entries to default.
    private void resetToDefault() {
        SharedPreferences.Editor editor = mPrefs.edit();

        mPrefs.edit()
                .putBoolean(PREF_CUSTOM_TRIGGER_ENABLE, true).commit();

	editor.putString(PREF_LEFT_TRIGGER,
                Action.ACTION_NULL).commit();
	editor.putString(PREF_RIGHT_TRIGGER,
                Action.ACTION_NULL).commit();

	mHapticFeedback.setChecked(true);
        editor.commit();
	initPrefs();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void shortcutPicked(String action,
                String description, Bitmap bmp, boolean isApplication) {
        if (mPendingkey == null || action == null) {
            return;
        }
        mPrefs.edit().putString(mPendingkey, action).commit();
        initPrefs();
        mPendingkey = null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);

            }
        } else {
            mPendingkey = null;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                    showDialogInner(DLG_RESET_TO_DEFAULT, null, 0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    private void showDialogInner(int id, String key, int title) {
        DialogFragment newFragment =
                MyAlertDialogFragment.newInstance(id, key, title);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(
                int id, String key, int title) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putString("key", key);
            args.putInt("title", title);
            frag.setArguments(args);
            return frag;
        }

        CustomTrigger getOwner() {
            return (CustomTrigger) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            final String key = getArguments().getString("key");
            int title = getArguments().getInt("title");
            switch (id) {
                case DLG_SHOW_ACTION_DIALOG:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(title)
                    .setNegativeButton(R.string.cancel, null)
                    .setItems(getOwner().mActionEntries,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            if (getOwner().mActionValues[item]
                                    .equals(Action.ACTION_APP)) {
                                if (getOwner().mPicker != null) {
                                    getOwner().mPendingkey = key;
                                    getOwner().mPicker.pickShortcut(getOwner().getId());
                                }
                            } else {
                                getOwner().mPrefs.edit()
                                        .putString(key,
                                        getOwner().mActionValues[item]).commit();
                                getOwner().initPrefs();
                            }
                        }
                    })
                    .create();
                case DLG_RESET_TO_DEFAULT:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().resetToDefault();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
        }
    }

}
