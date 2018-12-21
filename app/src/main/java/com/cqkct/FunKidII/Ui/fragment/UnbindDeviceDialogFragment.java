package com.cqkct.FunKidII.Ui.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.cqkct.FunKidII.R;

public class UnbindDeviceDialogFragment extends DialogFragment {
    String deviceId;
    MoreFragment fragment;
    OnUnbindClickListener listener;

    public static UnbindDeviceDialogFragment newInstance(@NonNull String deviceId) {
        UnbindDeviceDialogFragment newFragment = new UnbindDeviceDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString("deviceId", deviceId);
        newFragment.setArguments(bundle);
        return newFragment;
    }

    UnbindDeviceDialogFragment setUnbindClickListener(OnUnbindClickListener onUnbindClickListener){
        this.listener = onUnbindClickListener;
        return this;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            deviceId = args.getString("deviceId");
        }

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.unbind)
                .setMessage(R.string.unbind_owner_device_tip)
                .setPositiveButton(R.string.unbind_short, (dialog, id) -> listener.onPositiveButton())
                .setNegativeButton(R.string.cancel, null);
        return builder.create();
    }
    interface OnUnbindClickListener {
        void onPositiveButton();
    }
}