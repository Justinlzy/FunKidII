package com.cqkct.FunKidII.service.tlc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cqkct.FunKidII.Utils.ByteUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.service.Pkt;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UnknownFormatConversionException;

import javax.net.ssl.SSLContext;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;


/**
 * Created by T on 2018/1/5.
 */

public class Conn {
    private static final String TAG = Conn.class.getSimpleName();

    public interface OnEventListener {
        void onConnected(@NonNull Conn conn);
        void onDisconnected(@NonNull Conn conn, long timeSpentSinceConnect);
        void onRead(@NonNull Conn conn, @NonNull Pkt pkt);
    }

    String localPktAddr;
    boolean isLoggedIn;

    private OnEventListener evListener;
    private boolean useTls;
    private ConnThread mThread;
    private Socket mSocket;

    Conn(String localPktAddr, OnEventListener evListener, final boolean useTls) {
        this.localPktAddr = localPktAddr;
        this.evListener = evListener;
        this.useTls = useTls;
    }

    public interface ConnectFutureListener {
        void operationComplete(Conn conn, boolean isSuccess, long timeSpent);
    }

    private boolean connecting = false;
    private synchronized void setConnecting(boolean connecting) {
        this.connecting = connecting;
    }

    void connect(final String inetHost, final int inetPort, long timeoutMillis, final ConnectFutureListener listener) {
        if (isConncting() || (mThread != null && !mThread.isAlive())) {
            return;
        }
        setConnecting(true);
        mThread = new ConnThread(this, inetHost, inetPort, timeoutMillis, listener);
        mThread.start();
    }

    private static class ConnThread extends Thread {
        private WeakReference<Conn> mC;
        ConnThread(Conn c, String inetHost, int inetPort, long timeoutMillis, ConnectFutureListener listener) {
            mC = new WeakReference<>(c);
            this.inetHost = inetHost;
            this.inetPort = inetPort;
            this.listener = listener;
            this.timeoutMillis = timeoutMillis;
        }

        private String inetHost;
        private int inetPort;
        private long timeoutMillis;
        private ConnectFutureListener listener;
        private long beginConnectTime;

        @Override
        public void run() {
            beginConnectTime = System.currentTimeMillis();
            Socket socket;
            do {
                Conn c = mC.get();
                if (c == null)
                    return;

                L.d(TAG, "ConnThread connect...");
                socket = connect(timeoutMillis, c.useTls);
                L.d(TAG, "ConnThread connect END");
                c.setConnecting(false);
                if (listener != null) {
                    if (socket == null) {
                        c.isLoggedIn = false;
                    }
                    listener.operationComplete(c, socket != null, System.currentTimeMillis() - beginConnectTime);
                }
                if (socket == null)
                    return;
                L.i(TAG, "ConnThread connect success");
                c.onConnected(socket);
            } while (false);


            while (true) {
                Conn c = mC.get();
                if (c == null) {
                    L.i(TAG, "ConnThread Conn WeakReference.get is null in while(){...}");
                    break;
                }
                if (c.isClosed()) {
                    L.i(TAG, "ConnThread Conn isClosed in while(){...}");
                    break;
                }

                try {
                    byte[] buf = new byte[1024 * 4];
                    int n = socket.getInputStream().read(buf);
                    if (n < 0) {
                        L.i(TAG, socket.toString() + ": The connection maybe was closed");
                        break;
                    } else if (n == 0) {
                        L.w(TAG, socket.toString() + ": Read 0 byte???");
                        break;
                    }
                    onRead(socket, Unpooled.copiedBuffer(buf, 0, n));
                } catch (Exception e) {
                    L.e(TAG, "Conn RECV thread", e);
                    break;
                }
            }

            L.i(TAG, "ConnThread end while(){...}");

            try {
                socket.close();
            } catch (Exception e) {
                L.v(TAG, "close socket when end ConnThread", e);
            }

            Conn c = mC.get();
            if (c != null) {
                c.onDisconnected(socket, System.currentTimeMillis() - beginConnectTime);
            }
        }

        private Socket connect(long timeoutMillis, boolean useTls) {
            try {
                InetSocketAddressThread addrThread = new InetSocketAddressThread(inetHost, inetPort);
                addrThread.start();
                addrThread.join(timeoutMillis);
                InetSocketAddress addr = addrThread.get();
                if (addr == null) {
                    L.w(TAG, "ConnThread connect: DNS resolve timeout");
                    addrThread.interrupt();
                    return null;
                }
                L.v(TAG, "ConnThread connect: DNS resolve success");

                Socket socket;

                if (useTls) {
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, null, null);
                    socket = sslContext.getSocketFactory().createSocket();
                } else {
                    socket = new Socket();
                }

                socket.connect(addr, (int) timeoutMillis);
                return socket;
            } catch (Exception e) {
                L.e(TAG, "ConnThread connect: new Socket failure", e);
            }

            return null;
        }

        public class InetSocketAddressThread extends Thread {
            private InetSocketAddress addr;
            private String host;
            private int port;

            public InetSocketAddressThread(String host, int port) {
                this.host = host;
                this.port = port;
            }

            public void run() {
                try {
                    set(new InetSocketAddress(host, port));
                } catch (Exception ignored) {
                }
            }

            private synchronized void set(InetSocketAddress addr) {
                this.addr = addr;
            }

            public synchronized InetSocketAddress get() {
                return addr;
            }
        }


