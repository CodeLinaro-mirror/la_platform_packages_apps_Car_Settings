/*
 * Copyright (c) 2019, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.car.settings.wifi;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.p2p.WifiP2pConfig;

import androidx.preference.ListPreference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;

/**
 * Controls WiFi Hotspot AP Band configuration.
 */
public class P2pTetherApBandPreferenceController extends
        WifiTetherBasePreferenceController<ListPreference> {

    private String[] mBandEntries;
    private String[] mBandSummaries;
    private int mBandIndex;
    private boolean mIsDualMode;

    public P2pTetherApBandPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<ListPreference> getPreferenceType() {
        return ListPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        mIsDualMode = getCarWifiManager().isDualModeSupported();
        updatePreferenceEntries();
        getPreference().setEntries(mBandSummaries);
        getPreference().setEntryValues(mBandEntries);
    }

    private int toBandIndex(int index) {
        int bandIndex = WifiConfiguration.AP_BAND_2GHZ;

        switch(index) {
            case WifiP2pConfig.GROUP_OWNER_BAND_2GHZ:
                bandIndex = WifiConfiguration.AP_BAND_2GHZ;
                break;
            case WifiP2pConfig.GROUP_OWNER_BAND_5GHZ:
                bandIndex = WifiConfiguration.AP_BAND_5GHZ;
                break;
            case WifiP2pConfig.GROUP_OWNER_BAND_AUTO:
                bandIndex = WifiConfiguration.AP_BAND_ANY;
                break;
            default:
                break;
        }

        return bandIndex;
    }

    private int fromBandIndex(int bandIndex) {
        int index = WifiP2pConfig.GROUP_OWNER_BAND_2GHZ;

        switch(bandIndex) {
            case WifiConfiguration.AP_BAND_2GHZ:
                index = WifiP2pConfig.GROUP_OWNER_BAND_2GHZ;
                break;
            case WifiConfiguration.AP_BAND_5GHZ:
                index = WifiP2pConfig.GROUP_OWNER_BAND_5GHZ;
                break;
            case WifiConfiguration.AP_BAND_ANY:
                index = WifiP2pConfig.GROUP_OWNER_BAND_AUTO;
                break;
            default:
                break;
        }

        return index;
    }


    @Override
    public void updateState(ListPreference preference) {
        super.updateState(preference);

        WifiP2pConfig config = getCarP2pTetherConfiguration();
        if (config == null) {
            mBandIndex = 0;
        } else if (is5GhzBandSupported()) {
            mBandIndex = validateSelection(toBandIndex(config.groupOwnerBand));
        } else {
            config.groupOwnerBand = WifiP2pConfig.GROUP_OWNER_BAND_2GHZ;
            setCarP2pTetherConfiguration(config);
            mBandIndex = toBandIndex(config.groupOwnerBand);
        }

        if (!is5GhzBandSupported()) {
            preference.setEnabled(false);
            preference.setSummary(R.string.wifi_ap_choose_2G);
        } else {
            preference.setValue(Integer.toString(toBandIndex(config.groupOwnerBand)));
            preference.setSummary(getSummary());
        }

    }

    @Override
    protected String getSummary() {
        if (is5GhzBandSupported()) {
            if (mBandIndex != WifiConfiguration.AP_BAND_ANY) {
                return mBandSummaries[mBandIndex];
            } else {
                return getContext().getString(R.string.wifi_ap_prefer_5G);
            }
        } else {
            return getContext().getString(R.string.wifi_ap_choose_2G);
        }
    }

    @Override
    protected String getDefaultSummary() {
        return null;
    }

    @Override
    public boolean handlePreferenceChanged(ListPreference preference, Object newValue) {
        mBandIndex = validateSelection(Integer.parseInt((String) newValue));
        updateApBand(); // updating AP band because mBandIndex may have been assigned a new value.
        refreshUi();
        return true;
    }

    private int validateSelection(int band) {
        // Reset the band to 2.4 GHz if we get a weird config back to avoid a crash.
        boolean isDualMode = getCarWifiManager().isDualModeSupported();

        // unsupported states:
        // 1: no dual mode means we can't have AP_BAND_ANY - default to 5GHZ
        // 2: no 5 GHZ support means we can't have AP_BAND_5GHZ - default to 2GHZ
        // 3: With Dual mode support we can't have AP_BAND_5GHZ - default to ANY
        if (!isDualMode && WifiConfiguration.AP_BAND_ANY == band) {
            return WifiConfiguration.AP_BAND_5GHZ;
        } else if (!is5GhzBandSupported() && WifiConfiguration.AP_BAND_5GHZ == band) {
            return WifiConfiguration.AP_BAND_2GHZ;
        } else if (isDualMode && WifiConfiguration.AP_BAND_5GHZ == band) {
            return WifiConfiguration.AP_BAND_ANY;
        }

        return band;
    }

    private void updatePreferenceEntries() {
        Resources res = getContext().getResources();
        int entriesRes = R.array.wifi_ap_band_config_full;
        int summariesRes = R.array.wifi_ap_band_summary_full;
        // change the list options if this is a dual mode device
        if (mIsDualMode) {
            entriesRes = R.array.wifi_ap_band_dual_mode;
            summariesRes = R.array.wifi_ap_band_dual_mode_summary;
        }
        mBandEntries = res.getStringArray(entriesRes);
        mBandSummaries = res.getStringArray(summariesRes);
    }

    private void updateApBand() {
        WifiP2pConfig config = getCarP2pTetherConfiguration();
        config.groupOwnerBand = fromBandIndex(mBandIndex);
        setCarP2pTetherConfiguration(config);
        if (mBandIndex == WifiConfiguration.AP_BAND_ANY) {
            getPreference().setValue(mBandEntries[WifiConfiguration.AP_BAND_2GHZ]);
        } else {
            getPreference().setValue(mBandEntries[mBandIndex]);
        }
    }

    private boolean is5GhzBandSupported() {
        String countryCode = getCarWifiManager().getCountryCode();
        return getCarWifiManager().isDualBandSupported() && countryCode != null;
    }
}
