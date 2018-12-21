package com.cqkct.FunKidII.glide;

import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;

public interface Avatar {
    @Nullable String getFilename();
    @Nullable String getAuthToken();
    @Nullable String getResourceToken();
    @DrawableRes int getAlternate();
    @DrawableRes int getDefault();
}