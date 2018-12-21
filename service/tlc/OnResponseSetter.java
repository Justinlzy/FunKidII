package com.cqkct.FunKidII.service.tlc;

import android.support.annotation.Nullable;

import com.google.protobuf.GeneratedMessageV3;

/**
 * 回响设置器，用于发送回响
 */
public interface OnResponseSetter {
    /**
     * 设置回响
     *
     * @param rspMsg 回响数据的 protobuf
     */
    void setResponse(@Nullable GeneratedMessageV3 rspMsg);
}