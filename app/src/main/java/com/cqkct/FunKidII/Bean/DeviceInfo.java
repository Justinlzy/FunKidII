package com.cqkct.FunKidII.Bean;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.Utils.FileUtils;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import protocol.Message;

/**
 * Created by T on 2017/12/30.
 */

public class DeviceInfo {
    private static final String TAG = DeviceInfo.class.getSimpleName();

    @NonNull
    private static String mUserId = "";

    @NonNull
    private DeviceEntity deviceEntity;
    private int notificationChannel;

    private Boolean devConfQueried;
    private Boolean lastPositionQueried;
    private Boolean lastSensorDataQueried;
    private boolean online;
    private Boolean classDisableQueried;
    private Boolean SchoolGuardQueried;
    private Boolean FamilyGroupOfChatGroupIdQueried;
    private Boolean NotificationChannelQueried;

    public DeviceInfo(@NonNull DeviceEntity deviceEntity) {
        this.deviceEntity = deviceEntity;
    }

    public void setDeviceEntity(@NonNull DeviceEntity deviceEntity) {
        this.deviceEntity = deviceEntity;
    }

    public @NonNull String getDeviceId() {
        return deviceEntity.getDeviceId();
    }

    public String getBabyName() {
        return deviceEntity.getBaby().getName();
    }

    public String getBabyAvatar() {
        return deviceEntity.getBaby().getAvatar();
    }

    public @Nullable File getBabyAvatarFile() {
        String filename = getBabyAvatar();
        if (TextUtils.isEmpty(filename))
            return null;
        return new File(FileUtils.getExternalStorageImageCacheDirFile(), filename);
    }

    public @NonNull DeviceEntity getDeviceEntity() {
        return deviceEntity;
    }

    public @Nullable Message.Position getLastPosition() {
        Message.Position position = deviceEntity.getLastPosition();
        if (position.getTime() == 0)
            return null;
        return position;
    }

    public long getLastLocateTime() {
        return deviceEntity.getLastPosition().getTime();
    }

    public int getBatteryPercent() {
        return deviceEntity.getBatteryPercent();
    }

    public int getBatteryLevel() {
        return deviceEntity.getBatteryLevel();
    }

    public int getStepCount() {
        return deviceEntity.getStepCount();
    }

    public long getSensorDataTime() {
        return deviceEntity.getSensorDataTime();
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isOnline() {
        return online;
    }

    public @NonNull Message.Baby getBaby() {
        return deviceEntity.getBaby();
    }

    public int getNotificationChannel() {
        return notificationChannel;
    }

    public void setNotificationChannel(int notificationChannel) {
        this.notificationChannel = notificationChannel;
    }

    private void tryQueryFromServer(boolean forceQueryOnlineStatus) {
        if (forceQueryOnlineStatus /*|| getDeviceEntity().getSysInfo().getHwFeature() == 0*/ /* XXX: 重新请求配置信息，以避免出现硬件特新缺失的问题 */
                || (devConfQueried == null && deviceOfUserCachePending.get(getDeviceId()) == null)) {
            queryDevConf(getDeviceId());
        }
        if (lastPositionQueried == null) {
            queryLastPosition(getDeviceId());
        }
        if (lastSensorDataQueried == null) {
            queryLastSensorData(getDeviceId());
        }
        if (classDisableQueried == null) {
            queryClassDisable(getDeviceId());
        }
        if (SchoolGuardQueried == null) {
            querySchoolGuard(getDeviceId());
        }
        if (FamilyGroupOfChatGroupIdQueried == null) {
            queryFamilyGroupOfChatGroupId(getDeviceId());
        }
        if (NotificationChannelQueried == null) {
            queryNotificationChannel(getDeviceId());
        }
        if (forceQueryOnlineStatus) {
            queryOnlineStatus(getDeviceId());
        }
    }

    /** 用户的设备缓存 */
    private static final Map<String, DeviceInfo> deviceOfUserCache = new ConcurrentHashMap<>();
    private static Boolean devicesOfUserQueried = true;
    private static final Map<String, String> deviceOfUserCachePending = new ConcurrentHashMap<>();

    public static Map<String, DeviceInfo> getDeviceOfUserCache() {
        return deviceOfUserCache;
    }

    public static @Nullable DeviceInfo getDeviceInfo(String deviceId, boolean cacheIfNotExists) {
        if (TextUtils.isEmpty(deviceId))
            return null;
        DeviceInfo deviceInfo = deviceOfUserCache.get(deviceId);
        if (deviceInfo == null && cacheIfNotExists) {
            cache(deviceId);
        }
        return deviceInfo;
    }

    public static @Nullable DeviceInfo getDeviceInfo(String deviceId) {
        return getDeviceInfo(deviceId, true);
    }

    public static void fillDeviceOfUserCache(String userId, boolean clearly) {
        if (!mUserId.equals(userId)) {
            clearCache();
        }
        if (TextUtils.isEmpty(userId)) {
            mUserId = "";
            return;
        }

        mUserId = userId;

        if (devicesOfUserQueried == null || clearly) {
            clearCache();
            queryDevicesOfUser(userId);
            return;
        }

        List<BabyEntity> babies = GreenUtils.getBabyEntityDao().queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(userId))
                .list();
        List<String> deviceIds = new ArrayList<>(deviceOfUserCache.keySet());
        for (BabyEntity b : babies) {
            deviceIds.remove(b.getDeviceId());
            cache(b, true);
        }
        for (String deviceId : deviceIds) {
            deviceOfUserCache.remove(deviceId);
        }
    }

