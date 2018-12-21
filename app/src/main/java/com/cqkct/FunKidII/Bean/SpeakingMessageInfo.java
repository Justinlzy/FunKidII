package com.cqkct.FunKidII.Bean;

import java.io.Serializable;

public class SpeakingMessageInfo implements Serializable {

    private static final long serialVersionUID = 5261805398024940239L;

    private String userName;    //用户名称
    private String deviceId;    //设备号
    private long timestamp;     //发送时间
    private String filename;    //语音文件位置
    private String voiceTime;   //时间
    private boolean isSend;     //发送或接受
    private int status;         //消息状态
    private boolean isRead;     //添加未读标识
    private int messageType;    //消息类别 1.语音 2.文字/表情
    private String contentText;

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getVoiceTime() {
        return voiceTime;
    }

    public void setVoiceTime(String voiceTime) {
        this.voiceTime = voiceTime;
    }

    public void setIsSend(boolean isSend) {
        this.isSend = isSend;
    }

    public boolean isSend() {
        return isSend;
    }

    @Override
    public String toString() {
        return "SpeakingMessageInfo{" +
                ", userName='" + userName + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", timestamp=" + timestamp +
                ", filename='" + filename + '\'' +
                ", voiceTime='" + voiceTime + '\'' +
                ", isSend=" + isSend +
                ", status=" + status +
                '}';
    }
}
