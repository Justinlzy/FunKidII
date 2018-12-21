package com.cqkct.FunKidII.Ui.fragment;

import android.annotation.SuppressLint;
import android.support.annotation.StringRes;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import fr.tvbarthel.lib.blurdialogfragment.SupportBlurDialogFragment;

public class BaseBlurDialogFragment extends SupportBlurDialogFragment {
    private Toast mToast;

    @Override
    public void onResume() {
        super.onResume();

        MobclickAgent.onPageStart(getClass().getName());
    }

    @Override
    public void onPause() {
        super.onPause();

        MobclickAgent.onPageEnd(getClass().getName());
    }

    @SuppressLint("ShowToast")
    public void toast(int msgResId, int duration) {
        synchronized (this) {
            if (mToast == null) {
                mToast = Toast.makeText(getContext(), msgResId, duration);
            } else {
                mToast.setDuration(duration);
                mToast.setText(msgResId);
            }
        }
//        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    public void toast(@StringRes int msgResId) {
        toast(msgResId, Toast.LENGTH_SHORT);
    }

    @SuppressLint("ShowToast")
    public void toast(String msg, int duration) {
        synchronized (this) {
            if (mToast == null) {
                mToast = Toast.makeText(getContext(), msg, duration);
            } else {
                mToast.setDuration(duration);
                mToast.setText(msg);
            }
        }
//        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    public void toast(String msg) {
        toast(msg, Toast.LENGTH_SHORT);
    }
}
