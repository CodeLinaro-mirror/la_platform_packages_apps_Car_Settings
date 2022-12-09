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

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.UserHandle;

/**
 * BluetoothA2dpRequest is a receiver for Bluetooth A2DP set media player intent.
 */
public final class BluetoothA2dpRequest extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!action.equals(BluetoothA2dp.ACTION_SET_MEDIA_PLAYER)) {
            return;
        }
        // convert broadcast intent into activity intent (same action string)
        Intent a2dpIntent = getA2dpIntent(context, intent);

        PowerManager powerManager =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        BluetoothDevice device =
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        String deviceAddress = device != null ? device.getAddress() : null;
        String deviceName = device != null ? device.getName() : null;
        boolean shouldShowDialog = BluetoothUtils.shouldShowDialogInForeground(
                context, deviceAddress, deviceName);
        if (powerManager.isInteractive() && shouldShowDialog) {
            // Since the screen is on and the BT-related activity is in the foreground,
            // just open the dialog
            context.startActivityAsUser(a2dpIntent, UserHandle.CURRENT);
        } else {
            // TODO
        }
    }

    private static Intent getA2dpIntent(Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        Intent a2dpIntent = new Intent();
        a2dpIntent.setClass(context, BluetoothA2dpDialog.class);
        a2dpIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        a2dpIntent.setAction(BluetoothA2dp.ACTION_SET_MEDIA_PLAYER);
        return a2dpIntent;
    }
}
