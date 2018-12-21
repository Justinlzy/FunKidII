package com.cqkct.FunKidII.db.Entity;

import com.cqkct.FunKidII.Bean.BaseBean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.NotNull;

import java.io.Serializable;

@Entity
public class SmsEntity implements BaseBean, Serializable {

    private static final long serialVersionUID = -3830476323613388148L;

    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private String smsId;
    @NotNull
    private String deviceId;
    @NotNull
    private String userId;

    private long time;
    @NotNull
    private String number;
    @NotNull
    private String text;
    private boolean unreadMark;

    private boolean synced;



    @Generated(hash = 626108261)
    public SmsEntity(Long id, @NotNull String smsId, @NotNull String deviceId,
            @NotNull String userId, long time, @NotNull String number,
            @NotNull String text, boolean unreadMark, boolean synced) {
        this.id = id;
        this.smsId = smsId;
        this.deviceId = deviceId;
        this.userId = userId;
        this.time = time;
        this.number = number;
        this.text = text;
        this.unreadMark = unreadMark;
        this.synced = synced;
    }



    @Generated(hash = 1127714058)
    public SmsEntity() {
    }



    // KEEP
    @Override
    @Keep
    public Long getId() {
        return id;
    }
    // KEEP END



    public void setId(Long id) {
        this.id = id;
    }



    public String getSmsId() {
        return this.smsId;
    }



    public void setSmsId(String smsId) {
        this.smsId = smsId;
    }



    public String getDeviceId() {
        return this.deviceId;
    }



    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }



    public String getUserId() {
        return this.userId;
    }



    public void setUserId(String userId) {
        this.userId = userId;
    }



    public long getTime() {
        return this.time;
    }



    public void setTime(long time) {
        this.time = time;
    }



    public String getNumber() {
        return this.number;
    }



    public void setNumber(String number) {
        this.number = number;
    }



    public String getText() {
        return this.text;
    }



    public void setText(String text) {
        this.text = text;
    }



    public boolean getUnreadMark() {
        return this.unreadMark;
    }



    public void setUnreadMark(boolean unreadMark) {
        this.unreadMark = unreadMark;
    }



    public boolean getSynced() {
        return this.synced;
    }



    public void setSynced(boolean synced) {
        this.synced = synced;
    }

   
}
