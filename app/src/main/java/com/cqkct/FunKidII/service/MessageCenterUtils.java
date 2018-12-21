package com.cqkct.FunKidII.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.Activity.MessageListsActivity;
import com.cqkct.FunKidII.Ui.Activity.NotifyMessageSettingActivity;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.RelationUtils;
import com.cqkct.FunKidII.db.Dao.NotifyMessageEntityDao;
import com.cqkct.FunKidII.db.Entity.NotifyMessageEntity;
import com.cqkct.FunKidII.service.tlc.PreferencesWrapper;
import com.google.protobuf.GeneratedMessageV3;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;
import java.util.Random;

import protocol.Message;

/**
 * Created by T on 2018/3/26.
 */

public class MessageCenterUtils {
    private static final String TAG = MessageCenterUtils.class.getSimpleName();

    /**
     * 绑定/解绑
     */
    public static final int BIND_OR_UNBIND = 0x10000;
    /**
     * 绑定申请
     */
    public static final int ON_REQUEST_BIND = 0x10001; // 绑定申请
    /**
     * 绑定成功通知
     */
    public static final int ON_BIND = 0x10002;  // 绑定
    /**
     * 设备被解绑
     */
    public static final int ON_UNBIND = 0x10003; // 解绑


    /**
     * 用户的关联信息被修改 ignore this
     */
    public static final int ON_USR_DEV_ASSOC_MODIFIED = 0x20000;
    public static final int ON_USR_DEV_ASSOC_MODIFIED_RELATION = 0x20001;
    public static final int ON_USR_DEV_ASSOC_MODIFIED_PERMISSION = 0x20002;


    /**
     * 手表事件
     */
    private static final int incident = 0x30000;
    public static final int INCIDENT_IN_CALL = 0x30001;
    public static final int INCIDENT_OUT_CALL = 0x30002;
    public static final int INCIDENT_SOS = 0x30003;
    public static final int INCIDENT_LOW_BATTERY = 0x30004;
    public static final int INCIDENT_POWER_ON = 0x30005;
    public static final int INCIDENT_POWER_OFF = 0x30006;
    public static final int INCIDENT_OFF_WRIST = 0x30007;
    public static final int INCIDENT_SOAK_WATER = 0x30008;
    public static final int INCIDENT_REPORT_LOSS = 0x30009;
    public static final int INCIDENT_FENCE = 0x3000A;
    public static final int INCIDENT_SCHOOL_GUARD = 0x3000B;


    /**
     * 设备系统信息 ignore this
     */
    public static final int CONF_DEV_SYS_INFO = 0x40000; // 设备系统信息


    /**
     * 修改功能
     */
    public static final int CONF_FUNC = 0x50000; // 变更更多功能
    public static final int CONF_FUNC_LOCATION_MODE = 0x50001; // 变更定位方式
    // 功能设置具体项
    public static final int CONF_FUNC_REFUSE_STRANGER_CALL_IN = 0x60001; // 变更拒接陌生人
    public static final int CONF_FUNC_REFUSE_STRANGER_CALL_OUT = 0x60002; // 变更拒呼陌生人
    public static final int CONF_FUNC_TIMER_POWER_ON_OFF = 0x60003; // 变更定时开关机
    public static final int CONF_FUNC_INUNDATE_REMIND = 0x60004; // 变更泡水提醒
    public static final int CONF_FUNC_SAVE_POWER_MODE = 0x60005; // 变更省电模式
    public static final int CONF_FUNC_CALL_POSITION = 0x60006; // 通话时定位
    public static final int CONF_FUNC_WATCH_SET_LIGHT = 0x60007; // 变更设置手表亮屏时间
    public static final int CONF_FUNC_WATCH_REPORT_LOST = 0x60008; // 变更手表挂失
    public static final int CONF_FUNC_TIMER_POWER_ON = 0x60009; // 变更定时开机
    public static final int CONF_FUNC_TIMER_POWER_OFF = 0x6000A; // 变更定时关机
    public static final int CONF_FUNC_ENABLE_CALCULATOR = 0x6000B; // 变更计算器开关
    public static final int CONF_FUNC_ENABLE_SMS_AGENT = 0x6000C; // 变更短信代理开关 (未使用)


    /**
     * 修改宝贝信息
     */
    public static final int CONF_BABY = 0x70000;   //变更宝贝信息


    /**
     * 设备所属公司信息 ignore this
     */
    public static final int CONF_COMPANY_INFO = 0x80000;


    /**
     * 修改围栏信息
     */
    public static final int CONF_FENCE = 0x90000; // 修改围栏信息


    /**
     * 修改SOS
     */
    public static final int CONF_SOS = 0xA0000; // 变更SOS


    /**
     * 修改通讯录
     */
    public static final int CONF_CONTACTS = 0xB0000; //变更通讯录


    /**
     * 修改变更闹钟
     */
    public static final int CONF_ALARM_CLOCK = 0xC0000;//变更闹钟


    /**
     * 修改上课禁用
     */
    public static final int CONF_CLASS_DISABLE = 0xD0000; // 上课禁用


    /**
     * 上学守护
     */
    public static final int CONF_SCHOOL_GUARD = 0xE0000;


    /**
     * 集赞
     */
    public static final int CONF_COLLECT_PRAISE = 0xF0000;


    private static NotifyMessageEntity cloneNotifyMessageBean(NotifyMessageEntity bean) {
        NotifyMessageEntity newBean = new NotifyMessageEntity();
        newBean.setId(bean.getId());
        newBean.setUserId(bean.getUserId());
        newBean.setDeviceId(bean.getDeviceId());
        newBean.setTime(bean.getTime());
        newBean.setTag(bean.getTag());
        newBean.setData(bean.getData());
        newBean.setOriginator(bean.getOriginator());
        newBean.setOriginator_phone(bean.getOriginator_phone());
        newBean.setOriginator_relation(bean.getOriginator_relation());
        newBean.setDeviceName(bean.getDeviceName());
        newBean.setIsRead(bean.getIsRead());
        newBean.setContentType(bean.getContentType());
        return newBean;
    }

