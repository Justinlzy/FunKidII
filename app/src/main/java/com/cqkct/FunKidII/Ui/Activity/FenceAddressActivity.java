package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.amap.api.maps.AMap;
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
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Entity.FenceEntity;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;

import java.util.ArrayList;
import java.util.List;

import protocol.Message;


public class FenceAddressActivity extends BaseActivity implements GeocodeSearch.OnGeocodeSearchListener, PoiSearch.OnPoiSearchListener {

    public static final String TAG = FenceAddressActivity.class.getSimpleName();
    public static final int ACTIVITY_REQUEST_CODE_AAD_FENCE = 1;
    public static final String FENCE_ADDRESS_GUARDIANADDRINFO = "FENCE_ADDRESS_GUARDIANADDRINFO";
    public static final String ADD_MODE = "ADD_MODE";

    private FenceEntity fenceEntity;
    private String searchKey;
    private LocateRecyclerAdapter addressAdapter;
    private List<GuardianAddrInfo> addrList = new ArrayList<>();
    private EditText searchEt;
    private TextView currentAddressTv;
    private GeocodeSearch mGeocodeSearch;
    private LatLonPoint searchCenterLatLonPoint;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fence_address);
        setTitleBarTitle(R.string.search_address);
        Intent fenceIntent = getIntent();
        if (fenceIntent == null) {
            L.e(TAG, "onCreate getIntent() return null");
            finish();
            return;
        }
        fenceEntity = (FenceEntity) fenceIntent.getSerializableExtra(FenceEditActivity.INTENT_PRARM_fenceEntity);
        if (fenceEntity == null) {
            L.e(TAG, "onCreate getIntent() INTENT_PRARM_fenceEntity Extra is null");
            finish();
            return;
        }
        mGeocodeSearch = new GeocodeSearch(this);
        mGeocodeSearch.setOnGeocodeSearchListener(this);

        initView();
        initData();
    }

    private void initData() {
        if (searchCenterLatLonPoint == null) {
            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
            if (deviceInfo != null) {
                Message.Position position = deviceInfo.getLastPosition();
                if (position != null) {
                    Message.LatLon latLon = deviceInfo.getLastPosition().getLatLng();
                    if (latLon != null && latLon.getLatitude() != 0 && latLon.getLongitude() != 0) {
                        searchCenterLatLonPoint = new LatLonPoint(latLon.getLatitude(), latLon.getLongitude());
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

    protected void doSearchPoi(@NonNull String key, @NonNull LatLonPoint latLonPoint) {
        addrList.clear();
        addressAdapter.notifyDataSetChanged();
        PoiSearch.Query query = new PoiSearch.Query(key, "", "");
        query.setPageSize(30);
        PoiSearch search = new PoiSearch(this, query);
        //50 公里 内
        search.setBound(new PoiSearch.SearchBound(latLonPoint, 500000));
        search.setOnPoiSearchListener(this);
        search.searchPOIAsyn();
    }


    private void getAddressByLatlng(double latitude, double longitude) {
        //逆地理编码查询条件：逆地理编码查询的地理坐标点、查询范围、坐标类型。
        LatLonPoint latLonPoint = new LatLonPoint(latitude, longitude);
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 500f, GeocodeSearch.AMAP);
        //异步查询
        mGeocodeSearch.getFromLocationAsyn(query);
    }


    private void initView() {
        RecyclerView recyclerView = findViewById(R.id.nearby_address_recycle);
        addressAdapter = new LocateRecyclerAdapter(this, addrList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(addressAdapter);
        addressAdapter.setLocationItemClick((parent, view, position, addr) -> {
            Intent fenceIntent = new Intent(FenceAddressActivity.this, FenceActivity.class);
            fenceIntent.putExtra(FENCE_ADDRESS_GUARDIANADDRINFO, addr);
            fenceIntent.putExtra(ADD_MODE, TextUtils.isEmpty(fenceEntity.getFenceId()));
            startActivityForResult(fenceIntent, ACTIVITY_REQUEST_CODE_AAD_FENCE);
        });

        searchEt = findViewById(R.id.fence_address_name);
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //启动定位
                searchKey = searchEt.getText().toString();
                searchPoi(searchKey);
            }
        });
        currentAddressTv = findViewById(R.id.watch_current_address);
        Message.Position lastPosition = DeviceInfo.getLastPosition(mDeviceId);
        if (lastPosition != null) {
            double latitude = lastPosition.getLatLng().getLatitude();
            double longitude = lastPosition.getLatLng().getLongitude();
            getAddressByLatlng(latitude, longitude);
        }
        currentAddressTv.setText(TextUtils.isEmpty(fenceEntity.getFenceAddress()) ? "" : fenceEntity.getFenceAddress());
    }

    private void searchPoi(@NonNull String key) {
        if (searchCenterLatLonPoint == null) {
            initLocate();
        } else {
            doSearchPoi(key, searchCenterLatLonPoint);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == ACTIVITY_REQUEST_CODE_AAD_FENCE && resultCode == RESULT_OK)) {
            GuardianAddrInfo currentAddr = (GuardianAddrInfo) data.getSerializableExtra(FenceActivity.SELECT_FENCE_ADDRESS);
            Intent intent = new Intent();
            intent.putExtra(FenceActivity.SELECT_FENCE_ADDRESS, currentAddr);
            setResult(RESULT_OK, intent);
            this.finish();
        }
    }

    private void initLocate() {
        // TODO:
        L.v(TAG, "initLocate TODO");
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.iv_fence_address_next:
                break;
            case R.id.watch_current_location: {
                GuardianAddrInfo addr = new GuardianAddrInfo();
                Message.Fence.Shape.Round round = fenceEntity.getFence().getShape().getRound();
                addr.lat = round.getLatlon().getLatitude();
                addr.lng = round.getLatlon().getLongitude();
                addr.radius = round.getRadius();
                addr.name = fenceEntity.getName();
                addr.address = fenceEntity.getFenceAddress();
                Intent fenceIntent = new Intent(FenceAddressActivity.this, FenceActivity.class);
                fenceIntent.putExtra(FENCE_ADDRESS_GUARDIANADDRINFO, addr);
                fenceIntent.putExtra(ADD_MODE, TextUtils.isEmpty(fenceEntity.getFenceId()));
                startActivityForResult(fenceIntent, ACTIVITY_REQUEST_CODE_AAD_FENCE);
            }
            break;
        }
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        L.d(TAG, "onRegeocodeSearched: " + i + ": " + regeocodeResult);
        String poi = null;
        if (i == 1000) {
            if (regeocodeResult != null) {
                RegeocodeAddress regAddr = regeocodeResult.getRegeocodeAddress();
                if (regAddr != null) {
                    if (regAddr.getPois() != null && !regAddr.getPois().isEmpty()) {
                        poi = regAddr.getPois().get(0).getTitle();
                    } else {
                        poi = regAddr.getFormatAddress();
                    }
//                    currentPosition.setText(poi);
                    currentAddressTv.setText(regAddr.getFormatAddress());
                }
            }
        }

    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

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
            addrList.add(addr);
            L.d(TAG, "schoolName" + schoolName + "schoolAddress: " + schoolAddress);
        }
        addressAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

}
