package com.cqkct.FunKidII.Bean.google.geocode;

import java.util.List;

public class Addresscomponents {
	private String long_name;

	private String short_name;

	private List<String> types;

	public void setLong_name(String long_name) {
		this.long_name = long_name;
	}

	public String getLong_name() {
		return this.long_name;
	}

	public void setShort_name(String short_name) {
		this.short_name = short_name;
	}

	public String getShort_name() {
		return this.short_name;
	}

	public void setTypes(List<String> types) {
		this.types = types;
	}

	public List<String> getTypes() {
		return this.types;
	}

	@Override
	public String toString() {
		return "Addresscomponents [long_name=" + long_name + ", short_name=" + short_name + ", types=" + types + "]";
	}

}