    /**
     * @param udpi           修改者以及设备的相关信息
     * @param notifyProtoBuf 具体的通知信息
     * @param userId         当前用户的userId
     */
    public static void saveNotifyMessage(MainService mainService, Message.FetchUsrDevParticRspMsg udpi, @NonNull GeneratedMessageV3 notifyProtoBuf, String userId, Pkt reqPkt) {
        if (TextUtils.isEmpty(userId)) {
            L.e(TAG, "saveNotifyMessage: userId is empty");
            return;
        }

        Integer tag = Pkt.calcTagByProtoBufMessage(notifyProtoBuf);
        if (tag == null) {
            L.e(TAG, "saveNotifyMessage: calcTagByProtoBufMessage return null");
            return;
        }

        NotifyMessageEntity bean = new NotifyMessageEntity();
        bean.setUserId(userId);
        bean.setTag(tag);
        bean.setData(notifyProtoBuf.toByteArray());
        bean.setIsRead(false);

        // deviceId
        // time
        // originator
        // originator_phone
        // originator_relation
        // deviceName
        // contentType

        switch (tag) {
            case Message.Tag.ON_DEVICE_BIND_VALUE: {
                // 设备绑定成功
                Message.NotifyUserBindDevReqMsg reqMsg = (Message.NotifyUserBindDevReqMsg) notifyProtoBuf;
                Message.UsrDevAssoc usrDevAssoc = reqMsg.getUsrDevAssoc();

                bean.setDeviceId(usrDevAssoc.getDeviceId());
                bean.setTime(reqMsg.getTime() * 1000L);
                bean.setOriginator(reqMsg.getBinder());
                bean.setOriginator_phone(udpi.getUserInfo().getPhone());
                bean.setOriginator_relation(udpi.getUsrDevAssoc().getRelation());
                bean.setDeviceName(udpi.getBaby().getName());
                bean.setContentType(ON_BIND);

                saveNotifyAndNotificationDisplayed(mainService, bean, notifyProtoBuf);
                break;
            }

            case Message.Tag.ON_DEVICE_UNBIND_VALUE: {
                // 设备被解绑
                Message.NotifyUserUnbindDevReqMsg reqMsg = (Message.NotifyUserUnbindDevReqMsg) notifyProtoBuf;
                Message.UsrDevAssoc usrDevAssoc = reqMsg.getUsrDevAssoc();

                bean.setDeviceId(usrDevAssoc.getDeviceId());
                bean.setTime(reqMsg.getTime() * 1000L);
                bean.setOriginator(reqMsg.getUnbinder());
                bean.setOriginator_phone(udpi.getUserInfo().getPhone());
                bean.setOriginator_relation(udpi.getUsrDevAssoc().getRelation());
                bean.setDeviceName(udpi.getBaby().getName());
                bean.setContentType(ON_UNBIND);

                saveNotifyAndNotificationDisplayed(mainService, bean, notifyProtoBuf);
                break;
            }

            case Message.Tag.ON_BIND_DEVICE_REQUEST_VALUE: {
                // 绑定申请
                Message.NotifyAdminBindDevReqMsg reqMsg = (Message.NotifyAdminBindDevReqMsg) notifyProtoBuf;
                Message.UsrDevAssoc usrDevAssoc = reqMsg.getUsrDevAssoc();

                bean.setDeviceId(usrDevAssoc.getDeviceId());
                bean.setTime(reqMsg.getTime() * 1000L);
                bean.setOriginator(usrDevAssoc.getUserId());
                bean.setOriginator_phone(udpi.getUserInfo().getPhone());
                bean.setOriginator_relation(reqMsg.getUsrDevAssoc().getRelation());
                bean.setSeq(reqPkt.seq.toString());
                bean.setDeviceName(udpi.getBaby().getName());
                bean.setContentType(ON_REQUEST_BIND);

                saveNotifyAndNotificationDisplayed(mainService, bean, notifyProtoBuf);
                break;
            }

            case Message.Tag.ON_DEVICE_INCIDENT_VALUE: {
                // 主动触发
                Message.NotifyIncidentReqMsg incidentReqMsg = (Message.NotifyIncidentReqMsg) notifyProtoBuf;

                bean.setDeviceId(incidentReqMsg.getDeviceId());
                bean.setTime(incidentReqMsg.getTime() * 1000L);
                bean.setOriginator("");
                bean.setOriginator_phone("");
                bean.setOriginator_relation("");
                bean.setDeviceName(udpi.getBaby().getName());

                long IncidFlag = incidentReqMsg.getIncident().getFlag();
                if ((IncidFlag & Message.Incident.IncidentFlag.IN_CALL_VALUE) != 0) {
                    NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                    copyBean.setContentType(INCIDENT_IN_CALL);
                    saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                }
                if ((IncidFlag & Message.Incident.IncidentFlag.OUT_CALL_VALUE) != 0) {
                    NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                    copyBean.setContentType(INCIDENT_OUT_CALL);
                    saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                }
                if ((IncidFlag & Message.Incident.IncidentFlag.SOS_VALUE) != 0) {
                    NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                    copyBean.setContentType(INCIDENT_SOS);
                    saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                }
                if ((IncidFlag & Message.Incident.IncidentFlag.LOW_BATTERY_VALUE) != 0) {
                    NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                    copyBean.setContentType(INCIDENT_LOW_BATTERY);
                    saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                }
                if ((IncidFlag & Message.Incident.IncidentFlag.POWER_ON_VALUE) != 0) {
                    NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                    copyBean.setContentType(INCIDENT_POWER_ON);
                    saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                }
                if ((IncidFlag & Message.Incident.IncidentFlag.POWER_OFF_VALUE) != 0) {
                    NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                    copyBean.setContentType(INCIDENT_POWER_OFF);
                    saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                }
                if ((IncidFlag & Message.Incident.IncidentFlag.OFF_WRIST_VALUE) != 0) {
                    NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                    copyBean.setContentType(INCIDENT_OFF_WRIST);
                    saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                }
                if ((IncidFlag & Message.Incident.IncidentFlag.SOAK_WATER_VALUE) != 0) {
                    NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                    copyBean.setContentType(INCIDENT_SOAK_WATER);
                    saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                }
                if ((IncidFlag & Message.Incident.IncidentFlag.REPORT_LOSS_VALUE) != 0) {
                    NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                    copyBean.setContentType(INCIDENT_REPORT_LOSS);
                    saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                }
                if ((IncidFlag & Message.Incident.IncidentFlag.FENCE_VALUE) != 0) {
                    NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                    copyBean.setContentType(INCIDENT_FENCE);
                    saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                }
                if ((IncidFlag & Message.Incident.IncidentFlag.SCHOOL_GUARD_VALUE) != 0) {
                    NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                    copyBean.setContentType(INCIDENT_SCHOOL_GUARD);
                    saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                }
                break;
            }

            case Message.Tag.ON_DEV_CONF_CHANGED_VALUE: {
                Message.NotifyDevConfChangedReqMsg confChangedReqMsg = (Message.NotifyDevConfChangedReqMsg) notifyProtoBuf;

                bean.setDeviceId(confChangedReqMsg.getDeviceId());
                bean.setTime(confChangedReqMsg.getTime() * 1000L);
                bean.setOriginator(confChangedReqMsg.getChanger());
                bean.setOriginator_phone(udpi.getUserInfo().getPhone());
                bean.setOriginator_relation(udpi.getUsrDevAssoc().getRelation());
                bean.setDeviceName(udpi.getBaby().getName());

                long confFlag = confChangedReqMsg.getFlag();
                if ((confFlag & Message.DevConfFlag.DCF_BABY_VALUE) != 0) {
                    NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                    copyBean.setContentType(CONF_BABY);
                    saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                }

                if ((confFlag & Message.DevConfFlag.DCF_FUNCTIONS_VALUE) != 0) {
                    Message.DevConf conf = confChangedReqMsg.getConf();
                    Message.Functions func = conf.getFuncs();
                    long changeField = func.getChangedField();
                    if ((changeField & Message.Functions.FieldFlag.REJECT_STRANGER_CALL_IN_VALUE) != 0) {
                        NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                        copyBean.setContentType(CONF_FUNC_REFUSE_STRANGER_CALL_IN);
                        saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                    }
                    if ((changeField & Message.Functions.FieldFlag.REJECT_STRANGER_CALL_OUT_VALUE) != 0) {
                        NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                        copyBean.setContentType(CONF_FUNC_REFUSE_STRANGER_CALL_OUT);
                        saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                    }
                    if ((changeField & Message.Functions.FieldFlag.INUNDATE_REMIND_VALUE) != 0) {
                        NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                        copyBean.setContentType(CONF_FUNC_INUNDATE_REMIND);
                        saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                    }
                    if ((changeField & Message.Functions.FieldFlag.SAVE_POWER_MODE_VALUE) != 0) {
                        NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                        copyBean.setContentType(CONF_FUNC_SAVE_POWER_MODE);
                        saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                    }
                    if ((changeField & Message.Functions.FieldFlag.CALL_POSITION_VALUE) != 0) {
                        NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                        copyBean.setContentType(CONF_FUNC_CALL_POSITION);
                        saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                    }
                    if ((changeField & Message.Functions.FieldFlag.WATCH_SET_LIGHT_VALUE) != 0) {
                        NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                        copyBean.setContentType(CONF_FUNC_WATCH_SET_LIGHT);
                        saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                    }
//                    if ((changeField & Message.Functions.FieldFlag.FUNC_CHANG_WATCH_REPORT_LOST_VALUE) != 0) { TODO: 手表挂失！！！！
//                        NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
//                        copyBean.setContentType(GreenUtils.CONF_FUNC_WATCH_REPORT_LOST);
//                        saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
//                    }
                    if (true) {
                        if ((changeField & Message.Functions.FieldFlag.TIMER_POWER_ON_OFF_VALUE) != 0) {
                            NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                            copyBean.setContentType(CONF_FUNC_TIMER_POWER_ON_OFF);
                            saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                        }
                        if ((changeField & Message.Functions.FieldFlag.TIMER_POWER_ON_VALUE) != 0) {
                            NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                            copyBean.setContentType(CONF_FUNC_TIMER_POWER_ON);
                            saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                        }
                        if ((changeField & Message.Functions.FieldFlag.TIMER_POWER_OFF_VALUE) != 0) {
                            NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                            copyBean.setContentType(CONF_FUNC_TIMER_POWER_OFF);
                            saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                        }
                    } else {
                        if ((changeField & (Message.Functions.FieldFlag.TIMER_POWER_ON_OFF_VALUE | Message.Functions.FieldFlag.TIMER_POWER_ON_VALUE | Message.Functions.FieldFlag.TIMER_POWER_OFF_VALUE)) != 0) {
                            NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                            copyBean.setContentType(CONF_FUNC_TIMER_POWER_ON_OFF);
                            saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                        }
                    }
                    if ((changeField & Message.Functions.FieldFlag.ENABLE_CALCULATOR_VALUE) != 0) {
                        NotifyMessageEntity copyBean = cloneNotifyMessageBean(bean);
                        copyBean.setContentType(CONF_FUNC_ENABLE_CALCULATOR);
                        saveNotifyAndNotificationDisplayed(mainService, copyBean, notifyProtoBuf);
                    }
                }
                break;
            }

            case Message.Tag.ON_DEV_FENCE_CHANGED_VALUE: {
                // 修改围栏信息
                Message.NotifyFenceChangedReqMsg fencesChangedReqMsg = (Message.NotifyFenceChangedReqMsg) notifyProtoBuf;

                bean.setDeviceId(fencesChangedReqMsg.getDeviceId());
                bean.setTime(fencesChangedReqMsg.getTime() * 1000L);
                bean.setOriginator(udpi.getUserInfo().getUserId());
                bean.setOriginator_phone(udpi.getUserInfo().getPhone());
                bean.setOriginator_relation(udpi.getUsrDevAssoc().getRelation());
                bean.setDeviceName(udpi.getBaby().getName());
                bean.setContentType(CONF_FENCE);

                saveNotifyAndNotificationDisplayed(mainService, bean, notifyProtoBuf);
                break;
            }

            case Message.Tag.ON_DEV_SOS_CHANGED_VALUE: {
                Message.NotifySosChangedReqMsg sosChangedReqMsg = (Message.NotifySosChangedReqMsg) notifyProtoBuf;

                bean.setDeviceId(sosChangedReqMsg.getDeviceId());
                bean.setTime(sosChangedReqMsg.getTime() * 1000L);
                bean.setOriginator(udpi.getUserInfo().getUserId());
                bean.setOriginator_phone(udpi.getUserInfo().getPhone());
                bean.setOriginator_relation(udpi.getUsrDevAssoc().getRelation());
                bean.setDeviceName(udpi.getBaby().getName());
                bean.setContentType(CONF_SOS);

                saveNotifyAndNotificationDisplayed(mainService, bean, notifyProtoBuf);
                break;
            }

            case Message.Tag.ON_DEV_CONTACT_CHANGED_VALUE: {
                Message.NotifyContactChangedReqMsg contactChangedReqMsg = (Message.NotifyContactChangedReqMsg) notifyProtoBuf;

                bean.setDeviceId(contactChangedReqMsg.getDeviceId());
                bean.setTime(contactChangedReqMsg.getTime() * 1000L);
                bean.setOriginator(udpi.getUserInfo().getUserId());
                bean.setOriginator_phone(udpi.getUserInfo().getPhone());
                bean.setOriginator_relation(udpi.getUsrDevAssoc().getRelation());
                bean.setDeviceName(udpi.getBaby().getName());
                bean.setContentType(CONF_CONTACTS);

                saveNotifyAndNotificationDisplayed(mainService, bean, notifyProtoBuf);
                break;
            }

            case Message.Tag.ON_DEV_ALARM_CLOCK_CHANGED_VALUE: {
                Message.NotifyAlarmClockChangedReqMsg alarmClockChangedReqMsg = (Message.NotifyAlarmClockChangedReqMsg) notifyProtoBuf;

                bean.setDeviceId(alarmClockChangedReqMsg.getDeviceId());
                bean.setTime(alarmClockChangedReqMsg.getTime() * 1000L);
                bean.setOriginator(udpi.getUserInfo().getUserId());
                bean.setOriginator_phone(udpi.getUserInfo().getPhone());
                bean.setOriginator_relation(udpi.getUsrDevAssoc().getRelation());
                bean.setDeviceName(udpi.getBaby().getName());
                bean.setContentType(CONF_ALARM_CLOCK);

                saveNotifyAndNotificationDisplayed(mainService, bean, notifyProtoBuf);
                break;
            }

            case Message.Tag.ON_DEV_CLASS_DISABLE_CHANGED_VALUE: {
                Message.NotifyClassDisableChangedReqMsg classDisableChangedReqMsg = (Message.NotifyClassDisableChangedReqMsg) notifyProtoBuf;

                bean.setDeviceId(classDisableChangedReqMsg.getDeviceId());
                bean.setTime(classDisableChangedReqMsg.getTime() * 1000L);
                bean.setOriginator(udpi.getUserInfo().getUserId());
                bean.setOriginator_phone(udpi.getUserInfo().getPhone());
                bean.setOriginator_relation(udpi.getUsrDevAssoc().getRelation());
                bean.setDeviceName(udpi.getBaby().getName());

                bean.setContentType(CONF_CLASS_DISABLE);
                saveNotifyAndNotificationDisplayed(mainService, bean, notifyProtoBuf);
                break;
            }

            case Message.Tag.ON_SCHOOL_GUARD_CHANGED_VALUE: {
                Message.NotifySchoolGuardChangedReqMsg schoolGuardChangedReqMsg = (Message.NotifySchoolGuardChangedReqMsg) notifyProtoBuf;

                bean.setDeviceId(schoolGuardChangedReqMsg.getDeviceId());
                bean.setTime(schoolGuardChangedReqMsg.getTime() * 1000L);
                bean.setOriginator(udpi.getUserInfo().getUserId());
                bean.setOriginator_phone(udpi.getUserInfo().getPhone());
                bean.setOriginator_relation(udpi.getUsrDevAssoc().getRelation());
                bean.setDeviceName(udpi.getBaby().getName());

                bean.setContentType(CONF_SCHOOL_GUARD);
                saveNotifyAndNotificationDisplayed(mainService, bean, notifyProtoBuf);
                break;
            }

            case Message.Tag.ON_PRAISE_CHANGED_VALUE: {
                Message.NotifyPraiseChangedReqMsg praiseChangedReqMsg = (Message.NotifyPraiseChangedReqMsg) notifyProtoBuf;

                bean.setDeviceId(praiseChangedReqMsg.getDeviceId());
                bean.setTime(praiseChangedReqMsg.getTime() * 1000L);
                bean.setOriginator(udpi.getUserInfo().getUserId());
                bean.setOriginator_phone(udpi.getUserInfo().getPhone());
                bean.setOriginator_relation(udpi.getUsrDevAssoc().getRelation());
                bean.setDeviceName(udpi.getBaby().getName());

                bean.setContentType(CONF_COLLECT_PRAISE);
                saveNotifyAndNotificationDisplayed(mainService, bean, notifyProtoBuf);
                break;
            }
            case Message.Tag.ON_LOCATION_MODE_CHANGED_VALUE: {
                Message.NotifyLocationModeChangedReqMsg notifyLocationModeChangedReqMsg = (Message.NotifyLocationModeChangedReqMsg) notifyProtoBuf;

                bean.setDeviceId(notifyLocationModeChangedReqMsg.getDeviceId());
                bean.setTime(notifyLocationModeChangedReqMsg.getTime() * 1000L);
                bean.setOriginator(udpi.getUserInfo().getUserId());
                bean.setOriginator_phone(udpi.getUserInfo().getPhone());
                bean.setOriginator_relation(udpi.getUsrDevAssoc().getRelation());
                bean.setDeviceName(udpi.getBaby().getName());

                bean.setContentType(CONF_FUNC_LOCATION_MODE);
                saveNotifyAndNotificationDisplayed(mainService, bean, notifyProtoBuf);
                break;
            }

            default:
                break;
        }
    }

