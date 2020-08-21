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
import android.net.wifi.WifiConfiguration;
import android.text.InputType;
import android.text.TextUtils;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.ValidatedEditTextPreference;

import java.util.UUID;

/**
 * Controls Wifi Hotspot password configuration.
 *
 * <p>Note: This controller uses {@link ValidatedEditTextPreference} as opposed to
 * PasswordEditTextPreference because the input is not obscured by default, and the user is setting
 * their own password, as opposed to entering password for authentication.
 */
public class WifiTetherPasswordPreferenceController extends
        WifiTetherBasePreferenceController<ValidatedEditTextPreference> {

    protected static final String SHARED_PREFERENCE_PATH =
            "com.android.car.settings.wifi.WifiTetherPreferenceController";
    protected static final String KEY_SAVED_PASSWORD =
            "com.android.car.settings.wifi.SAVED_PASSWORD";

    private static final int SHARED_SECURITY_TYPE_UNSET = -1;

    private static final int HOTSPOT_PASSWORD_MIN_LENGTH = 8;
    private static final int HOTSPOT_PASSWORD_MAX_LENGTH = 63;
    private static final ValidatedEditTextPreference.Validator PASSWORD_VALIDATOR =
            value -> value.length() >= HOTSPOT_PASSWORD_MIN_LENGTH
                    && value.length() <= HOTSPOT_PASSWORD_MAX_LENGTH;

    private final SharedPreferences mSharedPreferences =
            getContext().getSharedPreferences(SHARED_PREFERENCE_PATH, Context.MODE_PRIVATE);

    private String mPassword;
    private int mSecurityType;

    public WifiTetherPasswordPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<ValidatedEditTextPreference> getPreferenceType() {
        return ValidatedEditTextPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        getPreference().setValidator(PASSWORD_VALIDATOR);
        mSecurityType = getCarWifiApConfig().getAuthType();
        syncPassword();
    }

    @Override
    protected void onStartInternal() {
        int newSecurityType = mSharedPreferences.getInt(
              WifiTetherSecurityPreferenceController.KEY_SECURITY_TYPE,
              /* defaultValue= */ SHARED_SECURITY_TYPE_UNSET);

        if (mSecurityType != newSecurityType
              && newSecurityType != SHARED_SECURITY_TYPE_UNSET) {
            // Security type has been changed - update ap configuration
            mSecurityType = newSecurityType;
            syncPassword();
            updateApConfiguration();
        }
    }

    @Override
    protected boolean handlePreferenceChanged(ValidatedEditTextPreference preference,
            Object newValue) {
        if (!newValue.toString().equals(mPassword)) {
            mPassword = newValue.toString();
            updateApConfiguration();
            refreshUi();
        }
        return true;
    }

    @Override
    protected void updateState(ValidatedEditTextPreference preference) {
        super.updateState(preference);
        updatePasswordDisplay();
        if (TextUtils.isEmpty(mPassword)) {
            preference.setSummaryInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            preference.setSummaryInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    @Override
    protected String getSummary() {
        return mPassword;
    }

    @Override
    protected String getDefaultSummary() {
        return getContext().getString(R.string.default_password_summary);
    }

    private void syncPassword() {
        mPassword = getSyncedPassword();
        refreshUi();
    }

    private String getSyncedPassword() {
        if (mSecurityType == WifiConfiguration.KeyMgmt.NONE
              || mSecurityType == WifiConfiguration.KeyMgmt.OWE) {
            return null;
        }

        if (!TextUtils.isEmpty(getCarWifiApConfig().preSharedKey)) {
            return getCarWifiApConfig().preSharedKey;
        }

        if (!TextUtils.isEmpty(
                mSharedPreferences.getString(KEY_SAVED_PASSWORD, /* defaultValue= */ null))) {
            return mSharedPreferences.getString(KEY_SAVED_PASSWORD, /* defaultValue= */ null);
        }

        return generateRandomPassword();
    }

    private static String generateRandomPassword() {
        String randomUUID = UUID.randomUUID().toString();
        // First 12 chars from xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
        return randomUUID.substring(0, 8) + randomUUID.substring(9, 13);
    }

    private void updateApConfiguration() {
        WifiConfiguration config = getCarWifiApConfig();
        config.allowedKeyManagement.clear();
        config.allowedKeyManagement.set(mSecurityType);
        if (mSecurityType == WifiConfiguration.KeyMgmt.NONE
            || mSecurityType == WifiConfiguration.KeyMgmt.OWE) {
            config.preSharedKey = "";
        } else {
            config.preSharedKey = mPassword;
        }
        setCarWifiApConfig(config);
        if (!TextUtils.isEmpty(mPassword)) {
            mSharedPreferences.edit().putString(KEY_SAVED_PASSWORD, mPassword).commit();
        }
    }

    private void updatePasswordDisplay() {
        getPreference().setText(mPassword);
        getPreference().setVisible(mSecurityType != WifiConfiguration.KeyMgmt.NONE
                                && mSecurityType != WifiConfiguration.KeyMgmt.OWE);
        getPreference().setSummary(getSummary());
    }

}
