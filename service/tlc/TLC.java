package com.cqkct.FunKidII.service.tlc;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cqkct.FunKidII.BuildConfig;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.service.Pkt;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.nio.channels.NotYetConnectedException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;



class TLC {
    private static final String TAG = TLC.class.getSimpleName();

    private boolean mUseTls = true;
    private String serverDomain = Pkt.SERVER_DOMAIN;
    private int serverPort = Pkt.SERVER_TLC_MSG_TLS_PORT;

    public static final long HEARTBEAT_TIME_MILLIS = 1000 * 60;


    private Conn mConn;
    private final Object mConnLock = new Object();

    private HeartbeatTimerHandler mHeartbeatTimerHandler;
    private TlcService mTLCService;
    private ExecHandler mExecHandler;
    private SendHandler mSendHandler;


    /** 等待回响的请求, 以包的流水号作为key */
    private Map<Pkt.Seq, ExecEntity> pendingExecMap = new ConcurrentHashMap<>();
    /** 等待S3回响的请求, 流水号作为key */
    private Map<String, ExecEntity> thirdStageMap = new ConcurrentHashMap<>();

    private class PktHandlerOnEventListener implements Conn.OnEventListener {

        @Override
        public void onConnected(@NonNull Conn conn) {
            if (BuildConfig.DEBUG) {
                L.v(TAG, "pktHandlerOnEventListener.onConnected() " + conn);
            }
            if (mExecHandler != null) {
                mExecHandler.obtainMessage(ExecHandler.ON_CONNECTED, conn).sendToTarget();
            }
        }

        @Override
        public void onDisconnected(@NonNull Conn conn, long timeSpentSinceConnect) {
            if (BuildConfig.DEBUG) {
                L.v(TAG, "pktHandlerOnEventListener.onDisconnected() " + conn);
            }
            if (mExecHandler != null) {
                Object[] objs = new Object[]{conn, timeSpentSinceConnect};
                mExecHandler.obtainMessage(ExecHandler.ON_DISCONNECTED, objs).sendToTarget();
            }
        }

        @Override
        public void onRead(@NonNull Conn conn, @NonNull Pkt pkt) {
            if (BuildConfig.DEBUG) {
                L.v(TAG, "pktHandlerOnEventListener.onRead() " + conn + ": " + pkt);
            }
            if (mExecHandler != null) {
                Object[] objects = new Object[]{conn, pkt};
                mExecHandler.obtainMessage(ExecHandler.ON_PKT_READ, objects).sendToTarget();
            }
            if (mHeartbeatTimerHandler != null)
                mHeartbeatTimerHandler.updateRecvTimer();
        }
    }


    public TLC(TlcService service) {
        mTLCService = service;
        mHeartbeatTimerHandler = new HeartbeatTimerHandler(this);
        mExecHandler = new ExecHandler(this);
        mSendHandler = new SendHandler(this);
    }

    public TLC(TlcService service, String domain, int port, boolean useTls) {
        this(service);
        setServerDomain(domain);
        setServerPort(port);
        setUseTls(useTls);
    }

    public TLC(TlcService service, String addr, boolean useTls) {
        this(service);
        setServerAddr(addr, useTls);
    }

    public void setServerDomain(String domain) {
        serverDomain = domain;
    }

    public String getServerDomain() {
        return serverDomain;
    }

    public String getUsableServerDomain() {
        return TextUtils.isEmpty(serverDomain) ? Pkt.SERVER_DOMAIN : serverDomain;
    }

