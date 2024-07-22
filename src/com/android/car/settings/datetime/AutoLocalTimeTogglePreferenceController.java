/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.car.settings.datetime;

import android.app.time.TimeConfiguration;
import android.app.time.TimeManager;
import android.app.time.TimeZoneConfiguration;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;

import androidx.preference.SwitchPreference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

/**
 * Business logic which controls the auto local time toggle.
 */
public class AutoLocalTimeTogglePreferenceController extends
        PreferenceController<SwitchPreference> {
    private final TimeManager mTimeManager;

    public AutoLocalTimeTogglePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mTimeManager = context.getSystemService(TimeManager.class);
    }

    @Override
    protected Class<SwitchPreference> getPreferenceType() {
        return SwitchPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        setClickableWhileDisabled(getPreference(), /* clickable= */ true, p ->
                DatetimeUtils.runClickableWhileDisabled(getContext(), getFragmentController()));
    }

    @Override
    protected void updateState(SwitchPreference preference) {
        preference.setChecked(DatetimeUtils.isAutoLocalTimeDetectionEnabled(mTimeManager));
    }

    @Override
    protected boolean handlePreferenceChanged(SwitchPreference preference, Object newValue) {
        if (!DatetimeUtils.isAutoTimeDetectionCapabilityPossessed(mTimeManager)
                || !DatetimeUtils.isAutoTimeZoneDetectionCapabilityPossessed(mTimeManager)) {
            return false;
        }

        boolean isAutoLocalTimeEnabled = (boolean) newValue;
        mTimeManager.updateTimeConfiguration(new TimeConfiguration.Builder()
                .setAutoDetectionEnabled(isAutoLocalTimeEnabled).build());
        mTimeManager.updateTimeZoneConfiguration(new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(isAutoLocalTimeEnabled).build());
        getContext().sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED));
        return true;
    }

    @Override
    public int getDefaultAvailabilityStatus() {
        return DatetimeUtils.getAvailabilityStatus(getContext());
    }
}
