package com.cqkct.FunKidII.db.Entity;

import com.cqkct.FunKidII.Bean.BaseBean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.NotNull;

import java.io.Serializable;

import protocol.Message;

/**
 * Created by Administrator on 2018/3/12.
 */

@Entity
public class SosEntity implements BaseBean, Serializable {

    private static final long serialVersionUID = -3830476323613388148L;
    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private String deviceId;
    @NotNull
    private String sosId;
    @NotNull
    private String name;
    @NotNull
    private String number;
    private int callOrder;
    private boolean synced;

    @Generated(hash = 177360531)
    public SosEntity(Long id, @NotNull String deviceId, @NotNull String sosId, @NotNull String name, @NotNull String number, int callOrder,
            boolean synced) {
        this.id = id;
        this.deviceId = deviceId;
        this.sosId = sosId;
        this.name = name;
        this.number = number;
        this.callOrder = callOrder;
        this.synced = synced;
    }

    @Generated(hash = 645745271)
    public SosEntity() {
    }

    // KEEP
    @Override
    @Keep
    public Long getId() {
        return id;
    }

    @Keep
    public Message.SOS toSOS() {
        return Message.SOS.newBuilder().setId(getSosId()).setName(getName()).setPhonenum(getNumber()).setCallOrder(getCallOrder()).build();
    }
    // KEEP END

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSosId() {
        return this.sosId;
    }

    public void setSosId(String sosId) {
        this.sosId = sosId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return this.number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getCallOrder() {
        return this.callOrder;
    }

    public void setCallOrder(int callOrder) {
        this.callOrder = callOrder;
    }

    public boolean getSynced() {
        return this.synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }
}