        ByteBuf cumulation;
        private void onRead(Socket socket, ByteBuf data) {
            try {
                if (cumulation == null) {
                    cumulation = data;
                } else {
                    cumulation = cumulate(UnpooledByteBufAllocator.DEFAULT, cumulation, data);
                }

                decode(socket, cumulation);
            } finally {
                if (cumulation != null && !cumulation.isReadable()) {
                    cumulation.release();
                    cumulation = null;
                }
            }
        }

        ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
            final ByteBuf buffer;
            if (cumulation.writerIndex() > cumulation.maxCapacity() - in.readableBytes()
                    || cumulation.refCnt() > 1 || cumulation.isReadOnly()) {
                // Expand cumulation (by replace it) when either there is not more room in the buffer
                // or if the refCnt is greater then 1 which may happen when the user use slice().retain() or
                // duplicate().retain() or if its read-only.
                buffer = expandCumulation(alloc, cumulation, in.readableBytes());
            } else {
                buffer = cumulation;
            }
            buffer.writeBytes(in);
            in.release();
            return buffer;
        }

        static ByteBuf expandCumulation(ByteBufAllocator alloc, ByteBuf cumulation, int readable) {
            ByteBuf oldCumulation = cumulation;
            cumulation = alloc.buffer(oldCumulation.readableBytes() + readable);
            cumulation.writeBytes(oldCumulation);
            oldCumulation.release();
            return cumulation;
        }

        private boolean sopFond = false;
        private long pktLen = -1;
        private boolean readerIndexMarked = false;
        private void decode(Socket socket, ByteBuf in) {
            for (; in.readableBytes() > 0; sopFond = false, pktLen = -1) {
                try {
                    if (!sopFond) {
                        int sop = in.indexOf(in.readerIndex(), in.writerIndex(), Pkt.SOP);
                        if (sop < 0) {
                            if (readerIndexMarked) {
                                readerIndexMarked = false;
                                in.resetReaderIndex();
                            }
                            fail(in);
                            return;
                        }

                        if (sop > in.readerIndex()) {
                            if (readerIndexMarked)
                                in.resetReaderIndex();
                            fail(in.readSlice(sop));
                        }

                        readerIndexMarked = false;

                        sopFond = true;
                    }

                    if (pktLen < 0) {
                        if (in.readableBytes() < (Pkt.LEN_SOP + Pkt.LEN_LEN))
                            return;
                        pktLen = in.getUnsignedInt(in.readerIndex() + Pkt.IDX_LEN);
                        if (pktLen < Pkt.LEN_MIN || pktLen > 1024 * 1024 * 10 /* FIXME: 数据包最大长度？ */) {
                            // 无效
                            if (!readerIndexMarked) {
                                in.markReaderIndex();
                                readerIndexMarked = true;
                            }

                            in.readByte();
                            continue;
                        }
                    }

                    if (in.readableBytes() < pktLen)
                        return;

                    byte[] raw = new byte[(int) pktLen];
                    in.getBytes(in.readerIndex(), raw);

                    try {
                        Pkt pkt = Pkt.decode(raw);
                        Conn c = mC.get();
                        if (c == null)
                            return;
                        c.onRead(socket, pkt);

                        in.skipBytes((int) pktLen);
                    } catch (Exception e) {
                        L.w(TAG,"Pkt decode failure raw.length:" + raw.length + " " + ByteUtils.rawToHexStr(raw), e);

                        if (!readerIndexMarked) {
                            in.markReaderIndex();
                            readerIndexMarked = true;
                        }
                        in.readByte();
                    }
                } catch (IndexOutOfBoundsException e) {
                    return;
                }
            }
        }

        private void fail(ByteBuf buf) {
            byte[] raw = new byte[buf.readableBytes()];
            buf.readBytes(raw);
            String msg = ByteUtils.rawToHexStr(raw);
            L.e(TAG, "fail", new UnknownFormatConversionException(msg));
        }
    }


    synchronized boolean isConncting() {
        return connecting;
    }

    synchronized boolean isActive() {
        return !isClosed() && mSocket != null && mSocket.isConnected() && !mSocket.isClosed();
    }

    synchronized void setOnEventListener(OnEventListener l) {
        evListener = l;
    }

    public boolean writeAndFlush(@NonNull Pkt pkt) {
        if (mSocket == null || (closed != null && closed))
            return false;
        try {
            mSocket.getOutputStream().write(pkt.encode());
            L.v(TAG, "writeAndFlush: " + pkt + ": " + ByteUtils.rawToHexStr(pkt.getRaw()));
            return true;
        } catch (Exception e) {
            L.e(TAG, "writeAndFlush", e);
        }
        return false;
    }

    @Nullable
    private Boolean closed;
    synchronized void close() {
        if (closed != null && closed)
            return;
        closed = true;
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (Exception e) {
                L.d(TAG, "Conn close()", e);
            }
            mSocket = null;
        }
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
    }

    public boolean isClosed() {
        return closed != null && closed;
    }

    @Override
    public String toString() {
        return "Conn(" + localPktAddr + ": " + (mSocket == null ? "?" : mSocket.getLocalAddress() + "->" + mSocket.getRemoteSocketAddress()) + ")";
    }

    synchronized void onConnected(Socket socket) {
        mSocket = socket;
        if (evListener != null)
            evListener.onConnected(this);
    }

    synchronized void onDisconnected(Socket socket, long timeSpent) {
        isLoggedIn = false;
        if (evListener != null)
            evListener.onDisconnected(this, timeSpent);
    }

    synchronized void onRead(Socket socket, Pkt pkt) {
        L.v(TAG, "onRead: " + pkt + ": " + ByteUtils.rawToHexStr(pkt.getRaw()));
        if (evListener != null)
            evListener.onRead(this, pkt);
    }
}