    //send NotificationManager 发送通知栏
    private static void saveNotifyAndNotificationDisplayed(MainService mainService, NotifyMessageEntity messageBean, GeneratedMessageV3 notifyProtoBuf) {
        if (messageBean == null || messageBean.getContentType() == 0)
            return;
        int contentType = messageBean.getContentType();
        if (contentType == 0)
            return;


        String msg = getNotifyMessageBeanMsgContent(mainService, messageBean, notifyProtoBuf);
        if (TextUtils.isEmpty(msg))
            return;

        NotifyMessageEntityDao messageEntityDao = GreenUtils.getNotifyMessageEntityDao();
        messageEntityDao.insert(messageBean);

        EventBus.getDefault().postSticky(new Event.HasNewMessageOfMessageCenter(messageBean.getDeviceId()));

        if (contentType == ON_REQUEST_BIND) {
            // 通知已经在收到消息的地方弹了
            return;
        }

        if (!shouldPopNotification(messageBean.getDeviceId(), contentType)) {
            return;
        }

        int titleStrResId = -1;
        switch (contentType & 0xFFFF0000) {
            case CONF_BABY:
                titleStrResId = R.string.notify_title_modify_baby_info;
                break;
            case CONF_FUNC:
                titleStrResId = R.string.notify_title_modify_dev_functions;
                break;

            case CONF_FENCE:
                titleStrResId = R.string.notify_title_modify_fences;
                break;
            case CONF_SOS:
                titleStrResId = R.string.notify_title_modify_sos;
                break;
            case CONF_CONTACTS:
                titleStrResId = R.string.notify_title_modify_contacts;
                break;
            case CONF_ALARM_CLOCK:
                titleStrResId = R.string.notify_title_modify_alarm_clock;
                break;
            case CONF_CLASS_DISABLE:
                titleStrResId = R.string.notify_title_modify_class_disable;
                break;
            case CONF_SCHOOL_GUARD:
                titleStrResId = R.string.notify_title_modify_scholl_guard;
                break;
            case CONF_COLLECT_PRAISE:
                titleStrResId = R.string.notify_title_modify_collect_praise;
                break;
            default:
                break;
        }
        switch (contentType) {
            case ON_BIND:
                titleStrResId = R.string.notify_title_bind_success;
                break;
            case ON_UNBIND:
                titleStrResId = R.string.notify_title_unbound;
                break;
            case INCIDENT_IN_CALL:
                titleStrResId = R.string.notify_title_dev_in_call;
                break;
            case INCIDENT_OUT_CALL:
                titleStrResId = R.string.notify_title_dev_out_call;
                break;
            case INCIDENT_SOS:
                titleStrResId = R.string.notify_title_dev_incid_sos;
                break;
            case INCIDENT_LOW_BATTERY:
                titleStrResId = R.string.notify_title_dev_incid_low_battery;
                break;
            case INCIDENT_POWER_ON:
                titleStrResId = R.string.notify_title_dev_incid_power_on;
                break;
            case INCIDENT_POWER_OFF:
                titleStrResId = R.string.notify_title_dev_incid_power_off;
                break;
            case INCIDENT_OFF_WRIST:
                titleStrResId = R.string.notify_title_dev_incid_off_wrist;
                break;
            case INCIDENT_SOAK_WATER:
                titleStrResId = R.string.notify_title_dev_incid_soak_water;
                break;
            case INCIDENT_REPORT_LOSS:
                titleStrResId = R.string.notify_title_dev_incid_report_loss;
                break;
            case INCIDENT_FENCE:
                titleStrResId = R.string.notify_title_dev_incid_fence;
                break;
            case INCIDENT_SCHOOL_GUARD:
                titleStrResId = R.string.notify_title_dev_incid_school_guard;
                break;

            default:
                break;
        }
        if (titleStrResId == -1) {
            titleStrResId = R.string.app_name;
        }
        PreferencesWrapper preferencesWrapper = PreferencesWrapper.getInstance(mainService);
        int notifyStatus = preferencesWrapper.getNotificationConf();

        if (BaseActivity.isAppRunOnForeground()) {
            if ((notifyStatus & Notification.DEFAULT_SOUND) != 0) {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone rt = RingtoneManager.getRingtone(mainService, uri);
                rt.play();
            }
            if ((notifyStatus & Notification.DEFAULT_VIBRATE) != 0) {
                Vibrator vibrator = (Vibrator) mainService.getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {100, 200, 0, 0}; // 停止 开启 停止 开启
                if (vibrator != null) {
                    vibrator.vibrate(pattern, -1); //重复两次上面的pattern 如果只想震动一次，index设为-1
                }
            }
        } else {

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(mainService, MainService.CHANNEL_ID_MSG_CENTER)
                            .setSmallIcon(R.drawable.app_icon)
                            .setContentTitle(mainService.getString(titleStrResId))
                            .setDefaults(notifyStatus) //使用默认的声音、振动、闪光
                            .setContentText(msg);
            // Creates an explicit intent for an Activity in your app
            Intent intent = new Intent(mainService, MessageListsActivity.class);
            mBuilder.setContentIntent(PendingIntent.getActivity(mainService.getApplicationContext(), new Random().nextInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT));
            mBuilder.setAutoCancel(true);
            mainService.mNotificationManagerCompat.notify(mainService.getNotificationID(), mBuilder.build());
        }

    }