    public static void invalidate() {
        devicesOfUserQueried = null;
        for (Map.Entry<String, DeviceInfo> set : deviceOfUserCache.entrySet()) {
            DeviceInfo di = set.getValue();
            if (di != null) {
                di.devConfQueried = null;
                di.lastPositionQueried = null;
                di.lastSensorDataQueried = null;
                di.classDisableQueried = null;
                di.SchoolGuardQueried = null;
                di.FamilyGroupOfChatGroupIdQueried = null;
                di.NotificationChannelQueried = null;
            }
        }
    }

    public static void remove(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "remove: deviceId is empty");
            return;
        }

        DeviceInfo deviceInfo = deviceOfUserCache.get(deviceId);
        if (deviceInfo != null) {
            if (!TextUtils.isEmpty(mUserId)) {
                List<BabyEntity> babies = GreenUtils.getBabyEntityDao().queryBuilder()
                        .where(BabyEntityDao.Properties.UserId.eq(mUserId), BabyEntityDao.Properties.DeviceId.eq(deviceId))
                        .list();
                if (babies.isEmpty()) {
                    deviceOfUserCache.remove(deviceId);
                }
            } else {
                deviceOfUserCache.remove(deviceId);
            }
        }
    }

    public static void clearCache() {
        deviceOfUserCache.clear();
        devicesOfUserQueried = null;
    }

    public static void cache(String deviceId) {
        if (TextUtils.isEmpty(mUserId) || TextUtils.isEmpty(deviceId)) {
            return;
        }
        List<BabyEntity> babies = GreenUtils.getBabyEntityDao().queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(mUserId), BabyEntityDao.Properties.DeviceId.eq(deviceId))
                .list();
        if (babies.isEmpty()) {
            return;
        }
        cache(babies.get(0), false);
    }

    private static synchronized void cache(BabyEntity babyEntity, boolean forceQueryOnlineStatus) {
        String deviceId = babyEntity.getDeviceId();
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "cache: deviceId is empty");
            return;
        }
        DeviceInfo old = deviceOfUserCache.get(deviceId);
        if (old == null) {
            String pending = deviceOfUserCachePending.get(deviceId);
            if (pending != null) {
                return;
            }
            List<DeviceEntity> devices = GreenUtils.getDeviceEntityDao().queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).list();
            if (devices.isEmpty()) {
                deviceOfUserCachePending.put(deviceId, deviceId);
                queryDevConf(deviceId);
            } else {
                cache(babyEntity, devices.get(0), forceQueryOnlineStatus);
            }
        } else {
            old.tryQueryFromServer(forceQueryOnlineStatus);
        }
    }

    public static void cache(DeviceEntity deviceEntity) {
        if (TextUtils.isEmpty(mUserId) || deviceEntity == null || TextUtils.isEmpty(deviceEntity.getDeviceId())) {
            return;
        }
        List<BabyEntity> babies = GreenUtils.getBabyEntityDao().queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(mUserId), BabyEntityDao.Properties.DeviceId.eq(deviceEntity.getDeviceId()))
                .list();
        if (babies.isEmpty()) {
            return;
        }
        cache(babies.get(0), deviceEntity, false);
    }

    private static synchronized void cache(BabyEntity babyEntity, DeviceEntity deviceEntity, boolean forceQueryOnlineStatus) {
        if (babyEntity == null || deviceEntity == null || TextUtils.isEmpty(deviceEntity.getDeviceId())) {
            L.w(TAG, "cache: invalid deviceEntity: " + deviceEntity);
            return;
        }
        DeviceInfo di = deviceOfUserCache.get(deviceEntity.getDeviceId());
        if (di == null) {
            di = new DeviceInfo(deviceEntity);
            deviceOfUserCache.put(deviceEntity.getDeviceId(), di);
            queryOnlineStatus(deviceEntity.getDeviceId());
        } else {
            di.deviceEntity = deviceEntity;
        }
        di.notificationChannel = babyEntity.getNotificationChannel();
        di.tryQueryFromServer(forceQueryOnlineStatus);
        deviceOfUserCachePending.remove(deviceEntity.getDeviceId());
    }

    private static void queryDevConf(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "queryDevConf: deviceId is empty");
            return;
        }

        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId, false);
        if (deviceInfo != null) {
            deviceInfo.devConfQueried = false;
        }

        TlcService service = App.getInstance().getTlcService();
        if (service != null) {
            final Message.FetchDevConfReqMsg reqMsg = Message.FetchDevConfReqMsg.newBuilder().setDeviceId(deviceId)
                    .setFlag(Message.DevConfFlag.DCF_ALL_MASK_VALUE)
                    .build();

            service.exec(
                    reqMsg,

                    new TlcService.OnExecListener() {
                        @Override
                        public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            try {
                                Message.FetchDevConfRspMsg rspMsg = response.getProtoBufMsg();
                                L.v(TAG, "queryDevConf() -> exec() -> onResponse(): " + rspMsg);
                                if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                    if (deviceInfo != null) {
                                        deviceInfo.devConfQueried = true;
                                    }
                                    long flag = rspMsg.getFlag();
                                    GreenUtils.saveConfigsAsync(rspMsg.getConf(), flag, reqMsg.getDeviceId(), true);
                                    return false;
                                }
                                L.e(TAG, "queryDevConf() -> exec() -> onResponse() failure: " + rspMsg.getErrCode());
                            } catch (Exception e) {
                                L.e(TAG, "queryDevConf() -> exec() -> onResponse() process failure", e);
                            }

                            deviceOfUserCachePending.remove(reqMsg.getDeviceId());
                            if (deviceInfo != null) {
                                deviceInfo.devConfQueried = null;
                            }

                            return false;
                        }

                        @Override
                        public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                            deviceOfUserCachePending.remove(reqMsg.getDeviceId());
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            if (deviceInfo != null) {
                                deviceInfo.devConfQueried = null;
                            }
                            L.e(TAG, "queryDevConf() -> exec() -> onException()", cause);
                        }

                        @Override
                        public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                            // never to here for this exec
                        }
                    }
            );
        }
    }

    private static void queryDevicesOfUser(final String userId) {
        if (TextUtils.isEmpty(userId)) {
            L.w(TAG, "queryDevicesOfUser: userId is empty");
            return;
        }

        devicesOfUserQueried = false;

        TlcService service = App.getInstance().getTlcService();
        if (service != null) {
            Message.FetchDeviceListReqMsg reqMsg = Message.FetchDeviceListReqMsg.newBuilder().setUserId(userId).build();

            service.exec(
                    reqMsg,

                    new TlcService.OnExecListener() {
                        @Override
                        public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                            devicesOfUserQueried = true;
                            try {
                                Message.FetchDeviceListRspMsg rspMsg = response.getProtoBufMsg();
                                L.v(TAG, "queryDevicesOfUser() -> exec() -> onResponse(): " + rspMsg);
                                if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                    GreenUtils.saveDevicesOfUserFromFetch(rspMsg.getUsrDevAssocList());
                                    DeviceInfo.fillDeviceOfUserCache(userId, false);
                                    return false;
                                }
                                L.e(TAG, "queryDevicesOfUser() -> exec() -> onResponse() failure: " + rspMsg.getErrCode());
                            } catch (Exception e) {
                                L.e(TAG, "queryDevicesOfUser() -> exec() -> onResponse() process failure", e);
                            }

                            return false;
                        }

                        @Override
                        public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                            devicesOfUserQueried = null;
                            L.e(TAG, "queryDevicesOfUser() -> exec() -> onException()", cause);
                        }

                        @Override
                        public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                            // never to here for this exec
                        }
                    }
            );
        }
    }

    private static void queryLastPosition(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "queryLastPosition: deviceId is empty");
            return;
        }

        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId, false);
        if (deviceInfo != null) {
            deviceInfo.lastPositionQueried = false;
        }

        TlcService service = App.getInstance().getTlcService();
        if (service != null) {
            final Message.GetDeviceLastPositionReqMsg reqMsg = Message.GetDeviceLastPositionReqMsg.newBuilder().setDeviceId(deviceId).build();

            service.exec(
                    reqMsg,

                    new TlcService.OnExecListener() {
                        @Override
                        public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            if (deviceInfo != null) {
                                deviceInfo.lastPositionQueried = true;
                            }
                            deviceOfUserCachePending.remove(reqMsg.getDeviceId());
                            try {
                                Message.GetDeviceLastPositionRspMsg rspMsg = response.getProtoBufMsg();
                                L.v(TAG, "queryLastPosition() -> exec() -> onResponse(): " + rspMsg);
                                if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                    GreenUtils.saveDeviceLastPositionAsync(reqMsg.getDeviceId(), rspMsg.getPosition());
                                    return false;
                                }
                                L.e(TAG, "queryLastPosition() -> exec() -> onResponse() failure: " + rspMsg.getErrCode());
                            } catch (Exception e) {
                                L.e(TAG, "queryLastPosition() -> exec() -> onResponse() process failure", e);
                            }

                            return false;
                        }

                        @Override
                        public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            if (deviceInfo != null) {
                                deviceInfo.lastPositionQueried = null;
                            }
                            deviceOfUserCachePending.remove(reqMsg.getDeviceId());
                            L.e(TAG, "queryLastPosition() -> exec() -> onException()", cause);
                        }

                        @Override
                        public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                            // never to here for this exec
                        }
                    }
            );
        }
    }

    private static void queryLastSensorData(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "queryLastSensorData: deviceId is empty");
            return;
        }

        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId, false);
        if (deviceInfo != null) {
            deviceInfo.lastSensorDataQueried = false;
        }

        TlcService service = App.getInstance().getTlcService();
        if (service != null) {
            final Message.GetLastDeviceSensorDataReqMsg reqMsg = Message.GetLastDeviceSensorDataReqMsg.newBuilder().setDeviceId(deviceId).build();

            service.exec(
                    reqMsg,

                    new TlcService.OnExecListener() {
                        @Override
                        public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            if (deviceInfo != null) {
                                deviceInfo.lastSensorDataQueried = true;
                            }
                            deviceOfUserCachePending.remove(reqMsg.getDeviceId());
                            try {
                                Message.GetLastDeviceSensorDataRspMsg rspMsg = response.getProtoBufMsg();
                                L.v(TAG, "queryLastSensorData() -> exec() -> onResponse(): " + rspMsg);
                                if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                    GreenUtils.saveDeviceLastSensorDataAsync(reqMsg.getDeviceId(), rspMsg.getData(), rspMsg.getReportTime());
                                    return false;
                                }
                                L.e(TAG, "queryLastSensorData() -> exec() -> onResponse() failure: " + rspMsg.getErrCode());
                            } catch (Exception e) {
                                L.e(TAG, "queryLastSensorData() -> exec() -> onResponse() process failure", e);
                            }

                            return false;
                        }

                        @Override
                        public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            if (deviceInfo != null) {
                                deviceInfo.lastSensorDataQueried = null;
                            }
                            deviceOfUserCachePending.remove(reqMsg.getDeviceId());
                            L.e(TAG, "queryLastSensorData() -> exec() -> onException()", cause);
                        }

                        @Override
                        public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                            // never to here for this exec
                        }
                    }
            );
        }
    }

    private static void queryClassDisable(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "queryClassDisable: deviceId is empty");
            return;
        }

        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId, false);
        if (deviceInfo != null) {
            deviceInfo.classDisableQueried = false;
        }

        TlcService service = App.getInstance().getTlcService();
        if (service != null) {
            final Message.GetClassDisableReqMsg reqMsg = Message.GetClassDisableReqMsg.newBuilder().setDeviceId(deviceId).build();

            service.exec(
                    reqMsg,

                    new TlcService.OnExecListener() {
                        @Override
                        public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            if (deviceInfo != null) {
                                deviceInfo.classDisableQueried = true;
                            }
                            try {
                                Message.GetClassDisableRspMsg rspMsg = response.getProtoBufMsg();
                                L.v(TAG, "queryClassDisable() -> exec() -> onResponse(): " + rspMsg);
                                if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                    GreenUtils.saveClassDisableAsync(reqMsg.getDeviceId(), rspMsg.getClassDisableList());
                                    return false;
                                }
                                L.e(TAG, "queryClassDisable() -> exec() -> onResponse() failure: " + rspMsg.getErrCode());
                            } catch (Exception e) {
                                L.e(TAG, "queryClassDisable() -> exec() -> onResponse() process failure", e);
                            }

                            return false;
                        }

                        @Override
                        public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            if (deviceInfo != null) {
                                deviceInfo.classDisableQueried = null;
                            }
                            L.e(TAG, "queryClassDisable() -> exec() -> onException()", cause);
                        }

                        @Override
                        public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                            // never to here for this exec
                        }
                    }
            );
        }
    }

    private static void querySchoolGuard(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "querySchoolGuard: deviceId is empty");
            return;
        }

        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId, false);
        if (deviceInfo != null) {
            deviceInfo.SchoolGuardQueried = false;
        }

        TlcService service = App.getInstance().getTlcService();
        if (service != null) {
            final Message.GetSchoolGuardReqMsg reqMsg = Message.GetSchoolGuardReqMsg.newBuilder().setDeviceId(deviceId).build();

            service.exec(
                    reqMsg,

                    new TlcService.OnExecListener() {
                        @Override
                        public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            if (deviceInfo != null) {
                                deviceInfo.SchoolGuardQueried = true;
                            }
                            deviceOfUserCachePending.remove(reqMsg.getDeviceId());
                            try {
                                Message.GetSchoolGuardRspMsg rspMsg = response.getProtoBufMsg();
                                L.v(TAG, "querySchoolGuard() -> exec() -> onResponse(): " + rspMsg);
                                if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                    GreenUtils.saveSchoolGuardAsync(reqMsg.getDeviceId(), rspMsg.getGuard());
                                    return false;
                                }
                                L.e(TAG, "querySchoolGuard() -> exec() -> onResponse() failure: " + rspMsg.getErrCode());
                            } catch (Exception e) {
                                L.e(TAG, "querySchoolGuard() -> exec() -> onResponse() process failure", e);
                            }

                            return false;
                        }

                        @Override
                        public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            if (deviceInfo != null) {
                                deviceInfo.SchoolGuardQueried = null;
                            }
                            deviceOfUserCachePending.remove(reqMsg.getDeviceId());
                            L.e(TAG, "querySchoolGuard() -> exec() -> onException()", cause);
                        }

                        @Override
                        public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                            // never to here for this exec
                        }
                    }
            );
        }
    }

    private static void queryFamilyGroupOfChatGroupId(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "queryFamilyGroupOfChatGroupId: deviceId is empty");
            return;
        }

        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId, false);
        if (deviceInfo != null) {
            deviceInfo.FamilyGroupOfChatGroupIdQueried = false;
        }

        TlcService service = App.getInstance().getTlcService();
        if (service != null) {
            final Message.GetFamilyGroupOfChatReqMsg reqMsg = Message.GetFamilyGroupOfChatReqMsg.newBuilder().setDeviceId(deviceId).build();

            service.exec(
                    reqMsg,

                    new TlcService.OnExecListener() {
                        @Override
                        public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            if (deviceInfo != null) {
                                deviceInfo.FamilyGroupOfChatGroupIdQueried = true;
                            }
                            deviceOfUserCachePending.remove(reqMsg.getDeviceId());
                            try {
                                Message.GetFamilyGroupOfChatRspMsg rspMsg = response.getProtoBufMsg();
                                L.v(TAG, "queryFamilyGroupOfChatGroupId() -> exec() -> onResponse(): " + rspMsg);
                                if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS && rspMsg.getGroupCount() > 0) {
                                    GreenUtils.saveFamilyGroupAsync(reqMsg.getDeviceId(), rspMsg.getGroup(0).getGroup());
                                    return false;
                                }
                                L.e(TAG, "queryFamilyGroupOfChatGroupId() -> exec() -> onResponse() failure: " + rspMsg.getErrCode());
                            } catch (Exception e) {
                                L.e(TAG, "queryFamilyGroupOfChatGroupId() -> exec() -> onResponse() process failure", e);
                            }

                            return false;
                        }

                        @Override
                        public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            if (deviceInfo != null) {
                                deviceInfo.FamilyGroupOfChatGroupIdQueried = null;
                            }
                            deviceOfUserCachePending.remove(reqMsg.getDeviceId());
                            L.e(TAG, "queryFamilyGroupOfChatGroupId() -> exec() -> onException()", cause);
                        }

                        @Override
                        public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                            // never to here for this exec
                        }
                    }
            );
        }
    }

    private static void queryNotificationChannel(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "queryNotificationChannel: deviceId is empty");
            return;
        }

        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId, false);
        if (deviceInfo != null) {
            deviceInfo.NotificationChannelQueried = false;
        }

        TlcService service = App.getInstance().getTlcService();
        if (service != null) {
            final Message.GetNotificationChannelReqMsg reqMsg = Message.GetNotificationChannelReqMsg.newBuilder().setUserId(mUserId).setDeviceId(deviceId).build();

            service.exec(
                    reqMsg,

                    new TlcService.OnExecListener() {
                        @Override
                        public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            if (deviceInfo != null) {
                                deviceInfo.NotificationChannelQueried = true;
                            }
                            try {
                                Message.GetNotificationChannelRspMsg rspMsg = response.getProtoBufMsg();
                                L.v(TAG, "queryNotificationChannel() -> exec() -> onResponse(): " + rspMsg);
                                if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                    int ch = rspMsg.getChan();
                                    if (deviceInfo != null) {
                                        deviceInfo.notificationChannel = ch;
                                    }
                                    GreenUtils.saveNotificationChannelAsync(reqMsg.getUserId(), reqMsg.getDeviceId(), ch);
                                    return false;
                                }
                                L.e(TAG, "queryNotificationChannel() -> exec() -> onResponse() failure: " + rspMsg.getErrCode());
                            } catch (Exception e) {
                                L.e(TAG, "queryNotificationChannel() -> exec() -> onResponse() process failure", e);
                            }

                            return false;
                        }

                        @Override
                        public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(reqMsg.getDeviceId(), false);
                            if (deviceInfo != null) {
                                deviceInfo.NotificationChannelQueried = null;
                            }
                            L.e(TAG, "queryNotificationChannel() -> exec() -> onException()", cause);
                        }

                        @Override
                        public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                            // never to here for this exec
                        }
                    }
            );
        }
    }

    private static void queryOnlineStatus(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "queryOnlineStatus: deviceId is empty");
            return;
        }

        TlcService service = App.getInstance().getTlcService();
        if (service != null) {
            Message.CheckDeviceOnlineReqMsg reqMsg = Message.CheckDeviceOnlineReqMsg.newBuilder().addDeviceId(deviceId).build();
            service.exec(
                    reqMsg,
                    new TlcService.OnExecListener() {
                        @Override
                        public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                            try {
                                Message.CheckDeviceOnlineRspMsg rspMsg = response.getProtoBufMsg();
                                L.v(TAG, "queryOnlineStatus() -> exec() -> onResponse(): " + rspMsg);
                                if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                    for (Message.DeviceOnline onlineInfo : rspMsg.getOnlineList()) {
                                        App.getInstance().updateDeviceOnlineStatus(onlineInfo.getDeviceId(), onlineInfo.getOnline());
                                    }
                                    return false;
                                }
                                L.e(TAG, "queryOnlineStatus() -> exec() -> onResponse() failure: " + rspMsg.getErrCode());
                            } catch (Exception e) {
                                L.e(TAG, "queryOnlineStatus() -> exec() -> onResponse() process failure", e);
                            }

                            return false;
                        }

                        @Override
                        public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                            L.e(TAG, "queryOnlineStatus() -> exec() -> onException()", cause);
                        }

                        @Override
                        public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                        }
                    }
            );
        }
    }

    public static @Nullable String getBabyName(String deviceId) {
        if (TextUtils.isEmpty(deviceId))
            return null;
        DeviceInfo info = DeviceInfo.getDeviceInfo(deviceId);
        if (info == null)
            return null;
        return info.getBabyName();
    }

    public static @Nullable String getBabyAvatar(String deviceId) {
        if (TextUtils.isEmpty(deviceId))
            return null;
        DeviceInfo info = DeviceInfo.getDeviceInfo(deviceId);
        if (info == null)
            return null;
        return info.getBabyAvatar();
    }

    public static  @Nullable Message.Position getLastPosition(String deviceId) {
        if (TextUtils.isEmpty(deviceId))
            return null;
        DeviceInfo info = DeviceInfo.getDeviceInfo(deviceId);
        if (info == null)
            return null;
        return info.getLastPosition();
    }

    public static long getLastLocateTime(String deviceId) {
        if (TextUtils.isEmpty(deviceId))
            return 0;
        DeviceInfo info = DeviceInfo.getDeviceInfo(deviceId);
        if (info == null)
            return 0;
        return info.getLastLocateTime();
    }

    public static boolean isOnline(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            return false;
        }
        DeviceInfo info = DeviceInfo.getDeviceInfo(deviceId);
        if (info == null)
            return false;
        return info.isOnline();
    }

    public static int getNotificationChannel(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            return 0;
        }
        DeviceInfo info = DeviceInfo.getDeviceInfo(deviceId);
        if (info == null)
            return 0;
        return info.getNotificationChannel();
    }

    public static int getBabySex(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            return Message.Baby.Sex.UNKNOWN_VALUE;
        }
        DeviceInfo info = DeviceInfo.getDeviceInfo(deviceId);
        if (info == null)
            return Message.Baby.Sex.UNKNOWN_VALUE;
        return info.getBaby().getSex().getNumber();
    }
}
