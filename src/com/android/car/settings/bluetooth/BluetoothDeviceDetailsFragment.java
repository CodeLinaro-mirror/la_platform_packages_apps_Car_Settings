/*
 * Copyright 2018 The Android Open Source Project
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
 * Changes from Qualcomm Technologies, Inc. are provided under the following license:
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * SPDX-License-Identifier: BSD-3-Clause-Clear.
 */

package com.android.car.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapterUtil;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.XmlRes;

import com.android.car.settings.R;
import com.android.car.settings.common.SettingsFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/**
 * Page which displays information for a remote Bluetooth device including the name, icon,
 * connection status, supported profiles, and MAC.
 */
public class BluetoothDeviceDetailsFragment extends SettingsFragment {

    private static final String KEY_DEVICE_ADDRESS = "device_address";
    private static final String KEY_ADAPTER_INDEX = "adapter_index";

    private CachedBluetoothDevice mCachedDevice;

    /**
     * Returns a new {@link BluetoothDeviceDetailsFragment} for the given {@code device}.
     */
    public static BluetoothDeviceDetailsFragment newInstance(CachedBluetoothDevice device) {
        Bundle args = new Bundle();
        args.putString(KEY_DEVICE_ADDRESS, device.getAddress());
        args.putInt(KEY_ADAPTER_INDEX, device.getDevice().getAdapterIndex());
        BluetoothDeviceDetailsFragment fragment = new BluetoothDeviceDetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_device_details_fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        LocalBluetoothManager manager = BluetoothUtils.getLocalBtManager(context);
        if (manager == null) {
            goBack();
            return;
        }
        String deviceAddress = getArguments().getString(KEY_DEVICE_ADDRESS);
        int adapterIndex = getArguments().getInt(KEY_ADAPTER_INDEX, 0);
        BluetoothAdapter adapter = adapterIndex == 0
                ? BluetoothAdapter.getDefaultAdapter()
                : BluetoothAdapterUtil.getNewAdapter();
        BluetoothDevice remoteDevice = adapter.getRemoteDevice(deviceAddress);
        mCachedDevice = manager.getCachedDeviceManager().findDevice(remoteDevice);
        if (mCachedDevice == null) {
            goBack();
            return;
        }

        use(BluetoothDeviceNamePreferenceController.class,
                R.string.pk_bluetooth_device_name).setCachedDevice(mCachedDevice);
        use(BluetoothDeviceActionButtonsPreferenceController.class,
                R.string.pk_bluetooth_device_action_buttons).setCachedDevice(mCachedDevice);
        use(BluetoothDeviceProfilesPreferenceController.class,
                R.string.pk_bluetooth_device_profiles).setCachedDevice(mCachedDevice);
        use(BluetoothDeviceAddressPreferenceController.class,
                R.string.pk_bluetooth_device_address).setCachedDevice(mCachedDevice);
    }
}
