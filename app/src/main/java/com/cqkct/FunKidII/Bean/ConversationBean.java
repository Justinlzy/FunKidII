package com.cqkct.FunKidII.Bean;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cqkct.FunKidII.db.Entity.ChatEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import protocol.Message;

public class ConversationBean {
    public enum Type { DEVICE, FAMILY_GROUP }

//    /** TODO:
//     * 这个会话所属的用户
//     */
//    @NonNull
//    public final String mUserId;

    /**
     * 会话所对应的设备
     */
    public final String mDeviceId;
    public String mBabyName;
    public String mBabyAvatar;

    public Type mType;
    public String mGroupId;
    public List<Member> mMembers;

    /**
     * 最新的一条消息
     */
    public ChatEntity lastMessage;

    /**
     * 未读消息条数
     */
    public long unreadCount;

    private Map<String, Member> memberMap;

    public static class Member {
        public final String mUserId;
        public final String mRelation;
        public final Message.UsrDevAssoc.Permission mPermission;
        public String mUserAvatar;

        public Member(@NonNull String userId, @NonNull String relation, @NonNull Message.UsrDevAssoc.Permission permission) {
            mUserId = userId;
            mRelation = relation;
            mPermission = permission;
        }
        public Member(@NonNull String userId, @NonNull String relation, String userAvatar, @NonNull Message.UsrDevAssoc.Permission permission) {
            mUserId = userId;
            mRelation = relation;
            mPermission = permission;
            mUserAvatar = userAvatar;
        }

    }

    public ConversationBean(@NonNull String deviceId, String babyName, String babyAvatar, @NonNull String userId, @NonNull String relation, @NonNull Message.UsrDevAssoc.Permission permission) {
        this(deviceId, babyName, babyAvatar, Type.DEVICE, "", new ArrayList<Member>(1) {
            {
                add(new Member(userId, relation, permission));
            }
        });
    }

    public ConversationBean(@NonNull String deviceId, String babyName, String babyAvatar, String userAvatar, @NonNull String userId, @NonNull String relation, @NonNull Message.UsrDevAssoc.Permission permission) {
        this(deviceId, babyName, babyAvatar, userAvatar, Type.DEVICE, "",  new ArrayList<Member>(1) {
            {
                add(new Member(userId, relation, userAvatar, permission));
            }
        });
    }

    public ConversationBean(String deviceId, String babyName, String babyAvatar, String userAvatar, Type type, String groupId, List<Member> members) {
        mDeviceId = deviceId;
        mBabyName = babyName;
        mBabyAvatar = babyAvatar;
        mType = type;
        mGroupId = groupId;
        mMembers = members;
        memberMap = new HashMap<>();
        for (Member m : members) {
            memberMap.put(m.mUserId, m);
        }
    }

    public ConversationBean(String deviceId, String babyName, String babyAvatar, Type type, String groupId, List<Member> members) {
        mDeviceId = deviceId;
        mBabyName = babyName;
        mBabyAvatar = babyAvatar;
        mType = type;
        mGroupId = groupId;
        mMembers = members;
        memberMap = new HashMap<>();
        for (Member m : members) {
            memberMap.put(m.mUserId, m);
        }
    }

    public void setLastMessage(ChatEntity lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public synchronized @Nullable Member getMember(String userId) {
        return memberMap.get(userId);
    }

    public synchronized void removeMember(String userId) {
        if (TextUtils.isEmpty(userId))
            return;
        Member member = memberMap.remove(userId);
        mMembers.remove(member);
    }

    public synchronized void addMember(@NonNull String userId, @NonNull String relation, @NonNull Message.UsrDevAssoc.Permission permission) {
        Member member = new Member(userId, relation, permission);
        mMembers.add(member);
        memberMap.put(userId, member);
    }

    public synchronized void addMember(@NonNull String userId, @NonNull String relation, String userAvatar, @NonNull Message.UsrDevAssoc.Permission permission) {
        Member member = new Member(userId, relation, userAvatar, permission);
        mMembers.add(member);
        memberMap.put(userId, member);
    }
    public boolean isGroup() {
        return !TextUtils.isEmpty(mGroupId);
    }
}
