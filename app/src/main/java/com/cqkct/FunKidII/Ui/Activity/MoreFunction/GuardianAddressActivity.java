package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.Bean.GuardianAddrInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Adapter.LocateRecyclerAdapter;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Utils.L;

import java.util.ArrayList;
import java.util.List;

import protocol.Message;

import static com.cqkct.FunKidII.Ui.Activity.MoreFunction.GuardianActivity.GUARDIAN_ADDRESS_DATA;
import static com.cqkct.FunKidII.Ui.Activity.MoreFunction.GuardianFenceActivity.SCHOOLFENCE_TO_ADDRESSFENCE;


/**
 * Created by Administrator on 2018/3/15.
 */

public class GuardianAddressActivity extends BaseActivity implements
        LocateRecyclerAdapter.OnLocationItemClick, PoiSearch.OnPoiSearchListener, GeocodeSearch.OnGeocodeSearchListener {

    private final String TAG = this.getClass().getSimpleName();
    public static final String SCHOOLADDRESS_TO_SCHOOLFENCE = "SCHOOLADDRESS_TO_SCHOOLFENCE";
    public static final String ADD_ADDRESS_BY_HAND_DATA = "ADD_ADDRESS_BY_HAND_DATA";

    public static final int ADDRESS_ITEM_ONCLICK_FLAG = 1;
    public static final int ADD_ADDRESS_BY_HAND_FLAG = 2;
    private EditText et_search;
    private RecyclerView mLocateRecycler;
    private GeocodeSearch mGeocodeSearch;

    private LocateRecyclerAdapter mAdapter;
    private String searchKey;
    private TextView currentPosition;
    private boolean is_home;
    private List<GuardianAddrInfo> addrList = new ArrayList<>();
    private TextView address_null;
    private TextView tv_nearbyPosition;
    private TextView recommendText;
    private LatLonPoint searchCenterLatLonPoint;
    private int radius;
    private EditText fence_address_name;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.school_address);

        if (TextUtils.isEmpty(mUserId) || TextUtils.isEmpty(mDeviceId)) {
            finish();
            return;
        }

        tv_nearbyPosition = findViewById(R.id.nearby_position);
        Intent intent = this.getIntent();
        is_home = intent.getBooleanExtra(GuardianActivity.GUARDIAN_ADDRESS_HOMEORSCHOOL, false);
        radius = intent.getIntExtra(GuardianActivity.GUARDIAN_ADDRESS_FENCE_RADIUS, 0);
        setTitleBarTitle(is_home ? getString(R.string.school_guide_home_address) : getString(R.string.school_guide_school_address));
        searchKey = is_home ? getString(R.string.school_guide_business_house) : getString(R.string.school_guide_school);
//        tv_nearbyPosition.setText(is_home ? getString(R.string.school_guide_nearby_add) : getString(R.string.school_guide_nearby_school));
        tv_nearbyPosition.setText(getString(R.string.school_guide_nearby_add));

        initView();
        et_search.setHint(is_home ? getString(R.string.fence_home_input_name) : getString(R.string.fence_school_input_name));
        mGeocodeSearch = new GeocodeSearch(this);
        mGeocodeSearch.setOnGeocodeSearchListener(this);

        if (false) { // 目前我们使用初始化为设备的当前位置
            double lat = intent.getDoubleExtra(GuardianActivity.GUARDIAN_ADDRESS_FENCE_LAT, 1000);
            double lon = intent.getDoubleExtra(GuardianActivity.GUARDIAN_ADDRESS_FENCE_LON, 1000);
            if (lat < 1000 && lon < 1000) {
                searchCenterLatLonPoint = new LatLonPoint(lat, lon);
                recommendText.setText(R.string.school_guide_current_addr);
            }
        }
        if (searchCenterLatLonPoint == null) {
            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
            if (deviceInfo != null) {
                Message.Position position = deviceInfo.getLastPosition();
                if (position != null) {
                    Message.LatLon latLon = deviceInfo.getLastPosition().getLatLng();
                    if (latLon != null && latLon.getLatitude() != 0 && latLon.getLongitude() != 0) {
                        searchCenterLatLonPoint = new LatLonPoint(latLon.getLatitude(), latLon.getLongitude());
                        recommendText.setText(R.string.school_guide_nearby_watch);
                    }
                }
            }
        }
        if (searchCenterLatLonPoint == null) {
            initLocate();
        } else {
            getAddressByLatlng(searchCenterLatLonPoint.getLatitude(), searchCenterLatLonPoint.getLongitude());
            doSearchPoi("", searchCenterLatLonPoint);
        }
    }

    public void initView() {
        address_null = findViewById(R.id.address_null);
        recommendText = findViewById(R.id.sch_location_label);
        mLocateRecycler = findViewById(R.id.sch_recycler);
        currentPosition = findViewById(R.id.tv_sch_dev_position);
        mAdapter = new LocateRecyclerAdapter(this, addrList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mLocateRecycler.setLayoutManager(layoutManager);
        mLocateRecycler.setAdapter(mAdapter);
        if (!addrList.isEmpty())
            address_null.setVisibility(View.GONE);
        mAdapter.setLocationItemClick(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        et_search = findViewById(R.id.search);
        et_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //启动定位
                searchKey = et_search.getText().toString();
                searchPoi(searchKey);
            }
        });
    }

    private void searchPoi(@NonNull String key) {
        if (searchCenterLatLonPoint == null) {
            initLocate();
        } else {
            doSearchPoi(key, searchCenterLatLonPoint);
        }
    }

    protected void doSearchPoi(@NonNull String key, @NonNull LatLonPoint latLonPoint) {
        addrList.clear();
        mAdapter.notifyDataSetChanged();
        address_null.setVisibility(View.VISIBLE);
        PoiSearch.Query query = new PoiSearch.Query(key, "", "");
        query.setPageSize(30);
        PoiSearch search = new PoiSearch(this, query);
        //50 公里 内
        search.setBound(new PoiSearch.SearchBound(latLonPoint, 500000));
        search.setOnPoiSearchListener(this);
        search.searchPOIAsyn();
    }

