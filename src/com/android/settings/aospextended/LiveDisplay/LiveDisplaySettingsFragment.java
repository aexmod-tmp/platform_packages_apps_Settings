package com.android.settings.aospextended.LiveDisplay;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.util.ArrayUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import android.provider.Settings;

import com.github.aexmod.livedisplay.LiveDisplayConfig;
import com.github.aexmod.livedisplay.LiveDisplayManager;
import com.github.aexmod.livedisplay.LineageHardwareManager;
import com.github.aexmod.livedisplay.DisplayMode;
import com.android.internal.util.lineageos.SettingsHelper; 

import com.android.internal.logging.nano.MetricsProto;

import static com.github.aexmod.livedisplay.LiveDisplayManager.FEATURE_DISPLAY_MODES;
import static com.github.aexmod.livedisplay.LiveDisplayManager.MODE_OUTDOOR;
import static com.github.aexmod.livedisplay.LiveDisplayManager.MODE_OFF;
import static com.github.aexmod.livedisplay.LiveDisplayManager.FEATURE_PICTURE_ADJUSTMENT;
import static com.github.aexmod.livedisplay.LiveDisplayManager.FEATURE_COLOR_ENHANCEMENT;
import static com.github.aexmod.livedisplay.LiveDisplayManager.FEATURE_COLOR_ADJUSTMENT;
import static com.github.aexmod.livedisplay.LiveDisplayManager.FEATURE_CABC;

public class LiveDisplaySettingsFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, SettingsHelper.OnSettingsChangeListener {

	private static final String KEY_CATEGORY_LIVE_DISPLAY = "live_display_options";
    private static final String KEY_CATEGORY_ADVANCED = "advanced";
    private static final String KEY_DISPLAY_COLOR = "color_calibration";
    private static final String KEY_PICTURE_ADJUSTMENT = "picture_adjustment";
    private static final String KEY_LIVE_DISPLAY = "live_display";
    private static final String KEY_LIVE_DISPLAY_COLOR_ENHANCE = "display_color_enhance";
    private static final String KEY_LIVE_DISPLAY_LOW_POWER = "display_low_power";
    private static final String KEY_LIVE_DISPLAY_READING_ENHANCEMENT = "display_reading_mode";
    private static final String KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE = "display_auto_outdoor_mode";
    private static final String KEY_LIVE_DISPLAY_TEMPERATURE = "live_display_color_temperature";
    private static final String KEY_LIVE_DISPLAY_COLOR_PROFILE = "live_display_color_profile";


    private static final String COLOR_PROFILE_TITLE = KEY_LIVE_DISPLAY_COLOR_PROFILE + "_%s_title";

    private static final String COLOR_PROFILE_SUMMARY = KEY_LIVE_DISPLAY_COLOR_PROFILE + "_%s_summary";

    private final Uri DISPLAY_TEMPERATURE_DAY_URI =
            Settings.System.getUriFor(Settings.System.DISPLAY_TEMPERATURE_DAY);
    private final Uri DISPLAY_TEMPERATURE_NIGHT_URI =
            Settings.System.getUriFor(Settings.System.DISPLAY_TEMPERATURE_NIGHT);
    private final Uri DISPLAY_TEMPERATURE_MODE_URI =
            Settings.System.getUriFor(Settings.System.DISPLAY_TEMPERATURE_MODE);

    private ListPreference mLiveDisplay;

    private String[] mModeEntries;
    private String[] mModeValues;
    private String[] mModeSummaries;
    private String[] mColorProfileSummaries;

    private PictureAdjustment mPictureAdjustment;
    private DisplayTemperature mDisplayTemperature;
    private DisplayColor mDisplayColor;

    private SwitchPreference mColorEnhancement;
    private SwitchPreference mLowPower;
    private SwitchPreference mOutdoorMode;
    private SwitchPreference mReadingMode;
    private ListPreference mColorProfile;

    private boolean mHasDisplayModes = false;

    private LiveDisplayManager mLiveDisplayManager;
    private LiveDisplayConfig mConfig;

    private LineageHardwareManager mHardware;

	@Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SYSTEM_PROFILES;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        final Resources res = getResources();

        mHardware = LineageHardwareManager.getInstance(getActivity());
        mLiveDisplayManager = LiveDisplayManager.getInstance(getActivity());
        mConfig = mLiveDisplayManager.getConfig();

        Log.d("LiveDisplay", mConfig.toString());
        if(mHardware == null){
            Log.d("LiveDisplay", "LineageHardwareManager null");
        }

        addPreferencesFromResource(R.xml.livedisplay);

		PreferenceCategory liveDisplayPrefs = (PreferenceCategory) findPreference(KEY_CATEGORY_LIVE_DISPLAY);
        PreferenceCategory advancedPrefs = (PreferenceCategory) findPreference(KEY_CATEGORY_ADVANCED);

