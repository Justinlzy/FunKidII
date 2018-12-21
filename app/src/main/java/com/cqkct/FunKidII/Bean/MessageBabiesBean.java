package com.cqkct.FunKidII.Bean;

import com.cqkct.FunKidII.db.Entity.NotifyMessageEntity;

import java.io.Serializable;

public class MessageBabiesBean implements Serializable {

    private String deviceId;
    private String babyName;
    private String babyAvatar;
    private int babySex;
    // 未读消息条数
    private long unreadCount;
    // 最新的一条消息
    private NotifyMessageEntity lastMessage;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getBabyName() {
        return babyName;
    }

    public void setBabyName(String babyName) {
        this.babyName = babyName;
    }

    public String getBabyAvatar() {
        return babyAvatar;
    }

    public void setBabyAvatar(String babyAvatar) {
        this.babyAvatar = babyAvatar;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public NotifyMessageEntity getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(NotifyMessageEntity lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getBabySex() {
        return babySex;
    }

    public void setBabySex(int babySex) {
        this.babySex = babySex;
    }
}
