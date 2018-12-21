package com.cqkct.FunKidII.Ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.Bean.UmengEvent;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.AppSettingActivity;
import com.cqkct.FunKidII.Ui.Activity.BabyCardActivity;
import com.cqkct.FunKidII.Ui.Activity.FeedbackActivity;
import com.cqkct.FunKidII.Ui.Activity.FenceListActivity;
import com.cqkct.FunKidII.Ui.Activity.LinkedAccountActivity;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.AlarmClockActivity;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.ClassDisableActivity;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.CollectPraiseActivity;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.ContactsActivity;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.RejectStrangerCallActivity;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.SmsAgentActivity;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.SosContactActivity;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.TimingShutdownWatchActivity;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.WatchSettingActivity;
import com.cqkct.FunKidII.Ui.Activity.UnBindDeviceAndBindActivity;
import com.cqkct.FunKidII.Ui.Adapter.BabyCardClipViewPagerAdapter;
import com.cqkct.FunKidII.Ui.Adapter.MoreActivityAdapter;
import com.cqkct.FunKidII.Ui.Listener.OperateDataListener;
import com.cqkct.FunKidII.Ui.Model.MoreModel;
import com.cqkct.FunKidII.Ui.PullExtend.ExtendRecyclerView;
import com.cqkct.FunKidII.Ui.view.ClipViewPager;
import com.cqkct.FunKidII.Ui.view.ScalePageTransformer;
import com.cqkct.FunKidII.Utils.FileUtils;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.Utils.StringUtils;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Dao.UserEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.db.Entity.UserEntity;
import com.cqkct.FunKidII.service.OkHttpRequestManager;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.NotYetLoginException;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.service.tlc.WaitThirdStageTimeoutException;
import com.cqkct.FunKidII.zxing.capture.CaptureBindNumberActivity;
import com.google.protobuf.GeneratedMessageV3;
import com.umeng.analytics.MobclickAgent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

import protocol.Message;

public class MoreFragment extends BaseFragment {
    public final String TAG = MoreFragment.class.getSimpleName();

    private List<MoreActivityAdapter.DataPrototype> mMoreFunctionsData = new ArrayList<>();
    public Map<String, Long> simplexCallRequstTimeMap = new ConcurrentHashMap<>();
    private boolean hasEditPermission;
    private MoreModel mMoreModel;
    private List<BabyEntity> mBabiesData = new ArrayList<>();
    public static final String MORE_BABY_LIST = "MORE_BABY_LIST";
    public static final String REMOVE_MORE_ICON = "REMOVE_MORE_ICON";
    public static final int MORE_BABY_LIST_FLAG = 1;

    private ExtendRecyclerView mRecyclerView;
    //    private HeaderWrapperAdapter mHeaderWrapperAdapter;
    //    private RecyclerView mBabiesRecyclerView;
    //    private MoreHeadAdapter mBabiesAdapter;
    private View rootView;
    private TextView tvTitle;
    private PopupWindow popupWindow;
    private ImageView ivAdd;
    private RecyclerView moreFunctionRecyclerView;
    private MoreActivityAdapter moreFunctionAdapter;
    private ClipViewPager mClipPager;
    private BabyCardClipViewPagerAdapter mClipAdapter;

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.more, container, false);//关联布局文件
        hasEditPermission = hasEditPermission();
