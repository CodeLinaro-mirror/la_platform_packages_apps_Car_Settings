/*
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.car.settings.battery;

import android.provider.Settings;

import androidx.annotation.XmlRes;

import com.android.car.settings.R;
import com.android.car.settings.common.SettingsFragment;
import com.android.car.settings.search.CarBaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Preference fragment to host Battery related preferences.
 */
@SearchIndexable
public class BatterySettingsFragment extends SettingsFragment {

    public static final CarBaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new CarBaseSearchIndexProvider(R.xml.battery_settings_fragment,
                    Settings.ACTION_DISPLAY_SETTINGS);

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.battery_settings_fragment;
    }
}
