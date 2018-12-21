package com.cqkct.FunKidII.Ui.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialog;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cqkct.FunKidII.R;

public class ConfirmDialogFragment extends BaseBlurDialogFragment {

    private int mThemeResId = R.style.FunKidII_2_Dialog_style;

    private boolean mShouldBlur = true;

    private View mTitleLayout;
    protected TextView mTitleView;
    private CharSequence mTitleText;

    protected TextView mMessageView;
    private CharSequence mMessageText;

    protected Button mButtonPositive;
    private CharSequence mPositiveButtonText;
    private DialogInterface.OnClickListener mPositiveButtonListener;

    protected Button mButtonNegative;
    private CharSequence mNegativeButtonText;
    private DialogInterface.OnClickListener mNegativeButtonListener;

    @Override
    protected boolean shouldBlur() {
        return mShouldBlur;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            dismiss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AppCompatDialog dialog = new AppCompatDialog(getActivity(), mThemeResId);
        dialog.setContentView(R.layout.confirm_dialog_fragment_layout);

        mTitleLayout = dialog.findViewById(R.id.title_layout);
        mTitleView = dialog.findViewById(R.id.dialog_title);
        if (!TextUtils.isEmpty(mTitleText)) {
            mTitleView.setText(mTitleText);
            mTitleLayout.setVisibility(View.VISIBLE);
        }

        mMessageView = dialog.findViewById(R.id.message);
        if (mMessageText != null) {
            mMessageView.setText(Html.fromHtml(mMessageText.toString()));
        }
        mButtonPositive = dialog.findViewById(R.id.button_positive);
        if (mPositiveButtonText != null) {
            mButtonPositive.setText(mPositiveButtonText);
        }
        mButtonNegative = dialog.findViewById(R.id.button_negative);
        if (mNegativeButtonText != null) {
            mButtonNegative.setText(mNegativeButtonText);
        }

        mButtonPositive.setOnClickListener(v -> {
            if (mPositiveButtonListener != null) {
                mPositiveButtonListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
            }
            dismiss();
        });
        mButtonNegative.setOnClickListener(v -> {
            if (mNegativeButtonListener != null) {
                mNegativeButtonListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
            }
            dismiss();
        });

        onDialogCreated(dialog);

        return dialog;
    }

    protected void onDialogCreated(AppCompatDialog dialog) {}

    public ConfirmDialogFragment setThemeResId(int themeResId) {
        mThemeResId = themeResId;
        return this;
    }

    public ConfirmDialogFragment setBlur(boolean blur) {
        mShouldBlur = blur;
        return this;
    }

    public ConfirmDialogFragment setTitle(CharSequence title) {
        mTitleText = title;
        if (mTitleView != null) {
            if (TextUtils.isEmpty(mTitleText)) {
                mTitleLayout.setVisibility(View.GONE);
            } else {
                mTitleView.setText(mTitleText);
                mTitleLayout.setVisibility(View.VISIBLE);
            }
        }
        return this;
    }

    public ConfirmDialogFragment setMessage(CharSequence message) {
        mMessageText = message;
        if (mMessageView != null) {
            mMessageView.setText(mMessageText);
        }
        return this;
    }

    /**
     * @param message
     * @param changeColor
     * 改变部分字体颜色
     * @return
     */
    public ConfirmDialogFragment setMessage(CharSequence message,boolean changeColor) {
        mMessageText = message;
        if (mMessageView != null) {
            mMessageView.setText(Html.fromHtml(mMessageText.toString()));
        }
        return this;
    }

    public ConfirmDialogFragment setPositiveButton(CharSequence text, final DialogInterface.OnClickListener listener) {
        mPositiveButtonText = text;
        if (mButtonPositive != null) {
            mButtonPositive.setText(mPositiveButtonText);
        }
        mPositiveButtonListener = listener;
        return this;
    }

    public ConfirmDialogFragment setNegativeButton(CharSequence text, final DialogInterface.OnClickListener listener) {
        mNegativeButtonText = text;
        if (mButtonNegative != null) {
            mButtonNegative.setText(mNegativeButtonText);
        }
        mNegativeButtonListener = listener;
        return this;
    }
}
