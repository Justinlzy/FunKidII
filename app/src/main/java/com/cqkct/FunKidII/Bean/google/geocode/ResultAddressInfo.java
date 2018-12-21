package com.cqkct.FunKidII.Bean.google.geocode;

import java.util.List;

public class ResultAddressInfo {

	private List<Addresscomponents> address_components;

	private String formatted_address;

	private Geometry geometry;

	private String place_id;

	private List<String> types;

	public List<Addresscomponents> getAddress_components() {
		return address_components;
	}

	public void setAddress_components(List<Addresscomponents> address_components) {
		this.address_components = address_components;
	}

	public String getFormatted_address() {
		return formatted_address;
	}

	public void setFormatted_address(String formatted_address) {
		this.formatted_address = formatted_address;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	public String getPlace_id() {
		return place_id;
	}

	public void setPlace_id(String place_id) {
		this.place_id = place_id;
	}

	public List<String> getTypes() {
		return types;
	}

	public void setTypes(List<String> types) {
		this.types = types;
	}

	@Override
	public String toString() {
		return "ResultAddressInfo [address_components=" + address_components + ", formatted_address="
				+ formatted_address + ", geometry=" + geometry + ", place_id=" + place_id + ", types=" + types + "]";
	}

}