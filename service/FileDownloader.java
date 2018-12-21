package com.cqkct.FunKidII.service;

import android.text.TextUtils;

import com.cqkct.FunKidII.Utils.L;
import com.google.protobuf.GeneratedMessageV3;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import protocol.Message;

/**
 * 上传下载
 * <p> &nbsp;&nbsp;&nbsp;&nbsp; stage 1: Tag:0x0f06 下载文件前协商请求
 * <p> &nbsp;&nbsp;&nbsp;&nbsp; stage 2: Tag:0x0f07 下载文件数据
 * <p> &nbsp;&nbsp;&nbsp;&nbsp; stage 3: Tag:0x0f08 文件传输完成通知
 * <pre>
 *  Usage:
 *      FileDownloader downloader = new FileDownloader(saveTo, filename, filesize, uploadedBy);
 *      downloader.download();
 * </pre>
 */
public class FileDownloader { // FIXME: 实现为service
    private static final String TAG = FileDownloader.class.getSimpleName();
    private static final int RESPONSE_TIMEOUT = 1000;
    private static final int PAYLOAD_SIZE = 1024;

    private File mSaveFile;
    private String mFilename;
    private int mFilesize;
    private String mUploadedBy;

    private String userId;

    private Pkt sendPkt;

    /** 文件数据包索引 */
    private int fileFragmentIdx;

    /** 数据包发送 + 重发次数 */
    private static final int PKT_SEND_RETRY_TIMES = 3;


    public FileDownloader(File saveTo, String filename, int filesize, String uploadedBy, String userId) {
        mSaveFile = saveTo;
        mFilename = filename;
        mFilesize = filesize;
        mUploadedBy = uploadedBy;
        this.userId = userId;
    }

