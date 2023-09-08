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

import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

import android.car.drivingstate.CarUxRestrictions;

public class BatteryProfilePreferenceController extends PreferenceController<Preference> {

    private static final int DEFAULT_SELECTOR_INDEX = 0;
    private static final int DEFAULT_PROFILE = 1;
    private static final Uri CURRENT_BATTERY_PROFILE_URI = Settings.Global.getUriFor(
            Settings.Global.CURRENT_BATTERY_PROFILE);

    final String[] mProfileTitles;

    private Handler mHandler = new Handler(Looper.myLooper());
    private ContentObserver mCurrentBatteryProfileObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            if (selfChange) {
                return;
            }
            refreshUi();
        }
    };

    public BatteryProfilePreferenceController(Context context, String preferenceKey,
                                              FragmentController fragmentController,
                                              CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);

        mProfileTitles = new String[]{
                context.getString(R.string.battery_profile_invalid),
                context.getString(R.string.battery_profile_normal),
                context.getString(R.string.battery_profile_low),
                context.getString(R.string.battery_profile_critical)
        };
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    protected void updateState(Preference preference) {
        CharSequence summary = getSummary(getCurrentBatteryProfile());
        preference.setSummary(summary);
    }

    @Override
    protected void onStartInternal() {
        registerContentObserver();
    }

    public void registerContentObserver() {
        unregisterContentObserver();
        ContentResolver cr = getContext().getContentResolver();
        cr.registerContentObserver(CURRENT_BATTERY_PROFILE_URI, false,
                mCurrentBatteryProfileObserver);
    }

    public void unregisterContentObserver() {
        ContentResolver cr = getContext().getContentResolver();
        cr.unregisterContentObserver(mCurrentBatteryProfileObserver);
    }

    @Override
    protected void onStopInternal() {
        unregisterContentObserver();
    }

    @Override
    protected int getAvailabilityStatus() {
        return AVAILABLE_FOR_VIEWING;
    }

    private CharSequence getSummary(int currentProfile) {
        if (currentProfile >= mProfileTitles.length && currentProfile < 0) {
            return mProfileTitles[0];
        }
        return mProfileTitles[currentProfile];
    }

    private int getCurrentBatteryProfile() {
        return Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.CURRENT_BATTERY_PROFILE, DEFAULT_PROFILE);
    }
}
