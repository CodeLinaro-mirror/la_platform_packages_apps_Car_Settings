/*
 * Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.car.settings.wifi;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;

import androidx.preference.TwoStatePreference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

/**
 * Controls wifi tethering enable 11be configuration
 */
public class WifiTetherEnableMloPreferenceController extends
        PreferenceController<TwoStatePreference> {

    private final WifiManager mWifiManager;

    public WifiTetherEnableMloPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mWifiManager = context.getSystemService(WifiManager.class);
    }

    @Override
    protected Class<TwoStatePreference> getPreferenceType() {
        return TwoStatePreference.class;
    }

    @Override
    protected void updateState(TwoStatePreference preference) {
        if (mWifiManager == null) {
            return;
        }
        SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        boolean enableMlo = softApConfiguration.isIeee80211beEnabled();
        preference.setChecked(enableMlo);
    }

    @Override
    protected boolean handlePreferenceChanged(TwoStatePreference preference, Object newValue) {
        boolean enableMlo = (Boolean) newValue;
        SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        SoftApConfiguration newSoftApConfiguration =
                new SoftApConfiguration.Builder(softApConfiguration)
                        .setIeee80211beEnabled(enableMlo)
                        .build();
        return mWifiManager.setSoftApConfiguration(newSoftApConfiguration);
    }
}

