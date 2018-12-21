package com.cqkct.FunKidII.Utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.db.Dao.AlarmClockEntityDao;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Dao.ChatEntityDao;
import com.cqkct.FunKidII.db.Dao.ClassDisableEntityDao;
import com.cqkct.FunKidII.db.Dao.CollectPraiseEntityDao;
import com.cqkct.FunKidII.db.Dao.ContactEntityDao;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Dao.FamilyChatGroupMemberEntityDao;
import com.cqkct.FunKidII.db.Dao.FenceEntityDao;
import com.cqkct.FunKidII.db.Dao.LocationEntityDao;
import com.cqkct.FunKidII.db.Dao.NotifyMessageEntityDao;
import com.cqkct.FunKidII.db.Dao.SmsEntityDao;
import com.cqkct.FunKidII.db.Dao.SosEntityDao;
import com.cqkct.FunKidII.db.Dao.UserEntityDao;
import com.cqkct.FunKidII.db.Entity.AlarmClockEntity;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.db.Entity.ChatEntity;
import com.cqkct.FunKidII.db.Entity.ClassDisableEntity;
import com.cqkct.FunKidII.db.Entity.CollectPraiseEntity;
import com.cqkct.FunKidII.db.Entity.ContactEntity;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.db.Entity.FamilyChatGroupMemberEntity;
import com.cqkct.FunKidII.db.Entity.FenceEntity;
import com.cqkct.FunKidII.db.Entity.LocationEntity;
import com.cqkct.FunKidII.db.Entity.NotifyMessageEntity;
import com.cqkct.FunKidII.db.Entity.SmsEntity;
import com.cqkct.FunKidII.db.Entity.SosEntity;
import com.cqkct.FunKidII.db.Entity.UserEntity;
import com.google.protobuf.GeneratedMessageV3;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.greendao.query.DeleteQuery;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import protocol.Message;

/**
 * Created by justin on 2017/8/16.
 */

public class GreenUtils {
    public static final String TAG = GreenUtils.class.getSimpleName();


    private static class GreenDaoSaveDataExecutor extends Handler {
        private static final int EXECUTE = 0;

        private static HandlerThread handlerThread;
        private static Looper createLooper() {
            if (handlerThread == null) {
                L.d(TAG, "Creating new handler thread");
                handlerThread = new HandlerThread("GreenDaoSaveDataExecutor");
                handlerThread.start();
            }
            return handlerThread.getLooper();
        }

        WeakReference<App> mA;
        GreenDaoSaveDataExecutor(App a) {
            super(createLooper());
            mA = new WeakReference<>(a);
        }

        public void execute(Runnable task) {
            obtainMessage(EXECUTE, task).sendToTarget();
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            App a = mA.get();
            if (a == null)
                return;

            switch (msg.what) {
                case EXECUTE:
                    executeInternal((Runnable) msg.obj);
                    break;
                default:
                    break;
            }
        }

        private void executeInternal(Runnable task) {
            try {
                task.run();
            } catch (Throwable t) {
                L.e(TAG, "run task: " + task, t);
            }
        }
    }

    private class SameThreadException extends Exception {
        private static final long serialVersionUID = -905639124232613768L;

        public SameThreadException() {
            super("Should be launched from a single worker thread");
        }
    }

    private abstract static class GreenDaoSaveDataRunnable implements Runnable {
        protected abstract void doRun() throws SameThreadException;

        public void run() {
            try {
                doRun();
            } catch (SameThreadException e) {
                L.e(TAG, "Not done from same thread");
            }
        }
    }

    private static volatile GreenDaoSaveDataExecutor mGreenDaoSaveDataExecutor;
    public static void init(App app) {
        mGreenDaoSaveDataExecutor = new GreenDaoSaveDataExecutor(app);
    }

    private static boolean execute(Runnable task) {
        if (mGreenDaoSaveDataExecutor == null) {
            return false;
        }
        mGreenDaoSaveDataExecutor.execute(task);
        return true;
    }

    private static boolean updateChatMessageSendStatus(@NonNull ChatEntity chatEntity, @ChatEntity.SendStatus int status, long serverTimestampMills) {
        if (chatEntity.getId() == null) {
            L.w(TAG, "updateChatMessageSendStatus: chatEntity.getId() == null");
            return false;
        }
        if (chatEntity.getSendStatus() != status) {
            if (serverTimestampMills > 0) {
                chatEntity.setTimestamp(serverTimestampMills);
            }
            chatEntity.setSendStatus(status);
            GreenUtils.getChatEntityDao().update(chatEntity);
            return true;
        }
        return false;
    }

    public static synchronized boolean updateChatMessageSendStatus(@NonNull ChatEntity chatEntity, @ChatEntity.SendStatus int status) {
        return updateChatMessageSendStatus(chatEntity, status, 0);
    }

    public static synchronized boolean updateChatMessageSendSuccess(@NonNull ChatEntity chatEntity, long serverTimestampMills) {
        return updateChatMessageSendStatus(chatEntity, ChatEntity.SEND_STAT_SENT, serverTimestampMills);
    }

    public static synchronized void updateVoiceChatMessageFileUploadStatus(@NonNull ChatEntity chatEntity, @ChatEntity.FileUploadStatus int status) {
        if (chatEntity.getFileUploadStatus() != status) {
            chatEntity.setFileUploadStatus(status);
            GreenUtils.getChatEntityDao().update(chatEntity);
        }
    }

    /**
     * update voice isPlayed
     *
     * @param id 消息ID
     */
    public static void updateVoiceMessagePlayed(long id) {
        ChatEntityDao dao = getChatEntityDao();
        List<ChatEntity> list = dao.queryBuilder().where(ChatEntityDao.Properties.Id.eq(id)).build().list();
        if (!list.isEmpty()) {
            ChatEntity messageInfoBean = list.get(0);
            messageInfoBean.setVoiceIsPlayed(true);
            dao.update(messageInfoBean);
        }
    }

