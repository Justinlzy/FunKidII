package com.cqkct.FunKidII.service;

import android.text.TextUtils;

import com.cqkct.FunKidII.Utils.L;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import protocol.Message;

/**
 * 上传文件
 * <p> &nbsp;&nbsp;&nbsp;&nbsp; stage 1: Tag:0x0f04 上传文件前协商请求
 * <p> &nbsp;&nbsp;&nbsp;&nbsp; stage 2: Tag:0x0f05 上传文件数据
 * <p> &nbsp;&nbsp;&nbsp;&nbsp; stage 3: Tag:0x0f08 文件传输完成通知
 * <pre>
 *  Usage:
 *      FileUploader uploader = new FileUploader(File);
 *      uploader.upload();
 * </pre>
 */
public class FileUploader { // FIXME: 实现为service
    private static final String TAG = FileUploader.class.getSimpleName();
    private static final int RESPONSE_TIMEOUT = 1000;

    private File mFile;

    private String userId;

    private Pkt sendPkt;

    /** 文件数据包索引 */
    private int fileFragmentIdx;
    private byte[] fileBuf = new byte[1024];

    /** 数据包发送 + 重发次数 */
    private static final int PKT_SEND_RETRY_TIMES = 3;


    public FileUploader(File file, String userId) {
        mFile = file;
        this.userId = userId;
    }

    public FileUploader(String file, String userId) {
        mFile = new File(file);
        this.userId = userId;
    }

    public void upload() throws Exception {
        L.d(TAG, "file: " + mFile.getAbsolutePath());

        if (mFile.length() > Integer.MAX_VALUE)
            throw new Exception("File length too large!!!");

        InetSocketAddress serverAddr = new InetSocketAddress(Pkt.SERVER_DOMAIN, Pkt.SERVER_UDP_FILE_PORT);

        if (TextUtils.isEmpty(userId))
            throw new Exception("there is no user!!!");

        DatagramSocket socket = null;
        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(mFile));
            socket = new DatagramSocket();
            uploadBefore(socket, serverAddr);
            try {
                transmitFile(socket, serverAddr, inputStream);
                L.v(TAG, "transmitFile end");
            } catch (Exception e) {
                L.d(TAG, "transmitFile failure", e);
            } finally {
                stopUpload(socket, serverAddr);
            }
        } finally {
            if (socket != null)
                socket.close();
            if (inputStream != null)
                inputStream.close();
        }
    }

    private void uploadBefore(DatagramSocket socket, InetSocketAddress srvAddr) throws IOException {
        Pkt.Seq seqNo = new Pkt.Seq();

        // value
        Message.FileUploadBeforeReqMsg msg = Message.FileUploadBeforeReqMsg.newBuilder()
                .setFilename(mFile.getName())
                .setSize((int) mFile.length())
                .setSeq(seqNo.toString())
                .setBufsize(fileBuf.length)
                .setReserve(1)
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
                        L.d(TAG, "uploadBefore received data not valid", e);
                    }
                }
                return null;
            }
        });

        Message.FileUploadBeforeRspMsg rspMsg = (Message.FileUploadBeforeRspMsg) generatedMsg;
        if (rspMsg.getErrCode() != Message.ErrorCode.SUCCESS) {
            throw new ServerResponseFailureException("FileUploadBefore");
        }
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
            L.d(TAG, "recv from " + rspDp.getSocketAddress() + ": " + rspPkt);
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

    private void transmitFile(DatagramSocket socket, InetSocketAddress srvAddr, InputStream inputStream) throws IOException {
        // send file data

        fileFragmentIdx = -1;

        sendPkt.setTag(Message.Tag.FILE_UPLOAD_VALUE);

        PktParser pktParser = new PktParser() {
            @Override
            public GeneratedMessageV3 protobufMessage(Pkt reqPkt, Pkt rspPkt) {
                if (rspPkt.isResponse()
                        && rspPkt.seq.equals(reqPkt.seq)
                        && rspPkt.tag.equals(reqPkt.tag)) {
                    try {
                        Message.FileUploadRspMsg msg = rspPkt.getProtoBufMsg();
                        if (msg.getIndex() == fileFragmentIdx)
                            return msg;
                    } catch (Exception e) {
                        L.d(TAG, "upload received data not valid", e);
                    }
                }
                return null;
            }
        };
        do {
            int readn = inputStream.read(fileBuf);
            if (readn <= 0)
                break;
            Message.FileUploadReqMsg reqMsg = Message.FileUploadReqMsg.newBuilder()
                    .setIndex(++fileFragmentIdx)
                    .setData(ByteString.copyFrom(fileBuf, 0, readn))
                    .setDataLen(readn)
                    .build();
            sendPkt.setValue(reqMsg);

            GeneratedMessageV3 generatedMsg = sendRecive(socket, srvAddr, sendPkt, RESPONSE_TIMEOUT, pktParser);
            Message.FileUploadRspMsg rspMsg = (Message.FileUploadRspMsg) generatedMsg;
            if (rspMsg.getErrCode() != Message.ErrorCode.SUCCESS) {
                throw new ServerResponseFailureException("FileUpload");
            }
        } while (true);
    }

    private void stopUpload(DatagramSocket socket, InetSocketAddress srvAddr) throws IOException {
        Message.FileFinishedReqMsg reqMsg = Message.FileFinishedReqMsg.newBuilder()
                .setFilename(mFile.getName())
                .setSize((int) mFile.length())
                .setSeq(sendPkt.seq.toString())
                .build();
        sendPkt.setValue(reqMsg);

        sendRecive(socket, srvAddr, sendPkt, RESPONSE_TIMEOUT, new PktParser() {
            @Override
            public GeneratedMessageV3 protobufMessage(Pkt reqPkt, Pkt rspPkt) {
                if (rspPkt.isResponse()
                        && rspPkt.seq.equals(reqPkt.seq)
                        && rspPkt.tag.equals(reqPkt.tag)) {
                    try {
                        return rspPkt.getProtoBufMsg();
                    } catch (Exception e) {
                        L.d(TAG, "stopUpload received data not valid", e);
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