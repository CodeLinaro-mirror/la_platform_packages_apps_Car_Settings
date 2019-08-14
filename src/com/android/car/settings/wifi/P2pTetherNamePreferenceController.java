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
import android.net.wifi.p2p.WifiP2pConfig;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.ValidatedEditTextPreference;

/**
 * Controls P2P Hotspot name configuration.
 */
public class P2pTetherNamePreferenceController extends
        WifiTetherBasePreferenceController<ValidatedEditTextPreference> {

    private static final int HOTSPOT_NAME_MIN_LENGTH = 1;
    private static final int HOTSPOT_NAME_MAX_LENGTH = 32;
    private static final ValidatedEditTextPreference.Validator NAME_VALIDATOR =
            value -> value.length() >= HOTSPOT_NAME_MIN_LENGTH
                    && value.length() <= HOTSPOT_NAME_MAX_LENGTH;

    private String mName;

    public P2pTetherNamePreferenceController(Context context, String preferenceKey,
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
        getPreference().setValidator(NAME_VALIDATOR);

        mName = getCarP2pTetherConfiguration().networkName;
    }

    @Override
    protected boolean handlePreferenceChanged(ValidatedEditTextPreference preference,
            Object newValue) {
        mName = newValue.toString();
        updateSSID(mName);
        refreshUi();
        return true;
    }

    @Override
    protected void updateState(ValidatedEditTextPreference preference) {
        super.updateState(preference);
        preference.setText(mName);
    }

    private void updateSSID(String ssid) {
        WifiP2pConfig config = getCarP2pTetherConfiguration();
        config.networkName = ssid;
        setCarP2pTetherConfiguration(config);
    }

    @Override
    protected String getSummary() {
        return mName;
    }

    @Override
    protected String getDefaultSummary() {
        return null;
    }
}
