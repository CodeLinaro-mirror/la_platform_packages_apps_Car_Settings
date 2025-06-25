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

package com.android.car.settings.applications.appinfo;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.UserHandle;

import androidx.preference.PreferenceGroup;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceController;
import com.android.car.settings.common.RadioWithImagePreference;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A PreferenceController handling the logic for selecting an aspect ratio.
 */
public class AppAspectRatiosGroupPreferenceController extends
        PreferenceController<PreferenceGroup> implements RadioWithImagePreference.OnClickListener {
    private static final Logger LOG = new Logger(AppAspectRatiosGroupPreferenceController.class);
    private static final String KEY_PREF_DEFAULT = "app_unset_pref";
    private static final String KEY_PREF_FULLSCREEN = "fullscreen_pref";
    private static final String KEY_PREF_HALF_SCREEN = "half_screen_pref";
    private static final String KEY_PREF_DISPLAY_SIZE = "display_size_pref";
    private static final String KEY_PREF_16_9 = "16_9_pref";
    private static final String KEY_PREF_4_3 = "4_3_pref";
    private static final String KEY_PREF_3_2 = "3_2_pref";
    private static Map<String, Integer> sKeyToAspectRatioMap = new HashMap<>();

    static {
        sKeyToAspectRatioMap = new HashMap<>();
        sKeyToAspectRatioMap.put(KEY_PREF_DEFAULT, PackageManager.USER_MIN_ASPECT_RATIO_UNSET);
        sKeyToAspectRatioMap.put(KEY_PREF_FULLSCREEN,
                PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN);
        sKeyToAspectRatioMap.put(KEY_PREF_HALF_SCREEN,
                PackageManager.USER_MIN_ASPECT_RATIO_SPLIT_SCREEN);
        sKeyToAspectRatioMap.put(KEY_PREF_DISPLAY_SIZE,
                PackageManager.USER_MIN_ASPECT_RATIO_DISPLAY_SIZE);
        sKeyToAspectRatioMap.put(KEY_PREF_4_3, PackageManager.USER_MIN_ASPECT_RATIO_4_3);
        sKeyToAspectRatioMap.put(KEY_PREF_16_9, PackageManager.USER_MIN_ASPECT_RATIO_16_9);
        sKeyToAspectRatioMap.put(KEY_PREF_3_2, PackageManager.USER_MIN_ASPECT_RATIO_3_2);
    }
    private List<RadioWithImagePreference> mPreferenceList;
    private String mSelectedKey = KEY_PREF_DEFAULT;
    private String mPackageName;
    private int mUserId;
    private AspectRatioManager mAspectRatioManager;

    public AppAspectRatiosGroupPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions restrictionInfo) {
        super(context, preferenceKey, fragmentController, restrictionInfo);
        mPreferenceList = new ArrayList<>();
        mUserId = UserHandle.myUserId();
        mAspectRatioManager = new AspectRatioManager(context);
    }

    /**
     * Set the packageName, which is used to perform actions on a particular package.
     */
    public AppAspectRatiosGroupPreferenceController setPackageName(String packageName) {
        mPackageName = packageName;
        return this;
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }

    @Override
    protected void onCreateInternal() {
        int currentAspectRatio = PackageManager.USER_MIN_ASPECT_RATIO_UNSET;
        try {
            currentAspectRatio = mAspectRatioManager.getUserMinAspectRatioValue(mPackageName,
                    mUserId);
        } catch (RemoteException e) {
            LOG.d("There is an exception when trying to get the current aspect ratio: " + e);
        }

        mSelectedKey = getSelectedAspectRatioKey(currentAspectRatio);
        for (int i = 0; i < getPreference().getPreferenceCount(); i++) {
            RadioWithImagePreference child =
                    (RadioWithImagePreference) getPreference().getPreference(i);
            mPreferenceList.add(child);
            child.setOnClickListener(this);
        }
    }

    @Override
    protected void updateState(PreferenceGroup preference) {
        super.updateState(preference);
        for (RadioWithImagePreference child : mPreferenceList) {
            child.setChecked(child.getKey().equals(mSelectedKey));
        }
    }

    @Override
    public void onRadioButtonClicked(RadioWithImagePreference selected) {
        String selectedKey = selected.getKey();
        if (selectedKey.equals(mSelectedKey)) {
            return;
        }

        int userAspectRatio = sKeyToAspectRatioMap.getOrDefault(selectedKey,
                PackageManager.USER_MIN_ASPECT_RATIO_UNSET);

        try {
            getAspectRatioManager().setUserMinAspectRatio(mPackageName, mUserId, userAspectRatio);
            mSelectedKey = selectedKey;
        } catch (RemoteException e) {
            LOG.e("Unable to set user min aspect ratio");
            return;
        }
        updateState(getPreference());
    }

    @VisibleForTesting
    AspectRatioManager getAspectRatioManager() {
        return mAspectRatioManager;
    }

    private static String getSelectedAspectRatioKey(int selectedKey) {
        switch (selectedKey) {
            case PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN:
                return KEY_PREF_FULLSCREEN;
            case PackageManager.USER_MIN_ASPECT_RATIO_SPLIT_SCREEN:
                return KEY_PREF_HALF_SCREEN;
            case PackageManager.USER_MIN_ASPECT_RATIO_4_3:
                return KEY_PREF_4_3;
            case PackageManager.USER_MIN_ASPECT_RATIO_16_9:
                return KEY_PREF_16_9;
            case PackageManager.USER_MIN_ASPECT_RATIO_3_2:
                return KEY_PREF_3_2;
            case PackageManager.USER_MIN_ASPECT_RATIO_DISPLAY_SIZE:
                return KEY_PREF_DISPLAY_SIZE;
            case PackageManager.USER_MIN_ASPECT_RATIO_UNSET:
            default:
                return KEY_PREF_DEFAULT;
        }
    }
}