    private static boolean shouldPopNotification(String deviceId, int contentType) {
        int notiChannel = DeviceInfo.getNotificationChannel(deviceId);
        switch (contentType) {
            case INCIDENT_FENCE:
            case INCIDENT_SCHOOL_GUARD:
                // 这两个不能关闭通知
                break;
            case MessageCenterUtils.ON_REQUEST_BIND:
                break;

            case MessageCenterUtils.BIND_OR_UNBIND:
            case MessageCenterUtils.ON_BIND:
            case MessageCenterUtils.ON_UNBIND:
            case MessageCenterUtils.ON_USR_DEV_ASSOC_MODIFIED:
            case MessageCenterUtils.ON_USR_DEV_ASSOC_MODIFIED_RELATION:
            case MessageCenterUtils.ON_USR_DEV_ASSOC_MODIFIED_PERMISSION:
                if (NotifyMessageSettingActivity.isNotificationChannelEnabled(notiChannel, Message.NotificationChannel.NC_CONTACTS_VALUE)) {
                    return true;
                }
                break;

            case MessageCenterUtils.INCIDENT_IN_CALL:
            case MessageCenterUtils.INCIDENT_OUT_CALL:
                if (NotifyMessageSettingActivity.isNotificationChannelEnabled(notiChannel, Message.NotificationChannel.NC_DEVICE_CALL_VALUE)) {
                    return true;
                }
                break;
            case MessageCenterUtils.INCIDENT_SOS:
                if (NotifyMessageSettingActivity.isNotificationChannelEnabled(notiChannel, Message.NotificationChannel.NC_DEVICE_SOS_VALUE)) {
                    return true;
                }
                break;
            case MessageCenterUtils.INCIDENT_LOW_BATTERY:
            case MessageCenterUtils.INCIDENT_POWER_ON:
            case MessageCenterUtils.INCIDENT_POWER_OFF:
            case MessageCenterUtils.INCIDENT_OFF_WRIST:
            case MessageCenterUtils.INCIDENT_SOAK_WATER:
            case MessageCenterUtils.INCIDENT_REPORT_LOSS:
                if (NotifyMessageSettingActivity.isNotificationChannelEnabled(notiChannel, Message.NotificationChannel.NC_DEVICE_INCIDENT_VALUE)) {
                    return true;
                }
                break;

            case MessageCenterUtils.CONF_DEV_SYS_INFO:

            case MessageCenterUtils.CONF_FUNC:
            case MessageCenterUtils.CONF_FUNC_LOCATION_MODE:
            case MessageCenterUtils.CONF_FUNC_REFUSE_STRANGER_CALL_IN:
            case MessageCenterUtils.CONF_FUNC_REFUSE_STRANGER_CALL_OUT:
            case MessageCenterUtils.CONF_FUNC_TIMER_POWER_ON_OFF:
            case MessageCenterUtils.CONF_FUNC_INUNDATE_REMIND:
            case MessageCenterUtils.CONF_FUNC_SAVE_POWER_MODE:
            case MessageCenterUtils.CONF_FUNC_CALL_POSITION:
            case MessageCenterUtils.CONF_FUNC_WATCH_SET_LIGHT:
            case MessageCenterUtils.CONF_FUNC_WATCH_REPORT_LOST:
            case MessageCenterUtils.CONF_FUNC_TIMER_POWER_ON:
            case MessageCenterUtils.CONF_FUNC_TIMER_POWER_OFF:
            case MessageCenterUtils.CONF_FUNC_ENABLE_CALCULATOR:
            case MessageCenterUtils.CONF_FUNC_ENABLE_SMS_AGENT:

            case MessageCenterUtils.CONF_BABY:

            case MessageCenterUtils.CONF_COMPANY_INFO:

            case MessageCenterUtils.CONF_FENCE:
                if (NotifyMessageSettingActivity.isNotificationChannelEnabled(notiChannel, Message.NotificationChannel.NC_SETTINGS_VALUE)) {
                    return true;
                }
                break;

            case MessageCenterUtils.CONF_SOS:
                if (NotifyMessageSettingActivity.isNotificationChannelEnabled(notiChannel, Message.NotificationChannel.NC_DEVICE_SOS_VALUE)) {
                    return true;
                }
                break;

            case MessageCenterUtils.CONF_CONTACTS:
                if (NotifyMessageSettingActivity.isNotificationChannelEnabled(notiChannel, Message.NotificationChannel.NC_CONTACTS_VALUE)) {
                    return true;
                }
                break;

            case MessageCenterUtils.CONF_ALARM_CLOCK:
                if (NotifyMessageSettingActivity.isNotificationChannelEnabled(notiChannel, Message.NotificationChannel.NC_SETTINGS_VALUE)) {
                    return true;
                }
                break;

            case MessageCenterUtils.CONF_CLASS_DISABLE:
                if (NotifyMessageSettingActivity.isNotificationChannelEnabled(notiChannel, Message.NotificationChannel.NC_SETTINGS_VALUE)) {
                    return true;
                }
                break;

            case MessageCenterUtils.CONF_SCHOOL_GUARD:
                if (NotifyMessageSettingActivity.isNotificationChannelEnabled(notiChannel, Message.NotificationChannel.NC_SETTINGS_VALUE)) {
                    return true;
                }
                break;

            case MessageCenterUtils.CONF_COLLECT_PRAISE:
                if (NotifyMessageSettingActivity.isNotificationChannelEnabled(notiChannel, Message.NotificationChannel.NC_PRAISE_COLLECTION_VALUE)) {
                    return true;
                }
                break;
            default:
                break;
        }

        return false;
    }

