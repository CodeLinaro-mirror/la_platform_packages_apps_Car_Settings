/*
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.car.settings.battery;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

import android.car.drivingstate.CarUxRestrictions;

/**
 * Business logic for changing the battery saver mode on battery profile.
 */
public class BatterySaverProfileListController extends PreferenceController<ListPreference> {

    private static final int DEFAULT_SELECTOR_INDEX = 2;
    private static final int DEFAULT_PROFILE = 1;
    private static final Uri BATTERY_SAVER_FOR_PROFILE_URI = Settings.Global.getUriFor(
            Settings.Global.ENABLE_BATTERY_SAVER_FOR_POWER_PROFILE);

    @VisibleForTesting
    final String[] mProfileTitles;
    private final String[] mProfileStringValues;
    private final float[] mProfileIntValues;

    private Handler mHandler = new Handler(Looper.myLooper());
    private ContentObserver mBatterySaverSettingObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            if (selfChange) {
                return;
            }
            refreshUi();
        }
    };

    public BatterySaverProfileListController(Context context, String preferenceKey,
                                             FragmentController fragmentController,
                                             CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mProfileTitles = new String[]{
                context.getString(R.string.battery_saver_never),
                context.getString(R.string.battery_saver_profile_low),
                context.getString(R.string.battery_saver_profile_critical)
        };
        mProfileStringValues = new String[]{
                "1",
                "2",
                "3"
        };
        mProfileIntValues = new float[mProfileStringValues.length];
        for (int i = 0; i < mProfileStringValues.length; i++) {
            mProfileIntValues[i] = Integer.parseInt(mProfileStringValues[i]);
        }
    }

    @Override
    protected void onStartInternal() {
        registerContentObserver();
    }

    public void registerContentObserver() {
        ContentResolver cr = getContext().getContentResolver();
        cr.registerContentObserver(BATTERY_SAVER_FOR_PROFILE_URI, false,
                mBatterySaverSettingObserver);
    }

    public void unregisterContentObserver() {
        ContentResolver cr = getContext().getContentResolver();
        cr.unregisterContentObserver(mBatterySaverSettingObserver);
    }

    @Override
    protected void onStopInternal() {
        unregisterContentObserver();
    }

    @Override
    protected Class<ListPreference> getPreferenceType() {
        return ListPreference.class;
    }

    @Override
    protected void updateState(ListPreference preference) {
        preference.setEntries(mProfileTitles);
        preference.setEntryValues(mProfileStringValues);
        int currentSaverProfile = getCurrentSaverProfile();
        preference.setValueIndex(currentSaverProfile);
        preference.setSummary(getSummary(currentSaverProfile));
    }

    @Override
    public boolean handlePreferenceChanged(ListPreference preference, Object newValue) {
        int newProfileValue = Integer.parseInt((String) newValue);
        Settings.Global.putInt(getContext().getContentResolver(),
                Settings.Global.ENABLE_BATTERY_SAVER_FOR_POWER_PROFILE, newProfileValue);
        return true;
    }

    private CharSequence getSummary(int currentSaverProfile) {
        return mProfileTitles[currentSaverProfile];
    }

    private int getCurrentSaverProfile() {
        float currrentProfile = Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.ENABLE_BATTERY_SAVER_FOR_POWER_PROFILE, DEFAULT_PROFILE);

        int selectorIndex = DEFAULT_SELECTOR_INDEX;
        for (int i = 0; i < mProfileIntValues.length; i++) {
            if (mProfileIntValues[i] == currrentProfile) {
                selectorIndex = i;
                break;
            }
        }

        return selectorIndex;
    }
}