//        setupRecyclerView(rootView);
        initView();
        initData();
        return rootView;
    }

    private void initView() {
        tvTitle = rootView.findViewById(R.id.title_bar_title_text);
        ivAdd = rootView.findViewById(R.id.more_add);
        ivAdd.setOnClickListener(v -> showPopupWindow(mCurrentBabyEntity));

        mMoreModel = new MoreModel(this, getContext());
        moreFunctionRecyclerView = rootView.findViewById(R.id.more_function_recyclerView);
        mBabiesData = mMoreModel.getClipViewPagerData();

        moreFunctionAdapter = new MoreActivityAdapter(mMoreFunctionsData, v -> {
            MoreActivityAdapter.DataPrototype dataPrototype = (MoreActivityAdapter.DataPrototype) v.getTag();
            onItemClick(dataPrototype.imgResId);
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        moreFunctionRecyclerView.setLayoutManager(layoutManager);
        moreFunctionRecyclerView.setAdapter(moreFunctionAdapter);


        mClipPager = rootView.findViewById(R.id.clip_viewpager);
        mClipPager.setPageTransformer(true, new ScalePageTransformer());
        mClipPager.setOffscreenPageLimit(9);
        rootView.findViewById(R.id.page_container).setOnTouchListener((v, event) -> mClipPager.dispatchTouchEvent(event));

        mClipAdapter = new BabyCardClipViewPagerAdapter(getContext(), mBabiesData, pos -> {
            if (false) {
                BabyEntity entity = mBabiesData.get(pos);
                if (!entity.getIs_select()) {
                    GreenUtils.selectBaby(entity.getUserId(), entity.getDeviceId());
                }
            }
        });
        mClipPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.dimen_15));
        mClipPager.setAdapter(mClipAdapter);
        mClipPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (true) {
                    if (state == ViewPager.SCROLL_STATE_IDLE) {
                        BabyEntity entity = mBabiesData.get(mClipPager.getCurrentItem());
                        if (!entity.getIs_select()) {
                            scrollToCurrentBaby = false;
                            GreenUtils.selectBaby(entity.getUserId(), entity.getDeviceId());
                        } else {
                            scrollToCurrentBaby = true;
                        }
                    }
                }
            }
        });

    }

//    private void setupRecyclerView(View rootView) {
//        tvTitle = rootView.findViewById(R.id.title_bar_title_text);
//        ivAdd = rootView.findViewById(R.id.more_add);
//        ivAdd.setOnClickListener(v -> showPopupWindow(mCurrentBabyEntity));
//
//        mMoreModel = new MoreModel(this, getContext());
//        mRecyclerView = rootView.findViewById(R.id.recyclerView);
//        mHeaderWrapperAdapter = new HeaderWrapperAdapter(
//                new MoreActivityAdapter(mMoreFunctionsData, new DebouncedOnClickListener() {
//                    @Override
//                    public void onDebouncedClick(View view) {
//                        MoreActivityAdapter.DataPrototype dataPrototype = (MoreActivityAdapter.DataPrototype) view.getTag();
//                        onItemClick(dataPrototype.imgResId);
//                    }
//                })
//        );
//
//        View headView = getLayoutInflater().inflate(R.layout.babies_header_layout_top_more, mRecyclerView, false);
//        mHeaderWrapperAdapter.addHeaderView(headView);
//        mHeaderWrapperAdapter.notifyDataSetChanged();
//        ExtendListHeaderNew extendListHeader = headView.findViewById(R.id.extend_header);
//        mRecyclerView.setAdapter(mHeaderWrapperAdapter);
//        mRecyclerView.setExtendListHeader(extendListHeader);
//
//        mBabiesRecyclerView = extendListHeader.getRecyclerView();
//        mBabiesData = mMoreModel.getClipViewPagerData();
//        setTitle(mBabiesData);
//        mBabiesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), OrientationHelper.HORIZONTAL, false));
//        mBabiesAdapter = new MoreHeadAdapter(mBabiesData, pos -> {
//            BabyEntity entity = mBabiesData.get(pos);
//            if (entity.getId() == null) {
//                Intent intent = new Intent(getContext(), AllBabiesActivity.class);
//                startActivityForResult(intent, MORE_BABY_LIST_FLAG);
//            } else {
//                GreenUtils.selectBaby(entity.getUserId(), entity.getDeviceId());
//            }
//        }, R.layout.more_baby_list);
//        mBabiesRecyclerView.setAdapter(mBabiesAdapter);
//    }


    private void showPopupWindow(BabyEntity mCurrentBabyEntity) {
        View popupWindowLayout = LayoutInflater.from(getContext()).inflate(R.layout.more_popupwindow, null);
        popupWindowLayout.findViewById(R.id.add_new_device).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CaptureBindNumberActivity.class);
            intent.putExtra("ACTIVITY_MODE", "MOREFRAGMENT");
            intent.putExtra(CaptureBindNumberActivity.PARAM_KEY_MODE, CaptureBindNumberActivity.PARAM_VALUE_MODE_BIND_DEVICE);
            startActivity(intent);
        });

        popupWindowLayout.findViewById(R.id.bind_unbind).setOnClickListener(v -> {
            String babyName = !StringUtils.isEmpty(mCurrentBabyEntity.getName()) ? mCurrentBabyEntity.getName() : getString(R.string.baby);
            ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                    .setMessage(getString(R.string.ask_really_unbind, babyName))
                    .setPositiveButton(getString(R.string.unbind_short), (dialog, which) -> unbindDevice(mDeviceId))
                    .setNegativeButton(getString(R.string.cancel), null);
            dialogFragment.show(getFragmentManager(), "UnbindDeviceDialog");
        });

        if (popupWindow == null) {
            popupWindow = new PopupWindow(popupWindowLayout);
        }
