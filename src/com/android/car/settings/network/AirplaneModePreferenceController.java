/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.car.settings.network;

import static android.provider.SettingsSlicesContract.KEY_AIRPLANE_MODE;

import android.car.drivingstate.CarUxRestrictions;
import com.android.car.ui.preference.CarUiTwoActionSwitchPreference;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.AirplaneModeEnabler;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

public class AirplaneModePreferenceController extends
        PreferenceController<CarUiTwoActionSwitchPreference>
        implements AirplaneModeEnabler.OnAirplaneModeChangedListener {

    public static final int REQUEST_CODE_EXIT_ECM = 1;
    private Fragment mFragment;
    private Context mContext;
    private AirplaneModeEnabler mAirplaneModeEnabler;

    public AirplaneModePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mContext = context;
        mAirplaneModeEnabler = new AirplaneModeEnabler(mContext, this);
    }

    @Override
    protected Class<CarUiTwoActionSwitchPreference> getPreferenceType() {
        return CarUiTwoActionSwitchPreference.class;
    }

    public void setFragment(Fragment hostFragment) {
        mFragment = hostFragment;
    }

    @VisibleForTesting
    void setAirplaneModeEnabler(AirplaneModeEnabler airplaneModeEnabler) {
        mAirplaneModeEnabler = airplaneModeEnabler;
    }

    @Override
    public boolean handlePreferenceClicked(CarUiTwoActionSwitchPreference preference) {
        if (KEY_AIRPLANE_MODE.equals(preference.getKey()) && mAirplaneModeEnabler.isInEcmMode()) {
            // In ECM mode launch ECM app dialog
            if (mFragment != null) {
                mFragment.startActivityForResult(
                        new Intent(TelephonyManager.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                        REQUEST_CODE_EXIT_ECM);
            }
            return true;
        } else {
            // Click on left or right side element should change airplane mode state.
            setChecked(!getPreference().isSecondaryActionChecked());
        }
        return false;
    }

    public static boolean isAvailable(Context context) {
        return context.getResources().getBoolean(R.bool.config_show_toggle_airplane)
                && !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    @Override
    public int getAvailabilityStatus() {
        return isAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        // First time initialization
        getPreference().setSecondaryActionChecked(mAirplaneModeEnabler.isAirplaneModeOn());
        getPreference().setOnSecondaryActionClickListener(isChecked -> {
            setChecked(isChecked);
        });
    }

    @Override
    protected void onStartInternal() {
        mAirplaneModeEnabler.start();
    }

    @Override
    protected void onStopInternal() {
        mAirplaneModeEnabler.stop();
    }

    @Override
    protected void onDestroyInternal() {
        mAirplaneModeEnabler.close();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EXIT_ECM) {
            final boolean isChoiceYes = (resultCode == Activity.RESULT_OK);
            // Set Airplane mode based on the return value and checkbox state
            mAirplaneModeEnabler.setAirplaneModeInEmergencyMode(isChoiceYes,
                    getPreference().isSecondaryActionChecked());
        }
    }

    public boolean isChecked() {
        return mAirplaneModeEnabler.isAirplaneModeOn();
    }

    public boolean setChecked(boolean isChecked) {
        if (isChecked() == isChecked) {
            return false;
        }
        mAirplaneModeEnabler.setAirplaneMode(isChecked);
        return true;
    }

    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        getPreference().setSecondaryActionChecked(isAirplaneModeOn);
    }
}
