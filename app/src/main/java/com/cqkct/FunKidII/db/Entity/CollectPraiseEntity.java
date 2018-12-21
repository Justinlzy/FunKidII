package com.cqkct.FunKidII.db.Entity;

import com.cqkct.FunKidII.Bean.BaseBean;
import com.google.protobuf.InvalidProtocolBufferException;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;

import protocol.Message;

/**
 * Created by T on 2018/3/19.
 */

@Entity
public class CollectPraiseEntity implements BaseBean, Serializable {

    // KEEP
    private static final long serialVersionUID = 5211885234221444303L;

    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private String deviceId;
    @NotNull
    private String praiseId;
    private long startTime;
    private byte[] praiseData;
    private long completeTime;
    private long finishTime;
    private String timezone;
    private boolean isCancel;
    @Transient
    private Message.Praise praise;

    @Generated(hash = 1350095681)
    public CollectPraiseEntity(Long id, @NotNull String deviceId,
            @NotNull String praiseId, long startTime, byte[] praiseData,
            long completeTime, long finishTime, String timezone, boolean isCancel) {
        this.id = id;
        this.deviceId = deviceId;
        this.praiseId = praiseId;
        this.startTime = startTime;
        this.praiseData = praiseData;
        this.completeTime = completeTime;
        this.finishTime = finishTime;
        this.timezone = timezone;
        this.isCancel = isCancel;
    }

    @Generated(hash = 304024010)
    public CollectPraiseEntity() {
    }

    // KEEP METHODS - put your custom methods here
    @Override
    @Keep
    public Long getId() {
        return id;
    }

    @Keep
    public Message.Praise getPraise() {
        if (this.praise == null) {
            byte[] data = getPraiseData();
            if (data == null) {
                data = new byte[0];
            }
            try {
                praise =  Message.Praise.parseFrom(data);
            } catch (InvalidProtocolBufferException e) {
                praise = Message.Praise.newBuilder().build();
            }
        }
        return praise;
    }

    @Keep
    public void setPraise(Message.Praise praise) {
        this.praise = praise;
        if (this.praise != null) {
            setPraiseId(praise.getId());
            setStartTime(praise.getStartTime());
            setPraiseData(praise.toByteArray());
            setCompleteTime(praise.getCompleteTime());
            setFinishTime(praise.getFinishTime());
            setTimezone(praise.getTimezone().getZone());
            setIsCancel(praise.getIsCancel());
        } else {
            setPraiseData(new byte[0]);
        }
    }
    // KEEP METHODS END

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPraiseId() {
        return this.praiseId;
    }

    public void setPraiseId(String praiseId) {
        this.praiseId = praiseId;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public byte[] getPraiseData() {
        return this.praiseData;
    }

    public void setPraiseData(byte[] praiseData) {
        this.praiseData = praiseData;
    }

    public long getCompleteTime() {
        return this.completeTime;
    }

    public void setCompleteTime(long completeTime) {
        this.completeTime = completeTime;
    }

    public long getFinishTime() {
        return this.finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public String getTimezone() {
        return this.timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean getIsCancel() {
        return this.isCancel;
    }

    public void setIsCancel(boolean isCancel) {
        this.isCancel = isCancel;
    }
}
