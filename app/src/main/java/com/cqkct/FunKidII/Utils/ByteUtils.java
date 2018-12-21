package com.cqkct.FunKidII.Utils;

import java.util.Arrays;

public class ByteUtils {
    private static final byte[] DIGIT = "0123456789ABCDEF".getBytes();

    /**
     * 将半字节转换为16进制字符
     * @param nibble 半字节
     * @return
     */
    public static byte nibbleToByte(int nibble) {
        return DIGIT[nibble & 0xF];
    }

    /**
     * 将数据转换为16进制表示的可打印ASCII字节数组 (0x5A -> {'5', 'A'})
     *
     * @param raw 原始数据
     * @param offset 偏移
     * @param len 欲转换的长度
     *
     * @return 转换后的结果，16进制表示的可打印字节数组。
     *
     * @see #rawToHex(byte[])
     * @see #rawToHexStr(byte[])
     */
    public static byte[] rawToHex(byte[] raw, int offset, int len) {
        if (raw == null)
            return null;
        byte[] hex = new byte[len * 2];
        for (int n = 0; n < len; ++n) {
            hex[n * 2] = nibbleToByte((raw[offset + n] & 0xF0) >> 4);
            hex[n * 2 + 1] = nibbleToByte(raw[offset + n] & 0x0F);
        }
        return hex;
    }

    /**
     * 将数据转换为16进制表示的可打印ASCII字节数组 (0x5A -> {'5', 'A'})
     * <p>
     * 等同于 rawToHex(raw, 0, raw.length)
     *
     * @param raw 原始数据
     *
     * @return 转换后的结果，16进制表示的可打印字节数组。
     *
     * @see #rawToHex(byte[], int, int)
     * @see #rawToHexStr(byte[])
     */
    public static byte[] rawToHex(byte[] raw) {
        if (raw == null)
            return null;
        return rawToHex(raw, 0, raw.length);
    }

    /**
     * 将数据转换为16进制表示的字符串 (0x5A -> "5A")
     *
     * @param raw 原始数据
     * @param offset 偏移
     * @param len 欲转换的长度
     *
     * @return 转换后的结果，16进制表示的字符串。
     *
     * @see #rawToHexStr(byte[])
     * @see #rawToHex(byte[], int, int)
     */
    public static String rawToHexStr(byte[] raw, int offset, int len) {
        return new String(rawToHex(raw, offset, len));
    }

    /**
     * 将数据转换为16进制表示的字符串 (0x5A -> "5A")
     * <p>
     * 等同于 rawToHexString(raw, 0, raw.length)
     *
     * @param raw 原始数据
     *
     * @return 转换后的结果，16进制表示的字符串。
     *
     * @see #rawToHexStr(byte[], int, int)
     * @see #rawToHex(byte[])
     */
    public static String rawToHexStr(byte[] raw) {
        byte[] hex = rawToHex(raw);
        if (hex == null)
            return null;
        return new String(hex);
    }

    /**
     * 检查String是不是有效的16进制字符串。
     * <p>
     * 偶数个有效的16进制字符。
     * <p>
     * 允许掺杂空格和水平制表符。
     *
     * @param str 字符串。
     *
     * @return 是规则的16进制字符串返回true，否则返回false。
     */
    public static boolean isHexStr(String str) {
        str = str.trim();
        int n = 0;

        for (int i = 0; i < str.length(); ++i) {
            switch (str.charAt(i)) {
                case '0': case '1': case '2': case '3': case '4': case '5':
                case '6': case '7': case '8': case '9':
                case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                    ++n;
                    break;
                case ' ': case '\t':
                    break;
                default:
                    return false;
            }
        }

        return (n & 1) == 0;
    }

    /**
     * 将16进制表示的字符串转换为数据 ("5A" -> 0x5A)
     * <p>
     * 仅转换偶数个有效的16进制字符。
     * <p>
     * 允许掺杂空格和水平制表符。
     *
     * @param str 字符串。
     *
     * @return 转换后的结果
     */
    public static byte[] hexStrToRaw(String str) {
        byte[] raw = new byte[str.length() / 2];
        int dstIdx = 0;

        try {
            boolean nibble = false;
            for (int srcIdx = 0; srcIdx < str.length(); ++srcIdx) {
                char c = str.charAt(srcIdx);
                if (c == ' ') {
                    continue;
                } else {
                    int value = xdigit(c);
                    if (nibble) {
                        nibble = false;
                        raw[dstIdx] |= value;
                        ++dstIdx;
                    } else {
                        raw[dstIdx] = (byte) (value << 4);
                        nibble = true;
                    }
                }
            }
        } catch (Exception e) { }

        return raw.length != dstIdx ? Arrays.copyOf(raw, dstIdx) : raw;
    }

