package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.Projection;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.Bean.GuardianAddrInfo;
import com.cqkct.FunKidII.Bean.google.geocode.GoogleRegeocodeResult;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseMapActivity;
import com.cqkct.FunKidII.Ui.view.WheelView;
import com.cqkct.FunKidII.Utils.Constants;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.StringUtils;
import com.cqkct.FunKidII.Utils.Utils;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.List;

import protocol.Message;

/**
 * Created by Administrator on 2018/3/15.
 */

public class GuardianFenceActivity extends BaseMapActivity implements
        AMap.OnCameraChangeListener,
        GeocodeSearch.OnGeocodeSearchListener,
        LocationListener,
        OnMapReadyCallback, GoogleMap.OnCameraChangeListener, GoogleMap.OnMyLocationChangeListener,
        BaseMapActivity.GoogleRegeocodeResultListener {
    public final String TAG = this.getClass().getSimpleName();
    public static final String SCHOOLFENCE_TO_ADDRESSFENCE = "SCHOOLFENCE_TO_SCHOOLGUARDIAN";

    private int FENCE_RADIUS_MIN = 200;
    private int FENCE_RADIUS_DEF = 1000;

    private MapView amapView = null;
    private com.google.android.gms.maps.MapView gmapView;
    private AMap aMap = null;
    private GoogleMap gMap = null;
    private Marker addAmapMarker = null;
    private com.google.android.gms.maps.model.Marker addGmapMarker;

    private BitmapDescriptor amapDescriptor;  // lx  icon_fence   icon_fence_new
    private com.google.android.gms.maps.model.BitmapDescriptor gmapDescriptor;

    private GeocodeSearch mGeocodeSearch;
    private TextView tv_realTimeLocation;

    private GuardianAddrInfo addrInfo;
    private boolean is_home;
    private List<String> stringList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.school_fence);
        setTitleBarTitle(R.string.school_guide_select_around);

        Intent intent = this.getIntent();
        addrInfo = (GuardianAddrInfo) intent.getSerializableExtra(GuardianAddressActivity.SCHOOLADDRESS_TO_SCHOOLFENCE);
        is_home = intent.getBooleanExtra(GuardianActivity.IS_GUARDIAN_HOME_ADDRESS, false);

        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
        if (deviceInfo != null) {
            Message.FuncModuleInfo funcModuleInfo = deviceInfo.getDeviceEntity().getFuncModuleInfo();
            int minRadius = funcModuleInfo.getFenceRadiusMin();
            if (minRadius > 0) {
                FENCE_RADIUS_MIN = minRadius;
            }
            int defRadius = funcModuleInfo.getFenceRadiusDef();
            if (defRadius > 0) {
                FENCE_RADIUS_DEF = defRadius;
            }
        }
        if (addrInfo.radius == 0)
            addrInfo.radius = FENCE_RADIUS_DEF;

        initView(savedInstanceState);
    }

    private void initView(Bundle savedInstanceState) {

        tv_realTimeLocation = findViewById(R.id.tv_real_time_location);

        TextView saveText = findViewById(R.id.title_bar_right_text);
        saveText.setVisibility(View.VISIBLE);
        saveText.setText(R.string.save);

        //初始化地图
        initMap(savedInstanceState);

        WheelView wheelRadius = findViewById(R.id.wheel_radius);
        wheelRadius.setAdditionCenterMark("m");
        stringList = StringUtils.getWheelRadius(FENCE_RADIUS_MIN);
        for (int i = 0; i < stringList.size(); i++) {
            if (stringList.get(i).equals(String.valueOf(addrInfo.radius))) {
                wheelRadius.selectIndex(i);
                break;
            }
        }
        wheelRadius.setItems(stringList);
        wheelRadius.setOnWheelItemSelectedListener(new WheelView.OnWheelItemSelectedListener() {
            @Override
            public void onWheelItemChanged(WheelView wheelView, int position) {

            }

            @Override
            public void onWheelItemSelected(WheelView wheelView, int position) {
                addrInfo.radius = Integer.parseInt(stringList.get(position));
                refreshRadio();
            }
        });

    }

    //初始化地图
    private void initMap(Bundle savedInstanceState) {
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            gmapView = findViewById(R.id.gmap_fence_manager);
            gmapView.setVisibility(View.VISIBLE);
            gmapView.onCreate(savedInstanceState);
            gmapDescriptor = com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(R.drawable.fence_location_icon);
            if (gMap == null)
                gmapView.getMapAsync(this);

        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            amapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.fence_location_icon);
            amapView = findViewById(R.id.amap_fence_manager);
            amapView.setVisibility(View.VISIBLE);
            amapView.onCreate(savedInstanceState); // 管理地图的生命周期
            if (aMap == null)
                aMap = amapView.getMap();
            setAMapLanguage(aMap);

            MyLocationStyle myLocationStyle = new MyLocationStyle();
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE); // 定位一次，且将视角移动到地图中心点。
            myLocationStyle.showMyLocation(false);
            aMap.setMyLocationStyle(myLocationStyle);
            aMap.setOnCameraChangeListener(this);

            UiSettings uiSettings = aMap.getUiSettings();
            aMap.getUiSettings().setScaleControlsEnabled(true); // 显示比例尺
            uiSettings.setRotateGesturesEnabled(false);
            uiSettings.setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
            aMap.setMyLocationEnabled(true);

            mGeocodeSearch = new GeocodeSearch(this);
            mGeocodeSearch.setOnGeocodeSearchListener(this);
            initAMapLocation();
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        com.google.android.gms.maps.UiSettings uiSettings = gMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setRotateGesturesEnabled(false);
        uiSettings.setMapToolbarEnabled(false);
        gMap.setOnCameraChangeListener(this);
        gMap.setOnMyLocationChangeListener(this);
        initGMapLocation();
    }

    private void initAMapLocation() {
        if (addrInfo.lat != 0 && addrInfo.lng != 0) {
            LatLng cenpt = new LatLng(addrInfo.lat, addrInfo.lng);
            addMarkersToMap(addrInfo.lat, addrInfo.lng);
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cenpt, 14.0f));// 方法设置地图的可视区域。
        } else {
            //主动定位 TODO:
//            if (locationClient == null) {
//                locationClient = new AMapLocationClient(this);
//            }
//            locationClient.setLocationListener(this);
//            AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
//            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
//            mLocationOption.setOnceLocation(true);
//            mLocationOption.setOnceLocationLatest(true);
//            locationClient.setLocationOption(mLocationOption);
//            locationClient.startLocation();
        }
        showAddMarker();
    }

    private void initGMapLocation() {
        if (addrInfo.lat != 0 && addrInfo.lng != 0) {
            com.google.android.gms.maps.model.LatLng cenpt = new com.google.android.gms.maps.model.LatLng(addrInfo.lat, addrInfo.lng);
            addMarkersToMap(addrInfo.lat, addrInfo.lng);
            gMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(cenpt, 14.0f));// 方法设置地图的可视区域。
        } else {
            //手机主动定位
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 10, this);
            }
        }
        showAddMarker();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (gMap == null)
            return;
        com.google.android.gms.maps.model.LatLng cenpt = new com.google.android.gms.maps.model.LatLng(location.getLatitude(), location.getLongitude());
        addMarkersToMap(location.getLatitude(), location.getLongitude());
        gMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(cenpt, 14.0f));// 方法设置地图的可视区域。
    }


    private void addMarkersToMap(double latitude, double longitude) {
        if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (aMap == null)
                return;
            aMap.clear();
            MarkerOptions markerOption = new MarkerOptions();
            LatLng point = new LatLng(latitude, longitude);
            markerOption.position(point);
            markerOption.draggable(false);
            markerOption.icon(amapDescriptor);
            markerOption.setFlat(true);
            aMap.addMarker(markerOption);
        } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (gMap == null)
                return;
            gMap.clear();
            com.google.android.gms.maps.model.MarkerOptions markerOption = new com.google.android.gms.maps.model.MarkerOptions();
            com.google.android.gms.maps.model.LatLng point = new com.google.android.gms.maps.model.LatLng(latitude, longitude);
            markerOption.position(point);
            markerOption.draggable(false);
            markerOption.icon(gmapDescriptor);
            markerOption.flat(true);
            gMap.addMarker(markerOption);
        }

    }

    private void showAddMarker() {
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (gMap == null)
                return;
            gMap.clear();
            com.google.android.gms.maps.model.MarkerOptions markerOptions = new com.google.android.gms.maps.model.MarkerOptions()
                    .anchor(63f / 125f, 97f / 139f)
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(R.drawable.fence_location_icon))
                    .draggable(false)
                    .flat(true)
                    .position(gMap.getCameraPosition().target);
            addGmapMarker = gMap.addMarker(markerOptions);
            gMap.addCircle(new com.google.android.gms.maps.model.CircleOptions()
                    .center(addGmapMarker.getPosition())
                    .radius(addrInfo.radius)
                    .strokeColor(Constants.FENCE_STROKE_COLOR)
                    .fillColor(Constants.FENCE_FILL_COLOR)
                    .strokeWidth(5.0f));
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (aMap == null)
                return;
            aMap.clear();
            MarkerOptions markerOptions = new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.fence_location_icon))
                    .draggable(false)
                    .anchor(63f / 125f, 97f / 139f)
                    .setFlat(true)
                    .position(new LatLng(addrInfo.lat, addrInfo.lng));
            addAmapMarker = aMap.addMarker(markerOptions);
            int mapWidth = amapView.getWidth();
            int mapHeight = amapView.getHeight();
            addAmapMarker.setPositionByPixels(mapWidth / 2, mapHeight / 2);

            aMap.addCircle(new CircleOptions()
                    .center(addAmapMarker.getPosition())
                    .radius(addrInfo.radius)
                    .strokeColor(Constants.FENCE_STROKE_COLOR)
                    .fillColor(Constants.FENCE_FILL_COLOR)
                    .strokeWidth(5.0f));
        }

    }


    private void refreshRadio() {
        showAddMarker();
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            jumpPointGaoDeMarker(addAmapMarker);
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            jumpPointGaoDeMarker(addGmapMarker);
        }
    }

    public void jumpPointGaoDeMarker(final Object marker) {
        if (marker == null)
            return;
        final Handler handler = new Handler();
        final long duration = 1500;
        final long start = SystemClock.uptimeMillis();
        final Interpolator interpolator = new BounceInterpolator();

        if (marker instanceof com.google.android.gms.maps.model.Marker) {
            if (gMap == null)
                return;
            final com.google.android.gms.maps.model.LatLng latLng = ((com.google.android.gms.maps.model.Marker) marker).getPosition();
            com.google.android.gms.maps.Projection projection = gMap.getProjection();
            Point point = projection.toScreenLocation(latLng);
            point.offset(0, -100);
            final com.google.android.gms.maps.model.LatLng startLatLng = projection.fromScreenLocation(point);

            handler.post(new Runnable() {
                @Override
                public void run() {
                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = interpolator.getInterpolation((float) elapsed / duration);
                    double lng = t * latLng.longitude + (1 - t) * startLatLng.longitude;
                    double lat = t * latLng.latitude + (1 - t) * startLatLng.latitude;
                    ((com.google.android.gms.maps.model.Marker) marker).setPosition(new com.google.android.gms.maps.model.LatLng(lat, lng));
                    if (t < 1.0)
                        handler.postDelayed(this, 16);
                }
            });

        } else if (marker instanceof Marker) {
            if (aMap == null)
                return;
            final LatLng latLng = ((Marker) marker).getPosition();
            Projection proj = aMap.getProjection();
            Point startPoint = proj.toScreenLocation(latLng);
            startPoint.offset(0, -100);
            final LatLng startLatLng = proj.fromScreenLocation(startPoint);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = interpolator.getInterpolation((float) elapsed / duration);
                    double lng = t * latLng.longitude + (1 - t) * startLatLng.longitude;
                    double lat = t * latLng.latitude + (1 - t) * startLatLng.latitude;
                    ((Marker) marker).setPosition(new LatLng(lat, lng));
                    if (t < 1.0)
                        handler.postDelayed(this, 16);
                }
            });
        }
    }


    public void onTitleBarClick(View view) {
        switch (view.getId()) {
            case R.id.title_bar_left_icon:
                finish();
                break;
            case R.id.title_bar_right_text:
                Intent intent = new Intent();
                intent.putExtra(SCHOOLFENCE_TO_ADDRESSFENCE, addrInfo);
                setResult(RESULT_OK, intent);
                this.finish();
                break;
        }
    }

    public void onViewClick(View view) {
        switch (view.getId()) {

            case R.id.map_type: {
                if (mMapType == Constants.MAP_TYPE_AMAP) {
                    if (aMap == null)
                        break;
                    if (aMap.getMapType() == AMap.MAP_TYPE_NORMAL) {
                        aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                        ((ImageButton) findViewById(R.id.map_type)).setImageResource(R.drawable.change_button_change);
                    } else {
                        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                        ((ImageButton) findViewById(R.id.map_type)).setImageResource(R.drawable.change_button);
                        setAMapLanguage(aMap);
                    }
                } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                    if (gMap == null)
                        return;
                    if (gMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                        gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        ((ImageButton) findViewById(R.id.map_type)).setImageResource(R.drawable.change_button_change);
                    } else {
                        gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        ((ImageButton) findViewById(R.id.map_type)).setImageResource(R.drawable.change_button);
                    }
                }
                break;
            }

        }
    }


    private void getAddressByLatlng(double latitude, double longitude) {
        //逆地理编码查询条件：逆地理编码查询的地理坐标点、查询范围、坐标类型。
        LatLonPoint latLonPoint = new LatLonPoint(latitude, longitude);
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 500f, GeocodeSearch.AMAP);
        //异步查询
        mGeocodeSearch.getFromLocationAsyn(query);
    }


    //OnGeocodeSearchListener 的回调
    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        L.d(TAG, "onRegeocodeSearched: " + rCode + ": " + result);
        String poi = null;
        if (rCode == 1000) {
            if (result != null) {
                RegeocodeAddress regAddr = result.getRegeocodeAddress();
                if (regAddr != null) {
                    poi = regAddr.getFormatAddress();
                }
            }
        }
        if (tv_realTimeLocation != null && !TextUtils.isEmpty(poi)) {
            tv_realTimeLocation.setText(poi);
            if (is_home)
                addrInfo.address = poi;
            else
                addrInfo.name = poi;
            tv_realTimeLocation.setText(String.valueOf(poi + getResources().getString(R.string.location_show_near)));
        }
    }

    //OnGeocodeSearchListener 的回调
    @Override
    public void onGeocodeSearched(GeocodeResult result, int rCode) {
        String currentAddress = null;
        if (rCode == 1000) {
            if (result != null && result.getGeocodeAddressList() != null
                    && result.getGeocodeAddressList().size() > 0) {
                GeocodeAddress address = result.getGeocodeAddressList().get(0);
                currentAddress = address.getFormatAddress();
            }
        }
        if (tv_realTimeLocation != null && !TextUtils.isEmpty(currentAddress)) {
            tv_realTimeLocation.setText(currentAddress);
        }
    }

    @Override
    public void onGoogleRegeocode(GoogleRegeocodeTask task, Throwable throwable, int httpStatusCode, GoogleRegeocodeResult geocodeResult, String formattedAddress, String pointOfInterest) {
        if (tv_realTimeLocation != null) {
            String addr = "";
            if (geocodeResult != null && geocodeResult.getResults() != null && !geocodeResult.getResults().isEmpty()) {
                addr = geocodeResult.getResults().get(0).getFormatted_address();
            }
            tv_realTimeLocation.setText(addr);
            if (is_home) {
                addrInfo.address = addr;
            } else {
                addrInfo.name = addr;
            }
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        tv_realTimeLocation.setText(R.string.school_guide_address_ing);
    }


    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        showAddMarker();
        Utils.startJumpAnimation(addAmapMarker, aMap, this);
        LatLng latLng = cameraPosition.target;
        addrInfo.lat = latLng.latitude;
        addrInfo.lng = latLng.longitude;
        getAddressByLatlng(latLng.latitude, latLng.longitude);
    }

    @Override
    public void onCameraChange(com.google.android.gms.maps.model.CameraPosition cameraPosition) {
        showAddMarker();
        jumpPointGaoDeMarker(addGmapMarker);
        com.google.android.gms.maps.model.LatLng latLng = cameraPosition.target;
        addrInfo.lat = latLng.latitude;
        addrInfo.lng = latLng.longitude;

        GoogleRegeocodeTask task = new GoogleRegeocodeTask(this, this);
        task.execute(latLng.latitude, latLng.longitude);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (gmapView != null)
                gmapView.onResume();
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (amapView != null)
                amapView.onResume();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (gmapView != null)
                gmapView.onStart();
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (gmapView != null) {
                gmapView.onPause();
            }
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (amapView != null) {
                amapView.onPause();
            }
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (gmapView != null)
                gmapView.onSaveInstanceState(outState);
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (amapView != null)
                amapView.onSaveInstanceState(outState);
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (gmapView != null)
                gmapView.onDestroy();
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (amapView != null)
                amapView.onDestroy();
        }
    }

//    @Override TODO:
//    public void onLocationChanged(AMapLocation aMapLocation) {
//        if (aMapLocation != null) {
//            if (aMapLocation.getErrorCode() == 0) {
//                addrInfo.lat = aMapLocation.getLatitude();
//                addrInfo.lng = aMapLocation.getLongitude();
//                getAddressByLatlng(addrInfo.lat, addrInfo.lng);
//            }
//        }
//    }


    @Override
    public void onMyLocationChange(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    @Override
    protected void onGoogleInaccessible() {
        findViewById(R.id.google_inaccessible).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onGoogleAccessible() {
        findViewById(R.id.google_inaccessible).setVisibility(View.GONE);
    }

    @Override
    protected void onGoogleAccessibilityUnknown() {
        findViewById(R.id.google_inaccessible).setVisibility(View.GONE);
    }

}
