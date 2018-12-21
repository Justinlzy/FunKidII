package com.cqkct.FunKidII.Bean.google.geocode;

import java.util.List;

public class GoogleRegeocodeResult {

	private List<ResultAddressInfo> results;
	private String status;

	public List<ResultAddressInfo> getResults() {
		return results;
	}

	public void setResults(List<ResultAddressInfo> results) {
		this.results = results;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "GoogleRegeocodeResult [results=" + results + ", status=" + status + "]";
	}

}