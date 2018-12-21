package com.cqkct.FunKidII.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cqkct.FunKidII.Bean.TrackPoint;
import com.cqkct.FunKidII.Bean.UmengEvent;
import com.cqkct.FunKidII.R;
import com.umeng.analytics.MobclickAgent;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import protocol.Message;


/**
 * Created by Teprinciple on 2016/8/22.
 * 跳转到高德地图进行导航
 */
public class NavigationUtils {
    private static final String TAG = NavigationUtils.class.getSimpleName();

    public static final String AMapPackageName = "com.autonavi.minimap";
    public static final String BaiduMapPackageName = "com.baidu.BaiduMap";
    public static final String GoogleMapPackageName = "com.google.android.apps.maps";

    private static final Map<String, GotoMapApp> mapAppMap = new HashMap<>();

    static {
        mapAppMap.put(AMapPackageName, new GotoMapApp() {
            @Override
            public void gotoApp(Context context, final String deviceId, Message.LatLon latLon, String address, boolean shouldWalking) {
                MobclickAgent.onEvent(context, UmengEvent.NAVIGATION_TIMES, new HashMap<String, String>() {
                    {
                        put(UmengEvent.NAVIGATION_TIMES_PARAM_MAP_APP, UmengEvent.NAVIGATION_TIMES_PARAM_MAP_APP_AMAP);
                    }
                });
                gotoAMap(context, latLon, address, shouldWalking);
            }
        });

        mapAppMap.put(BaiduMapPackageName, new GotoMapApp() {
            @Override
            public void gotoApp(Context context, final String deviceId, Message.LatLon endPoint, String address, boolean shouldWalking) {
                MobclickAgent.onEvent(context, UmengEvent.NAVIGATION_TIMES, new HashMap<String, String>() {
                    {
                        put(UmengEvent.NAVIGATION_TIMES_PARAM_MAP_APP, UmengEvent.NAVIGATION_TIMES_PARAM_MAP_APP_BAIDU);
                    }
                });
                gotoBaiduMap(context, endPoint, address, shouldWalking);
            }
        });

        mapAppMap.put(GoogleMapPackageName, new GotoMapApp() {
            @Override
            public void gotoApp(Context context, final String deviceId, Message.LatLon endPoint, String address, boolean shouldWalking) {
                MobclickAgent.onEvent(context, UmengEvent.NAVIGATION_TIMES, new HashMap<String, String>() {
                    {
                        put(UmengEvent.NAVIGATION_TIMES_PARAM_MAP_APP, UmengEvent.NAVIGATION_TIMES_PARAM_MAP_APP_GOOGLE);
                    }
                });
                gotoGoogleMap(context, endPoint, address, shouldWalking);
            }
        });
    }

    public interface GotoMapApp {
        void gotoApp(Context context, String deviceId, Message.LatLon latLon, String address, boolean shouldWalking);
    }

