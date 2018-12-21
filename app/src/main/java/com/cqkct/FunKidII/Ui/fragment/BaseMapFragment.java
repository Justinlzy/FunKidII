package com.cqkct.FunKidII.Ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.amap.api.maps.AMap;
import com.amap.api.services.core.ServiceSettings;
import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.Bean.google.geocode.Addresscomponents;
import com.cqkct.FunKidII.Bean.google.geocode.GoogleRegeocodeResult;
import com.cqkct.FunKidII.Bean.google.geocode.ResultAddressInfo;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.Constants;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.service.tlc.PreferencesWrapper;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class BaseMapFragment extends BaseFragment {
    private static final String TAG = BaseMapFragment.class.getSimpleName();

    protected int mMapType = Constants.MAP_TYPE_AMAP;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int appArea = selectAppArea();
        if (appArea == PreferencesWrapper.APP_AREA_OVER_SEA) {
            mMapType = Constants.MAP_TYPE_GOOGLE;
            googleApiAvailability();
        } else {
            mMapType = Constants.MAP_TYPE_AMAP;
        }
        EventBus.getDefault().removeStickyEvent(Event.AppAreaSwitched.class);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onGoogleAccessibility(Event.GoogleAccessibility accessible) {
        if (mMapType != Constants.MAP_TYPE_GOOGLE)
            return;
        switch (accessible) {
            case UNKNOWN:
                onGoogleAccessibilityUnknown();
                break;
            case ACCESSIBLE:
                onGoogleAccessible();
                break;
            case INACCESSIBLE:
                if (tlcIsConnected) {
                    onGoogleInaccessible();
                }
                break;
        }
    }

    protected void onGoogleAccessible() {
    }

    protected void onGoogleInaccessible() {
    }

    protected void onGoogleAccessibilityUnknown() {
    }

    private boolean tlcIsConnected;

    @Override
    public void onConnected(TlcService tlcService, boolean isSticky) {
        tlcIsConnected = true;
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (App.getInstance().getGoogleAccessibility() == Event.GoogleAccessibility.INACCESSIBLE) {
                onGoogleInaccessible();
            }
        }
    }

    @Override
    public void onDisconnected(TlcService tlcService, boolean isSticky) {
        tlcIsConnected = false;
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            onGoogleAccessibilityUnknown();
        }
    }

    protected void googleApiAvailability() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(getContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            mMapType = Constants.MAP_TYPE_AMAP;
            if (googleApiAvailability.isUserResolvableError(resultCode) && resultCode == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) {
                googleApiAvailability.getErrorDialog(getActivity(), resultCode, 2404).show();
            } else {
                new AlertDialog.Builder(getContext())
                        .setMessage(R.string.not_support_google_play_cannot_use_google_map)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }
    }

    //设置高德地图语言*/
    public void setAMapLanguage(AMap aMap) {
        if (!getCurrentLanguageUseResources().toLowerCase().contains("zh")) {
            aMap.setMapLanguage(AMap.ENGLISH);
            // 英文进行搜索和地址解析
            ServiceSettings.getInstance().setLanguage(ServiceSettings.ENGLISH);
        } else {
            aMap.setMapLanguage(AMap.CHINESE);
            ServiceSettings.getInstance().setLanguage(ServiceSettings.CHINESE);
        }
    }

    public int getMapType() {
        return mMapType;
    }

    public void setMapType(int mapType) {
        mMapType = mapType;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onAppAreaSwitched(Event.AppAreaSwitched ev) {
        int mapSdk;
        if (ev.getAppArea() == PreferencesWrapper.APP_AREA_CHINA) {
            mapSdk = Constants.MAP_TYPE_AMAP;
        } else {
            mapSdk = Constants.MAP_TYPE_GOOGLE;
        }
        if (mapSdk != mMapType) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    }

    public interface GoogleRegeocodeResultListener {
        void onGoogleRegeocode(GoogleRegeocodeTask task, Throwable throwable, int httpStatusCode, GoogleRegeocodeResult geocodeResult, String formattedAddress, String pointOfInterest);
    }




    public static class GoogleRegeocodeTask extends Thread {
        private WeakReference<BaseMapFragment> mF;
        private BaseMapFragment.GoogleRegeocodeResultListener mGoogleRegeocodeResultListener;
        private Object mTag;

        private double mLatitude, mLongitude;
        private String mLanguage;
        private boolean isFinish;

        public GoogleRegeocodeTask(BaseMapFragment a, BaseMapFragment.GoogleRegeocodeResultListener listener) {
            mF = new WeakReference<>(a);
            mGoogleRegeocodeResultListener = listener;
        }

        public GoogleRegeocodeTask execute(double latitude, double longitude) {
            mLatitude = latitude;
            mLongitude = longitude;
            BaseMapFragment f = mF.get();
            if (f == null) {
                mLanguage = "en";
            } else {
                mLanguage = f.getCurrentLanguageUseResources();
            }
            start();
            return this;
        }

        public void cancel() {
            try {
                isFinish = true;
                interrupt();
            } catch (Exception e) {
                L.e(TAG, "GoogleRegeocodeTask.cancel()", e);
            }
        }

        public boolean isFinish() {
            return isFinish;
        }

        public void setTag(Object tag) {
            mTag = tag;
        }

        public Object getTag() {
            return mTag;
        }

        public void run1() {
            BaseMapFragment f = mF.get();
            if (f == null)
                return;

            // return: [Exception, HTTP code, GoogleRegeocodeResult, address, POI, GoogleRegeocodeTask]
            Object[] ret = new Object[6];
            ret[5] = this;

            try {
                Geocoder geocoder = new Geocoder(f.getContext(), new Locale(mLanguage, Locale.getDefault().getCountry()));
                List<Address> addresses = geocoder.getFromLocation(mLatitude, mLongitude, 1);
                if (addresses == null || addresses.isEmpty())
                    return;
                Address addr = addresses.get(0);
                String fmtAddr = "";
                String poi = "";
                String province = addr.getAdminArea(); // 省
                if (province == null)
                    province = "";
                String city = addr.getLocality(); // 市
                if (city == null)
                    city = "";
                String district = addr.getSubLocality(); // 区
                if (district == null)
                    district = "";
                int addrLine = 0;
                if (TextUtils.isEmpty(province) && TextUtils.isEmpty(city) && TextUtils.isEmpty(district)) {
                    if (addr.getMaxAddressLineIndex() > addrLine) {
                        fmtAddr = addr.getAddressLine(addrLine++);
                    }
                }
                if (TextUtils.isEmpty(fmtAddr)) {
                    while (addrLine < addr.getMaxAddressLineIndex()) {
                        String str = addr.getAddressLine(addrLine++);
                        if (TextUtils.isEmpty(str)) {
                            continue;
                        }
                        if (!TextUtils.isEmpty(province) && str.contains(province)) {
                            fmtAddr = str;
                            break;
                        }
                        if (!TextUtils.isEmpty(city) && str.contains(city)) {
                            fmtAddr = province + str;
                            break;
                        }
                        if (!TextUtils.isEmpty(district) && str.contains(district)) {
                            fmtAddr = province + city + str;
                            break;
                        }
                        fmtAddr = province + city + district + str;
                        break;
                    }
                    if (TextUtils.isEmpty(fmtAddr)) {
                        fmtAddr = province + city + district;
                    }
                }
                for (; addrLine < addr.getMaxAddressLineIndex(); ++addrLine) {
                    String str = addr.getAddressLine(addrLine);
                    if (TextUtils.isEmpty(str)) {
                        continue;
                    }
                    if (!TextUtils.isEmpty(province) && str.contains(province)) {
                        continue;
                    }
                    if (!TextUtils.isEmpty(city) && str.contains(city)) {
                        continue;
                    }
                    if (!TextUtils.isEmpty(district) && str.contains(district)) {
                        continue;
                    }
                    poi = str;
                    break;
                }
                ret[3] = fmtAddr;
                ret[4] = poi;
            } catch (Exception e) {
                if (isInterrupted()) {
                    return;
                }
                L.w(TAG, "GoogleRegeocodeTask", e);
                ret[0] = e;
            } finally {
                isFinish = true;
            }
            postReturn(ret);
        }

        public void run() {
            // return: [Exception, HTTP code, GoogleRegeocodeResult, address, POI, GoogleRegeocodeTask]
            Object[] ret = new Object[6];
            ret[5] = this;

            try {
                double lat = mLatitude;
                double lon = mLongitude;
                String urlStr = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lon + "&language=" + mLanguage + "&key=AIzaSyC--ogpte6S2GqFZ9ZrIqtOGdE87_U0JTg";
                URL url = new URL(urlStr);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);
                int httpStatusCode = urlConnection.getResponseCode();
                if (isInterrupted()) {
                    return;
                }
                ret[1] = httpStatusCode;
                if (httpStatusCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = urlConnection.getInputStream();
                    int len;
                    byte[] buffer = new byte[1024];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    //读取输入流
                    while ((len = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    baos.flush(); //清除缓冲区
                    if (isInterrupted()) {
                        return;
                    }
                    String reslutStr = new String(baos.toByteArray());
                    L.d(TAG, "GoogleRegeocodeTask doInBackground reslutStr: " + reslutStr);
                    Gson gson = new Gson();
                    GoogleRegeocodeResult gmphResult = gson.fromJson(reslutStr, GoogleRegeocodeResult.class);
                    ret[2] = gmphResult;
                    String status = gmphResult.getStatus();
                    if (!TextUtils.isEmpty(status) && ("OK").equals(status)) {
                        List<ResultAddressInfo> addressBeens = gmphResult.getResults();
                        if (addressBeens != null && !addressBeens.isEmpty()) {

                            Addresscomponents poiAddrComp = findPOI(addressBeens);
                            if (poiAddrComp != null) {
                                ret[4] = poiAddrComp.getLong_name();
                                ret[3] = addressBeens.get(0).getFormatted_address();
                                postReturn(ret);
                                return;
                            }

                            ResultAddressInfo info = addressBeens.get(0);
                            StringBuilder fmtAddr = new StringBuilder();
                            List<Addresscomponents> addrComps = info.getAddress_components();
                            int i = 0;
                            while (i < addrComps.size()) {
                                Addresscomponents addrComp = addrComps.get(i++);
                                boolean containStreetNumber = false;
                                for (String type : addrComp.getTypes()) {
                                    if (type.equals("street_number")) {
                                        containStreetNumber = true;
                                        break;
                                    }
                                }
                                if (containStreetNumber)
                                    continue;
                                ret[4] = addrComp.getLong_name();
                                break;
                            }
                            while (i < addrComps.size()) {
                                Addresscomponents addrComp = addrComps.get(i++);
//                                boolean isCountry = false;
//                                for (String type : addrComp.getTypes()) {
//                                    if (type.equals("country")) {
//                                        isCountry = true;
//                                        break;
//                                    }
//                                }
//                                if (isCountry)
//                                    continue;

                                fmtAddr.insert(0, addrComp.getLong_name());
                            }
                            ret[3] = fmtAddr.toString();
                        }
                    }
                } else {
                    L.d("PublicTools getGoogleAddressTextStr ResponseCode = " + urlConnection.getResponseCode());
                }
            } catch (Exception e) {
                if (isInterrupted()) {
                    return;
                }
                L.w(TAG, "GoogleRegeocodeTask", e);
                ret[0] = e;
            } finally {
                isFinish = true;
            }
            postReturn(ret);
        }

        private Addresscomponents findPOI(List<ResultAddressInfo> result) {
            for (ResultAddressInfo addrInfo : result) {
                for (Addresscomponents addrComp : addrInfo.getAddress_components()) {
                    for (String type : addrComp.getTypes()) {
                        if (type.toLowerCase().equals("point_of_interest")) {
                            // POI found
                            return addrComp;
                        }
                    }
                }
            }
            return null;
        }

        private void postReturn(Object[] ret) {
            isFinish = true;
            BaseMapFragment f = mF.get();
            if (f != null && !f.isRemoving()) {
                f.mXHandler.obtainMessage(BaseMapFragment.XHandler.ON_GOOGLE_REGEOCODE, ret).sendToTarget();
            }
        }
    }

    private volatile XHandler mXHandler = new XHandler(this);

    private static class XHandler extends Handler {
        static final int ON_GOOGLE_REGEOCODE = 1;

        private WeakReference<BaseMapFragment> mF;

        private XHandler(BaseMapFragment a) {
            mF = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            BaseMapFragment f = mF.get();
            if (f == null)
                return;
            if (f.isRemoving())
                return;
            switch (msg.what) {
                case ON_GOOGLE_REGEOCODE: {
                    // return: [Exception, HTTP code, GoogleRegeocodeResult, address, POI, GoogleRegeocodeTask]
                    Object[] objs = (Object[]) msg.obj;
                    if (objs[5] != null) {
                        GoogleRegeocodeTask task = (GoogleRegeocodeTask) objs[5];
                        if (task.mGoogleRegeocodeResultListener != null) {
                            int httpStatusCode = objs[1] == null ? 0 : (int) objs[1];
                            task.mGoogleRegeocodeResultListener.onGoogleRegeocode(task, (Throwable) objs[0], httpStatusCode, (GoogleRegeocodeResult) objs[2], (String) objs[3], (String) objs[4]);
                        }
                    }
                }
                break;
            }
        }
    }
}
