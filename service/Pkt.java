package com.cqkct.FunKidII.service;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;

import com.cqkct.FunKidII.Utils.ByteUtils;
import com.cqkct.FunKidII.Utils.L;
import com.google.protobuf.GeneratedMessageV3;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import protocol.Message;

/**
 * 与服务器通信的数据包
 */
public class Pkt implements Serializable, Parcelable {

    public static final String SERVER_DOMAIN = "kid.cqkct.com";
    public static final int SERVER_TLC_MSG_PORT = 6618;
    public static final int SERVER_TLC_MSG_TLS_PORT = 6619;
    public static final int SERVER_UDP_FILE_PORT = 6638;

    private static final Charset UTF_8;
    static {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            UTF_8 = StandardCharsets.UTF_8;
        } else {
            UTF_8 = Charset.forName("UTF-8");
        }
    }

    public static final byte SOP = '(';
    public static final byte EOP = ')';
    public static final int IDX_SOP = 0;
    public static final int LEN_SOP = 1;
    public static final int IDX_LEN = IDX_SOP + LEN_SOP;
    public static final int LEN_LEN = 4;
    public static final int IDX_VER = IDX_LEN + LEN_LEN;
    public static final int IDX_VER_major = IDX_VER;
    public static final int IDX_VER_minor = IDX_VER + 1;
    public static final int LEN_VER = 2;
    public static final int IDX_DIR = IDX_VER + LEN_VER;
    public static final int LEN_DIR = 1;
    public static final int IDX_SEQ = IDX_DIR + LEN_DIR;
    public static final int LEN_SEQ = 32;
    public static final int IDX_ADDR = IDX_SEQ + LEN_SEQ;
    public static final int LEN_SUM = 2;
    public static final int LEN_EOP = 1;

    public static final int IDX_OF_TLV = 0;
    public static final int LEN_OF_TLV = 2;
    public static final int IDX_LEN_OF_TLV = IDX_OF_TLV + LEN_OF_TLV;
    public static final int LEN_LEN_OF_TLV = 4;
    public static final int IDX_VALUE_OF_TLV = IDX_LEN_OF_TLV + LEN_LEN_OF_TLV;

    public static final int LEN_MIN = LEN_SOP + LEN_LEN + LEN_VER + LEN_DIR + LEN_SEQ + 4 /* address pair ?:?\0 */ + LEN_OF_TLV + LEN_LEN_OF_TLV + LEN_SUM + LEN_EOP;

    public static final int VER_MAJOR = 0;
    public static final int VER_MINOR = 2;

    public static final byte DIR_REQUEST = 0;
    public static final byte DIR_RESPONSE = 1;

    public static final String ADDR_SERVER = new String(new byte[]{'0'}, UTF_8);

    /**
     * 包长度
     * <p>
     *     该值在解析后的包有效。
     * </p>
     */
    public int len = -1;

    /**
     * 主版本号
     */
    public byte major = VER_MAJOR;

    /**
     * 次版本号
     */
    public byte minor = VER_MINOR;

    /**
     * 传输方向。
     * 0 请求，1 应答
     */
    public Byte dir = null;

    public static class Seq {
        @NonNull
        private byte[] dat;

        public Seq() {
            UUID uuid = UUID.randomUUID();
            dat = uuid.toString().replace("-", "").getBytes();
        }

        public Seq(@NonNull byte[] raw, int offset) {
            dat = new byte[LEN_SEQ];
            System.arraycopy(raw, offset, dat, 0, LEN_SEQ);
        }

        public Seq(@NonNull byte[] raw) {
            this(raw, 0);
        }

        public Seq(@NonNull String str) {
            this(str.getBytes());
        }

        /** Cache the hash code */
        private int hash;
        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                for (byte b : dat) {
                    h = 31 * h + b;
                }
                hash = h;
            }
            return h;
        }

        @Override
        public boolean equals(Object anObject) {
            if (this == anObject) {
                return true;
            }
            if (anObject instanceof Seq) {
                Seq anotherSeq = (Seq) anObject;
                for (int i = 0; i < LEN_SEQ; ++i) {
                    if (anotherSeq.dat[i] != dat[i])
                        return false;
                }
                return true;
            }
            return false;
        }

        public byte[] getBytes() {
            return dat;
        }

        @Override
        public String toString() {
            return new String(dat);
        }
    }

    /**
     * 流水号
     */
    public Seq seq = null;

    /**
     * 目的地址
     */
    public String dstAddr = "";

    /**
     * 源地址
     */
    public String srcAddr = "";

    /**
     * TLV 标签，表示命令码
     */
    public Short tag = null;

    /**
     * TLV 数据
     */
    public byte[] value = null;

    /**
     * TLV 数据对应的 protoBuf
     */
    private GeneratedMessageV3 protoBufMsg;
    private boolean _protoBufUnmarshaled = false;

    /** 记录成员变量是否有变化，有变化时，raw 成员应该重新生成 */
    private boolean _classMemberChanged = false;

    /** 编码后的数据 */
    private byte[] _raw;


    public boolean isResponse() {
        return dir == DIR_RESPONSE;
    }

    public boolean isRequest() {
        return dir == DIR_REQUEST;
    }

    public byte[] getRaw() {
        return _raw;
    }

    public void setMajor(byte major) {
        if (this.major != major) {
            this.major = major;
            _classMemberChanged = true;
        }
    }

    public void setMajor(int major) {
        setMajor((byte) major);
    }

    public void setMinor(byte minor) {
        if (this.minor != minor) {
            this.minor = minor;
            _classMemberChanged = true;
        }
    }

    public void setMinor(int minor) {
        setMinor((byte) minor);
    }

    public void setDir(byte dir) {
        if (this.dir != dir) {
            this.dir = dir;
            _classMemberChanged = true;
            _protoBufUnmarshaled = false;
        }
    }

    public void setDir(int dir) {
        setDir((byte) dir);
    }

    public void setDir(boolean isRequest) {
        byte dir = isRequest ? DIR_REQUEST : DIR_RESPONSE;
        setDir(dir);
    }

    public void setSeq(@NonNull Seq seq) {
        if (this.seq == null || !this.seq.equals(seq)) {
            this.seq = seq;
            _classMemberChanged = true;
        }
    }

    public void setDstAddr(String dstAddr) {
        if (dstAddr == null) {
            if (!TextUtils.isEmpty(this.dstAddr)) {
                this.dstAddr = "";
                _classMemberChanged = true;
            }
        } else if (!dstAddr.equals(this.dstAddr)) {
            this.dstAddr = dstAddr;
            _classMemberChanged = true;
        }
    }

    public void setSrcAddr(String srcAddr) {
        if (srcAddr == null) {
            if (!TextUtils.isEmpty(this.srcAddr)) {
                this.srcAddr = "";
                _classMemberChanged = true;
            }
        } else if (!srcAddr.equals(this.srcAddr)) {
            this.srcAddr = srcAddr;
            _classMemberChanged = true;
        }
    }

    public void setTag(short tag) {
        if (this.tag == null || this.tag != tag) {
            this.tag = tag;
            _classMemberChanged = true;
            _protoBufUnmarshaled = false;
        }
    }

    public void setTag(int tag) {
        setTag((short) tag);
    }

    private void setValue(byte[] value) {
        this.value = value;
        _classMemberChanged = true;
        _protoBufUnmarshaled = false;
    }

    public void setValue(GeneratedMessageV3 protoBufMsg) {
        setValue(protoBufMsg.toByteArray());
        this.protoBufMsg = protoBufMsg;
    }

    public GeneratedMessageV3 getBaseProtoBufMsg() {
        if (protoBufMsg == null && tag != null && dir != null && value != null && !_protoBufUnmarshaled) {
            protoBufMsg = protoBufUnmarshal(tag, dir, value);
            _protoBufUnmarshaled = true;
        }
        return protoBufMsg;
    }

    public <T extends GeneratedMessageV3> T getProtoBufMsg() throws ClassCastException {
        return (T) getBaseProtoBufMsg();
    }

    public static class Builder {
        private byte major = VER_MAJOR;
        private byte minor = VER_MINOR;
        private Byte dir = null;
        private Seq seq = null;
        private String dstAddr = ADDR_SERVER;
        private String srcAddr;
        private Short tag = null;
        private byte[] value;
        private GeneratedMessageV3 protoBufMsg;

        public Builder setMajor(int major) {
            this.major = (byte) major;
            return this;
        }

        public Builder setMinor(int minor) {
            this.minor = (byte) minor;
            return this;
        }

        public Builder setDir(int dir) {
            this.dir = (byte) dir;
            return this;
        }

        public Builder setDir(boolean isRequest) {
            this.dir = isRequest ? DIR_REQUEST : DIR_RESPONSE;
            return this;
        }

        public Builder setSeq(@NonNull Seq seq) {
            this.seq = seq;
            return this;
        }

        public Builder setDstAddr(String dstAddr) {
            this.dstAddr = dstAddr;
            return this;
        }

        public Builder setSrcAddr(String srcAddr) {
            this.srcAddr = srcAddr;
            return this;
        }

        public Builder setTag(short tag) {
            this.tag = tag;
            return this;
        }

        public Builder setTag(int tag) {
            return setTag((short) tag);
        }

        public Builder setValue(@NonNull GeneratedMessageV3 protoBufMsg) {
            Long key = getKeyByProtoBufMessage(protoBufMsg);
            if (key == null) {
                throw new UnsupportedMessageException(protoBufMsg.getClass().getSimpleName());
            }
            this.tag = (short) ((key >> 8) & 0xFFFF);
            this.dir = (byte) (key & 0xFF);
            this.value = protoBufMsg.toByteArray();
            this.protoBufMsg = protoBufMsg;
            return this;
        }

        public Pkt build() {
            Pkt pkt = new Pkt();
            pkt.major = major;
            pkt.minor = minor;
            pkt.dir = dir;
            if (seq == null)
                seq = new Seq();
            pkt.seq = seq;
            pkt.dstAddr = dstAddr;
            pkt.srcAddr = srcAddr;
            pkt.tag = tag;
            pkt.value = value;
            pkt.protoBufMsg = protoBufMsg;
            pkt._classMemberChanged = true;
            return pkt;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 将 raw 数据解码为 Pkt
     * @param raw 数据 buffer
     * @param offset 偏移
     * @param length 有效数据字节数
     * @return Pkt
     * @throws InvalidLenghtException
     * @throws InvalidStartOfPktException
     * @throws InvalidEndOfPktException
     * @throws ChecksumException
     * @throws AddressPairException
     */
    public static Pkt decode(byte[] raw, int offset, int length) throws InvalidLenghtException, InvalidStartOfPktException, InvalidEndOfPktException, ChecksumException, AddressPairException {
        check(raw, offset, length);

        Pkt pkt = new Pkt();

        pkt.len = ByteUtils.getIntBE(raw, offset + IDX_LEN);

        pkt.major = raw[offset + IDX_VER_major];
        pkt.minor = raw[offset + IDX_VER_minor];

        pkt.dir = raw[offset + IDX_DIR];

        pkt.seq = new Seq(raw, offset + IDX_SEQ);

        int srcAddrIdx = indexOf(raw, offset + IDX_ADDR, (byte) ':');
        if (srcAddrIdx < 0)
            throw new AddressPairException("Not found separator \":\"");
        pkt.dstAddr = new String(raw, offset + IDX_ADDR, srcAddrIdx - (offset + IDX_ADDR), UTF_8);
        srcAddrIdx += 1;
        int endOfAddr = indexOf(raw, srcAddrIdx, (byte) '\0');
        if (endOfAddr < 0)
            throw new AddressPairException("Not found separator \"\\0\"");
        pkt.srcAddr = new String(raw, srcAddrIdx, endOfAddr - srcAddrIdx, UTF_8);

        int tlvIdx = endOfAddr + 1;
        pkt.tag = ByteUtils.getShortBE(raw, tlvIdx + IDX_OF_TLV);
        int lenOfTlv = ByteUtils.getIntBE(raw, tlvIdx + IDX_LEN_OF_TLV);
        pkt.value = new byte[lenOfTlv];
        if (lenOfTlv > 0)
            System.arraycopy(raw, tlvIdx + IDX_VALUE_OF_TLV, pkt.value, 0, lenOfTlv);

        if (offset == 0 && length == raw.length) {
            pkt._raw = raw;
        } else {
            pkt._raw = new byte[length];
            System.arraycopy(raw, offset, pkt._raw, 0, length);
        }
        pkt._classMemberChanged = false;

        pkt.protoBufMsg = protoBufUnmarshal(pkt.tag, pkt.dir, pkt.value);
        pkt._protoBufUnmarshaled = true;

        return pkt;
    }

    private static GeneratedMessageV3 protoBufUnmarshal(int tag, int dir, byte[] value) {
        Class<? extends GeneratedMessageV3> clazz = getProtoBufMessageClassByTag(tag, dir);
        if (clazz != null) {
            try {
                Method method = clazz.getMethod("parseFrom", byte[].class);
                return (GeneratedMessageV3) method.invoke(null, (Object) value);
            } catch (Exception e) {
                L.e("Pkt", "parseFrom for tag: " + tag + " dir: " + dir, e);
            }
        } else {
            L.e("Pkt", "NotSupportMessageException", new UnsupportedMessageException("" + tag + " " + dir));
        }
        return null;
    }

    /**
     * 将 raw 数据解码为 Pkt
     * @param raw 数据 buffer
     * @return Pkt
     * @throws InvalidLenghtException
     * @throws InvalidStartOfPktException
     * @throws InvalidEndOfPktException
     * @throws ChecksumException
     * @throws AddressPairException
     */
    public static Pkt decode(byte[] raw) throws InvalidLenghtException, InvalidStartOfPktException, InvalidEndOfPktException, ChecksumException, AddressPairException {
        return decode(raw, 0, raw.length);
    }

    /**
     * 检查数据是否为有效的 Pkt 数据
     * @param raw 数据
     * @throws InvalidLenghtException
     * @throws InvalidStartOfPktException
     * @throws InvalidEndOfPktException
     * @throws ChecksumException
     */
    public static void check(byte[] raw, int offset, int length) throws InvalidLenghtException, InvalidStartOfPktException, InvalidEndOfPktException, ChecksumException {
        short sum, calcSum;

        if (length < LEN_MIN)
            throw new InvalidLenghtException();
        if (raw[offset + IDX_SOP] != SOP)
            throw new InvalidStartOfPktException();
        if (raw[offset + length - 1] != EOP)
            throw new InvalidEndOfPktException();
        int len = ByteUtils.getIntBE(raw, offset + IDX_LEN);
        if (len != length)
            throw new InvalidLenghtException("expect: " + len + ", real: " + length);

        // check address
        int srcAddrIdx = indexOf(raw, offset + IDX_ADDR, (byte) ':');
        if (srcAddrIdx < 0)
            throw new AddressPairException("separator \':\' not fond");
        if (indexOf(raw, srcAddrIdx + 1, (byte) '\0') < 0)
            throw new AddressPairException("address terminator(\\0) not fond");

        sum = ByteUtils.getShortBE(raw, offset + length - 1 - LEN_EOP - 1);
        calcSum = 0;
        for (int i = offset, endPos = offset + length - (LEN_SUM + LEN_EOP); i < endPos; ++i) {
            calcSum += raw[i] & 0xFF;
        }
        if (calcSum != sum)
            throw new ChecksumException();
    }

    public static void check(byte[] raw) throws InvalidLenghtException, InvalidStartOfPktException, InvalidEndOfPktException, ChecksumException {
        check(raw, 0, raw.length);
    }

    /**
     * 取得数据包中的数据段的索引
     * @param pktRaw Pkt byte 数组
     * @return Pkt 数据段的 index
     */
    public static int indexOfDataInPkt(byte[] pktRaw) {
        int ret = -1;
        for (int idx = IDX_ADDR; idx < (pktRaw.length - (LEN_SUM + LEN_EOP)); ++idx) {
            if (pktRaw[idx] == '\0') {
                ret = idx + 1;
                break;
            }
        }
        return ret;
    }

    /**
     * 取得数据包中的数据段
     * @param pktRaw Pkt byte 数组
     * @return 数据段的数据
     */
    public static byte[] getDataInPkt(byte[] pktRaw) {
        int idx = indexOfDataInPkt(pktRaw);
        if (idx < 0)
            return null;
        int len = pktRaw.length - (LEN_SUM + LEN_EOP) - idx;
        byte[] dat = new byte[len];
        if (len > 0)
            System.arraycopy(pktRaw, idx, dat, 0, len);
        return dat;
    }

    public byte[] encode() {
        if (_raw != null && !_classMemberChanged)
            return _raw;

        // 准备地址对
        String addrPair = (dstAddr == null ? "" : dstAddr) + ':' + (srcAddr == null ? "" : srcAddr) + '\0';
        byte[] addrPairBytes = addrPair.getBytes(UTF_8);

        // 计算包长度
        int pktLen = LEN_SOP + LEN_LEN + LEN_VER + LEN_DIR + LEN_SEQ;
        pktLen += addrPairBytes.length;
        pktLen += LEN_OF_TLV + LEN_LEN_OF_TLV + (value == null ? 0 : value.length);
        pktLen += LEN_SUM + LEN_EOP;

        // 分配内存
        byte[] raw = new byte[pktLen];


        int idx = 0;

        // 开始符
        raw[idx++] = SOP;

        // 长度
        ByteUtils.toByteArrayBE(pktLen, raw, idx);
        idx += 4;

        // 版本号
        raw[idx++] = major;
        raw[idx++] = minor;

        // 类型
        if (dir == null)
            throw new NullPointerException("dir of Pkt is null");
        raw[idx++] = dir;

        // 业务流水号
        if (seq == null)
            seq = new Seq();
        System.arraycopy(seq.getBytes(), 0, raw, idx, LEN_SEQ);
        idx += LEN_SEQ;

        // 地址对
        System.arraycopy(addrPairBytes, 0, raw, idx, addrPairBytes.length);
        idx += addrPairBytes.length;

        // 数据段
        if (tag == null)
            throw new NullPointerException("tag of Pkt is null");
        ByteUtils.toByteArrayBE(tag, raw, idx);
        idx += LEN_OF_TLV;
        int lenOfValueOfTLV = value == null ? 0 : value.length;
        ByteUtils.toByteArrayBE(lenOfValueOfTLV, raw, idx);
        idx += LEN_LEN_OF_TLV;
        if (value != null && value.length > 0) {
            System.arraycopy(value, 0, raw, idx, value.length);
            idx += value.length;
        }

        // 校验和
        short sum = 0;
        for (int i = 0; i < idx; ++i) {
            sum += raw[i] & 0xFF;
        }
        ByteUtils.toByteArrayBE(sum, raw, idx);
        idx += LEN_SUM;

        // 结束符
        raw[idx] = EOP;

        _raw = raw;

        return raw;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('v').append(major).append('.').append(minor)
                .append(' ')
                .append(dir == 0 ? "REQ" : "RSP")
                .append(' ')
                .append(seq)
                .append(' ')
                .append(dstAddr).append(':').append(srcAddr)
                .append(" [")
                .append(String.format("0x%04X ", tag))
                .append(value == null ? "" : String.valueOf(value.length))
                .append(']');
        return sb.toString();
    }

    public String seqBitOrTag() {
        if (seq == null || tag == null)
            return null;
        return seq.toString() + tag;
    }

    public static int indexOf(byte[] dat, int offset, int endPos, byte key) {
        for (int i = offset; i < endPos; ++i) {
            if (dat[i] == key)
                return i;
        }
        return -1;
    }

    public static int indexOf(byte[] dat, int offset, byte key) {
        return indexOf(dat, offset, dat.length, key);
    }

    public static int indexOf(byte[] dat, byte key) {
        for (int i = 0; i < dat.length; ++i) {
            if (dat[i] == key)
                return i;
        }
        return -1;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(len);
        out.writeByte(major);
        out.writeByte(minor);
        out.writeByte(dir);
        out.writeByteArray(seq.getBytes());
        out.writeString(dstAddr);
        out.writeString(srcAddr);
        out.writeInt(tag & 0xFFFF);
        out.writeByteArray(value);
        out.writeByte((byte) (_classMemberChanged ? 1 : 0));
        out.writeByteArray(_raw);
    }

    public static final Creator<Pkt> CREATOR = new Creator<Pkt>() {
        public Pkt createFromParcel(Parcel in) {
            Pkt pkt = new Pkt();
            pkt.len = in.readInt();
            pkt.major = in.readByte();
            pkt.minor = in.readByte();
            pkt.dir = in.readByte();
            pkt.seq = new Seq(in.createByteArray());
            pkt.dstAddr = in.readString();
            pkt.srcAddr = in.readString();
            pkt.tag = (short) (in.readInt() & 0xFFFF);
            pkt.value = in.createByteArray();
            pkt._classMemberChanged = in.readByte() != 0;
            pkt._raw = in.createByteArray();
            return pkt;
        }

        public Pkt[] newArray(int size) {
            return new Pkt[size];
        }
    };



    private static final Map<Class<? extends GeneratedMessageV3>, Long> protoBufMsgTagMap = new HashMap<>();
    static {
        protoBufMsgTagMap.put(Message.HeartReqMsg.class, (((long) Message.Tag.HEARTRATE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.HeartRspMsg.class, (((long) Message.Tag.HEARTRATE_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.CheckAppVerReqMsg.class, (((long) Message.Tag.CHECK_APP_VER_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.CheckAppVerRspMsg.class, (((long) Message.Tag.CHECK_APP_VER_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.LoginReqMsg.class, (((long) Message.Tag.LOGIN_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.LoginRspMsg.class, (((long) Message.Tag.LOGIN_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.RegisterReqMsg.class, (((long) Message.Tag.REGISTER_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.RegisterRspMsg.class, (((long) Message.Tag.REGISTER_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.ChangePwdReqMsg.class, (((long) Message.Tag.CHANGE_PASSWD_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ChangePwdRspMsg.class, (((long) Message.Tag.CHANGE_PASSWD_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.CheckUserReqMsg.class, (((long) Message.Tag.CHECK_USER_EXISTS_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.CheckUserRspMsg.class, (((long) Message.Tag.CHECK_USER_EXISTS_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.ForceExitReqMsg.class, (((long) Message.Tag.ON_EXTRUDED_LOGIN_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ForceExitRspMsg.class, (((long) Message.Tag.ON_EXTRUDED_LOGIN_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.SetPwdReqMsg.class, (((long) Message.Tag.SET_PASSWD_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.SetPwdRspMsg.class, (((long) Message.Tag.SET_PASSWD_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.DeviceLoginReqMsg.class, (((long) Message.Tag.DEV_LOGIN_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DeviceLoginRspMsg.class, (((long) Message.Tag.DEV_LOGIN_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.FetchUserInfoReqMsg.class, (((long) Message.Tag.FETCH_USER_INFO_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FetchUserInfoRspMsg.class, (((long) Message.Tag.FETCH_USER_INFO_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.AssociateThirdAccountReqMsg.class, (((long) Message.Tag.ASSOCIATE_THIRD_ACCOUNT_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.AssociateThirdAccountRspMsg.class, (((long) Message.Tag.ASSOCIATE_THIRD_ACCOUNT_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.DisassociateThirdAccountReqMsg.class, (((long) Message.Tag.DISASSOCIATE_THIRD_ACCOUNT_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DisassociateThirdAccountRspMsg.class, (((long) Message.Tag.DISASSOCIATE_THIRD_ACCOUNT_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.SetAppPushConfReqMsg.class, (((long) Message.Tag.SET_APP_PUSH_CONF_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.SetAppPushConfRspMsg.class, (((long) Message.Tag.SET_APP_PUSH_CONF_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.QueryThirdAccountByPhoneReqMsg.class, (((long) Message.Tag.QUERY_THIRD_ACCOUNT_BY_PHONE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.QueryThirdAccountByPhoneRspMsg.class, (((long) Message.Tag.QUERY_THIRD_ACCOUNT_BY_PHONE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetNotificationChannelReqMsg.class, (((long) Message.Tag.GET_NOTIFICATION_CHANNEL_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetNotificationChannelRspMsg.class, (((long) Message.Tag.GET_NOTIFICATION_CHANNEL_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.SetNotificationChannelReqMsg.class, (((long) Message.Tag.SET_NOTIFICATION_CHANNEL_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.SetNotificationChannelRspMsg.class, (((long) Message.Tag.SET_NOTIFICATION_CHANNEL_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.CheckDeviceReqMsg.class, (((long) Message.Tag.CHECK_DEVICE_ACTIVATION_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.CheckDeviceRspMsg.class, (((long) Message.Tag.CHECK_DEVICE_ACTIVATION_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.FetchUserListReqMsg.class, (((long) Message.Tag.FETCH_USERS_OF_DEVICE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FetchUserListRspMsg.class, (((long) Message.Tag.FETCH_USERS_OF_DEVICE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.FetchDeviceListReqMsg.class, (((long) Message.Tag.FETCH_DEVICES_OF_USER_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FetchDeviceListRspMsg.class, (((long) Message.Tag.FETCH_DEVICES_OF_USER_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.BindDevReqMsg.class, (((long) Message.Tag.BIND_DEVICE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.BindDevRspMsg.class, (((long) Message.Tag.BIND_DEVICE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyAdminBindDevReqMsg.class, (((long) Message.Tag.ON_BIND_DEVICE_REQUEST_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyAdminBindDevRspMsg.class, (((long) Message.Tag.ON_BIND_DEVICE_REQUEST_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyUserBindDevReqMsg.class, (((long) Message.Tag.ON_DEVICE_BIND_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyUserBindDevRspMsg.class, (((long) Message.Tag.ON_DEVICE_BIND_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.UnbindDevReqMsg.class, (((long) Message.Tag.UNBIND_DEVICE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.UnbindDevRspMsg.class, (((long) Message.Tag.UNBIND_DEVICE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.UnbindDevSelfReqMsg.class, (((long) Message.Tag.DEVICE_UNBIND_SELF_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.UnbindDevSelfRspMsg.class, (((long) Message.Tag.DEVICE_UNBIND_SELF_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyUserUnbindDevReqMsg.class, (((long) Message.Tag.ON_DEVICE_UNBIND_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyUserUnbindDevRspMsg.class, (((long) Message.Tag.ON_DEVICE_UNBIND_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.FetchUsrDevParticReqMsg.class, (((long) Message.Tag.FETCH_USR_DEV_PARTIC_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FetchUsrDevParticRspMsg.class, (((long) Message.Tag.FETCH_USR_DEV_PARTIC_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.PushDevConfReqMsg.class, (((long) Message.Tag.PUSH_DEV_CONF_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.PushDevConfRspMsg.class, (((long) Message.Tag.PUSH_DEV_CONF_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.FetchDevConfReqMsg.class, (((long) Message.Tag.FETCH_DEV_CONF_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FetchDevConfRspMsg.class, (((long) Message.Tag.FETCH_DEV_CONF_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyDevConfChangedReqMsg.class, (((long) Message.Tag.ON_DEV_CONF_CHANGED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyDevConfChangedRspMsg.class, (((long) Message.Tag.ON_DEV_CONF_CHANGED_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyDevConfSyncedReqMsg.class, (((long) Message.Tag.ON_DEV_CONF_SYNCED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyDevConfSyncedRspMsg.class, (((long) Message.Tag.ON_DEV_CONF_SYNCED_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.AddFenceReqMsg.class, (((long) Message.Tag.ADD_FENCE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.AddFenceRspMsg.class, (((long) Message.Tag.ADD_FENCE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.DelFenceReqMsg.class, (((long) Message.Tag.DEL_FENCE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DelFenceRspMsg.class, (((long) Message.Tag.DEL_FENCE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.ModifyFenceReqMsg.class, (((long) Message.Tag.MODIFY_FENCE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ModifyFenceRspMsg.class, (((long) Message.Tag.MODIFY_FENCE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetFenceReqMsg.class, (((long) Message.Tag.GET_FENCE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetFenceRspMsg.class, (((long) Message.Tag.GET_FENCE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyFenceChangedReqMsg.class, (((long) Message.Tag.ON_DEV_FENCE_CHANGED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyFenceChangedRspMsg.class, (((long) Message.Tag.ON_DEV_FENCE_CHANGED_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.AddSosReqMsg.class, (((long) Message.Tag.ADD_SOS_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.AddSosRspMsg.class, (((long) Message.Tag.ADD_SOS_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.DelSosReqMsg.class, (((long) Message.Tag.DEL_SOS_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DelSosRspMsg.class, (((long) Message.Tag.DEL_SOS_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.ModifySosReqMsg.class, (((long) Message.Tag.MODIFY_SOS_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ModifySosRspMsg.class, (((long) Message.Tag.MODIFY_SOS_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetSosReqMsg.class, (((long) Message.Tag.GET_SOS_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetSosRspMsg.class, (((long) Message.Tag.GET_SOS_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifySosChangedReqMsg.class, (((long) Message.Tag.ON_DEV_SOS_CHANGED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifySosChangedRspMsg.class, (((long) Message.Tag.ON_DEV_SOS_CHANGED_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifySosSyncedReqMsg.class, (((long) Message.Tag.ON_DEV_SOS_SYNCED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifySosSyncedRspMsg.class, (((long) Message.Tag.ON_DEV_SOS_SYNCED_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.ChangeSosCallOrderReqMsg.class, (((long) Message.Tag.CHANGE_SOS_CALL_ORDER_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ChangeSosCallOrderRspMsg.class, (((long) Message.Tag.CHANGE_SOS_CALL_ORDER_VALUE) << 8) | DIR_RESPONSE);



        protoBufMsgTagMap.put(Message.AddContactReqMsg.class, (((long) Message.Tag.ADD_CONTACT_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.AddContactRspMsg.class, (((long) Message.Tag.ADD_CONTACT_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.DelContactReqMsg.class, (((long) Message.Tag.DEL_CONTACT_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DelContactRspMsg.class, (((long) Message.Tag.DEL_CONTACT_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.ModifyContactReqMsg.class, (((long) Message.Tag.MODIFY_CONTACT_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ModifyContactRspMsg.class, (((long) Message.Tag.MODIFY_CONTACT_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetContactReqMsg.class, (((long) Message.Tag.GET_CONTACT_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetContactRspMsg.class, (((long) Message.Tag.GET_CONTACT_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyContactChangedReqMsg.class, (((long) Message.Tag.ON_DEV_CONTACT_CHANGED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyContactChangedRspMsg.class, (((long) Message.Tag.ON_DEV_CONTACT_CHANGED_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyContactSyncedReqMsg.class, (((long) Message.Tag.ON_DEV_CONTACT_SYNCED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyContactSyncedRspMsg.class, (((long) Message.Tag.ON_DEV_CONTACT_SYNCED_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.AddAlarmClockReqMsg.class, (((long) Message.Tag.ADD_ALARM_CLOCK_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.AddAlarmClockRspMsg.class, (((long) Message.Tag.ADD_ALARM_CLOCK_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.DelAlarmClockReqMsg.class, (((long) Message.Tag.DEL_ALARM_CLOCK_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DelAlarmClockRspMsg.class, (((long) Message.Tag.DEL_ALARM_CLOCK_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.ModifyAlarmClockReqMsg.class, (((long) Message.Tag.MODIFY_ALARM_CLOCK_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ModifyAlarmClockRspMsg.class, (((long) Message.Tag.MODIFY_ALARM_CLOCK_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetAlarmClockReqMsg.class, (((long) Message.Tag.GET_ALARM_CLOCK_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetAlarmClockRspMsg.class, (((long) Message.Tag.GET_ALARM_CLOCK_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyAlarmClockChangedReqMsg.class, (((long) Message.Tag.ON_DEV_ALARM_CLOCK_CHANGED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyAlarmClockChangedRspMsg.class, (((long) Message.Tag.ON_DEV_ALARM_CLOCK_CHANGED_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyAlarmClockSyncedReqMsg.class, (((long) Message.Tag.ON_DEV_ALARM_CLOCK_SYNCED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyAlarmClockSyncedRspMsg.class, (((long) Message.Tag.ON_DEV_ALARM_CLOCK_SYNCED_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.AddClassDisableReqMsg.class, (((long) Message.Tag.ADD_CLASS_DISABLE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.AddClassDisableRspMsg.class, (((long) Message.Tag.ADD_CLASS_DISABLE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.DelClassDisableReqMsg.class, (((long) Message.Tag.DEL_CLASS_DISABLE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DelClassDisableRspMsg.class, (((long) Message.Tag.DEL_CLASS_DISABLE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.ModifyClassDisableReqMsg.class, (((long) Message.Tag.MODIFY_CLASS_DISABLE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ModifyClassDisableRspMsg.class, (((long) Message.Tag.MODIFY_CLASS_DISABLE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetClassDisableReqMsg.class, (((long) Message.Tag.GET_CLASS_DISABLE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetClassDisableRspMsg.class, (((long) Message.Tag.GET_CLASS_DISABLE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyClassDisableChangedReqMsg.class, (((long) Message.Tag.ON_DEV_CLASS_DISABLE_CHANGED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyClassDisableChangedRspMsg.class, (((long) Message.Tag.ON_DEV_CLASS_DISABLE_CHANGED_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyClassDisableSyncedReqMsg.class, (((long) Message.Tag.ON_DEV_CLASS_DISABLE_SYNCED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyClassDisableSyncedRspMsg.class, (((long) Message.Tag.ON_DEV_CLASS_DISABLE_SYNCED_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.GetSchoolGuardReqMsg.class, (((long) Message.Tag.GET_SCHOOL_GUARD_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetSchoolGuardRspMsg.class, (((long) Message.Tag.GET_SCHOOL_GUARD_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.AddSchoolGuardReqMsg.class, (((long) Message.Tag.ADD_SCHOOL_GUARD_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.AddSchoolGuardRspMsg.class, (((long) Message.Tag.ADD_SCHOOL_GUARD_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.DelSchoolGuardReqMsg.class, (((long) Message.Tag.DEL_SCHOOL_GUARD_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DelSchoolGuardRspMsg.class, (((long) Message.Tag.DEL_SCHOOL_GUARD_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.ModifySchoolGuardReqMsg.class, (((long) Message.Tag.MODIFY_SCHOOL_GUARD_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ModifySchoolGuardRspMsg.class, (((long) Message.Tag.MODIFY_SCHOOL_GUARD_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifySchoolGuardChangedReqMsg.class, (((long) Message.Tag.ON_SCHOOL_GUARD_CHANGED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifySchoolGuardChangedRspMsg.class, (((long) Message.Tag.ON_SCHOOL_GUARD_CHANGED_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.AddPraiseReqMsg.class, (((long) Message.Tag.ADD_PRAISE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.AddPraiseRspMsg.class, (((long) Message.Tag.ADD_PRAISE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.DelPraiseReqMsg.class, (((long) Message.Tag.DEL_PRAISE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DelPraiseRspMsg.class, (((long) Message.Tag.DEL_PRAISE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.ModifyPraiseReqMsg.class, (((long) Message.Tag.MODIFY_PRAISE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ModifyPraiseRspMsg.class, (((long) Message.Tag.MODIFY_PRAISE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetPraiseReqMsg.class, (((long) Message.Tag.GET_PRAISE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetPraiseRspMsg.class, (((long) Message.Tag.GET_PRAISE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyPraiseChangedReqMsg.class, (((long) Message.Tag.ON_PRAISE_CHANGED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyPraiseChangedRspMsg.class, (((long) Message.Tag.ON_PRAISE_CHANGED_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.DoPraiseReqMsg.class, (((long) Message.Tag.DO_PRAISE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DoPraiseRspMsg.class, (((long) Message.Tag.DO_PRAISE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.CancelPraiseReqMsg.class, (((long) Message.Tag.CANCEL_PRAISE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.CancelPraiseRspMsg.class, (((long) Message.Tag.CANCEL_PRAISE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.PrizeOfPraiseGotReqMsg.class, (((long) Message.Tag.PRIZE_OF_PRAISE_GOT_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.PrizeOfPraiseGotRspMsg.class, (((long) Message.Tag.PRIZE_OF_PRAISE_GOT_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.LocationReqMsg.class, (((long) Message.Tag.LOCATION_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.LocationRspMsg.class, (((long) Message.Tag.LOCATION_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.LocateS1ReqMsg.class, (((long) Message.Tag.LOCATE_S1_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.LocateS1RspMsg.class, (((long) Message.Tag.LOCATE_S1_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.LocateS2ReqMsg.class, (((long) Message.Tag.LOCATE_S2_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.LocateS2RspMsg.class, (((long) Message.Tag.LOCATE_S2_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.LocateS3ReqMsg.class, (((long) Message.Tag.LOCATE_S3_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.LocateS3RspMsg.class, (((long) Message.Tag.LOCATE_S3_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.QueryDaysLocationReqMsg.class, (((long) Message.Tag.QUERY_DAYS_LOCATION_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.QueryDaysLocationRspMsg.class, (((long) Message.Tag.QUERY_DAYS_LOCATION_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.QueryTimeSegmentLocationReqMsg.class, (((long) Message.Tag.QUERY_LOCATIONS_IN_TIME_SEGMENT_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.QueryTimeSegmentLocationRspMsg.class, (((long) Message.Tag.QUERY_LOCATIONS_IN_TIME_SEGMENT_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.CheckDeviceOnlineReqMsg.class, (((long) Message.Tag.CHECK_DEVICE_ONLINE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.CheckDeviceOnlineRspMsg.class, (((long) Message.Tag.CHECK_DEVICE_ONLINE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyOnlineStatusOfDevReqMsg.class, (((long) Message.Tag.ON_DEVICE_ONLINE_STATUS_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyOnlineStatusOfDevRspMsg.class, (((long) Message.Tag.ON_DEVICE_ONLINE_STATUS_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyDevicePositionReqMsg.class, (((long) Message.Tag.ON_DEVICE_POSITION_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyDevicePositionRspMsg.class, (((long) Message.Tag.ON_DEVICE_POSITION_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetDeviceLastPositionReqMsg.class, (((long) Message.Tag.FETCH_DEVICE_LOCATION_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetDeviceLastPositionRspMsg.class, (((long) Message.Tag.FETCH_DEVICE_LOCATION_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetLocationModeReqMsg.class, (((long) Message.Tag.GET_LOCATION_MODE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetLocationModeRspMsg.class, (((long) Message.Tag.GET_LOCATION_MODE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.ModifyLocationModeReqMsg.class, (((long) Message.Tag.MODIFY_LOCATION_MODE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ModifyLocationModeRspMsg.class, (((long) Message.Tag.MODIFY_LOCATION_MODE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyLocationModeChangedReqMsg.class, (((long) Message.Tag.ON_LOCATION_MODE_CHANGED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyLocationModeChangedRspMsg.class, (((long) Message.Tag.ON_LOCATION_MODE_CHANGED_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.DeviceIncidentReqMsg.class, (((long) Message.Tag.DEVICE_INCIDENT_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DeviceIncidentRspMsg.class, (((long) Message.Tag.DEVICE_INCIDENT_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyIncidentReqMsg.class, (((long) Message.Tag.ON_DEVICE_INCIDENT_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyIncidentRspMsg.class, (((long) Message.Tag.ON_DEVICE_INCIDENT_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.SMSAgentGetReqMsg.class, (((long) Message.Tag.SMS_AGENT_GET_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.SMSAgentGetRspMsg.class, (((long) Message.Tag.SMS_AGENT_GET_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.SMSAgentSwitchReqMsg.class, (((long) Message.Tag.SMS_AGENT_SWITCH_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.SMSAgentSwitchRspMsg.class, (((long) Message.Tag.SMS_AGENT_SWITCH_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifySMSAgentNewSMSReqMsg.class, (((long) Message.Tag.SMS_AGENT_ON_NEW_SMS_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifySMSAgentNewSMSRspMsg.class, (((long) Message.Tag.SMS_AGENT_ON_NEW_SMS_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.SMSAgentGetSwitchStatusReqMsg.class, (((long) Message.Tag.SMS_AGENT_GET_SWITCH_STATUS_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.SMSAgentGetSwitchStatusRspMsg.class, (((long) Message.Tag.SMS_AGENT_GET_SWITCH_STATUS_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.SyncTimeReqMsg.class, (((long) Message.Tag.SYNC_TIME_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.SyncTimeRspMsg.class, (((long) Message.Tag.SYNC_TIME_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetWeatherReqMsg.class, (((long) Message.Tag.GET_WEATHER_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetWeatherRspMsg.class, (((long) Message.Tag.GET_WEATHER_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.MakeFriendsReqMsg.class, (((long) Message.Tag.MAKE_FRIENDS_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.MakeFriendsRspMsg.class, (((long) Message.Tag.MAKE_FRIENDS_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.DelFriendReqMsg.class, (((long) Message.Tag.DEL_FRIEND_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DelFriendRspMsg.class, (((long) Message.Tag.DEL_FRIEND_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetFriendReqMsg.class, (((long) Message.Tag.GET_FRIEND_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetFriendRspMsg.class, (((long) Message.Tag.GET_FRIEND_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyFriendChangedReqMsg.class, (((long) Message.Tag.ON_FRIEND_CHANGED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyFriendChangedRspMsg.class, (((long) Message.Tag.ON_FRIEND_CHANGED_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.ModifyFriendNicknameReqMsg.class, (((long) Message.Tag.MODIFY_FRIEND_NICKNAME_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ModifyFriendNicknameRspMsg.class, (((long) Message.Tag.MODIFY_FRIEND_NICKNAME_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.ModifyFriendFamilyShortNumReqMsg.class, (((long) Message.Tag.MODIFY_FRIEND_FAMILY_SHORT_NUM_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ModifyFriendFamilyShortNumRspMsg.class, (((long) Message.Tag.MODIFY_FRIEND_FAMILY_SHORT_NUM_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.ActivateReqMsg.class, (((long) Message.Tag.ACTIVATE_DEVICE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ActivateRspMsg.class, (((long) Message.Tag.ACTIVATE_DEVICE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetQrcReqMsg.class, (((long) Message.Tag.GET_QRC_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetQrcRspMsg.class, (((long) Message.Tag.GET_QRC_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.DevReportTelNumReqMsg.class, (((long) Message.Tag.DEV_REPORT_TEL_NUM_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DevReportTelNumRspMsg.class, (((long) Message.Tag.DEV_REPORT_TEL_NUM_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.FindDeviceS1ReqMsg.class, (((long) Message.Tag.FIND_DEVICE_S1_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FindDeviceS1RspMsg.class, (((long) Message.Tag.FIND_DEVICE_S1_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.FindDeviceS2ReqMsg.class, (((long) Message.Tag.FIND_DEVICE_S2_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FindDeviceS2RspMsg.class, (((long) Message.Tag.FIND_DEVICE_S2_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.FindDeviceS3ReqMsg.class, (((long) Message.Tag.FIND_DEVICE_S3_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FindDeviceS3RspMsg.class, (((long) Message.Tag.FIND_DEVICE_S3_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.ReportDeviceLossReqMsg.class, (((long) Message.Tag.REPORT_DEVICE_LOSS_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ReportDeviceLossRspMsg.class, (((long) Message.Tag.REPORT_DEVICE_LOSS_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.FetchDeviceSensorDataS1ReqMsg.class, (((long) Message.Tag.FETCH_DEVICE_SENSOR_DATA_S1_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FetchDeviceSensorDataS1RspMsg.class, (((long) Message.Tag.FETCH_DEVICE_SENSOR_DATA_S1_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.FetchDeviceSensorDataS2ReqMsg.class, (((long) Message.Tag.FETCH_DEVICE_SENSOR_DATA_S2_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FetchDeviceSensorDataS2RspMsg.class, (((long) Message.Tag.FETCH_DEVICE_SENSOR_DATA_S2_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.FetchDeviceSensorDataS3ReqMsg.class, (((long) Message.Tag.FETCH_DEVICE_SENSOR_DATA_S3_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FetchDeviceSensorDataS3RspMsg.class, (((long) Message.Tag.FETCH_DEVICE_SENSOR_DATA_S3_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.DeviceReportSensorDataReqMsg.class, (((long) Message.Tag.DEVICE_REPORT_SENSOR_DATA_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.DeviceReportSensorDataRspMsg.class, (((long) Message.Tag.DEVICE_REPORT_SENSOR_DATA_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyDeviceSensorDataReqMsg.class, (((long) Message.Tag.ON_DEVICE_SENSOR_DATA_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyDeviceSensorDataRspMsg.class, (((long) Message.Tag.ON_DEVICE_SENSOR_DATA_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetLastDeviceSensorDataReqMsg.class, (((long) Message.Tag.GET_LAST_DEVICE_SENSOR_DATA_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetLastDeviceSensorDataRspMsg.class, (((long) Message.Tag.GET_LAST_DEVICE_SENSOR_DATA_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.ModifyUsrDevAssocReqMsg.class, (((long) Message.Tag.MODIFY_USER_DEV_ASSOC_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.ModifyUsrDevAssocRspMsg.class, (((long) Message.Tag.MODIFY_USER_DEV_ASSOC_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyUsrDevAssocModifiedReqMsg.class, (((long) Message.Tag.ON_USR_DEV_ASSOC_MODIFIED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyUsrDevAssocModifiedRspMsg.class, (((long) Message.Tag.ON_USR_DEV_ASSOC_MODIFIED_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.TakePhotoS1ReqMsg.class, (((long) Message.Tag.TAKE_PHOTO_S1_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.TakePhotoS1RspMsg.class, (((long) Message.Tag.TAKE_PHOTO_S1_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.TakePhotoS2ReqMsg.class, (((long) Message.Tag.TAKE_PHOTO_S2_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.TakePhotoS2RspMsg.class, (((long) Message.Tag.TAKE_PHOTO_S2_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.TakePhotoS3ReqMsg.class, (((long) Message.Tag.TAKE_PHOTO_S3_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.TakePhotoS3RspMsg.class, (((long) Message.Tag.TAKE_PHOTO_S3_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.SimplexCallS1ReqMsg.class, (((long) Message.Tag.SIMPLEX_CALL_S1_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.SimplexCallS1RspMsg.class, (((long) Message.Tag.SIMPLEX_CALL_S1_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.SimplexCallS2ReqMsg.class, (((long) Message.Tag.SIMPLEX_CALL_S2_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.SimplexCallS2RspMsg.class, (((long) Message.Tag.SIMPLEX_CALL_S2_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.SimplexCallS3ReqMsg.class, (((long) Message.Tag.SIMPLEX_CALL_S3_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.SimplexCallS3RspMsg.class, (((long) Message.Tag.SIMPLEX_CALL_S3_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.NotifyMicroChatEmoticonReqMsg.class, (((long) Message.Tag.ON_NEW_MICRO_CHAT_EMOTICON_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyMicroChatEmoticonRspMsg.class, (((long) Message.Tag.ON_NEW_MICRO_CHAT_EMOTICON_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyMicroChatVoiceReqMsg.class, (((long) Message.Tag.ON_NEW_MICRO_CHAT_VOICE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyMicroChatVoiceRspMsg.class, (((long) Message.Tag.ON_NEW_MICRO_CHAT_VOICE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyMicroChatTextReqMsg.class, (((long) Message.Tag.ON_NEW_MICRO_CHAT_TEXT_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyMicroChatTextRspMsg.class, (((long) Message.Tag.ON_NEW_MICRO_CHAT_TEXT_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.SendChatMessageReqMsg.class, (((long) Message.Tag.SEND_CHAT_MESSAGE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.SendChatMessageRspMsg.class, (((long) Message.Tag.SEND_CHAT_MESSAGE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyChatMessageReqMsg.class, (((long) Message.Tag.ON_NEW_CHAT_MESSAGE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyChatMessageRspMsg.class, (((long) Message.Tag.ON_NEW_CHAT_MESSAGE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.SendGroupChatMessageReqMsg.class, (((long) Message.Tag.SEND_GROUP_CHAT_MESSAGE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.SendGroupChatMessageRspMsg.class, (((long) Message.Tag.SEND_GROUP_CHAT_MESSAGE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyGroupChatMessageReqMsg.class, (((long) Message.Tag.ON_NEW_GROUP_CHAT_MESSAGE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyGroupChatMessageRspMsg.class, (((long) Message.Tag.ON_NEW_GROUP_CHAT_MESSAGE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.GetFamilyGroupOfChatReqMsg.class, (((long) Message.Tag.GET_FAMILY_GROUP_OF_CHAT_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.GetFamilyGroupOfChatRspMsg.class, (((long) Message.Tag.GET_FAMILY_GROUP_OF_CHAT_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifyChatGroupMemberChangedReqMsg.class, (((long) Message.Tag.ON_CHAT_GROUP_MEMBER_CHANGED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifyChatGroupMemberChangedRspMsg.class, (((long) Message.Tag.ON_CHAT_GROUP_MEMBER_CHANGED_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.NotifySosCallOrderChangedReqMsg.class, (((long) Message.Tag.ON_SOS_CALL_ORDER_CHANGED_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.NotifySosCallOrderChangedRspMsg.class, (((long) Message.Tag.ON_SOS_CALL_ORDER_CHANGED_VALUE) << 8) | DIR_RESPONSE);

        protoBufMsgTagMap.put(Message.FileUploadBeforeReqMsg.class, (((long) Message.Tag.FILE_UPLOAD_BEFORE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FileUploadBeforeRspMsg.class, (((long) Message.Tag.FILE_UPLOAD_BEFORE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.FileUploadReqMsg.class, (((long) Message.Tag.FILE_UPLOAD_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FileUploadRspMsg.class, (((long) Message.Tag.FILE_UPLOAD_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.FileDownloadBeforeReqMsg.class, (((long) Message.Tag.FILE_DOWNLOAD_BEFORE_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FileDownloadBeforeRspMsg.class, (((long) Message.Tag.FILE_DOWNLOAD_BEFORE_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.FileDownloadReqMsg.class, (((long) Message.Tag.FILE_DOWNLOAD_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FileDownloadRspMsg.class, (((long) Message.Tag.FILE_DOWNLOAD_VALUE) << 8) | DIR_RESPONSE);
        protoBufMsgTagMap.put(Message.FileFinishedReqMsg.class, (((long) Message.Tag.FILE_TRANS_FINISH_VALUE) << 8) | DIR_REQUEST);
        protoBufMsgTagMap.put(Message.FileFinishedRspMsg.class, (((long) Message.Tag.FILE_TRANS_FINISH_VALUE) << 8) | DIR_RESPONSE);
    }
    private static Long getKeyByProtoBufMessage(GeneratedMessageV3 protoBufMsg) {
        if (protoBufMsg == null)
            return null;
        return protoBufMsgTagMap.get(protoBufMsg.getClass());
    }
    public static Integer calcTagByProtoBufMessage(GeneratedMessageV3 protoBufMsg) {
        Long key = getKeyByProtoBufMessage(protoBufMsg);
        if (key == null)
            return null;
        return (int) ((key >> 8) & 0xFFFF);
    }
    public static Integer calcDirByProtoBufMessage(GeneratedMessageV3 protoBufMsg) {
        Long key = getKeyByProtoBufMessage(protoBufMsg);
        if (key == null)
            return null;
        return (int) (key & 0xFF);
    }

    private static final LongSparseArray<Class<? extends GeneratedMessageV3>> TagProtoBufMsgMap = new LongSparseArray<>();
    static {
        for (Map.Entry<Class<? extends GeneratedMessageV3>, Long> entry : protoBufMsgTagMap.entrySet()) {
            TagProtoBufMsgMap.put(entry.getValue(), entry.getKey());
        }
    }
    public static Class<? extends GeneratedMessageV3> getProtoBufMessageClassByTag(int tag, int dir) {
        long key = tag;
        key <<= 8;
        key |= dir;
        return TagProtoBufMsgMap.get(key);
    }

    public static class InvalidLenghtException extends RuntimeException {
        public InvalidLenghtException() {
        }

        public InvalidLenghtException(String msg) {
            super(msg);
        }
    }

    public static class InvalidStartOfPktException extends RuntimeException {
        public InvalidStartOfPktException() {
        }

        public InvalidStartOfPktException(String msg) {
            super(msg);
        }
    }

    public static class InvalidEndOfPktException extends RuntimeException {
        public InvalidEndOfPktException() {
        }

        public InvalidEndOfPktException(String msg) {
            super(msg);
        }
    }

    public static class ChecksumException extends RuntimeException {
        public ChecksumException() {
        }

        public ChecksumException(String msg) {
            super(msg);
        }
    }

    public static class AddressPairException extends RuntimeException {
        public AddressPairException() {
        }

        public AddressPairException(String msg) {
            super(msg);
        }
    }

    public static class UnsupportedMessageException extends RuntimeException {
        public UnsupportedMessageException() {
        }

        public UnsupportedMessageException(String msg) {
            super(msg);
        }
    }
}
