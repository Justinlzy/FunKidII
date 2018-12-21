package com.cqkct.FunKidII.service.tlc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cqkct.FunKidII.service.Pkt;

//import java.lang.ref.WeakReference;

public class ExecEntity {

    TLC mTlc;
    Conn mConn;

    @NonNull
    final Pkt request;

    final long timeoutMillis;

//    final WeakReference<TlcService.OnExecListener> listener;
    @Nullable
    final TlcService.OnExecListener listener;

    @Nullable
    final Long thirdStageTimeoutMillis;

    @Nullable
    Pkt response;

    @Nullable
    Throwable cause;

    @Nullable
    Pkt thirdStageEvent;


    ExecEntity(@NonNull Pkt request, long timeoutMillis, @Nullable TlcService.OnExecListener listener, @Nullable Long thirdStageTimeoutMillis) {
        this.request = request;
        this.timeoutMillis = timeoutMillis;
//        this.listener = new WeakReference<>(listener);
        this.listener = listener;
        this.thirdStageTimeoutMillis = thirdStageTimeoutMillis;
    }

    @Override
    public String toString() {
        return request.toString() + " " + timeoutMillis + " " + listener + " " + thirdStageTimeoutMillis;
    }

    /**
     * 取消执行
     * @return 整个或失败
     */
    public boolean cancel() {
        return mTlc != null && mTlc.cancelExec(this);
    }
}