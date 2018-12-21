package com.cqkct.FunKidII.Ui.Activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
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
import com.cqkct.FunKidII.Bean.TrackPoint;
import com.cqkct.FunKidII.Bean.google.geocode.GoogleRegeocodeResult;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.fragment.CalendarDialogFragment;
import com.cqkct.FunKidII.Utils.Constants;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.ImageCacheUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.LatLon;
import com.cqkct.FunKidII.Utils.NavigationUtils;
import com.cqkct.FunKidII.Utils.StringUtils;
import com.cqkct.FunKidII.Utils.Utils;
import com.cqkct.FunKidII.db.Dao.LocationEntityDao;
import com.cqkct.FunKidII.db.Entity.LocationEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.NotYetLoginException;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.gyf.barlibrary.ImmersionBar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.NotYetConnectedException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import protocol.Message;


public class LocationRecordActivity extends BaseMapActivity implements View.OnClickListener,
        AMap.OnMarkerClickListener,
        AMap.OnMapClickListener,
        AMap.InfoWindowAdapter,
        AMap.OnMyLocationChangeListener,
        GeocodeSearch.OnGeocodeSearchListener,
        OnMapReadyCallback, GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMyLocationChangeListener,
        GoogleMap.InfoWindowAdapter,
        LocationListener,
        BaseMapActivity.GoogleRegeocodeResultListener {
    private static final String TAG = LocationRecordActivity.class.getSimpleName();


    private static final int DATEPICKER_ACTIVITY = 0;

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private List<RecordLocation> recordLocations = new ArrayList<>();

    private String devid;
    private String userid;

    private MapView mAMapView;
    private com.google.android.gms.maps.MapView mGoogleMapView;
    private AMap mAMap;
    private GoogleMap gMap;

    private Button selectDateButton;

    private LatLng currentPhoneAmapLatLng; // TODO: 得到宝贝当前位置
    private com.google.android.gms.maps.model.LatLng currentPhoneGmapLatLng; // TODO: 得到宝贝当前位置
    private SparseArray<LatLng> records = new SparseArray<>();
    private GeocodeSearch geocoderSearch;  //地址反查
    private Marker mAmapCurrentMarker;
    private com.google.android.gms.maps.model.Marker mGmapCurrentMarker;
    private Date selectDate;
    private Date daysLocationQueriedDate;
    private LoadDataTask mLoadDataTask;

    private View mAddrCard;
    private View navigationButton;
    private TextView mapAddrShortView;
    private TextView mapAddrFullView;
    private TextView locationTimeView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_record);


        ImmersionBar.with(this)
                .statusBarDarkFont(true)
                .init();

        selectDateButton = findViewById(R.id.select_date_btn);

        geocoderSearch = new GeocodeSearch(this);
        geocoderSearch.setOnGeocodeSearchListener(this);


        initUserInfo();
        if (mMapType == Constants.MAP_TYPE_AMAP) {
            mAMapView = findViewById(R.id.amap);
            mAMapView.setVisibility(View.VISIBLE);
            mAMapView.onCreate(savedInstanceState);
            if (mAMap == null) {
                mAMap = mAMapView.getMap();
                initAMap();
            }
        } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            mGoogleMapView = findViewById(R.id.gmap);
            mGoogleMapView.setVisibility(View.VISIBLE);
            mGoogleMapView.onCreate(savedInstanceState);
            mGoogleMapView.getMapAsync(this);
        }

        mAddrCard = findViewById(R.id.map_address_card_view);
        navigationButton = findViewById(R.id.map_button_navigation);
        mAddrCard.setOnClickListener(v -> { /* do noting, but do not delete this */ });
        mapAddrShortView = findViewById(R.id.map_addr_description);
        mapAddrFullView = findViewById(R.id.map_address);
        locationTimeView = findViewById(R.id.map_dev_last_login_time);
        navigationButton = findViewById(R.id.map_button_navigation);
        navigationButton.setOnClickListener(v -> {
            if (currentPhoneAmapLatLng == null && currentPhoneGmapLatLng == null) {
                popErrorDialog(R.string.net_error_tip);
                return;
            }

            LatLon phoneLatLon = null, kidLatLon = null;
            String address = null;
            try {
                RecordLocation recordLocation = null;
                if (mMapType == Constants.MAP_TYPE_AMAP) {
                    if (currentPhoneAmapLatLng != null) {
                        phoneLatLon = new LatLon(currentPhoneAmapLatLng.latitude, currentPhoneAmapLatLng.longitude);
                    }
                    recordLocation = (RecordLocation) mAmapCurrentMarker.getObject();
                } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                    if (currentPhoneGmapLatLng != null) {
                        phoneLatLon = new LatLon(currentPhoneGmapLatLng.latitude, currentPhoneGmapLatLng.longitude);
                    }
                    recordLocation = (RecordLocation) mGmapCurrentMarker.getTag();
                }
                if (recordLocation != null) {
                    kidLatLon = new LatLon(recordLocation.latLonPoint.getLatitude(), recordLocation.latLonPoint.getLongitude());
                    address = recordLocation.address;
                    if (address == null)
                        address = "";
                }
            } catch (Exception e) {
                L.w(TAG, "navigationButton.setOnClickListener", e);
            }

            if (kidLatLon == null) {
                return;
            }

            NavigationUtils.onNavigationButtonClick(LocationRecordActivity.this, mDeviceId, phoneLatLon, kidLatLon, address, new NavigationUtils.OnShowSelectMapAppListener() {
                @Override
                public void onShowSelectMapListener(Map<String, String> mapApps, double lat, double lon, String address, boolean shouldWalking) {
                    showSelectMapAppDialog(mapApps, lat, lon, address, shouldWalking);
                }

                @Override
                public void onShowSelectMapNoMapListener() {
                    popInfoDialog(R.string.please_install_map_app);
                }
            });

        });

        onDateChanged(new Date());
    }

    private void showSelectMapAppDialog(final Map<String, String> mapApps, final double lat, final double lon, final String address, final boolean shouldWalking) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.map_select_map);
        final String[] items = new String[mapApps.size()];
        int i = 0;
        for (Map.Entry<String, String> stringStringEntry : mapApps.entrySet()) {
            String name = stringStringEntry.getKey();
            items[i++] = name;
        }
        alert.setItems(items, (dialog, which) -> {
            try {
                NavigationUtils.navigation(LocationRecordActivity.this, mDeviceId, mapApps.get(items[which]), lat, lon, address, shouldWalking);
            } catch (Exception e) {
                L.e(TAG, "showSelectMapAppDialog onClick(" + which + ")", e);
            }
        });
        alert.show();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (mGoogleMapView != null) {
                mGoogleMapView.onResume();
            }
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (mAMapView != null) {
                mAMapView.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (mGoogleMapView != null) {
                mGoogleMapView.onPause();
            }
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (mAMapView != null) {
                mAMapView.onPause();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapType == Constants.MAP_TYPE_AMAP)
            mAMapView.onSaveInstanceState(outState);
        else if (mMapType == Constants.MAP_TYPE_GOOGLE)
            mGoogleMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        synchronized (LocationRecordActivity.this) {
            if (mLoadDataTask != null) {
                mLoadDataTask.cancel(true);
                mLoadDataTask = null;
            }
        }
        super.onDestroy();
        if (mMapType == Constants.MAP_TYPE_GOOGLE) {
            if (mGoogleMapView != null) {
                mGoogleMapView.onDestroy();
            }
        } else if (mMapType == Constants.MAP_TYPE_AMAP) {
            if (mAMapView != null) {
                mAMapView.onDestroy();
            }
        }
    }

    private void initUserInfo() {
        Intent intent = getIntent();
        devid = intent.getStringExtra("devid");
        if (TextUtils.isEmpty(devid)) {
            devid = mDeviceId;
        }
        userid = mUserId;
    }

    private void initAMap() {
        MyLocationStyle myLocationStyle;
        myLocationStyle = new MyLocationStyle();// 初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.interval(2000); // 设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW); // 只定位一次。
        myLocationStyle.showMyLocation(false); // 设置是否显示定位小蓝点，用于满足只想使用定位，不想使用定位小蓝点的场景，设置false以后图面上不再有定位蓝点的概念，但是会持续回调位置信息。
        mAMap.setMyLocationStyle(myLocationStyle);// 设置定位蓝点的Style

        mAMap.getUiSettings().setMyLocationButtonEnabled(false); // 设置默认定位按钮是否显示，非必需设置。
        mAMap.getUiSettings().setZoomControlsEnabled(false); // 不显示缩放按钮
        mAMap.getUiSettings().setRotateGesturesEnabled(false); // 禁用旋转手势
        mAMap.getUiSettings().setTiltGesturesEnabled(false); // 禁用倾斜手势
//        mAMap.getUiSettings().setLogoBottomMargin(-50);// 隐藏logo
        mAMap.getUiSettings().setScaleControlsEnabled(true); // 显示比例尺

        mAMap.setMyLocationEnabled(true); // 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        mAMap.setOnMarkerClickListener(this); // 设置 marker 点击事件监听
        mAMap.setOnMapClickListener(this);

        mAMap.setOnMyLocationChangeListener(this);

        mAMap.setInfoWindowAdapter(this);
        //设置高德地图语言
        setAMapLanguage(mAMap);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.gMap = googleMap;
        com.google.android.gms.maps.UiSettings uiSettings = gMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setRotateGesturesEnabled(false);

        gMap.setOnMapClickListener(this);

        gMap.setInfoWindowAdapter(this);
//        gMap.getUiSettings().setMapToolbarEnabled(false);
        gMap.getUiSettings().setMyLocationButtonEnabled(false); // 设置默认定位按钮是否显示，非必需设置。
        gMap.getUiSettings().setZoomControlsEnabled(false); // 不显示缩放按钮
//        gMap.getUiSettings().setAllGesturesEnabled(false); //禁用所有手势

//        gMap.setMyLocationEnabled(true); // 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        gMap.setOnMarkerClickListener(this); // 设置 marker 点击事件监听
        gMap.setOnMapClickListener(this);
        gMap.setOnMyLocationChangeListener(this);

        //手机主动定位
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 10, this);
        }
    }


    private void onDateChanged(Date date) {
        Calendar nowCal = Calendar.getInstance();
        Calendar selectCal = Calendar.getInstance();
        selectCal.setTime(date);
        if (selectCal.get(Calendar.YEAR) > nowCal.get(Calendar.YEAR) ||
                (selectCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && selectCal.get(Calendar.DAY_OF_YEAR) > nowCal.get(Calendar.DAY_OF_YEAR))) {
            // 未来
            toast(R.string.location_record_time_not_for);
            return;
        }

        boolean needQueryDaysLocation = false;
        if (selectDate != null) {
            Calendar oldCal = Calendar.getInstance();
            oldCal.setTime(selectDate);
            if (selectDate == null || selectCal.get(Calendar.YEAR) != oldCal.get(Calendar.YEAR) || selectCal.get(Calendar.MONTH) != oldCal.get(Calendar.MONTH))
                needQueryDaysLocation = true;
        } else {
            needQueryDaysLocation = true;
        }
        selectDate = date;
        selectDateButton.setText(simpleDateFormat.format(selectDate));
//        loadData(date, needQueryDaysLocation);
        loadDataFromServer(date);
    }

    private void loadData(Date date) {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "loadData: deviceId is empty");
            return;
        }

        LocationEntity locationEntity = null;

        // 从数据库加载
        String dateStr = GreenUtils.LOCATION_RECORD_DATE_FORMAT.format(date);
        LocationEntityDao dao = GreenUtils.getLocationEntityDao();
        List<LocationEntity> list = dao.queryBuilder()
                .where(LocationEntityDao.Properties.DeviceId.eq(deviceId))
                .where(LocationEntityDao.Properties.Date.eq(dateStr))
                .list();
        if (!list.isEmpty()) {
            locationEntity = list.get(0);
            if (!locationEntity.getComplete()) {
                locationEntity = null;
            }
        }

        if (locationEntity != null) {
            synchronized (LocationRecordActivity.this) {
                if (mLoadDataTask != null) {
                    mLoadDataTask.cancel(true);
                }
                mLoadDataTask = new LoadDataTask(LocationRecordActivity.this);
                mLoadDataTask.execute(locationEntity.getLocationRspMsg());
            }
        } else {
            loadDataFromServer(date);
        }
    }

    private void loadDataFromDb(Date date) {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "loadDataFromDb: deviceId is empty");
            return;
        }

        String dateStr = GreenUtils.LOCATION_RECORD_DATE_FORMAT.format(date);
        LocationEntityDao dao = GreenUtils.getLocationEntityDao();
        List<LocationEntity> list = dao.queryBuilder()
                .where(LocationEntityDao.Properties.DeviceId.eq(deviceId))
                .where(LocationEntityDao.Properties.Date.eq(dateStr))
                .list();
        if (!list.isEmpty()) {
            LocationEntity locationEntity = list.get(0);
            synchronized (LocationRecordActivity.this) {
                if (mLoadDataTask != null) {
                    mLoadDataTask.cancel(true);
                }
                mLoadDataTask = new LoadDataTask(LocationRecordActivity.this);
                mLoadDataTask.execute(locationEntity.getLocationRspMsg());
            }
        }
    }

    private void loadDataFromServer(final Date date) {
        int timeBegin, timeEnd;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        timeBegin = (int) (cal.getTimeInMillis() / 1000);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.SECOND, -1);
        timeEnd = (int) (cal.getTimeInMillis() / 1000);

        final protocol.Message.QueryTimeSegmentLocationReqMsg reqMsg = protocol.Message.QueryTimeSegmentLocationReqMsg.newBuilder()
                .setDeviceId(devid)
                .setStartTime(timeBegin)
                .setStopTime(timeEnd)
                .build();

        popWaitingDialog(R.string.location_record_load_data_ing);
        exec(
                reqMsg,

                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            protocol.Message.QueryTimeSegmentLocationRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "loadDataFromServer() -> exec(" + protocol.Message.QueryTimeSegmentLocationReqMsg.class.getSimpleName() + ") -> onResponse()" + rspMsg);
                            Message.ErrorCode errorCode = rspMsg.getErrCode();
                            switch (errorCode) {
                                case SUCCESS:
                                    synchronized (LocationRecordActivity.this) {
                                        if (mLoadDataTask != null) {
                                            mLoadDataTask.cancel(true);
                                        }
                                        mLoadDataTask = new LoadDataTask(LocationRecordActivity.this);
                                        mLoadDataTask.execute(rspMsg);

                                        //重置当前宝贝maker
                                        hideAddressCard();
                                        if (mMapType == Constants.MAP_TYPE_AMAP) {
                                            mAmapCurrentMarker = null;
                                        } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                                            mGmapCurrentMarker = null;
                                        }
                                    }
                                    if (rspMsg.getPositionsCount() > 0) {
                                        GreenUtils.saveLocationAsync(reqMsg.getDeviceId(), date, rspMsg);
                                    }
                                    break;
                                default:
                                    L.w(TAG, "loadDataFromServer failure: " + rspMsg);
                                    break;
                            }
                        } catch (Exception e) {
                            L.e(TAG, "loadDataFromServer() -> exec(" + protocol.Message.QueryTimeSegmentLocationReqMsg.class.getSimpleName() + ") -> onResponse() process failure", e);
                            popErrorDialog(R.string.location_record_load_fail, hud -> loadDataFromDb(date));
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "loadDataFromServer() -> exec(" + protocol.Message.QueryTimeSegmentLocationReqMsg.class.getSimpleName() + ") -> onException()", cause);
                        String errStr;
                        if (cause instanceof TimeoutException) {
                            errStr = getString(R.string.request_timed_out);
                        } else if (cause instanceof NotYetConnectedException || cause instanceof IOException) {
                            errStr = getString(R.string.network_quality_poor);
                        } else if (cause instanceof NotYetLoginException) {
                            errStr = getString(R.string.network_quality_poor);
                        } else {
                            errStr = getString(R.string.location_record_load_fail);
                        }
                        popErrorDialog(errStr, hud -> loadDataFromDb(date));
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        // never to here for this exec
                    }
                }
        );
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public View getInfoContents(com.google.android.gms.maps.model.Marker marker) {
        return null;
    }


    private static class LoadDataTask extends AsyncTask<Message.QueryTimeSegmentLocationRspMsg, Object, String> {
        WeakReference<LocationRecordActivity> mA;
        LatLngBounds.Builder amapBoundsBuilder = new LatLngBounds.Builder();
        com.google.android.gms.maps.model.LatLngBounds.Builder gmapBoundsBuiler = new com.google.android.gms.maps.model.LatLngBounds.Builder();

        LoadDataTask(LocationRecordActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        protected void onPreExecute() {
            LocationRecordActivity a = mA.get();
            if (a == null)
                return;
            a.records.clear();
            if (a.mMapType == Constants.MAP_TYPE_GOOGLE && a.gMap != null) {
                a.gMap.clear();
            } else if (a.mMapType == Constants.MAP_TYPE_AMAP && a.mAMap != null) {
                a.mAMap.clear();
            }
        }


        @Override
        protected String doInBackground(Message.QueryTimeSegmentLocationRspMsg... queryTimeSegmentLocationRspMsgs) {
            LocationRecordActivity a = mA.get();
            if (a == null)
                return null;
            Message.QueryTimeSegmentLocationRspMsg rspMsg = queryTimeSegmentLocationRspMsgs[0];
            if (rspMsg == null)
                return null;

            List<protocol.Message.PositionRecord> positionRecords = rspMsg.getPositionsList();
            double satelliteMergeDistance = rspMsg.getSatelliteMergeDistance();
            if (satelliteMergeDistance < 0.01) {
                satelliteMergeDistance = 200;
            }
            double wifiMergeDistance = rspMsg.getWifiMergeDistance();
            if (wifiMergeDistance < 0.01) {
                wifiMergeDistance = 500;
            }
            double cellMergeDistance = rspMsg.getCellMergeDistance();
            if (cellMergeDistance < 0.01) {
                cellMergeDistance = 500;
            }

            List<TrackPoint> points = TrackPoint.merge(positionRecords, satelliteMergeDistance, wifiMergeDistance, cellMergeDistance);
            int i = 0;
            for (TrackPoint point : points) {
                ++i;
                point.setSerialNumber(i);

                if (isCancelled())
                    return null;

                View view = LayoutInflater.from(a).inflate(R.layout.marker, null);
                ((TextView) view.findViewById(R.id.serial_number)).setText(String.valueOf(i));

                if (a.mMapType == Constants.MAP_TYPE_AMAP) {
                    LatLng latLng = new LatLng(point.getLat(), point.getLon());
                    MarkerOptions markerOptions = new MarkerOptions()
                            .anchor(40f / 80f, 66f / 80f)
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.fromView(view));

                    if (isCancelled())
                        return null;
                    publishProgress(point, markerOptions);

                    amapBoundsBuilder.include(latLng);
                } else if (a.mMapType == Constants.MAP_TYPE_GOOGLE) {
                    com.google.android.gms.maps.model.LatLng latLng = new com.google.android.gms.maps.model.LatLng(point.getLat(), point.getLon());
                    com.google.android.gms.maps.model.MarkerOptions markerOptions = new com.google.android.gms.maps.model.MarkerOptions()
                            .anchor(40f / 80f, 66f / 80f)
                            .position(latLng)
                            .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(ImageCacheUtils.getBitmaByView(view)));

                    if (isCancelled())
                        return null;
                    publishProgress(point, markerOptions);
                    gmapBoundsBuiler.include(latLng);
                }

            }

            if (positionRecords.isEmpty()) {
                return "";
            } else {
                return String.valueOf(i);
            }
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            LocationRecordActivity a = mA.get();
            if (a == null)
                return;
            TrackPoint point = (TrackPoint) values[0];
            Object markerOptions = values[1];
            a.addMarker(point, markerOptions);
        }

        @Override
        protected void onPostExecute(String s) {
            LocationRecordActivity a = mA.get();
            if (a == null)
                return;

            synchronized (a) {
                a.mLoadDataTask = null;
            }

            a.dismissDialog();

            if (s == null) {
            } else if (s.isEmpty()) {
                a.showDeviceLastLocate();
            } else {
                if (a.mMapType == Constants.MAP_TYPE_AMAP && a.mAMap != null) {
                    a.mAMap.animateCamera(CameraUpdateFactory.newLatLngBounds(amapBoundsBuilder.build(), 80));//第二个参数为四周留空宽度
                } else if (a.mMapType == Constants.MAP_TYPE_GOOGLE && a.gMap != null) {
                    a.gMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(gmapBoundsBuiler.build(), 80));
                }
            }
        }
    }

    private void addMarker(TrackPoint point, Object markerOptions) {
        if (markerOptions instanceof com.google.android.gms.maps.model.MarkerOptions) {
            if (gMap == null)
                return;
            com.google.android.gms.maps.model.Marker marker = gMap.addMarker((com.google.android.gms.maps.model.MarkerOptions) markerOptions);
            recordLocations.add(new RecordLocation(point, marker));
        } else if (markerOptions instanceof MarkerOptions) {
            Marker marker = mAMap.addMarker((MarkerOptions) markerOptions);
            recordLocations.add(new RecordLocation(point, marker));
        }
    }

    private void showDeviceLastLocate() {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "fetchDeviceLocate deviceId is isEmpty");
            return;
        }

        exec(
                Message.GetDeviceLastPositionReqMsg.newBuilder().setDeviceId(deviceId).build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.GetDeviceLastPositionRspMsg fetchDevLocRspMsg = response.getProtoBufMsg();
                            L.v(TAG, "showDeviceLastLocate() -> exec() -> onResponse(): " + fetchDevLocRspMsg);
                            if (fetchDevLocRspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                double lat = fetchDevLocRspMsg.getPosition().getLatLng().getLatitude();
                                double lng = fetchDevLocRspMsg.getPosition().getLatLng().getLongitude();
                                if (mMapType == Constants.MAP_TYPE_AMAP) {
                                    LatLng latLng = new LatLng(lat, lng);
                                    mAMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
                                } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                                    if (gMap != null) {
                                        com.google.android.gms.maps.model.LatLng latLng = new com.google.android.gms.maps.model.LatLng(lat, lng);
                                        gMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
                                    }
                                }

                                return true;
                            }
                        } catch (Exception e) {
                            L.e(TAG, "showDeviceLastLocate() -> exec() -> onResponse()", e);
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "showDeviceLastLocate() -> exec() -> onException()", cause);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }
        );
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.select_date_btn: {
//                Intent mIntent = new Intent(this, CalendarActivity.class);
//                mIntent.putExtra(CalendarActivity.PARAM_SELECTED_DAY, simpleDateFormat.format(seletecDate));
//                mIntent.putStringArrayListExtra(CalendarActivity.PARAM_MARK_DAY_LIST, dateListOfHasData);
//                startActivityForResult(mIntent, DATEPICKER_ACTIVITY);
                android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
                CalendarDialogFragment calendarDialogFragment = new CalendarDialogFragment();
                Bundle bundle = new Bundle();
                bundle.putString(CalendarActivity.PARAM_SELECTED_DAY, simpleDateFormat.format(selectDate));
//                bundle.putStringArrayList(CalendarActivity.PARAM_MARK_DAY_LIST, dateListOfHasData);
                calendarDialogFragment.setArguments(bundle);
                calendarDialogFragment.show(fragmentManager, "fragment_bottom_dialog");
            }
            break;
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.map_zoom_in:
                if (mMapType == Constants.MAP_TYPE_AMAP && mAMap != null) {
                    mAMap.animateCamera(CameraUpdateFactory.zoomIn());
                } else if (mMapType == Constants.MAP_TYPE_GOOGLE && gMap != null) {
                    gMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.zoomIn());
                }
                break;
            case R.id.map_zoom_out:
                if (mMapType == Constants.MAP_TYPE_AMAP && mAMap != null) {
                    mAMap.animateCamera(CameraUpdateFactory.zoomOut());
                } else if (mMapType == Constants.MAP_TYPE_GOOGLE && gMap != null) {
                    gMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.zoomOut());
                }
                break;
            case R.id.map_type:
                if (mMapType == Constants.MAP_TYPE_AMAP) {
                    if (mAMap == null)
                        break;
                    if (mAMap.getMapType() == AMap.MAP_TYPE_NORMAL) {
                        mAMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                        ((ImageButton) findViewById(R.id.map_type)).setImageResource(R.drawable.change_button_change);
                    } else {
                        mAMap.setMapType(AMap.MAP_TYPE_NORMAL);
                        ((ImageButton) findViewById(R.id.map_type)).setImageResource(R.drawable.change_button);
                        setAMapLanguage(mAMap);
                    }
                } else if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                    if (gMap == null)
                        break;
                    if (gMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                        gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        ((ImageButton) findViewById(R.id.map_type)).setImageResource(R.drawable.change_button_change);
                    } else {
                        gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        ((ImageButton) findViewById(R.id.map_type)).setImageResource(R.drawable.change_button);
                    }
                }
                break;

            case R.id.pre_day:
            case R.id.pre_day_label:
                getOtherDayData(R.id.pre_day);
                break;
            case R.id.next_day:
            case R.id.next_day_label:
                getOtherDayData(R.id.next_day);
                break;
            case R.id.back:
                this.finish();
                break;
            default:
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getDataFromCalendarDialogFragment(Event.CalendarOnClickData calendarOnClickData) {
        String dateStr = calendarOnClickData.getDate();
        if (TextUtils.isEmpty(dateStr))
            return;
        try {
            Date date = simpleDateFormat.parse(dateStr);
            onDateChanged(date);
        } catch (Exception e) {
            L.e(TAG, "onActivityResult Result date string invalid", e);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCalendarOnMonthChanged(final Event.CalendarOnMonthChanged monthChanged) {
        Calendar base = Calendar.getInstance();
        Calendar cal = (Calendar) base.clone();
        cal.setTime(monthChanged.getDate());
        if (cal.get(Calendar.YEAR) > base.get(Calendar.YEAR) ||
                (cal.get(Calendar.YEAR) == base.get(Calendar.YEAR) && cal.get(Calendar.MONTH) > base.get(Calendar.MONTH))) {
            // 未来
            return;
        }
        if (daysLocationQueriedDate != null) {
            base.setTime(daysLocationQueriedDate);
            if (cal.get(Calendar.YEAR) == base.get(Calendar.YEAR) && cal.get(Calendar.MONTH) == base.get(Calendar.MONTH)) {
                // 已经请求过了
                return;
            }
        }
        daysLocationQueriedDate = monthChanged.getDate();
        queryDaysLocation(daysLocationQueriedDate);
    }

    private void queryDaysLocation(final Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        // 我们需要计算这一月的第一天的00:00:00，与这一月的最后一天的23:59:59
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.SECOND, -1);
        // cal 现在的时间是这一天的 23:59:59
        // 先调整到下一月的第一天，
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, 1);
        // 然后再减去一天，得到这一月最后一天的23:59:59
        cal.add(Calendar.DAY_OF_MONTH, -1);
        int timeEnd = (int) (cal.getTimeInMillis() / 1000);

        // 调整到这一月的第一天的00:00:00
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        int timeBegin = (int) (cal.getTimeInMillis() / 1000);

        protocol.Message.QueryDaysLocationReqMsg reqMsg2 = protocol.Message.QueryDaysLocationReqMsg.newBuilder()
                .setDeviceId(devid)
                .setStartTime(timeBegin)
                .setStopTime(timeEnd)
                .build();

        exec(
                reqMsg2,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        List<String> daysList = new ArrayList<>();
                        try {
                            Message.QueryDaysLocationRspMsg rspMsg = response.getProtoBufMsg();
                            L.d(TAG, "loadDataFromServer() -> exec(" + protocol.Message.QueryDaysLocationReqMsg.class.getSimpleName() + ") -> onResponse(): " + rspMsg);
                            List<String> days = rspMsg.getDaysList();
                            if (days != null) {
                                for (String day : days) {
                                    if (TextUtils.isEmpty(day))
                                        continue;
                                    daysList.add(day);
                                }
                            }
                        } catch (Exception e) {
                            L.e(TAG, "loadDataFromServer() -> exec(" + protocol.Message.QueryDaysLocationReqMsg.class.getSimpleName() + ") -> onResponse() process failure", e);
                            if (daysLocationQueriedDate == date) {
                                daysLocationQueriedDate = null;
                            }
                        }
                        EventBus.getDefault().postSticky(new Event.DaysLocationOfDevice(date, daysList));
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "loadDataFromServer() -> exec(" + protocol.Message.QueryDaysLocationReqMsg.class.getSimpleName() + ") -> onException()", cause);
                        if (daysLocationQueriedDate == date) {
                            daysLocationQueriedDate = null;
                        }
                        EventBus.getDefault().postSticky(new Event.DaysLocationOfDevice(date, null));
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }
        );
    }

    private void getOtherDayData(int v_id) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectDate);
        if (v_id == R.id.pre_day) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        } else {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        onDateChanged(cal.getTime());
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        L.e(TAG, "marker onclick :" + marker.getPosition().toString());
        RecordLocation recordLocation = (RecordLocation) marker.getObject();
        if (recordLocation == null)
            return false;

        //上一个选中的marker
        if (mAmapCurrentMarker != null && mAmapCurrentMarker != marker) {
            View view = LayoutInflater.from(this).inflate(R.layout.marker, null);
            TextView serial_number = view.findViewById(R.id.serial_number); //setText(String.valueOf(recordLocation.point.getSerialNumber()));
            ImageView markerIcon = view.findViewById(R.id.map_position); //.setBackgroundResource(R.drawable.location_blue_history);

            RecordLocation recordLocationLast = (RecordLocation) mAmapCurrentMarker.getObject();
            serial_number.setText(String.valueOf(recordLocationLast.point.getSerialNumber()));
            markerIcon.setBackgroundResource(R.drawable.location_blue_history);
            mAmapCurrentMarker.setIcon(BitmapDescriptorFactory.fromView(view));
        }
        //当前点击marker
        View view = LayoutInflater.from(this).inflate(R.layout.marker, null);
        TextView serial_number = view.findViewById(R.id.serial_number); //setText(String.valueOf(recordLocation.point.getSerialNumber()));
        ImageView markerIcon = view.findViewById(R.id.map_position); //.setBackgroundResource(R.drawable.location_blue_history);

        mAmapCurrentMarker = recordLocation.mAMapMarker;
        markerIcon.setBackgroundResource(R.drawable.location_red_history);
        serial_number.setText(String.valueOf(recordLocation.point.getSerialNumber()));
        mAmapCurrentMarker.setIcon(BitmapDescriptorFactory.fromView(view));

        if (TextUtils.isEmpty(recordLocation.address)) {
            getAddress(recordLocation.latLonPoint);
            L.v(TAG, "onMarkerClick getAddress()");
        } else {
            showAddressCard(recordLocation);
            L.v(TAG, "onMarkerClick marker.showAddressCard(): " + recordLocation.address);
        }
        return true;
    }

    @Override
    public boolean onMarkerClick(com.google.android.gms.maps.model.Marker marker) {
        RecordLocation recordLocation = (RecordLocation) marker.getTag();
        assert recordLocation != null;


        //上一个选中的marker
        if (mGmapCurrentMarker != null && mGmapCurrentMarker != marker) {
            View view = LayoutInflater.from(this).inflate(R.layout.marker, null);
            TextView serial_number = view.findViewById(R.id.serial_number); //setText(String.valueOf(recordLocation.point.getSerialNumber()));
            ImageView markerIcon = view.findViewById(R.id.map_position); //.setBackgroundResource(R.drawable.location_blue_history);

            RecordLocation recordLocationLast = (RecordLocation) mGmapCurrentMarker.getTag();
            serial_number.setText(String.valueOf(recordLocationLast.point.getSerialNumber()));
            markerIcon.setBackgroundResource(R.drawable.location_blue_history);
            mGmapCurrentMarker.setIcon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(ImageCacheUtils.getBitmaByView(view)));
        }
        //当前点击marker
        View view = LayoutInflater.from(this).inflate(R.layout.marker, null);
        TextView serial_number = view.findViewById(R.id.serial_number); //setText(String.valueOf(recordLocation.point.getSerialNumber()));
        ImageView markerIcon = view.findViewById(R.id.map_position); //.setBackgroundResource(R.drawable.location_blue_history);

        mGmapCurrentMarker = recordLocation.mGmapMarker;
        markerIcon.setBackgroundResource(R.drawable.location_red_history);
        serial_number.setText(String.valueOf(recordLocation.point.getSerialNumber()));
        mGmapCurrentMarker.setIcon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(ImageCacheUtils.getBitmaByView(view)));


        if (TextUtils.isEmpty(recordLocation.address) && Geocoder.isPresent()) {
            // Since the geocoding API is synchronous and may take a while.  You don't want to lock
            // up the UI thread.  Invoking reverse geocoding in an AsyncTask.
            GoogleRegeocodeTask task = new GoogleRegeocodeTask(this, this);
            task.setTag(recordLocation);
            task.execute(recordLocation.point.getLat(), recordLocation.point.getLon());
        } else {
            showAddressCard(recordLocation);
            L.v(TAG, "onMarkerClick marker.showAddressCard(): " + recordLocation.address);
        }
        return true;
    }


    public synchronized void showAddressCard(RecordLocation recordLocation) {

        String locationTime = "";
        Long beginT = recordLocation.point.getLocateTimeList().get(0);
        if (beginT != null && beginT != 0) {
            long epochTime = beginT;
            locationTime = StringUtils.getStrDate(Utils.epochToDate((int) epochTime), "HH:mm");
        }
        if (recordLocation.point.getLocateTimeList().size() > 1) {
            Long endT = recordLocation.point.getLocateTimeList().get(recordLocation.point.getLocateTimeList().size() - 1);
            if (endT != null && endT != 0) {
                if (!TextUtils.isEmpty(locationTime)) {
                    locationTime += " -- ";
                }
                long epochTime = endT;
                locationTime += StringUtils.getStrDate(Utils.epochToDate((int) epochTime), "HH:mm");
            }
        }

        mapAddrShortView.setText(recordLocation.poi);
        mapAddrFullView.setText(recordLocation.address);
        locationTimeView.setText(locationTime);
        if (mAddrCard.getVisibility() != View.VISIBLE) {
            mAddrCard.setVisibility(View.VISIBLE);
        }
    }

    public synchronized void hideAddressCard() {
        if (mAddrCard == null)
            return;
        if (mAddrCard.getVisibility() == View.VISIBLE)
            mAddrCard.setVisibility(View.GONE);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (mAmapCurrentMarker != null)
            hideAddressCard();
    }

    @Override
    public void onMapClick(com.google.android.gms.maps.model.LatLng latLng) {
        if (mGmapCurrentMarker != null)
            hideAddressCard();
    }


    @Override
    public void onMyLocationChange(Location location) {
        L.d(TAG, "onMyLocationChange: " + location);
        if (mAMap == null)
            return;
        currentPhoneAmapLatLng = new LatLng(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onLocationChanged(Location location) {
        L.d(TAG, "onMyLocationChange: " + location);
        if (gMap == null)
            return;
        currentPhoneGmapLatLng = new com.google.android.gms.maps.model.LatLng(location.getLatitude(), location.getLongitude());
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


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case DATEPICKER_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    String dateStr = data.getStringExtra(CalendarActivity.RETURN_SELECTED_DATE);
                    if (TextUtils.isEmpty(dateStr))
                        break;
                    try {
                        Date date = simpleDateFormat.parse(dateStr);
                        onDateChanged(date);
                    } catch (Exception e) {
                        L.e(TAG, "onActivityResult Result date string invalid", e);
                    }
                }
                break;
            default:
                break;
        }
    }


    //获取坐标对应的地理信息
    private void getAddress(final LatLonPoint latLonPoint) {
        L.i("获取坐标对应的地理信息: " + latLonPoint.toString());
        if (geocoderSearch == null) {
            geocoderSearch = new GeocodeSearch(this);
        }
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP);// 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        geocoderSearch.getFromLocationAsyn(query);// 设置同步逆地理编码请求
    }

    /**
     * 经纬度获取详细 address info
     *
     * @param result: 地理编码
     * @param rCode:
     */
    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        L.v(TAG, "onRegeocodeSearched  rCode= " + rCode + " result: " + result);
        if (rCode != 1000 || result == null) {
            return;
        }
        String address = null, poi = null;
        RegeocodeAddress regeAddr = result.getRegeocodeAddress();
        if (regeAddr != null) {
            if (regeAddr.getPois() != null && !regeAddr.getPois().isEmpty()) {
                poi = regeAddr.getPois().get(0).getTitle();
            } else {
                poi = regeAddr.getCity() + regeAddr.getDistrict();
            }
            address = regeAddr.getFormatAddress();
        }

        LatLonPoint latLonPoint = result.getRegeocodeQuery().getPoint();
        for (RecordLocation one : recordLocations) {
            if (one.latLonPoint == latLonPoint) {
//                L.v(TAG, "onRegeocodeSearched fond RecordLocation.marker: " + one.mMarker + ", mCurrentMarker: " + mCurrentMarker);
                one.setAddress(address, poi);
                if (mAmapCurrentMarker == one.mAMapMarker) {
                    L.v(TAG, "onRegeocodeSearched is current marker");
                    showAddressCard(one);
                }
            }
        }
    }

    /**
     * 详细地址获取经纬度
     *
     * @param geocodeResult：地理编码
     * @param i                  ：
     */
    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    @Override
    public void onGoogleRegeocode(GoogleRegeocodeTask task, Throwable throwable, int httpStatusCode, GoogleRegeocodeResult geocodeResult, String formattedAddress, String pointOfInterest) {
        RecordLocation recordLocation = (RecordLocation) task.getTag();
        for (RecordLocation one : recordLocations) {
            if (one == recordLocation) {
                one.setAddress(formattedAddress, pointOfInterest);
                if (mGmapCurrentMarker == one.mGmapMarker) {
                    L.v(TAG, "onRegeocodeSearched is current marker address: " + formattedAddress);
                    showAddressCard(one);
                }
            }
        }
    }

    @Override
    public View getInfoWindow(com.google.android.gms.maps.model.Marker marker) {
        return initInfoWindowData(marker);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return initInfoWindowData(marker);
    }

    private View initInfoWindowData(Object marker) {
        RecordLocation recordLocation = null;
        if (marker instanceof com.google.android.gms.maps.model.Marker) {
            recordLocation = (RecordLocation) ((com.google.android.gms.maps.model.Marker) marker).getTag();
        } else if (marker instanceof Marker) {
            recordLocation = (RecordLocation) ((Marker) marker).getObject();
        }
        if (recordLocation == null && recordLocation.address == null)
            return null;

        String locationTime = "";
        Long beginT = recordLocation.point.getLocateTimeList().get(0);
        if (beginT != null && beginT != 0) {
            long epochTime = beginT;
            locationTime = StringUtils.getStrDate(Utils.epochToDate((int) epochTime), "yyyy-MM-dd HH:mm");
        }
        if (recordLocation.point.getLocateTimeList().size() > 1) {
            Long endT = recordLocation.point.getLocateTimeList().get(recordLocation.point.getLocateTimeList().size() - 1);
            if (endT != null && endT != 0) {
                if (!TextUtils.isEmpty(locationTime)) {
                    locationTime += " -- ";
                }
                long epochTime = endT;
                locationTime += StringUtils.getStrDate(Utils.epochToDate((int) epochTime), "yyyy-MM-dd HH:mm");
            }
        }

        View infoWind = getInfoWindowView();
        ((TextView) infoWind.findViewById(R.id.map_address)).setText(recordLocation.address);
        ((TextView) infoWind.findViewById(R.id.map_addr_description)).setText(recordLocation.poi);
        ((TextView) infoWind.findViewById(R.id.map_dev_last_login_time)).setText(locationTime);

        StringBuilder sb = new StringBuilder();
        // TODO: 事件
        long flag = 0;
        for (TrackPoint.Incident incident : recordLocation.point.getIncidentList()) {
            flag |= incident.getIncident().getFlag();
        }
        if ((flag & Message.Incident.IncidentFlag.IN_CALL_VALUE) != 0) {
            sb.append(getString(R.string.location_record_load_call_in) + " ");
        }
        if ((flag & Message.Incident.IncidentFlag.OUT_CALL_VALUE) != 0) {
            sb.append(getString(R.string.location_record_load_call_out) + " ");
        }
        if ((flag & Message.Incident.IncidentFlag.SOS_VALUE) != 0) {
            sb.append(getString(R.string.location_record_sos) + " ");
        }
        if ((flag & Message.Incident.IncidentFlag.LOW_BATTERY_VALUE) != 0) {
            sb.append(getString(R.string.location_record_low_power) + " ");
        }
        if ((flag & Message.Incident.IncidentFlag.POWER_ON_VALUE) != 0) {
            sb.append(getString(R.string.location_record_start_up) + " ");
        }
        if ((flag & Message.Incident.IncidentFlag.POWER_OFF_VALUE) != 0) {
            sb.append(getString(R.string.location_record_power_off) + " ");
        }
        if ((flag & Message.Incident.IncidentFlag.OFF_WRIST_VALUE) != 0) {
            sb.append(getString(R.string.location_record_get_hand) + " ");
        }
        if ((flag & Message.Incident.IncidentFlag.SOAK_WATER_VALUE) != 0) {
            sb.append(getString(R.string.location_record_water) + " ");
        }
        if ((flag & Message.Incident.IncidentFlag.REPORT_LOSS_VALUE) != 0) {
            sb.append(getString(R.string.location_record_lose) + " ");
        }
        if ((flag & Message.Incident.IncidentFlag.FENCE_VALUE) != 0) {
            sb.append(getString(R.string.location_record_fence) + " ");
        }
        String incident = sb.toString();
        if (TextUtils.isEmpty(incident))
            incident = getString(R.string.location_record_not_thing);

        ((TextView) infoWind.findViewById(R.id.map_incident)).setText(incident);
        return infoWind;
    }

    private View getInfoWindowView() {
        if (infoWindowView == null)
            infoWindowView = LayoutInflater.from(LocationRecordActivity.this).inflate(R.layout.view_map_record_infowindow, null);
        return infoWindowView;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    /**
     * 当前需要显示的 Location
     */
    private View infoWindowView;

    private class RecordLocation {
        TrackPoint point;
        private Marker mAMapMarker;
        private com.google.android.gms.maps.model.Marker mGmapMarker;
        private String address, poi;

        LatLonPoint latLonPoint;

        RecordLocation(TrackPoint point, Marker marker) {
            this.point = point;
            mAMapMarker = marker;
            LatLng latLng = marker.getPosition();
            latLonPoint = new LatLonPoint(latLng.latitude, latLng.longitude);
            marker.setObject(this);
        }

        RecordLocation(TrackPoint point, com.google.android.gms.maps.model.Marker marker) {
            this.point = point;
            mGmapMarker = marker;
            marker.setTag(this);
        }


        public void setAddress(String address, String poi) {
            this.address = address;
            this.poi = poi;
        }
    }

    //按照定位时间 排序
    private SparseArray<LatLng> sortSparseArray(SparseArray<LatLng> arr) {
        List<Integer> keys = new ArrayList<>();
        List<LatLng> vals = new ArrayList<>();

        for (int i = 0; i < arr.size(); i++) {
            keys.add(arr.keyAt(i));
            vals.add(arr.get(keys.get(i)));
        }
        Collections.sort(keys);
        arr.clear();
        for (int i = 0; i < keys.size(); i++) {
            arr.put(keys.get(i), vals.get(i));
        }
        return arr;
    }

    @Override
    public void onDeviceUnbind(TlcService tlcService, @Nullable Pkt reqPkt, @NonNull protocol.Message.NotifyUserUnbindDevReqMsg reqMsg) {
        if (reqMsg.getUsrDevAssoc().getUserId().equals(mUserId) && reqMsg.getUsrDevAssoc().getDeviceId().equals(mDeviceId)) {
            L.w(TAG, "current device unbound!!! Activity finish");
            finish();
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
}