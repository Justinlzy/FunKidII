package com.cqkct.FunKidII.Bean;

import com.cqkct.FunKidII.db.Entity.ContactEntity;

import java.util.List;

public class WeChatMemberBean {
    private String  devId;
    private String  babyName;
    private String  lastText;
    private int     messageType;
    private long    lastTextTime;
    private boolean unReads;
    private List<ContactEntity> contactEntityList;

    public String getDevId() {
        return devId;
    }

    public void setDevId(String devId) {
        this.devId = devId;
    }

    public String getBabyName() {
        return babyName;
    }

    public void setBabyName(String babyName) {
        this.babyName = babyName;
    }

    public String getLastText() {
        return lastText;
    }

    public void setLastText(String lastText) {
        this.lastText = lastText;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public long getLastTextTime() {
        return lastTextTime;
    }

    public void setLastTextTime(long lastTextTime) {
        this.lastTextTime = lastTextTime;
    }

    public boolean isUnReads() {
        return unReads;
    }

    public void setUnReads(boolean unReads) {
        this.unReads = unReads;
    }


    public List<ContactEntity> getContactEntityList() {
        return contactEntityList;
    }

    public void setContactEntityList(List<ContactEntity> contactEntityList) {
        this.contactEntityList = contactEntityList;
    }
}
