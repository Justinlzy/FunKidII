package com.cqkct.FunKidII.EventBus;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cqkct.FunKidII.db.Entity.ChatEntity;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.sharesdk.framework.Platform;
import protocol.Message;

/**
 * Created by T on 2018/3/29.
 */

public class Event {

    public static class ExtrudedLoggedOut {
        public ExtrudedLoggedOut() {
        }
    }

    public static class ServerApiNotCompat {
        public ServerApiNotCompat() {
        }
    }

    public static class CalendarOnClickData {

        private String date;

        public CalendarOnClickData(String message) {
            this.date = message;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }

    public static class CalendarOnMonthChanged {
        private Date date;

        public CalendarOnMonthChanged(Date date) {
            this.date = date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public Date getDate() {
            return date;
        }
    }

    public static class DaysLocationOfDevice {
        private Date date;
        private List<String> daysList;
        public DaysLocationOfDevice(@NonNull Date date, List<String> days) {
            this.date = date;
            this.daysList = days;
            if (this.daysList == null) {
                this.daysList = Collections.emptyList();
            }
        }

        public Date getDate() {
            return date;
        }

        public List<String> getDaysList() {
            return daysList;
        }
    }

    public static class DeviceOnline {
        private static final Map<String, Boolean> onlineCache = new ConcurrentHashMap<>();

        @NonNull
        private String deviceId;
        private boolean online;

        public DeviceOnline(@NonNull String deviceId, boolean online) {
            onlineCache.put(deviceId, online);
            this.deviceId = deviceId;
            this.online = online;
        }

        @NonNull
        public String getDeviceId() {
            return deviceId;
        }

        public boolean isOnline() {
            return online;
        }

        public boolean contain(String deviceId) {
            return onlineCache.get(deviceId) != null;
        }

        public Boolean isOnline(String deviceId) {
            if (TextUtils.isEmpty(deviceId))
                return null;
            return onlineCache.get(deviceId);
        }
    }

    public static class ClassDisableUpdated {
        public ClassDisableUpdated() {}
    }

    public static class SchoolGuardUpdated {
        public SchoolGuardUpdated() {}
    }

    public static class ThirdLoginPlatform {
        @NonNull
        private Platform mPlatform;

        public ThirdLoginPlatform(@NonNull Platform platform) {
            mPlatform = platform;
        }

        public @NonNull Platform getmPlatform() {
            return mPlatform;
        }
    }

    public static class HasNewMessageOfMessageCenter {
        private String mDeviceId;
        public HasNewMessageOfMessageCenter(String deviceId) {
            mDeviceId = deviceId;
        }

        public String getDeviceId() {
            return mDeviceId;
        }
    }

    public enum GoogleAccessibility { UNKNOWN, ACCESSIBLE, INACCESSIBLE }

    public static class AppAreaSwitched {
        private int mAppArea;
        public AppAreaSwitched(int appArea) {
            mAppArea = appArea;
        }

        public int getAppArea() {
            return mAppArea;
        }
    }

    public static class FamilyChatGroupMemberUpdated {
        private String mDeviceId;
        private String mgroupId;

        public FamilyChatGroupMemberUpdated(String deviceId, String groupId) {
            mDeviceId = deviceId;
            mgroupId = groupId;
        }

        public String getDeviceId() {
            return mDeviceId;
        }

        public String getGroupId() {
            return mgroupId;
        }
    }

    public static class ResendChatMessage {
        private ChatEntity mChatEntity;

        public ResendChatMessage(ChatEntity chatEntity) {
            mChatEntity = chatEntity;
        }

        public ChatEntity getChatEntity() {
            return mChatEntity;
        }
    }

    public static class VoiceChatMessageClick {
        private ChatEntity mChatEntity;

        public VoiceChatMessageClick(ChatEntity chatEntity) {
            mChatEntity = chatEntity;
        }

        public ChatEntity getChatEntity() {
            return mChatEntity;
        }
    }

    public static class SendTextChatMessage {
        public final long millis;
        public final @NonNull String text;
        public SendTextChatMessage(long millis, @NonNull String text) {
            this.millis = millis;
            this.text = text;
        }
    }

    public static class SendVoiceChatMessage {
        public final long millis;
        public final @NonNull File file;
        /**
         * unit: second
         */
        public final int duration;
        public SendVoiceChatMessage(long millis, @NonNull File file, int duration) {
            this.millis = millis;
            this.file = file;
            this.duration = duration;
        }
    }

    public static class SendEmoticonChatMessage {
        public final long millis;
        public final @NonNull String name;
        public SendEmoticonChatMessage(long millis, @NonNull String name) {
            this.millis = millis;
            this.name = name;
        }
    }

    public static class ShouldUpdateFamilyGroupMember {
        public @Nullable final Message.UsrDevAssoc usrDevAssoc;
        public ShouldUpdateFamilyGroupMember(@Nullable Message.UsrDevAssoc usrDevAssoc) {
            this.usrDevAssoc = usrDevAssoc;
        }
    }
}