    public static String getNotifyMessageBeanMsgContent(@NonNull Context context, @NonNull NotifyMessageEntity notifyMessageEntity, @Nullable GeneratedMessageV3 notifyProtoBuf) {
        if (notifyProtoBuf == null) {
            try {
                switch (notifyMessageEntity.getTag()) {
                    case Message.Tag.ON_BIND_DEVICE_REQUEST_VALUE:
                        notifyProtoBuf = Message.NotifyAdminBindDevReqMsg.parseFrom(notifyMessageEntity.getData());
                        break;
                    case Message.Tag.ON_DEVICE_BIND_VALUE:
                        notifyProtoBuf = Message.NotifyUserBindDevReqMsg.parseFrom(notifyMessageEntity.getData());
                        break;
                    case Message.Tag.ON_DEVICE_UNBIND_VALUE:
                        notifyProtoBuf = Message.NotifyUserUnbindDevReqMsg.parseFrom(notifyMessageEntity.getData());
                        break;
                    case Message.Tag.ON_DEVICE_INCIDENT_VALUE:
                        notifyProtoBuf = Message.NotifyIncidentReqMsg.parseFrom(notifyMessageEntity.getData());
                        break;
                    case Message.Tag.ON_DEV_CONF_CHANGED_VALUE:
                        notifyProtoBuf = Message.NotifyDevConfChangedReqMsg.parseFrom(notifyMessageEntity.getData());
                        break;
                    case Message.Tag.ON_DEV_FENCE_CHANGED_VALUE:
                        notifyProtoBuf = Message.NotifyFenceChangedReqMsg.parseFrom(notifyMessageEntity.getData());
                        break;
                    case Message.Tag.ON_DEV_SOS_CHANGED_VALUE:
                        notifyProtoBuf = Message.NotifySosChangedReqMsg.parseFrom(notifyMessageEntity.getData());
                        break;
                    case Message.Tag.ON_DEV_CONTACT_CHANGED_VALUE:
                        notifyProtoBuf = Message.NotifyContactChangedReqMsg.parseFrom(notifyMessageEntity.getData());
                        break;
                    case Message.Tag.ON_DEV_ALARM_CLOCK_CHANGED_VALUE:
                        notifyProtoBuf = Message.NotifyAlarmClockChangedReqMsg.parseFrom(notifyMessageEntity.getData());
                        break;
                    case Message.Tag.ON_DEV_CLASS_DISABLE_CHANGED_VALUE:
                        notifyProtoBuf = Message.NotifyClassDisableChangedReqMsg.parseFrom(notifyMessageEntity.getData());
                        break;
                    case Message.Tag.ON_SCHOOL_GUARD_CHANGED_VALUE:
                        notifyProtoBuf = Message.NotifySchoolGuardChangedReqMsg.parseFrom(notifyMessageEntity.getData());
                        break;
                    case Message.Tag.ON_PRAISE_CHANGED_VALUE:
                        notifyProtoBuf = Message.NotifyPraiseChangedReqMsg.parseFrom(notifyMessageEntity.getData());
                        break;
                    case Message.Tag.ON_LOCATION_MODE_CHANGED_VALUE:
                        notifyProtoBuf = Message.NotifyLocationModeChangedReqMsg.parseFrom(notifyMessageEntity.getData());
                        break;
                    default:
                        L.w(TAG, "getNotifyMessageBeanMsgContent: cannot process NotifyMessageEntity, unsupported the Message.Tag: " + notifyMessageEntity.getTag());
                        return null;
                }
            } catch (Exception e) {
                L.e(TAG, "getNotifyMessageBeanMsgContent notifyProtoBuf parseFrom NotifyMessageEntity.getData() to specific protoBuf message failure", e);
                return null;
            }
        }

        String devName = notifyMessageEntity.getDeviceName();
        if (TextUtils.isEmpty(devName)) {
            devName = context.getString(R.string.baby);
        }

        StringBuilder sb = new StringBuilder();

        try {
            switch (notifyMessageEntity.getContentType()) {
                case ON_REQUEST_BIND: // 绑定申请
                    getOnRequestBindMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case ON_BIND: // 绑定
                    getOnBindMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case ON_UNBIND: // 解绑
                    getOnUnbindMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;

                case INCIDENT_IN_CALL: // 打入电话
                    getIncidentInCallMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case INCIDENT_OUT_CALL: // 打出电话
                    getIncidentOutCallMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case INCIDENT_SOS: // 触发 SOS
                    getIncidentSosMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case INCIDENT_LOW_BATTERY: // 电量低
                    getIncidentLowBatteryMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case INCIDENT_POWER_ON: // 开机
                    getIncidentPowerOnMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case INCIDENT_POWER_OFF: // 关机
                    getIncidentPowerOffMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case INCIDENT_OFF_WRIST: // 脱腕
                    getIncidentOffWristMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case INCIDENT_SOAK_WATER: // 泡水
                    getIncidentSoakWaterMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case INCIDENT_REPORT_LOSS: // 挂失
                    // TODO: 挂失
                    break;
                case INCIDENT_FENCE: // 围栏
                    getIncidentFenceMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case INCIDENT_SCHOOL_GUARD: // 上学守护
                    getIncidentSchoolGuardMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;

                case CONF_FUNC_REFUSE_STRANGER_CALL_IN: // 变更拒接陌生人
                    getConfFuncRefuseStrangerCallInMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case CONF_FUNC_REFUSE_STRANGER_CALL_OUT: // 变更拒呼陌生人
                    getConfFuncRefuseStrangerCallOutMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case CONF_FUNC_TIMER_POWER_ON_OFF: // 开启/关闭定时开关机
                    getConfFuncTimerPowerOnOffMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case CONF_FUNC_INUNDATE_REMIND: // 变更泡水提醒
                    getConfFuncInundateRemindMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case CONF_FUNC_SAVE_POWER_MODE: // 变更省电模式
                    getConfFuncSavePowerModeMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case CONF_FUNC_CALL_POSITION: // 通话时定位
                    getConfFuncCallPositionMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case CONF_FUNC_WATCH_SET_LIGHT: //变更设置手表亮屏时间
                    getConfFuncWatchSetLightMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case CONF_FUNC_WATCH_REPORT_LOST: // 变更手表挂失
                    // TODO: 变更手表挂失
                    break;
                case CONF_FUNC_TIMER_POWER_ON: // 变更定时开机
                    getConfFuncTimerPowerOnMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case CONF_FUNC_TIMER_POWER_OFF: // 变更定时关机
                    getConfFuncTimerPowerOffMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case CONF_FUNC_ENABLE_CALCULATOR: // 变更计算器开关
                    getConfFuncEnableCalculatorMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;

                case CONF_BABY: //变更宝贝信息
                    getConfBabyMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;

                case CONF_FENCE: // 修改围栏信息
                    getConfFenceMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;

                case CONF_SOS: // 变更SOS
                    getConfSosMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;

                case CONF_CONTACTS: // 变更通讯录
                    getConfContactsMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;

                case CONF_ALARM_CLOCK: // 变更闹钟
                    getConfAlarmClockMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;

                case CONF_CLASS_DISABLE: // 上课禁用
                    getConfClassDisableMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;

                case CONF_SCHOOL_GUARD: // 上学守护
                    getConfSchoolGuardMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;

                case CONF_COLLECT_PRAISE: // 集赞
                    getConfCollectPraiseMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                case CONF_FUNC_LOCATION_MODE: //定位模式
                    getConfLocationModeMsg(context, sb, devName, notifyMessageEntity, notifyProtoBuf);
                    break;
                default:
                    L.w(TAG, "getNotifyMessageBeanMsgContent cannot process content type: " + notifyMessageEntity.getContentType());
                    return "";
            }
        } catch (Exception e) {
            L.e(TAG, "getNotifyMessageBeanMsgContent Exception", e);
            return null;
        }

        return sb.toString();
    }

