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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
import com.android.car.ui.preference.CarUiDialogFragment;
import com.android.internal.annotations.VisibleForTesting;

import java.util.List;

/**
 * A dialogFragment used by {@link BluetoothA2dpDialog} to create an appropriately styled dialog
 * for the bluetooth device.
 */
public class BluetoothA2dpDialogFragment extends CarUiDialogFragment implements
        OnClickListener {

    private static final Logger LOG = new Logger(BluetoothA2dpDialogFragment.class);

    private AlertDialog.Builder mBuilder;
    private AlertDialog mDialog;
    private BluetoothA2dpController mA2dpController;
    private BluetoothA2dpDialog mA2dpDialogActivity;
    private MediaPlayerAdapter mMediaPlayerAdapter;

    /**
     * The interface we expect a listener to implement. Typically this should be done by
     * the controller.
     */
    public interface BluetoothA2dpDialogListener {

        void onDialogMediaPlayerSelected(ApplicationInfo appInfo);

        void onDialogPositiveClick();
    }

    private final AdapterView.OnItemClickListener mListViewClickListener =
            new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
            ApplicationInfo appInfo = (ApplicationInfo) parent.getItemAtPosition(position);
            mA2dpController.onDialogMediaPlayerSelected(appInfo);
            mA2dpDialogActivity.dismiss();
        }
    };

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!isA2dpControllerSet()) {
            throw new IllegalStateException(
                "Must call setA2dpController() before showing dialog");
        }
        if (!isA2dpDialogActivitySet()) {
            throw new IllegalStateException(
                "Must call setPairingDialogActivity() before showing dialog");
        }
        mMediaPlayerAdapter = createMediaPlayerAdapter();

        mBuilder = new AlertDialog.Builder(getActivity());
        mDialog = setupDialog();
        mDialog.setCanceledOnTouchOutside(false);
        return mDialog;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mA2dpController.onDialogPositiveClick();
        }
        mA2dpDialogActivity.dismiss();
    }

    /**
     * Sets the controller that the fragment should use. this method MUST be called
     * before you try to show the dialog or an error will be thrown. An implementation
     * of a pairing controller can be found at {@link BluetoothA2dpController}. A
     * controller may not be substituted once it is assigned. Forcibly switching a
     * controller for a new one will lead to undefined behavior.
     */
    void setA2dpController(BluetoothA2dpController a2dpController) {
        if (isA2dpControllerSet()) {
            throw new IllegalStateException("The controller can only be set once. "
                    + "Forcibly replacing it will lead to undefined behavior");
        }
        mA2dpController = a2dpController;
    }

    /**
     * Checks whether mA2dpController is set
     * @return True when mA2dpController is set, False otherwise
     */
    boolean isA2dpControllerSet() {
        return mA2dpController != null;
    }

    /**
     * Sets the BluetoothA2dpDialog activity that started this fragment
     * @param a2dpDialogActivity The pairing dialog activty that started this fragment
     */
    void setA2dpDialogActivity(BluetoothA2dpDialog a2dpDialogActivity) {
        if (isA2dpDialogActivitySet()) {
            throw new IllegalStateException("The A2DP dialog activity can only be set once");
        }
        mA2dpDialogActivity = a2dpDialogActivity;
    }

    /**
     * Checks whether mA2dpDialogActivity is set
     * @return True when mA2dpDialogActivity is set, False otherwise
     */
    boolean isA2dpDialogActivitySet() {
        return mA2dpDialogActivity != null;
    }

    /**
     * Creates the appropriate type of dialog and returns it.
     */
    private AlertDialog setupDialog() {
        return createConfirmationDialog();
    }

    /**
     * Creates a dialog with UI elements that allow the user to confirm A2DP
     * set media player request.
     */
    private AlertDialog createConfirmationDialog() {
        mBuilder.setTitle(getString(R.string.bluetooth_a2dp_set_media_player_request,
                mA2dpController.getDeviceName()));
        mBuilder.setView(createView());
        mBuilder.setPositiveButton(getString(R.string.okay), this);
        AlertDialog dialog = mBuilder.create();
        return dialog;
    }

    /**
     * Creates a custom view for dialogs which need to show users additional information but do
     * not require user input.
     */
    private View createView() {
        View view = (View) getActivity().getLayoutInflater().inflate(
                R.layout.bluetooth_a2dp_media_player, /* root= */ null);
        view.setFocusable(false);
        view.setVisibility(View.VISIBLE);

        ListView mediaPlayerList = (ListView) view.findViewById(
                R.id.media_player_list);
        mediaPlayerList.setAdapter(mMediaPlayerAdapter);
        mediaPlayerList.setOnItemClickListener(mListViewClickListener);
        return view;
    }

    private MediaPlayerAdapter createMediaPlayerAdapter() {
        MediaPlayerAdapter adapter = new MediaPlayerAdapter(getActivity(),
                mA2dpController.getMediaPlayerIndex());
        List<ApplicationInfo> appList = mA2dpController.getMediaPlayerList();
        adapter.addAll(appList);
        return adapter;
    }

    private class MediaPlayerAdapter extends ArrayAdapter<ApplicationInfo> {
        private static final String TAG = "MediaPlayerAdapter";

        private Context mContext;
        private int mSelection = -1;

        class MediaPlayerHolder {
            public TextView mAppName;
            public ImageView mAppIcon;
        }

        MediaPlayerAdapter(Context context, int selection) {
            super(context, R.layout.bluetooth_a2dp_media_player_row);
            mContext = context;
            mSelection = selection;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                rowView = getLayoutInflater().inflate(R.layout.bluetooth_a2dp_media_player_row, null);
                MediaPlayerHolder holder = new MediaPlayerHolder();
                holder.mAppName = (TextView) rowView.findViewById(R.id.media_player_title);
                holder.mAppIcon = (ImageView) rowView.findViewById(R.id.media_player_icon);
                rowView.setTag(holder);
            }

            MediaPlayerHolder holder = (MediaPlayerHolder) rowView.getTag();
            ApplicationInfo appInfo = getItem(position);
            PackageManager pm = mContext.getPackageManager();
            holder.mAppName.setText(appInfo.loadLabel(pm));
            holder.mAppName.setBackgroundColor(position == mSelection ?
                    mContext.getColor(R.color.bluetooth_a2dp_media_player_background_color) :
                    Color.TRANSPARENT);
            holder.mAppIcon.setImageDrawable(appInfo.loadIcon(pm));

            return rowView;
        }
    }
}