    public void download() throws Exception {
        L.d(TAG, "download \"" + mFilename + "\" to " + mSaveFile.getAbsolutePath());

        InetSocketAddress serverAddr = new InetSocketAddress(Pkt.SERVER_DOMAIN, Pkt.SERVER_UDP_FILE_PORT);

        if (TextUtils.isEmpty(userId))
            throw new Exception("there is no user!!!");

        DatagramSocket socket = null;
        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(mSaveFile));
            socket = new DatagramSocket();
            downloadBefore(socket, serverAddr);
            try {
                transmitFile(socket, serverAddr, outputStream);
                L.v(TAG, "transmitFile end");
            } catch (Exception e) {
                L.d(TAG, "transmitFile failure", e);
            } finally {
                stopDownload(socket, serverAddr);
            }
        } catch (Exception e) {
            try {mSaveFile.delete();} catch (Exception ignore) {}
        } finally {
            if (socket != null)
                socket.close();
            if (outputStream != null)
                outputStream.close();
        }
    }

    private void downloadBefore(DatagramSocket socket, InetSocketAddress srvAddr) throws IOException {
        Pkt.Seq seqNo = new Pkt.Seq();

        // value
        Message.FileDownloadBeforeReqMsg msg = Message.FileDownloadBeforeReqMsg.newBuilder()
                .setFilename(mFilename)
                .setSize(mFilesize)
                .setSeq(seqNo.toString())
                .setBufsize(PAYLOAD_SIZE)
                .setSrc(mUploadedBy)
                .setReserve(0)
                .build();

        // Pkt
        sendPkt = new Pkt.Builder()
                .setSeq(seqNo)
                .setDstAddr(Pkt.ADDR_SERVER)
                .setSrcAddr(userId)
                .setValue(msg)
                .build();

        GeneratedMessageV3 generatedMsg = sendRecive(socket, srvAddr, sendPkt, RESPONSE_TIMEOUT, new PktParser() {
            @Override
            public GeneratedMessageV3 protobufMessage(Pkt reqPkt, Pkt rspPkt) {
                if (rspPkt.isResponse()
                        && rspPkt.seq.equals(reqPkt.seq)
                        && rspPkt.tag.equals(reqPkt.tag)) {
                    try {
                        return rspPkt.getProtoBufMsg();
                    } catch (Exception e) {
                        L.d(TAG, "downloadBefore received data not valid", e);
                    }
                }
                return null;
            }
        });

        Message.FileDownloadBeforeRspMsg rspMsg = (Message.FileDownloadBeforeRspMsg) generatedMsg;
        if (rspMsg.getErrCode() != Message.ErrorCode.SUCCESS)
            throw new ServerResponseFailureException("FileDownloadBefore");
    }

    private GeneratedMessageV3 sendRecive(DatagramSocket socket, InetSocketAddress srvAddr, Pkt reqPkt, int timeoutMillis, PktParser pktParser) throws IOException {
        int retry = PKT_SEND_RETRY_TIMES;
        do {
            // send
            send(socket, srvAddr, reqPkt, retry != PKT_SEND_RETRY_TIMES);

            // receive
            try {
                return receive(socket, reqPkt, timeoutMillis, pktParser);
            } catch (SocketTimeoutException e) {
                if (--retry > 0) {
                    timeoutMillis <<= 1; // 延长 timeout
                    continue;
                }
                throw e;
            }
        } while (true);
    }

    private void send(DatagramSocket socket, InetSocketAddress srvAddr, Pkt reqPkt, boolean isRetry) throws IOException {
        byte[] sendDat = reqPkt.encode();
        L.d(TAG, (isRetry ? "resend" : "send") + " to " + srvAddr + ": " + reqPkt /*+ ": " + UTIL.bytesToHexString(sendDat)*/);
        DatagramPacket req = new DatagramPacket(sendDat, sendDat.length, srvAddr);
        socket.send(req);
    }

    private GeneratedMessageV3 receive(DatagramSocket socket, Pkt reqPkt, int timeoutMillis, PktParser pktParser) throws IOException {
        long lastTime = System.currentTimeMillis();
        byte[] buf = new byte[1024 * 2];
        do {
            DatagramPacket rspDp = new DatagramPacket(buf, buf.length);
            socket.setSoTimeout(timeoutMillis);
            socket.receive(rspDp);
            Pkt rspPkt = Pkt.decode(rspDp.getData(), rspDp.getOffset(), rspDp.getLength());
            L.d(TAG, "recv from " + rspDp.getSocketAddress() + ": " + rspPkt /*+ ": " + UTIL.bytesToHexString(rspDp)*/);
            GeneratedMessageV3 msg = pktParser.protobufMessage(reqPkt, rspPkt);
            if (msg != null)
                return msg;
            L.d(TAG, "invalid or repeated Pkt");
            long now = System.currentTimeMillis();
            timeoutMillis -= now - lastTime;
            lastTime = now;
            if (timeoutMillis <= 0)
                timeoutMillis = 1;
        } while (true);
    }

    private void transmitFile(DatagramSocket socket, InetSocketAddress srvAddr, final OutputStream outputStream) throws IOException {
        // receive file data
        fileFragmentIdx = -1;

        int leftSize = mFilesize;

        sendPkt.setTag(Message.Tag.FILE_DOWNLOAD_VALUE);

        PktParser pktParser = new PktParser() {
            @Override
            public GeneratedMessageV3 protobufMessage(Pkt reqPkt, Pkt rspPkt) {
                if (rspPkt.isResponse()
                        && rspPkt.seq.equals(reqPkt.seq)
                        && rspPkt.tag.equals(reqPkt.tag)) {
                    try {
                        Message.FileDownloadRspMsg rspMsg = rspPkt.getProtoBufMsg();
                        if (rspMsg.getIndex() == fileFragmentIdx)
                            return rspMsg;
                    } catch (Exception e) {
                        L.d(TAG, "download received data not valid", e);
                    }
                }
                return null;
            }
        };

        do {
            Message.FileDownloadReqMsg msg = Message.FileDownloadReqMsg.newBuilder()
                    .setIndex(++fileFragmentIdx)
                    .build();
            sendPkt.setValue(msg);

            GeneratedMessageV3 generatedMsg = sendRecive(socket, srvAddr, sendPkt, RESPONSE_TIMEOUT, pktParser);
            Message.FileDownloadRspMsg downloadRspMsg = (Message.FileDownloadRspMsg) generatedMsg;
            if (downloadRspMsg.getErrCode() != Message.ErrorCode.SUCCESS) {
                throw new ServerResponseFailureException("FileDownload");
            }
            outputStream.write(downloadRspMsg.getData().toByteArray());
            leftSize -= downloadRspMsg.getDataLen();
        } while (leftSize > 0);
    }

    private void stopDownload(DatagramSocket socket, InetSocketAddress srvAddr) throws IOException {
        Message.FileFinishedReqMsg msg = Message.FileFinishedReqMsg.newBuilder()
                .setFilename(mFilename)
                .setSize(mFilesize)
                .setSeq(sendPkt.seq.toString())
                .build();
        sendPkt.setValue(msg);

        sendRecive(socket, srvAddr, sendPkt, RESPONSE_TIMEOUT, new PktParser() {
            @Override
            public GeneratedMessageV3 protobufMessage(Pkt reqPkt, Pkt rspPkt) {
                if (rspPkt.isResponse()
                        && rspPkt.seq.equals(reqPkt.seq)
                        && rspPkt.tag.equals(reqPkt.tag)) {
                    try {
                        return rspPkt.getProtoBufMsg();
                    } catch (Exception e) {
                        L.d(TAG, "stopDownload received data not valid", e);
                    }
                }
                return null;
            }
        });
    }


    /**
     * 回响处理器
     */
    private interface PktParser {
        /**
         * 处理收到的包，解析为 protobuf.GeneratedMessageV3
         * @param reqPkt 请求包
         * @param rspPkt 回响包
         * @return false: protobuf.GeneratedMessageV3
         */
        GeneratedMessageV3 protobufMessage(Pkt reqPkt, Pkt rspPkt);
    }
}