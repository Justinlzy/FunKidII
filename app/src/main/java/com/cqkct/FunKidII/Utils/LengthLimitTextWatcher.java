package com.cqkct.FunKidII.Utils;

import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class LengthLimitTextWatcher implements TextWatcher {
    @NonNull
    private final EditText mEditText;
    private final int mMaxLength;

    public LengthLimitTextWatcher(@NonNull EditText editText, int maxLength) {
        mEditText = editText;
        mMaxLength = maxLength;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        int editStart = mEditText.getSelectionStart();
        int editEnd = mEditText.getSelectionEnd();

        // 先去掉监听器，否则会出现栈溢出
        mEditText.removeTextChangedListener(this);

        // 注意这里只能每次都对整个EditText的内容求长度，不能对删除的单个字符求长度
        // 因为是中英文混合，单个字符而言，calculateLength函数都会返回1
        int len;
        while ((len = Utils.charSequenceLength_zhCN(s.toString())) > mMaxLength) { // 当输入字符个数超过限制的大小时，进>行截断操作
            if (editStart == 0 && editEnd == 0) {
                int sub = 1;
                int diff = len - mMaxLength;
                if (diff > 10) {
                    sub = diff - 10;
                }
                int pos = s.length() - 1;
                s.delete(pos - sub, pos + 1);
            } else {
                s.delete(editStart - 1, editEnd);
                editStart--;
                editEnd--;
            }
        }
        mEditText.setSelection(editStart);

        // 恢复监听器
        mEditText.addTextChangedListener(this);
    }
}