//        popupWindow.setWidth(PublicTools.dip2px(getContext(), 160));
        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setAnimationStyle(R.style.windowAnimation);
        popupWindow.showAsDropDown(ivAdd, 0 , 10);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        notifyBabiesDataSetChanged();
    }

    private boolean scrollToCurrentBaby = true;

    private void notifyBabiesDataSetChanged() {
        BabyEntityDao dao = GreenUtils.getBabyEntityDao();
        List<BabyEntity> list = dao.queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(mUserId))
                .list();
        List<BabyEntity> babies = new ArrayList<>();
        int curPos = mClipPager.getCurrentItem();
        for (int i = 0; i < list.size(); ++i) {
            BabyEntity babyEntity = list.get(i);
            dao.detach(babyEntity);
            babies.add(babyEntity);
            if (babyEntity.getIs_select()) {
                curPos = i;
            }
        }
        int newSize = babies.size();
        if (curPos >= newSize) {
            curPos = newSize - 1;
        }
        if (newSize != mBabiesData.size()) {
            mBabiesData.clear();
            mBabiesData.addAll(babies);
        } else {
            for (int i = 0; i < newSize; ++i) {
                if (!mBabiesData.get(i).equals(babies.get(i))) {
                    mBabiesData.clear();
                    mBabiesData.addAll(babies);
                    break;
                }
            }
        }

        setTitle(mBabiesData);
        mClipAdapter.notifyDataSetChanged();

        if (scrollToCurrentBaby) {
            scrollToCurrentBaby = false;
            if (mClipPager.getCurrentItem() != curPos) {
                mClipPager.setCurrentItem(curPos, true);
            }
        }
    }

    private void initData() {
        mMoreFunctionsData.clear();
        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
        if (deviceInfo == null)
            return;
        long func_module = (int) deviceInfo.getDeviceEntity().getFuncModuleInfo().getFuncModule();
        mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(getContext().getString(R.string.more_common_function)));
        mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_baby_card, R.string.title_baby_card));

        if ((func_module & Message.FuncModule.FM_CONTACTS_VALUE) != 0) {
            mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_contacts, R.string.contact));
        }
        if ((func_module & Message.FuncModule.FM_SOS_VALUE) != 0) {
            mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_sos, R.string.sos_contact_SOS_number));
        }
        if ((func_module & Message.FuncModule.FM_CLASS_DISABLE_VALUE) != 0) {
            mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_disable_time, R.string.class_disable_period));
        }
        if ((func_module & Message.FuncModule.FM_CAMPUS_GUARD_VALUE) != 0 || (func_module & Message.FuncModule.FM_FENCE_VALUE) != 0) {
            mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_fence, R.string.fence_guard));
        }
        if ((func_module & Message.FuncModule.FM_TIMER_POWER_ON_OFF_VALUE) != 0) {
            mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_timing_shutdown, R.string.str_piece_timer_power_on_off));
        }
        if ((func_module & Message.FuncModule.FM_ALARM_CLOCK_VALUE) != 0) {
            mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_alarm_clock, R.string.alarm_clock));
        }
        mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_app_set, R.string.app_setting));
        mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_watch_set, R.string.watch_setting));

        if ((func_module & Message.FuncModule.FM_PRAISE_COLLECTION_VALUE) != 0) {
            mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_collect_praise, R.string.collect_praise));
        }
        mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(getContext().getString(R.string.more_function)));

        mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_reject_stranger, R.string.refuse_stranger_call));
        if ((func_module & Message.FuncModule.FM_SIMPLEX_CALL_VALUE) != 0) {
            mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_single_call, R.string.more_line_call));
        }
        if ((func_module & Message.FuncModule.FM_FIND_DEVICE_VALUE) != 0) {
            mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_find_watch, R.string.find_watch));
        }
        if ((func_module & Message.FuncModule.FM_TAKE_PHOTO_VALUE) != 0) {
            mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_photograph, R.string.take_photo));
        }
         mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_feedback_question, R.string.problems_and_feedback));
         mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_associated_accounts, R.string.other_login_connected_account));

       if ((func_module & Message.FuncModule.FM_SMS_AGENT_VALUE) != 0) {
           mMoreFunctionsData.add(new MoreActivityAdapter.DataPrototype(R.drawable.more_sms_agent, R.string.sms_agent));
       }
