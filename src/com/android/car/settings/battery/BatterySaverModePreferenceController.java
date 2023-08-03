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

public class BatterySaverModePreferenceController extends PreferenceController<Preference> {

    private static final int DEFAULT_SELECTOR_INDEX = 0;
    private static final int DEFAULT_MODE = 0;
    private static final Uri LOW_POWER_MODE_URI = Settings.Global.getUriFor(
            Settings.Global.LOW_POWER_MODE);

    private Handler mHandler = new Handler(Looper.myLooper());
    private ContentObserver mLowBatteryObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            if (selfChange) {
                return;
            }
            refreshUi();
        }
    };

    public BatterySaverModePreferenceController(Context context, String preferenceKey,
                                                FragmentController fragmentController,
                                                CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    protected void updateState(Preference preference) {
        CharSequence summary = getSummary(isLowPowerModeOn());
        preference.setSummary(summary);
    }

    @Override
    protected void onStartInternal() {
        registerContentObserver();
    }

    public void registerContentObserver() {
        unregisterContentObserver();
        ContentResolver cr = getContext().getContentResolver();
        cr.registerContentObserver(LOW_POWER_MODE_URI, false, mLowBatteryObserver);
    }

    public void unregisterContentObserver() {
        ContentResolver cr = getContext().getContentResolver();
        cr.unregisterContentObserver(mLowBatteryObserver);
    }

    @Override
    protected void onStopInternal() {
        unregisterContentObserver();
    }

    private CharSequence getSummary(boolean isSaverOn) {

        String summary = isSaverOn ? getContext().getString(R.string.battery_saver_on_summary) :
                getContext().getString(R.string.battery_saver_off_summary);
        return summary;
    }

    private boolean isLowPowerModeOn() {
        int mode = Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.LOW_POWER_MODE, DEFAULT_MODE);
        return (mode > 0);
    }
}
