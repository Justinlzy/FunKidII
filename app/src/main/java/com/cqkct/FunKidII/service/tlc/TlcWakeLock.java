package com.cqkct.FunKidII.service.tlc;

import android.os.PowerManager;

import com.cqkct.FunKidII.Utils.L;

import java.util.HashSet;

/**
 * TCP Long Connection WakeLock
 */
class TlcWakeLock {
    private static final String TAG = TlcWakeLock.class.getSimpleName();

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private PowerManager.WakeLock mTimerWakeLock;
    private HashSet<Object> mHolders = new HashSet<>();

    TlcWakeLock(PowerManager powerManager) {
        mPowerManager = powerManager;
    }

    /**
     * Release this lock and reset all holders
     */
    public synchronized void reset() {
        mHolders.clear();
        release(null);
        if (mWakeLock != null) {
            while (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            L.v(TAG, "~~~ hard reset wakelock :: still held : " + mWakeLock.isHeld());
        }
    }

    public synchronized void acquire(long timeout) {
        if (mTimerWakeLock == null) {
            mTimerWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TLCWakeLock.timer");
            mTimerWakeLock.setReferenceCounted(true);
        }
        mTimerWakeLock.acquire(timeout);
    }

    public synchronized void acquire(Object holder) {
        mHolders.add(holder);
        if (mWakeLock == null) {
            mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TLCWakeLock");
        }
        if (!mWakeLock.isHeld()) mWakeLock.acquire();
        L.v(TAG, "acquire wakelock: holder count=" + mHolders.size());
    }

    public synchronized void release(Object holder) {
        mHolders.remove(holder);
        if ((mWakeLock != null) && mHolders.isEmpty() && mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        L.v(TAG, "release wakelock: holder count=" + mHolders.size());
    }
}