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


/**
 * Created by Administrator on 2018/3/12.
 */

@Entity
public class ClassDisableEntity implements BaseBean, Serializable {

    private static final long serialVersionUID = 957874069825295956L;
    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private String deviceId;
    @NotNull
    private String classDisableId;

    private String name;
    @NotNull
    private long beginTime;
    @NotNull
    private long endTime;
    @NotNull
    private int repeat;
    @NotNull
    private String timezone;
    @NotNull
    private boolean enable;
    private boolean synced;

    @Transient
    private Integer beginHour;
    @Transient
    private Integer beginMinute;
    @Transient
    private Integer endHour;
    @Transient
    private Integer endMinute;
    @Transient
    private TimeZone tz;
    @Transient
    private Calendar beginCal;
    @Transient
    private Calendar endCal;

    @Generated(hash = 796347608)
    public ClassDisableEntity(Long id, @NotNull String deviceId,
            @NotNull String classDisableId, String name, long beginTime,
            long endTime, int repeat, @NotNull String timezone, boolean enable,
            boolean synced) {
        this.id = id;
        this.deviceId = deviceId;
        this.classDisableId = classDisableId;
        this.name = name;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.repeat = repeat;
        this.timezone = timezone;
        this.enable = enable;
        this.synced = synced;
    }

    @Generated(hash = 1180401591)
    public ClassDisableEntity() {
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
    private Calendar getBeginCal() {
        if (beginCal == null) {
            beginCal = Calendar.getInstance();
            beginCal.set(Calendar.SECOND, 0);
            beginCal.set(Calendar.MILLISECOND, 0);
            beginCal.setTimeZone(getTz());
        }
        long t = getBeginTime();
        if (t == 0) {
            setBeginTime(beginCal.getTimeInMillis() / 1000L);
        } else if (t != beginCal.getTimeInMillis() / 1000L) {
            beginCal.setTimeInMillis(t * 1000L);
        }
        return beginCal;
    }
    @Keep
    private void setBeginCal(Calendar calendar) {
    }
    @Keep
    public int getBeginHour() {
        if (beginHour == null) {
            Calendar cal = getBeginCal();
            beginHour = cal.get(Calendar.HOUR_OF_DAY);
        }
        return beginHour;
    }
    @Keep
    public void setBeginHour(int hour) {
           if (getBeginHour() != hour) {
            beginHour = hour;
            Calendar cal = getBeginCal();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            setBeginTime(cal.getTimeInMillis() / 1000L);
        }
    }
    @Keep
    public int getBeginMinute() {
        if (beginMinute == null) {
            Calendar cal = getBeginCal();
            beginMinute = cal.get(Calendar.MINUTE);
        }
        return beginMinute;
    }
    @Keep
    public void setBeginMinute(int minute) {
        if (getBeginMinute() != minute) {
            beginMinute = minute;
            Calendar cal = getBeginCal();
            cal.set(Calendar.MINUTE, minute);
            setBeginTime(cal.getTimeInMillis() / 1000L);
        }
    }
    @Keep
    private Calendar getEndCal() {
        if (endCal == null) {
            endCal = Calendar.getInstance();
            endCal.set(Calendar.SECOND, 0);
            endCal.set(Calendar.MILLISECOND, 0);
            endCal.setTimeZone(getTz());
            long t = getEndTime();
            if (t == 0) {
                setEndTime(endCal.getTimeInMillis() / 1000L);
            } else if (t != endCal.getTimeInMillis() / 1000L) {
                endCal.setTimeInMillis(t * 1000L);
            }
        }
        return endCal;
    }
    @Keep
    private void setEndCal(Calendar calendar) {
    }
    @Keep
    public int getEndHour() {
        if (endHour == null) {
            Calendar cal = getEndCal();
            endHour = cal.get(Calendar.HOUR_OF_DAY);
        }
        return endHour;
    }
    @Keep
    public void setEndHour(int hour) {
        if (getEndHour() != hour) {
            endHour = hour;
            Calendar cal = getEndCal();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            setEndTime(cal.getTimeInMillis() / 1000L);
        }
    }
    @Keep
    public int getEndMinute() {
        if (endMinute == null) {
            Calendar cal = getEndCal();
            endMinute = cal.get(Calendar.MINUTE);
        }
        return endMinute;
    }
    @Keep
    public void setEndMinute(int minute) {
        if (getEndMinute() != minute) {
            endMinute = minute;
            Calendar cal = getEndCal();
            cal.set(Calendar.MINUTE, minute);
            setEndTime(cal.getTimeInMillis() / 1000L);
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

    public String getClassDisableId() {
        return this.classDisableId;
    }

    public void setClassDisableId(String classDisableId) {
        this.classDisableId = classDisableId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getBeginTime() {
        return this.beginTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
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