    public static void navigation(Context context, String deviceId, String mapPackageName, double lat, double lon, String address, boolean shouldWalking) {
        GotoMapApp iface = mapAppMap.get(mapPackageName);
        if (iface == null) {
            L.e(TAG, "navigation " + mapPackageName + ": not impl!!!");
            return;
        }
        if (address == null)
            address = "";
        iface.gotoApp(context, deviceId, Message.LatLon.newBuilder()
                .setLatitude(lat)
                .setLongitude(lon).build(), address, shouldWalking);
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            L.i(TAG, "isAppInstalled(" + packageName + ")", e);
        }
        return false;
    }

    /**
     * 跳转到高德地图进行导航
     */
    private static void gotoAMap(Context context, Message.LatLon endPoint, String address, boolean shouldWalking) {
        // 参考 http://lbs.amap.com/api/amap-mobile/guide/android/route
        try {
            String t = "0";
            if (shouldWalking) {
                t = "2";
            }
            Uri uri = Uri.parse("amapuri://route/plan/?sourceApplication=com.cqkct.FunKidII&dlat=" + endPoint.getLatitude() + "&dlon=" + endPoint.getLongitude() + "&dname=" + URLEncoder.encode(address, "utf-8") + "&dev=0&t=" + t);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(uri);
            intent.setPackage(AMapPackageName);
            context.startActivity(intent);
        } catch (Exception e) {
            L.e(TAG, "gotoAMap", e);
        }
    }

    /**
     * 跳转到百度地图进行导航
     */
    private static void gotoBaiduMap(Context context, Message.LatLon endPoint, String address, boolean shouldWalking) {
        try {
            String mode = "driving";
            if (shouldWalking) {
                mode = "walking";
            }
            double[] latLng = LatLngUtils.gcj02_To_Bd09(endPoint.getLatitude(), endPoint.getLongitude());
            Uri uri = Uri.parse("baidumap://map/direction?src=com.cqkct.FunKidII&destination=latlng:" + latLng[0] + "," + latLng[1] + "|name:" + URLEncoder.encode(address, "utf-8") + "&mode=" + mode);
            Intent intent = new Intent();
            intent.setData(uri);
            context.startActivity(intent);
        } catch (Exception e) {
            L.e(TAG, "gotoBaiduMap", e);
        }
    }

    private static void gotoGoogleMap(Context context, Message.LatLon endPoint, String address, boolean shouldWalking) {
        try {
            String mode = "driving";
            if (shouldWalking) {
                mode = "walking";
            }
            Uri gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + endPoint.getLatitude() + "," + endPoint.getLongitude() + "&travelmode=" + mode);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage(GoogleMapPackageName);
            context.startActivity(mapIntent);
        } catch (Exception e) {
            L.e(TAG, "gotoGoogleMap", e);
        }
    }

    public static void onNavigationButtonClick(Context mContext, String deviceId, @Nullable LatLon phoneLatLon, @NonNull LatLon kidLatLon, String address, OnShowSelectMapAppListener listener) {
        try {
            boolean shouldWalking = false;

            Map<String, String> mapApps = new LinkedHashMap<>();
            if (NavigationUtils.isAppInstalled(mContext, NavigationUtils.AMapPackageName)) {
                mapApps.put(mContext.getString(R.string.map_amap), NavigationUtils.AMapPackageName);
            }
            if (NavigationUtils.isAppInstalled(mContext, NavigationUtils.BaiduMapPackageName)) {
                mapApps.put(mContext.getString(R.string.map_baidumap), NavigationUtils.BaiduMapPackageName);
            }
            if (NavigationUtils.isAppInstalled(mContext, NavigationUtils.GoogleMapPackageName)) {
                mapApps.put(mContext.getString(R.string.map_googlemap), NavigationUtils.GoogleMapPackageName);
            }
            if (mapApps.isEmpty()) {
                listener.onShowSelectMapNoMapListener();
                return;
            }
            if (phoneLatLon != null) {
                double dis = TrackPoint.calcDistance(phoneLatLon.lon, phoneLatLon.lat, kidLatLon.lon, kidLatLon.lat);
                if (dis < 500) {
                    shouldWalking = true;
                }
            }
            if (mapApps.size() > 1) {
                listener.onShowSelectMapListener(mapApps, kidLatLon.lat, kidLatLon.lon, address, shouldWalking);
            } else {
                String mapPackageName = mapApps.entrySet().iterator().next().getValue();
                NavigationUtils.navigation(mContext, deviceId, mapPackageName, kidLatLon.lat, kidLatLon.lon, address, shouldWalking);
            }
        } catch (Exception e) {
            L.w(TAG, "navigation_to_kid_btn onClick", e);
            listener.onShowSelectMapNoMapListener();
        }
    }

    public interface OnShowSelectMapAppListener {
        void onShowSelectMapListener(Map<String, String> mapApps, double lat, double lon, final String address, boolean shouldWalking);
        void onShowSelectMapNoMapListener();
    }
}
