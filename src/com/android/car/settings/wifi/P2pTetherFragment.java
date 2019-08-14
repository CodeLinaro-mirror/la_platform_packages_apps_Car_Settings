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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;

import androidx.annotation.LayoutRes;
import androidx.annotation.XmlRes;

import com.android.car.settings.R;
import com.android.car.settings.common.SettingsFragment;

/**
 * Fragment to host tethering-related preferences.
 */
public class P2pTetherFragment extends SettingsFragment implements Switch.OnCheckedChangeListener {

    private static final String TAG = "P2pTetherFragment";
    private CarWifiManager mCarWifiManager;
    private ConnectivityManager mConnectivityManager;
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private ProgressBar mProgressBar;
    private Switch mTetherSwitch;

/*    private final ConnectivityManager.OnStartTetheringCallback mOnStartTetheringCallback =
            new ConnectivityManager.OnStartTetheringCallback() {
                @Override
                public void onTetheringFailed() {
                    super.onTetheringFailed();
                    mTetherSwitch.setChecked(false);
                    mTetherSwitch.setEnabled(true);
                }
            };
*/

    @Override
    @LayoutRes
    protected int getActionBarLayoutId() {
        return R.layout.action_bar_with_toggle;
    }

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.p2p_tether_fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mCarWifiManager = new CarWifiManager(context);
        mConnectivityManager = (ConnectivityManager) getContext().getSystemService(
                Context.CONNECTIVITY_SERVICE);
        mWifiP2pManager = (WifiP2pManager) getContext().getSystemService(
                Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(getContext(), Looper.getMainLooper(), null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mProgressBar = getActivity().findViewById(R.id.progress_bar);
        mTetherSwitch = getActivity().findViewById(R.id.toggle_switch);
        setupTetherSwitch();
    }

    @Override
    public void onStart() {
        super.onStart();
//        mCarWifiManager.start();
        getContext().registerReceiver(mReceiver,
                new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        getContext().registerReceiver(mReceiver,
                new IntentFilter(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION));
    }

    @Override
    public void onStop() {
        super.onStop();
//        mCarWifiManager.stop();
        getContext().unregisterReceiver(mReceiver);
        mProgressBar.setVisibility(View.GONE);
    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        mCarWifiManager.destroy();
//    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (!isChecked) {
            Log.d(TAG, "stop P2P Tethering...");
            mConnectivityManager.stopTethering(ConnectivityManager.TETHERING_P2P);
        } else {
            Log.d(TAG, "start P2P Tethering...");
            mConnectivityManager.startTethering(ConnectivityManager.TETHERING_P2P,
                true /* showProvisioningUi */,
                new ConnectivityManager.OnStartTetheringCallback() {
                    public void onTetheringStarted() {
                        Log.e(TAG, "Start Tether success");
                    }
                    public void onTetheringFailed() {
                        Log.e(TAG, "Start Tether failure");
                        mTetherSwitch.setChecked(false);
                        mTetherSwitch.setEnabled(true);
                    }
                }, new Handler(Looper.getMainLooper()));
        }
    }

    protected void setupTetherSwitch() {
        mWifiP2pManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                boolean checked;
                Log.d(TAG, "onConnectionInfoAvailable() - info=" + info);

                if (info == null || !info.groupFormed || !info.isGroupOwner || !info.tetherable) {
                    checked = false;
                } else {
                    checked = true;
                }
                setupTetherSwitch2(checked);
            }
        });
    }

    protected void setupTetherSwitch2(boolean checked) {
        mTetherSwitch.setChecked(checked);
        mTetherSwitch.setOnCheckedChangeListener(this);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                handleWifiStateChanged(mCarWifiManager.getWifiState());
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                setupTetherSwitch();
            }
        }
    };

    private void handleWifiStateChanged(int state) {
        // TODO: necessary to disable whole UI?
    }
}