        int adaptiveMode = mLiveDisplayManager.getMode();
        if(mHardware == null){
            Log.d("LiveDisplay", "adaptiveMode "+adaptiveMode);
        }

        mLiveDisplay = (ListPreference) findPreference(KEY_LIVE_DISPLAY);
        mLiveDisplay.setValue(String.valueOf(adaptiveMode));

        mModeEntries = res.getStringArray(R.array.live_display_entries);
        mModeValues = res.getStringArray(R.array.live_display_values);
        mModeSummaries = res.getStringArray(R.array.live_display_summaries);

        // Remove outdoor mode from lists if there is no support
        if (!mConfig.hasFeature(LiveDisplayManager.MODE_OUTDOOR)) {
            Log.d("LiveDisplay", "MODE_OUTDOOR false");
            int idx = ArrayUtils.indexOf(mModeValues, String.valueOf(MODE_OUTDOOR));
            String[] entriesTemp = new String[mModeEntries.length - 1];
            String[] valuesTemp = new String[mModeValues.length - 1];
            String[] summariesTemp = new String[mModeSummaries.length - 1];
            int j = 0;
            for (int i = 0; i < mModeEntries.length; i++) {
                if (i == idx) {
                    continue;
                }
                entriesTemp[j] = mModeEntries[i];
                valuesTemp[j] = mModeValues[i];
                summariesTemp[j] = mModeSummaries[i];
                j++;
            }
            mModeEntries = entriesTemp;
            mModeValues = valuesTemp;
            mModeSummaries = summariesTemp;
        }

        mLiveDisplay.setEntries(mModeEntries);
        mLiveDisplay.setEntryValues(mModeValues);
        mLiveDisplay.setOnPreferenceChangeListener(this);

        mDisplayTemperature = (DisplayTemperature) findPreference(KEY_LIVE_DISPLAY_TEMPERATURE);

        mColorProfile = (ListPreference) findPreference(KEY_LIVE_DISPLAY_COLOR_PROFILE);
        if (liveDisplayPrefs != null && mColorProfile != null
                && (!mConfig.hasFeature(FEATURE_DISPLAY_MODES) || !updateDisplayModes())) {
            liveDisplayPrefs.removePreference(mColorProfile);
        Log.d("LiveDisplay", "FEATURE_DISPLAY_MODES false");
        } else {
            mHasDisplayModes = true;
            mColorProfile.setOnPreferenceChangeListener(this);
        }

        mOutdoorMode = (SwitchPreference) findPreference(KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE);
        if (liveDisplayPrefs != null && mOutdoorMode != null
                && !mConfig.hasFeature(MODE_OUTDOOR)) {
            Log.d("LiveDisplay", "MODE_OUTDOOR false");
            liveDisplayPrefs.removePreference(mOutdoorMode);
            mOutdoorMode = null;
        }

        mReadingMode = (SwitchPreference) findPreference(KEY_LIVE_DISPLAY_READING_ENHANCEMENT);
        if (liveDisplayPrefs != null && mReadingMode != null
                && !mHardware.isSupported(LineageHardwareManager.FEATURE_READING_ENHANCEMENT)) {
            liveDisplayPrefs.removePreference(mReadingMode);
            mReadingMode = null;
            Log.d("LiveDisplay", "FEATURE_READING_ENHANCEMENT false");
        } else {
            mReadingMode.setOnPreferenceChangeListener(this);
        }

        mLowPower = (SwitchPreference) findPreference(KEY_LIVE_DISPLAY_LOW_POWER);
        if (advancedPrefs != null && mLowPower != null
                && !mConfig.hasFeature(FEATURE_CABC)) {
            advancedPrefs.removePreference(mLowPower);
            mLowPower = null;
            Log.d("LiveDisplay", "FEATURE_CABC false");
        }

        mColorEnhancement = (SwitchPreference) findPreference(KEY_LIVE_DISPLAY_COLOR_ENHANCE);
        if (advancedPrefs != null && mColorEnhancement != null
                && !mConfig.hasFeature(FEATURE_COLOR_ENHANCEMENT)) {
            advancedPrefs.removePreference(mColorEnhancement);
            mColorEnhancement = null;
        }

        mPictureAdjustment = (PictureAdjustment) findPreference(KEY_PICTURE_ADJUSTMENT);
        if (advancedPrefs != null && mPictureAdjustment != null &&
                    !mConfig.hasFeature(LiveDisplayManager.FEATURE_PICTURE_ADJUSTMENT)) {
            advancedPrefs.removePreference(mPictureAdjustment);
            mPictureAdjustment = null;
            Log.d("LiveDisplay", "FEATURE_PICTURE_ADJUSTMENT false");
        }