    public void setServerPort(int port) {
        serverPort = port;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getUsableServerPort() {
        return serverPort <= 0 || serverPort >= 65535 ? (mUseTls ? Pkt.SERVER_TLC_MSG_TLS_PORT : Pkt.SERVER_TLC_MSG_PORT) : serverPort;
    }

    public void setUseTls(boolean useTls) {
        mUseTls = useTls;
    }

    public boolean isUseTls() {
        return mUseTls;
    }

    public void setServerAddr(String addr, boolean useTls) {
        String domain = null;
        // schema
        if (!TextUtils.isEmpty(addr)) {
            int idx = addr.indexOf("://");
            if (idx >= 0) {
                addr = addr.substring(idx + 3);
            }
        }
        // domain
        if (!TextUtils.isEmpty(addr)) {
            if (addr.charAt(0) == '[') {
                int idx = addr.indexOf(']');
                if (idx >= 0) {
                    domain = addr.substring(1, idx);
                    addr = addr.substring(idx + 1);
                }
            }
            if (domain == null) {
                int idx = addr.indexOf(':');
                if (idx >= 0) {
                    domain = addr.substring(0, idx);
                    addr = addr.substring(idx);
                }
            }
            if (domain == null) {
                int idx = addr.indexOf('/');
                if (idx >= 0) {
                    domain = addr.substring(0, idx);
                    addr = addr.substring(idx);
                }
            }
            if (domain == null) {
                domain = addr;
                addr = "";
            }
        }
        // port
        int port = -1;
        if (!TextUtils.isEmpty(addr)) {
            if (addr.charAt(0) == ':') {
                String portStr;
                int idx = addr.indexOf('/');
                if (idx >= 0) {
                    portStr = addr.substring(1, idx);
                } else {
                    portStr = addr.substring(1);
                }
                if (!TextUtils.isEmpty(portStr)) {
                    try {
                        port = Integer.parseInt(portStr);
                    } catch (Exception ignored) {}
                }
            }
        }
        setServerDomain(domain);
        setServerPort(port);
        setUseTls(useTls);
    }

    private boolean stop() {
        synchronized (mConnLock) {
            if (mConn != null) {
                mConn.close();
            }
        }
        return true;
    }

    public boolean isConnected() {
        synchronized (mConnLock) {
            return mConn != null && mConn.isActive();
        }
    }

    public boolean isConnecting() {
        synchronized (mConnLock) {
            return mConn != null && mConn.isConncting();
        }
    }

    private void doConnect(String localPktAddr) {
        L.v(TAG, "doConnect");
        synchronized (mConnLock) {
//            if (isConnecting() || isConnected())
//                return;
            if (mConn != null) {
                mConn.close();
                mConn = null;
            }
            try {
                mConn = new Conn(localPktAddr, new PktHandlerOnEventListener(), isUseTls());
                L.v(TAG, "connect to " + getUsableServerDomain() + ":" + getUsableServerPort() + "...");
                mConn.connect(getUsableServerDomain(), getUsableServerPort(), 1000L * 5, new Conn.ConnectFutureListener() {
                    @Override
                    public void operationComplete(Conn conn, boolean isSuccess, long timeSpent) {
                        if (!isSuccess) {
                            Object[] objs = new Object[]{conn, timeSpent};
                            mExecHandler.obtainMessage(ExecHandler.ON_DISCONNECTED, objs).sendToTarget();
                        }
                    }
                });
            } catch (Exception e) {
                mConn = null;
                L.e(TAG, "on doConnect() failure", e);
                Object[] objs = new Object[]{null, 0};
                mExecHandler.obtainMessage(ExecHandler.ON_DISCONNECTED, objs).sendToTarget();
            }
        }
    }

    private void doDisconnect() {
        L.v(TAG, "doDisconnect");
        synchronized (mConnLock) {
            if (mConn != null) {
                mConn.close();
            }
        }
    }

    private boolean doSend(Pkt pkt) {
        try {
            synchronized (mConnLock) {
                if (!mConn.isActive())
                    return false;
                mConn.writeAndFlush(pkt);
                mHeartbeatTimerHandler.updateSendTimer();
                return true;
            }
        } catch (Exception e) {
            L.e(TAG, "doSend() failure", e);
        }

        return false;
    }

    private void doSendSync(@NonNull Pkt pkt, boolean needIsLoggedIn, @Nullable TlcService.OnSendListener onSendListener) {
        Throwable throwable = null;
        synchronized (mConnLock) {
            do {
                if (!isConnected()) {
                    throwable = new NotYetConnectedException();
                    break;
                }
                if (needIsLoggedIn && !isLoggedIn()) {
                    throwable = new NotYetLoginException();
                    break;
                }
                try {
                    mConn.writeAndFlush(pkt);
                    mHeartbeatTimerHandler.updateSendTimer();
                } catch (Exception e) {
                    throwable = e;
                }
            } while (false);
        }
        if (onSendListener != null) {
            if (throwable != null) {
                onSendListener.onException(pkt, throwable);
            } else {
                onSendListener.onSuccess(pkt);
            }
        }
    }

    public synchronized void connect(@Nullable String localPktAddr, long connectDelay) {
        mExecHandler.removeMessages(ExecHandler.DISCONNECT);
        if (connectDelay <= 0) {
            mExecHandler.removeMessages(ExecHandler.CONNECT);
            mExecHandler.obtainMessage(ExecHandler.CONNECT, localPktAddr).sendToTarget();
        } else {
            if (!mExecHandler.hasMessages(ExecHandler.CONNECT)) {
                mExecHandler.sendMessageDelayed(mExecHandler.obtainMessage(ExecHandler.CONNECT, localPktAddr), connectDelay);
            }
        }
    }

    public String getLocalPktAddr() {
        synchronized (mConnLock) {
            try {
                return mConn.localPktAddr;
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    public void setLocalPktAddr(String localPktAddr) {
        synchronized (mConnLock) {
            try {
                mConn.localPktAddr = localPktAddr;
            } catch (Exception ignore) {
            }
        }
    }

    public void setIsLoggedIn(boolean loggedIn) {
        synchronized (mConnLock) {
            try {
                mConn.isLoggedIn = loggedIn;
            } catch (Exception ignore) {
            }
        }
    }

    public boolean isLoggedIn() {
        synchronized (mConnLock) {
            return mConn != null && mConn.isLoggedIn;
        }
    }

    public void enableHeartbeat(int expireSecs) {
        mHeartbeatTimerHandler.enableHeartbeat(expireSecs * 1000L);
    }

    public synchronized void disconnect() {
        L.v(TAG, "enter disconnect", new Exception("enter disconnect"));
        mExecHandler.removeMessages(ExecHandler.CONNECT);
        mExecHandler.removeMessages(ExecHandler.DISCONNECT);
        mExecHandler.obtainMessage(ExecHandler.DISCONNECT).sendToTarget();
    }

    public void send(@NonNull Pkt pkt, boolean needIsLoggedIn, @Nullable TlcService.OnSendListener onSendListener) {
        Object[] objs = new Object[]{pkt, needIsLoggedIn, onSendListener};
        mExecHandler.obtainMessage(ExecHandler.SEND_DATA_REQ, objs).sendToTarget();
    }

    void stopThirdStageTimer(ExecEntity execEntity) {
        String key = makeThirdStageMapKey(execEntity.request);
        if (key != null) {
            ExecEntity thirdStageWait = thirdStageMap.remove(key);
            if (thirdStageWait != null) {
                mExecHandler.removeMessages(ExecHandler.ON_WAIT_THIRD_STAGE_TIMER, thirdStageWait);
            }
        }
    }

    private static class HeartbeatTimerHandler extends Handler {
        private static final int HEARTBEAT = 0;

        private static final int CONNECTION_TIMEOUT_TIMER = 1000;

        private boolean enable = false;
        private long expireMillis = HEARTBEAT_TIME_MILLIS;

        private static HandlerThread handlerThread;
        private static Looper createLooper() {
            if (handlerThread == null) {
                L.d(TAG, "Creating new handler thread");
                handlerThread = new HandlerThread("TLCHeartbeatTimer");
                handlerThread.start();
            }
            return handlerThread.getLooper();
        }

        WeakReference<TLC> mTLC;
        HeartbeatTimerHandler(TLC tlc) {
            super(createLooper());
            mTLC = new WeakReference<>(tlc);
        }

        @Override
        public void handleMessage(Message msg) {
            TLC tlc = mTLC.get();
            if (tlc == null)
                return;

            switch (msg.what) {
                case HEARTBEAT:
                    if (enable) {
                        String srcaddr = null;
                        synchronized (tlc.mConnLock) {
                            try {
                                srcaddr = tlc.mConn.localPktAddr;
                            } catch (Exception ignore) {}
                        }
                        if (!TextUtils.isEmpty(srcaddr)) {
                            Pkt pkt = Pkt.newBuilder()
                                    .setSrcAddr(srcaddr)
                                    .setValue(protocol.Message.HeartReqMsg.newBuilder().build())
                                    .build();
                            tlc.sendPkt(pkt);
                        } else {
                            stopHeartbeat();
                        }
                    }
                    break;

                case CONNECTION_TIMEOUT_TIMER:
                    if (enable) {
                        L.w(TAG, "connection heartbeat timeout! post disconnect event");
                        tlc.mExecHandler.obtainMessage(ExecHandler.DISCONNECT).sendToTarget();
                    }
                    break;

                default:
                    break;
            }
        }

        public synchronized void updateSendTimer() {
            removeMessages(HEARTBEAT);
            sendEmptyMessageDelayed(HEARTBEAT, expireMillis);
        }

        public synchronized void updateRecvTimer() {
            removeMessages(CONNECTION_TIMEOUT_TIMER);
            sendEmptyMessageDelayed(CONNECTION_TIMEOUT_TIMER, expireMillis + 1000L * protocol.Message.Heartbeat.ExpireTime.ADJUST_VALUE * 2);
        }

        public synchronized void stopHeartbeat() {
            enable = false;
            removeMessages(HEARTBEAT);
            removeMessages(CONNECTION_TIMEOUT_TIMER);
        }

        public synchronized void enableHeartbeat(long expireMillis) {
            this.expireMillis = expireMillis;
            enable = true;
            updateSendTimer();
            updateRecvTimer();
        }
    }

    /**
     * 执行网络请求
     * @param pkt 请求包
     * @param timeoutMillis 得到回响超时时间
     * @param forceReconnect 无连接时，是否需要马上连接
     * @param listener 结果回调
     * @param thirdStageTimeoutMillis 第3阶段回响超时时间 （如果为 null, 则不等待第3阶段）
     * @return 执行实体
     */
    public ExecEntity exec(@NonNull Pkt pkt, long timeoutMillis, boolean forceReconnect, @Nullable TlcService.OnExecListener listener, @Nullable Long thirdStageTimeoutMillis) {
        ExecEntity execEntity = new ExecEntity(pkt, timeoutMillis, listener, thirdStageTimeoutMillis);
        execEntity.mTlc = this;
        mExecHandler.obtainMessage(ExecHandler.EXEC, forceReconnect ? 1 : 0, 0, execEntity).sendToTarget();
        return execEntity;
    }

    ExecEntity exec(@NonNull ExecEntity execEntity, boolean forceReconnect, @NonNull String userId) {
        synchronized (mConnLock) {
            try {
                if (!userId.equals(mConn.localPktAddr)) {
                    disconnect();
                }
            } catch (Exception ignore) {}
        }
        execEntity.mTlc = this;
        mExecHandler.obtainMessage(ExecHandler.EXEC, forceReconnect ? 1 : 0, 0, execEntity).sendToTarget();
        return execEntity;
    }

    boolean cancelExec(ExecEntity execEntity) {
        stopThirdStageTimer(execEntity);
        return pendingExecMap.remove(execEntity.request.seq) != null;
    }

    private boolean addWaitThirdStageIfNeed(ExecEntity execEntity) {
        if (execEntity.thirdStageTimeoutMillis == null)
            return false;

        String key = makeThirdStageMapKey(execEntity.request);
        if (key == null)
            return false;

        thirdStageMap.put(key, execEntity);
        mExecHandler.sendMessageDelayed(mExecHandler.obtainMessage(ExecHandler.ON_WAIT_THIRD_STAGE_TIMER, execEntity), execEntity.thirdStageTimeoutMillis);
        return true;
    }

    private String makeThirdStageMapKey(Pkt firstStagePkt) {
        Pkt tmpPkt = new Pkt();
        tmpPkt.setSeq(firstStagePkt.seq);

        switch (firstStagePkt.tag) {
            case protocol.Message.Tag.LOCATE_S1_VALUE:
                tmpPkt.setTag(protocol.Message.Tag.LOCATE_S3_VALUE);
                break;
            case protocol.Message.Tag.FIND_DEVICE_S1_VALUE:
                tmpPkt.setTag(protocol.Message.Tag.FIND_DEVICE_S3_VALUE);
                break;
            case protocol.Message.Tag.FETCH_DEVICE_SENSOR_DATA_S1_VALUE:
                tmpPkt.setTag(protocol.Message.Tag.FETCH_DEVICE_SENSOR_DATA_S3_VALUE);
                break;
            case protocol.Message.Tag.TAKE_PHOTO_S1_VALUE:
                tmpPkt.setTag(protocol.Message.Tag.TAKE_PHOTO_S3_VALUE);
                break;
            case protocol.Message.Tag.SIMPLEX_CALL_S1_VALUE:
                tmpPkt.setTag(protocol.Message.Tag.SIMPLEX_CALL_S3_VALUE);
                break;
            /* case ...: TODO: 增加相关指令后，这里需要添加 */
            default:
                return null;
        }

        return tmpPkt.seqBitOrTag();
    }


    private static class ExecHandler extends Handler {
        /** 连接请求 */
        static final int CONNECT = -1;
        /** 断开连接请求 */
        static final int DISCONNECT = -2;

        /** 已连接事件 */
        static final int ON_CONNECTED = 0;
        /** 已断开连接事件 */
        static final int ON_DISCONNECTED = 1;
        /** 读取到数据包 */
        static final int ON_PKT_READ = 2;

        /** 执行请求 */
        static final int EXEC = 1000;
        /** 发送数据包请求 */
        static final int SEND_DATA_REQ = 1001;

        /** 发送数据包超时 */
        static final int ON_EXEC_TIMEOUT_TIMER = 2000;

        /** 等待第三阶段超时 */
        static final int ON_WAIT_THIRD_STAGE_TIMER = 3000;


        private static HandlerThread handlerThread;
        private static Looper createLooper() {
            if (handlerThread == null) {
                L.d(TAG, "Creating new handler thread");
                handlerThread = new HandlerThread("TLC");
                handlerThread.start();
            }
            return handlerThread.getLooper();
        }

        WeakReference<TLC> mTLC;
        ExecHandler(TLC tlc) {
            super(createLooper());
            mTLC = new WeakReference<>(tlc);
        }

        @Override
        public void handleMessage(Message msg) {
            TLC tlc = mTLC.get();
            if (tlc == null)
                return;

            switch (msg.what) {
                case CONNECT: {
                    String localPktAddr = (String) msg.obj;
                    tlc.doConnect(localPktAddr);
                }
                    break;
                case DISCONNECT:
                    tlc.doDisconnect();
                    break;

                case ON_CONNECTED: {
                    Conn conn = (Conn) msg.obj;
                    tlc.doOnConnected(conn);
                }
                    break;
                case ON_DISCONNECTED: {
                    Object[] objs = (Object[]) msg.obj;
                    Conn conn = (Conn) objs[0];
                    long timeSpent = objs[1] == null ? 0 : (long) objs[1];
                    tlc.doOnDisconnected(conn, timeSpent);
                }
                    break;
                case ON_PKT_READ: {
                    Object[] objs = (Object[]) msg.obj;
                    Conn conn = (Conn) objs[0];
                    Pkt pkt = (Pkt) objs[1];
                    tlc.doOnPktRead(conn, pkt);
                }
                    break;


                case EXEC: {
                    ExecEntity execEntity = (ExecEntity) msg.obj;
                    tlc.doExec(execEntity, msg.arg1 != 0);
                }
                    break;

                case SEND_DATA_REQ: {
                    Object[] objs = (Object[]) msg.obj;
                    Pkt pkt = (Pkt) objs[0];
                    boolean needIsLoggedIn = objs[1] != null && (boolean) objs[1];
                    TlcService.OnSendListener onSendListener = (TlcService.OnSendListener) objs[2];
                    tlc.doSendSync(pkt, needIsLoggedIn, onSendListener);
                }
                    break;



                case ON_EXEC_TIMEOUT_TIMER: {
                    ExecEntity execEntity = (ExecEntity) msg.obj;
                    tlc.doPktExecTimeout(execEntity);
                }
                    break;

                case ON_WAIT_THIRD_STAGE_TIMER: {
                    ExecEntity execEntity = (ExecEntity) msg.obj;
                    tlc.doThirdStageTimeout(execEntity);
                }

                default:
                    break;
            }
        }
    }

    private void doOnConnected(@NonNull Conn conn) {
        synchronized (mConnLock) {
            if (conn != mConn) {
                conn.close();
                return;
            }
        }

        for (Map.Entry<Pkt.Seq, ExecEntity> e : pendingExecMap.entrySet()) {
            ExecEntity execEntity = e.getValue();
            if (execEntity.mConn == null) {
                execEntity.mConn = conn;
                sendPkt(execEntity.request);
            }
        }

        mTLCService.mHandler.sendEmptyMessage(TlcService.ServiceHandler.ON_TLC_CONNECTED);
    }

    private void doOnDisconnected(@Nullable Conn conn, long timeSpent) {
        L.v(TAG, "doOnDisconnected(): " + conn + ",  mConn: " + mConn);
        mSendHandler.removeAllPendingSend();
        Conn tlcWorkConn = mConn;
        synchronized (mConnLock) {
            if (conn == null) {
                // 创建 Conn 出现异常时会出现 null
                // 那么 mConn 也应该为 null

                if (mConn != null) {
                    mConn.close();
                    mConn = null;
                }
            } else {
                if (mConn == conn) {
                    mConn = null;
                }
            }
        }

        if (conn == null) {
            // 创建 Conn 出现异常

            // 停止心跳 worker
            if (mHeartbeatTimerHandler != null) {
                mHeartbeatTimerHandler.stopHeartbeat();
            }

            // 结束所有 execEntity
            for (Iterator<Map.Entry<Pkt.Seq, ExecEntity>> it = pendingExecMap.entrySet().iterator(); it.hasNext(); ) {
                ExecEntity execEntity = it.next().getValue();
                mExecHandler.removeMessages(ExecHandler.ON_EXEC_TIMEOUT_TIMER, execEntity);

                execEntity.cause = new ConnectException("create connection failure");
                mTLCService.mHandler.obtainMessage(TlcService.ServiceHandler.ON_EXEC_EXCEPTION, execEntity).sendToTarget();

                mExecHandler.removeMessages(ExecHandler.ON_WAIT_THIRD_STAGE_TIMER, execEntity);
                it.remove();
            }

            // 向 Service 发送连接断开的事件
            mTLCService.mHandler.obtainMessage(TlcService.ServiceHandler.ON_TLC_DISCONNECTED, timeSpent).sendToTarget();
        } else {

            // 结束所有断开的连接上的 execEntity
            for (Iterator<Map.Entry<Pkt.Seq, ExecEntity>> it = pendingExecMap.entrySet().iterator(); it.hasNext(); ) {
                ExecEntity execEntity = it.next().getValue();
                if (execEntity.mConn == conn) {
                    mExecHandler.removeMessages(ExecHandler.ON_EXEC_TIMEOUT_TIMER, execEntity);

                    execEntity.cause = new IOException("Connection disconnected");
                    mTLCService.mHandler.obtainMessage(TlcService.ServiceHandler.ON_EXEC_EXCEPTION, execEntity).sendToTarget();

                    mExecHandler.removeMessages(ExecHandler.ON_WAIT_THIRD_STAGE_TIMER, execEntity);
                    it.remove();
                }
            }

            if (tlcWorkConn == conn) {
                // TLC 当前工作的 Conn 断开

                // 停止心跳worker
                if (mHeartbeatTimerHandler != null) {
                    mHeartbeatTimerHandler.stopHeartbeat();
                }

                // 结束 execEntity.mConn == null 的请求
                // execEntity.mConn == null 表示等待创建连接并连接成功后发送

                for (Iterator<Map.Entry<Pkt.Seq, ExecEntity>> it = pendingExecMap.entrySet().iterator(); it.hasNext(); ) {
                    ExecEntity execEntity = it.next().getValue();
                    if (execEntity.mConn == null) {
                        mExecHandler.removeMessages(ExecHandler.ON_EXEC_TIMEOUT_TIMER, execEntity);

                        execEntity.cause = new IOException("Connection disconnected");
                        mTLCService.mHandler.obtainMessage(TlcService.ServiceHandler.ON_EXEC_EXCEPTION, execEntity).sendToTarget();

                        mExecHandler.removeMessages(ExecHandler.ON_WAIT_THIRD_STAGE_TIMER, execEntity);
                        it.remove();
                    }
                }

                // 向 Service 发送连接断开的事件
                mTLCService.mHandler.obtainMessage(TlcService.ServiceHandler.ON_TLC_DISCONNECTED, timeSpent).sendToTarget();
            }
        }
    }

    private void doExec(ExecEntity execEntity, boolean forceReconnect) {
        synchronized (mConnLock) {
            boolean isConnecting = isConnecting();
            boolean isConnected = isConnected();
            boolean isLoggedIn = isLoggedIn();
            if ((forceReconnect && (isConnecting || isConnected)) || (isConnected && isLoggedIn)) {
                execEntity.mConn = mConn;
                pendingExecMap.put(execEntity.request.seq, execEntity);
                mExecHandler.sendMessageDelayed(mExecHandler.obtainMessage(ExecHandler.ON_EXEC_TIMEOUT_TIMER, execEntity), execEntity.timeoutMillis);
                addWaitThirdStageIfNeed(execEntity);
                if (isConnected) {
                    sendPkt(execEntity.request);
                }
                return;
            }

            if (forceReconnect) {
                pendingExecMap.put(execEntity.request.seq, execEntity);
                mExecHandler.sendMessageDelayed(mExecHandler.obtainMessage(ExecHandler.ON_EXEC_TIMEOUT_TIMER, execEntity), execEntity.timeoutMillis);
                addWaitThirdStageIfNeed(execEntity);
                connect(execEntity.request.srcAddr, 0);
            } else if (execEntity.listener != null) {
                execEntity.cause = isConnected ? new NotYetLoginException() : new NotYetConnectedException();
                mTLCService.mHandler.obtainMessage(TlcService.ServiceHandler.ON_EXEC_EXCEPTION, execEntity).sendToTarget();
            }
        }
    }

    private void doOnPktRead(@NonNull Conn conn, final Pkt pkt) {
        if (pkt == null)
            return;

        if (pkt.isResponse()) {
            // 这是回响

            if (false) {
                boolean pktIsOnNowConn = true;
                synchronized (mConnLock) {
                    if (conn != mConn) {
                        pktIsOnNowConn = false;
                    }
                }
                if (!pktIsOnNowConn) {
                    L.w(TAG, "doOnPktRead() the pkt(" + pkt + ") is not on now Conn!!! drop it!!!");
                    return;
                }
            }

            ExecEntity execEntity = pendingExecMap.get(pkt.seq);
            if (execEntity != null) {
                mExecHandler.removeMessages(ExecHandler.ON_EXEC_TIMEOUT_TIMER, execEntity);
                pendingExecMap.remove(pkt.seq);
                execEntity.response = pkt;
                mTLCService.mHandler.obtainMessage(TlcService.ServiceHandler.ON_EXEC_RESPONSE, execEntity).sendToTarget();
            } else if (pkt.tag != protocol.Message.Tag.HEARTRATE_VALUE) {
                L.w(TAG, "no match request Pkt of response Pkt: " + pkt);
            }
            return;
        }


        // 这是事件

        ExecEntity thirdStageWait = thirdStageMap.get(pkt.seqBitOrTag());
        if (thirdStageWait != null) {
            // 某一请求的第3阶段的事件
            mExecHandler.removeMessages(ExecHandler.ON_WAIT_THIRD_STAGE_TIMER, thirdStageWait);
            thirdStageMap.remove(pkt.seqBitOrTag());
            thirdStageWait.thirdStageEvent = pkt;
            mTLCService.mHandler.obtainMessage(TlcService.ServiceHandler.ON_THIRD_STAGE_EVENT, thirdStageWait).sendToTarget();
        } else {
            mTLCService.mHandler.obtainMessage(TlcService.ServiceHandler.ON_EVENT_PKT, pkt).sendToTarget();
        }
    }

    private void doPktExecTimeout(ExecEntity timedoutExecEntity) {
        pendingExecMap.remove(timedoutExecEntity.request.seq);
        String key = makeThirdStageMapKey(timedoutExecEntity.request);
        if (key != null) {
            ExecEntity thirdStageWait = thirdStageMap.remove(key);
            if (thirdStageWait != null) {
                mExecHandler.removeMessages(ExecHandler.ON_WAIT_THIRD_STAGE_TIMER, thirdStageWait);
            }
        }
        timedoutExecEntity.cause = new TimeoutException("Pkt exec timedout");
        mTLCService.mHandler.obtainMessage(TlcService.ServiceHandler.ON_EXEC_EXCEPTION, timedoutExecEntity).sendToTarget();
    }

    private void doThirdStageTimeout(ExecEntity timedoutExecEntity) {
        String key = makeThirdStageMapKey(timedoutExecEntity.request);
        if (key != null)
            thirdStageMap.remove(key);
        timedoutExecEntity.cause = new WaitThirdStageTimeoutException("wait third stage timedout");
        mTLCService.mHandler.obtainMessage(TlcService.ServiceHandler.ON_EXEC_EXCEPTION, timedoutExecEntity).sendToTarget();
    }

    private void sendPkt(Pkt pkt) {
        mSendHandler.send(pkt);
    }

    private static class SendHandler extends Handler {
        private static final int SEND = 0;

        private static HandlerThread handlerThread;
        private static Looper createLooper() {
            if (handlerThread == null) {
                L.d(TAG, "Creating new handler thread");
                handlerThread = new HandlerThread("TLC SEND PKT");
                handlerThread.start();
            }
            return handlerThread.getLooper();
        }

        WeakReference<TLC> mTLC;
        SendHandler(TLC tlc) {
            super(createLooper());
            mTLC = new WeakReference<>(tlc);
        }

        @Override
        public void handleMessage(Message msg) {
            TLC tlc = mTLC.get();
            if (tlc == null)
                return;

            switch (msg.what) {
                case SEND:
                    tlc.doSend((Pkt) msg.obj);
                    break;
                default:
                    break;
            }
        }

        void send(Pkt pkt) {
            obtainMessage(SEND, pkt).sendToTarget();
        }

        void removeAllPendingSend() {
            removeMessages(SEND);
        }
    }
}