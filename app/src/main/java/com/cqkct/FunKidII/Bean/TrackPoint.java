package com.cqkct.FunKidII.Bean;

import java.util.ArrayList;
import java.util.List;

import protocol.Message;

/**
 * 轨迹点
 */
public class TrackPoint {
    /**
     * 纬度
     */
    private double lat;
    /**
     * 经度
     */
    private double lon;
    /**
     * 定位精度，单位：米
     */
    private double accuracy;
    /**
     * 定位时间
     */
    private long time;

    /**
     * 定位类型
     */
    private Message.Position.LocateType locateType;
    /**
     * 轨迹点的时间，单位：秒
     * 包含所有合并后的每一个点的时间
     */
    private List<Long> locateTimeList = new ArrayList<>();

    private int serialNumber;

    /**
     * 轨迹点处的事件
     */
    public class Incident {
        /**
         * 事件发生的时间，单位：秒
         */
        private long time;
        /**
         * 具体事件 protoBuf 中的 Incident
         */
        private Message.Incident incident;

        public Incident(Message.Incident incident, long time) {
            this.incident = incident;
            this.time = time;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public Message.Incident getIncident() {
            return incident;
        }

        public void setIncident(Message.Incident incident) {
            this.incident = incident;
        }
    }

    /**
     * 这段时间的事件
     */
    private List<Incident> incidents = new ArrayList<>();


    /** 最大可能的移动速度 */
    private static final double MAX_SPEED = 1500 * 1000.0 / 3600;

    public TrackPoint(Message.PositionRecord record) {
        setLatLon(record.getPosition().getLatLng());
        setTime(record.getPosition().getTime());
        setLocateType(record);
        merge(record);
    }

    /**
     * 合并一个定位点进来
     */
    public void merge(Message.PositionRecord record) {
        locateTimeList.add(record.getPosition().getTime());
        if (record.getIncident().getFlag() != 0) {
            incidents.add(new Incident(record.getIncident(), record.getPosition().getTime()));
        }
        if (isSatellitePosition(record)) {
            setAccuracy(record.getPosition().getAccuracy());
        } else if (record.getPosition().getAccuracy() > getAccuracy()) {
            setAccuracy(record.getPosition().getAccuracy());
        }
    }

    /**
     * 合并一个轨迹点进来
     */
    public void merge(TrackPoint point) {
        if (point.isSatellitePoint() && !this.isSatellitePoint()) {
            this.setLocateType(point.locateType);
        }
        this.locateTimeList.addAll(point.locateTimeList);
        this.incidents.addAll(point.incidents);
        if (point.isSatellitePoint()) {
            setAccuracy(point.getAccuracy());
        } else if (point.getAccuracy() > getAccuracy()) {
            setAccuracy(point.getAccuracy());
        }
    }

    /**
     * 设置轨迹点的经纬度
     */
    public void setLatLon(Message.LatLon latLon) {
        lat = latLon.getLatitude();
        lon = latLon.getLongitude();
    }

    /**
     * 设置轨迹点的经纬度
     */
    public void setLatLon(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }

    /**
     * 设置定位类型
     */
    public void setLocateType(Message.PositionRecord record) {
        Message.Position.LocateType type = record.getPosition().getLocateType();
        if (type == Message.Position.LocateType.HYBRID) {
            type = record.getAccessType() == Message.StationInfo.AccessType.WIFI
                    ? Message.Position.LocateType.WIFI
                    : Message.Position.LocateType.CELL;
        }
        setLocateType(type);
    }

    /**
     * 设置定位类型
     */
    public void setLocateType(Message.Position.LocateType type) {
        locateType = type;
    }

    public Message.Position.LocateType getLocateType() {
        return locateType;
    }

    public List<Long> getLocateTimeList() {
        return locateTimeList;
    }

