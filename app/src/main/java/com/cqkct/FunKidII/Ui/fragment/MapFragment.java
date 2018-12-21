package com.cqkct.FunKidII.Ui.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.view.animation.Interpolator;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.CoordinateConverter;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.Bean.UmengEvent;
import com.cqkct.FunKidII.Bean.google.geocode.GoogleRegeocodeResult;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.LocationRecordActivity;
import com.cqkct.FunKidII.Ui.Activity.MessageBabiesListActivity;
import com.cqkct.FunKidII.Ui.Adapter.BabyListAdapter;
import com.cqkct.FunKidII.Ui.Listener.DebouncedOnClickListener;
import com.cqkct.FunKidII.Ui.view.BatteryView;
import com.cqkct.FunKidII.Ui.view.MyDividerItemDecoration;
import com.cqkct.FunKidII.Utils.AndroidPermissions;
import com.cqkct.FunKidII.Utils.Constants;
import com.cqkct.FunKidII.Utils.DensityUtils;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.LatLon;
import com.cqkct.FunKidII.Utils.NavigationUtils;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Dao.NotifyMessageEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.glide.DeviceAvatar;
import com.cqkct.FunKidII.glide.GlideDefines;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.ExecEntity;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.umeng.analytics.MobclickAgent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import protocol.Message;

import static com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds;


/**
 * Created by justin on 2017/9/16.
 */

