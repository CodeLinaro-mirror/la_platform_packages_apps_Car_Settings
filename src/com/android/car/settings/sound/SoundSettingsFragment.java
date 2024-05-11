/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.settings.sound;

import static android.car.hardware.power.PowerComponent.AUDIO;

import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.annotation.XmlRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.R;
import com.android.car.settings.common.PowerPolicyListener;
import com.android.car.settings.common.SettingsFragment;
import com.android.car.settings.search.CarBaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import android.os.Bundle;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/** Fragment which shows the settings for sounds. */
@SearchIndexable
public class SoundSettingsFragment extends SettingsFragment {

    private PreferenceScreen mScreen;
    private boolean mIsPowerPolicyOn = true;

    @VisibleForTesting
    PowerPolicyListener mPowerPolicyListener;

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.sound_settings_fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Instantiating the PowerPolicyListener class
        mPowerPolicyListener = new PowerPolicyListener(context, AUDIO,
                isPowerOn -> {
                    mIsPowerPolicyOn = isPowerOn;
                    if (mScreen != null) {
                        if (!mIsPowerPolicyOn) {
                            Toast.makeText(getContext(),
                                    getContext().getString(R.string.power_component_disabled),
                                    Toast.LENGTH_LONG).show();
                        }
                        // Enabling/Disabling the sound setting screen based on power policy action.
                        mScreen.setEnabled(mIsPowerPolicyOn);
                    }
                });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        mScreen = getPreferenceScreen();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Executes the registered call back with the current power policy.
        mPowerPolicyListener.handleCurrentPolicy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPowerPolicyListener.release();
    }

    public static final CarBaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new CarBaseSearchIndexProvider(R.xml.sound_settings_fragment,
                    Settings.ACTION_SOUND_SETTINGS);
}
