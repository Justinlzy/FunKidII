package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.Bean.UmengEvent;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.GuardianActivity;
import com.cqkct.FunKidII.Ui.Adapter.FenceListAdapter;
import com.cqkct.FunKidII.Ui.Listener.OperateDataListener;
import com.cqkct.FunKidII.Ui.Model.FenceListMode;
import com.cqkct.FunKidII.Ui.Model.GuardianModel;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.FenceEntityDao;
import com.cqkct.FunKidII.db.Entity.FenceEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.google.protobuf.GeneratedMessageV3;
import com.umeng.analytics.MobclickAgent;
import com.yanzhenjie.recyclerview.swipe.SwipeMenu;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItem;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItemClickListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import protocol.Message;


/**
 * Created by Kct on 2017/8/12.
 */
public class FenceListActivity extends BaseMapActivity implements GeocodeSearch.OnGeocodeSearchListener, AMap.OnMyLocationChangeListener, AMap.OnMapClickListener {
    private static final String TAG = FenceListActivity.class.getSimpleName();

    public static final int ACTIVITY_REQUEST_CODE_EDIT_FENCE = 1;
    public static final int ACTIVITY_REQUEST_CODE_EDIT_GUARDIAN_FENCE = 2;
    public static final String GUARDIAN_DATA = "GUARDIAN_DATA";

    private FenceListAdapter listAdapter;
    private FenceListMode fenceListMode;
    private GuardianModel guardianModel;
    private GeocodeSearch geocoderSearch;  //地址反查
    private Map<Object, Integer> regeocodeQueries;
    private DeviceInfo deviceInfo;


    private Message.SchoolGuard schoolGuard;

