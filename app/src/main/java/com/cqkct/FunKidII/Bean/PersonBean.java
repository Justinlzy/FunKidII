package com.cqkct.FunKidII.Bean;

import android.support.annotation.StringRes;

public class PersonBean {

	private @StringRes int nameStringResId;
	private String Name;
	private String PinYin;
	private String FirstPinYin;

	public void setId(int id) {
		nameStringResId = id;
	}

	public @StringRes int getId() {
		return nameStringResId;
	}

	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name;
	}

	public String getPinYin() {
		return PinYin;
	}

	public void setPinYin(String pinYin) {
		PinYin = pinYin;
	}

	public String getFirstPinYin() {
		return FirstPinYin;
	}

	public void setFirstPinYin(String firstPinYin) {
		FirstPinYin = firstPinYin;
	}

	public String toString() {
		return "������" + getName() + "   ƴ����" + getPinYin() + "    ����ĸ��"
				+ getFirstPinYin();

	}

}
