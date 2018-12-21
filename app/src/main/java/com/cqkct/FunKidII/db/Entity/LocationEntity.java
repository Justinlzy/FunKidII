package com.cqkct.FunKidII.db.Entity;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 

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
 * Entity mapped to table LOCATION_BEAN.
 */

@Entity
public class LocationEntity implements BaseBean, Serializable {

    private static final long serialVersionUID = 7148186239155847805L;
    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private String deviceId;
    @NotNull
    private String date;
    @NotNull
    private byte[] locationsData;
    private boolean complete;
    @Transient
    private Message.QueryTimeSegmentLocationRspMsg locationRspMsg;


    @Generated(hash = 39742363)
    public LocationEntity(Long id, @NotNull String deviceId, @NotNull String date,
            @NotNull byte[] locationsData, boolean complete) {
        this.id = id;
        this.deviceId = deviceId;
        this.date = date;
        this.locationsData = locationsData;
        this.complete = complete;
    }
    @Generated(hash = 1723987110)
    public LocationEntity() {
    }


    @Override
    @Keep
    public Long getId() {
        return id;
    }
    @Keep
    public Message.QueryTimeSegmentLocationRspMsg getLocationRspMsg() {
        if (locationRspMsg == null) {
            byte[] data = getLocationsData();
            if (data == null) {
                data = new byte[0];
            }
            try {
                locationRspMsg = Message.QueryTimeSegmentLocationRspMsg.parseFrom(data);
            } catch (InvalidProtocolBufferException e) {
                locationRspMsg = Message.QueryTimeSegmentLocationRspMsg.newBuilder().build();
            }
        }
        return locationRspMsg;
    }
    @Keep
    public void setLocationRspMsg(Message.QueryTimeSegmentLocationRspMsg rspMsg) {
        this.locationRspMsg = rspMsg;
        if (this.locationRspMsg != null) {
            setLocationsData(locationRspMsg.toByteArray());
        } else {
            setLocationsData(new byte[0]);
        }
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getDeviceId() {
        return this.deviceId;
    }
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    public String getDate() {
        return this.date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public byte[] getLocationsData() {
        return this.locationsData;
    }
    public void setLocationsData(byte[] locationsData) {
        this.locationsData = locationsData;
    }
    public boolean getComplete() {
        return this.complete;
    }
    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
