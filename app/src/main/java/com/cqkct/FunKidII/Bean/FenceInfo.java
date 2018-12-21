package com.cqkct.FunKidII.Bean;


import java.io.Serializable;

import protocol.Message;

public class FenceInfo implements Serializable {
	private String device_id;
	private int stamp_time;
	private String name;
	private int state;
	private int radius;
	private int location_stamp_time;
	private int location_type;
	private float location_lat;
	private float location_lng;

	public static int getFenceAlertTypeIn() {
		return Message.Fence.CondFlag.ENTER_VALUE;
	}

	public static int getFenceAlertTypeOut() {
		return Message.Fence.CondFlag.LEAVE_VALUE;
	}

	public static int getFenceAlertTypeInOut() {
		return Message.Fence.CondFlag.ENTER_VALUE | Message.Fence.CondFlag.LEAVE_VALUE;
	}

	public String getDevice_id() {
		return device_id;
	}

	public void setDevice_id(String device_id) {
		this.device_id = device_id;
	}

	public int getStamp_time() {
		return stamp_time;
	}

	public void setStamp_time(int stamp_time) {
		this.stamp_time = stamp_time;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public int getLocation_stamp_time() {
		return location_stamp_time;
	}

	public void setLocation_stamp_time(int location_stamp_time) {
		this.location_stamp_time = location_stamp_time;
	}

	public int getLocation_type() {
		return location_type;
	}

	public void setLocation_type(int location_type) {
		this.location_type = location_type;
	}

	public float getLocation_lat() {
		return location_lat;
	}

	public void setLocation_lat(float location_lat) {
		this.location_lat = location_lat;
	}

	public float getLocation_lng() {
		return location_lng;
	}

	public void setLocation_lng(float location_lng) {
		this.location_lng = location_lng;
	}
}
