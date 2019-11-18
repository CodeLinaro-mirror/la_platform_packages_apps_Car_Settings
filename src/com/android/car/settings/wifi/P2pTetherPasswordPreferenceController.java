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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.p2p.WifiP2pConfig;
import android.text.InputType;
import android.text.TextUtils;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.ValidatedEditTextPreference;

/**
 * Controls Wifi Hotspot password configuration.
 *
 * <p>Note: This controller uses {@link ValidatedEditTextPreference} as opposed to
 * PasswordEditTextPreference because the input is not obscured by default, and the user is setting
 * their own password, as opposed to entering password for authentication.
 */
public class P2pTetherPasswordPreferenceController extends
        WifiTetherBasePreferenceController<ValidatedEditTextPreference> {

    private static final int HOTSPOT_PASSWORD_MIN_LENGTH = 8;
    private static final int HOTSPOT_PASSWORD_MAX_LENGTH = 63;
    private static final ValidatedEditTextPreference.Validator PASSWORD_VALIDATOR =
            value -> value.length() >= HOTSPOT_PASSWORD_MIN_LENGTH
                    && value.length() <= HOTSPOT_PASSWORD_MAX_LENGTH;

    private String mPassword;

    public P2pTetherPasswordPreferenceController(Context context, String preferenceKey,
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
        syncPassword();
    }

/*
    @Override
    protected void onStartInternal() { }

    @Override
    protected void onStopInternal() {
        super.onStopInternal();
    }
*/
    @Override
    protected boolean handlePreferenceChanged(ValidatedEditTextPreference preference,
            Object newValue) {
        mPassword = newValue.toString();
        updatePassword(mPassword);
        refreshUi();
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
        mPassword = getCarP2pTetherConfiguration().passphrase;
        updatePassword(mPassword);
        refreshUi();
    }

    private void updatePassword(String password) {
        WifiP2pConfig config = getCarP2pTetherConfiguration();
        config.passphrase = password;
        setCarP2pTetherConfiguration(config);
    }

    private void updatePasswordDisplay() {
        getPreference().setText(mPassword);
        getPreference().setSummary(getSummary());
    }
}