    public static void deleteNotifyMessage(String deviceId, String userId) {
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(userId))
            return;
        NotifyMessageEntityDao dao = getNotifyMessageEntityDao();
        DeleteQuery<NotifyMessageEntity> deleteQuery = dao.queryBuilder()
                .where(NotifyMessageEntityDao.Properties.DeviceId.eq(deviceId))
                .where(NotifyMessageEntityDao.Properties.UserId.eq(userId))
                .buildDelete();
        deleteQuery.executeDeleteWithoutDetachingEntities();
        dao.detachAll();
    }

    /**
     * 更新 绑定申请同意后删除流水号
     * @param seq 流水号
     */
    public static void upDateNotifyMessageEntity(String seq){
        List<NotifyMessageEntity> list = GreenUtils.getNotifyMessageEntityDao().queryBuilder()
                .where(NotifyMessageEntityDao.Properties.Seq.eq(seq))
                .build().list();
        if (!list.isEmpty()){
            NotifyMessageEntity entity = list.get(0);
            entity.setSeq("");
            GreenUtils.getNotifyMessageEntityDao().update(entity);
        }
    }

    public static void markNotifyMessageSeen(String deviceId, String userId) {
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(userId))
            return;
        App.getInstance().getDaoSession().getDatabase().execSQL("UPDATE " + NotifyMessageEntityDao.TABLENAME +
                        " SET " + NotifyMessageEntityDao.Properties.IsRead.columnName + "=?" +
                        " WHERE " +
                        NotifyMessageEntityDao.Properties.DeviceId.columnName + "=?" +
                        " AND " +
                        NotifyMessageEntityDao.Properties.UserId.columnName + "=?" +
                        " AND " +
                        NotifyMessageEntityDao.Properties.IsRead.columnName + "=?"
                ,

                new Object[]{true, deviceId, userId, false}
        );
        getNotifyMessageEntityDao().detachAll();
    }

    /**
     * 切换当前宝贝
     *
     * @param userId   用户id
     * @param deviceId 欲切换到的设备id
     * @return 最终的当前宝贝（该值可能不是 deviceId 参数所对应的宝贝，因为该用户可能没有绑定该设备）
     */
    public static synchronized BabyEntity selectBaby(@NonNull String userId, @NonNull String deviceId) {
        BabyEntityDao babyEntityDao = GreenUtils.getBabyEntityDao();
        List<BabyEntity> list = babyEntityDao.queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(userId))
                .build().list();
        if (list.isEmpty())
            return null;

        BabyEntity from = null;
        BabyEntity to = null;
        List<BabyEntity> forUpdate = new ArrayList<>();

        for (BabyEntity entity : list) {
            if (entity.getDeviceId().equals(deviceId)) {
                to = entity;
                if (!to.getIs_select()) {
                    to.setIs_select(true);
                    forUpdate.add(to);
                }
            } else {
                if (entity.getIs_select()) {
                    if (from == null) {
                        from = entity;
                    } else {
                        entity.setIs_select(false);
                        forUpdate.add(entity);
                    }
                }
            }
        }

        if (to == null) {
            if (from != null) {
                to = from;
            } else if (!list.isEmpty()) { // 没有当前宝贝，吧第一个作为当前宝贝
                to = list.get(0);
                to.setIs_select(true);
                forUpdate.add(to);
            }
        } else if (from != null) {
            from.setIs_select(false);
            forUpdate.add(from);
        }

        if (forUpdate.isEmpty()) {
            // 当前宝贝没变
            return to;
        }

        babyEntityDao.updateInTx(forUpdate);

        App.getInstance().setCurrentBaby(to);

        return to;
    }

    public static void saveDevicesOfUserFromFetchAsync(final List<Message.UsrDevAssoc> usrDevAssocs) {
        execute(new GreenDaoSaveDataRunnable() {
            @Override
            protected void doRun() throws SameThreadException {
                saveDevicesOfUserFromFetch(usrDevAssocs);
            }
        });
    }

    /**
     * 更新用户设备列表
     *
     * @param usrDevAssocs
     * @return 当前宝贝
     */
    public static synchronized BabyEntity saveDevicesOfUserFromFetch(List<Message.UsrDevAssoc> usrDevAssocs) {
        boolean hasSelected = false;
        BabyEntity firstNewOne = null;
        BabyEntity seletedOne = null;

        if (usrDevAssocs == null) {
            L.w(TAG, "saveDevicesOfUserFromFetch usrDevAssocs == null");
            return null;
        }

        BabyEntityDao BabyEntityDao = getBabyEntityDao();
        List<BabyEntity> oldData = BabyEntityDao.queryBuilder()
                .build().list();

        List<BabyEntity> updateList = new ArrayList<>();
        List<BabyEntity> insertList = new ArrayList<>();

        for (Message.UsrDevAssoc newOne : usrDevAssocs) {
            BabyEntity baby = null;
            for (Iterator<BabyEntity> it = oldData.iterator(); it.hasNext(); ) {
                BabyEntity oldBaby = it.next();
                if (newOne.getUserId().equals(oldBaby.getUserId()) && newOne.getDeviceId().equals(oldBaby.getDeviceId())) {
                    baby = oldBaby;
                    if (baby.getIs_select()) {
                        seletedOne = baby;
                        hasSelected = true;
                    }
                    updateList.add(baby);
                    it.remove();
                    if (firstNewOne == null)
                        firstNewOne = baby;
                    break;
                }
            }

            if (baby == null) {
                baby = new BabyEntity();
                baby.setUserId(newOne.getUserId());
                baby.setDeviceId(newOne.getDeviceId());
                insertList.add(baby);
                if (firstNewOne == null)
                    firstNewOne = baby;
            }

            baby.setPermission(newOne.getPermissionValue());
            baby.setRelation(newOne.getRelation());
            if (!TextUtils.isEmpty(newOne.getAvatar())) {
                baby.setUserAvatar(newOne.getAvatar());
            }
        }

        if (!hasSelected) {
            if (firstNewOne != null) {
                firstNewOne.setIs_select(true);
                seletedOne = firstNewOne;
            }
        }

        if (!updateList.isEmpty())
            BabyEntityDao.updateInTx(updateList);

        if (!insertList.isEmpty())
            BabyEntityDao.insertInTx(insertList);

        if (!oldData.isEmpty()) {
            DeviceEntityDao dao = getDeviceEntityDao();
            for (BabyEntity removed : oldData) {
                if (TextUtils.isEmpty(removed.getDeviceId())) {
                    BabyEntityDao.delete(removed);
                    continue;
                }
                List<DeviceEntity> dl = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(removed.getDeviceId())).list();
                if (dl.isEmpty()) {
                    BabyEntityDao.delete(removed);
                    continue;
                }
                clearDeviceWhenUnbind(removed.getDeviceId(), removed.getUserId(), dl.get(0).getUnbindClearLevel());
            }
        }

        App.getInstance().setCurrentBaby(seletedOne);

        return seletedOne;
    }

    public static synchronized void saveUsrDevAssoc(protocol.Message.UsrDevAssoc usrDevAssoc) {
        if (usrDevAssoc == null || TextUtils.isEmpty(usrDevAssoc.getUserId()) || TextUtils.isEmpty(usrDevAssoc.getDeviceId()))
            return;

        BabyEntityDao dao = getBabyEntityDao();
        List<BabyEntity> list = dao.queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(usrDevAssoc.getUserId()))
                .build().list();

        BabyEntity currSelected = null;
        BabyEntity entity = null;

        for (BabyEntity one : list) {
            if (one.getIs_select()) {
                currSelected = one;
                if (entity != null) {
                    break;
                }
            }
            if (one.getDeviceId().equals(usrDevAssoc.getDeviceId())) {
                entity = one;
                if (currSelected != null) {
                    break;
                }
            }
        }

        boolean isUpdate = false;
        if (entity == null) {
            entity = new BabyEntity();
            entity.setUserId(usrDevAssoc.getUserId());
            entity.setDeviceId(usrDevAssoc.getDeviceId());
            entity.setPermission(usrDevAssoc.getPermissionValue());
            entity.setRelation(usrDevAssoc.getRelation());
            if (currSelected == null) {
                entity.setIs_select(true);
            }
            if (!TextUtils.isEmpty(usrDevAssoc.getAvatar())) {
                entity.setUserAvatar(usrDevAssoc.getAvatar());
            }
            entity.setId(dao.insert(entity));
            if (currSelected == null) {
                currSelected = entity;
            }
        } else {
            isUpdate = true;
            entity.setPermission(usrDevAssoc.getPermissionValue());
            entity.setRelation(usrDevAssoc.getRelation());

            if (!TextUtils.isEmpty(usrDevAssoc.getAvatar())) {
                entity.setUserAvatar(usrDevAssoc.getAvatar());
            }
            if (currSelected == null) {
                entity.setIs_select(true);
                currSelected = entity;
            }
            dao.update(entity);
        }

        if (currSelected.getDeviceId().equals(entity.getDeviceId())) {
            App.getInstance().setCurrentBaby(currSelected);
        } else if (isUpdate) {
            List<FamilyChatGroupMemberEntity> memberEntities = getFamilyChatGroupMemberEntityDao().queryBuilder().where(
                    FamilyChatGroupMemberEntityDao.Properties.DeviceId.eq(usrDevAssoc.getDeviceId()),
                    FamilyChatGroupMemberEntityDao.Properties.UserId.eq(usrDevAssoc.getUserId())
            ).list();
            if (!memberEntities.isEmpty()) {
                EventBus.getDefault().postSticky(new Event.FamilyChatGroupMemberUpdated(usrDevAssoc.getDeviceId(), memberEntities.get(0).getGroupId()));
            }
        }
    }

    /**
     * 删除宝贝
     */
    public static synchronized void deleteBaby(String deviceId, String userId) {
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(userId)) {
            return;
        }

        BabyEntityDao babyEntityDao = getBabyEntityDao();
        List<BabyEntity> list = babyEntityDao.queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(userId))
                .build().list();

        BabyEntity selectOne = null;

        List<BabyEntity> delList = new ArrayList<>();

        for (Iterator<BabyEntity> it = list.iterator(); it.hasNext(); ) {
            BabyEntity one = it.next();
            if (one.getIs_select())
                selectOne = one;
            if (one.getDeviceId().equals(deviceId)) {
                delList.add(one);
                if (one.getIs_select())
                    selectOne = null;

                it.remove();
            }
        }

        babyEntityDao.deleteInTx(delList);

        // 选择新的宝贝作为当前宝贝
        if (selectOne == null && !list.isEmpty()) {
            selectOne = list.get(0);
            selectOne.setIs_select(true);
            babyEntityDao.update(selectOne);
        }

        // 通知宝贝变动
        App.getInstance().setCurrentBaby(selectOne);
    }

    public static void saveConfigsAsync(final Message.DevConf configs, final long flag, final String deviceId, final boolean fullReplace) {
        execute(new GreenDaoSaveDataRunnable() {
            @Override
            protected void doRun() throws SameThreadException {
                saveConfigs(configs, flag, deviceId, fullReplace);
            }
        });
    }

    public static void saveConfigsAsync(final Message.DevConf configs, final long flag, final String deviceId) {
        execute(new GreenDaoSaveDataRunnable() {
            @Override
            protected void doRun() throws SameThreadException {
                saveConfigs(configs, flag, deviceId);
            }
        });
    }

    public static void saveConfigs(Message.DevConf configs, long flag, String deviceId) {
        saveConfigs(configs, flag, deviceId, false);
    }

    /**
     * 保存服务器取下来的用户数据
     *
     * @param configs     配置信息
     * @param flag        有效的信息字段
     * @param deviceId    设备号
     * @param fullReplace 是否完整替换
     */
    public static synchronized void saveConfigs(Message.DevConf configs, long flag, String deviceId, boolean fullReplace) {
        if ((flag & Message.DevConfFlag.DCF_BABY_VALUE) != 0) {
            saveBaby(deviceId, configs.getBaby(), fullReplace);
        }

        if ((flag & Message.DevConfFlag.DCF_FUNCTIONS_VALUE) != 0) {
            saveFunctions(deviceId, configs.getFuncs());
        }

        if ((flag & Message.DevConfFlag.DCF_DEV_SYS_INFO_VALUE) != 0) {
            saveDevSysInfo(deviceId, configs.getDevSysInfo(), fullReplace);
        }

        if ((flag & Message.DevConfFlag.DCF_COMPANY_INFO_VALUE) != 0) {
            saveCompanyInfo(deviceId, configs.getCompInfo());
        }

        if ((flag & Message.DevConfFlag.DCF_FUNC_MODULE_INFO_VALUE) != 0) {
            saveFuncModuleInfo(deviceId, configs.getFuncModuleInfo());
        }

        if ((flag & Message.DevConfFlag.DCF_UNBIND_CLEAR_LEVEL_VALUE) != 0) {
            saveUnbindClearLevel(deviceId, configs.getUnbindClearLevel());
        }

        if (flag != 0) {
            List<DeviceEntity> list = getDeviceEntityDao().queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
            if (!list.isEmpty())
                App.getInstance().notifyDeviceEntity(list.get(0));
        }
    }


    /**
     * 保存宝贝信息到本地数据库
     *
     * @param baby        宝贝信息
     * @param deviceId    设备ID
     * @param fullReplace 是否完整替换
     */
    private static void saveBaby(String deviceId, Message.Baby baby, boolean fullReplace) {
        if (TextUtils.isEmpty(deviceId) || baby == null) {
            return;
        }

        // 先更新宝贝信息
        BabyEntityDao babyEntityDao = getBabyEntityDao();
        List<BabyEntity> babyList = babyEntityDao.queryBuilder()
                .where(BabyEntityDao.Properties.DeviceId.eq(deviceId))
                .build().list();
        if (!babyList.isEmpty()) {
            for (BabyEntity babyBean : babyList) {
                babyBean.setDeviceId(deviceId);
                if (fullReplace || !TextUtils.isEmpty(baby.getAvatar())) {
                    babyBean.setBabyAvatar(baby.getAvatar());
                }
                if (fullReplace || !TextUtils.isEmpty(baby.getName())) {
                    babyBean.setName(baby.getName());
                }
                if (fullReplace || !TextUtils.isEmpty(baby.getPhone())) {
                    babyBean.setPhone(baby.getPhone());
                }
                if (fullReplace || baby.getSex() != Message.Baby.Sex.UNKNOWN) {
                    babyBean.setSex(baby.getSexValue());
                }
                if (fullReplace || baby.getBirthday() != 0) {
                    babyBean.setBirthday(baby.getBirthday());
                }
                if (fullReplace || baby.getGrade() != 0) {
                    babyBean.setGrade(baby.getGrade());
                }
                if (fullReplace || baby.getHeight() != 0) {
                    babyBean.setHeight(baby.getHeight());
                }
                if (fullReplace || baby.getWeight() != 0) {
                    babyBean.setWeight(baby.getWeight());
                }
            }
            babyEntityDao.updateInTx(babyList);
        }

        // 再更新设备信息
        DeviceEntityDao dao = getDeviceEntityDao();
        List<DeviceEntity> list = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
        if (list.isEmpty()) {
            DeviceEntity bean = new DeviceEntity();
            bean.setDeviceId(deviceId);
            bean.setBaby(baby, fullReplace);
            dao.insert(bean);
        } else {
            DeviceEntity bean = list.get(0);
            bean.setBaby(baby, fullReplace);
            dao.update(bean);
        }

        // 通知
        if (!babyList.isEmpty()) {
            try {
                BabyEntity current = App.getInstance().getCurrentBabyBean();
                if (current != null && current.getDeviceId().equals(deviceId)) {
                    for (BabyEntity babyBean : babyList) {
                        if (babyBean.getUserId().equals(current.getUserId())) {
                            App.getInstance().setCurrentBaby(babyBean);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                L.w(TAG, "saveBaby()", e);
            }
        } else {
            list = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
            if (!list.isEmpty())
                App.getInstance().notifyDeviceEntity(list.get(0));
        }
    }

    private static void saveFunctions(String deviceId, Message.Functions functions) {
        if (TextUtils.isEmpty(deviceId) || functions == null)
            return;

        DeviceEntityDao dao = getDeviceEntityDao();
        List<DeviceEntity> list = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
        if (list.isEmpty()) {
            DeviceEntity bean = new DeviceEntity();
            bean.setDeviceId(deviceId);
            bean.setFunctions(functions);
            dao.insert(bean);
        } else {
            DeviceEntity bean = list.get(0);
            bean.setFunctions(functions);
            dao.update(bean);
        }
    }

    private static void saveDevSysInfo(String deviceId, Message.DeviceSysInfo devSysInfo, boolean fullReplace) {
        if (TextUtils.isEmpty(deviceId) || devSysInfo == null)
            return;

        DeviceEntity bean;

        DeviceEntityDao dao = getDeviceEntityDao();
        List<DeviceEntity> list = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
        if (list.isEmpty()) {
            bean = new DeviceEntity();
            bean.setDeviceId(deviceId);
        } else {
            bean = list.get(0);
        }

        if (fullReplace || !TextUtils.isEmpty(devSysInfo.getModel())) {
            bean.setModel(devSysInfo.getModel());
        }
        if (fullReplace || devSysInfo.getHwFeature() != 0) {
            bean.setHw_feature(devSysInfo.getHwFeature());
        }
        if (fullReplace || TextUtils.isEmpty(devSysInfo.getHwVer())) {
            bean.setHw_ver(devSysInfo.getHwVer());
        }
        if (fullReplace || TextUtils.isEmpty(devSysInfo.getFwVer())) {
            bean.setFw_ver(devSysInfo.getFwVer());
        }
        if (fullReplace || TextUtils.isEmpty(devSysInfo.getSwVer())) {
            bean.setSw_ver(devSysInfo.getSwVer());
        }
        if (fullReplace || devSysInfo.getSwFeature() != 0) {
            bean.setSw_feature(devSysInfo.getSwFeature());
        }
        if (fullReplace || !TextUtils.isEmpty(devSysInfo.getCustomModel())) {
            bean.setCustom_model(devSysInfo.getCustomModel());
        }
        bean.setSysInfo(devSysInfo, fullReplace);

        if (list.isEmpty()) {
            dao.insert(bean);
        } else {
            dao.update(bean);
        }
    }

    private static void saveCompanyInfo(String deviceId, Message.CompanyInfo companyInfo) {
        if (TextUtils.isEmpty(deviceId) || companyInfo == null)
            return;

        DeviceEntityDao dao = getDeviceEntityDao();
        List<DeviceEntity> list = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
        if (list.isEmpty()) {
            DeviceEntity bean = new DeviceEntity();
            bean.setDeviceId(deviceId);
            bean.setCompanyInfo(companyInfo);
            dao.insert(bean);
        } else {
            DeviceEntity bean = list.get(0);
            bean.setCompanyInfo(companyInfo);
            dao.update(bean);
        }
    }

    private static void saveFuncModuleInfo(String deviceId, Message.FuncModuleInfo funcModuleInfo) {
        if (TextUtils.isEmpty(deviceId) || funcModuleInfo == null)
            return;

        DeviceEntityDao dao = getDeviceEntityDao();
        List<DeviceEntity> list = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
        if (list.isEmpty()) {
            DeviceEntity bean = new DeviceEntity();
            bean.setDeviceId(deviceId);
            bean.setFuncModuleInfo(funcModuleInfo);
            dao.insert(bean);
        } else {
            DeviceEntity bean = list.get(0);
            bean.setFuncModuleInfo(funcModuleInfo);
            dao.update(bean);
        }
    }

    private static void saveUnbindClearLevel(String deviceId, long unbindClearLevel) {
        if (TextUtils.isEmpty(deviceId))
            return;

        DeviceEntityDao dao = getDeviceEntityDao();
        List<DeviceEntity> list = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
        if (list.isEmpty()) {
            DeviceEntity bean = new DeviceEntity();
            bean.setDeviceId(deviceId);
            bean.setUnbindClearLevel(unbindClearLevel);
            dao.insert(bean);
        } else {
            DeviceEntity bean = list.get(0);
            bean.setUnbindClearLevel(unbindClearLevel);
            dao.update(bean);
        }
    }

    public static SosEntity addOrModifySos(String deviceId, Message.SOS sos) {
        if (TextUtils.isEmpty(deviceId) || sos == null)
            return null;
        SosEntityDao sosEntityDao = getSosEntityDao();
        List<SosEntity> list = sosEntityDao.queryBuilder().where(SosEntityDao.Properties.DeviceId.eq(deviceId))
                .where(SosEntityDao.Properties.SosId.eq(sos.getId())).build().list();
        SosEntity sosEntity;
        if (list.isEmpty()) {
            sosEntity = new SosEntity();
            sosEntity.setDeviceId(deviceId);
            sosEntity.setSosId(sos.getId());
            sosEntity.setName(sos.getName());
            sosEntity.setNumber(sos.getPhonenum());
            sosEntity.setCallOrder(sos.getCallOrder());
            sosEntity.setSynced(false);
            sosEntity.setId(sosEntityDao.insert(sosEntity));
        } else {
            sosEntity = list.get(0);
            sosEntity.setName(sos.getName());
            sosEntity.setNumber(sos.getPhonenum());
            sosEntity.setCallOrder(sos.getCallOrder());
            sosEntity.setSynced(false);
            sosEntityDao.update(sosEntity);
        }
        return sosEntity;
    }

    public static void updateSosSyncState(String devId, List<String> sosIds) {
        if (TextUtils.isEmpty(devId))
            return;
        SosEntityDao sosEntityDao = getSosEntityDao();
        List<SosEntity> list = sosEntityDao.queryBuilder()
                .where(SosEntityDao.Properties.DeviceId.eq(devId), SosEntityDao.Properties.SosId.in(sosIds)).list();
        if (list.isEmpty())
            return;
        for (SosEntity entity : list) {
            entity.setSynced(true);
        }
        sosEntityDao.updateInTx(list);
    }

    public static void saveLocationAsync(@NonNull final String deviceId, @NonNull final Date date, @NonNull final Message.QueryTimeSegmentLocationRspMsg rspMsg) {
        execute(new GreenDaoSaveDataRunnable() {
            @Override
            protected void doRun() throws SameThreadException {
                saveLocation(deviceId, date, rspMsg);
            }
        });
    }

    public static final SimpleDateFormat LOCATION_RECORD_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static void saveLocation(@NonNull String deviceId, @NonNull Date date, @NonNull Message.QueryTimeSegmentLocationRspMsg rspMsg) {
        String dateStr = LOCATION_RECORD_DATE_FORMAT.format(date);

        LocationEntityDao dao = getLocationEntityDao();
        List<LocationEntity> list = dao.queryBuilder()
                .where(LocationEntityDao.Properties.DeviceId.eq(deviceId))
                .where(LocationEntityDao.Properties.Date.eq(dateStr))
                .build().list();
        LocationEntity entity = null;
        if (!list.isEmpty()) {
            entity = list.get(0);
        } else {
            entity = new LocationEntity();
        }
        entity.setLocationRspMsg(rspMsg);

        Calendar now = Calendar.getInstance();
        Calendar dataDate = Calendar.getInstance();
        dataDate.setTime(date);
        if (now.get(Calendar.YEAR) > dataDate.get(Calendar.YEAR) ||
                (now.get(Calendar.YEAR) == dataDate.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) > dataDate.get(Calendar.DAY_OF_YEAR))) {
            // 以往的数据
            entity.setComplete(true);
        }

        if (entity.getId() == null) {
            entity.setDeviceId(deviceId);
            entity.setDate(dateStr);
            dao.insert(entity);
        } else {
            dao.update(entity);
        }

        long n = dao.queryBuilder().where(LocationEntityDao.Properties.DeviceId.eq(deviceId)).count() - 7;
        if (n > 1) {
            list = dao.queryBuilder().where(LocationEntityDao.Properties.DeviceId.eq(deviceId)).orderAsc(LocationEntityDao.Properties.Date).list();
            List<LocationEntity> deletes = new ArrayList<>();
            for (int i = 0; i < n; ++i) {
                deletes.add(list.get(i));
            }
            dao.deleteInTx(deletes);
        }
    }

    public static void deleteLocations(@NonNull String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            return;
        }
        LocationEntityDao dao = getLocationEntityDao();
        List<LocationEntity> list = dao.queryBuilder()
                .where(LocationEntityDao.Properties.DeviceId.eq(deviceId))
                .build().list();
        if (!list.isEmpty()) {
            dao.deleteInTx(list);
        }
    }

    public static void deleteSos(String deviceId, String sosId) {
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(sosId))
            return;
        SosEntityDao sosEntityDao = getSosEntityDao();
        List<SosEntity> list = sosEntityDao.queryBuilder().where(SosEntityDao.Properties.DeviceId.eq(deviceId))
                .where(SosEntityDao.Properties.SosId.eq(sosId)).build().list();
        if (!list.isEmpty()) {
            sosEntityDao.deleteInTx(list);
        }
    }

    private static void deleteSos(String deviceId) {
        if (TextUtils.isEmpty(deviceId))
            return;
        SosEntityDao sosEntityDao = getSosEntityDao();
        List<SosEntity> list = sosEntityDao.queryBuilder().where(SosEntityDao.Properties.DeviceId.eq(deviceId)).list();
        if (!list.isEmpty()) {
            sosEntityDao.deleteInTx(list);
        }
    }

    public static void saveSos(String deviceId, Message.NotifySosChangedReqMsg.Detail detail) {
        SosEntityDao sosEntityDao = getSosEntityDao();
        List<SosEntity> list = sosEntityDao.queryBuilder().where(SosEntityDao.Properties.DeviceId.eq(deviceId)).build().list();

        List<SosEntity> update = new ArrayList<>();
        List<SosEntity> insert = new ArrayList<>();
        List<SosEntity> delete = new ArrayList<>();

        for (Message.SOS sos : detail.getSosList()) {
            switch (detail.getAction()) {
                case ADD:
                case MODIFY: {
                    SosEntity entity = null;
                    for (SosEntity old : list) {
                        if (sos.getId().equals(old.getSosId())) {
                            entity = old;
                            update.add(entity);
                            break;
                        }
                    }
                    if (entity == null) {
                        entity = new SosEntity();
                        entity.setDeviceId(deviceId);
                        entity.setSosId(sos.getId());
                        insert.add(entity);
                    }
                    entity.setName(sos.getName());
                    entity.setNumber(sos.getPhonenum());
                    entity.setSynced(false);
                    break;
                }
                case DEL: {
                    for (SosEntity old : list) {
                        if (sos.getId().equals(old.getSosId())) {
                            delete.add(old);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }

        if (!delete.isEmpty()) {
            sosEntityDao.deleteInTx(delete);
        }
        if (!update.isEmpty()) {
            sosEntityDao.updateInTx(update);
        }
        if (!insert.isEmpty()) {
            sosEntityDao.insertInTx(insert);
        }
    }

    public static void saveSos(String deviceId, List<Message.SOS> sosList) {
        if (TextUtils.isEmpty(deviceId))
            return;
        if (sosList == null)
            return;

        SosEntityDao dao = getSosEntityDao();

        List<SosEntity> old = dao.queryBuilder()
                .where(SosEntityDao.Properties.DeviceId.eq(deviceId))
                .build().list();
        List<SosEntity> update = new ArrayList<>();
        List<SosEntity> insert = new ArrayList<>();
        for (protocol.Message.SOS sos : sosList) {
            SosEntity entity = null;
            for (Iterator<SosEntity> it = old.iterator(); it.hasNext(); ) {
                SosEntity oldEntity = it.next();
                if (sos.getId().equals(oldEntity.getSosId())) {
                    entity = oldEntity;
                    update.add(entity);

                    it.remove();
                    break;
                }
            }
            if (entity == null) {
                entity = new SosEntity();
                entity.setDeviceId(deviceId);
                entity.setSosId(sos.getId());
                insert.add(entity);
            }

            entity.setName(sos.getName());
            entity.setNumber(sos.getPhonenum());
            entity.setCallOrder(sos.getCallOrder());
            entity.setSynced(sos.getDevSynced());
        }
        if (!old.isEmpty()) {
            dao.deleteInTx(old);
        }
        if (!update.isEmpty()) {
            dao.updateInTx(update);
        }
        if (!insert.isEmpty()) {
            dao.insertInTx(insert);
        }
    }

    public static ContactEntity addOrModifyContact(String deviceId, Message.Contact contact) {
        if (TextUtils.isEmpty(deviceId) || contact == null)
            return null;
        ContactEntityDao dao = getContactEntityDao();
        List<ContactEntity> list = dao.queryBuilder().where(ContactEntityDao.Properties.DeviceId.eq(deviceId)).list();

        ContactEntity entity = null;
        for (ContactEntity one : list) {
            if (!TextUtils.isEmpty(contact.getId())) {
                if (contact.getId().equals(one.getContactId())) { // 普通联系人
                    entity = one;
                    break;
                }
            } else if (!TextUtils.isEmpty(contact.getUsrDevAssoc().getUserId())) { // 绑定用户联系人
                if (contact.getUsrDevAssoc().getUserId().equals(one.getUserId())) {
                    entity = one;
                    break;
                }
            } else if (!TextUtils.isEmpty(contact.getFriendId())) { // 设备好友
                if (contact.getFriendId().equals(one.getFriendId())) {
                    entity = one;
                    break;
                }
            }
        }

        if (entity == null) {
            entity = new ContactEntity();

            entity.setDeviceId(deviceId);
            entity.setContactId(contact.getId());

            entity.setName(contact.getName());
            entity.setNumber(contact.getNumber());

            entity.setUserId(contact.getUsrDevAssoc().getUserId());
            entity.setPermission(contact.getUsrDevAssoc().getPermissionValue());
            entity.setRelation(contact.getUsrDevAssoc().getRelation());
            entity.setUserAvatar(contact.getUsrDevAssoc().getAvatar());

            entity.setFriendDeviceId(contact.getFriendDeviceId());
            entity.setFriendId(contact.getFriendId());
            entity.setFriendNickname(contact.getFriendNickname());
            entity.setFriendBabyAvatar(contact.getFriendBabyAvatar());

            entity.setFamilyShortNum(contact.getFamilyShortNum());

            entity.setSynced(contact.getDevSynced());

            entity.setId(dao.insert(entity));
        } else {
            entity.setName(contact.getName());
            entity.setNumber(contact.getNumber());

            entity.setUserId(contact.getUsrDevAssoc().getUserId());
            entity.setPermission(contact.getUsrDevAssoc().getPermissionValue());
            entity.setRelation(contact.getUsrDevAssoc().getRelation());
            entity.setUserAvatar(contact.getUsrDevAssoc().getAvatar());

            entity.setFriendDeviceId(contact.getFriendDeviceId());
            entity.setFriendId(contact.getFriendId());
            entity.setFriendNickname(contact.getFriendNickname());
            entity.setFriendBabyAvatar(contact.getFriendBabyAvatar());

            entity.setFamilyShortNum(contact.getFamilyShortNum());

            entity.setSynced(contact.getDevSynced());

            dao.update(entity);
        }

        return entity;
    }

    public static void deleteContact(ContactEntity contactEntity) {
        if (contactEntity == null || contactEntity.getId() == null || TextUtils.isEmpty(contactEntity.getDeviceId()))
            return;
        getContactEntityDao().delete(contactEntity);
    }

    public static void deleteContact(String deviceId, String userId) {
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(userId))
            return;
        ContactEntityDao dao = getContactEntityDao();
        List<ContactEntity> list = dao.queryBuilder()
                .where(ContactEntityDao.Properties.DeviceId.eq(deviceId))
                .where(ContactEntityDao.Properties.UserId.eq(userId))
                .list();
        if (!list.isEmpty()) {
            dao.deleteInTx(list);
        }
    }

    public static void saveContact(String deviceId, List<Message.Contact> contacts) {
        if (TextUtils.isEmpty(deviceId))
            return;
        if (contacts == null)
            return;

        ContactEntityDao dao = getContactEntityDao();

        List<ContactEntity> old = dao.queryBuilder()
                .where(ContactEntityDao.Properties.DeviceId.eq(deviceId))
                .list();
        List<ContactEntity> update = new ArrayList<>();
        List<ContactEntity> insert = new ArrayList<>();
        for (protocol.Message.Contact contact : contacts) {
            ContactEntity entity = null;
            for (Iterator<ContactEntity> it = old.iterator(); it.hasNext(); ) {
                ContactEntity oldEntity = it.next();
                if (!TextUtils.isEmpty(contact.getId())) { // 普通联系人
                    if (contact.getId().equals(oldEntity.getContactId())) {
                        entity = oldEntity;
                        update.add(entity);
                        it.remove();
                        break;
                    }
                } else if (!TextUtils.isEmpty(contact.getUsrDevAssoc().getUserId())) { // 绑定用户联系人
                    if (contact.getUsrDevAssoc().getUserId().equals(oldEntity.getUserId())) {
                        entity = oldEntity;
                        update.add(entity);
                        it.remove();
                        break;
                    }
                } else if (!TextUtils.isEmpty(contact.getFriendId())) { // 设备好友
                    if (contact.getFriendId().equals(oldEntity.getFriendId())) {
                        entity = oldEntity;
                        update.add(entity);
                        it.remove();
                        break;
                    }
                }
            }

            if (entity == null) {
                entity = new ContactEntity();
                entity.setDeviceId(deviceId);
                entity.setContactId(contact.getId());
                insert.add(entity);
            }

            entity.setName(contact.getName());
            entity.setNumber(contact.getNumber());

            entity.setUserId(contact.getUsrDevAssoc().getUserId());
            entity.setPermission(contact.getUsrDevAssoc().getPermissionValue());
            entity.setRelation(contact.getUsrDevAssoc().getRelation());
            entity.setUserAvatar(contact.getUsrDevAssoc().getAvatar());

            entity.setFriendDeviceId(contact.getFriendDeviceId());
            entity.setFriendId(contact.getFriendId());
            entity.setFriendNickname(contact.getFriendNickname());
            entity.setFriendBabyAvatar(contact.getFriendBabyAvatar());

            entity.setFamilyShortNum(contact.getFamilyShortNum());

            entity.setSynced(contact.getDevSynced());
        }

        if (!old.isEmpty()) {
            dao.deleteInTx(old);
        }
        if (!update.isEmpty()) {
            dao.updateInTx(update);
        }
        if (!insert.isEmpty()) {
            dao.insertInTx(insert);
        }
    }

    public static void saveContact(String deviceId, Message.NotifyContactChangedReqMsg.Detail detail) {
        ContactEntityDao dao = getContactEntityDao();
        List<ContactEntity> list = dao.queryBuilder().where(ContactEntityDao.Properties.DeviceId.eq(deviceId)).list();

        List<ContactEntity> update = new ArrayList<>();
        List<ContactEntity> insert = new ArrayList<>();
        List<ContactEntity> delete = new ArrayList<>();

        for (Message.Contact contact : detail.getContactList()) {
            switch (detail.getAction()) {
                case ADD:
                case MODIFY: {
                    ContactEntity entity = null;
                    for (ContactEntity old : list) {
                        if (!TextUtils.isEmpty(contact.getId())) { // 普通联系人
                            if (contact.getId().equals(old.getContactId())) {
                                entity = old;
                                update.add(entity);
                                break;
                            }
                        } else if (!TextUtils.isEmpty(contact.getUsrDevAssoc().getUserId())) { // 绑定用户联系人
                            if (contact.getUsrDevAssoc().getUserId().equals(old.getUserId())) {
                                entity = old;
                                update.add(entity);
                                break;
                            }
                        } else if (!TextUtils.isEmpty(contact.getFriendId())) { // 设备好友
                            if (contact.getFriendId().equals(old.getFriendId())) {
                                entity = old;
                                update.add(entity);
                                break;
                            }
                        }
                    }
                    if (entity == null) {
                        entity = new ContactEntity();
                        entity.setDeviceId(deviceId);
                        entity.setContactId(contact.getId());
                        entity.setSynced(contact.getDevSynced());
                        insert.add(entity);
                    }
                    entity.setName(contact.getName());
                    entity.setNumber(contact.getNumber());
                    entity.setUserId(contact.getUsrDevAssoc().getUserId());
                    entity.setPermission(contact.getUsrDevAssoc().getPermissionValue());
                    entity.setRelation(contact.getUsrDevAssoc().getRelation());
                    entity.setUserAvatar(contact.getUsrDevAssoc().getAvatar());
                    entity.setFriendDeviceId(contact.getFriendDeviceId());
                    entity.setFriendId(contact.getFriendId());
                    entity.setFriendNickname(contact.getFriendNickname());
                    entity.setFriendBabyAvatar(contact.getFriendBabyAvatar());
                    entity.setFamilyShortNum(contact.getFamilyShortNum());
                    break;
                }
                case DEL: {
                    for (ContactEntity old : list) {
                        if (contact.getId().equals(old.getContactId())) {
                            delete.add(old);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }

        if (!delete.isEmpty()) {
            dao.deleteInTx(delete);
        }
        if (!update.isEmpty()) {
            dao.updateInTx(update);
        }
        if (!insert.isEmpty()) {
            dao.insertInTx(insert);
        }
    }

    public static void saveFriendContact(String deviceId, Message.NotifyFriendChangedReqMsg.Detail detail) {
        Message.Friend friend = detail.getFriend();
        ContactEntityDao dao = getContactEntityDao();
        List<ContactEntity> list = dao.queryBuilder().where(ContactEntityDao.Properties.DeviceId.eq(deviceId),
                ContactEntityDao.Properties.FriendId.eq(friend.getId())).list();

        switch (detail.getAction()) {
            case ADD:
            case MODIFY: {
                for (ContactEntity entity : list) {
                    if (friend.getId().equals(entity.getFriendId())) {
                        entity.setFriendDeviceId(friend.getPeerDeviceId());
                        entity.setFriendNickname(friend.getPeerNickname());
                        entity.setFriendBabyAvatar(friend.getPeerBabyAvatar());
                        entity.setFamilyShortNum(friend.getPeerFamilyShortNum());
                        dao.update(entity);
                        break;
                    }
                }
                break;
            }
            case DEL:
                if (!list.isEmpty()) {
                    dao.deleteInTx(list);
                }
                break;
            default:
                break;
        }
    }


    public static AlarmClockEntity saveAlarmClock(String deviceId, Message.AlarmClock alarmClock) {
        if (TextUtils.isEmpty(deviceId) || alarmClock == null)
            return null;
        AlarmClockEntityDao dao = getAlarmClockEntityDao();
        List<AlarmClockEntity> list = dao.queryBuilder().where(AlarmClockEntityDao.Properties.DeviceId.eq(deviceId))
                .where(AlarmClockEntityDao.Properties.AlarmClockId.eq(alarmClock.getId())).build().list();
        AlarmClockEntity entity;
        if (list.isEmpty()) {
            entity = new AlarmClockEntity();
            entity.setDeviceId(deviceId);
            entity.setAlarmClockId(alarmClock.getId());
            entity.setName(alarmClock.getName());
            entity.setTimePoint(alarmClock.getTime().getTime());
            entity.setRepeat(alarmClock.getRepeat());
            entity.setNoticeFlag(alarmClock.getNoticeFlag());
            entity.setTimezone(alarmClock.getTimezone().getZone());
            entity.setEnable(alarmClock.getEnable());
            entity.setSynced(alarmClock.getDevSynced());
            entity.setId(dao.insert(entity));
        } else {
            entity = list.get(0);
            entity.setName(alarmClock.getName());
            entity.setTimePoint(alarmClock.getTime().getTime());
            entity.setRepeat(alarmClock.getRepeat());
            entity.setNoticeFlag(alarmClock.getNoticeFlag());
            entity.setTimezone(alarmClock.getTimezone().getZone());
            entity.setEnable(alarmClock.getEnable());
            entity.setSynced(alarmClock.getDevSynced());
            dao.update(entity);
        }
        return entity;
    }

    public static void delAlarmClock(String deviceId, AlarmClockEntity alarmClockEntity) {
        if (TextUtils.isEmpty(deviceId) || alarmClockEntity == null || TextUtils.isEmpty(alarmClockEntity.getAlarmClockId())) {
            return;
        }
        AlarmClockEntityDao dao = getAlarmClockEntityDao();
        List<AlarmClockEntity> list = dao.queryBuilder().where(AlarmClockEntityDao.Properties.DeviceId.eq(deviceId),
                AlarmClockEntityDao.Properties.AlarmClockId.eq(alarmClockEntity.getAlarmClockId())).build().list();
        if (!list.isEmpty()) {
            dao.deleteInTx(list);
        }
    }

    private static void deleteAlarmClock(String deviceId) {
        if (TextUtils.isEmpty(deviceId))
            return;
        AlarmClockEntityDao dao = getAlarmClockEntityDao();
        List<AlarmClockEntity> list = dao.queryBuilder().where(AlarmClockEntityDao.Properties.DeviceId.eq(deviceId)).list();
        if (!list.isEmpty()) {
            dao.deleteInTx(list);
        }
    }

    public static void saveAlarmClock(String deviceId, Message.NotifyAlarmClockChangedReqMsg.Detail detail) {
        AlarmClockEntityDao dao = getAlarmClockEntityDao();
        List<AlarmClockEntity> list = dao.queryBuilder().where(AlarmClockEntityDao.Properties.DeviceId.eq(deviceId)).build().list();

        List<AlarmClockEntity> update = new ArrayList<>();
        List<AlarmClockEntity> insert = new ArrayList<>();
        List<AlarmClockEntity> delete = new ArrayList<>();

        for (Message.AlarmClock alarmClock : detail.getAlarmClockList()) {
            switch (detail.getAction()) {
                case ADD:
                case MODIFY: {
                    AlarmClockEntity entity = null;
                    for (AlarmClockEntity old : list) {
                        if (alarmClock.getId().equals(old.getAlarmClockId())) {
                            entity = old;
                            update.add(entity);
                            break;
                        }
                    }
                    if (entity == null) {
                        entity = new AlarmClockEntity();
                        entity.setDeviceId(deviceId);
                        entity.setAlarmClockId(alarmClock.getId());
                        insert.add(entity);
                    }
                    entity.setName(alarmClock.getName());
                    entity.setTimePoint(alarmClock.getTime().getTime());
                    entity.setRepeat(alarmClock.getRepeat());
                    entity.setNoticeFlag(alarmClock.getNoticeFlag());
                    entity.setTimezone(alarmClock.getTimezone().getZone());
                    entity.setEnable(alarmClock.getEnable());
                    entity.setSynced(alarmClock.getDevSynced());
                    break;
                }
                case DEL: {
                    for (AlarmClockEntity old : list) {
                        if (alarmClock.getId().equals(old.getAlarmClockId())) {
                            delete.add(old);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }

        if (!delete.isEmpty()) {
            dao.deleteInTx(delete);
        }
        if (!update.isEmpty()) {
            dao.updateInTx(update);
        }
        if (!insert.isEmpty()) {
            dao.insertInTx(insert);
        }
    }

    public static void saveAlarmClock(String deviceId, List<Message.AlarmClock> alarmClocks) {
        if (TextUtils.isEmpty(deviceId))
            return;
        if (alarmClocks == null)
            return;

        AlarmClockEntityDao dao = getAlarmClockEntityDao();

        List<AlarmClockEntity> old = dao.queryBuilder()
                .where(AlarmClockEntityDao.Properties.DeviceId.eq(deviceId))
                .build().list();
        List<AlarmClockEntity> update = new ArrayList<>();
        List<AlarmClockEntity> insert = new ArrayList<>();
        for (protocol.Message.AlarmClock alarmClock : alarmClocks) {
            AlarmClockEntity entity = null;
            for (Iterator<AlarmClockEntity> it = old.iterator(); it.hasNext(); ) {
                AlarmClockEntity oldEntity = it.next();
                if (alarmClock.getId().equals(oldEntity.getAlarmClockId())) {
                    entity = oldEntity;
                    update.add(entity);

                    it.remove();
                    break;
                }
            }
            if (entity == null) {
                entity = new AlarmClockEntity();
                entity.setDeviceId(deviceId);
                entity.setAlarmClockId(alarmClock.getId());
                insert.add(entity);
            }
            entity.setName(alarmClock.getName());
            entity.setTimePoint(alarmClock.getTime().getTime());
            entity.setRepeat(alarmClock.getRepeat());
            entity.setNoticeFlag(alarmClock.getNoticeFlag());
            entity.setTimezone(alarmClock.getTimezone().getZone());
            entity.setEnable(alarmClock.getEnable());
            entity.setSynced(alarmClock.getDevSynced());
        }
        if (!old.isEmpty()) {
            dao.deleteInTx(old);
        }
        if (!update.isEmpty()) {
            dao.updateInTx(update);
        }
        if (!insert.isEmpty()) {
            dao.insertInTx(insert);
        }
    }

    public static void updateAlarmClockSyncState(String devId, List<String> alarmClocks) {
        if (TextUtils.isEmpty(devId))
            return;
        AlarmClockEntityDao dao = getAlarmClockEntityDao();
        List<AlarmClockEntity> list = dao.queryBuilder()
                .where(AlarmClockEntityDao.Properties.DeviceId.eq(devId), AlarmClockEntityDao.Properties.AlarmClockId.in(alarmClocks)).list();
        if (list.isEmpty())
            return;
        for (AlarmClockEntity entity : list) {
            entity.setSynced(true);
        }
        dao.updateInTx(list);
    }

    public static ClassDisableEntity addOrModifyClassDisable(String deviceId, ClassDisableEntity entity) {
        if (TextUtils.isEmpty(deviceId) || entity == null)
            return null;
        ClassDisableEntityDao dao = getClassDisableEntityDao();
        List<ClassDisableEntity> list = dao.queryBuilder().where(ClassDisableEntityDao.Properties.DeviceId.eq(deviceId))
                .where(ClassDisableEntityDao.Properties.ClassDisableId.eq(entity.getClassDisableId()))
                .build().list();
        if (list.isEmpty()) {
            entity.setSynced(false);
            entity.setId(dao.insert(entity));
        } else {
            ClassDisableEntity old = list.get(0);
            entity.setId(old.getId());
            entity.setSynced(false);
            dao.update(entity);
        }
        return entity;
    }

    public static void delClassDisable(String devId, ClassDisableEntity disableEntity) {
        if (TextUtils.isEmpty(devId) || disableEntity == null || TextUtils.isEmpty(disableEntity.getClassDisableId())) {
            return;
        }
        ClassDisableEntityDao dao = getClassDisableEntityDao();
        List<ClassDisableEntity> list = dao.queryBuilder().where(ClassDisableEntityDao.Properties.DeviceId.eq(devId))
                .where(ClassDisableEntityDao.Properties.ClassDisableId.eq(disableEntity.getClassDisableId())).build().list();
        if (!list.isEmpty()) {
            dao.deleteInTx(list);
        }
    }

    private static void deleteClassDisable(String devId) {
        if (TextUtils.isEmpty(devId)) {
            return;
        }
        ClassDisableEntityDao dao = getClassDisableEntityDao();
        List<ClassDisableEntity> list = dao.queryBuilder().where(ClassDisableEntityDao.Properties.DeviceId.eq(devId)).list();
        if (!list.isEmpty()) {
            dao.deleteInTx(list);
        }
    }

    public static void saveClassDisableAsync(final String deviceId, final List<Message.ClassDisable> classDisables) {
        execute(new GreenDaoSaveDataRunnable() {
            @Override
            protected void doRun() throws SameThreadException {
                saveClassDisable(deviceId, classDisables);
                EventBus.getDefault().postSticky(new Event.ClassDisableUpdated());
            }
        });
    }

    public static void saveClassDisable(String deviceId, List<Message.ClassDisable> classDisables) {
        if (TextUtils.isEmpty(deviceId))
            return;
        if (classDisables == null)
            return;

        ClassDisableEntityDao dao = getClassDisableEntityDao();

        List<ClassDisableEntity> old = dao.queryBuilder()
                .where(ClassDisableEntityDao.Properties.DeviceId.eq(deviceId))
                .build().list();
        List<ClassDisableEntity> update = new ArrayList<>();
        List<ClassDisableEntity> insert = new ArrayList<>();
        for (protocol.Message.ClassDisable classDisable : classDisables) {
            ClassDisableEntity entity = null;
            for (Iterator<ClassDisableEntity> it = old.iterator(); it.hasNext(); ) {
                ClassDisableEntity oldEntity = it.next();
                if (classDisable.getId().equals(oldEntity.getClassDisableId())) {
                    entity = oldEntity;
                    update.add(entity);

                    it.remove();
                    break;
                }
            }
            if (entity == null) {
                entity = new ClassDisableEntity();
                entity.setDeviceId(deviceId);
                entity.setClassDisableId(classDisable.getId());
                insert.add(entity);
            }

            entity.setName(classDisable.getName());
            entity.setBeginTime(classDisable.getStartTime().getTime());
            entity.setEndTime(classDisable.getEndTime().getTime());
            entity.setRepeat(classDisable.getRepeat());
            entity.setTimezone(classDisable.getTimezone().getZone());
            entity.setEnable(classDisable.getEnable());
            entity.setSynced(classDisable.getDevSynced());
        }
        if (!old.isEmpty()) {
            dao.deleteInTx(old);
        }
        if (!update.isEmpty()) {
            dao.updateInTx(update);
        }
        if (!insert.isEmpty()) {
            dao.insertInTx(insert);
        }
    }

    public static void saveClassDisable(String deviceId, Message.NotifyClassDisableChangedReqMsg.Detail detail) {
        ClassDisableEntityDao dao = getClassDisableEntityDao();
        List<ClassDisableEntity> list = dao.queryBuilder().where(ClassDisableEntityDao.Properties.DeviceId.eq(deviceId)).build().list();

        List<ClassDisableEntity> update = new ArrayList<>();
        List<ClassDisableEntity> insert = new ArrayList<>();
        List<ClassDisableEntity> delete = new ArrayList<>();

        for (Message.ClassDisable classDisable : detail.getClassDisableList()) {
            switch (detail.getAction()) {
                case ADD:
                case MODIFY: {
                    ClassDisableEntity entity = null;
                    for (ClassDisableEntity old : list) {
                        if (classDisable.getId().equals(old.getClassDisableId())) {
                            entity = old;
                            update.add(entity);
                            break;
                        }
                    }
                    if (entity == null) {
                        entity = new ClassDisableEntity();
                        entity.setDeviceId(deviceId);
                        entity.setClassDisableId(classDisable.getId());
                        insert.add(entity);
                    }
                    entity.setName(classDisable.getName());
                    entity.setBeginTime(classDisable.getStartTime().getTime());
                    entity.setEndTime(classDisable.getEndTime().getTime());
                    entity.setRepeat(classDisable.getRepeat());
                    entity.setTimezone(classDisable.getTimezone().getZone());
                    entity.setEnable(classDisable.getEnable());
                    entity.setSynced(false);
                    break;
                }
                case DEL: {
                    for (ClassDisableEntity old : list) {
                        if (classDisable.getId().equals(old.getClassDisableId())) {
                            delete.add(old);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }

        if (!delete.isEmpty()) {
            dao.deleteInTx(delete);
        }
        if (!update.isEmpty()) {
            dao.updateInTx(update);
        }
        if (!insert.isEmpty()) {
            dao.insertInTx(insert);
        }
    }

    public static void updateClassDisableSyncState(String devId, List<String> classDisables) {
        if (TextUtils.isEmpty(devId))
            return;
        ClassDisableEntityDao dao = getClassDisableEntityDao();
        List<ClassDisableEntity> list = dao.queryBuilder()
                .where(ClassDisableEntityDao.Properties.DeviceId.eq(devId), ClassDisableEntityDao.Properties.ClassDisableId.in(classDisables)).list();
        if (list.isEmpty())
            return;
        for (ClassDisableEntity entity : list) {
            entity.setSynced(true);
        }
        dao.updateInTx(list);
    }

    public static List<ClassDisableEntity> QueryClassDisableEntities(String deviId) {
        ClassDisableEntityDao dao = getClassDisableEntityDao();
        return dao.queryBuilder().where(ClassDisableEntityDao.Properties.DeviceId.eq(deviId)).build().list();
    }

    public static void saveSchoolGuardAsync(final String deviceId, final Message.SchoolGuard guard) {
        execute(new GreenDaoSaveDataRunnable() {
            @Override
            protected void doRun() throws SameThreadException {
                saveSchoolGuard(deviceId, guard);
                EventBus.getDefault().postSticky(new Event.SchoolGuardUpdated());
            }
        });
    }

    public static void saveSchoolGuard(String deviceId, Message.SchoolGuard guard) {
        DeviceEntityDao dao = getDeviceEntityDao();
        List<DeviceEntity> list = dao.queryBuilder()
                .where(DeviceEntityDao.Properties.DeviceId.eq(deviceId))
                .build().list();

        if (list.isEmpty()) {
            DeviceEntity entity = new DeviceEntity();
            entity.setDeviceId(deviceId);
            entity.setSchoolGuard(guard);
            dao.insert(entity);
        } else {
            DeviceEntity entity = list.get(0);
            entity.setSchoolGuard(guard);
            dao.update(entity);
        }

        if (!list.isEmpty())
            App.getInstance().notifyDeviceEntity(list.get(0));
    }

    public static synchronized void saveCollectPraise(String deviceId, Message.NotifyPraiseChangedReqMsg.Detail detail) {
        CollectPraiseEntityDao dao = getCollectPraiseEntityDao();
        List<CollectPraiseEntity> list = dao.queryBuilder().where(CollectPraiseEntityDao.Properties.DeviceId.eq(deviceId)).build().list();

        List<CollectPraiseEntity> update = new ArrayList<>();
        List<CollectPraiseEntity> insert = new ArrayList<>();
        List<CollectPraiseEntity> delete = new ArrayList<>();

        Message.Praise praise = detail.getPraise();
        switch (detail.getAction()) {
            case ADD:
            case MODIFY:
            case PRAISE:
            case CANCELED:
            case FINISH:
            case COMPLETE: {
                CollectPraiseEntity entity = null;
                for (CollectPraiseEntity old : list) {
                    if (praise.getId().equals(old.getPraiseId())) {
                        entity = old;
                        update.add(entity);
                        break;
                    }
                }
                if (entity == null) {
                    entity = new CollectPraiseEntity();
                    entity.setDeviceId(deviceId);
                    entity.setPraiseId(praise.getId());
                    insert.add(entity);
                }
                entity.setPraise(praise);
                entity.setCompleteTime(praise.getCompleteTime());
                entity.setFinishTime(praise.getFinishTime());
                entity.setTimezone(praise.getTimezone().getZone());
                entity.setIsCancel(praise.getIsCancel());
                break;
            }
            case DEL: {
                for (CollectPraiseEntity old : list) {
                    if (praise.getId().equals(old.getPraiseId())) {
                        delete.add(old);
                    }
                }
                break;
            }
            default:
                break;
        }

        if (!delete.isEmpty()) {
            dao.deleteInTx(delete);
        }
        if (!update.isEmpty()) {
            dao.updateInTx(update);
        }
        if (!insert.isEmpty()) {
            dao.insertInTx(insert);
        }
    }

    public static void saveDeviceLastPositionAsync(final String deviceId, final Message.Position position) {
        execute(new GreenDaoSaveDataRunnable() {
            @Override
            protected void doRun() throws SameThreadException {
                saveDeviceLastPosition(deviceId, position);
            }
        });
    }

    public static synchronized void saveDeviceLastPosition(String deviceId, Message.Position position) {
        if (TextUtils.isEmpty(deviceId) || position == null)
            return;

        DeviceEntityDao dao = getDeviceEntityDao();
        List<DeviceEntity> list = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
        if (list.isEmpty()) {
            DeviceEntity bean = new DeviceEntity();
            bean.setDeviceId(deviceId);
            bean.setLastPosition(position);
            dao.insert(bean);
        } else {
            DeviceEntity bean = list.get(0);
            bean.setLastPosition(position);
            dao.update(bean);
        }

        list = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
        if (!list.isEmpty())
            App.getInstance().notifyDeviceEntity(list.get(0));
    }

    public static void saveDeviceLastSensorDataAsync(final String deviceId, final Message.DeviceSensorData data, final long timeSec) {
        execute(new GreenDaoSaveDataRunnable() {
            @Override
            protected void doRun() throws SameThreadException {
                saveDeviceLastSensorData(deviceId, data, timeSec);
            }
        });
    }

    public static synchronized void saveDeviceLastSensorData(String deviceId, Message.DeviceSensorData data, long timeSec) {
        if (TextUtils.isEmpty(deviceId) || data == null)
            return;

        DeviceEntityDao dao = getDeviceEntityDao();
        List<DeviceEntity> list = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
        if (list.isEmpty()) {
            DeviceEntity bean = new DeviceEntity();
            bean.setDeviceId(deviceId);
            bean.setBatteryPercent(data.getBatteryPercent());
            bean.setBatteryVoltage(data.getBatteryVoltage());
            bean.setBatteryLevel(data.getBatteryLevel());
            bean.setStepCount(data.getStep());
            bean.setSensorDataTime(timeSec);
            dao.insert(bean);
        } else {
            DeviceEntity bean = list.get(0);
            bean.setBatteryPercent(data.getBatteryPercent());
            bean.setBatteryVoltage(data.getBatteryVoltage());
            bean.setBatteryLevel(data.getBatteryLevel());
            bean.setStepCount(data.getStep());
            bean.setSensorDataTime(timeSec);
            dao.update(bean);
        }

        list = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
        if (!list.isEmpty())
            App.getInstance().notifyDeviceEntity(list.get(0));
    }

    public static DeviceEntity queryDeviceEntity(String mDevId) {
        DeviceEntityDao dao = getDeviceEntityDao();
        List<DeviceEntity> list = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(mDevId)).build().list();
        if (list.isEmpty())
            return null;
        else
            return list.get(0);
    }

    public static void saveChatMessage(String userId, GeneratedMessageV3 messageV3) {
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setUserId(userId);

        if (messageV3 instanceof Message.NotifyMicroChatTextReqMsg) {
            Message.NotifyMicroChatTextReqMsg reqMsg = (Message.NotifyMicroChatTextReqMsg) messageV3;
            chatEntity.setDeviceId(reqMsg.getSrcAddr().getAddr());
            chatEntity.setSenderId(reqMsg.getSrcAddr().getAddr());
            chatEntity.setSenderType(reqMsg.getSrcAddr().getTypeValue());
            chatEntity.setTimestamp(reqMsg.getTimestamp() / 1000000L);
            chatEntity.setMessageType(ChatEntity.TYPE_TEXT);
            chatEntity.setText(reqMsg.getText());
        } else if (messageV3 instanceof Message.NotifyMicroChatVoiceReqMsg) {
            Message.NotifyMicroChatVoiceReqMsg reqMsg = (Message.NotifyMicroChatVoiceReqMsg) messageV3;
            chatEntity.setDeviceId(reqMsg.getSrcAddr().getAddr());
            chatEntity.setSenderId(reqMsg.getSrcAddr().getAddr());
            chatEntity.setSenderType(reqMsg.getSrcAddr().getTypeValue());
            chatEntity.setTimestamp(reqMsg.getTimestamp() / 1000000L);
            chatEntity.setMessageType(ChatEntity.TYPE_VOICE);
            chatEntity.setFilename(reqMsg.getFileName());
            chatEntity.setFileSize(reqMsg.getFileSize());
            chatEntity.setVoiceDuration(reqMsg.getDuration());
        } else if (messageV3 instanceof Message.NotifyMicroChatEmoticonReqMsg) {
            Message.NotifyMicroChatEmoticonReqMsg reqMsg = (Message.NotifyMicroChatEmoticonReqMsg) messageV3;
            chatEntity.setDeviceId(reqMsg.getSrcAddr().getAddr());
            chatEntity.setSenderId(reqMsg.getSrcAddr().getAddr());
            chatEntity.setSenderType(reqMsg.getSrcAddr().getTypeValue());
            chatEntity.setTimestamp(reqMsg.getTimestamp() / 1000000L);
            chatEntity.setMessageType(ChatEntity.TYPE_EMOTICON);
            chatEntity.setEmoticon(reqMsg.getEmoticonId());
        } else if (messageV3 instanceof Message.NotifyChatMessageReqMsg) {
            Message.NotifyChatMessageReqMsg reqMsg = (Message.NotifyChatMessageReqMsg) messageV3;
            chatEntity.setDeviceId(reqMsg.getSrc().getAddr());
            chatEntity.setSenderId(reqMsg.getSrc().getAddr());
            chatEntity.setSenderType(reqMsg.getSrc().getTypeValue());
            chatEntity.setTimestamp(reqMsg.getTimestamp() / 1000000L);
            Message.ChatMessage msg = reqMsg.getMsg();
            switch (msg.getMessageCase()) {
                case TEXT:
                    chatEntity.setMessageType(ChatEntity.TYPE_TEXT);
                    chatEntity.setText(msg.getText());
                    break;
                case VOICE: {
                    chatEntity.setMessageType(ChatEntity.TYPE_VOICE);
                    Message.ChatMessage.Voice voice = msg.getVoice();
                    chatEntity.setFilename(voice.getFileName());
                    chatEntity.setFileSize(voice.getFileSize());
                    chatEntity.setVoiceDuration(voice.getDuration());
                }
                break;
                case EMOTICON:
                    chatEntity.setMessageType(ChatEntity.TYPE_EMOTICON);
                    chatEntity.setEmoticon(msg.getEmoticon());
                    break;
                default:
                    return;
            }
        } else if (messageV3 instanceof Message.NotifyGroupChatMessageReqMsg) {
            Message.NotifyGroupChatMessageReqMsg reqMsg = (Message.NotifyGroupChatMessageReqMsg) messageV3;
            chatEntity.setDeviceId(reqMsg.getDeviceId());
            chatEntity.setGroupId(reqMsg.getGroupId());
            chatEntity.setSenderId(reqMsg.getSender().getAddr());
            chatEntity.setSenderType(reqMsg.getSender().getTypeValue());
            chatEntity.setTimestamp(reqMsg.getTimestamp() / 1000000L);
            Message.ChatMessage msg = reqMsg.getMsg();
            switch (msg.getMessageCase()) {
                case TEXT:
                    chatEntity.setMessageType(ChatEntity.TYPE_TEXT);
                    chatEntity.setText(msg.getText());
                    break;
                case VOICE: {
                    chatEntity.setMessageType(ChatEntity.TYPE_VOICE);
                    Message.ChatMessage.Voice voice = msg.getVoice();
                    chatEntity.setFilename(voice.getFileName());
                    chatEntity.setFileSize(voice.getFileSize());
                    chatEntity.setVoiceDuration(voice.getDuration());
                }
                break;
                case EMOTICON:
                    chatEntity.setMessageType(ChatEntity.TYPE_EMOTICON);
                    chatEntity.setEmoticon(msg.getEmoticon());
                    break;
                default:
                    return;
            }
        }

        GreenUtils.getChatEntityDao().insert(chatEntity);
    }

    public static void deleteChat(String deviceId, String userId) {
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(userId)) {
            return;
        }
        ChatEntityDao dao = getChatEntityDao();
        List<ChatEntity> list = dao.queryBuilder()
                .where(ChatEntityDao.Properties.DeviceId.eq(deviceId))
                .where(ChatEntityDao.Properties.UserId.eq(userId))
                .build().list();
        if (!list.isEmpty()) {
            dao.deleteInTx(list);
        }
    }

    public static void markChatSeen(String deviceId, String userId) {
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(userId)) {
            return;
        }
        String familyGroupId = "";
        List<DeviceEntity> device = getDeviceEntityDao().queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).list();
        if (!device.isEmpty()) {
            familyGroupId = device.get(0).getFamilyGroup();
        }
        App.getInstance().getDaoSession().getDatabase().execSQL("UPDATE " + ChatEntityDao.TABLENAME +
                        " SET " + ChatEntityDao.Properties.IsSeen.columnName + "=?" +
                        " WHERE " +
                        ChatEntityDao.Properties.UserId.columnName + "=?" +
                        " AND " +
                        ChatEntityDao.Properties.DeviceId.columnName + "=?" +
                        " AND " +
                        ChatEntityDao.Properties.IsSeen.columnName + "=?"
                ,
                new Object[]{true, deviceId, userId, false});
        if (!TextUtils.isEmpty(familyGroupId)) {
            App.getInstance().getDaoSession().getDatabase().execSQL("UPDATE " + ChatEntityDao.TABLENAME +
                            " SET " + ChatEntityDao.Properties.IsSeen.columnName + "=?" +
                            " WHERE " +
                            ChatEntityDao.Properties.GroupId.columnName + "=?" +
                            " AND " +
                            ChatEntityDao.Properties.IsSeen.columnName + "=?"
                    ,
                    new Object[]{true, familyGroupId, false});
        }
        getChatEntityDao().detachAll();
    }

    public static synchronized void savePraise(String devId, List<protocol.Message.Praise> praises) {
        if (TextUtils.isEmpty(devId))
            return;
        if (praises == null)
            return;

        CollectPraiseEntityDao dao = App.getInstance().getDaoSession().getCollectPraiseEntityDao();

        List<CollectPraiseEntity> old = dao.queryBuilder()
                .where(CollectPraiseEntityDao.Properties.DeviceId.eq(devId))
                .build().list();
        List<CollectPraiseEntity> update = new ArrayList<>();
        List<CollectPraiseEntity> insert = new ArrayList<>();
        for (protocol.Message.Praise praise : praises) {
            CollectPraiseEntity entity = null;
            for (Iterator<CollectPraiseEntity> it = old.iterator(); it.hasNext(); ) {
                entity = it.next();
                if (praise.getId().equals(entity.getPraiseId())) {
                    if (!praise.equals(entity.getPraise())) {
                        entity.setPraise(praise);
                        update.add(entity);
                    }

                    it.remove();
                    break;
                }
            }
            if (entity == null) {
                entity = new CollectPraiseEntity();
                entity.setDeviceId(devId);
                entity.setPraise(praise);
                insert.add(entity);
            }
        }
        if (!old.isEmpty()) {
            dao.deleteInTx(old);
        }
        if (!update.isEmpty()) {
            dao.updateInTx(update);
        }
        if (!insert.isEmpty()) {
            dao.insertInTx(insert);
        }
    }

    public static synchronized void savePraise(String devId, protocol.Message.Praise praise) {
        if (TextUtils.isEmpty(devId))
            return;
        if (praise == null)
            return;

        CollectPraiseEntityDao dao = App.getInstance().getDaoSession().getCollectPraiseEntityDao();

        List<CollectPraiseEntity> old = dao.queryBuilder()
                .where(CollectPraiseEntityDao.Properties.DeviceId.eq(devId))
                .where(CollectPraiseEntityDao.Properties.PraiseId.eq(praise.getId()))
                .build().list();
        if (old.isEmpty()) {
            CollectPraiseEntity entity = new CollectPraiseEntity();
            entity.setDeviceId(devId);
            entity.setPraise(praise);
            dao.insert(entity);
        } else {
            CollectPraiseEntity entity = old.get(0);
            if (!praise.equals(entity.getPraise())) {
                entity.setPraise(praise);
                dao.update(entity);
            }
        }
    }

    public static synchronized void deletePraise(String deviceId, Message.Praise praise) {
        if (TextUtils.isEmpty(deviceId))
            return;
        if (praise == null)
            return;

        CollectPraiseEntityDao dao = App.getInstance().getDaoSession().getCollectPraiseEntityDao();
        List<CollectPraiseEntity> list = dao.queryBuilder()
                .where(CollectPraiseEntityDao.Properties.DeviceId.eq(deviceId))
                .where(CollectPraiseEntityDao.Properties.PraiseId.eq(praise.getId()))
                .build().list();
        if (!list.isEmpty()) {
            dao.deleteInTx(list);
        }
    }

    private static void deletePraise(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            return;
        }

        CollectPraiseEntityDao dao = getCollectPraiseEntityDao();
        List<CollectPraiseEntity> list = dao.queryBuilder()
                .where(CollectPraiseEntityDao.Properties.DeviceId.eq(deviceId))
                .list();
        if (!list.isEmpty()) {
            dao.deleteInTx(list);
        }
    }

    public static void clearDeviceWhenUnbind(String deviceId, String userId, long clearLevel) {
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(userId))
            return;

        // 删除设备-用户相关的 联系人、消息中心、宝贝信息, 设备信息
        deleteContact(deviceId, userId);
        deleteSos(deviceId);
        deleteClassDisable(deviceId);
        deleteAlarmClock(deviceId);
        deleteFence(deviceId);
        deletePraise(deviceId);
        deleteFamilyChatGroupMember(deviceId);
        if ((clearLevel & Message.UnbindClearFlag.UBCF_NOTIFI_MESSAGE_VALUE) != 0) {
            deleteNotifyMessage(deviceId, userId);
        } else {
            markNotifyMessageSeen(deviceId, userId);
        }
        if ((clearLevel & Message.UnbindClearFlag.UBCF_CHAT_VALUE) != 0) {
            deleteChat(deviceId, userId);
        } else {
            markChatSeen(deviceId, userId);
        }
        if ((clearLevel & Message.UnbindClearFlag.UBCF_LOCATION_HISTORY_VALUE) != 0) {
            deleteLocations(deviceId);
        }
        deleteBaby(deviceId, userId);
        deleteDevice(deviceId);
        DeviceInfo.remove(deviceId);
    }

    private static void deleteDevice(String deviceId) {
        DeviceEntityDao dao = getDeviceEntityDao();
        List<DeviceEntity> list = dao.queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId))
                .build().list();
        if (!list.isEmpty()) {
            dao.deleteInTx(list);
        }
    }

    public static void saveFence(String deviceId, List<Message.Fence> fences) {
        if (TextUtils.isEmpty(deviceId))
            return;
        if (fences == null)
            return;

        FenceEntityDao dao = getFenceEntityDao();

        List<FenceEntity> old = dao.queryBuilder()
                .where(FenceEntityDao.Properties.DeviceId.eq(deviceId))
                .build().list();
        List<FenceEntity> update = new ArrayList<>();
        List<FenceEntity> insert = new ArrayList<>();
        for (protocol.Message.Fence fence : fences) {
            FenceEntity entity = null;
            for (Iterator<FenceEntity> it = old.iterator(); it.hasNext(); ) {
                FenceEntity oldEntity = it.next();
                if (fence.getId().equals(oldEntity.getFenceId())) {
                    entity = oldEntity;
                    update.add(entity);

                    it.remove();
                    break;
                }
            }
            if (entity == null) {
                entity = new FenceEntity();
                entity.setDeviceId(deviceId);
                insert.add(entity);
            }
            entity.setFence(fence);
        }
        if (!old.isEmpty()) {
            dao.deleteInTx(old);
        }
        if (!update.isEmpty()) {
            dao.updateInTx(update);
        }
        if (!insert.isEmpty()) {
            dao.insertInTx(insert);
        }
    }

    public static void modifyFence(FenceEntity fenceEntity) {
        FenceEntityDao dao = getFenceEntityDao();
        dao.update(fenceEntity);
    }

    public static void saveFence(String deviceId, Message.NotifyFenceChangedReqMsg.Detail detail) {
        if (TextUtils.isEmpty(deviceId) || detail == null) {
            return;
        }

        FenceEntityDao dao = getFenceEntityDao();
        List<FenceEntity> list = dao.queryBuilder().where(FenceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();

        List<FenceEntity> update = new ArrayList<>();
        List<FenceEntity> insert = new ArrayList<>();
        List<FenceEntity> delete = new ArrayList<>();

        for (Message.Fence fence : detail.getFenceList()) {
            switch (detail.getAction()) {
                case ADD:
                case MODIFY: {
                    FenceEntity entity = null;
                    for (FenceEntity old : list) {
                        if (fence.getId().equals(old.getFenceId())) {
                            entity = old;
                            update.add(entity);
                            break;
                        }
                    }
                    if (entity == null) {
                        entity = new FenceEntity();
                        entity.setDeviceId(deviceId);
                        insert.add(entity);
                    }
                    entity.setFence(fence);
                    break;
                }
                case DEL: {
                    for (FenceEntity old : list) {
                        if (fence.getId().equals(old.getFenceId())) {
                            delete.add(old);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }

        if (!delete.isEmpty()) {
            dao.deleteInTx(delete);
        }
        if (!update.isEmpty()) {
            dao.updateInTx(update);
        }
        if (!insert.isEmpty()) {
            dao.insertInTx(insert);
        }
    }

    public static FenceEntity saveFence(String deviceId, Message.Fence fence) {
        if (TextUtils.isEmpty(deviceId) || fence == null)
            return null;
        FenceEntityDao dao = getFenceEntityDao();
        List<FenceEntity> list = dao.queryBuilder().where(FenceEntityDao.Properties.DeviceId.eq(deviceId))
                .where(FenceEntityDao.Properties.FenceId.eq(fence.getId())).build().list();
        FenceEntity entity;
        if (list.isEmpty()) {
            entity = new FenceEntity();
            entity.setDeviceId(deviceId);
            entity.setFence(fence);
            entity.setId(dao.insert(entity));
        } else {
            entity = list.get(0);
            entity.setFence(fence);
            dao.update(entity);
        }
        return entity;
    }

    public static void deleteFence(String deviceId, String fenceId) {
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(fenceId)) {
            return;
        }

        FenceEntityDao dao = getFenceEntityDao();
        List<FenceEntity> list = dao.queryBuilder()
                .where(FenceEntityDao.Properties.DeviceId.eq(deviceId))
                .where(FenceEntityDao.Properties.FenceId.eq(fenceId))
                .list();
        if (!list.isEmpty()) {
            dao.deleteInTx(list);
        }
    }

    private static void deleteFence(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            return;
        }

        FenceEntityDao dao = getFenceEntityDao();
        List<FenceEntity> list = dao.queryBuilder()
                .where(FenceEntityDao.Properties.DeviceId.eq(deviceId))
                .list();
        if (!list.isEmpty()) {
            dao.deleteInTx(list);
        }
    }

    public static void saveUserInfo(Message.UserInfo userInfo) {
        if (userInfo == null || TextUtils.isEmpty(userInfo.getUserId()))
            return;

        UserEntityDao dao = getUserEntityDao();
        List<UserEntity> list = dao.queryBuilder().where(UserEntityDao.Properties.UserId.eq(userInfo.getUserId())).list();
        UserEntity entity;
        if (list.isEmpty()) {
            entity = new UserEntity();
            entity.setUserId(userInfo.getUserId());
        } else {
            entity = list.get(0);
        }
        entity.setUserInfo(userInfo);
        if (entity.getId() == null) {
            entity.setId(dao.insert(entity));
        } else {
            dao.update(entity);
        }
    }

    public static @Nullable String getUserPhone(@Nullable String userId) {
        if (TextUtils.isEmpty(userId))
            return null;
        List<UserEntity> userEntityList = GreenUtils.getUserEntityDao().queryBuilder().where(UserEntityDao.Properties.UserId.eq(userId)).list();
        if (userEntityList.isEmpty()) {
            return null;
        }
        String phone = userEntityList.get(0).getPhone();
        if (phone == null)
            return "";
        return phone;
    }

    public static void upDataLocationMode(Message.LocationMode locationMode, String devId) {
        List<DeviceEntity> deviceEntities = GreenUtils.getDeviceEntityDao().queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(devId)).build().list();
        if (!deviceEntities.isEmpty()) {
            DeviceEntity deviceEntity = deviceEntities.get(0);
            deviceEntity.setLocationMode(locationMode.getNumber());
            GreenUtils.getDeviceEntityDao().update(deviceEntity);
        }
    }

    public static void saveFamilyGroupAsync(@NonNull String deviceId, @NonNull Message.ChatGroup group) {
        execute(new GreenDaoSaveDataRunnable() {
            @Override
            protected void doRun() throws SameThreadException {
                saveFamilyGroup(deviceId, group);
            }
        });
    }

    public static void saveFamilyGroup(@NonNull String deviceId, @NonNull Message.ChatGroup group) {
        if (TextUtils.isEmpty(deviceId) || group == null)
            return;
        List<BabyEntity> bl = getBabyEntityDao().queryBuilder().where(BabyEntityDao.Properties.DeviceId.eq(deviceId)).list();
        List<BabyEntity> updateBl = new ArrayList<>();
        for (BabyEntity b : bl) {
            if (!group.getId().equals(b.getFamilyGroup())) {
                b.setFamilyGroup(group.getId());
                updateBl.add(b);
            }
        }
        if (!updateBl.isEmpty()) {
            getBabyEntityDao().updateInTx(updateBl);
        }

        List<DeviceEntity> dl = getDeviceEntityDao().queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).list();
        DeviceEntity deviceEntity = null;
        if (dl.isEmpty()) {
            deviceEntity = new DeviceEntity();
            deviceEntity.setDeviceId(deviceId);
            deviceEntity.setFamilyGroup(group.getId());
            deviceEntity.setId(getDeviceEntityDao().insert(deviceEntity));
        } else {
            DeviceEntity d = dl.get(0);
            if (!group.getId().equals(d.getFamilyGroup())) {
                deviceEntity = d;
                deviceEntity.setFamilyGroup(group.getId());
                getDeviceEntityDao().update(deviceEntity);
            }
        }

        if (group.getMemberCount() > 0) {
            FamilyChatGroupMemberEntityDao memberEntityDao = getFamilyChatGroupMemberEntityDao();
            List<FamilyChatGroupMemberEntity> gl = memberEntityDao.queryBuilder().where(FamilyChatGroupMemberEntityDao.Properties.GroupId.eq(group.getId())).list();
            List<FamilyChatGroupMemberEntity> update = new ArrayList<>();
            List<FamilyChatGroupMemberEntity> insert = new ArrayList<>();
            for (Message.ChatGroup.Member m : group.getMemberList()) {
                String id;
                FamilyChatGroupMemberEntity memberEntity = null;
                switch (m.getInfoCase()) {
                    case USER:
                        id = m.getUser().getUsrDevAssoc().getUserId();
                        for (Iterator<FamilyChatGroupMemberEntity> it = gl.iterator(); it.hasNext(); ) {
                            FamilyChatGroupMemberEntity old = it.next();
                            if (!TextUtils.isEmpty(old.getUserId()) && id.equals(old.getUserId())) {
                                memberEntity = old;

                                boolean shouldUpdate = false;
                                if (!m.getUser().getUsrDevAssoc().getRelation().equals(old.getRelation())) {
                                    old.setRelation(m.getUser().getUsrDevAssoc().getRelation());
                                    shouldUpdate = true;
                                }
                                if (m.getUser().getUsrDevAssoc().getPermissionValue() != old.getPermission()) {
                                    old.setPermission(m.getUser().getUsrDevAssoc().getPermissionValue());
                                    shouldUpdate = true;
                                }
                                if (m.getUser().getUsrDevAssoc().getAvatar().equals(old.getUserAvatar())) {
                                    old.setUserAvatar(m.getUser().getUsrDevAssoc().getAvatar());
                                    shouldUpdate = true;
                                }

                                if (shouldUpdate) {
                                    update.add(old);
                                }
                                it.remove();
                                break;
                            }
                        }
                        if (memberEntity == null) {
                            memberEntity = new FamilyChatGroupMemberEntity();
                            memberEntity.setGroupId(group.getId());
                            memberEntity.setUserId(id);
                            memberEntity.setRelation(m.getUser().getUsrDevAssoc().getRelation());
                            memberEntity.setPermission(m.getUser().getUsrDevAssoc().getPermissionValue());
                            if (!TextUtils.isEmpty(m.getUser().getUsrDevAssoc().getAvatar())) {
                                memberEntity.setUserAvatar(m.getUser().getUsrDevAssoc().getAvatar());
                            }
                            insert.add(memberEntity);
                        }
                    case DEVICE:
                        id = m.getDevice().getDeviceId();
                        for (Iterator<FamilyChatGroupMemberEntity> it = gl.iterator(); it.hasNext(); ) {
                            FamilyChatGroupMemberEntity old = it.next();
                            if (!TextUtils.isEmpty(old.getDeviceId()) && id.equals(old.getDeviceId())) {
                                memberEntity = old;

                                boolean shouldUpdate = false;
                                if (!m.getDevice().getName().equals(old.getBabyName())) {
                                    old.setBabyName(m.getDevice().getName());
                                    shouldUpdate = true;
                                }
                                if (!m.getDevice().getAvatar().equals(old.getBabyAvatar())) {
                                    old.setBabyAvatar(m.getDevice().getAvatar());
                                    shouldUpdate = true;
                                }
                                if (shouldUpdate) {
                                    update.add(old);
                                }
                                it.remove();
                            }
                        }
                        if (memberEntity == null) {
                            memberEntity = new FamilyChatGroupMemberEntity();
                            memberEntity.setGroupId(group.getId());
                            memberEntity.setDeviceId(id);
                            memberEntity.setBabyName(m.getDevice().getName());
                            memberEntity.setBabyAvatar(m.getDevice().getAvatar());
                            insert.add(memberEntity);
                        }
                    default:
                        break;
                }
            }

            boolean memberUpdated = false;
            if (!gl.isEmpty()) {
                memberEntityDao.deleteInTx(gl);
                memberUpdated = true;
            }
            if (!update.isEmpty()) {
                memberEntityDao.updateInTx(update);
                memberUpdated = true;
            }
            if (!insert.isEmpty()) {
                memberEntityDao.insertInTx(insert);
                memberUpdated = true;
            }
            if (memberUpdated) {
                EventBus.getDefault().postSticky(new Event.FamilyChatGroupMemberUpdated(deviceId, group.getId()));
            }
        }

        if (deviceEntity != null)
            App.getInstance().notifyDeviceEntity(deviceEntity);
    }

    public static void deleteFamilyChatGroupMember(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            return;
        }
        List<DeviceEntity> del = getDeviceEntityDao().queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).list();
        if (del.isEmpty())
            return;
        DeviceEntity de = del.get(0);
        if (TextUtils.isEmpty(de.getFamilyGroup()))
            return;
        FamilyChatGroupMemberEntityDao dao = getFamilyChatGroupMemberEntityDao();
        List<FamilyChatGroupMemberEntity> list = dao.queryBuilder()
                .where(FamilyChatGroupMemberEntityDao.Properties.GroupId.eq(de.getFamilyGroup()))
                .list();
        if (!list.isEmpty()) {
            dao.deleteInTx(list);
            EventBus.getDefault().postSticky(new Event.FamilyChatGroupMemberUpdated(deviceId, list.get(0).getGroupId()));
        }
    }

    public static void deleteFamilyChatGroupMember(Message.ChatGroup.Member member, String group_id) {

        if (member != null && !TextUtils.isEmpty(group_id)) {
            switch (member.getInfoCase()) {
                case DEVICE:

                    break;
                case USER:
                    String userId = member.getUser().getUsrDevAssoc().getUserId();
                    FamilyChatGroupMemberEntityDao dao = getFamilyChatGroupMemberEntityDao();
                    List<FamilyChatGroupMemberEntity> list = dao.queryBuilder()
                            .where(FamilyChatGroupMemberEntityDao.Properties.GroupId.eq(group_id))
                            .where(FamilyChatGroupMemberEntityDao.Properties.UserId.eq(userId))
                            .list();
                    if (!list.isEmpty()) {
                        String deviceId = member.getUser().getUsrDevAssoc().getDeviceId();
                        dao.deleteInTx(list);
                        EventBus.getDefault().postSticky(new Event.FamilyChatGroupMemberUpdated(deviceId, group_id));
                    }
                    break;
            }
        }
    }

    public static void tryModifyFamilyChatGroupMember(Message.UsrDevAssoc usrDevAssoc) {
        if (usrDevAssoc == null || TextUtils.isEmpty(usrDevAssoc.getDeviceId()) || TextUtils.isEmpty(usrDevAssoc.getUserId())) {
            return;
        }

        FamilyChatGroupMemberEntityDao dao = getFamilyChatGroupMemberEntityDao();
        List<FamilyChatGroupMemberEntity> list = dao.queryBuilder()
                .where(FamilyChatGroupMemberEntityDao.Properties.DeviceId.eq(usrDevAssoc.getDeviceId()))
                .where(FamilyChatGroupMemberEntityDao.Properties.UserId.eq(usrDevAssoc.getUserId()))
                .list();
        if (!list.isEmpty()) {
            for (FamilyChatGroupMemberEntity entity : list) {
                entity.setPermission(usrDevAssoc.getPermissionValue());
                entity.setRelation(usrDevAssoc.getRelation());
                entity.setUserAvatar(usrDevAssoc.getAvatar());
            }
            dao.updateInTx(list);

            for (FamilyChatGroupMemberEntity entity : list) {
                if (TextUtils.isEmpty(entity.getGroupId())) {
                    continue;
                }
                EventBus.getDefault().postSticky(new Event.FamilyChatGroupMemberUpdated(usrDevAssoc.getDeviceId(), entity.getGroupId()));
            }
        }
    }

    public static void modifyFamilyChatGroupMember(Message.ChatGroup.Member member, String group_id) {
        if (member != null && !TextUtils.isEmpty(group_id)) {
            switch (member.getInfoCase()) {
                case DEVICE:

                    break;
                case USER:
                    String userId = member.getUser().getUsrDevAssoc().getUserId();
                    FamilyChatGroupMemberEntityDao dao = getFamilyChatGroupMemberEntityDao();
                    List<FamilyChatGroupMemberEntity> list = dao.queryBuilder()
                            .where(FamilyChatGroupMemberEntityDao.Properties.GroupId.eq(group_id))
                            .where(FamilyChatGroupMemberEntityDao.Properties.UserId.eq(userId))
                            .list();
                    if (!list.isEmpty()) {
                        for (FamilyChatGroupMemberEntity entity : list) {
                            entity.setPermission(member.getUser().getUsrDevAssoc().getPermissionValue());
                            entity.setRelation(member.getUser().getUsrDevAssoc().getRelation());
                            entity.setUserAvatar(member.getUser().getUsrDevAssoc().getAvatar());
                        }
                        dao.updateInTx(list);

                        String deviceId = member.getUser().getUsrDevAssoc().getDeviceId();
                        EventBus.getDefault().postSticky(new Event.FamilyChatGroupMemberUpdated(deviceId, group_id));
                    }
                    break;
            }
        }
    }

    public static void addFamilyChatGroupMember(Message.ChatGroup.Member member, String group_id) {
        if (member != null && !TextUtils.isEmpty(group_id)) {
            switch (member.getInfoCase()) {
                case DEVICE:

                    break;
                case USER:
                    String userId = member.getUser().getUsrDevAssoc().getUserId();
                    FamilyChatGroupMemberEntityDao dao = getFamilyChatGroupMemberEntityDao();
                    List<FamilyChatGroupMemberEntity> list = dao.queryBuilder()
                            .where(FamilyChatGroupMemberEntityDao.Properties.GroupId.eq(group_id))
                            .where(FamilyChatGroupMemberEntityDao.Properties.UserId.eq(userId))
                            .build().list();
                    if (list.isEmpty()) {
                        Message.UsrDevAssoc uda = member.getUser().getUsrDevAssoc();
                        String deviceId = uda.getDeviceId();
                        FamilyChatGroupMemberEntity entity = new FamilyChatGroupMemberEntity();
                        entity.setGroupId(group_id);
                        entity.setBabyName(member.getDevice().getName());
                        entity.setBabyAvatar(member.getDevice().getAvatar());
                        entity.setUserId(userId);
                        entity.setUserAvatar(member.getUser().getUsrDevAssoc().getAvatar());
                        entity.setPermission(uda.getPermissionValue());
                        entity.setRelation(uda.getRelation());
                        if (!TextUtils.isEmpty(uda.getAvatar())) {
                            entity.setUserAvatar(uda.getAvatar());
                        }
                        dao.insert(entity);
                        EventBus.getDefault().postSticky(new Event.FamilyChatGroupMemberUpdated(deviceId, group_id));
                    }
                    break;
            }
        }
    }


    public static void upDataSmsAgentStatus(boolean smsAgentEnable, String devId, String userId) {
        List<BabyEntity> babyEntities = GreenUtils.getBabyEntityDao().queryBuilder()
                .where(BabyEntityDao.Properties.DeviceId.eq(devId), BabyEntityDao.Properties.UserId.eq(userId))
                .list();
        if (!babyEntities.isEmpty()) {
            BabyEntity babyEntity = babyEntities.get(0);
            babyEntity.setSmsAgentEnabled(smsAgentEnable);
            GreenUtils.getBabyEntityDao().update(babyEntity);
        }
    }

    public static void saveSmsEntity(List<SmsEntity> smsEntities) {
        SmsEntityDao dao = getSmsEntityDao();
        dao.deleteAll();
        for (SmsEntity smsEntity : smsEntities) {
            dao.insert(smsEntity);
        }
//        List<SmsEntity> list = dao.queryBuilder().where(SmsEntityDao.Properties.UserId.eq(devId))
//                .where(SmsEntityDao.Properties.DeviceId.eq(userId))
//                .build().list();
//        if (!list.isEmpty()) {
//            for (SmsEntity sms : list) {
//                if (smsEntity.getSmsId().equals(sms.getSmsId())) {
//                    smsEntity.setId(sms.getId());
//                } else {
//                    smsEntity.setId(dao.insert(smsEntity));
//                }
//            }
//        }
    }

    public static void saveSmsEntity(Message.SMS sms, String devId, String userId) {

        List<SmsEntity> list = getSmsEntityDao().queryBuilder()
                .where(SmsEntityDao.Properties.SmsId.eq(sms.getId()))
                .build().list();
        if (!list.isEmpty()) {
            L.e(TAG, "saveSmsEntity error");
            return;
        }
        SmsEntity entity = new SmsEntity();
        entity.setSmsId(sms.getId());
        entity.setDeviceId(devId);
        entity.setUserId(userId);
        entity.setTime(sms.getRecvTime()); //通知时间
        entity.setNumber(sms.getPeerNumber());
        entity.setText(sms.getContent());
        entity.setSynced(false);

        SmsEntityDao dao = GreenUtils.getSmsEntityDao();
        dao.insert(entity);

    }

    public static void updateSmsUnreadMark(List<SmsEntity> entities) {
        if (entities.isEmpty())
            return;
        SmsEntityDao dao = getSmsEntityDao();
        for (SmsEntity entity : entities) {
            entity.setUnreadMark(true);
            dao.update(entity);
        }
    }

    public static void saveNotificationChannelAsync(String userId, String deviceId, int notificationChannel) {
        execute(new GreenDaoSaveDataRunnable() {
            @Override
            protected void doRun() throws SameThreadException {
                saveNotificationChannel(userId, deviceId, notificationChannel);
            }
        });
    }

    public static void saveNotificationChannel(String userId, String deviceId, int notificationChannel) {
        BabyEntityDao dao = GreenUtils.getBabyEntityDao();
        List<BabyEntity> babyEntities = dao.queryBuilder()
                .where(BabyEntityDao.Properties.DeviceId.eq(deviceId), BabyEntityDao.Properties.UserId.eq(userId))
                .list();
        if (!babyEntities.isEmpty()) {
            boolean updated = false;
            for (BabyEntity babyEntity : babyEntities) {
                if (babyEntity.getNotificationChannel() != notificationChannel) {
                    babyEntity.setNotificationChannel(notificationChannel);
                    updated = true;
                }
            }
            if (updated) {
                dao.updateInTx(babyEntities);

                DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId);
                if (deviceInfo != null) {
                    deviceInfo.setNotificationChannel(notificationChannel);
                }
            }
        }
    }

    public static int getNotificationChannel(String userId, String deviceId) {
        BabyEntityDao dao = GreenUtils.getBabyEntityDao();
        List<BabyEntity> babyEntities = dao.queryBuilder()
                .where(BabyEntityDao.Properties.DeviceId.eq(deviceId), BabyEntityDao.Properties.UserId.eq(userId))
                .list();
        if (!babyEntities.isEmpty()) {
            return babyEntities.get(0).getNotificationChannel();
        }
        return 0;
    }

    public static BabyEntityDao getBabyEntityDao() {
        return App.getInstance().getDaoSession().getBabyEntityDao();
    }

    public static ChatEntityDao getChatEntityDao() {
        return App.getInstance().getDaoSession().getChatEntityDao();
    }

    public static NotifyMessageEntityDao getNotifyMessageEntityDao() {
        return App.getInstance().getDaoSession().getNotifyMessageEntityDao();
    }

    public static SosEntityDao getSosEntityDao() {
        return App.getInstance().getDaoSession().getSosEntityDao();
    }

    public static AlarmClockEntityDao getAlarmClockEntityDao() {
        return App.getInstance().getDaoSession().getAlarmClockEntityDao();
    }

    public static ClassDisableEntityDao getClassDisableEntityDao() {
        return App.getInstance().getDaoSession().getClassDisableEntityDao();
    }

    public static CollectPraiseEntityDao getCollectPraiseEntityDao() {
        return App.getInstance().getDaoSession().getCollectPraiseEntityDao();
    }

    public static LocationEntityDao getLocationEntityDao() {
        return App.getInstance().getDaoSession().getLocationEntityDao();
    }


    public static FenceEntityDao getFenceEntityDao() {
        return App.getInstance().getDaoSession().getFenceEntityDao();
    }

    public static ContactEntityDao getContactEntityDao() {
        return App.getInstance().getDaoSession().getContactEntityDao();
    }

    public static DeviceEntityDao getDeviceEntityDao() {
        return App.getInstance().getDaoSession().getDeviceEntityDao();
    }

    public static UserEntityDao getUserEntityDao() {
        return App.getInstance().getDaoSession().getUserEntityDao();
    }

    public static FamilyChatGroupMemberEntityDao getFamilyChatGroupMemberEntityDao() {
        return App.getInstance().getDaoSession().getFamilyChatGroupMemberEntityDao();
    }

    public static SmsEntityDao getSmsEntityDao() {
        return App.getInstance().getDaoSession().getSmsEntityDao();
    }
}