public class MapFragment extends BaseMapFragment implements
        View.OnClickListener,
        //AMap
        CompoundButton.OnCheckedChangeListener,
        AMap.OnMyLocationChangeListener,
        AMap.OnMarkerClickListener,
        AMap.OnMapClickListener,
        AMap.InfoWindowAdapter,
        AMap.OnCameraChangeListener,
        //GoogleMap
        LocationListener,
        GeocodeSearch.OnGeocodeSearchListener, OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.InfoWindowAdapter,
        GoogleMap.OnCameraIdleListener, GoogleMap.OnMyLocationChangeListener,

        BaseMapFragment.GoogleRegeocodeResultListener {
    private static final String TAG = MapFragment.class.getSimpleName();

    public static final LatLng BEIJING = new LatLng(39.90403, 116.407525);// 北京市经纬度

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION_TEHN_LOCATE = 2;
    private static final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 3;
    private static final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION_TEHN_LOCATE = 4;

    private static final int ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_LOCATION_PERMISSION = 1;
    private static final int ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_ENABLE_LOCATION_SERVICE = 2;

    private static final long DEVICE_LOCATE_TIMEOUT = 1000L * 90 * 1; // 等待定位 S3 的超时时间
    private static final long REFRESH_KID_LOCATION_MIN_TIME_SEC = 30;

    private ImageView mBabyHeadIconView;
    private TextView mBabyNameView;
    private BatteryView mBabyBatteryView;
    private View mBabyStepCountLayout;
    private TextView mBabyStepCountView;
    private TextView mBabyOnlineStatusView;
    private View mEnvelopeBadge;

    // 高德地图
    private TextureMapView amapMapView;
    private AMap mAMap;
    //google地图
    private com.google.android.gms.maps.MapView googleMapView;
    private com.google.android.gms.maps.GoogleMap googleMap = null;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    private float mScalePerPixel;

    private GeocodeSearch mGeocodeSearch;

    private Location phoneLocation;
    private boolean systemLocationServiceRequested;
    private Location kidLocation;
    ShowMarkerAnimationHandler handler = new ShowMarkerAnimationHandler(this);

    private ToggleButton phoneLocateToggleButton;

    private View cannotLocateTipView;
    private View googleInaccessible;
    private ExecEntity locateDeviceExecEntity;
    private View rootView;

    private View mAddrCard;

    private boolean initPositionCalled; // initPosition 调用标记，避免引起 Fragment  not attached to Activity 异常
    private RecyclerView mBabyRecyclerView;
    private List<BabyEntity> mBabyEntities = new ArrayList<>();
    private BabyListAdapter mBabyListAdapter;
    private RelativeLayout title_bar;
    private View contentView;
    private PopupWindow popupWindow;

    @Override
    @javax.annotation.Nullable
    public View onCreateView(LayoutInflater inflater,
                             @javax.annotation.Nullable ViewGroup container, @javax.annotation.Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.map, container, false);//关联布局文件

        initView(rootView, savedInstanceState);
        List<String> locationPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            locationPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!locationPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(getActivity(), locationPermissions.toArray(new String[0]), 0);
        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (googleMapView != null) {
                googleMapView.onStart();
                if (googleMap != null) {
                    monitorPhoneLocationChange();
                }
            }
        }

        // initPosition 放到这里来调用，避免引起 Fragment  not attached to Activity 异常
        if (!initPositionCalled) {
            initPositionCalled = true;
            initPosition();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (googleMapView != null)
                googleMapView.onResume();
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (amapMapView != null)
                amapMapView.onResume();
        }
        mScalePerPixel = getScalePerPixel();

        showBabyName();
        showBabyAvatar();
        showOnlineStatus();
        showEnvelopeBadge();
        showDeviceSensorData();
        handler.refreshKidPosition();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (googleMapView != null) {
                googleMapView.onPause();
            }
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (amapMapView != null) {
                amapMapView.onPause();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        unmonitorPhoneLocationChange();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
            case PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    toast(R.string.no_location_permission);
                }
                break;
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION_TEHN_LOCATE:
            case PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION_TEHN_LOCATE:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    toast(R.string.no_location_permission);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_LOCATION_PERMISSION:

                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    toast(R.string.no_location_permission);
                }
                break;
            case ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_ENABLE_LOCATION_SERVICE:
                break;
            default:
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (googleMapView != null)
                googleMapView.onSaveInstanceState(outState);
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (amapMapView != null)
                amapMapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onDestroy() {
        if (mCircleBreatheTimer != null) {
            mCircleBreatheTimer.cancel();
            mCircleBreatheTimer = null;
        }
        if (kidLocation != null) {
            kidLocation.hideAddressCard();
            kidLocation.hideMarker();
        }
        if (phoneLocation != null) {
            phoneLocation.hideAddressCard();
            phoneLocation.hideMarker();
        }
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (googleMapView != null)
                googleMapView.onDestroy();
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (amapMapView != null)
                amapMapView.onDestroy();
        }
        super.onDestroy();
    }

    private void initView(View view, Bundle savedInstanceState) {
        title_bar = view.findViewById(R.id.title_bar);
        mBabyHeadIconView = view.findViewById(R.id.baby_head_icon);
        mBabyNameView = view.findViewById(R.id.baby_name);
        mBabyBatteryView = view.findViewById(R.id.baby_battery);
        mBabyStepCountLayout = view.findViewById(R.id.baby_step_count_layout);
        mBabyStepCountView = view.findViewById(R.id.baby_step_count);
        mBabyOnlineStatusView = view.findViewById(R.id.baby_online_status);
        mEnvelopeBadge = view.findViewById(R.id.envelope_badge);

        amapMapView = view.findViewById(R.id.map_amap);
        googleMapView = view.findViewById(R.id.map_google);
        phoneLocateToggleButton = view.findViewById(R.id.locate_self);
        phoneLocateToggleButton.setOnCheckedChangeListener(this);

        cannotLocateTipView = view.findViewById(R.id.cannot_locate_tip_view);
        googleInaccessible = view.findViewById(R.id.google_inaccessible);

        mAddrCard = view.findViewById(R.id.map_address_card_view);

        if (mMapType == Constants.MAP_TYPE_GOOGLE) {//google地图的时候  显示 google地图所在布局
            if (googleMapView.getVisibility() == View.GONE) {
                googleMapView.setVisibility(View.VISIBLE);
                amapMapView.setVisibility(View.GONE);
            }
            googleMapView.onCreate(savedInstanceState);
            if (googleMap == null)
                googleMapView.getMapAsync(this);
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {//高德地图的时候 不显示google地图所在布局
            if (googleMapView.getVisibility() == View.VISIBLE) {
                googleMapView.setVisibility(View.GONE);
            }
            amapMapView.onCreate(savedInstanceState); // 管理地图的生命周期
            if (mAMap == null)
                mAMap = amapMapView.getMap();
            setAMapLanguage(mAMap);
            initLocationView();
        }

        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
        long func_module = 0;
        if (deviceInfo != null) {
            func_module = deviceInfo.getDeviceEntity().getFuncModuleInfo().getFuncModule();
        }

        mAddrCard.setOnClickListener(v -> { /* do noting, but do not delete this */ });

        view.findViewById(R.id.baby_head_icon_cl).setOnClickListener(this);
        view.findViewById(R.id.envelope_view).setOnClickListener(this);
        ImageButton imageButton = view.findViewById(R.id.location_history);
        imageButton.setVisibility((func_module & Message.FuncModule.FM_LOCATION_RECORD_VALUE) != 0 ? View.VISIBLE : View.GONE);
        imageButton.setOnClickListener(this);
        view.findViewById(R.id.locate_device).setOnClickListener(this);
        view.findViewById(R.id.map_type).setOnClickListener(this);
        view.findViewById(R.id.map_zoom_in).setOnClickListener(this);
        view.findViewById(R.id.map_zoom_out).setOnClickListener(this);

        contentView = LayoutInflater.from(getContext()).inflate(R.layout.popupwindow_baby_list, null);
        mBabyRecyclerView = contentView.findViewById(R.id.baby_list_cy);

        if (!mBabyEntities.isEmpty()) {
            mBabyEntities.clear();
        }
        mBabyEntities = getClipViewPagerData();

        mBabyListAdapter = new BabyListAdapter(mBabyEntities, pos -> {
            BabyEntity entity = mBabyEntities.get(pos);
            GreenUtils.selectBaby(entity.getUserId(), entity.getDeviceId());
            mBabyListAdapter.onNotifyDataSetChanged(getClipViewPagerData());
        }, mBabyRecyclerView, getContext());
        mBabyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mBabyRecyclerView.setAdapter(mBabyListAdapter);
        MyDividerItemDecoration itemDecoration = new MyDividerItemDecoration(getContext(), new LinearLayoutManager(getContext())
                .getOrientation(), false);
        mBabyRecyclerView.addItemDecoration(itemDecoration);
//        mBabyRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
    }

    /**
     * 初始化定位
     */
    private void initLocationView() {
        // 自定义系统定位小蓝点--我的位置
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        myLocationStyle.interval(3000); // 设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.location_red));// 设置小蓝点的图标
        myLocationStyle.strokeColor(getResources().getColor(R.color.blue));// 设置圆形的边框颜色
        myLocationStyle.radiusFillColor(Constants.MAP_MARK_COLOR);// 设置圆形的填充颜色
        myLocationStyle.strokeWidth(1.0f);// 设置圆形的边框粗细
        myLocationStyle.showMyLocation(false);
        mAMap.setMyLocationStyle(myLocationStyle);
        mAMap.setOnMapClickListener(this);
        mAMap.setOnMyLocationChangeListener(this);
        mAMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        // 自定义InfoWindow
        mAMap.setOnMarkerClickListener(this);
        mAMap.setInfoWindowAdapter(this);
        mAMap.setOnCameraChangeListener(this);

        mAMap.getUiSettings().setZoomControlsEnabled(false); // 隐藏自带缩放控件
        mAMap.getUiSettings().setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
//        mAMap.getUiSettings().setLogoBottomMargin(-50);// 隐藏logo
        mAMap.getUiSettings().setScaleControlsEnabled(true); // 显示比例尺
        mScalePerPixel = mAMap.getScalePerPixel();

        mGeocodeSearch = new GeocodeSearch(getContext());
        mGeocodeSearch.setOnGeocodeSearchListener(this);

        kidLocation = new Location(mAMap, R.drawable.location_red, 42f / 83f, 64f / 92f, true);
        phoneLocation = new Location(mAMap, R.drawable.location_yellow, 42f / 83f, 82f / 111f, false);
    }

    /**
     * google 地图初始化完成
     */
    @Override
    public void onMapReady(GoogleMap gMap) {
        MapFragment.this.googleMap = gMap;
//        com.google.android.gms.maps.UiSettings uiSettings = gMap.getUiSettings();
//        uiSettings.setZoomControlsEnabled(true);
//        uiSettings.setRotateGesturesEnabled(false);

        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.setInfoWindowAdapter(this);
        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnCameraIdleListener(this);
        googleMap.setOnMapClickListener(this);
        googleMap.setOnMyLocationChangeListener(this);

        mScalePerPixel = getScalePerPixel();

        kidLocation = new Location(googleMap, R.drawable.location_red, 42f / 83f, 64f / 92f, true);
        phoneLocation = new Location(googleMap, R.drawable.location_yellow, 42f / 83f, 82f / 111f, false);
        //手机主动定位
        monitorPhoneLocationChange();
    }

    private synchronized void monitorPhoneLocationChange() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        }
        if (mLocationManager == null)
            return;
        if (!(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)) {
            // 无权限
            return;
        }
        if (mLocationListener != null) {
            return;
        }
        mLocationListener = this;
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 10, mLocationListener);
    }

    private synchronized void unmonitorPhoneLocationChange() {
        if (mLocationManager != null && mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
            mLocationListener = null;
        }
    }

    private void initPosition() {
        // 使用本地缓存的位置信息
        Message.Position position = DeviceInfo.getLastPosition(mDeviceId);
        if (position != null && position.getTime() != 0) {
            handler.postKidLocation(position);
        }
        // 向服务器查询最新位置
        if (Math.abs(System.currentTimeMillis() / 1000L - DeviceInfo.getLastLocateTime(mDeviceId)) > REFRESH_KID_LOCATION_MIN_TIME_SEC) {
            // 静默主动定位手表
            handler.postLocateDevice();
        }
    }

    private float getScalePerPixel() {
        switch (mMapType) {
            case Constants.MAP_TYPE_AMAP:
                return mAMap.getScalePerPixel();
            case Constants.MAP_TYPE_GOOGLE:
                if (googleMap == null)
                    return 0;
                try {
                    com.google.android.gms.maps.model.CameraPosition cameraPosition = googleMap.getCameraPosition();
                    com.google.android.gms.maps.model.LatLng latLng = cameraPosition.target;
                    float zoom = cameraPosition.zoom;
                    return (float) ((Math.cos(latLng.latitude * Math.PI / 180.0) * 2.0 * Math.PI * 6378137.0 / (256.0 * Math.pow(2, zoom))) * 0.5/*FIXME: ????*/);
                } catch (Exception e) {
                    return 0;
                }
        }
        return 0;
    }

    private void onKidLocation(Message.Position position) {
        if (kidLocation != null) {
            kidLocation.setPosition(position);
            if (!phoneLocateToggleButton.isChecked() && (kidLocation.addrCard == null)) {
                kidLocation.showAddressCard();
            }
        }
        if (phoneLocateToggleButton.isChecked()) {
            boundKidAndPhoneMarkers();
        } else if (kidLocation != null) {
            if (mMapType == Constants.MAP_TYPE_AMAP) {
                if (mAMap == null || kidLocation.latLon == null) {
                    return;
                }
                mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(kidLocation.latLon.lat, kidLocation.latLon.lon), 15.0f));
            } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                if (googleMap == null || kidLocation.latLon == null)
                    return;
                googleMap.animateCamera(
                        com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                new com.google.android.gms.maps.model.LatLng(kidLocation.latLon.lat, kidLocation.latLon.lon), 15.0f));
            }
        }
    }

    private void showKidPosition() {
        if (kidLocation == null)
            return;
        Message.Position position = null;
        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
        if (deviceInfo != null) {
            position = deviceInfo.getLastPosition();
        }
        if (position == null) {
            kidLocation.clear();
            if (phoneLocateToggleButton.isChecked()) {
                boundKidAndPhoneMarkers();
            } else {
                aMapMoveCameraToKidPosition();
            }
            return;
        }
        kidLocation.setShouldShowMarker(true);
        handler.postKidLocation(position);
    }

    /**
     * 标记 {@link #firstGetFromDeviceDeviceIfShould} 是否已经调用过
     */
    private boolean firstGetFromDeviceDeviceIfShouldFlag = true;

    /**
     * 进入页面后，根据情况自动查询手表的当前位置
     *
     * @return 不需要查询手表的当前位置返回 false
     */
    private boolean firstGetFromDeviceDeviceIfShould(long locateTime) {
        if (firstGetFromDeviceDeviceIfShouldFlag) {
            firstGetFromDeviceDeviceIfShouldFlag = false;
            if (Math.abs(System.currentTimeMillis() / 1000L - locateTime) > REFRESH_KID_LOCATION_MIN_TIME_SEC) {
                // 静默主动定位手表
                locateDevice();
                return true;
            }
        }
        return false;
    }

    private void queryDeviceLastLocateServer() {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "fetchDeviceLocate deviceId is isEmpty");
            return;
        }

        ///eg: 2.静默获取服务器的最后一次位置信息
        ExecEntity execEntity = exec(
                Message.GetDeviceLastPositionReqMsg.newBuilder().setDeviceId(deviceId).build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.GetDeviceLastPositionRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "queryDeviceLastLocateServer() -> exec() -> onResponse(): " + rspMsg);
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                if (DeviceInfo.getLastLocateTime(deviceId) != rspMsg.getPosition().getTime()) {
                                    GreenUtils.saveDeviceLastPosition(deviceId, rspMsg.getPosition());
                                    handler.postKidLocation(rspMsg.getPosition());
                                }
                                return false;
                            }
                            L.w(TAG, "queryDeviceLastLocateServer() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                            if (rspMsg.getErrCode() == Message.ErrorCode.NOT_EXISTS) {
                                firstGetFromDeviceDeviceIfShould(0);
                                return false;
                            }
                        } catch (Exception e) {
                            L.e(TAG, "queryDeviceLastLocateServer() -> exec() -> onResponse()", e);
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "queryDeviceLastLocateServer() -> exec() -> onException()", cause);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );

        if (execEntity == null) {
            handler.stopLocateAnimation();
        }
    }

    private void locateDevice() {
        final String deviceId = App.getInstance().getDeviceId();
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "locateDevice(): device ID is null");
            return;
        }

        MobclickAgent.onEvent(getContext(), UmengEvent.LOCATE_DEVICE_TIMES);

        // 开启动画
        handler.startLocateAnimation();

        long deviceLocateTimeout = DEVICE_LOCATE_TIMEOUT;
        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId);
        if (deviceInfo != null) {
            int sec = deviceInfo.getDeviceEntity().getFuncModuleInfo().getFuncTimeoutDef().getSecOfLocate();
            if (sec > 0) {
                deviceLocateTimeout = sec * 1000L;
            }
        }
        locateDeviceExecEntity = exec(
                Message.LocateS1ReqMsg.newBuilder().setDeviceId(deviceId).build(),

                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.LocateS1RspMsg locateS1RspMsg = response.getProtoBufMsg();
                            L.v(TAG, "locateDevice() -> exec(TAG_LOCATE_S1) -> onResponse(): " + locateS1RspMsg);
                            if (locateS1RspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                return true;
                            }
                            L.w(TAG, "locateDevice() -> exec(TAG_LOCATE_S1) -> onResponse(): " + locateS1RspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "locateDevice() -> exec(TAG_LOCATE_S1) -> onResponse() process Pkt failure", e);
                        }
                        locateDeviceExecEntity = null;
                        handler.stopLocateAnimation();
                        cannotLocateTipView.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        locateDeviceExecEntity = null;
                        L.e(TAG, "locateDevice() -> exec(TAG_LOCATE_S1) -> onException()", cause);
                        handler.stopLocateAnimation();
                        cannotLocateTipView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        locateDeviceExecEntity = null;
                        handler.stopLocateAnimation();
                        try {
                            Message.LocateS3ReqMsg locateS3ReqMsg = thirdStageRequest.getProtoBufMsg();
                            L.d(TAG, "locateDevice() -> exec(TAG_LOCATE_S3) -> onThirdStage()" + locateS3ReqMsg);
                            handler.postKidLocation(locateS3ReqMsg.getPosition());
                            responseSetter.setResponse(Message.LocateS3RspMsg.newBuilder().setErrCode(Message.ErrorCode.SUCCESS).build());
                            GreenUtils.saveDeviceLastPosition(deviceId, locateS3ReqMsg.getPosition());
                        } catch (Exception e) {
                            cannotLocateTipView.setVisibility(View.VISIBLE);
                            L.e(TAG, "locateDevice() -> exec(TAG_LOCATE_S3) -> onThirdStage() process Pkt failure", e);
                        }
                    }
                },

                deviceLocateTimeout
        );

        if (locateDeviceExecEntity == null) {
            handler.stopLocateAnimation();
        }
    }

    private void cancelLocateDeviceExecEntity() {
        L.d(TAG, "cancelLocateDeviceExecEntity: " + (locateDeviceExecEntity == null ? "!" : "") + "= null", new Exception("This is debug StackTrace Exception"));
        if (locateDeviceExecEntity != null) {
            locateDeviceExecEntity.cancel();
            locateDeviceExecEntity = null;
        }
        handler.stopLocateAnimation();
        cannotLocateTipView.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.baby_head_icon_cl:
                showBabyListPopupWindow();
                break;

            case R.id.envelope_view:
                startActivity(new Intent(getContext(), MessageBabiesListActivity.class));
                MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_ENTER_MESSAGE_CENTER);
                break;

            case R.id.location_history:
                if (!TextUtils.isEmpty(App.getInstance().getDeviceId())) {
                    Intent intent = new Intent(getContext(), LocationRecordActivity.class);
                    startActivity(intent);
                    MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_ENTER_LOCATION_RECORD);
                }
                break;

            case R.id.locate_device:
                cannotLocateTipView.setVisibility(View.GONE);
                phoneLocateToggleButton.setChecked(false);
                aMapMoveCameraToKidPosition();
                // 如果间隔30秒以上，才请主动获取最新位置
                if (Math.abs(System.currentTimeMillis() / 1000L - DeviceInfo.getLastLocateTime(mDeviceId)) > REFRESH_KID_LOCATION_MIN_TIME_SEC) {
                    locateDevice();
                } else {
                    if (kidLocation != null)
                        kidLocation.showAddressCard();
                }
                break;

            case R.id.map_zoom_in:
                if (mMapType == Constants.MAP_TYPE_AMAP) {
                    if (mAMap == null)
                        break;
                    mAMap.animateCamera(CameraUpdateFactory.zoomIn());
                } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                    if (googleMap == null)
                        break;
                    googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.zoomIn());
                }
                break;

            case R.id.map_zoom_out:
                if (mMapType == Constants.MAP_TYPE_AMAP) {
                    if (mAMap == null)
                        break;
                    mAMap.animateCamera(CameraUpdateFactory.zoomOut());
                } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                    if (googleMap == null)
                        break;
                    googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.zoomOut());
                }

                break;

            case R.id.map_type:
                if (mMapType == Constants.MAP_TYPE_AMAP) {
                    if (mAMap == null)
                        break;
                    if (mAMap.getMapType() == AMap.MAP_TYPE_NORMAL) {
                        mAMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                        ((ImageButton) rootView.findViewById(R.id.map_type)).setImageResource(R.drawable.change_button_change);
                    } else {
                        mAMap.setMapType(AMap.MAP_TYPE_NORMAL);
                        ((ImageButton) rootView.findViewById(R.id.map_type)).setImageResource(R.drawable.change_button);
                        setAMapLanguage(mAMap);
                    }
                } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                    if (googleMap == null)
                        break;
                    if (googleMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        ((ImageButton) rootView.findViewById(R.id.map_type)).setImageResource(R.drawable.change_button_change);
                    } else {
                        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        ((ImageButton) rootView.findViewById(R.id.map_type)).setImageResource(R.drawable.change_button);
                    }
                }

                break;

            default:
                break;
        }
    }

    private void showBabyListPopupWindow() {
        if (popupWindow == null) {
            popupWindow = new PopupWindow(contentView);
        }
        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setAnimationStyle(R.style.windowAnimation);
        popupWindow.showAsDropDown(title_bar);
    }

    public List<BabyEntity> getClipViewPagerData() {
        List<BabyEntity> babyDataList = new ArrayList<>();

        if (TextUtils.isEmpty(mUserId)) {
            L.w(TAG, "refreshClipViewPagerData userId is isEmpty");
            return null;
        }
        List<BabyEntity> list = GreenUtils.getBabyEntityDao().queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(mUserId))
                .build().list();

        for (int i = 0; i < list.size() && i < 8; ++i) {
            BabyEntity babyEntity = list.get(i);
            babyDataList.add(babyEntity);
        }
        if (babyDataList.size() >= 8) {
            babyDataList.add(new BabyEntity());
        }
        return babyDataList;
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.locate_self:
                phoneLocateToggleButton.setOnCheckedChangeListener(null);
                onPhoneLocateToggleButtonCheckedChanged(isChecked);
                phoneLocateToggleButton.setOnCheckedChangeListener(this);
                break;
            default:
                break;
        }
    }

    private void onPhoneLocateToggleButtonCheckedChanged(boolean isChecked) {
        if (isChecked) {
            phoneLocateToggleButton.setChecked(false);
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (AndroidPermissions.shouldShowGuide(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showLocationPermissionGuide();
                } else {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION_TEHN_LOCATE);
                }
                return;
            }

            if (phoneLocation == null)
                return;

            if (phoneLocation.latLon == null || phoneLocation.latLon.lat == 0 && phoneLocation.latLon.lon == 0) {
                if (!systemLocationServiceRequested) {
                    systemLocationServiceRequested = true;
                    if (!AndroidPermissions.isLocationServiceEnable(getContext())) {
                        showEnableLocationServiceGuide();
                        return;
                    }
                }
                toast(R.string.locating);
            }
        }
        phoneLocateToggleButton.setChecked(isChecked);
        if (isChecked) {
            phoneLocationChecked();
        } else {
            phoneLocationUnchecked();
        }
    }

    private void aMapMoveCameraToKidPosition() {
        if (kidLocation == null)
            return;
        if (!kidLocation.isLocating) {
            kidLocation.showMarker();
        }
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (kidLocation.latLon != null) {
                googleMap.animateCamera(
                        com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                new com.google.android.gms.maps.model.LatLng(kidLocation.latLon.lat, kidLocation.latLon.lon), 15.0f));
                handler.postShowKidLocation(50);
            } else {
                googleMap.animateCamera(
                        com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                new com.google.android.gms.maps.model.LatLng(0, 0), 0));
            }
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (kidLocation.latLon != null) {
                mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(kidLocation.latLon.lat, kidLocation.latLon.lon), 15.0f));
                handler.postShowKidLocation(50);
            } else {
                mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(BEIJING, 10));
            }
        }
    }


    private void boundKidAndPhoneMarkers() {
        int boundsIncludeCount = 0;
        if (mMapType == Constants.MAP_TYPE_AMAP) {
            LatLngBounds.Builder amapBoundsBuilder = new LatLngBounds.Builder();
            if (phoneLocation != null) {
                Marker phoneMarker = phoneLocation.getAmapMaker();
                if (phoneMarker != null) {
                    amapBoundsBuilder.include(phoneMarker.getPosition());
                    ++boundsIncludeCount;
                }
            }
            if (kidLocation != null) {
                Marker kidMarker = kidLocation.getAmapMaker();
                if (kidMarker != null) {
                    amapBoundsBuilder.include(kidMarker.getPosition());
                    ++boundsIncludeCount;
                }
            }
            if (boundsIncludeCount > 0) {
                mAMap.animateCamera(CameraUpdateFactory.newLatLngBounds(amapBoundsBuilder.build(), (int) DensityUtils.dp2px(getContext(), 100)));
            }
        } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            com.google.android.gms.maps.model.LatLngBounds.Builder gmapBoundsBuilder = new com.google.android.gms.maps.model.LatLngBounds.Builder();
            if (phoneLocation != null) {
                com.google.android.gms.maps.model.Marker phoneMarker = phoneLocation.getGmapMaker();
                if (phoneMarker != null) {
                    gmapBoundsBuilder.include(phoneMarker.getPosition());
                    ++boundsIncludeCount;
                }
            }
            if (kidLocation != null) {
                com.google.android.gms.maps.model.Marker kidMarker = kidLocation.getGmapMaker();
                if (kidMarker != null) {
                    gmapBoundsBuilder.include(kidMarker.getPosition());
                    ++boundsIncludeCount;
                }
            }
            if (boundsIncludeCount > 0) {
                googleMap.animateCamera(newLatLngBounds(gmapBoundsBuilder.build(), (int) DensityUtils.dp2px(getContext(), 100)));
            }
        }
    }

    /**
     * phoneMaker is able check
     */
    private void phoneLocationChecked() {
        if (phoneLocation == null)
            return;
        phoneLocation.setShouldShowMarker(true);
        if (phoneLocation.latLon != null && !(phoneLocation.latLon.lat == 0 && phoneLocation.latLon.lon == 0)) {
            phoneLocation.showMarker();
        }

        if (kidLocation != null && !kidLocation.isLocating) {
            kidLocation.hideAddressCard();
        }
        phoneLocation.hideAddressCard();

        if (kidLocation != null) {
            kidLocation.showMarker();
        }

        boundKidAndPhoneMarkers();
    }

    private void phoneLocationUnchecked() {
        if (phoneLocation != null) {
            phoneLocation.setShouldShowMarker(false);
            phoneLocation.hideMarker();
            phoneLocation.hideAddressCard();
        }
        aMapMoveCameraToKidPosition();
    }

    private Object regeoCode(double lat, double lon) {
        if (mMapType == Constants.MAP_TYPE_AMAP) {
            return getAddressByAmap(new LatLonPoint(lat, lon));
        } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            GoogleRegeocodeTask task = new GoogleRegeocodeTask(this, this);
            task.execute(lat, lon);
            return task;
        } else {
            return null;
        }
    }

    // 高德地图获取坐标对应的地理信息
    private RegeocodeQuery getAddressByAmap(LatLonPoint latLonPoint) {
        L.d(TAG, "获取坐标对应的地理信息: " + latLonPoint.toString());
        if (mGeocodeSearch == null) {
            mGeocodeSearch = new GeocodeSearch(getContext());
        }
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP); // 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        mGeocodeSearch.getFromLocationAsyn(query); // 设置同步逆地理编码请求
        return query;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        if (marker.getObject() == kidLocation) {
            if (kidLocation.isLocating) {
                return kidLocation.infoWindowView;
            }
        }
        return null;
    }

    @Override
    public View getInfoWindow(com.google.android.gms.maps.model.Marker marker) {
        if (marker.getTag() == kidLocation) {
            if (kidLocation.isLocating) {
                return kidLocation.infoWindowView;
            }
        }
        return null;
    }


    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(com.google.android.gms.maps.model.Marker marker) {
        return null;
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        mScalePerPixel = mAMap.getScalePerPixel();
    }

    @Override
    public void onCameraIdle() {
        mScalePerPixel = getScalePerPixel();
    }


    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        mScalePerPixel = mAMap.getScalePerPixel();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        // 点击地图上没 amapMaker 的地方，隐藏 AddressCard
        if (kidLocation != null) {
            kidLocation.hideAddressCard();
        }
        if (phoneLocation != null)
            phoneLocation.hideAddressCard();
    }

    @Override
    public void onMapClick(com.google.android.gms.maps.model.LatLng latLng) {
        // 点击地图上没 Maker 的地方，隐藏 AddressCard
        if (kidLocation != null) {
            kidLocation.hideAddressCard();
            if (kidLocation.isLocating) {
                kidLocation.showInfoWindow();
            }
        }
        if (phoneLocation != null)
            phoneLocation.hideAddressCard();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getObject() == kidLocation) {
            kidLocation.showAddressCard();
            if (TextUtils.isEmpty(kidLocation.addrFull) && kidLocation.regeocodeQuery == null) {
                kidLocation.regeocode();
            }
        } else if (marker.getObject() == phoneLocation) {
            phoneLocation.showAddressCard();
        }
        return true;
    }

    @Override
    public boolean onMarkerClick(com.google.android.gms.maps.model.Marker marker) {
        Location location = null;
        if (marker.getTag() == kidLocation) {
            location = kidLocation;
            if (kidLocation.isLocating) {
                kidLocation.showInfoWindow();
            }
        } else if (marker.getTag() == phoneLocation) {
            location = phoneLocation;
        }
        if (location != null) {
            location.showAddressCard();
            if (TextUtils.isEmpty(location.addrFull) && location.regeocodeQuery == null) {
                location.regeocode();
            }
        }
        return true;
    }

    // 高德定位到位置的回调
    @Override
    public void onMyLocationChange(android.location.Location location) {
        L.v(TAG, "AMap onMyLocationChange: " + location.toString());
        if (mAMap == null)
            return;

        onPhoneLocationChanged(location);
    }

    // google 定位到手机位置
    @Override
    public void onLocationChanged(android.location.Location location) {
        L.v(TAG, "google onLocationChanged: " + location.toString());
        if (googleMap == null)
            return;

        CoordinateConverter converter = new CoordinateConverter(getContext());
        // CoordType.GPS 待转换坐标类型
        converter.from(CoordinateConverter.CoordType.GPS);
        // sourceLatLng待转换坐标点 LatLng类型
        LatLng sourceLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        converter.coord(sourceLatLng);
        // 执行转换操作
        LatLng desLatLng = converter.convert();
        location.setLatitude(desLatLng.latitude);
        location.setLongitude(desLatLng.longitude);

        onPhoneLocationChanged(location);
    }

    private void onPhoneLocationChanged(android.location.Location location) {
        boolean isFirst = phoneLocation.latLon == null || (phoneLocation.latLon.lat == 0 && phoneLocation.latLon.lon == 0);
        phoneLocation.setLocation(location);
        if (isFirst && phoneLocateToggleButton.isChecked()) {
            phoneLocationChecked();
        }
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        L.v(TAG, "onRegeocodeSearched: " + "rCode=" + rCode + ", result: " + result);
        if (kidLocation == null || phoneLocation == null)
            return;
        if (rCode != 1000)
            return;
        if (result == null)
            return;
        RegeocodeAddress regAddr = result.getRegeocodeAddress();
        if (regAddr == null)
            return;
        L.v(TAG, "onRegeocodeSearched: RegeocodeAddress: " + regAddr);

        String currentDeviceAddress, poi;
        if (regAddr.getPois() != null && !regAddr.getPois().isEmpty()) {
            poi = regAddr.getPois().get(0).getTitle();
        } else {
            poi = regAddr.getCity() + regAddr.getDistrict();
        }
        currentDeviceAddress = regAddr.getFormatAddress();
        L.v(TAG, "poi: " + poi + ", currentDeviceAddress:" + currentDeviceAddress);
        RegeocodeQuery query = result.getRegeocodeQuery();
        if (query == kidLocation.regeocodeQuery) {
            kidLocation.regeocodeQuery = null;
            kidLocation.setAddress(poi, currentDeviceAddress);
        } else if (query == phoneLocation.regeocodeQuery) {
            phoneLocation.regeocodeQuery = null;
            phoneLocation.setAddress(poi, currentDeviceAddress);
        }
    }

    @Override
    public void onGoogleRegeocode(GoogleRegeocodeTask task, Throwable throwable, int httpStatusCode, GoogleRegeocodeResult geocodeResult, String formattedAddress, String pointOfInterest) {
        if (kidLocation == null || phoneLocation == null)
            return;
        if (task == kidLocation.regeocodeQuery) {
            kidLocation.regeocodeQuery = null;
            kidLocation.setAddress(pointOfInterest, formattedAddress);
        } else if (task == phoneLocation.regeocodeQuery) {
            phoneLocation.regeocodeQuery = null;
            phoneLocation.setAddress(pointOfInterest, formattedAddress);
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
        // 地址转 latlng
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


    private static class ShowMarkerAnimationHandler extends Handler {
        private static final int ON_KID_LOCATION = 2;
        private static final int SHOW_KID_LOCATION = 3;
        private static final int QUERY_DEVICE_LAST_LOCATION = 4;
        private static final int SHOW_KID_POSITION = 5;

        private WeakReference<? extends MapFragment> mF;


        ShowMarkerAnimationHandler(MapFragment a) {
            mF = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            MapFragment f = mF.get();
            if (f == null)
                return;

            switch (msg.what) {
                case ON_KID_LOCATION:
                    if (f.kidLocation != null) {
                        f.onKidLocation((Message.Position) msg.obj);
                    }
                    break;

                case SHOW_KID_LOCATION:
                    if (f.kidLocation != null) {
                        f.kidLocation.showMarker();
                        f.kidLocation.showAddressCard();
                    }
                    break;

                case QUERY_DEVICE_LAST_LOCATION:
                    f.queryDeviceLastLocateServer();
                    break;

                case SHOW_KID_POSITION:
                    f.showKidPosition();
                    break;

                default:
                    break;
            }
        }

        void circleStartBreathe() {
            MapFragment f = mF.get();
            if (f == null || f.kidLocation == null || f.mCircleBreatheTimer != null)
                return;
            f.kidLocation.isLocating = true;
            f.kidLocation.showInfoWindow();
            if (f.mMapType == Constants.MAP_TYPE_AMAP) {
                f.scaleAMapCircle(f.kidLocation.amapCircle);
            } else if (f.mMapType == Constants.MAP_TYPE_GOOGLE) {
                if (f.googleMap == null)
                    return;
                f.scaleGMapCircle(f.kidLocation.googleCircle);
            }
        }

        void circleStopBreathe() {
            MapFragment f = mF.get();
            if (f != null && f.mCircleBreatheTimer != null && f.kidLocation != null) {
                f.kidLocation.isLocating = false;
                f.kidLocation.hideInfoWindow();
                f.mCircleBreatheTimer.cancel();
                if (f.mTimerTask.originAmapCircle == null && f.mTimerTask.circleAmap != null) {
                    f.mTimerTask.circleAmap.remove();
                    f.mTimerTask.circleAmap = null;
                }
                if (f.mTimerTask.originGmapCircle == null && f.mTimerTask.circleGmap != null) {
                    f.mTimerTask.circleGmap.remove();
                    f.mTimerTask.circleGmap = null;
                }
                f.mCircleBreatheTimer = null;
                postShowKidLocation(100);
            }
        }

        void startLocateAnimation() {
            circleStartBreathe();
        }

        void stopLocateAnimation() {
            circleStopBreathe();
        }

        void postKidLocation(Message.Position position) {
            removeMessages(ON_KID_LOCATION);
            sendMessageDelayed(obtainMessage(ON_KID_LOCATION, position), 200);
        }

        void postLocateDevice() {
            L.i(TAG, "postLocateDevice xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", new Exception());
            removeMessages(QUERY_DEVICE_LAST_LOCATION);
            sendEmptyMessageDelayed(QUERY_DEVICE_LAST_LOCATION, 250);
        }

        void postShowKidLocation(long delayMillis) {
            sendEmptyMessageDelayed(SHOW_KID_LOCATION, delayMillis);
        }

        void refreshKidPosition() {
            sendEmptyMessage(SHOW_KID_POSITION);
        }
    }

    private void showLocationPermissionGuide() {
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setBlur(false)
                .setTitle(getText(R.string.location_permission))
                .setMessage(getText(R.string.please_enable_location_permission_in_setting))
                .setPositiveButton(getText(R.string.ok), (dialog, which) -> {
                    Intent intent = AndroidPermissions.permissionSettingPageIntent(getContext());
                    startActivityForResult(intent, ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_LOCATION_PERMISSION);
                })
                .setNegativeButton(getText(R.string.cancel), null);
        dialogFragment.show(getFragmentManager(), "showLocationPermissionGuide");
    }

    private void showEnableLocationServiceGuide() {
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setBlur(false)
                .setTitle(getText(R.string.location_service))
                .setMessage(getText(R.string.please_enable_location_service_in_setting))
                .setPositiveButton(getText(R.string.ok), (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_ENABLE_LOCATION_SERVICE);
                })
                .setNegativeButton(getText(R.string.cancel), null);
        dialogFragment.show(getFragmentManager(), "showEnableLocationServiceGuide");
    }

    private class Location {
        private boolean isKid;
        //高德地图
        private AMap aMap;
        private MarkerOptions amapMarkerOptions;
        private Marker amapMaker;
        private Circle amapCircle;
        private Object regeocodeQuery;
        //谷歌地图
        private GoogleMap gMap;
        private com.google.android.gms.maps.model.MarkerOptions googleMarkerOptions;
        private com.google.android.gms.maps.model.Marker googleMarker;
        private com.google.android.gms.maps.model.Circle googleCircle;

        private String addrShort;
        private String addrFull;

        private View addrCard;
        private TextView mapAddrShortView;
        private TextView mapAddrFullView;
        private TextView locationTimeView;
        private ImageView locationWayView;
        private TextView locationAccuracyView;
        private Group kidAdditionalViewGroup;
        private View navigationButton;
        Message.Position mDevicePosition;
        private LatLon latLon;
        private double accuracy;
        private boolean shouldShowMarker;

        private boolean isLocating;
        private View infoWindowView;

        private int mapType;

        Location(AMap aMap, int markerIconResId, float markerIconAnchorU, float markerIconAnchorV, boolean kid) {
            this.aMap = aMap;
            mapType = Constants.MAP_TYPE_AMAP;
            amapMarkerOptions = new MarkerOptions();
            amapMarkerOptions.anchor(markerIconAnchorU, markerIconAnchorV);
            amapMarkerOptions.icon(BitmapDescriptorFactory.fromResource(markerIconResId));

            initLocation(kid);
        }

        Location(GoogleMap map, int markerIconResId, float markerIconAnchorU, float markerIconAnchorV, boolean kid) {
            this.gMap = map;
            mapType = Constants.MAP_TYPE_GOOGLE;
            googleMarkerOptions = new com.google.android.gms.maps.model.MarkerOptions();
            googleMarkerOptions.anchor(markerIconAnchorU, markerIconAnchorV);
            googleMarkerOptions.icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(markerIconResId));

            initLocation(kid);
        }

        private void initLocation(final boolean kid) {
            isKid = kid;

            infoWindowView = LayoutInflater.from(getContext()).inflate(R.layout.view_map_locating_infowindow, null);

            mapAddrShortView = rootView.findViewById(R.id.map_addr_short);
            mapAddrFullView = rootView.findViewById(R.id.map_addr_full);
            locationTimeView = rootView.findViewById(R.id.map_location_time);
            locationWayView = rootView.findViewById(R.id.location_way);
            locationAccuracyView = rootView.findViewById(R.id.location_accuracy);
            navigationButton = rootView.findViewById(R.id.map_button_navigation);
            kidAdditionalViewGroup = rootView.findViewById(R.id.kid_additional_views);

            if (kid) {
                shouldShowMarker = true;
                navigationButton.setOnClickListener(new DebouncedOnClickListener() {
                    @Override
                    public void onDebouncedClick(View view) {
                        NavigationUtils.onNavigationButtonClick(getContext(), mDeviceId, phoneLocation.latLon, kidLocation.latLon, kidLocation.addrFull, new NavigationUtils.OnShowSelectMapAppListener() {
                            @Override
                            public void onShowSelectMapListener(Map<String, String> mapApps, double lat, double lon, String address, boolean shouldWalking) {
                                showSelectMapAppDialog(mapApps, lat, lon, address, shouldWalking);
                            }

                            @Override
                            public void onShowSelectMapNoMapListener() {
                                popInfoDialog(R.string.please_install_map_app);
                            }
                        });
                    }
                });
            }
        }


        public void setShouldShowMarker(boolean should) {
            shouldShowMarker = should;
        }

        public void setAddress(String poi, String addrStr) {
            addrShort = poi;
            addrFull = addrStr;

            if (addrCard != null) {
                mapAddrShortView.setText(poi);
                mapAddrFullView.setText(addrStr);
            }
        }

        private void _setLatLon(double lat, double lon) {
            if (latLon == null) {
                latLon = new LatLon();
            }
            latLon.lat = lat;
            latLon.lon = lon;
            if (mapType == Constants.MAP_TYPE_AMAP) {
                amapMarkerOptions.position(new LatLng(latLon.lat, latLon.lon));
                if (amapMaker != null) {
                    amapMaker.setSnippet(amapMarkerOptions.getSnippet());
                    amapMaker.setTitle(amapMarkerOptions.getTitle());
                }
            } else if (mapType == Constants.MAP_TYPE_GOOGLE) {
                googleMarkerOptions.position(new com.google.android.gms.maps.model.LatLng(latLon.lat, latLon.lon));
                if (googleMarker != null) {
                    googleMarker.setSnippet(googleMarkerOptions.getSnippet());
                    googleMarker.setTitle(googleMarkerOptions.getTitle());
                }
            }
        }

        synchronized void setLocation(android.location.Location location) {
            _setLatLon(location.getLatitude(), location.getLongitude());
            showMarker();
            regeocode();
        }

        void setMarkerIcon(Bitmap bitmap) {
            if (mapType == Constants.MAP_TYPE_AMAP) {
                amapMarkerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
                if (amapMaker != null) {
                    amapMaker.setIcon(amapMarkerOptions.getIcon());
                    amapMaker.setObject(this);
                }
            } else if (mapType == Constants.MAP_TYPE_GOOGLE) {
                googleMarkerOptions.icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap));
                if (googleMarker != null) {
                    googleMarker.setIcon(googleMarkerOptions.getIcon());
                    googleMarker.setTag(this);
                }
            }
        }

        synchronized void setPosition(Message.Position position) {
            if (mDevicePosition != null && mDevicePosition.getTime() == position.getTime()
                    && mDevicePosition.getLatLng().getLatitude() - position.getLatLng().getLatitude() < Constants.LAT_LON_EPSILON
                    && mDevicePosition.getLatLng().getLongitude() - position.getLatLng().getLongitude() < Constants.LAT_LON_EPSILON
                    && mDevicePosition.getAccuracy() - position.getAccuracy() < Constants.LAT_LON_EPSILON) {
                return;
            }

            mDevicePosition = position;
            addrShort = "";
            addrFull = "";

            // address Card
            Calendar nowCal = Calendar.getInstance();
            Calendar posCal = Calendar.getInstance();
            posCal.setTimeInMillis(mDevicePosition.getTime() * 1000L);
            String timeStr;
            if (posCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
                timeStr = getString(R.string.kid_position_time_MMddHHmm, posCal.get(Calendar.MONTH) + 1, posCal.get(Calendar.DAY_OF_MONTH), posCal.get(Calendar.HOUR_OF_DAY), posCal.get(Calendar.MINUTE));
            } else {
                timeStr = getString(R.string.kid_position_time_yyyyMMddHHmm, posCal.get(Calendar.YEAR), posCal.get(Calendar.MONTH) + 1, posCal.get(Calendar.DAY_OF_MONTH), posCal.get(Calendar.HOUR_OF_DAY), posCal.get(Calendar.MINUTE));
            }
            locationTimeView.setText(timeStr);
            int locationWayRes = R.drawable.location_satellite;
            switch (mDevicePosition.getLocateType()) {
                case CELL:
                case HYBRID:
                    locationWayRes = R.drawable.location_vague;
                    break;
                case WIFI:
                    locationWayRes = R.drawable.location_wifi;
                    break;
            }
            locationWayView.setImageResource(locationWayRes);
            locationAccuracyView.setText(getString(R.string.map_location_accuracy_fmt, mDevicePosition.getAccuracy()));
            if (addrCard != null) {
                mapAddrShortView.setText(addrShort);
                mapAddrFullView.setText(addrFull);
            }
            accuracy = mDevicePosition.getAccuracy();

            _setLatLon(mDevicePosition.getLatLng().getLatitude(), mDevicePosition.getLatLng().getLongitude());
            showMarker();
            regeocode();
        }

        private void regeocode() {
            if (latLon == null)
                return;
            regeocodeQuery = regeoCode(latLon.lat, latLon.lon);
        }

        public synchronized void showMarker() {
            if (!shouldShowMarker)
                return;
            if (isLocating)
                return;
            if (latLon == null)
                return;

            switch (mapType) {
                case Constants.MAP_TYPE_AMAP: {
                    if (amapMaker != null) {
                        amapMaker.remove();
                    }
                    amapMaker = aMap.addMarker(amapMarkerOptions);
                    amapMaker.setObject(this);
                    if (amapCircle != null)
                        amapCircle.remove();
                    amapCircle = aMap.addCircle(new CircleOptions()
                            .center(amapMaker.getPosition())
                            .radius(accuracy)
                            .fillColor(Constants.FENCE_FILL_COLOR)
                            .strokeColor(Constants.FENCE_STROKE_COLOR)
                            .strokeWidth(3.0f));
                }
                break;
                case Constants.MAP_TYPE_GOOGLE: {
                    if (googleMarker != null) {
                        googleMarker.remove();
                    }
                    googleMarker = gMap.addMarker(googleMarkerOptions);
                    googleMarker.setTag(this);
                    if (googleCircle != null)
                        googleCircle.remove();
                    googleCircle = gMap.addCircle(new com.google.android.gms.maps.model.CircleOptions()
                            .center(googleMarker.getPosition())
                            .radius(accuracy)
                            .fillColor(Constants.FENCE_FILL_COLOR)
                            .strokeColor(Constants.FENCE_STROKE_COLOR)
                            .strokeWidth(3.0f));
                }
                break;
            }
        }

        public @Nullable
        Marker getAmapMaker() {
            return amapMaker;
        }

        public @Nullable
        com.google.android.gms.maps.model.Marker getGmapMaker() {
            return googleMarker;
        }


        public synchronized void hideMarker() {
            if (mapType == Constants.MAP_TYPE_AMAP) {
                if (amapMaker != null) {
                    amapMaker.remove();
                    amapMaker = null;
                }
                if (amapCircle != null) {
                    amapCircle.remove();
                    amapCircle = null;
                }
            } else if (mapType == Constants.MAP_TYPE_GOOGLE) {
                if (googleMarker != null) {
                    googleMarker.remove();
                    googleMarker = null;
                }
                if (googleCircle != null) {
                    googleCircle.remove();
                    googleCircle = null;
                }
            }
        }

        public synchronized void showInfoWindow() {
            if (mapType == Constants.MAP_TYPE_GOOGLE) {
                if (googleMarker != null) {
                    googleMarker.showInfoWindow();
                }
            } else if (mapType == Constants.MAP_TYPE_AMAP) {
                if (amapMaker != null) {
                    amapMaker.showInfoWindow();
                }
            }
        }

        public synchronized void hideInfoWindow() {
            if (mapType == Constants.MAP_TYPE_GOOGLE) {
                if (googleMarker != null) {
                    googleMarker.hideInfoWindow();
                }
            } else if (mapType == Constants.MAP_TYPE_AMAP) {
                if (amapMaker != null) {
                    amapMaker.hideInfoWindow();
                }
            }
        }

        public synchronized void showAddressCard() {
            if (mapType == Constants.MAP_TYPE_GOOGLE) {
                if (googleMarker == null) {
                    return;
                }
            } else if (mapType == Constants.MAP_TYPE_AMAP) {
                if (amapMaker == null) {
                    return;
                }
            }

            if (this == kidLocation) {
                if (addrCard == null) {
                    addrCard = mAddrCard;
                    phoneLocation.addrCard = null;
                }
            } else {
                if (addrCard == null) {
                    addrCard = mAddrCard;
                    kidLocation.addrCard = null;
                }
            }

            mapAddrShortView.setText(addrShort);
            mapAddrFullView.setText(addrFull);

            if (addrCard.getVisibility() != View.VISIBLE) {
                addrCard.setVisibility(View.VISIBLE);
            }
            if (isKid && (kidAdditionalViewGroup.getVisibility() != View.VISIBLE)) {
                kidAdditionalViewGroup.setVisibility(View.VISIBLE);
            } else if (!isKid && (kidAdditionalViewGroup.getVisibility() == View.VISIBLE)) {
                kidAdditionalViewGroup.setVisibility(View.GONE);
            }
        }

        public synchronized void hideAddressCard() {
            if (addrCard == null)
                return;
            if (mAddrCard.getVisibility() == View.VISIBLE)
                mAddrCard.setVisibility(View.GONE);
        }

        public synchronized void clear() {
            hideMarker();
            addrShort = "";
            addrFull = "";
            if (addrCard != null) {
                hideAddressCard();
                addrCard = null;
            }
            mDevicePosition = null;
            latLon = null;
            accuracy = 0;
            shouldShowMarker = false;
            isLocating = false;
        }
    }

    private void showSelectMapAppDialog(final Map<String, String> mapApps, final double lat, final double lng, final String address, final boolean shouldWalking) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle(R.string.map_select_map);
        final String[] items = new String[mapApps.size()];
        int i = 0;
        for (Map.Entry<String, String> stringStringEntry : mapApps.entrySet()) {
            String name = stringStringEntry.getKey();
            items[i++] = name;
        }
        alert.setItems(items, (dialog, which) -> {
            try {
                NavigationUtils.navigation(getContext(), mDeviceId, mapApps.get(items[which]), lat, lng, address, shouldWalking);
            } catch (Exception e) {
                L.e(TAG, "showSelectMapAppDialog onClick(" + which + ")", e);
            }
        });
        alert.show();
    }

    public void scaleAMapCircle(final Circle circle) {
        mTimerTask = new CircleTask(circle, 2000);
        mCircleBreatheTimer = new Timer();
        mCircleBreatheTimer.schedule(mTimerTask, 0, 30);
    }

    public void scaleGMapCircle(com.google.android.gms.maps.model.Circle circle) {
        mTimerTask = new CircleTask(circle, 2000);
        mCircleBreatheTimer = new Timer();
        mCircleBreatheTimer.schedule(mTimerTask, 0, 30);
    }


    private Timer mCircleBreatheTimer;
    CircleTask mTimerTask;

    private class CircleTask extends TimerTask {
        private long start = SystemClock.uptimeMillis();
        private final Interpolator interpolator = new CycleInterpolator(1);
        private int screenWidth = -1;
        private double rMin;
        private double r;
        private Circle originAmapCircle;
        private Circle circleAmap;
        private com.google.android.gms.maps.model.Circle originGmapCircle;
        private com.google.android.gms.maps.model.Circle circleGmap;
        private long duration = 3000;

        public CircleTask(Circle circle, long rate) {
            originAmapCircle = circle;
            circleAmap = originAmapCircle;
            if (rate > 0) {
                this.duration = rate;
            }
        }

        public CircleTask(com.google.android.gms.maps.model.Circle circle, long rate) {
            originGmapCircle = circle;
            circleGmap = originGmapCircle;
            if (rate > 0) {
                this.duration = rate;
            }
        }


        @Override
        public void run() {
            handler.post(() -> {
                try {
                    if (screenWidth < 0) {
                        screenWidth = getResources().getDisplayMetrics().widthPixels;
                        rMin = screenWidth / 3 / 5;
                        r = mScalePerPixel * rMin;

                        if (mMapType == Constants.MAP_TYPE_AMAP) {
                            if (circleAmap == null) {
                                circleAmap = mAMap.addCircle(new CircleOptions()
                                        .center(BEIJING)
                                        .radius(r)
                                        .fillColor(Constants.FENCE_FILL_COLOR)
                                        .strokeColor(Constants.FENCE_STROKE_COLOR)
                                        .strokeWidth(3.0f));
                            }
                        } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                            if (circleGmap == null) {
                                circleGmap = googleMap.addCircle(new com.google.android.gms.maps.model.CircleOptions()
                                        .center(new com.google.android.gms.maps.model.LatLng(0, 0))
                                        .radius(r)
                                        .fillColor(Constants.FENCE_FILL_COLOR)
                                        .strokeColor(Constants.FENCE_STROKE_COLOR)
                                        .strokeWidth(3.0f));
                            }
                        }
                    }

                    long elapsed = SystemClock.uptimeMillis() - start;
                    float input = (float) elapsed / duration;
                    r = mScalePerPixel * rMin;
                    float t = interpolator.getInterpolation(input - 0.25f);
                    double r1 = (t + 3) * r;
                    if (mMapType == Constants.MAP_TYPE_AMAP) {
                        circleAmap.setRadius(r1);
                    } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                        circleGmap.setRadius(r1);
                    }
                    if (input > 3) {
                        start = SystemClock.uptimeMillis();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        }
    }


    private void showBabyName() {
        String showName = getString(R.string.baby);
        if (mCurrentBabyEntity != null) {
            String name = mCurrentBabyEntity.getName();
            if (!TextUtils.isEmpty(name)) {
                showName = name;
            }
        }
        mBabyNameView.setText(showName);
    }

    private void showBabyAvatar() {
        int alternateAvatarResId = DeviceInfo.getBabySex(mDeviceId) == Message.Baby.Sex.FEMALE_VALUE ? R.drawable.mod_baby_female : R.drawable.mod_baby_male;
        Glide.with(this)
                .load(new DeviceAvatar(mDeviceId, DeviceInfo.getBabyAvatar(mDeviceId), alternateAvatarResId))
                .apply(RequestOptions.placeholderOf(mBabyHeadIconView.getDrawable()))
                .apply(RequestOptions.errorOf(alternateAvatarResId))
                .apply(DeviceInfo.isOnline(mDeviceId) ? GlideDefines.DEVICE_AVATAR_OPTIONS : GlideDefines.DEVICE_OFFLINE_AVATAR_OPTIONS)
                .into(mBabyHeadIconView);
    }

    private void showOnlineStatus() {
        showOnlineStatus(DeviceInfo.isOnline(mDeviceId));
    }

    private void showOnlineStatus(boolean online) {
        if (online) {
            mBabyOnlineStatusView.setBackgroundResource(R.drawable.online_status_online_background_on_main_map_title_bar);
            mBabyOnlineStatusView.setText(R.string.is_online);
        } else {
            mBabyOnlineStatusView.setBackgroundResource(R.drawable.online_status_offline_background_on_main_map_title_bar);
            mBabyOnlineStatusView.setText(R.string.is_offline);
        }
    }

    private void showEnvelopeBadge() {
        String userId = mUserId;
        boolean showBadge = false;
        if (!TextUtils.isEmpty(userId)) {
            List<BabyEntity> babies = GreenUtils.getBabyEntityDao().queryBuilder().where(BabyEntityDao.Properties.UserId.eq(userId)).list();
            List<String> deviceIDs = new ArrayList<>();
            for (BabyEntity baby : babies) {
                deviceIDs.add(baby.getDeviceId());
            }
            long count = GreenUtils.getNotifyMessageEntityDao().queryBuilder()
                    .where(NotifyMessageEntityDao.Properties.UserId.eq(userId))
                    .where(NotifyMessageEntityDao.Properties.DeviceId.in(deviceIDs))
                    .where(NotifyMessageEntityDao.Properties.IsRead.eq(false))
                    .buildCount().count();
            showBadge = count != 0;
        }
        mEnvelopeBadge.setVisibility(showBadge ? View.VISIBLE : View.GONE);
    }

    private void showDeviceSensorData() {
        float battery = -1;
        int step = 0;
        do {
            if (TextUtils.isEmpty(mDeviceId)) {
                L.w(TAG, "showDevicePosition: mDeviceId is empty");
                break;
            }
            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
            if (deviceInfo == null || deviceInfo.getSensorDataTime() == 0) { // 无效的信息
                break;
            }

            // 我们始终显示电量状态
            int batteryLevelTotal;
            int defaultLevel;
            int batteryLevel = deviceInfo.getBatteryLevel();
            if (batteryLevel != 0 && (batteryLevel & 0x10000) != 0) { // 应该以电量格数显示
                batteryLevelTotal = (batteryLevel >>> 8) & 0xFF;
                batteryLevel &= 0xFF;
                defaultLevel = batteryLevelTotal - 1;

                if (deviceInfo.getBatteryPercent() > 100) {
                    // 充电中
                    batteryLevel = batteryLevelTotal + 1;
                }
            } else {
                batteryLevelTotal = 100;
                batteryLevel = deviceInfo.getBatteryPercent();
                defaultLevel = 80;
            }
            if (batteryLevel > batteryLevelTotal) {
                if (!deviceInfo.isOnline()) {
                    // 充电中，但是手表离线了，我们显示 defaultLevel 的电量
                    batteryLevel = defaultLevel;
                }
            }
            battery = batteryLevel * 1.0f / batteryLevelTotal;

            // 计步数
            Calendar now = Calendar.getInstance();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(deviceInfo.getSensorDataTime() * 1000L);
            if (calendar.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)) {
                step = deviceInfo.getStepCount();
            }
        } while (false);
        if (battery < 0) {
            mBabyBatteryView.setVisibility(View.GONE);
        } else {
            mBabyBatteryView.setVisibility(View.VISIBLE);
            mBabyBatteryView.setPowerPercent(battery);
        }
        mBabyStepCountView.setText(String.valueOf(step));
    }

    private void stepCountViewWhetherShow() {
        // XXX: 暂时不显示
        boolean shouldShow = false;
        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
        if (deviceInfo != null) {
            shouldShow = (deviceInfo.getDeviceEntity().getFuncModuleInfo().getFuncModule() & Message.FuncModule.FM_STEP_COUNT_VALUE) != 0;
        }
        mBabyStepCountLayout.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCurrentBabyChanged(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        super.onCurrentBabyChanged(oldBabyBean, newBabyBean, isSticky);
        mBabyListAdapter.onNotifyDataSetChanged(getClipViewPagerData());
        if (newBabyBean != null) {
            if (oldBabyBean == null || oldBabyBean.getDeviceId() == null || !newBabyBean.getDeviceId().equals(oldBabyBean.getDeviceId())) {
                // 切换了设备

                handler.stopLocateAnimation();
                cannotLocateTipView.setVisibility(View.GONE);
                handler.refreshKidPosition();
            }
        }
    }

    @Override
    public void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo) {
        mBabyListAdapter.onNotifyDataSetChanged(getClipViewPagerData());
        if (deviceInfo.getDeviceId().equals(mDeviceId)) {
            showBabyName();
            showBabyAvatar();
            showOnlineStatus();
            showEnvelopeBadge();
            showDeviceSensorData();
            stepCountViewWhetherShow();
            handler.refreshKidPosition();
        }
    }

    @Override
    public void onDeviceUnbind(TlcService tlcService, @Nullable Pkt reqPkt, @NonNull protocol.Message.NotifyUserUnbindDevReqMsg reqMsg) {
        mBabyListAdapter.onNotifyDataSetChanged(getClipViewPagerData());
        if (reqMsg.getUsrDevAssoc().getUserId().equals(mUserId) && reqMsg.getUsrDevAssoc().getDeviceId().equals(mDeviceId)) {
            L.w(TAG, "current device unbound!!! Activity finish");
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onDevicePosition(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDevicePositionReqMsg reqMsg) {
        if (reqMsg.getDeviceId().equals(mDeviceId)) {
            cancelLocateDeviceExecEntity();
            handler.postKidLocation(reqMsg.getPosition());
        }
    }

    @Override
    public void onLocateS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.LocateS3ReqMsg reqMsg) {
        if (reqMsg.getDeviceId().equals(mDeviceId)) {
            if (DeviceInfo.getLastLocateTime(mDeviceId) < reqMsg.getPosition().getTime()) {
                cancelLocateDeviceExecEntity();
                handler.postKidLocation(reqMsg.getPosition());
            }
        }
    }

    @Override
    protected void onGoogleInaccessible() {
        googleInaccessible.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onGoogleAccessible() {
        googleInaccessible.setVisibility(View.GONE);
    }

    @Override
    protected void onGoogleAccessibilityUnknown() {
        googleInaccessible.setVisibility(View.GONE);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onHasNewMessageOfMessageCenter(Event.HasNewMessageOfMessageCenter ev) {
        showEnvelopeBadge();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onDeviceOnline(Event.DeviceOnline ev) {
        mBabyListAdapter.onNotifyDataSetChanged(getClipViewPagerData());
        if (ev.getDeviceId().equals(mDeviceId)) {
            showOnlineStatus(ev.isOnline());
            showBabyAvatar();
        }
    }
}
