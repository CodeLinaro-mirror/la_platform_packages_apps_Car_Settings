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

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.SoftApConfiguration;

import androidx.preference.ListPreference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;

import android.net.wifi.SoftApCapability;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.util.Log;
import java.util.ArrayList;
/**
 * Controls WiFi Hotspot Security Type configuration.
 */
public class WifiTetherSecurityPreferenceController extends
        WifiTetherBasePreferenceController<ListPreference> {

    protected static final String KEY_SECURITY_TYPE =
            "com.android.car.settings.wifi.KEY_SECURITY_TYPE";

    private int mSecurityType;
    private static final String TAG = "WifiTetherSecurityPreferenceController";
    final Context mContext;
    private WifiManager mWifiManager;
    private String[] mSecurityEntries;
    private String[] mSecurityValues;
    private boolean mSecurityCapaFetched;
    private boolean mSaeSapSupported;
    private WifiManager.SoftApCallback mSoftApCallback = new WifiManager.SoftApCallback() {
        @Override
        public void onCapabilityChanged(SoftApCapability capability) {
            if (mSecurityCapaFetched)
                return;
            ArrayList<String> securityEntries =  new ArrayList<String>();
            ArrayList<String> securityValues =  new ArrayList<String>();

            mSecurityCapaFetched = true;

            if (capability.areFeaturesSupported(SoftApCapability.SOFTAP_FEATURE_WPA3_SAE)) {
                mSaeSapSupported = true;
            }
            if (mSaeSapSupported) {
            // Add SAE transition security type
                securityValues.add(String.valueOf(SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION));
                securityEntries.add(mContext.getString(R.string.wifi_security_sae));
            }
            // Add WPA2-PSK security type
            securityValues.add(String.valueOf(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK));
            securityEntries.add(mContext.getString(R.string.wifi_security_wpa2));
            // Add open security type
            securityValues.add(String.valueOf(SoftApConfiguration.SECURITY_TYPE_OPEN));
            securityEntries.add(mContext.getString(R.string.wifi_security_none));

            mSecurityEntries = securityEntries.toArray(new String[securityEntries.size()]);
            mSecurityValues = securityValues.toArray(new String[securityValues.size()]);

            updateDisplay();
            Log.i(TAG, "Updated supported SoftAp AKMs");
        }
    };

    private void updateDisplay() {
        final SoftApConfiguration config = getCarSoftApConfig();
        if (config == null) {
            mSecurityType = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
        } else if (config.getSecurityType() == SoftApConfiguration.SECURITY_TYPE_OPEN) {
            mSecurityType = SoftApConfiguration.SECURITY_TYPE_OPEN;
        } else if (mSaeSapSupported
                    && config.getSecurityType() == SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION) {
            mSecurityType = SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION;
        } else {
            mSecurityType = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
        }
        getPreference().setEntries(mSecurityEntries);
        getPreference().setEntryValues(mSecurityValues);
        getPreference().setValue(String.valueOf(mSecurityType));
    }

    private final SharedPreferences mSharedPreferences = getContext().getSharedPreferences(
                    WifiTetherPasswordPreferenceController.SHARED_PREFERENCE_PATH,
                    Context.MODE_PRIVATE);

    public WifiTetherSecurityPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mContext = context;
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mWifiManager.registerSoftApCallback(new HandlerExecutor(new Handler()), mSoftApCallback);
    }

    @Override
    protected Class<ListPreference> getPreferenceType() {
        return ListPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        mSecurityType = getCarSoftApConfig().getSecurityType();
        getPreference().setEntries(mSecurityEntries);
        getPreference().setEntryValues(mSecurityValues);
        getPreference().setValue(String.valueOf(mSecurityType));
    }

    @Override
    protected boolean handlePreferenceChanged(ListPreference preference,
            Object newValue) {
        mSecurityType = Integer.parseInt(newValue.toString());
        // Rather than updating the ap config here, we will only update the security type shared
        // preference. When the user confirms their selection by going back, the config will be
        // updated by the WifiTetherPasswordPreferenceController. By updating the config in that
        // controller, we avoid running into a transient state where the (securityType, passphrase)
        // pair is invalid due to not being updated simultaneously.
        mSharedPreferences.edit().putInt(KEY_SECURITY_TYPE, mSecurityType).commit();
        refreshUi();
        return true;
    }

    @Override
    protected void updateState(ListPreference preference) {
        super.updateState(preference);
        preference.setValue(Integer.toString(mSecurityType));
    }

    @Override
    protected String getSummary() {
        int stringResId = R.string.wifi_security_wpa2;
        if (mSecurityType == SoftApConfiguration.SECURITY_TYPE_OPEN) {
            stringResId = R.string.wifi_security_none;
        } else if (mSecurityType == SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION) {
            stringResId = R.string.wifi_security_sae;
        }
        return getContext().getString(stringResId);
    }

    @Override
    protected String getDefaultSummary() {
        return null;
    }
}