    private static void getOnRequestBindMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        if (TextUtils.isEmpty(msgEntity.getOriginator())) {
            return;
        }
        Message.NotifyAdminBindDevReqMsg reqMsg = (Message.NotifyAdminBindDevReqMsg) pb;
        Message.UsrDevAssoc usrDevAssoc = reqMsg.getUsrDevAssoc();
        sb.append(context.getString(R.string.notify_who_request_bind_which_device,
                RelationUtils.decodeRelation(context, usrDevAssoc.getRelation()),
                devName));
    }

    private static void getOnBindMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        if (TextUtils.isEmpty(msgEntity.getOriginator())) {
            return;
        }
        Message.NotifyUserBindDevReqMsg reqMsg = (Message.NotifyUserBindDevReqMsg) pb;
        if (reqMsg.getUsrDevAssoc().getUserId().equals(msgEntity.getUserId())) {
            sb.append(context.getString(R.string.notify_who_agree_you_bind_which_device,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    devName));
        } else {
            sb.append(context.getString(R.string.notify_who_bind_which_device,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    devName));
        }
    }

    private static void getOnUnbindMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyUserUnbindDevReqMsg reqMsg = (Message.NotifyUserUnbindDevReqMsg) pb;
        Message.UsrDevAssoc usrDevAssoc = reqMsg.getUsrDevAssoc();
        if (TextUtils.isEmpty(msgEntity.getOriginator())) {
            // 管理台解绑了什么设备
            if (usrDevAssoc.getUserId().equals(msgEntity.getUserId())) {
                // 管理台解绑了我的yyy手表
                sb.append(context.getString(R.string.notify_which_device_unbind,
                        devName));
            }
            return;
        }
        if (msgEntity.getOriginator().equals(usrDevAssoc.getDeviceId())) {
            // 设备自行解绑了
            sb.append(context.getString(R.string.notify_which_device_unbind,
                    devName));
            return;
        }
        if (msgEntity.getOriginator().equals(usrDevAssoc.getUserId())) {
            // 自己解绑的设备被自己解绑了
            return;
        }
        // xxx解绑yyy设备
        if (usrDevAssoc.getUserId().equals(msgEntity.getUserId())) {
            // yyy手表被xxx解绑
            sb.append(context.getString(R.string.notify_which_device_unbind_by_who,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    devName));
        } else {
            // xxx解绑了xxx
            sb.append(context.getString(R.string.notify_who_unbind_which_device,
                    RelationUtils.decodeRelation(context, usrDevAssoc.getRelation()),
                    devName));
        }
    }

    private static void getIncidentInCallMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyIncidentReqMsg reqMsg = (Message.NotifyIncidentReqMsg) pb;
        String phone = reqMsg.getIncident().getPhoneNum();
        sb.append(context.getString(R.string.notify_who_trigger_call_in_event, devName));
        if (!TextUtils.isEmpty(phone)) {
            String str = phone.trim();
            if (!str.isEmpty()) {
                sb.append(": ");
                sb.append(str);
            }
        }
    }

    private static void getIncidentOutCallMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyIncidentReqMsg reqMsg = (Message.NotifyIncidentReqMsg) pb;
        String phone = reqMsg.getIncident().getPhoneNum();
        sb.append(context.getString(R.string.notify_who_trigger_call_out_event, devName));
        if (!TextUtils.isEmpty(phone)) {
            String str = phone.trim();
            if (!str.isEmpty()) {
                sb.append(": ");
                sb.append(str);
            }
        }
    }

    private static void getIncidentSosMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        sb.append(context.getString(R.string.notify_who_trigger_sos_event, devName));
    }

    private static void getIncidentLowBatteryMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        sb.append(context.getString(R.string.notify_who_trigger_low_battery_event, devName));
    }

    private static void getIncidentPowerOnMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        sb.append(context.getString(R.string.notify_who_trigger_power_on_event, devName));
    }

    private static void getIncidentPowerOffMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        sb.append(context.getString(R.string.notify_who_trigger_power_off_event, devName));
    }


    private static void getIncidentOffWristMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        sb.append(context.getString(R.string.notify_who_trigger_off_wrist_event, devName));
    }

    private static void getIncidentSoakWaterMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        sb.append(context.getString(R.string.notify_who_trigger_soak_water_event, devName));
    }

    private static void getIncidentFenceMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyIncidentReqMsg reqMsg = (Message.NotifyIncidentReqMsg) pb;

        String action = "";
        StringBuilder fenceStr = new StringBuilder();
        String sep = "";
        for (Message.Fence.Event ev : reqMsg.getIncident().getFenceEvList()) {
            switch (ev.getTrigger()) {
                case Message.Fence.CondFlag.LEAVE_VALUE:
                    action = context.getString(R.string.notify_message_out);
                    break;
                case Message.Fence.CondFlag.ENTER_VALUE:
                    action = context.getString(R.string.notify_message_in);
                    break;
                default:
                    L.w(TAG, "getIncidentFenceMsg INCIDENT_FENCE: invalid fence trigger event: " + ev.getTrigger());
                    sb.setLength(0);
                    return;
            }
            fenceStr.append(sep)
                    .append(ev.getFence().getName());
            sep = ", ";
        }
        if (fenceStr.length() < 1) {
            L.w(TAG, "getIncidentFenceMsg INCIDENT_FENCE: no fence info");
            sb.setLength(0);
            return;
        }
        sb.append(context.getString(R.string.notify_who_trigger_what_fence_event, devName, action, fenceStr.toString()));
    }

    private static void getIncidentSchoolGuardMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyIncidentReqMsg reqMsg = (Message.NotifyIncidentReqMsg) pb;

        String triggerEv;
        Message.SchoolGuard.Event ev = reqMsg.getIncident().getSchoolGuardEv();
        switch (ev.getCode()) {
            case ENTER_SCHOOL:
                triggerEv = context.getString(R.string.notify_message_go_school);
                break;
            case LEAVE_SCHOOL:
                triggerEv = context.getString(R.string.notify_message_leave_school);
                break;
            case BACK_SCHOOL:
                triggerEv = context.getString(R.string.notify_message_back_school);
                break;
            case NOT_YET_REACH_SCHOOL:
                triggerEv = context.getString(R.string.notify_message_not_go_school);
                break;
            case BACK_HOME:
                triggerEv = context.getString(R.string.notify_message_go_home);
                break;
            case NOT_YET_REACH_HOME:
                triggerEv = context.getString(R.string.notify_message_not_go_home);
                break;
            default:
                L.w(TAG, "getIncidentSchoolGuardMsg INCIDENT_SCHOOL_GUARD: invalid event code: " + ev.getCode());
                sb.setLength(0);
                return;
        }
        sb.append(context.getString(R.string.notify_who_trigger_what_school_guard_event,
                devName, triggerEv));
    }

    private static void getConfFuncRefuseStrangerCallInMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyDevConfChangedReqMsg reqMsg = (Message.NotifyDevConfChangedReqMsg) pb;
        String action = reqMsg.getConf().getFuncs().getRejectStrangerCallIn() ? context.getString(R.string.notify_message_opened) : context.getString(R.string.notify_message_closed);
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.append(context.getString(R.string.notify_who_reject_stranger_in_modified_on_device, devName, action));
        } else {
            sb.append(context.getString(R.string.notify_who_reject_stranger_in_modified_by_who,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    devName, action));
        }
    }

    private static void getConfFuncRefuseStrangerCallOutMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyDevConfChangedReqMsg reqMsg = (Message.NotifyDevConfChangedReqMsg) pb;
        String action = reqMsg.getConf().getFuncs().getRejectStrangerCallOut() ? context.getString(R.string.notify_message_opened) : context.getString(R.string.notify_message_closed);
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.append(context.getString(R.string.notify_who_reject_stranger_out_modified_on_device,
                    devName, action));
        } else {
            sb.append(context.getString(R.string.notify_who_reject_stranger_out_modified_by_who,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    devName, action));
        }
    }

    /*开启/关闭定时开关机*/
    private static void getConfFuncTimerPowerOnOffMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyDevConfChangedReqMsg reqMsg = (Message.NotifyDevConfChangedReqMsg) pb;
        String action = reqMsg.getConf().getFuncs().getTimerPowerOnOff() ? context.getString(R.string.notify_message_opened) : context.getString(R.string.notify_message_closed);
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.append(context.getString(R.string.notify_who_timer_power_onoff_modified_on_device_onoff, devName, action));
        } else {
            sb.append(context.getString(R.string.notify_who_timer_power_onoff_modified_by_who_onoff,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    devName, action));
        }
    }

    private static void getConfFuncInundateRemindMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyDevConfChangedReqMsg reqMsg = (Message.NotifyDevConfChangedReqMsg) pb;
        String action = reqMsg.getConf().getFuncs().getInundateRemind() ? context.getString(R.string.notify_message_opened) : context.getString(R.string.notify_message_closed);
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.append(context.getString(R.string.notify_who_inundate_hint_modified_on_device,
                    devName, action));
        } else {
            sb.append(context.getString(R.string.notify_who_inundate_hint_modified_by_who,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    devName, action));
        }
    }

    private static void getConfFuncSavePowerModeMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyDevConfChangedReqMsg reqMsg = (Message.NotifyDevConfChangedReqMsg) pb;
        String action = reqMsg.getConf().getFuncs().getSavePowerMode() ? context.getString(R.string.notify_message_opened) : context.getString(R.string.notify_message_closed);
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.append(context.getString(R.string.notify_who_save_power_mode_modified_on_device,
                    devName, action));
        } else {
            sb.append(context.getString(R.string.notify_who_save_power_mode_modified_by_who,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    devName, action));
        }
    }

    private static void getConfFuncCallPositionMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyDevConfChangedReqMsg reqMsg = (Message.NotifyDevConfChangedReqMsg) pb;
        String action = reqMsg.getConf().getFuncs().getSavePowerMode() ? context.getString(R.string.notify_message_opened) : context.getString(R.string.notify_message_closed);
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.append(context.getString(R.string.notify_who_call_location_modified_on_device,
                    devName, action));
        } else {
            sb.append(context.getString(R.string.notify_who_call_location_modified_by_who,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    devName, action));
        }
    }

    private static void getConfFuncWatchSetLightMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyDevConfChangedReqMsg reqMsg = (Message.NotifyDevConfChangedReqMsg) pb;
        int t = reqMsg.getConf().getFuncs().getWatchSetLight();
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.append(context.getString(R.string.notify_who_light_time_modified_on_device,
                    devName, t));
        } else {
            sb.append(context.getString(R.string.notify_who_light_time_modified_by_who,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    devName,
                    t));
        }
    }

    // 变更定时开机
    private static void getConfFuncTimerPowerOnMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyDevConfChangedReqMsg reqMsg = (Message.NotifyDevConfChangedReqMsg) pb;
        long timePointOn = reqMsg.getConf().getFuncs().getTimerPowerOn().getTime();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timePointOn * 1000L);
        String power_on = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));

        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.append(context.getString(R.string.notify_who_timer_power_onoff_modified_on_device_to_on,
                    devName, power_on));
        } else {
            sb.append(context.getString(R.string.notify_who_timer_power_onoff_modified_by_who_on,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    
                    devName,
                    power_on));
        }
    }

    // 变更定时关机
    private static void getConfFuncTimerPowerOffMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyDevConfChangedReqMsg reqMsg = (Message.NotifyDevConfChangedReqMsg) pb;

        long timePointOff = reqMsg.getConf().getFuncs().getTimerPowerOff().getTime();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timePointOff * 1000L);
        String power_off = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));

        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.append(context.getString(R.string.notify_who_timer_power_onoff_modified_on_device_to_off,
                    devName, power_off));
        } else {
            sb.append(context.getString(R.string.notify_who_timer_power_onoff_modified_by_who_off,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    
                    devName, power_off));
        }
    }

    private static void getConfFuncEnableCalculatorMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyDevConfChangedReqMsg reqMsg = (Message.NotifyDevConfChangedReqMsg) pb;
        String action = reqMsg.getConf().getFuncs().getEnableCalculator() ? context.getString(R.string.notify_message_opened) : context.getString(R.string.notify_message_closed);
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.append(context.getString(R.string.notify_who_calculator_modified_on_device,
                    devName, action));
        } else {
            sb.append(context.getString(R.string.notify_who_calculator_modified_by_who,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    
                    devName, action));
        }
    }

    private static void getConfBabyMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyDevConfChangedReqMsg reqMsg = (Message.NotifyDevConfChangedReqMsg) pb;
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.append(context.getString(R.string.notify_who_kid_info_modified_on_device,
                    devName));
        } else {
            sb.append(context.getString(R.string.notify_who_kid_info_modified_by_who,
                    RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                    
                    devName));
        }
    }

    private static void getConfFenceMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyFenceChangedReqMsg reqMsg = (Message.NotifyFenceChangedReqMsg) pb;
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.setLength(0);
            return;
        }

        switch (reqMsg.getDetail().getAction()) {
            case ADD:
                sb.append(context.getString(R.string.notify_who_which_fence_add_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName,
                        reqMsg.getDetail().getFence(0).getName()));
                break;
            case DEL:
                sb.append(context.getString(R.string.notify_who_which_fence_del_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName,
                        reqMsg.getDetail().getFence(0).getName()));
                break;
            case MODIFY:
                sb.append(context.getString(R.string.notify_who_which_fence_modified_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName,
                        reqMsg.getDetail().getFence(0).getName()));
                break;
            default:
                sb.setLength(0);
                break;
        }
    }

    private static void getConfSosMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifySosChangedReqMsg reqMsg = (Message.NotifySosChangedReqMsg) pb;
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.setLength(0);
            return;
        }

        switch (reqMsg.getDetail().getAction()) {
            case ADD:
                sb.append(context.getString(R.string.notify_who_which_sos_add_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        devName,
                        reqMsg.getDetail().getSos(0).getName(),
                        reqMsg.getDetail().getSos(0).getPhonenum()));
                break;
            case DEL:
                sb.append(context.getString(R.string.notify_who_which_sos_del_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        devName,
                        reqMsg.getDetail().getSos(0).getName(),
                        reqMsg.getDetail().getSos(0).getPhonenum()));
                break;
            case MODIFY:
                sb.append(context.getString(R.string.notify_who_which_sos_modified_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        devName,
                        reqMsg.getDetail().getSos(0).getName(),
                        reqMsg.getDetail().getSos(0).getPhonenum()));
                break;
            default:
                sb.setLength(0);
                break;
        }
    }

    private static void getConfContactsMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyContactChangedReqMsg reqMsg = (Message.NotifyContactChangedReqMsg) pb;
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.setLength(0);
            return;
        }

        switch (reqMsg.getDetail().getAction()) {
            case ADD:
                sb.append(context.getString(R.string.notify_who_which_contacts_add_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName,
                        reqMsg.getDetail().getContact(0).getName(),
                        reqMsg.getDetail().getContact(0).getNumber()));
                break;
            case DEL:
                sb.append(context.getString(R.string.notify_who_which_contacts_del_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName,
                        reqMsg.getDetail().getContact(0).getName(),
                        reqMsg.getDetail().getContact(0).getNumber()));
                break;
            case MODIFY:
                sb.append(context.getString(R.string.notify_who_which_contacts_modified_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName,
                        reqMsg.getDetail().getContact(0).getName(),
                        reqMsg.getDetail().getContact(0).getNumber()));
                break;
            default:
                sb.setLength(0);
                break;
        }
    }

    private static void getConfAlarmClockMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyAlarmClockChangedReqMsg reqMsg = (Message.NotifyAlarmClockChangedReqMsg) pb;
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            switch (reqMsg.getDetail().getAction()) {
                case ADD:
                    sb.append(context.getString(R.string.notify_who_which_alarm_clock_add_on_device,
                            devName,
                            reqMsg.getDetail().getAlarmClock(0).getName()));
                    break;
                case DEL:
                    sb.append(context.getString(R.string.notify_who_which_alarm_clock_del_on_device,
                            devName,
                            reqMsg.getDetail().getAlarmClock(0).getName()));
                    break;
                case MODIFY:
                    sb.append(context.getString(R.string.notify_who_which_alarm_clock_modified_on_device,
                            devName,
                            reqMsg.getDetail().getAlarmClock(0).getName()));
                    break;
                default:
                    sb.setLength(0);
                    break;
            }
            return;
        }

        switch (reqMsg.getDetail().getAction()) {
            case ADD:
                sb.append(context.getString(R.string.notify_who_which_alarm_clock_add_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName,
                        reqMsg.getDetail().getAlarmClock(0).getName()));
                break;
            case DEL:
                sb.append(context.getString(R.string.notify_who_which_alarm_clock_del_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName,
                        reqMsg.getDetail().getAlarmClock(0).getName()));
                break;
            case MODIFY:
                sb.append(context.getString(R.string.notify_who_which_alarm_clock_modified_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName,
                        reqMsg.getDetail().getAlarmClock(0).getName()));
                break;
            default:
                sb.setLength(0);
                break;
        }
    }

    private static void getConfClassDisableMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyClassDisableChangedReqMsg reqMsg = (Message.NotifyClassDisableChangedReqMsg) pb;
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            switch (reqMsg.getDetail().getAction()) {
                case ADD:
                    sb.append(context.getString(R.string.notify_who_which_class_disable_add_on_device,
                            devName,
                            reqMsg.getDetail().getClassDisable(0).getName()));
                    break;
                case DEL:
                    sb.append(context.getString(R.string.notify_who_which_class_disable_del_on_device,
                            devName,
                            reqMsg.getDetail().getClassDisable(0).getName()));
                    break;
                case MODIFY:
                    sb.append(context.getString(R.string.notify_who_which_class_disable_modified_on_device,
                            devName,
                            reqMsg.getDetail().getClassDisable(0).getName()));
                    break;
                default:
                    sb.setLength(0);
                    break;
            }
            return;
        }

        switch (reqMsg.getDetail().getAction()) {
            case ADD:
                sb.append(context.getString(R.string.notify_who_which_class_disable_add_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName,
                        reqMsg.getDetail().getClassDisable(0).getName()));
                break;
            case DEL:
                sb.append(context.getString(R.string.notify_who_which_class_disable_del_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName,
                        reqMsg.getDetail().getClassDisable(0).getName()));
                break;
            case MODIFY:
                sb.append(context.getString(R.string.notify_who_which_class_disable_modified_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName,
                        reqMsg.getDetail().getClassDisable(0).getName()));
                break;
            default:
                sb.setLength(0);
                break;
        }
    }

    private static void getConfSchoolGuardMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifySchoolGuardChangedReqMsg reqMsg = (Message.NotifySchoolGuardChangedReqMsg) pb;
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.setLength(0);
            return;
        }
        sb.append(context.getString(R.string.notify_who_school_guard_modified_by_who,
                RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                
                devName));
    }

    private static void getConfCollectPraiseMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyPraiseChangedReqMsg reqMsg = (Message.NotifyPraiseChangedReqMsg) pb;
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.setLength(0);
            return;
        }

        String userRelation = RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation());
        String userPhone = msgEntity.getOriginator_phone();

        switch (reqMsg.getDetail().getAction()) {
            case ADD:
                sb.append(context.getString(R.string.notify_who_which_collect_praise_add_by_who,
                        userRelation, 
                        devName,
                        reqMsg.getDetail().getPraise().getPrize()));
                break;
            case DEL:
                sb.append(context.getString(R.string.notify_who_which_collect_praise_del_by_who,
                        userRelation,
                        devName,
                        reqMsg.getDetail().getPraise().getPrize()));
                break;
            case MODIFY:
                sb.append(context.getString(R.string.notify_who_which_collect_praise_modified_by_who,
                        userRelation,
                        devName,
                        reqMsg.getDetail().getPraise().getPrize()));
                break;
            case PRAISE: {
                if (reqMsg.getDetail().getPraisedItemIdCount() == 0) {
                    sb.setLength(0);
                    break;
                }
                String itemId = reqMsg.getDetail().getPraisedItemId(0);
                if (TextUtils.isEmpty(itemId)) {
                    sb.setLength(0);
                    break;
                }
                Message.Praise.Item praisedItem = null;
                for (Message.Praise.Item item : reqMsg.getDetail().getPraise().getItemList()) {
                    if (item.getId().equals(itemId)) {
                        praisedItem = item;
                        break;
                    }
                }
                if (praisedItem == null) {
                    sb.setLength(0);
                    break;
                }
                sb.append(context.getString(R.string.notify_who_which_collect_praise_item_praised_by_who,
                        userRelation,
                        devName,
                        praisedItem.getName()));
                break;
            }
            case COMPLETE:
                sb.append(context.getString(R.string.notify_who_which_collect_praise_complete,
                        devName,
                        reqMsg.getDetail().getPraise().getPrize()));
                break;
            case FINISH:
                sb.append(context.getString(R.string.notify_who_which_collect_praise_finish,
                        devName,
                        reqMsg.getDetail().getPraise().getPrize()));
                break;
            case CANCELED:
                sb.append(context.getString(R.string.notify_who_which_collect_praise_canceled_by_who,
                        userRelation,
                        devName,
                        reqMsg.getDetail().getPraise().getPrize()));
                break;
            default:
                sb.setLength(0);
                break;
        }
    }

    private static void getConfLocationModeMsg(@NonNull Context context, @NonNull StringBuilder sb, @NonNull String devName, @NonNull NotifyMessageEntity msgEntity, @NonNull GeneratedMessageV3 pb) {
        Message.NotifyLocationModeChangedReqMsg reqMsg = (Message.NotifyLocationModeChangedReqMsg) pb;
        if (TextUtils.isEmpty(reqMsg.getChanger()) || reqMsg.getChanger().equals(msgEntity.getDeviceId())) {
            sb.setLength(0);
            return;
        }
        switch (reqMsg.getLocationMode().getNumber()) {
            case Message.LocationMode.LM_PASSIVE_VALUE:
                sb.append(context.getString(R.string.notify_who_which__modified_location_mode_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName, context.getString(R.string.passive_mode)));
                break;
            case Message.LocationMode.LM_POWER_SAVING_VALUE:
                sb.append(context.getString(R.string.notify_who_which__modified_location_mode_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName, context.getString(R.string.save_power_mode)));
                break;
            case Message.LocationMode.LM_NORMAL_VALUE:
                sb.append(context.getString(R.string.notify_who_which__modified_location_mode_by_who,
                        RelationUtils.decodeRelation(context, msgEntity.getOriginator_relation()),
                        
                        devName, context.getString(R.string.normal_mode)));
                break;
            default:
                break;

        }
    }
}
