package com.cqkct.FunKidII.db.Entity;

import android.text.TextUtils;

import com.cqkct.FunKidII.Bean.BaseBean;
import com.cqkct.FunKidII.Utils.DateUtil;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;
import java.util.Calendar;
import java.util.TimeZone;

import protocol.Message;

/**
 * Created by Administrator on 2018/3/12.
 */

@Entity
public class AlarmClockEntity implements BaseBean, Serializable {

    private static final long serialVersionUID = -3830476323618488148L;

    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private String deviceId;
    @NotNull
    private String alarmClockId;
    private String name;
    @NotNull
    private long timePoint;
    @NotNull
    private int repeat;
    @NotNull
    private String timezone;
    @NotNull
    private int noticeFlag;
    @NotNull
    private boolean enable;
    private boolean synced;

    @Transient
    private Integer hour;
    @Transient
    private Integer minute;
    @Transient
    private TimeZone tz;
    @Transient
    private Calendar cal;


    @Generated(hash = 1724651447)
    public AlarmClockEntity(Long id, @NotNull String deviceId, @NotNull String alarmClockId,
            String name, long timePoint, int repeat, @NotNull String timezone, int noticeFlag,
            boolean enable, boolean synced) {
        this.id = id;
        this.deviceId = deviceId;
        this.alarmClockId = alarmClockId;
        this.name = name;
        this.timePoint = timePoint;
        this.repeat = repeat;
        this.timezone = timezone;
        this.noticeFlag = noticeFlag;
        this.enable = enable;
        this.synced = synced;
    }


    @Generated(hash = 1382368525)
    public AlarmClockEntity() {
    }


    // KEEP METHODS - put your custom methods here
    @Override
    @Keep
    public Long getId() {
        return id;
    }
    @Keep
    private TimeZone getTz() {
        if (tz == null) {
            String tz1 = getTimezone();
            if (TextUtils.isEmpty(tz1)) {
                setTimezone(DateUtil.timezoneISO8601());
            }
            tz = DateUtil.parseTimeZone(getTimezone());
        }
        return tz;
    }
    @Keep
    private void setTz(TimeZone tz) {
    }
    @Keep
    private Calendar getCalendar() {
        if (cal == null) {
            cal = Calendar.getInstance();
            if (false) {
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.setTimeZone(getTz());
            }
        }
        long t = getTimePoint();
        if (t == 0) {
            setTimePoint(cal.getTimeInMillis() / 1000L);
        } else if (t != cal.getTimeInMillis() / 1000L) {
            cal.setTimeInMillis(t * 1000L);
        }
        return cal;
    }
    @Keep
    public int getHour() {
        if (hour == null) {
            Calendar cal = getCalendar();
            hour = cal.get(Calendar.HOUR_OF_DAY);
        }
        return hour;
    }
    @Keep
    public void setHour(int hour) {
        if (getHour() != hour) {
            this.hour = hour;
            Calendar cal = getCalendar();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            setTimePoint(cal.getTimeInMillis() / 1000L);
        }
    }
    @Keep
    public int getMinute() {
        if (minute == null) {
            Calendar cal = getCalendar();
            minute = cal.get(Calendar.MINUTE);
        }
        return minute;
    }
    @Keep
    public void setMinute(int minute) {
        if (getMinute() != minute) {
            this.minute = minute;
            Calendar cal = getCalendar();
            cal.set(Calendar.MINUTE, minute);
            setTimePoint(cal.getTimeInMillis() / 1000L);
        }
    }
    @Keep
    public Message.AlarmClock getAlarmClock() {
        Message.AlarmClock.Builder b = Message.AlarmClock.newBuilder();
        b.setName(getName())
                .setTime(Message.TimePoint.newBuilder().setTime(getTimePoint()))
                .setRepeat(getRepeat())
                .setTimezone(Message.Timezone.newBuilder().setZone(DateUtil.timezoneISO8601()))
                .setNoticeFlag(getNoticeFlag())
                .setEnable(getEnable())
                .setDevSynced(getSynced());
        if (!TextUtils.isEmpty(getAlarmClockId())) {
            b.setId(getAlarmClockId());
        }
        return b.build();
    }
    @Keep
    public void setAlarmClock(Message.AlarmClock alarmClock) {
        setAlarmClockId(alarmClock.getId());
        setName(alarmClock.getName());
        setTimePoint(alarmClock.getTime().getTime());
        setRepeat(alarmClock.getRepeat());
        setTimezone(alarmClock.getTimezone().getZone());
        setNoticeFlag(alarmClock.getNoticeFlag());
        setEnable(alarmClock.getEnable());
        setSynced(alarmClock.getDevSynced());
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


    public String getAlarmClockId() {
        return this.alarmClockId;
    }


    public void setAlarmClockId(String alarmClockId) {
        this.alarmClockId = alarmClockId;
    }


    public String getName() {
        return this.name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public long getTimePoint() {
        return this.timePoint;
    }


    public void setTimePoint(long timePoint) {
        this.timePoint = timePoint;
    }


    public int getRepeat() {
        return this.repeat;
    }


    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }


    public String getTimezone() {
        return this.timezone;
    }


    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }


    public int getNoticeFlag() {
        return this.noticeFlag;
    }


    public void setNoticeFlag(int noticeFlag) {
        this.noticeFlag = noticeFlag;
    }


    public boolean getEnable() {
        return this.enable;
    }


    public void setEnable(boolean enable) {
        this.enable = enable;
    }


    public boolean getSynced() {
        return this.synced;
    }


    public void setSynced(boolean synced) {
        this.synced = synced;
    }


}