        mDisplayColor = (DisplayColor) findPreference(KEY_DISPLAY_COLOR);
        if (advancedPrefs != null && mDisplayColor != null &&
                !mConfig.hasFeature(LiveDisplayManager.FEATURE_COLOR_ADJUSTMENT)) {
            advancedPrefs.removePreference(mDisplayColor);
            mDisplayColor = null;
            Log.d("LiveDisplay", "FEATURE_COLOR_ADJUSTMENT false");
        }
    }

    private boolean updateDisplayModes() {
        final DisplayMode[] modes = mHardware.getDisplayModes();
        if (modes == null || modes.length == 0) {
            return false;
        }

        final DisplayMode cur = mHardware.getCurrentDisplayMode() != null
                ? mHardware.getCurrentDisplayMode() : mHardware.getDefaultDisplayMode();
        int curId = -1;
        String[] entries = new String[modes.length];
        String[] values = new String[modes.length];
        mColorProfileSummaries = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            values[i] = String.valueOf(modes[i].id);
            entries[i] = ResourceUtils.getLocalizedString(
                    getResources(), modes[i].name, COLOR_PROFILE_TITLE);

            // Populate summary
            String summary = ResourceUtils.getLocalizedString(
                    getResources(), modes[i].name, COLOR_PROFILE_SUMMARY);
            if (summary != null) {
                summary = String.format("%s - %s", entries[i], summary);
            }
            mColorProfileSummaries[i] = summary;

            if (cur != null && modes[i].id == cur.id) {
                curId = cur.id;
            }
        }
        mColorProfile.setEntries(entries);
        mColorProfile.setEntryValues(values);
        if (curId >= 0) {
            mColorProfile.setValue(String.valueOf(curId));
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateModeSummary();
        updateTemperatureSummary();
        updateColorProfileSummary(null);
        SettingsHelper.get(getActivity()).startWatching(this, DISPLAY_TEMPERATURE_DAY_URI,
                DISPLAY_TEMPERATURE_MODE_URI, DISPLAY_TEMPERATURE_NIGHT_URI);
    }

    @Override
    public void onPause() {
        super.onPause();
        SettingsHelper.get(getActivity()).stopWatching(this);
    }

    private void updateColorProfileSummary(String value) {
        if (!mHasDisplayModes) {
            return;
        }

        if (value == null) {
            DisplayMode cur = mHardware.getCurrentDisplayMode() != null
                    ? mHardware.getCurrentDisplayMode() : mHardware.getDefaultDisplayMode();
            if (cur != null && cur.id >= 0) {
                value = String.valueOf(cur.id);
            }
        }

        int idx = mColorProfile.findIndexOfValue(value);
        if (idx < 0) {
            //Log.e(TAG, "No summary resource found for profile " + value);
            mColorProfile.setSummary(null);
            return;
        }

        mColorProfile.setValue(value);
        mColorProfile.setSummary(mColorProfileSummaries[idx]);
    }

    private void updateModeSummary() {
        int mode = mLiveDisplayManager.getMode();

        int index = ArrayUtils.indexOf(mModeValues, String.valueOf(mode));
        if (index < 0) {
            index = ArrayUtils.indexOf(mModeValues, String.valueOf(MODE_OFF));
        }

        mLiveDisplay.setSummary(mModeSummaries[index]);
        mLiveDisplay.setValue(String.valueOf(mode));

        if (mDisplayTemperature != null) {
            mDisplayTemperature.setEnabled(mode != MODE_OFF);
        }
        if (mOutdoorMode != null) {
            mOutdoorMode.setEnabled(mode != MODE_OFF);
        }
    }

    private void updateTemperatureSummary() {
        int day = mLiveDisplayManager.getDayColorTemperature();
        int night = mLiveDisplayManager.getNightColorTemperature();

        mDisplayTemperature.setSummary(getResources().getString(
                R.string.live_display_color_temperature_summary,
                mDisplayTemperature.roundUp(day),
                mDisplayTemperature.roundUp(night)));
    }

	@Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        Log.d("LiveDisplay", "preference: " + preference.toString());
        Log.d("LiveDisplay", "value: " + String.valueOf(objValue));
    	if (preference == mLiveDisplay) {
            mLiveDisplayManager.setMode(Integer.valueOf((String)objValue));
        } else if (preference == mColorProfile) {
            int id = Integer.valueOf((String)objValue);
            Log.d("LiveDisplay", "Setting mode: " + id);
            for (DisplayMode mode : mHardware.getDisplayModes()) {
                if (mode.id == id) {
                    mHardware.setDisplayMode(mode, true);
                    updateColorProfileSummary((String)objValue);
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public void onSettingsChanged(Uri uri) {
        updateModeSummary();
        updateTemperatureSummary();
    }

}