    /**
     * 将数据转换为字符串显示。（utf-8 字符集）
     * <p>
     * '\' 字符被作为转义字符。转义 '\' 和不可打印字符。
     * <p>
     * 不可打印的字符将以16进制显示。例如 0x01 会被转换为 "\x01"。
     *
     * @param raw 原始数据
     * @param offset 偏移
     * @param byteCount 欲转换的长度
     * @return 转换后的结果。
     * @see #rawToStr(byte[])
     * @see #strToRaw(String)
     */
    public static String rawToStr(byte[] raw, int offset, int byteCount) {
        if (raw == null)
            return null;

        StringBuilder sb = new StringBuilder();

        int idx = offset;
        int last = offset + byteCount;
        outer: while (idx < last) {
            byte b0 = raw[idx++];

            if ((b0 & 0x80) == 0) {
                // 这是 ASCII 字符！
                // 0xxxxxxx
                // Range: U-00000000 - U-0000007F
                appendByteToStringBuilder(sb, b0);
            } else if (((b0 & 0xE0) == 0xC0) || ((b0 & 0xF0) == 0xE0)
                    || ((b0 & 0xF8) == 0xF0) || ((b0 & 0xFC) == 0xF8)
                    || ((b0 & 0xFE) == 0xFC)) {
                // 这个字节看起来像是 utf8 的第一个字节，那我们试着解析下。

                // 先看看这个字符额外所需的字节数
                int utfCount = 1;
                if ((b0 & 0xF0) == 0xE0)
                    utfCount = 2;
                else if ((b0 & 0xF8) == 0xF0)
                    utfCount = 3;
                else if ((b0 & 0xFC) == 0xF8)
                    utfCount = 4;
                else if ((b0 & 0xFE) == 0xFC)
                    utfCount = 5;

                // 110xxxxx (10xxxxxx)+
                // Range: U-00000080 - U-000007FF (count == 1)
                // Range: U-00000800 - U-0000FFFF (count == 2)
                // Range: U-00010000 - U-001FFFFF (count == 3)
                // Range: U-00200000 - U-03FFFFFF (count == 4)
                // Range: U-04000000 - U-7FFFFFFF (count == 5)

                if (idx + utfCount > last) {
                    // 剩下的字节数不够，说明这不是有效的utf8字符
                    appendByteToStringBuilder(sb, b0);
                    continue;
                }

                // 获取有效的位，组装成整数
                int val = b0 & (0x1F >> (utfCount - 1)); // 取出 b0 中可用的位。
                for (int i = 0; i < utfCount; ++i) {
                    byte b = raw[idx + i];
                    if ((b & 0xC0) != 0x80) {
                        // 呃~~~，这看起来不对
                        appendByteToStringBuilder(sb, b0);
                        continue outer;
                    }
                    // Push new bits in from the right side
                    val <<= 6;
                    val |= b & 0x3f;
                }

                // Unicode有效性检测
                if ((utfCount != 2) && (val >= 0xD800) && (val <= 0xDFFF)) {
                    appendByteToStringBuilder(sb, b0);
                    continue;
                }
                if (val > 0x10FFFF) {
                    appendByteToStringBuilder(sb, b0);
                    continue;
                }

                // OK，确实是有效的字符
                if (val < 0x10000) {
                    sb.append((char) val);
                } else {
                    int x = val & 0xffff;
                    int u = (val >> 16) & 0x1f;
                    int w = (u - 1) & 0xffff;
                    int hi = 0xd800 | (w << 6) | (x >> 10);
                    int lo = 0xdc00 | (x & 0x3ff);
                    sb.append((char) hi);
                    sb.append((char) lo);
                }

                idx += utfCount;
            } else {
                // 无效的utf8字节: 0x8*, 0x9*, 0xa*, 0xb*, 0xfd-0xff
                appendByteToStringBuilder(sb, b0);
            }
        }

        return sb.toString();
    }

    private static void appendByteToStringBuilder(StringBuilder sb, byte b) {
        if ((b < 0x20 || b > 0x7E) /*&& (b != 0x0D && b != 0x0A)*/) {
            sb.append("\\x");
            sb.append((char) nibbleToByte((b & 0xF0) >> 4));
            sb.append((char) nibbleToByte(b & 0x0F));
        } else {
            if (b == '\\')
                sb.append('\\');
            sb.appendCodePoint(b);
        }
    }

    /**
     * 将数据转换为字符串显示。（ASCII 字符集）
     * <p>
     * 不可打印的字符将以16进制显示。例如 0x01 会被转换为 "\x01"。
     *
     * @param raw 原始数据
     * @return 转换后的结果。
     * @see #rawToStr(byte[], int, int)
     * @see #strToRaw(String)
     */
    public static String rawToStr(byte[] raw) {
        return rawToStr(raw, 0, raw.length);
    }

    /**
     * 将字符串({@link #rawToStr}方法生成的)转换为原始数据
     * @param str 字符串
     * @return 原始数据
     */
    public static byte[] strToRaw(String str) {
        byte[] raw = str.getBytes();
        int srcIdx = 0;
        int dstIdx = 0;
        int last = raw.length;
        while (srcIdx < last) {
            byte b = raw[srcIdx++];
            if (b == '\\') {
                if (srcIdx + 3 <= last && raw[srcIdx] == 'x') {
                    try {
                        raw[dstIdx++] = (byte) ((xdigit(raw[srcIdx + 1]) << 4)
                                | xdigit(raw[srcIdx + 2]));
                        srcIdx += 3;
                    } catch (NumberFormatException e) { }
                }
                continue;
            }
            raw[dstIdx++] = b;
        }

        return Arrays.copyOfRange(raw, 0, dstIdx);
    }