    public List<Incident> getIncidentList() {
        return incidents;
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

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isSatellitePoint() {
        return isSatelliteType(locateType);
    }

    public boolean isWifiPosition() {
        return locateType == Message.Position.LocateType.WIFI;
    }

    public boolean isCellPosition() {
        return locateType == Message.Position.LocateType.CELL;
    }

    public boolean hasIncident() {
        return !incidents.isEmpty();
    }

    public static boolean isSatelliteType(Message.Position.LocateType type) {
        switch (type) {
            case GPS:
            case BDS:
            case GALILEO:
            case GLONASS:
                return true;
            default:
                return false;
        }
    }

    public static boolean isSatellitePosition(Message.PositionRecord position) {
        return isSatelliteType(position.getPosition().getLocateType());
    }

    public static boolean isWifiPosition(Message.PositionRecord position) {
        return position.getPosition().getLocateType() == Message.Position.LocateType.WIFI ||
                (position.getPosition().getLocateType() == Message.Position.LocateType.HYBRID &&
                        position.getAccessType() == Message.StationInfo.AccessType.WIFI);
    }

    public static boolean isCellPosition(Message.PositionRecord position) {
        return position.getPosition().getLocateType() == Message.Position.LocateType.CELL ||
                (position.getPosition().getLocateType() == Message.Position.LocateType.HYBRID &&
                        position.getAccessType() == Message.StationInfo.AccessType.CELL);
    }

    public static boolean hasIncident(Message.PositionRecord position) {
        return position.getIncident().getFlag() != 0;
    }

    public static List<TrackPoint> merge(List<protocol.Message.PositionRecord> positions, double satelliteMergeDistance, double wifiMergeDistance, double cellMergeDistance) {
        List<TrackPoint> points = new ArrayList<>();
        List<TrackPoint> section = new ArrayList<>();
        String secKey = ""; // 段键
        for (Message.PositionRecord position : positions) {
            String key = "";

            if (TrackPoint.isSatellitePosition(position)) {
                // 这是卫星定位信息
                key = "SATELLITE";
            } else {
                // 这是基站定位信息
                if (TrackPoint.isWifiPosition(position)) {
                    // 以 WiFi 为主的定位
                    key = position.getPrimaryWifiStationInfo().getSsid() + position.getPrimaryWifiStationInfo().getBssid();
                } else {
                    // 以 蜂窝基站 为主的定位
                    if (position.getPrimaryCellStationInfo().getGsm().getSignal() == 0) {
                        // CDMA 蜂窝基站
                        Message.CdmaStationInfo cdma = position.getPrimaryCellStationInfo().getCdma();
                        key += "" + cdma.getSid() + cdma.getNid() + cdma.getBid();
                    } else {
                        // GSM 基站
                        Message.GsmStationInfo gsm = position.getPrimaryCellStationInfo().getGsm();
                        key += "" + gsm.getMcc() + gsm.getMnc() + gsm.getLac() + gsm.getCellid();
                    }
                }
            }

            if (!secKey.equals(key)) {
                // 打断
                secKey = key;
                if (!section.isEmpty()) {
                    points.addAll(sectionMerge(section, satelliteMergeDistance, wifiMergeDistance, cellMergeDistance));
                    section.clear();
                }
            }

            section.add(new TrackPoint(position));
        }

        if (!section.isEmpty()) {
            points.addAll(sectionMerge(section, satelliteMergeDistance, wifiMergeDistance, cellMergeDistance));
        }

        return mixingMerge(points, satelliteMergeDistance, wifiMergeDistance, cellMergeDistance);
    }

    public int x(List<TrackPoint> points) {
        int out = 0;
        for (TrackPoint point : points) {
            out += point.locateTimeList.size();
        }
        return out;
    }

    private static List<TrackPoint> sectionMerge(List<TrackPoint> section, double satelliteMergeDistance, double wifiMergeDistance, double cellMergeDistance) {
        if (section.size() <= 1) {
            return section;
        }

        TrackPoint firstPoint = section.get(0);

        if (firstPoint.isSatellitePoint()) {
            // 卫星定位点
            return satelliteSectionMergeSatellite(section, satelliteMergeDistance);
        }

        if (firstPoint.isWifiPosition()) {
            // WiFi 定位点
            return wifiSectionMergeSatellite(section, wifiMergeDistance);
        }

        return cellSectionMergeSatellite(section, cellMergeDistance);
    }

    public static double calcDistance(double long1, double lat1, double long2, double lat2) {
        double a, b, R;
        R = 6378137; // 地球半径
        lat1 = lat1 * Math.PI / 180.0;
        lat2 = lat2 * Math.PI / 180.0;
        a = lat1 - lat2;
        b = (long1 - long2) * Math.PI / 180.0;
        double d;
        double sa2, sb2;
        sa2 = Math.sin(a / 2.0);
        sb2 = Math.sin(b / 2.0);
        d = 2 * R
                * Math.asin(Math.sqrt(sa2 * sa2 + Math.cos(lat1)
                * Math.cos(lat2) * sb2 * sb2));
        return d;
    }

    private static List<TrackPoint> satelliteSectionMergeSatellite(List<TrackPoint> section, double satelliteMergeDistance) {
        if (section.size() <= 1)
            return section;

        List<TrackPoint> out = new ArrayList<>();

        TrackPoint basePoint = section.get(0);
        for (int i = 1; i < section.size(); ++i) {
            TrackPoint point = section.get(i);
            double dis = calcDistance(point.getLon(), point.getLat(), basePoint.getLon(), basePoint.getLat());
            double speed = dis / Math.abs(point.getTime() - basePoint.getTime());
            if (speed > MAX_SPEED) {
                // 丢弃该点
                continue;
            }
            if (dis > satelliteMergeDistance) {
                out.add(basePoint);
                basePoint = point;
                continue;
            }
            basePoint.merge(point);
        }
        out.add(basePoint);

        return out;
    }

    private Message.LatLon averageCenter(List<TrackPoint> points) {
        double lat = 0, lon = 0;
        for (TrackPoint point : points) {
            lat += point.getLat() * Math.PI / 180;
            lon += point.getLon() * Math.PI / 180;
        }
        lat /= points.size();
        lat = lat * 180 / Math.PI;
        lon /= points.size();
        lon = lon * 180 / Math.PI;

        return Message.LatLon.newBuilder().setLongitude(lon).setLatitude(lat).build();
    }

    private static Message.LatLon medianCenter(List<TrackPoint> points) {
        TrackPoint firstPoint = points.get(0);
        double latMin = firstPoint.getLat();
        double latMax = firstPoint.getLat();
        double lonMin = firstPoint.getLon();
        double lonMax = firstPoint.getLon();
        for (int i = 1; i < points.size(); ++i) {
            TrackPoint point = points.get(i);

            double dis = calcDistance(point.getLon(), point.getLat(), firstPoint.getLon(), firstPoint.getLat());
            double speed = dis / Math.abs(point.getTime() - firstPoint.getTime());
            if (speed > MAX_SPEED) {
                // 丢弃该点
                continue;
            }

            if (point.getLat() < latMin) {
                latMin = point.getLat();
            } else if (point.getLat() > latMax) {
                latMax = point.getLat();
            }
            if (point.getLon() < lonMin) {
                lonMin = point.getLon();
            } else if (point.getLon() > lonMax) {
                lonMax = point.getLon();
            }
        }
        double lat = (latMin + latMax) / 2;
        double lon = (lonMin + lonMax) / 2;

        return Message.LatLon.newBuilder().setLongitude(lon).setLatitude(lat).build();
    }

    private static List<TrackPoint> wifiSectionMergeSatellite(List<TrackPoint> section, double wifiMergeDistance) {
        if (section.size() <= 1)
            return section;

        List<TrackPoint> out = new ArrayList<>();

        // 找出中心点
        Message.LatLon centor = medianCenter(section);

//        // 合并 使用中心点
//        TrackPoint basePoint = section.get(0);
//        for (int i = 1; i < section.size(); ++i) {
//            basePoint.merge(section.get(i));
//        }
//        basePoint.setLat(centor.getLatitude());
//        basePoint.setLon(centor.getLongitude());

        // 合并 使用离中心最近的点
        TrackPoint basePoint = section.get(0);
        TrackPoint nearestPoint = basePoint;
        double dis = Math.abs(centor.getLatitude() - nearestPoint.getLat()) + Math.abs(centor.getLongitude() - nearestPoint.getLon());
        for (int i = 1; i < section.size(); ++i) {
            TrackPoint point = section.get(i);
            double newDis = Math.abs(centor.getLatitude() - point.getLat()) + Math.abs(centor.getLongitude() - point.getLon());
            if (newDis < dis) {
                dis = newDis;
                nearestPoint = point;
            }
            basePoint.merge(point);
        }
        basePoint.setLat(nearestPoint.getLat());
        basePoint.setLon(nearestPoint.getLon());
        basePoint.setTime(nearestPoint.getTime());

        out.add(basePoint);
        return out;
    }

    private static List<TrackPoint> cellSectionMergeSatellite(List<TrackPoint> section, double cellMergeDistance) {
//        if (section.size() <= 1)
//            return section;
//
//        List<TrackPoint> out = new ArrayList<>();
//
//        TrackPoint basePoint = section.get(0);
//        for (int i = 1; i < section.size(); ++i) {
//            TrackPoint point = section.get(i);
//            if (calcDistance(point.position.getPosition().getLatLng(), basePoint.position.getPosition().getLatLng()) > cellMergeDistance) {
//                basePoint.periodEnd = section.get(i - 1).periodEnd;
//                out.add(basePoint);
//                basePoint = point;
//            }
//        }
//        basePoint.periodEnd = section.get(section.size() - 1).periodEnd;
//        out.add(basePoint);
//
//        return out;

        if (section.size() <= 1)
            return section;

        List<TrackPoint> out = new ArrayList<>();

        // 找出中心点
        Message.LatLon centor = medianCenter(section);

//        // 合并 使用中心点
//        TrackPoint basePoint = section.get(0);
//        for (int i = 1; i < section.size(); ++i) {
//            basePoint.merge(section.get(i));
//        }
//        basePoint.setLat(centor.getLatitude());
//        basePoint.setLon(centor.getLongitude());

        // 合并 使用离中心最近的点
        TrackPoint basePoint = section.get(0);
        TrackPoint nearestPoint = basePoint;
        double dis = Math.abs(centor.getLatitude() - nearestPoint.getLat()) + Math.abs(centor.getLongitude() - nearestPoint.getLon());
        for (int i = 1; i < section.size(); ++i) {
            TrackPoint point = section.get(i);
            double newDis = Math.abs(centor.getLatitude() - point.getLat()) + Math.abs(centor.getLongitude() - point.getLon());
            if (newDis < dis) {
                dis = newDis;
                nearestPoint = point;
            }
            basePoint.merge(point);
        }
        basePoint.setLat(nearestPoint.getLat());
        basePoint.setLon(nearestPoint.getLon());
        basePoint.setTime(nearestPoint.getTime());

        out.add(basePoint);
        return out;
    }

    private static List<TrackPoint> mixingMerge(List<TrackPoint> points, double satelliteMergeDistance, double wifiMergeDistance, double cellMergeDistance) {
        List<TrackPoint> out = points;
        do {
            points = out;
            out = distancingMerge(points, satelliteMergeDistance, wifiMergeDistance, cellMergeDistance);
            out = breakageMerge(out);
        } while (out.size() > 1 && out.size() != points.size());
        return out;
    }

    private static List<TrackPoint> distancingMerge(List<TrackPoint> points, double satelliteMergeDistance, double wifiMergeDistance, double cellMergeDistance) {
        if (points.size() <= 1)
            return points;

        List<TrackPoint> out = new ArrayList<>();

        TrackPoint basePoint = null;
        for (TrackPoint point : points) {
            if (point.isSatellitePoint()) {
                // 这是卫星定位信息

                if (basePoint != null && !basePoint.isSatellitePoint()) {
                    // 基点是基站定位
                    // 保存基点
                    out.add(basePoint);
                    basePoint = null;
                }

                if (basePoint == null) {
                    // 无基点，将该点作为基点
                    basePoint = point;
                    continue;
                }

                double dis = calcDistance(point.getLon(), point.getLat(), basePoint.getLon(), basePoint.getLat());
                double speed = dis / Math.abs(point.getTime() - basePoint.getTime());
                if (speed > MAX_SPEED) {
                    // 丢弃该点
                    continue;
                }
                // 与基点距离小于某一值，合并到基点上
                if (dis < satelliteMergeDistance) {
                    basePoint.merge(point);
                    continue;
                }
            } else {
                // 这是基站定位信息

                if (basePoint != null && basePoint.isSatellitePoint()) {
                    // 基点是卫星定位
                    // 保存基点
                    out.add(basePoint);
                    basePoint = null;
                }

                if (basePoint == null) {
                    // 无基点，将该点作为基点
                    basePoint = point;
                    continue;
                }

                // 与基准点距离小于某一值，合并到基点上
                double dis = calcDistance(point.getLon(), point.getLat(), basePoint.getLon(), basePoint.getLat());
                double speed = dis / Math.abs(point.getTime() - basePoint.getTime());
                if (speed > MAX_SPEED) {
                    // 丢弃该点
                    continue;
                }
                // 两点距离小于两点定位精度之和
                if (calcDistance(point.getLon(), point.getLat(), basePoint.getLon(), basePoint.getLat()) < (point.getAccuracy() + basePoint.getAccuracy())) {
                    basePoint.merge(point);
                    continue;
                }
            }

            // 无法合并的点
            out.add(basePoint);
            basePoint = point;
        }
        if (basePoint != null) {
            out.add(basePoint);
        }

        return out;
    }

    private static List<TrackPoint> breakageMerge(List<TrackPoint> points) {
        if (points.size() <= 1)
            return points;

        List<TrackPoint> out = new ArrayList<>();

        TrackPoint lastPoint = points.get(0);
        out.add(lastPoint);
        for (int i = 1; i < points.size(); ++i) {
            TrackPoint point = points.get(i);

            if (point.isSatellitePoint() && !lastPoint.isSatellitePoint()) {
                // 这一个是卫星定位点，上一个是基站定位点
                double dis = calcDistance(point.getLon(), point.getLat(), lastPoint.getLon(), lastPoint.getLat());
                double speed = dis / Math.abs(point.getTime() - lastPoint.getTime());
                if (speed > MAX_SPEED) {
                    // 丢弃该点
                    continue;
                }
                // 他们之间的距离小于基站定位点的定位精度时，合并，取卫星定位点的经纬度。
                if (dis < lastPoint.getAccuracy()) {
                    lastPoint.merge(point);
                    lastPoint.setLat(point.getLat());
                    lastPoint.setLon(point.getLon());
                    continue;
                }
            }

            if (!point.isSatellitePoint() && lastPoint.isSatellitePoint()) {
                // 这一个是基站定位点，上一个是卫星定位点
                double dis = calcDistance(point.getLon(), point.getLat(), lastPoint.getLon(), lastPoint.getLat());
                double speed = dis / Math.abs(point.getTime() - lastPoint.getTime());
                if (speed > MAX_SPEED) {
                    // 丢弃该点
                    continue;
                }
                // 他们之间的距离小于基站定位点的定位精度时，合并，取卫星定位点的经纬度。
                if (dis < point.getAccuracy()) {
                    lastPoint.merge(point);
                    continue;
                }
            }

            // 独立的点
            out.add(point);
            lastPoint = point;
        }

        return out;
    }
}
