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

import android.util.Log;

import java.lang.reflect.Method;

/**
 * Controls wifi tethering enable 11be configuration
 */
public class WifiTetherEnableMloPreferenceController extends
        PreferenceController<TwoStatePreference> {

    private final String TAG = "WifiTetherEnableMloPreferenceController";
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
        try {
            Method methodIsMloEnabled = softApConfiguration.getClass().getMethod("isMultiLinkOperationEnabled");
            boolean enableMlo = (boolean) methodIsMloEnabled.invoke(softApConfiguration);
            preference.setChecked(enableMlo);
        } catch (Exception e) {
            Log.e(TAG, "Can't find isMultiLinkOperationEnabled function in SoftApConfiguration");
        }
    }

    @Override
    protected boolean handlePreferenceChanged(TwoStatePreference preference, Object newValue) {
        boolean enableMlo = (Boolean) newValue;
        SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        SoftApConfiguration newSoftApConfiguration =
                new SoftApConfiguration.Builder(softApConfiguration)
                        .setIeee80211beEnabled(enableMlo)
                        .build();
        try {
            Method methodSetMloEnable = softApConfiguration.getClass().getMethod("setMultiLinkOperationEnabled");
            methodSetMloEnable.invoke(softApConfiguration, enableMlo);
        } catch (Exception e) {
            Log.e(TAG, "Can't find setMultiLinkOperationEnabled function in SoftApConfiguration");
        }
        return mWifiManager.setSoftApConfiguration(newSoftApConfiguration);
    }
}