    List<FenceListAdapter.FenceDataType> mFenceData = new ArrayList<>();
    boolean hasEditPermission = false;
    //围栏个数限制
    private int limit = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fence_setting);
        setTitleBarTitle(R.string.fence_title);
        if (mDeviceId == null) {
            L.w(TAG, "mDeviceId is null");
            finish();
            return;
        }

        hasEditPermission = hasEditPermission();
        fenceListMode = new FenceListMode(this);
        guardianModel = new GuardianModel(this);
        deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
        initView();
        initData();
    }


    private void initData() {
        limit = fenceListMode.loadLimit(mDeviceId);
        if (!((deviceInfo.getDeviceEntity().getFuncModuleInfo().getFuncModule() & Message.FuncModule.FM_FENCE_VALUE) == 0)) {
            L.e(TAG, "Message.FuncModule.FM_FENCE_VALUE have it");
            getFenceInfoInDb();
            popWaitingDialog(R.string.tip_get_fencesing);
            fenceListMode.getFenceInfo(mDeviceId, new OperateDataListener() {
                @Override
                public void operateSuccess(GeneratedMessageV3 messageV3) {
                    Message.GetFenceRspMsg rspMsg = (Message.GetFenceRspMsg) messageV3;
                    limit = rspMsg.getCountLimit();
                    GreenUtils.saveFence(mDeviceId, rspMsg.getFenceList());
                    getFenceInfoInDb();
                    dismissDialog();
                }

                @Override
                public void operateFailure(Message.ErrorCode errorCode) {
                    switch (errorCode) {
                        case TIMEOUT:
                            popErrorDialog(R.string.tip_get_fences_timieout);
                            break;
                        case FAILURE:
                            popErrorDialog(R.string.tip_get_fences_failure);
                            break;
                    }
                }
            });
        }


        if (!((deviceInfo.getDeviceEntity().getFuncModuleInfo().getFuncModule() & Message.FuncModule.FM_CAMPUS_GUARD_VALUE) == 0)) {
            L.e(TAG, "Message.FuncModule.FM_CAMPUS_GUARD_VALUE have it");
            schoolGuard = guardianModel.loadData(mDeviceId);
            if (mFenceData.isEmpty()) {
                mFenceData.add(0, new FenceListAdapter.FenceDataType(schoolGuard));
            } else {
                mFenceData.set(0, new FenceListAdapter.FenceDataType(schoolGuard));
            }
            listAdapter.notifyDataSetChanged();

            //form server
            popWaitingDialog(R.string.loading);
            guardianModel.getSchoolGuardianData(mDeviceId, new OperateDataListener() {
                @Override
                public void operateSuccess(GeneratedMessageV3 messageV3) {
                    if (mFenceData.isEmpty()) {
                        mFenceData.add(0, new FenceListAdapter.FenceDataType(schoolGuard));
                    } else {
                        mFenceData.set(0, new FenceListAdapter.FenceDataType(schoolGuard));
                    }
                    listAdapter.notifyDataSetChanged();
                    dismissDialog();
                }

                @Override
                public void operateFailure(Message.ErrorCode errorCode) {
                    switch (errorCode) {
                        case FAILURE:
                            popErrorDialog(R.string.load_failure);
                            break;
                        case TIMEOUT:
                            popErrorDialog(R.string.load_timeout);
                            break;
                    }
                }
            });
        }
    }

    private void getFenceInfoInDb() {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "getFenceInfoInDb: deviceId is empty");
            return;
        }

        FenceEntityDao dao = GreenUtils.getFenceEntityDao();
        List<FenceEntity> list = dao.queryBuilder()
                .where(FenceEntityDao.Properties.DeviceId.eq(deviceId))
                .list();
        if (!list.isEmpty()) {
            if (mFenceData.isEmpty()) {
                for (FenceEntity entity : list) {
                    mFenceData.add(new FenceListAdapter.FenceDataType(entity));
                }
                listAdapter.notifyDataSetChanged();
            } else {
                List<FenceListAdapter.FenceDataType> tempAddList = new ArrayList();
                tempAddList.clear();
                if (mFenceData.get(0).schoolGuard == null) {
                    for (FenceEntity entity : list) {
                        tempAddList.add(new FenceListAdapter.FenceDataType(entity));
                    }
                } else {
                    tempAddList.add(mFenceData.get(0));
                    for (FenceEntity entity : list) {
                        tempAddList.add(new FenceListAdapter.FenceDataType(entity));
                    }
                }
                mFenceData.clear();
                mFenceData.addAll(tempAddList);
            }
        }
        regeocodeQueries = new HashMap<>();
        for (int i = 0; i < mFenceData.size(); i++) {
            if (mFenceData.get(i).schoolGuard != null) {
                continue;
            }
            FenceEntity entity = mFenceData.get(i).entity;
            Message.LatLon latLon = entity.getFence().getShape().getRound().getLatlon();
            if (latLon != null) {
                regeocodeQueries.put(getAddress(latLon), i);
            }
        }

    }

    public void initView() {
        //添加围栏信息
        listAdapter = new FenceListAdapter(mFenceData, this, hasEditPermission, getMapType());
        SwipeMenuRecyclerView recyclerView = findViewById(R.id.list_fence);
        //        fenceList.setEmptyView(findViewById(android.R.id.empty));
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setSwipeMenuCreator((swipeLeftMenu, swipeRightMenu, viewType) -> {
            if (viewType == FenceListAdapter.TYPE_ITEM_SCHOOL_GUARD)
                return;
            int width = getResources().getDimensionPixelSize(R.dimen.dp_70);
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            SwipeMenuItem deleteItem = new SwipeMenuItem(this)
                    .setImage(R.drawable.delete_left_slip)
                    .setWidth(width)
                    .setHeight(height);
            swipeRightMenu.addMenuItem(deleteItem);// 添加菜单到右侧。
        });
        recyclerView.setSwipeMenuItemClickListener(mMenuItemClickListener);
        recyclerView.setAdapter(listAdapter);
        listAdapter.setMapOnClickListener(new FenceListAdapter.OnItemOnClickListener() {
            @Override
            public void onFenceItemClick(int position) {
                Intent fenceInfo = new Intent(FenceListActivity.this, FenceEditActivity.class);  //进入围栏详情页面
                fenceInfo.putExtra(FenceEditActivity.INTENT_PRARM_fenceList, (Serializable) mFenceData);
                fenceInfo.putExtra(FenceEditActivity.INTENT_PRARM_fenceEntity, mFenceData.get(position).entity);
                startActivityForResult(fenceInfo, ACTIVITY_REQUEST_CODE_EDIT_FENCE);
            }

            @Override
            public void onGuardianItemClick() {
                Intent intent = new Intent(FenceListActivity.this, GuardianActivity.class);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_EDIT_GUARDIAN_FENCE);
                MobclickAgent.onEvent(FenceListActivity.this, UmengEvent.TIMES_OF_ENTER_CAMPUS_GUARD);
            }

            @Override
            public void onGuardianAddClick() {
                Intent intent = new Intent(FenceListActivity.this, GuardianActivity.class);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_EDIT_GUARDIAN_FENCE);
                MobclickAgent.onEvent(FenceListActivity.this, UmengEvent.TIMES_OF_ENTER_CAMPUS_GUARD);
            }

            @Override
            public void onCompoundButtonClick(int position, CompoundButton cb) {
                if (cb.getId() == R.id.ib_switch) {
                    Message.SchoolGuard schoolGuard = mFenceData.get(position).schoolGuard;
                    if (schoolGuard != null) {

                        Message.ModifySchoolGuardReqMsg reqMsg = Message.ModifySchoolGuardReqMsg.newBuilder()
                                .setDeviceId(mDeviceId)
                                .setGuard(schoolGuard.toBuilder().
                                        setEnable(!schoolGuard.getEnable())
                                        .build())
                                .build();
                        popWaitingDialog(R.string.please_wait);
                        if (!guardianModel.modifyGuardianSchool(reqMsg, mDeviceId, new OperateDataListener() {
                            @Override
                            public void operateSuccess(GeneratedMessageV3 messageV3) {
                                dismissDialog();
                                Message.SchoolGuard.Builder builder = mFenceData.get(position).schoolGuard.toBuilder();
                                builder.setEnable(!mFenceData.get(position).schoolGuard.getEnable());
                                mFenceData.set(position, new FenceListAdapter.FenceDataType(builder.build()));
                                listAdapter.notifyItemChanged(position);
                            }

                            @Override
                            public void operateFailure(Message.ErrorCode errorCode) {
                                switch (errorCode) {
                                    case FAILURE:
                                        popErrorDialog(R.string.school_guardian_error);
                                        L.e(TAG, "onResponse FAILURE");
                                        break;
                                    case NO_DEVICE:
                                        popErrorDialog(R.string.school_guardian_error);
                                        L.e(TAG, "onResponse NODEVICE");
                                        break;
                                    case OFFLINE:
                                        popErrorDialog(R.string.school_guardian_error);
                                        L.e(TAG, "onResponse OFFLINE");
                                        break;
                                    case INVALID_PARAM:
                                        popErrorDialog(R.string.school_guardian_error);
                                        L.e(TAG, "onResponse INVALID_PARAM");
                                        break;
                                    case TIMEOUT:
                                        popErrorDialog(R.string.school_guide_setting_time_out);
                                        L.e(TAG, "onResponse INVALID_PARAM");
                                        break;
                                }
                            }
                        })) ;
                    }
                }
            }
        });

        if (hasEditPermission) {
            findViewById(R.id.title_bar_right_icon).setVisibility(View.VISIBLE); // 显示添加按钮
        }
    }


    @Override
    public void onMapClick(LatLng latLng) {
        if (hasEditPermission) {
            Intent intent = new Intent(this, GuardianActivity.class);
            startActivityForResult(intent, ACTIVITY_REQUEST_CODE_EDIT_GUARDIAN_FENCE);
            MobclickAgent.onEvent(FenceListActivity.this, UmengEvent.TIMES_OF_ENTER_CAMPUS_GUARD);
        }
    }


    /**
     * RecyclerView的Item的Menu点击监听。
     */
    private SwipeMenuItemClickListener mMenuItemClickListener = menuBridge -> {
        menuBridge.closeMenu();

        int direction = menuBridge.getDirection(); // 左侧还是右侧菜单。
        int adapterPosition = menuBridge.getAdapterPosition(); // RecyclerView的Item的position。
        int menuPosition = menuBridge.getPosition(); // 菜单在RecyclerView的Item中的Position。

        if (direction == SwipeMenuRecyclerView.RIGHT_DIRECTION) {
            if (adapterPosition >= 0 && adapterPosition < mFenceData.size())
                popWaitingDialog(R.string.tip_del_fencesing);
            fenceListMode.deleteFence(mDeviceId, mFenceData.get(adapterPosition).entity, new OperateDataListener() {
                @Override
                public void operateSuccess(GeneratedMessageV3 messageV3) {
                    dismissDialog();
                    mFenceData.remove(adapterPosition);
                    listAdapter.notifyDataSetChanged();
                }

                @Override
                public void operateFailure(Message.ErrorCode errorCode) {
                    switch (errorCode) {
                        case TIMEOUT:
                            popErrorDialog(R.string.fence_setting_delete_timeout);
                            break;
                        case FAILURE:
                            popErrorDialog(R.string.fence_setting_delete_fail);
                            break;
                    }
                }
            });

        }
    };


    @Override
    public void onTitleBarClick(View view) {
        super.onTitleBarClick(view);
        super.onDebouncedClick(view);
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.title_bar_right_icon: {
                if (mFenceData.size() >= limit) {
                    toast(R.string.number_of_fence_out_of_limit);
                    return;
                }
                Intent intent = new Intent(this, FenceEditActivity.class);
                intent.putExtra(FenceEditActivity.INTENT_PRARM_fenceList, (Serializable) mFenceData);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_EDIT_FENCE);
                break;
            }
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        if (requestCode == ACTIVITY_REQUEST_CODE_EDIT_FENCE) {
            if (data != null) {
                FenceEntity entity = (FenceEntity) data.getSerializableExtra(FenceEditActivity.INTENT_PRARM_fenceEntity);
                if (entity != null) {
                    int position = -1;
                    if (entity.getFenceId() != null) {

                        for (int i = 0; i < mFenceData.size(); i++) {
                            FenceListAdapter.FenceDataType dataType = mFenceData.get(i);
                            if (dataType.schoolGuard != null)
                                continue;
                            if (dataType.entity.getFenceId().equals(entity.getFenceId())) {
                                position = i;
                                break;
                            }

                        }
                    }
                    if (position > 0) {
                        mFenceData.set(position, new FenceListAdapter.FenceDataType(entity));
                    } else {
                        mFenceData.add(new FenceListAdapter.FenceDataType(entity));
                    }
                    listAdapter.notifyDataSetChanged();
                }
            }
        }
        if (requestCode == ACTIVITY_REQUEST_CODE_EDIT_GUARDIAN_FENCE) {
            if (data != null) {
                Message.SchoolGuard schoolGuard = (Message.SchoolGuard) data.getSerializableExtra(FenceListActivity.GUARDIAN_DATA);
                if (schoolGuard != null) {
                    if (mFenceData.isEmpty()) {
                        mFenceData.add(0, new FenceListAdapter.FenceDataType(schoolGuard));
                    } else {
                        mFenceData.set(0, new FenceListAdapter.FenceDataType(schoolGuard));
                    }
                    listAdapter.notifyDataSetChanged();
                }
            }
        }
    }


    @Override
    public void onDevFenceChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull protocol.Message.NotifyFenceChangedReqMsg reqMsg) {
        if (!reqMsg.getDeviceId().equals(mDeviceId)) {
            return;
        }
        getFenceInfoInDb();
    }

    @Override
    public void onDeviceUnbind(TlcService tlcService, @Nullable Pkt reqPkt, @NonNull protocol.Message.NotifyUserUnbindDevReqMsg reqMsg) {
        if (reqMsg.getUsrDevAssoc().getUserId().equals(mUserId) && reqMsg.getUsrDevAssoc().getDeviceId().equals(mDeviceId)) {
            L.w(TAG, "current device unbound!!! Activity finish");
            finish();
        }
    }


    //获取坐标对应的地理信息
    private RegeocodeQuery getAddress(Message.LatLon latLon) {
        LatLonPoint point = new LatLonPoint(latLon.getLatitude(), latLon.getLongitude());
        L.i("获取坐标对应的地理信息: " + point);
        if (geocoderSearch == null) {
            geocoderSearch = new GeocodeSearch(this);
            geocoderSearch.setOnGeocodeSearchListener(this);
        }
        RegeocodeQuery query = new RegeocodeQuery(point, 200, GeocodeSearch.AMAP);// 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        geocoderSearch.getFromLocationAsyn(query);// 设置同步逆地理编码请求

        return query;
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        L.v(TAG, "onRegeocodeSearched  rCode= " + rCode + " result: " + result);
        if (rCode != 1000 || result == null) {
            return;
        }
        String currentDeviceAddress = getString(R.string.unknown);
        RegeocodeAddress regAddr = result.getRegeocodeAddress();
        if (regAddr != null) {
            currentDeviceAddress = regAddr.getFormatAddress();
        }

        Iterator<Map.Entry<Object, Integer>> it = regeocodeQueries.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<Object, Integer> entry = it.next();
            if (entry.getKey() == result.getRegeocodeQuery()) {
                int position = regeocodeQueries.get(entry.getKey());
                mFenceData.get(position).entity.setFenceAddress(currentDeviceAddress);
            }
        }

        listAdapter.notifyDataSetChanged(); // FIXME:  after map dismiss

    }


    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        if (listAdapter != null) {
            listAdapter.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onMyLocationChange(Location location) {

    }

}
