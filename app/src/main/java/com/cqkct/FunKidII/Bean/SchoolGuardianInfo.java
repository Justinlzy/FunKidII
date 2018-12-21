package com.cqkct.FunKidII.Bean;

import java.io.Serializable;

/**
 * Created by 木子饼干 on 2016/11/9.
 */

public class SchoolGuardianInfo implements Serializable{
    private static final long serialVersionUID = 8627617758247471308L;
    private String id;
    private String name;
    private long am_arrival_time;
    private long am_leave_time;
    private long pm_arrival_time;
    private long pm_leave_time;
    private long latest_arrival_home;
    private int repeat;
    private boolean is_holiday;
    private boolean is_enable;

    private int school_radius;
    private String school_address;
    private Double school_Latitude;
    private Double school_LonTitude;

    private int home_radius;
    private String home_address;
    private Double home_Latitude;
    private Double home_LonTitude;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAm_arrival_time() {
        return am_arrival_time;
    }

    public void setAm_arrival_time(long am_arrival_time) {
        this.am_arrival_time = am_arrival_time;
    }

    public long getAm_leave_time() {
        return am_leave_time;
    }

    public void setAm_leave_time(long am_leave_time) {
        this.am_leave_time = am_leave_time;
    }

    public long getPm_arrival_time() {
        return pm_arrival_time;
    }

    public void setPm_arrival_time(long pm_arrival_time) {
        this.pm_arrival_time = pm_arrival_time;
    }

    public long getPm_leave_time() {
        return pm_leave_time;
    }

    public void setPm_leave_time(long pm_leave_time) {
        this.pm_leave_time = pm_leave_time;
    }

    public long getLatest_arrival_home() {
        return latest_arrival_home;
    }

    public void setLatest_arrival_home(long latest_arrival_home) {
        this.latest_arrival_home = latest_arrival_home;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public int getSchool_radius() {
        return school_radius;
    }

    public void setSchool_radius(int school_radius) {
        this.school_radius = school_radius;
    }

    public boolean isIs_holiday() {
        return is_holiday;
    }

    public void setIs_holiday(boolean is_holiday) {
        this.is_holiday = is_holiday;
    }

    public boolean isIs_enable() {
        return is_enable;
    }

    public void setIs_enable(boolean is_enable) {
        this.is_enable = is_enable;
    }



    public String getSchool_address() {
        return school_address;
    }

    public void setSchool_address(String school_address) {
        this.school_address = school_address;
    }

    public Double getSchool_Latitude() {
        return school_Latitude;
    }

    public void setSchool_Latitude(Double school_Latitude) {
        this.school_Latitude = school_Latitude;
    }

    public Double getSchool_LonTitude() {
        return school_LonTitude;
    }

    public void setSchool_LonTitude(Double school_LonTitude) {
        this.school_LonTitude = school_LonTitude;
    }

    public int getHome_radius() {
        return home_radius;
    }

    public void setHome_radius(int home_radius) {
        this.home_radius = home_radius;
    }

    public String getHome_address() {
        return home_address;
    }

    public void setHome_address(String home_address) {
        this.home_address = home_address;
    }

    public Double getHome_Latitude() {
        return home_Latitude;
    }

    public void setHome_Latitude(Double home_Latitude) {
        this.home_Latitude = home_Latitude;
    }

    public Double getHome_LonTitude() {
        return home_LonTitude;
    }

    public void setHome_LonTitude(Double home_LonTitude) {
        this.home_LonTitude = home_LonTitude;
    }
}
