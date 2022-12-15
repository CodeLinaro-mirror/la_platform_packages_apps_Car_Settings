/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.settings.wifi;

import static android.net.wifi.SoftApConfiguration.BAND_2GHZ;
import static android.net.wifi.SoftApConfiguration.BAND_5GHZ;
import static android.net.wifi.SoftApConfiguration.BAND_6GHZ;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.util.SparseIntArray;
import android.util.Log;
import android.content.SharedPreferences;

import androidx.preference.ListPreference;

import com.android.car.settings.Flags;
import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.internal.annotations.VisibleForTesting;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controls WiFi Hotspot AP Band configuration.
 */
public class WifiTetherApBandPreferenceController extends
        WifiTetherBasePreferenceController<ListPreference> {
    private static final String TAG = "CarWifiTetherApBandPref";

    /** Wi-Fi hotspot band 2.4GHz and 5GHz. */
    @VisibleForTesting
    static final int BAND_2GHZ_5GHZ = BAND_2GHZ | BAND_5GHZ;

    /** Wi-Fi hotspot dual band for 2.4GHz and 5GHz. */
    @VisibleForTesting
    static final int[] DUAL_BANDS = new int[] {
            BAND_2GHZ,
            BAND_2GHZ_5GHZ };

    protected static final String KEY_AP_BAND =
                                  "com.android.car.settings.wifi.AP_BAND";

    private final SharedPreferences mSharedPreferences = getContext().getSharedPreferences(
                              WifiTetherPasswordPreferenceController.SHARED_PREFERENCE_PATH,
                              Context.MODE_PRIVATE);

    private static final int BAND_2GHZ_6GHZ = BAND_6GHZ | BAND_2GHZ;

    private final Map<Integer, String> mHotspotBandMap = new LinkedHashMap<>();

    private int mBand;

    public WifiTetherApBandPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions,
                new CarWifiManager(context, fragmentController.getSettingsLifecycle()));
    }

    @VisibleForTesting
    WifiTetherApBandPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            CarWifiManager carWifiManager) {
        super(context, preferenceKey, fragmentController, uxRestrictions, carWifiManager);

        String[] bandNames = getContext().getResources().getStringArray(
                R.array.wifi_ap_band_summary);
        String[] bandValues = getContext().getResources().getStringArray(
                R.array.wifi_ap_band);
        for (int i = 0; i < bandNames.length; i++) {
            mHotspotBandMap.put(Integer.parseInt(bandValues[i]), bandNames[i]);
        }
    }

    @Override
    protected Class<ListPreference> getPreferenceType() {
        return ListPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        updatePreferenceEntries();
    }

    @Override
    public void updateState(ListPreference preference) {
        super.updateState(preference);
        updateApBand(); // updating AP band because mBandIndex may have been assigned a new value.

        SoftApConfiguration config = getCarSoftApConfig();
        int configBand = getBandFromConfig();
        if (config == null) {
            mBand = BAND_2GHZ;
        } else if (!is5GhzBandSupported() && !is6GhzBandSupported() && configBand > BAND_2GHZ) {
            SoftApConfiguration newConfig = new SoftApConfiguration.Builder(config)
                    .setBand(BAND_2GHZ)
                    .build();
            setCarSoftApConfig(newConfig);
            mBand = BAND_2GHZ;
        } else {
            mBand = validateSelection(configBand);
        }

        if (!is5GhzBandSupported() && !is6GhzBandSupported()) {
            preference.setEnabled(false);
            preference.setSummary(R.string.wifi_ap_choose_2G);
        } else {
            preference.setValue(Integer.toString(mBand));
            preference.setSummary(getSummary());
        }
    }

    @Override
    protected String getSummary() {
        return mHotspotBandMap.getOrDefault(mBand,
                getContext().getString(R.string.wifi_ap_prefer_5G));
    }

    @Override
    protected String getDefaultSummary() {
        return null;
    }

    @Override
    public boolean handlePreferenceChanged(ListPreference preference, Object newValue) {
        mBand = validateSelection(Integer.parseInt((String) newValue));
        return true;
    }

    private void updatePreferenceEntries() {
        // key-band: 1-2GHz, 2-5GHz, 3-2GHz|5GHz, 5-6GHz|2GHz
        if (!is6GhzBandSupported()) {
            mHotspotBandMap.keySet().remove(BAND_2GHZ_6GHZ);
        }

        // If 5 GHz is not supported, default to 2 GHz
        if (!is5GhzBandSupported()) {
            mHotspotBandMap.keySet().removeIf(key -> key > BAND_2GHZ);
        }

        if (!isDualBandSupported()) {
            mHotspotBandMap.keySet().remove(BAND_2GHZ_5GHZ);
        }

        getPreference().setEntries(mHotspotBandMap.values().toArray(CharSequence[]::new));
        getPreference().setEntryValues(
                mHotspotBandMap.keySet().stream().map(Object::toString).toArray(
                        CharSequence[]::new));

        mBand = validateSelection(getBandFromConfig());
        getPreference().setValue(Integer.toString(mBand));
    }

    private int getBandFromConfig() {
        int band = 0;

        SparseIntArray channels = getCarSoftApConfig().getChannels();
        for (int i = 0; i < channels.size(); i++) {
            band |= channels.keyAt(i);
        }

        if (band == BAND_2GHZ_5GHZ && channels.size() == 1) {
            // band should be set to BAND_5GHZ to differentiate between dual band which would also
            // be BAND_2GHZ_5GHZ.
            band = BAND_5GHZ;
        } else if (band == BAND_2GHZ_6GHZ) {
            band = BAND_6GHZ;
        }

        return band;
    }

    private int validateSelection(int band) {
        return mHotspotBandMap.containsKey(band) ? band : BAND_2GHZ;
    }

    private void updateApBand() {
        SoftApConfiguration.Builder configBuilder = new SoftApConfiguration.Builder(
                getCarSoftApConfig());

        if (mBand == BAND_5GHZ) {
            // Only BAND_5GHZ is not supported, must include BAND_2GHZ since some of countries
            // don't support 5G
            configBuilder.setBand(BAND_2GHZ_5GHZ);
        } else if (mBand == BAND_2GHZ_5GHZ) {
            configBuilder.setBands(DUAL_BANDS);
        } else if (mBand == BAND_6GHZ) {
            configBuilder.setBand(BAND_2GHZ_6GHZ);
        } else {
            configBuilder.setBand(BAND_2GHZ);
        }

        setCarSoftApConfig(configBuilder.build());
        getPreference().setValue(Integer.toString(mBand));
    }

    private boolean is5GhzBandSupported() {
        String countryCode = getCarWifiManager().getCountryCode();
        return getCarWifiManager().is5GhzBandSupported() && countryCode != null;
    }

    private boolean is6GhzBandSupported() {
        String countryCode = getCarWifiManager().getCountryCode();
        return getCarWifiManager().is6GhzBandSupported() && countryCode != null;
    }

    private boolean isDualBandSupported() {
        return getCarWifiManager().isDualBandSupported();
    }
}