//    @Override
    // TODO:
//    public void onLocationChanged(AMapLocation amapLocation) {
//        if (amapLocation != null) {
//            if (amapLocation.getErrorCode() == 0) {
//                //定位成功回调信息，设置相关消息
//                if (searchCenterLatLonPoint == null) {
//                    recommendText.setText(R.string.school_guide_nearby_phone);
//                    double latitude = amapLocation.getLatitude();//获取纬度
//                    double longitude = amapLocation.getLongitude();//获取经度
//                    searchCenterLatLonPoint = new LatLonPoint(latitude, longitude);
//                    getAddressByLatlng(latitude, longitude);
//                    doSearchPoi(searchKey, searchCenterLatLonPoint);
//                }
//            } else {
//                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
//                L.e("AmapError", "location Error, ErrCode:"
//                        + amapLocation.getErrorCode() + ", errInfo:"
//                        + amapLocation.getErrorInfo());
//            }
//        }
//    }

    private void initLocate() {
        // TODO:
    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int i) {
//        PoiSearch.Query query = poiResult.getQuery();
        ArrayList<PoiItem> pois = poiResult.getPois();
        address_null.setVisibility(View.GONE);
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
            addrList.add(addr);
            L.d(TAG, "schoolName" + schoolName + "schoolAddress: " + schoolAddress);
        }

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

    private void getAddressByLatlng(double latitude, double longitude) {
        //逆地理编码查询条件：逆地理编码查询的地理坐标点、查询范围、坐标类型。
        LatLonPoint latLonPoint = new LatLonPoint(latitude, longitude);
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 500f, GeocodeSearch.AMAP);
        //异步查询
        mGeocodeSearch.getFromLocationAsyn(query);
    }

    @Override
    public void OnItemClick(RecyclerView parent, View view, int position, GuardianAddrInfo addr) {
        Intent intent = new Intent(this, GuardianFenceActivity.class);
        intent.putExtra(SCHOOLADDRESS_TO_SCHOOLFENCE, addr);
        intent.putExtra(GuardianActivity.IS_GUARDIAN_HOME_ADDRESS, is_home);
        startActivityForResult(intent, ADDRESS_ITEM_ONCLICK_FLAG);
        L.e(TAG, "OnItemClick: position:" + position);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == ADDRESS_ITEM_ONCLICK_FLAG && resultCode == RESULT_OK)
                || (requestCode == ADD_ADDRESS_BY_HAND_FLAG && resultCode == RESULT_OK)) {
            GuardianAddrInfo currentAddr = (GuardianAddrInfo) data.getSerializableExtra(SCHOOLFENCE_TO_ADDRESSFENCE);
            Intent intent = new Intent();
            intent.putExtra(GUARDIAN_ADDRESS_DATA, currentAddr);
            setResult(RESULT_OK, intent);
            this.finish();
        }
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        L.d(TAG, "onRegeocodeSearched: " + rCode + ": " + result);
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
//                    currentPosition.setText(poi);
                    currentPosition.setText(regAddr.getFormatAddress());
                }
            }
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.rl_device_current_location: {
                Intent intent = new Intent(this, GuardianFenceActivity.class);
                intent.putExtra(GuardianActivity.IS_GUARDIAN_HOME_ADDRESS, is_home);
                GuardianAddrInfo addr = new GuardianAddrInfo();
                if (searchCenterLatLonPoint != null) {
                    addr.lat = searchCenterLatLonPoint.getLatitude();
                    addr.lng = searchCenterLatLonPoint.getLongitude();
                }
                addr.radius = radius;
                intent.putExtra(SCHOOLADDRESS_TO_SCHOOLFENCE, addr);
                startActivityForResult(intent, ADD_ADDRESS_BY_HAND_FLAG);
            }
            break;
        }
    }
}
