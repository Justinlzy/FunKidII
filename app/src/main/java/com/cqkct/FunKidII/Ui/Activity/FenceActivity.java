package com.cqkct.FunKidII.Ui.Activity;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.Bean.GuardianAddrInfo;
import com.cqkct.FunKidII.Bean.google.geocode.GoogleRegeocodeResult;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.view.WheelView;
import com.cqkct.FunKidII.Utils.Constants;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.StringUtils;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLngBounds;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import protocol.Message;

/**
 * 显示某个围栏或设置某个围栏的界面
 */
public class FenceActivity extends BaseMapActivity implements
        AMap.OnCameraChangeListener, GeocodeSearch.OnGeocodeSearchListener,
        PoiSearch.OnPoiSearchListener,
        AMap.OnMyLocationChangeListener,
        LocationListener,
        GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMyLocationChangeListener, OnMapReadyCallback,
        BaseMapActivity.GoogleRegeocodeResultListener {
    private static final String TAG = FenceActivity.class.getSimpleName();
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    public static final String SELECT_FENCE_ADDRESS = "SELECT_FENCE_ADDRESS";

    public static final LatLng BEIJING = new LatLng(39.90403, 116.407525);// 北京市经纬度

    private MapView amapView = null;
    private AMap aMap = null;
    private com.google.android.gms.maps.MapView gmapView;
    private GoogleMap gMap = null;

    public GuardianAddrInfo addrInfo;

    private Marker fenceAmapPointMarker = null;
    private com.google.android.gms.maps.model.Marker fenceGmapPointMarker = null;
    private Circle mAmapRadiusCircle = null;
    private com.google.android.gms.maps.model.Circle mGmapRadiusCircle = null;
    private BitmapDescriptor amapDescriptor = null;
    private com.google.android.gms.maps.model.BitmapDescriptor gmapDescriptor = null;
    private TextView addressTextView;
    private GeocodeSearch mGeocodeSearch;
    private List<GuardianAddrInfo> addressList = new ArrayList<>();
    private boolean hasEditPermission;
    private ImageView btSearch;
    private RelativeLayout ll_search;
    private boolean ADD_MODE = false;
    private List<String> stringList;

    private int FENCE_RADIUS_MIN = 200;
    private int FENCE_RADIUS_DEF = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fence);
        Intent fenceIntent = getIntent();
        if (fenceIntent == null) {
            L.e(TAG, "onCreate getIntent() return null");
            finish();
            return;
        }

        hasEditPermission = hasEditPermission();

        addrInfo = (GuardianAddrInfo) fenceIntent.getSerializableExtra(FenceAddressActivity.FENCE_ADDRESS_GUARDIANADDRINFO);
        ADD_MODE = fenceIntent.getBooleanExtra(FenceAddressActivity.ADD_MODE, false);
        if (addrInfo == null) {
            L.e(TAG, "onCreate getIntent() INTENT_PRARM_fenceEntity Extra is null");
            finish();
            return;
        }

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

        addressTextView = findViewById(R.id.addressTextView);
        WheelView wheelRadius = findViewById(R.id.wheel_radius);
        wheelRadius.setAdditionCenterMark("m");
        stringList = StringUtils.getWheelRadius(FENCE_RADIUS_MIN);
        wheelRadius.setItems(stringList);
        for (int i = 0; i < stringList.size(); i++) {
            if (stringList.get(i).equals(String.valueOf(addrInfo.radius))) {
                wheelRadius.selectIndex(i);
                break;
            }
        }
        wheelRadius.setOnWheelItemSelectedListener(new WheelView.OnWheelItemSelectedListener() {
            @Override
            public void onWheelItemChanged(WheelView wheelView, int position) {

            }

            @Override
            public void onWheelItemSelected(WheelView wheelView, int position) {
                addrInfo.radius = Integer.parseInt(stringList.get(position));
                refreshRadio(addrInfo.radius);
            }
        });

        if (hasEditPermission) {
            findViewById(R.id.title_bar_right_text).setVisibility(View.VISIBLE);
            TextView okText = findViewById(R.id.title_bar_right_text);
            okText.setText(R.string.save);
        }

        if (mMapType == Constants.MAP_TYPE_AMAP) {
            amapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.fence_location_icon);
            if (hasEditPermission) {
                ll_search = findViewById(R.id.ll_search);
                ll_search.setVisibility(View.VISIBLE);
            }
            //初始化地图
            initAMap(savedInstanceState);
            initMarkerDisplay();
        } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (hasEditPermission) {
                btSearch = findViewById(R.id.iv_search_google);
                btSearch.setVisibility(View.VISIBLE);

                gmapDescriptor = com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(R.drawable.fence_location_icon);
            }
            initGMap(savedInstanceState);
        }

        setTitleBarTitle(getString(R.string.fence_add_area_set));
    }

    private void initMarkerDisplay() {
        if (ADD_MODE) {
            addrInfo.radius = FENCE_RADIUS_DEF;
            //第一步 获取本地保存的最后一次位置信息
            Message.Position position = DeviceInfo.getLastPosition(mDeviceId);
            if (position != null) {
                addrInfo.lat = position.getLatLng().getLatitude();
                addrInfo.lng = position.getLatLng().getLongitude();
                addMarkToMap(addrInfo.lat, addrInfo.lng);
            } else {
                mXHandler.sendMessageDelayed(mXHandler.obtainMessage(XHandler.SET_LAT_LON, Message.LatLon.newBuilder().setLatitude(BEIJING.latitude).setLongitude(BEIJING.longitude).build()), 1500L);
            }
        } else {
            if (hasEditPermission) {
                addMarkToMap(addrInfo.lat, addrInfo.lng);
            } else {
                displayCurrentFence();
            }
        }
    }

    private void addMarkToMap(double lat, double lon) {
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (gMap == null)
                return;
            com.google.android.gms.maps.model.LatLng cenp = new com.google.android.gms.maps.model.LatLng(lat, lon);
            addMarkersToMap(lat, lon);
            gMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(cenp, 14.0f));// 方法设置地图的可视区域。

            gMap.setOnCameraChangeListener(this);
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            LatLng cenp = new LatLng(lat, lon);
            addMarkersToMap(lat, lon);
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cenp, 14.0f));// 方法设置地图的可视区域。
            aMap.setOnCameraChangeListener(this);
        }

    }


    //初始化地图
    private void initAMap(Bundle savedInstanceState) {
        amapView = (MapView) findViewById(R.id.amap_fence_manager);
        amapView.setVisibility(View.VISIBLE);
        amapView.onCreate(savedInstanceState); // 管理地图的生命周期
        aMap = amapView.getMap();
        setAMapLanguage(aMap);
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE); // 定位一次，且将视角移动到地图中心点。
        myLocationStyle.showMyLocation(false);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setOnMyLocationChangeListener(this);


        UiSettings uiSettings = aMap.getUiSettings();
        aMap.getUiSettings().setScaleControlsEnabled(true); // 显示比例尺
        uiSettings.setRotateGesturesEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);

        mGeocodeSearch = new GeocodeSearch(this);
        mGeocodeSearch.setOnGeocodeSearchListener(this);
    }

    private void initGMap(Bundle savedInstanceState) {
        gmapView = findViewById(R.id.gmap_fence_manager);
        gmapView.setVisibility(View.VISIBLE);
        gmapView.onCreate(savedInstanceState); // 管理地图的生命周期
        if (gMap == null) {
            gmapView.getMapAsync(this);
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
        //手机主动定位
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && (ActivityCompat.checkSelfPermission(FenceActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(FenceActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 10, FenceActivity.this);
        }

        initMarkerDisplay();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (gMap == null)
            return;
        mXHandler.obtainMessage(XHandler.SET_LAT_LON, Message.LatLon.newBuilder().setLatitude(location.getLatitude()).setLongitude(location.getLongitude()).build()).sendToTarget();
    }


    private void addMarkersToMap(double latitude, double longitude) {
        if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (fenceAmapPointMarker != null) {
                fenceAmapPointMarker.remove();
            }
            if (mAmapRadiusCircle != null) {
                mAmapRadiusCircle.remove();
            }
            LatLng cenpt = new LatLng(latitude, longitude);
            MarkerOptions markerOption = new MarkerOptions()
                    .icon(amapDescriptor)
                    .setFlat(true)
                    .draggable(false)
                    .position(cenpt);
            fenceAmapPointMarker = aMap.addMarker(markerOption);
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cenpt, 14.0f));// 方法设置地图的可视区域。
            mAmapRadiusCircle = aMap.addCircle(new CircleOptions().center(cenpt)
                    .radius(addrInfo.radius)
                    .strokeColor(Constants.FENCE_STROKE_COLOR)
                    .fillColor(Constants.FENCE_FILL_COLOR));
        } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (gMap == null)
                return;
            if (fenceGmapPointMarker != null) {
                fenceGmapPointMarker.remove();
            }
            if (mGmapRadiusCircle != null) {
                mGmapRadiusCircle.remove();
            }
            com.google.android.gms.maps.model.LatLng cenpt = new com.google.android.gms.maps.model.LatLng(latitude, longitude);
            com.google.android.gms.maps.model.MarkerOptions markerOption = new com.google.android.gms.maps.model.MarkerOptions()
                    .icon(gmapDescriptor)
                    .draggable(false)
                    .flat(true)
                    .position(cenpt);
            fenceGmapPointMarker = gMap.addMarker(markerOption);
            gMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(cenpt, 14.0f));// 方法设置地图的可视区域。
            mGmapRadiusCircle = gMap.addCircle(new com.google.android.gms.maps.model.CircleOptions().center(cenpt)
                    .radius(addrInfo.radius)
                    .strokeColor(Constants.FENCE_STROKE_COLOR)
                    .fillColor(Constants.FENCE_FILL_COLOR));
        }

    }


    private void displayCurrentFence() {
        protocol.Message.LatLon gpsInfo = Message.LatLon.newBuilder().setLatitude(addrInfo.lat).setLongitude(addrInfo.lng).build();
        if (gpsInfo == null) {
            L.e(TAG, "no fence (gpsInfo == null)");
            return;
        }
        if (addrInfo == null) {
            L.e(TAG, "no fence (currentFence == null)");
            return;
        }

        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (gMap == null)
                return;
            gMap.clear();
            com.google.android.gms.maps.model.LatLng latLng = new com.google.android.gms.maps.model.LatLng(gpsInfo.getLatitude(), gpsInfo.getLongitude());
            geocode(latLng);
            addMarkersToMap(gpsInfo.getLatitude(), gpsInfo.getLongitude());
            L.e(TAG, "show locate：" + gpsInfo.getLatitude() + " ," + gpsInfo.getLongitude());
            gMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latLng, 14.0f));// 方法设置地图的可视区域。
            gMap.addCircle(new com.google.android.gms.maps.model.CircleOptions().center(latLng)
                    .radius(addrInfo.radius)
                    .strokeColor(Constants.FENCE_STROKE_COLOR)
                    .fillColor(Constants.FENCE_FILL_COLOR));

        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            aMap.clear();

            // 设定中心点坐标
            // currentFenceTextView.setText(String.valueOf("范围" + Integer.toString(mFenceBuilder.getShape().getRound().getRadius()) + "m"));
            getAddress(gpsInfo.getLatitude(), gpsInfo.getLongitude());
            LatLng cenpt = new LatLng(gpsInfo.getLatitude(), gpsInfo.getLongitude());
            addMarkersToMap(gpsInfo.getLatitude(), gpsInfo.getLongitude());
            L.e(TAG, "show locate：" + gpsInfo.getLatitude() + " ," + gpsInfo.getLongitude());
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cenpt, 14.0f));// 方法设置地图的可视区域。
            aMap.addCircle(new CircleOptions().center(cenpt)
                    .radius(addrInfo.radius)
                    .strokeColor(Constants.FENCE_STROKE_COLOR)
                    .fillColor(Constants.FENCE_FILL_COLOR));

        }

    }

    private void refreshRadio(double radio) {
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (gMap == null)
                return;
            if (mGmapRadiusCircle != null) {
                mGmapRadiusCircle.remove();
            }
            mGmapRadiusCircle = gMap.addCircle(new com.google.android.gms.maps.model.CircleOptions().center(fenceGmapPointMarker.getPosition())
                    .radius(radio)
                    .strokeColor(Constants.FENCE_STROKE_COLOR)
                    .fillColor(Constants.FENCE_FILL_COLOR)
                    .strokeWidth(5.0f));
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (mAmapRadiusCircle != null) {
                mAmapRadiusCircle.remove();
            }
            mAmapRadiusCircle = aMap.addCircle(new CircleOptions().center(fenceAmapPointMarker.getPosition())
                    .radius(radio)
                    .strokeColor(Constants.FENCE_STROKE_COLOR)
                    .fillColor(Constants.FENCE_FILL_COLOR)
                    .strokeWidth(5.0f));

        }

    }

    public void onTitleBarClick(View view) {
        super.onTitleBarClick(view);
        super.onDebouncedClick(view);
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.title_bar_right_text: {
                if (addrInfo.lat == 0 || addrInfo.lng == 0) {
                    if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                        if (gMap == null || fenceGmapPointMarker == null)
                            break;
                        addrInfo.lat = fenceGmapPointMarker.getPosition().latitude;
                        addrInfo.lng = fenceGmapPointMarker.getPosition().longitude;

                    } else if (mMapType == Constants.MAP_TYPE_AMAP) {
                        addrInfo.lat = fenceAmapPointMarker.getPosition().latitude;
                        addrInfo.lng = fenceAmapPointMarker.getPosition().longitude;
                    }

                }
                Intent intent = new Intent();
                intent.putExtra(SELECT_FENCE_ADDRESS, addrInfo);
                setResult(RESULT_OK, intent);
                this.finish();
            }
            break;

            case R.id.iv_search_google:
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                try {
                    com.google.android.gms.maps.model.LatLng latLng = null;
                    if (addrInfo != null) {
                        protocol.Message.LatLon latLon = Message.LatLon.newBuilder().setLatitude(addrInfo.lat).setLongitude(addrInfo.lng).build();
                        if (latLon.getLatitude() != 0 && latLon.getLongitude() != 0) {
                            latLng = new com.google.android.gms.maps.model.LatLng(latLon.getLatitude(), latLon.getLongitude());
                        }
                        if (latLng == null) {
                            String deviceId = mDeviceId;
                            if (!TextUtils.isEmpty(deviceId)) {
                                Message.Position lastPosition = DeviceInfo.getLastPosition(deviceId);
                                if (lastPosition != null) {
                                    latLng = new com.google.android.gms.maps.model.LatLng(lastPosition.getLatLng().getLatitude(), lastPosition.getLatLng().getLongitude());
                                    if (latLng.latitude == 0 && latLng.longitude == 0) {
                                        latLng = null;
                                    }
                                }
                            }
                        }
                    }
                    if (latLng != null)
                        builder.setLatLngBounds(LatLngBounds.builder().include(latLng).build());
                    Intent intent = builder.build(this);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.map_type: {
                if (mMapType == Constants.MAP_TYPE_AMAP) {
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
            }
            break;
//            case R.id.bt_cancel_search: {
//                cl_fenceList.setVisibility(View.GONE);
//                et_search.setText("");
//                et_search.setFocusable(false);
//                et_search.setFocusableInTouchMode(true);
//                btCancelSearch.setVisibility(View.INVISIBLE);
//                PublicTools.hideInputMethod(this);
//            }
//            break;

            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.i(TAG, "Place: " + place.getName());
                com.google.android.gms.maps.model.LatLng latLng = place.getLatLng();
                addMarkersToMap(latLng.latitude, latLng.longitude);
                String address = place.getAddress().toString();
                if (!TextUtils.isEmpty(address)) {
                    addressTextView.setText(address);
                    addrInfo.address = address;
                }
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.i(TAG, status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
                // TODO: Handle the CANCELED.
            }
        }
    }

    //OnGeocodeSearchListener 的回调
    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        L.d(TAG, "onRegeocodeSearched: " + rCode + ": " + result + ",  longitude:" + fenceAmapPointMarker.getPosition().longitude
                + ", latitude: " + fenceAmapPointMarker.getPosition().latitude);
        String poi = null;
        if (rCode == 1000) {
            if (result != null) {
                RegeocodeAddress regAddr = result.getRegeocodeAddress();
                if (regAddr != null) {
                    if (regAddr.getPois() != null && !regAddr.getPois().isEmpty()) {
                        poi = regAddr.getPois().get(0).getTitle();
                    } else {
                        poi = regAddr.getFormatAddress();
                    }
                }
            }
        }
        if (addressTextView != null && !TextUtils.isEmpty(poi)) {
            addressTextView.setText(poi);
            addrInfo.address = poi;
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
        if (addressTextView != null && !TextUtils.isEmpty(currentAddress)) {
            addressTextView.setText(currentAddress);
            addrInfo.address = currentAddress;
        }
    }

    @Override
    public void onGoogleRegeocode(GoogleRegeocodeTask task, Throwable throwable, int httpStatusCode, GoogleRegeocodeResult geocodeResult, String formattedAddress, String pointOfInterest) {
        if (addressTextView != null) {
            if (geocodeResult != null && geocodeResult.getResults() != null && !geocodeResult.getResults().isEmpty()) {
                String address = geocodeResult.getResults().get(0).getFormatted_address();
                addressTextView.setText(address);
                addrInfo.address = address;
            } else {
                addressTextView.setText(R.string.unknown);
                addrInfo.address = getString(R.string.unknown);
            }
        }
    }

    //获取坐标对应的地理信息
    private void getAddress(double lat, double lon) {
        RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(lat, lon), 200, GeocodeSearch.AMAP);// 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        mGeocodeSearch.getFromLocationAsyn(query);// 设置同步逆地理编码请求
    }

    @Override
    public void onDeviceUnbind(TlcService tlcService, @Nullable Pkt reqPkt, @NonNull protocol.Message.NotifyUserUnbindDevReqMsg reqMsg) {
        if (reqMsg.getUsrDevAssoc().getUserId().equals(mUserId) && reqMsg.getUsrDevAssoc().getDeviceId().equals(mDeviceId)) {
            L.w(TAG, "current device unbound!!! Activity finish");
            finish();
        }
    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int i) {
        ArrayList<PoiItem> pois = poiResult.getPois();

        for (PoiItem poi : pois) {
            String schoolName = poi.getTitle();
            String schoolAddress = poi.getSnippet();
            GuardianAddrInfo addr = new GuardianAddrInfo();
            addr.name = schoolName;
            addr.address = schoolAddress;
            LatLonPoint point = poi.getLatLonPoint();
            if (TextUtils.isEmpty(poi.getTitle()))
                continue;
            addr.lat = point.getLatitude();
            addr.lng = point.getLongitude();
            addressList.add(addr);
            L.d(TAG, "schoolName" + schoolName + "schoolAddress: " + schoolAddress);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
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
    public void onPause() {
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

    @Override
    public void onDestroy() {
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (gmapView != null) {
                gmapView.onDestroy();
            }
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (amapView != null) {
                amapView.onDestroy();
            }
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (gmapView != null)
                gmapView.onSaveInstanceState(outState);
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (amapView != null)
                amapView.onSaveInstanceState(outState);
        }

    }

    //OnCameraChangeListener 的回调
    @Override
    public void onCameraChange(CameraPosition position) {//地图移动过程中的回调
        // TODO Auto-generated method stub
    }

    //OnCameraChangeListener 的回调
    @Override
    public void onCameraChangeFinish(CameraPosition arg0) {//地图移动完的回调
        aMap.clear();
        fenceAmapPointMarker = aMap.addMarker(new MarkerOptions().icon(
                BitmapDescriptorFactory.fromResource(R.drawable.fence_location_icon))
                .draggable(false).anchor(0.49f, 0.89f));
        int mapWidth = amapView.getWidth();
        int mapHeight = amapView.getHeight();
        fenceAmapPointMarker.setPositionByPixels(mapWidth / 2, mapHeight / 2);
        addrInfo.lat = fenceAmapPointMarker.getPosition().latitude;
        addrInfo.lng = fenceAmapPointMarker.getPosition().longitude;

        mAmapRadiusCircle = aMap.addCircle(new CircleOptions().center(fenceAmapPointMarker.getPosition())
                .radius(addrInfo.radius)
                .strokeColor(Constants.FENCE_STROKE_COLOR)
                .fillColor(Constants.FENCE_FILL_COLOR)
                .strokeWidth(5.0f));

        getAddress(fenceAmapPointMarker.getPosition().latitude, fenceAmapPointMarker.getPosition().longitude);
    }

    @Override
    public void onCameraChange(com.google.android.gms.maps.model.CameraPosition cameraPosition) {
        if (gMap == null)
            return;
        gMap.clear();
        fenceGmapPointMarker = gMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(R.drawable.fence_location_icon))
                .draggable(false)
                .position(gMap.getCameraPosition().target)
                .anchor(0.49f, 0.89f));
        addrInfo.lat = fenceGmapPointMarker.getPosition().latitude;
        addrInfo.lng = fenceGmapPointMarker.getPosition().longitude;
        mGmapRadiusCircle = gMap.addCircle(new com.google.android.gms.maps.model.CircleOptions().center(fenceGmapPointMarker.getPosition())
                .radius(addrInfo.radius)
                .strokeColor(Constants.FENCE_STROKE_COLOR)
                .fillColor(Constants.FENCE_FILL_COLOR)
                .strokeWidth(5.0f));
        geocode(new com.google.android.gms.maps.model.LatLng(fenceGmapPointMarker.getPosition().latitude, fenceGmapPointMarker.getPosition().longitude));
    }

    private void geocode(com.google.android.gms.maps.model.LatLng latLng) {
        GoogleRegeocodeTask task = new GoogleRegeocodeTask(this, this);
        task.execute(latLng.latitude, latLng.longitude);
    }

    @Override
    public void onMyLocationChange(Location location) {
        if (aMap == null)
            return;
        mXHandler.obtainMessage(XHandler.SET_LAT_LON, Message.LatLon.newBuilder().setLatitude(location.getLatitude()).setLongitude(location.getLongitude()).build()).sendToTarget();
    }

    private XHandler mXHandler = new XHandler(this);





    private static class XHandler extends Handler {
        static final int SET_LAT_LON = 1;

        private WeakReference<FenceActivity> mA;

        XHandler(FenceActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            FenceActivity a = mA.get();
            if (a == null)
                return;
            if (a.isFinishing())
                return;

            switch (msg.what) {
                case SET_LAT_LON:
                    if (a.addrInfo.lat == 0 || a.addrInfo.lng == 0) {
                        Message.LatLon latLng = (Message.LatLon) msg.obj;
                        a.addrInfo.lat = latLng.getLatitude();
                        a.addrInfo.lng = latLng.getLongitude();
                        a.addMarkToMap(a.addrInfo.lat, a.addrInfo.lng);
                    }
                    break;
            }
        }
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

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
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


}
