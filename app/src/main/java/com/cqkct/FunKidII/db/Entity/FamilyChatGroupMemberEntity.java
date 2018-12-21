package com.cqkct.FunKidII.db.Entity;

import com.cqkct.FunKidII.Bean.BaseBean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.NotNull;

@Entity(nameInDb = "FAMILY_CHAT_GROUP_MEMBER")
public class FamilyChatGroupMemberEntity implements BaseBean, Serializable {
    private static final long serialVersionUID = 1065646190325496053L;
    @Id(autoincrement = true)
    private Long id;

    @NotNull
    private String groupId;

    private String deviceId;
    private String babyName;
    private String babyAvatar;
    private String userAvatar;

    private String userId;
    private int permission;
    private String relation;


    @Generated(hash = 202646964)
    public FamilyChatGroupMemberEntity(Long id, @NotNull String groupId,
            String deviceId, String babyName, String babyAvatar, String userAvatar,
            String userId, int permission, String relation) {
        this.id = id;
        this.groupId = groupId;
        this.deviceId = deviceId;
        this.babyName = babyName;
        this.babyAvatar = babyAvatar;
        this.userAvatar = userAvatar;
        this.userId = userId;
        this.permission = permission;
        this.relation = relation;
    }


    @Generated(hash = 1377885810)
    public FamilyChatGroupMemberEntity() {
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


    public String getGroupId() {
        return this.groupId;
    }


    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }


    public String getDeviceId() {
        return this.deviceId;
    }


    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }


    public String getBabyName() {
        return this.babyName;
    }


    public void setBabyName(String babyName) {
        this.babyName = babyName;
    }


    public String getBabyAvatar() {
        return this.babyAvatar;
    }


    public void setBabyAvatar(String babyAvatar) {
        this.babyAvatar = babyAvatar;
    }


    public String getUserAvatar() {
        return this.userAvatar;
    }


    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }


    public String getUserId() {
        return this.userId;
    }


    public void setUserId(String userId) {
        this.userId = userId;
    }


    public int getPermission() {
        return this.permission;
    }


    public void setPermission(int permission) {
        this.permission = permission;
    }


    public String getRelation() {
        return this.relation;
    }


    public void setRelation(String relation) {
        this.relation = relation;
    }

    
}
