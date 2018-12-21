package com.cqkct.FunKidII.Ui.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialog;
import android.view.View;

import com.cqkct.FunKidII.R;

public class ChooseAvatarWayDialogFragment extends BaseBlurDialogFragment {

    private View.OnClickListener mFromCameraOnClickListener;
    private View.OnClickListener mFromAlbumOnClickListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AppCompatDialog dialog = new AppCompatDialog(getActivity(), R.style.FunKidII_2_Dialog_style);
        dialog.setContentView(R.layout.choose_avatar_way_dialog_layout);
        dialog.findViewById(R.id.cancel).setOnClickListener(v -> dismiss());
        View mFromCamera = dialog.findViewById(R.id.take_photo);
        mFromCamera.setOnClickListener(v -> {
            if (mFromCameraOnClickListener != null) {
                mFromCameraOnClickListener.onClick(v);
            }
            dismiss();
        });
        View mFromAlbum = dialog.findViewById(R.id.album);
        mFromAlbum.setOnClickListener(v -> {
            if (mFromAlbumOnClickListener != null) {
                mFromAlbumOnClickListener.onClick(v);
            }
            dismiss();
        });
        return dialog;
    }

    public ChooseAvatarWayDialogFragment setFromCameraOnClickListener(View.OnClickListener listener) {
        mFromCameraOnClickListener = listener;
        return this;
    }

    public ChooseAvatarWayDialogFragment setFromAlbumOnClickListener(View.OnClickListener listener) {
        mFromAlbumOnClickListener = listener;
        return this;
    }
}
