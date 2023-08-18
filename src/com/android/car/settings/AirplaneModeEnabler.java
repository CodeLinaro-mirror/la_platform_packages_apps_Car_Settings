/*
 * Copyright (C) 2007 The Android Open Source Project
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
/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 *
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.car.settings;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.car.settings.network.GlobalSettingsChangeListener;
import com.android.settingslib.WirelessUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Monitor and update configuration of airplane mode settings
 */
public class AirplaneModeEnabler extends GlobalSettingsChangeListener {

    private static final String LOG_TAG = "AirplaneModeEnabler";
    private static final boolean DEBUG = false;

    private final Context mContext;

    private OnAirplaneModeChangedListener mOnAirplaneModeChangedListener;

    public interface OnAirplaneModeChangedListener {
        /**
         * Called when airplane mode status is changed.
         *
         * @param isAirplaneModeOn the airplane mode is on
         */
        void onAirplaneModeChanged(boolean isAirplaneModeOn);
    }

    private TelephonyManager mTelephonyManager;
    @VisibleForTesting
    PhoneStateListener mPhoneStateListener;

    public AirplaneModeEnabler(Context context, OnAirplaneModeChangedListener listener) {
        super(context, Settings.Global.AIRPLANE_MODE_ON);

        mContext = context;
        mOnAirplaneModeChangedListener = listener;

        mTelephonyManager = context.getSystemService(TelephonyManager.class);

        mPhoneStateListener = new PhoneStateListener(Looper.getMainLooper()) {
            @Override
            public void onRadioPowerStateChanged(int state) {
                if (DEBUG) {
                    Log.d(LOG_TAG, "RadioPower: " + state);
                }
                onAirplaneModeChanged();
            }
        };
    }

    /**
     * Implementation of GlobalSettingsChangeListener.onChanged
     */
    @Override
    public void onChanged(String field) {
        if (DEBUG) {
            Log.d(LOG_TAG, "Airplane mode configuration update");
        }
        onAirplaneModeChanged();
    }

    /**
     * Start listening to the phone state change
     */
    public void start() {
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_RADIO_POWER_STATE_CHANGED);
    }

    /**
     * Stop listening to the phone state change
     */
    public void stop() {
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_NONE);
    }

    private void setAirplaneModeOn(boolean enabling) {
        // Change the system setting
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                enabling ? 1 : 0);

        // Notify listener the system setting is changed.
        if (mOnAirplaneModeChangedListener != null) {
            mOnAirplaneModeChangedListener.onAirplaneModeChanged(enabling);
        }

        // Post the intent
        final Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /**
     * Called when we've received confirmation that the airplane mode was set.
     */
    private void onAirplaneModeChanged() {
        if (mOnAirplaneModeChangedListener != null) {
            mOnAirplaneModeChangedListener.onAirplaneModeChanged(isAirplaneModeOn());
        }
    }

    /**
     * Check the status of ECM mode
     *
     * @return any subscription within device is under ECM mode
     */
    public boolean isInEcmMode() {
        if (mTelephonyManager.getEmergencyCallbackMode()) {
            return true;
        }
        SubscriptionManager subscriptionManager =
                mContext.getSystemService(SubscriptionManager.class);

        final List<SubscriptionInfo> subInfoList =
                getActiveSubscriptions(subscriptionManager);
        if (subInfoList == null) {
            return false;
        }
        for (SubscriptionInfo subInfo : subInfoList) {
            final TelephonyManager telephonyManager =
                    mTelephonyManager.createForSubscriptionId(subInfo.getSubscriptionId());
            if (telephonyManager != null) {
                if (telephonyManager.getEmergencyCallbackMode()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<SubscriptionInfo> getActiveSubscriptions(SubscriptionManager manager) {
        if (manager == null) {
            return Collections.emptyList();
        }
        final List<SubscriptionInfo> subscriptions = manager.getActiveSubscriptionInfoList();
        if (subscriptions == null) {
            return new ArrayList<>();
        }
        return subscriptions;
    }


    public void setAirplaneMode(boolean isAirplaneModeOn) {
        if (isInEcmMode()) {
            // In Emergency mode, do not update database at this point
            Log.d(LOG_TAG, "Emergency mode airplane mode=" + isAirplaneModeOn);
        } else {
            setAirplaneModeOn(isAirplaneModeOn);
        }
    }

    public void setAirplaneModeInEmergencyMode(boolean isEmergencyModeExit,
            boolean isAirplaneModeOn) {
        Log.d(LOG_TAG, "Exist Emergency Mode=" + isEmergencyModeExit +
                ", with airplane mode=" + isAirplaneModeOn);
        if (isEmergencyModeExit) {
            // update database based on the current checkbox state
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            // update summary
            onAirplaneModeChanged();
        }
    }

    public boolean isAirplaneModeOn() {
        return WirelessUtils.isAirplaneModeOn(mContext);
    }
}
