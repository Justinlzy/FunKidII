/**
 *
 */
package com.cqkct.FunKidII.Bean;

import java.io.Serializable;

/**
 * @author wuzhiyi
 * @date 2016-4-21 10:36:21
 */
public class LocationPoint implements Serializable {
    public double lat;
    public double lon;

    public LocationPoint(){

    }

    public LocationPoint(String lat, String lon){
        this.lat = Double.valueOf(lat);
        this.lon = Double.valueOf(lon);
    }
    public LocationPoint(double lat, double lon){
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    @Override
    public String toString() {
        return "LocationPoint{" +
                "lat=" + lat +
                ", lon=" + lon +
                '}';
    }
}