    /**
     * 查看字符是不是有效的16进制字符。
     *
     * @param c 字符
     *
     * @return 结果
     */
    public static boolean isxdigit(int c) {
        switch (c) {
            case '0': case '1': case '2': case '3': case '4': case '5': case '6':
            case '7': case '8': case '9':
            case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
            case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                return true;
            default:
                return false;
        }
    }

    /**
     * 将16进制字符转换为其对应的数值。
     *
     * @param c 16进制字符
     * @return 数值
     * @throws NumberFormatException
     */
    public static int xdigit(int c) throws NumberFormatException {
        switch (c) {
            case '0': case '1': case '2': case '3': case '4': case '5': case '6':
            case '7': case '8': case '9':
                return c - '0';
            case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                return c - 'A' + 10;
            case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                return c - 'a' + 10;
            default:
                throw new NumberFormatException("Invalid hex char: \"" + (char) c + "\"");
        }
    }

    public static int indexOf(byte[] haystack, byte key) {
        return indexOf(haystack, 0, haystack.length, key);
    }

    public static int indexOf(byte[] haystack, int off, byte key) {
        return indexOf(haystack, off, haystack.length, key);
    }

    public static int indexOf(byte[] haystack, int off, int len, byte key) {
        if (haystack == null || off >= haystack.length)
            return -1;
        if (off + len > haystack.length) {
            len = haystack.length;
        } else {
            len += off;
        }
        while (off != len) {
            if (haystack[off] == key)
                return off;
            ++off;
        }
        return -1;
    }

    public static int indexOf(byte[] haystack, byte[] needle) {
        return indexOf(haystack, 0, haystack.length, needle);
    }

    public static int indexOf(byte[] haystack, int off, byte[] needle) {
        return indexOf(haystack, off, haystack.length, needle);
    }

    public static int indexOf(byte[] haystack, int off, int len, byte[] needle) {
        int scan;

        if (haystack == null || needle == null || off >= haystack.length)
            return -1;

        int hlen = off + len > haystack.length ? haystack.length - off : len; // haystack length
        int nlen = needle.length; // needle length

        if (hlen <= 0 || nlen <= 0 || hlen < nlen)
            return -1;

        if (nlen == 1)
            return indexOf(haystack, off, hlen, needle[0]);


        /* Boyer–Moore–Horspool algorithm */

        int[] badByteSkip = new int[256];
        for (scan = 0; scan <= 255; ++scan)
            badByteSkip[scan] = nlen;

        int last = nlen - 1;

        for (scan = 0; scan < last; ++scan)
            badByteSkip[needle[scan] & 0xFF] = last - scan;

        int idx = off;
        while (hlen >= nlen) {
            for (scan = last; haystack[idx + scan] == needle[scan]; --scan)
                if (scan == 0)
                    return idx;

            hlen -= badByteSkip[haystack[idx + last] & 0xFF];
            idx += badByteSkip[haystack[idx + last] & 0xFF];
        }

        return -1;
    }

    public static short getShortBE(byte[] bytes, int offset) {
        return (short) (((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF));
    }

    public static int getUnsignedShortBE(byte[] bytes, int offset) {
        return (((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF)) & 0xFFFF;
    }

    public static int getIntBE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
    }

    public static long getUnsignedIntBE(byte[] bytes, int offset) {
        return getIntBE(bytes, offset) & 0xFFFFFFFFL;
    }

    public static short getShortLE(byte[] bytes, int offset) {
        return (short) ((bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8));
    }

    public static int getUnsignedShortLE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8)) & 0xFFFF;
    }

    public static int getIntLE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16) | ((bytes[offset + 3] & 0xFF) << 24);
    }

    public static long getUnsignedIntLE(byte[] bytes, int offset) {
        return getIntLE(bytes, offset) & 0xFFFFFFFFL;
    }

    public static void toByteArrayBE(short num, byte[] out, int offset) {
        out[offset++] = (byte) ((num >> 8) & 0xFF);
        out[offset] = (byte) (num & 0xFF);
    }

    public static void toByteArrayBE(int num, byte[] out, int offset) {
        out[offset++] = (byte) ((num >> 24) & 0xFF);
        out[offset++] = (byte) ((num >> 16) & 0xFF);
        out[offset++] = (byte) ((num >> 8) & 0xFF);
        out[offset] = (byte) (num & 0xFF);
    }

    public static void toByteArrayLE(short num, byte[] out, int offset) {
        out[offset++] = (byte) (num & 0xFF);
        out[offset] = (byte) ((num >> 8) & 0xFF);
    }

    public static void toByteArrayLE(int num, byte[] out, int offset) {
        out[offset++] = (byte) (num & 0xFF);
        out[offset++] = (byte) ((num >> 8) & 0xFF);
        out[offset++] = (byte) ((num >> 16) & 0xFF);
        out[offset] = (byte) ((num >> 24) & 0xFF);
    }

}