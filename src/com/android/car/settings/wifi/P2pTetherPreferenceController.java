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
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.car.settings.R;

/**
 * Controls the availability of wifi tethering preference based on whether tethering is supported
 */
public class P2pTetherPreferenceController extends PreferenceController<Preference> {

    private final ConnectivityManager mConnectivityManager =
            (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    private final CarWifiManager mCarWifiManager = new CarWifiManager(getContext());
    private final IntentFilter mFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
              refreshUi();
          }
    };

    public P2pTetherPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    protected int getAvailabilityStatus() {
        return  mConnectivityManager.isTetheringSupported() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    protected void onStartInternal() {
        super.onStartInternal();
        getContext().registerReceiver(mReceiver, mFilter);
    }

    @Override
    protected void onStopInternal() {
        super.onStopInternal();
        getContext().unregisterReceiver(mReceiver);
    }

    @Override
    protected void updateState(Preference preference) {
        super.updateState(preference);

        if (mCarWifiManager.isWifiEnabled()) {
            preference.setEnabled(true);
            preference.setSummary("");
        } else {
            String not_available =
                getContext().getResources().getString(
                    R.string.device_info_not_available) + ": " +
                getContext().getResources().getString(
                    R.string.wifi_disabled);

            preference.setEnabled(false);
            preference.setSummary(not_available);
        }
    }
}