//        mHeaderWrapperAdapter.notifyDataSetChanged();
        moreFunctionAdapter.notifyDataSetChanged();
    }

    void setTitle(List<BabyEntity> entities) {
        for (BabyEntity entity : entities) {
            if (entity.getIs_select()) {
                tvTitle.setText(TextUtils.isEmpty(entity.getName()) ? getString(R.string.baby) : entity.getName());
                break;
            }
        }
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void onItemClick(int v) {
        switch (v) {
            case R.drawable.more_baby_card:
                // 宝贝名片
                startActivity(new Intent(getContext(), BabyCardActivity.class));
                break;
            case R.drawable.more_contacts:
                // 联系人
                startActivity(new Intent(getContext(), ContactsActivity.class));
                MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_ENTER_CONTACTS);
                break;
            case R.drawable.more_sos:
                // SOS
                startActivity(new Intent(getContext(), SosContactActivity.class));
                MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_ENTER_SOS);
                break;
            case R.drawable.more_disable_time:
                // 上课禁用
                startActivity(new Intent(getContext(), ClassDisableActivity.class));
                MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_ENTER_CLASS_DISABLE);
                break;
            case R.drawable.more_fence:
                // 围栏守护
                startActivity(new Intent(getContext(), FenceListActivity.class));
                MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_ENTER_FENCE_PAGE);
                break;
            case R.drawable.more_timing_shutdown:
                // 定时开关机
                startActivity(new Intent(getContext(), TimingShutdownWatchActivity.class));
                MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_ENTER_TIMING_SWITCH);
                break;
            case R.drawable.more_alarm_clock:
                // 闹钟
                startActivity(new Intent(getContext(), AlarmClockActivity.class));
                MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_ENTER_ALARM_CLOCK);
                break;
            case R.drawable.more_app_set:
                //APP设置
                startActivity(new Intent(getContext(), AppSettingActivity.class));
                break;
            case R.drawable.more_watch_set:
                // 手表设置
                startActivity(new Intent(getContext(), WatchSettingActivity.class));
                MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_ENTER_WATCH_SETTINGS);
                break;
            case R.drawable.more_reject_stranger:
                // 拒绝陌生人
                startActivity(new Intent(getContext(), RejectStrangerCallActivity.class));
                MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_ENTER_STRANGER_REJECT);
                break;
            case R.drawable.more_single_call:
                // 单向通话
                if (hasEditPermission) {
                    long now = System.currentTimeMillis();
                    Long last = simplexCallRequstTimeMap.get(mDeviceId);
                    if (last == null) {
                        last = 0L;
                    }
                    if (now - last < 1000L * 60) {
                        popInfoDialog(R.string.simplex_call_do_not_frequent);
                    } else {
                        showSingleCall();
                    }
                } else {
                    popInfoDialog(R.string.has_no_permission);
                }
                break;
            case R.drawable.more_find_watch:
                // 查找手表
                if (hasEditPermission) {
                    if (PublicTools.isInClassDisable(GreenUtils.QueryClassDisableEntities(mDeviceId))) {
                        showClassDisabledDialog();
                    } else {
                        MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_FIND_WATCH);
                        mMoreModel.sendFindDevice();
                    }
                } else {
                    popInfoDialog(R.string.has_no_permission);
                }
                break;
            case R.drawable.more_photograph:
                // 拍照
                showTakePicture();
                break;
            case R.drawable.more_collect_praise:
                // 有奖集赞
                startActivity(new Intent(getContext(), CollectPraiseActivity.class));
                MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_ENTER_LIKE_COLLECTIONS);
                break;
            case R.drawable.more_sms_agent:
                // 短信代收
                startActivity(new Intent(getContext(), SmsAgentActivity.class));
                break;
            case R.drawable.more_feedback_question:
                // 问题反馈
                startActivity(new Intent(getContext(), FeedbackActivity.class));
                break;
            case R.drawable.more_associated_accounts:
                // 关联账户
                startActivity(new Intent(getContext(), LinkedAccountActivity.class));
                break;
            case R.drawable.more_unbind:
                // 关联账户
                startActivity(new Intent(getContext(), UnBindDeviceAndBindActivity.class));
                break;
            default:
                break;
        }
    }


    private void unbindDevice(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "unbindDevice: deviceId is empty");
            return;
        }
        List<BabyEntity> babyEntityList = GreenUtils.getBabyEntityDao().queryBuilder().where(BabyEntityDao.Properties.DeviceId.eq(deviceId)).list();
        if (babyEntityList.isEmpty()) {
            L.e(TAG, "not found bind relation on device: " + deviceId);
            return;
        }
        BabyEntity babyEntity = babyEntityList.get(0);
        if (babyEntity.getPermission() == Message.UsrDevAssoc.Permission.OWNER_VALUE) {
            UnbindDeviceDialogFragment.newInstance(deviceId)
                    .setUnbindClickListener(() -> doUnbindDevice(deviceId))
                    .show(getFragmentManager(), "UnbindDeviceDialogFragment");

        } else {
            doUnbindDevice(deviceId);
        }
    }


    private void doUnbindDevice(final String deviceId) {
        final String userId = mUserId;
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "doUnbindDevice failure: userId isEmpty or deviceId isEmpty");
            return;
        }

        popWaitingDialog(R.string.tip_unbinding_device);

        protocol.Message.UnbindDevReqMsg reqMsg = protocol.Message.UnbindDevReqMsg.newBuilder()
                .setUsrDevAssoc(protocol.Message.UsrDevAssoc.newBuilder()
                        .setUserId(userId)
                        .setDeviceId(deviceId)
                        .build())
                .build();

        exec(reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            protocol.Message.UnbindDevRspMsg rspMsg = response.getProtoBufMsg();
                            L.d(TAG, "doUnbindDevice() -> exec() -> onResponse(): " + rspMsg);
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                case NOT_EXISTS:
                                    GreenUtils.clearDeviceWhenUnbind(deviceId, userId, rspMsg.getClearLevel());
                                    popSuccessDialog(R.string.unbind_and_bind_unbind_suc);
                                    return false;
                            }
                        } catch (Exception e) {
                            L.d(TAG, "doUnbindDevice() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.unbind_and_bind_unbind_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.d(TAG, "doUnbindDevice() -> exec() -> onException()", cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.request_timed_out);
                        } else if (cause instanceof NotYetConnectedException || cause instanceof IOException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof NotYetLoginException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof WaitThirdStageTimeoutException) {
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }

    private void showTakePicture() {
        if (hasEditPermission) {
            MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_TAKE_PHOTO);
//                    popWaitingDialog(getString(R.string.taking_photo));
//                    popWaitingDialog(getString(R.string.take_photo_instructions_already_send_pleast_wait), true);
            int secTakePhoto = 60;
            DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
            if (deviceInfo != null) {
                Message.FuncModuleInfo funcModuleInfo = deviceInfo.getDeviceEntity().getFuncModuleInfo();
                Message.FuncTimeoutDef def = funcModuleInfo.getFuncTimeoutDef();
                secTakePhoto = def.getSecOfTakePhoto();
            }
            View view = LayoutInflater.from(getContext()).inflate(R.layout.more_take_picture_wait_dialog, null);
            AlertDialog dialog = createAlertDialog(getContext(), view);
            dialog.setCanceledOnTouchOutside(false);
            TextView tips = view.findViewById(R.id.take_picture_tips);
            CountDownTimer countDownTimer = new CountDownTimer(secTakePhoto * 1000L, 100) {
                @Override public void onTick(long millisUntilFinished) {
                    tips.setText(getString(R.string.take_photo_instructions_already_send_please_wait, String.valueOf(millisUntilFinished / 1000 + 1)));
                }
                @Override public void onFinish() {
                    tips.setText(getString(R.string.take_photo_instructions_already_send_please_wait, String.valueOf(0)));
                }
            }.start();

            ImageView imageView = view.findViewById(R.id.iv_more_take_picture);
            Handler handler = new Handler();
            dialog.show();

            mMoreModel.sendTakePhoto(new OperateDataListener() {
                @Override
                public void operateSuccess(GeneratedMessageV3 messageV3) {
                    Message.TakePhotoS3ReqMsg takePhotoS3ReqMsg = (Message.TakePhotoS3ReqMsg) messageV3;
                    String filename = takePhotoS3ReqMsg.getFilename();
                    if (TextUtils.isEmpty(filename)) {
                        L.w(TAG, "Filename is empty in TakePhotoS3ReqMsg");
                        setDialogTips(handler, dialog, R.string.take_photo_failure);
                        return;
                    }
                    dialog.dismiss();
                    File file = new File(FileUtils.getExternalStoragePhotoDirectoryFile(), filename);
                    downloadPicture(file, mCurrentBabyEntity);
                }

                @Override
                public void operateFailure(Message.ErrorCode errorCode) {
                    switch (errorCode) {
                        case SUCCESS:
                            dialog.dismiss();
                            break;
                        case FAILURE:
                            setDialogTips(handler, dialog, R.string.take_photo_failure);
                            break;
                        case NO_DEVICE:
                            setDialogTips(handler, dialog, R.string.device_does_not_exist);
                            break;
                        case OFFLINE:
                            handler.postDelayed(() -> {
                                countDownTimer.cancel();
                                tips.setText(R.string.tip_device_not_online);
                                imageView.setImageResource(R.drawable.take_picture_failure);
                            }, 2000);
                            handler.postDelayed(dialog::dismiss, 3000);

                            break;
                        case INVALID_PARAM:
                            setDialogTips(handler, dialog, R.string.invalid_parameter);
                            break;
                        case TIMEOUT:
                            setDialogTips(handler, dialog, R.string.network_quality_poor);
                            break;
                        default:
                            setDialogTips(handler, dialog, R.string.take_photo_failure);
                            break;

                    }
                }
            });
        } else {
            popInfoDialog(R.string.has_no_permission);
        }
    }

    private void setDialogTips(Handler handler, Dialog dialog, int stringIds) {
        handler.postDelayed(dialog::dismiss, 2000);
        handler.postDelayed(() -> popErrorDialog(stringIds), 2500);
    }

    private void showClassDisabledDialog() {
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment();
        dialogFragment.setTitle(getString(R.string.class_disable))
                .setMessage(getString(R.string.more_class_disable_find_device_sure_setting))
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.ok), (dialog1, which) -> {
                    startActivity(new Intent(getContext(), ClassDisableActivity.class));
                });
        dialogFragment.show(getFragmentManager(), "showClassDisabledDialog");
    }

    private void showSingleCall() {
        if (PublicTools.isInClassDisable(GreenUtils.QueryClassDisableEntities(mDeviceId))) {
            showClassDisabledDialog();
        } else {
            MobclickAgent.onEvent(getContext(), UmengEvent.TIMES_OF_ONE_WAY_CALL);
            if (TextUtils.isEmpty(mUserId) || TextUtils.isEmpty(mDeviceId)) {
                L.e(TAG, "showSingleCall  mUserId || mDeviceId  is null");
                return;
            }
            popWaitingDialog(R.string.please_wait);
            List<UserEntity> userEntityList = GreenUtils.getUserEntityDao().queryBuilder().where(UserEntityDao.Properties.UserId.eq(mUserId)).list();
            mMoreModel.getUserPhoneToListen(mUserId, mDeviceId, new OperateDataListener() {
                @Override
                public void operateSuccess(GeneratedMessageV3 messageV3) {
                    Message.FetchUserInfoRspMsg rspMsg = (Message.FetchUserInfoRspMsg) messageV3;
                    dismissDialog();
                    mMoreModel.listenDevice(mUserId, mDeviceId, rspMsg.getUserInfo().getPhone());
                    GreenUtils.saveUserInfo(rspMsg.getUserInfo());
                }

                @Override
                public void operateFailure(Message.ErrorCode errorCode) {
                    switch (errorCode) {
                        case FAILURE:
                            dismissDialog();
                            mMoreModel.listenDevice(mUserId, mDeviceId, null);
                            break;
                        case INVALID_PARAM:
                            dismissDialog();
                            mMoreModel.listenDevice(mUserId, mDeviceId, userEntityList.get(0).getPhone());
                            break;
                    }
                }
            }, userEntityList);
        }
    }

    void downloadPicture(final File file, BabyEntity mCurrentBabyBean) {
        if (mCurrentBabyBean == null || file.exists()) {
            return;
        }
        OkHttpRequestManager.getInstance(getContext()).downloadDeviceHeadIcon(file, mCurrentBabyBean.getUserId(), mCurrentBabyBean.getDeviceId(),
                new OkHttpRequestManager.ReqProgressCallBack() {

                    @Override
                    public void onProgress(long total, long current) {
                    }

                    @Override
                    public void onReqSuccess(Object result) {
                        try {
                            if (file != null) {
                                mMoreModel.showPhotoDialog(file, getContext());
                                dismissDialog();
                                return;
                            } else {
                                L.e(TAG, "onReqSuccess in downloadPicture: file maybe not picture");
                            }
                        } catch (Exception e) {
                            L.e(TAG, "onReqSuccess in downloadPicture", e);
                        }
                        popErrorDialog(R.string.take_photo_failure);
                    }

                    @Override
                    public void onReqFailed(String errorMsg) {
                        L.e(TAG, "downloadPicture onReqFailed");
                        popErrorDialog(R.string.take_photo_failure);
                    }
                });
    }


    @Override
    public void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo) {
        if (deviceInfo.getDeviceId().equals(mDeviceId)) {
            initData();
            moreFunctionAdapter.notifyDataSetChanged();
        }
        notifyBabiesDataSetChanged();
    }

    @Override
    public void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg) {
        notifyBabiesDataSetChanged();
    }

    @Override
    public void onDeviceUnbind(TlcService tlcService, @android.support.annotation.Nullable Pkt reqPkt, @NonNull Message.NotifyUserUnbindDevReqMsg reqMsg) {
        notifyBabiesDataSetChanged();
    }

    @Override
    public void onCurrentBabyChanged(@android.support.annotation.Nullable BabyEntity oldBabyBean, @android.support.annotation.Nullable BabyEntity newBabyBean, boolean isSticky) {
        super.onCurrentBabyChanged(oldBabyBean, newBabyBean, isSticky);
        initData();
        notifyBabiesDataSetChanged();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onDeviceOnline(Event.DeviceOnline ev) {
        for (BabyEntity babyEntity : mBabiesData) {
            if (ev.getDeviceId().equals(babyEntity.getDeviceId())) {
                notifyBabiesDataSetChanged();
                break;
            }
        }
    }
}

