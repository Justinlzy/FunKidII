package com.cqkct.FunKidII.Bean.google.geocode;

public class Location {
	private double lat;

	private double lng;

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLat() {
		return this.lat;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}

	public double getLng() {
		return this.lng;
	}

	@Override
	public String toString() {
		return "Location [lat=" + lat + ", lng=" + lng + "]";
	}

}