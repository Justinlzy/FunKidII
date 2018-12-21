package com.cqkct.FunKidII.Bean;


import java.io.Serializable;

public class FenceAlertInfo implements Serializable {
	public String userName;
	public String teId = null;
	public String deviceName;
	public long alertTime;
	public String alertTimeStr;
	public String fenceName;
	public int alertState;
	public int curAlertState;

	public FenceAlertInfo(){

	}

	
	public String getFenceStateKey(){
		return this.teId+this.fenceName+this.curAlertState;
	}

	public long getAlertTime() {
		return alertTime;
	}

	public void setAlertTime(long alertTime) {
		this.alertTime = alertTime;
	}

	public String getAlertTimeStr() {
		return alertTimeStr;
	}

	public void setAlertTimeStr(String alertTimeStr) {
		this.alertTimeStr = alertTimeStr;
	}

	public String getFenceName() {
		return fenceName;
	}

	public void setFenceName(String fenceName) {
		this.fenceName = fenceName;
	}

	public int getAlertState() {
		return alertState;
	}

	public void setAlertState(int alertState) {
		this.alertState = alertState;
	}

	public int getCurAlertState() {
		return curAlertState;
	}

	public void setCurAlertState(int curAlertState) {
		this.curAlertState = curAlertState;
	}
}
