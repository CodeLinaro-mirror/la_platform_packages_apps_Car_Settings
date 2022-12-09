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

import android.annotation.Nullable;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.android.settingslib.core.lifecycle.HideNonSystemOverlayMixin;

/**
 * BluetoothA2dpDialog asks the user to select media player to map with
 * audio zone. It is an activity that appears as a dialog.
 */
public class BluetoothA2dpDialog extends FragmentActivity {
    public static final String FRAGMENT_TAG = "bluetooth.a2dp.fragment";

    private BluetoothA2dpController mBluetoothA2dpController;
    private boolean mReceiverRegistered = false;

    /**
     * Dismiss the dialog if Bluetooth is turned off/on, or if it is
     * canceled for {@link BluetoothA2dpController#mDevice}.
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.STATE_OFF);
                if (state == BluetoothAdapter.STATE_OFF ||
                        state == BluetoothAdapter.STATE_ON) {
                    dismiss();
                }
            } else if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                        BluetoothProfile.STATE_DISCONNECTED);
                if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    dismiss();
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_NONE) {
                    dismiss();
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLifecycle().addObserver(new HideNonSystemOverlayMixin(this));

        Intent intent = getIntent();
        mBluetoothA2dpController = new BluetoothA2dpController(intent, this);
        // build the dialog fragment
        boolean fragmentFound = true;
        // check if the fragment has been preloaded
        BluetoothA2dpDialogFragment bluetoothFragment =
                (BluetoothA2dpDialogFragment) getSupportFragmentManager().findFragmentByTag(
                        FRAGMENT_TAG);
        // dismiss the fragment if it is already used
        if (bluetoothFragment != null && (bluetoothFragment.isA2dpControllerSet()
                || bluetoothFragment.isA2dpDialogActivitySet())) {
            bluetoothFragment.dismiss();
            bluetoothFragment = null;
        }
        // build a new fragment if it is null
        if (bluetoothFragment == null) {
            fragmentFound = false;
            bluetoothFragment = new BluetoothA2dpDialogFragment();
        }
        bluetoothFragment.setA2dpController(mBluetoothA2dpController);
        bluetoothFragment.setA2dpDialogActivity(this);
        // pass the fragment to the manager when it is created from scratch
        if (!fragmentFound) {
            bluetoothFragment.show(getSupportFragmentManager(), FRAGMENT_TAG);
        }
        /*
         * Leave this registered through pause/resume since we still want to
         * finish the activity in the background if set media player request
         * is canceled.
         */
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(mReceiver, new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED));
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        mReceiverRegistered = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiverRegistered) {
            mReceiverRegistered = false;
            unregisterReceiver(mReceiver);
        }
    }

    @VisibleForTesting
    void dismiss() {
        if (!isFinishing()) {
            finish();
        }
    }
}
