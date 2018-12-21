package com.cqkct.FunKidII.Ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class CustomDialog extends Dialog {
    public CustomDialog(@NonNull Context context) {
        super(context);
    }

    public CustomDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected CustomDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    private volatile InputMethodManager mInputMethodManager;

    public InputMethodManager getInputMethodManager() {
        if (mInputMethodManager == null) {
            synchronized (this) {
                if (mInputMethodManager == null) {
                    mInputMethodManager = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
                }
            }
        }
        return mInputMethodManager;
    }

    @Override
    public void dismiss() {
        View localView = getCurrentFocus();
        if (localView != null) {
            InputMethodManager imm = getInputMethodManager();
            if (imm != null) {
                imm.hideSoftInputFromWindow(localView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
        super.dismiss();
    }
}
