/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 * Changes from Qualcomm Innovation Center are provided under the following license:
 *
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.car.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.android.car.settings.R;
import com.android.car.settings.bluetooth.BluetoothA2dpDialogFragment
        .BluetoothA2dpDialogListener;
import com.android.car.settings.common.Logger;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A controller used by {@link BluetoothA2dpDialog} to manage connection state while we try to
 * set media player for a bluetooth device. It includes methods that allow the
 * {@link BluetoothA2dpDialogFragment} to interrogate the current state as well.
 */
public class BluetoothA2dpController implements BluetoothA2dpDialogListener {
    private final Context mContext;
    // Bluetooth dependencies for the connection we are trying to establish
    private LocalBluetoothManager mBluetoothManager;
    private BluetoothDevice mDevice;
    private String mUserInput;
    private String mDeviceName;
    private A2dpProfile mA2dpProfile;
    private final MediaPlayer mMediaPlayer;
    private List<ApplicationInfo> mApplicationList = null;
    private final boolean mStartMediaPlayer;

    /**
     * Creates an instance of a BluetoothA2dpController.
     */
    public BluetoothA2dpController(Intent intent, Context context) {
        mContext = Objects.requireNonNull(context);
        mBluetoothManager = BluetoothUtils.getLocalBtManager(context);
        mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (mBluetoothManager == null) {
            throw new IllegalStateException("Could not obtain LocalBluetoothManager");
        } else if (mDevice == null) {
            throw new IllegalStateException("Could not find BluetoothDevice");
        }

        mDeviceName = mBluetoothManager.getCachedDeviceManager().getName(mDevice);
        mA2dpProfile = mBluetoothManager.getProfileManager().getA2dpProfile();
        mMediaPlayer = new MediaPlayer(context);
        mStartMediaPlayer = context.getResources().getBoolean(
                R.bool.config_bluetooth_a2dp_start_media_player);
    }

    @Override
    public void onDialogMediaPlayerSelected(ApplicationInfo appInfo) {
        if (appInfo == null) {
            return;
        }
        handleMediaPlayer(appInfo.packageName);
    }

    @Override
    public void onDialogPositiveClick() {
        if (mA2dpProfile == null) {
            return;
        }
        // Get media player stored in Bluetooth process
        String mediaPlayer = mA2dpProfile.getMediaPlayer(mDevice);
        if (mediaPlayer.isEmpty()) {
            // Get default media player if it's not ever stored
            ApplicationInfo appInfo = getDefaultApplication();
            if (appInfo == null) {
                return;
            }
            mediaPlayer = appInfo.packageName;
        }
        handleMediaPlayer(mediaPlayer);
    }

    private void handleMediaPlayer(String mediaPlayer) {
        if (mA2dpProfile == null) {
            return;
        }
        if (!mA2dpProfile.setMediaPlayer(mDevice, mediaPlayer)) {
            return;
        }
        if (mStartMediaPlayer) {
            startMediaPlayer(mediaPlayer);
        }
    }

    private void startMediaPlayer(String mediaPlayer) {
        String mediaPlayerLaunch = mMediaPlayer.getMediaPlayerLaunch(mediaPlayer);
        Intent intent = mContext.getPackageManager().
                getLaunchIntentForPackage(mediaPlayerLaunch);
        if (intent == null) {
            return;
        }
        mContext.startActivity(intent);
    }

    /**
     * @return - A string containing the name provided by the device.
     */
    public String getDeviceName() {
        return mDeviceName;
    }

    public List<ApplicationInfo> getMediaPlayerList() {
        if (mApplicationList == null) {
            mApplicationList = mMediaPlayer.getApplicationInstalled();
        }
        return mApplicationList;
    }

    public ApplicationInfo getDefaultApplication() {
        return mMediaPlayer.getDefaultApplication(getMediaPlayerList());
    }

    public int getMediaPlayerIndex() {
        String mediaPlayer = mA2dpProfile.getMediaPlayer(mDevice);
        if (mediaPlayer.isEmpty()) {
            return -1;
        }
        List<ApplicationInfo> applicationList = getMediaPlayerList();
        for (int index = 0; index < applicationList.size(); index++) {
            ApplicationInfo appInfo = applicationList.get(index);
            if (appInfo.packageName.equals(mediaPlayer)) {
                return index;
            }
        }
        return -1;
    }

    private class MediaPlayer {
        private final String[] MEDIA_PLAYER_KEY_WORD = {
                "media", "player", "radio", "music", "youtube"
        };

        private static final String CAR_MEDIA = "com.android.car.media";
        private final String LOCAL_MEDIA_PLAYER = CAR_MEDIA + ".localmediaplayer";

        private final List<String> APPLICATION_FLITERED = new ArrayList<String>(Arrays.asList(
                /* Car Media Center */
                CAR_MEDIA,
                /* Radio */
                "com.android.car.radio",
                /* Android Open Source Music Player */
                "com.android.music",
                /* MusicFX */
                "com.android.musicfx",
                /* Multi-zone Audio Manager */
                "com.media.multizoneaudiomanager"));

        private final String PROVIDERS_PREFIX = "com.android.providers";

        private Context mContext;

        MediaPlayer(Context context) {
            mContext = context;
        }

        List<ApplicationInfo> getApplicationInstalled() {
            List<ApplicationInfo> mediaPlayers = new ArrayList<ApplicationInfo>();
            List<ApplicationInfo> apps = mContext.getPackageManager().
                    getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo appInfo : apps) {
                String packageName = appInfo.packageName;
                if (filterApplication(packageName)) {
                    // Ignore
                    continue;
                }
                for (String keyword : MEDIA_PLAYER_KEY_WORD) {
                    if (packageName.contains(keyword)) {
                        mediaPlayers.add(appInfo);
                        break;
                    }
                }
            }
            return mediaPlayers;
        }

        private boolean filterApplication(String packageName) {
            return packageName.startsWith(PROVIDERS_PREFIX) ||
                    APPLICATION_FLITERED.contains(packageName);
        }

        ApplicationInfo getDefaultApplication(List<ApplicationInfo> mediaPlayers) {
            for (ApplicationInfo appInfo : mediaPlayers) {
                String packageName = appInfo.packageName;
                if (packageName.equals(LOCAL_MEDIA_PLAYER)) {
                    return appInfo;
                }
            }
            return null;
        }

        String getMediaPlayerLaunch(String mediaPlayer) {
            return mediaPlayer.equals(LOCAL_MEDIA_PLAYER) ?
                    CAR_MEDIA :
                    mediaPlayer;
        }
    }
}
