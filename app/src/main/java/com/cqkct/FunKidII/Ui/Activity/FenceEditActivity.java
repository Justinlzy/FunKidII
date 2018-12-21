package com.cqkct.FunKidII.Ui.Activity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.cqkct.FunKidII.Bean.GuardianAddrInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.SetClassDisableRepeatActivity;
import com.cqkct.FunKidII.Ui.Adapter.FenceListAdapter;
import com.cqkct.FunKidII.Ui.Listener.OperateDataListener;
import com.cqkct.FunKidII.Ui.Model.FenceEditModel;
import com.cqkct.FunKidII.Ui.view.NumberPickerBlue;
import com.cqkct.FunKidII.Ui.view.NumberPickerRed;
import com.cqkct.FunKidII.Utils.Constants;
import com.cqkct.FunKidII.Utils.DateUtil;
import com.cqkct.FunKidII.Utils.EmojiInputFilter;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.Utils.Utils;
import com.cqkct.FunKidII.db.Entity.FenceEntity;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.protobuf.GeneratedMessageV3;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import protocol.Message;


/**
 * Created by Administrator on 2018/1/22.
 */

public class FenceEditActivity extends BaseMapActivity {

    public static final String TAG = FenceEditActivity.class.getSimpleName();

    public static final String INTENT_PRARM_fenceList = "INTENT_PRARM_fenceList";
    public static final String INTENT_PRARM_fenceEntity = "INTENT_PRARM_fenceEntity";
    public static final String WEEK_FLAG = "weekFlag";

    public static final int ACTIVITY_REQUEST_CODE_SET_REPEAT_DATA = 1;
    public static final int ACTIVITY_REQUEST_CODE_AAD_FENCE = 2;
    public static final int START_GOOGLE_MAP_PLACE_PICKER_FENCE_ADDRESS = 3;
    public static final int START_GOOGLE_MAP_PLACE_PICKER_FENCE_SELECT_ADDRESS = 4;

    public static final int FENCE_DEFAULT_ICON_TYPE = 0;
    public static final int FENCE_HOME_ICON_TYPE = 1;
    public static final int FENCE_SCHOOL_ICON_TYPE = 2;

    InputFilter emojiFilter = new EmojiInputFilter();

    private SwitchCompat inSwitchCompat, outSwitchCompat;
    private int startTimeHour = 8, startTimeMin = 0;
    private int endTimeHour = 12, endTimeMin = 0;
    private long startTime = 0L, endTime = 0L;
    private int fenceType = 0, repeat = 0, maxLength;
    private EditText edName;
    private String fenceName;
    private TextView fenceImage;
    private TextView tv_fenceRepeat;
    private List<FenceListAdapter.FenceDataType> fenceList;
    private Message.Fence.Shape shape;

    private Message.Fence.Builder mFenceBuilder;

    private FenceEntity mFenceEntity;
    boolean hasEditPermission;

    private FenceEditModel model;
    private TextView fence_address_name;
    private ImageView iv_fence_address_next;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_fence);

        Intent intent = this.getIntent();
        fenceList = (List<FenceListAdapter.FenceDataType>) intent.getSerializableExtra(INTENT_PRARM_fenceList);
        mFenceEntity = (FenceEntity) intent.getSerializableExtra(FenceEditActivity.INTENT_PRARM_fenceEntity);
        setTitleBarTitle(isAddNew() ? getString(R.string.fence_adding_fence) : getString(R.string.fence_setting_edit_fence));
        mFenceBuilder = isAddNew() ? Message.Fence.newBuilder() : mFenceEntity.getFence().toBuilder();

        hasEditPermission = hasEditPermission();

        initView();
    }

    private boolean isAddNew() {
        return mFenceEntity == null || mFenceEntity.getId() == null;
    }

    private void initView() {
        model = new FenceEditModel(this);
        maxLength = getResources().getInteger(R.integer.maxLength_of_fence_name);
        edName = findViewById(R.id.ed_fence_name);
        fence_address_name = findViewById(R.id.fence_address_name);
        iv_fence_address_next = findViewById(R.id.iv_fence_address_next);
        String name = edName.getText().toString();
        fenceName = name;
        edName.setText(name);
        edName.setSelection(fenceName.length());
        edName.setFilters(new InputFilter[]{emojiFilter});

        edName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                int editStart;
                int editEnd;
                editStart = edName.getSelectionStart();
                editEnd = edName.getSelectionEnd();

                // 先去掉监听器，否则会出现栈溢出
                edName.removeTextChangedListener(this);

                // 注意这里只能每次都对整个EditText的内容求长度，不能对删除的单个字符求长度
                // 因为是中英文混合，单个字符而言，calculateLength函数都会返回1
                while (Utils.charSequenceLength_zhCN(s.toString()) > maxLength) { // 当输入字符个数超过限制的大小时，进行截断操作
                    s.delete(editStart - 1, editEnd);
                    editStart--;
                    editEnd--;
                }
                edName.setSelection(editStart);

                // 恢复监听器
                edName.addTextChangedListener(this);
                fenceName = edName.getText().toString();
            }
        });

        inSwitchCompat = findViewById(R.id.ib_switch_in_fence);
        outSwitchCompat = findViewById(R.id.ib_switch_out_fence);
        tv_fenceRepeat = findViewById(R.id.repeat_text);
        fenceImage = findViewById(R.id.fence_icon);
        fenceImage.setText(R.string.bind_default);


        if (hasEditPermission) {
            inSwitchCompat.setClickable(true);
            outSwitchCompat.setClickable(true);
            findViewById(R.id.ed_fence_name).setClickable(true);
            findViewById(R.id.bt_save_fence).setClickable(true);
            findViewById(R.id.iv_fence_repeat_next).setVisibility(View.VISIBLE);
            findViewById(R.id.iv_fence_select_icon_next).setVisibility(View.VISIBLE);
        } else {
            inSwitchCompat.setClickable(false);
            outSwitchCompat.setClickable(false);
            findViewById(R.id.ed_fence_name).setClickable(false);

            findViewById(R.id.repeat_time).setClickable(false);
            findViewById(R.id.select_fence_icon).setClickable(false);
        }
        initDate();
        NumberPickerBlue startHourNP = findViewById(R.id.start_time_hour);
        NumberPickerBlue startMinNP = findViewById(R.id.start_time_min);
        initStartTimeNp(startHourNP, startMinNP);

        NumberPickerRed endHourNP = findViewById(R.id.end_time_hour);
        NumberPickerRed endMinNP = findViewById(R.id.end_time_min);
        initEndTimeNp(endHourNP, endMinNP);
    }


    private void initDate() {
        if (mFenceEntity == null) {
            mFenceEntity = new FenceEntity();
            return;
        }

        FenceEntity entity = mFenceEntity;
        fenceName = entity.getName();
        edName.setText(fenceName);
        Message.Fence fence = entity.getFence();
        int cond = fence.getCond();
        if ((cond & protocol.Message.Fence.CondFlag.ENTER_VALUE) != 0)
            inSwitchCompat.setChecked(true);
        if ((cond & protocol.Message.Fence.CondFlag.LEAVE_VALUE) != 0)
            outSwitchCompat.setChecked(true);

        Message.Fence.Period period = fence.getPeriodList().get(0);
        repeat = period.getRepeat();
        startTime = period.getStartTime().getTime();
        endTime = period.getEndTime().getTime();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime * 1000L);
        startTimeHour = calendar.get(Calendar.HOUR_OF_DAY);
        startTimeMin = calendar.get(Calendar.MINUTE);
        calendar.setTimeInMillis(endTime * 1000L);
        endTimeHour = calendar.get(Calendar.HOUR_OF_DAY);
        endTimeMin = calendar.get(Calendar.MINUTE);
        //围栏范围
        int radius = fence.getShape().getRound().getRadius();
        fence_address_name.setText(String.format("%s", radius));
        tv_fenceRepeat.setText(PublicTools.getDecoderWeak(this, period.getRepeat()));
        fenceType = fence.getIconType();
        switch (fence.getIconType()) {
            //0: 默认；1: 私人； 2: 公共
            case 0:
                fenceImage.setText(R.string.bind_default);
                break;
            case 1:
                fenceImage.setText(R.string.private_place);
                break;
            case 2:
                fenceImage.setText(R.string.public_place);
                break;
        }
        shape = entity.getFence().getShape();

    }


    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.repeat_time: {
                Intent intent = new Intent(this, SetClassDisableRepeatActivity.class);
                intent.putExtra("weekFlag", repeat);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_SET_REPEAT_DATA);
                break;
            }
            case R.id.select_fence_icon: {
                showSelectIcon();
                break;
            }
            case R.id.fence_address: {
                if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                    Message.Fence fence = mFenceEntity.getFence();
                    double lat = fence.getShape().getRound().getLatlon().getLatitude();
                    double lng = fence.getShape().getRound().getLatlon().getLongitude();
                    if (lat == 0 || lng == 0) {
                        startGoogleMapPlacePicker(START_GOOGLE_MAP_PLACE_PICKER_FENCE_ADDRESS, null);
                    } else {
                        startGoogleMapPlacePicker(START_GOOGLE_MAP_PLACE_PICKER_FENCE_ADDRESS, new LatLng(lat, lng));
                    }
                } else if (mMapType == Constants.MAP_TYPE_AMAP) {
                    Intent fenceIntent = new Intent(getApplicationContext(), FenceAddressActivity.class);
                    fenceIntent.putExtra(INTENT_PRARM_fenceEntity, mFenceEntity);
                    startActivityForResult(fenceIntent, ACTIVITY_REQUEST_CODE_AAD_FENCE);
                }
                break;
            }
            case R.id.bt_save_fence: {
                saveFence();
                break;
            }
            default:
                break;
        }
    }


    private void startGoogleMapPlacePicker(int flag, LatLng latLng) {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            if (latLng != null)
                builder.setLatLngBounds(LatLngBounds.builder().include(latLng).build());
            Intent intent = builder.build(this);
            startActivityForResult(intent, flag);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    private void saveFence() {
        if (prepareFenceEntity()) {
            popWaitingDialog(R.string.please_wait);
            if (isAddNew()) {
                model.addNewFence(mDeviceId, mFenceBuilder, new OperateDataListener() {
                    @Override
                    public void operateSuccess(GeneratedMessageV3 messageV3) {
                        dismissDialog();
                        Message.AddFenceRspMsg rspMsg = (Message.AddFenceRspMsg) messageV3;
                        FenceEntity entity = GreenUtils.saveFence(mFenceEntity.getDeviceId(), rspMsg.getFence(0));
                        Intent intent = new Intent();
                        intent.putExtra(FenceEditActivity.INTENT_PRARM_fenceEntity, entity);
                        setResult(RESULT_OK, intent);
                        finish();
                    }

                    @Override
                    public void operateFailure(Message.ErrorCode errorCode) {
                        switch (errorCode) {
                            case OUT_OF_LIMIT:
                                popInfoDialog(R.string.number_of_fence_out_of_limit);
                                break;
                            case TIMEOUT:
                                popErrorDialog(R.string.tip_add_fence_timeout);
                                break;
                            case FAILURE:
                                popErrorDialog(R.string.tip_add_fence_failure);
                                break;

                        }
                    }
                });
            } else {

                final Message.ModifyFenceReqMsg reqMsg = Message.ModifyFenceReqMsg.newBuilder()
                        .setDeviceId(mFenceEntity.getDeviceId())
                        .addFence(mFenceEntity.getFence())
                        .build();

                model.modifyFence(mFenceEntity.getDeviceId(), reqMsg, new OperateDataListener() {
                    @Override
                    public void operateSuccess(GeneratedMessageV3 messageV3) {
                        mFenceEntity.setFence(reqMsg.getFence(0));
                        GreenUtils.modifyFence(mFenceEntity);
                        Intent intent = new Intent();
                        intent.putExtra(FenceEditActivity.INTENT_PRARM_fenceEntity, mFenceEntity);
                        setResult(RESULT_OK, intent);
                        finish();
                    }

                    @Override
                    public void operateFailure(Message.ErrorCode errorCode) {
                        switch (errorCode) {
                            case OUT_OF_LIMIT:
                                popInfoDialog(R.string.number_of_fence_out_of_limit);
                                break;
                            case TIMEOUT:
                                popErrorDialog(R.string.tip_modify_fence_timeout);
                                break;
                            case FAILURE:
                                popErrorDialog(R.string.tip_modify_fence_failure);
                                break;

                        }
                    }
                });
            }
        }
    }

    private boolean prepareFenceEntity() {
        if (TextUtils.isEmpty(fenceName)) {
            toast(R.string.fence_setting_fence_name_null);
            return false;
        }
        if (fenceList != null) {
            for (FenceListAdapter.FenceDataType fenceDataType : fenceList) {
                if (fenceDataType.schoolGuard != null)
                    continue;
                if (!isAddNew() && fenceDataType.entity.getFenceId().equals(mFenceEntity.getFenceId()))
                    continue;
                if (fenceName.equals(fenceDataType.entity.getName())) {
                    toast(R.string.fence_name_conflict);
                    return false;
                }
            }
        }

        if (!inSwitchCompat.isChecked() && !outSwitchCompat.isChecked()) {
            toast(R.string.fence_select_fence_type); //围栏类型选择
            return false;
        }
        int fenceAlertType = protocol.Message.Fence.CondFlag.NONE_VALUE;
        if (inSwitchCompat.isChecked()) {
            fenceAlertType |= protocol.Message.Fence.CondFlag.ENTER_VALUE;
        }
        if (outSwitchCompat.isChecked()) {
            fenceAlertType |= protocol.Message.Fence.CondFlag.LEAVE_VALUE;
        }

        endTime = DateUtil.getTimeAtHM(endTimeHour, endTimeMin);
        startTime = DateUtil.getTimeAtHM(startTimeHour, startTimeMin);

        if (startTime == 0) {
            toast(R.string.fence_setting_setting_start_time);
            return false;
        }
        if (endTime == 0) {
            toast(R.string.fence_setting_setting_over_time);
            return false;
        }
        if (startTime > endTime) {
            toast(R.string.class_disable_start_should_be_less_than_over_time);
            return false;
        }
        if (repeat == 0) {
            toast(R.string.class_disable_select_weekclass_disable_select_week);
            return false;
        }
        if (fenceType < 0) {
            toast(R.string.select_icon);
            return false;
        }
        if (shape == null) {
            toast(R.string.select_fence_area);
            return false;
        }


        try {
            String startTime = String.format("%02d:%02d", startTimeHour, startTimeMin);
            String endTime = String.format("%02d:%02d", endTimeHour, endTimeMin);
            SimpleDateFormat sf = new SimpleDateFormat("HH:mm");
            Date startDate = sf.parse(startTime);
            Date endDate = sf.parse(endTime);
            if (Math.abs(startDate.getTime() - endDate.getTime()) < 5 * 1000L * 60) {
                toast(R.string.fence_setting_begin_over_time_5min);
                return false;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }


        mFenceBuilder.setName(fenceName);
        mFenceBuilder.setCond(fenceAlertType);
        mFenceBuilder.setIconType(fenceType);

        Message.Fence.Period.Builder periodBuilder;
        if (mFenceBuilder.getPeriodCount() > 0) {
            periodBuilder = mFenceBuilder.getPeriod(0).toBuilder();
        } else {
            periodBuilder = Message.Fence.Period.newBuilder();
            mFenceBuilder.addPeriod(periodBuilder);
        }
        periodBuilder.setStartTime(Message.TimePoint.newBuilder().setTime(startTime))
                .setEndTime(Message.TimePoint.newBuilder().setTime(endTime))
                .setRepeat(repeat);
        mFenceBuilder.setPeriod(0, periodBuilder);
        if (TextUtils.isEmpty(mFenceBuilder.getTimezone().getZone())) {
            mFenceBuilder.setTimezone(Message.Timezone.newBuilder().setZone(DateUtil.timezoneISO8601()));
        }
        mFenceBuilder.setShape(shape);

        mFenceEntity.setFence(mFenceBuilder.build());
        if (TextUtils.isEmpty(mFenceEntity.getDeviceId())) {
            mFenceEntity.setDeviceId(mDeviceId);
        }

        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ACTIVITY_REQUEST_CODE_SET_REPEAT_DATA: {
                    repeat = data.getIntExtra(WEEK_FLAG, 0);
                    String showStr = PublicTools.getDecoderWeak(this, repeat);
                    tv_fenceRepeat.setText(showStr);
                }
                break;
                case ACTIVITY_REQUEST_CODE_AAD_FENCE: {
                    GuardianAddrInfo addrInfo = (GuardianAddrInfo) data.getSerializableExtra(FenceActivity.SELECT_FENCE_ADDRESS);
                    shape = Message.Fence.Shape.newBuilder()
                            .setRound(Message.Fence.Shape.Round.newBuilder()
                                    .setLatlon(Message.LatLon.newBuilder()
                                            .setLatitude(addrInfo.lat)
                                            .setLongitude(addrInfo.lng)
                                            .build())
                                    .setRadius(addrInfo.radius)
                                    .build())
                            .build();
                    mFenceEntity.setFenceAddress(addrInfo.address);
                    fence_address_name.setText(String.format("%d", addrInfo.radius));
                }
                break;
                case START_GOOGLE_MAP_PLACE_PICKER_FENCE_ADDRESS: {
                    Place place = PlaceAutocomplete.getPlace(this, data);
                    L.i(TAG, "Place: " + place.getName());
                    com.google.android.gms.maps.model.LatLng latLng = place.getLatLng();
                    GuardianAddrInfo guardianAddrInfo = new GuardianAddrInfo();
                    guardianAddrInfo.lat = latLng.latitude;
                    guardianAddrInfo.lng = latLng.longitude;
                    Intent guardianIntent = new Intent(this, FenceActivity.class);
                    guardianIntent.putExtra(FenceAddressActivity.FENCE_ADDRESS_GUARDIANADDRINFO, guardianAddrInfo);
                    startActivityForResult(guardianIntent, START_GOOGLE_MAP_PLACE_PICKER_FENCE_SELECT_ADDRESS);
                }
                break;
                case START_GOOGLE_MAP_PLACE_PICKER_FENCE_SELECT_ADDRESS: {
                    GuardianAddrInfo addrInfo = (GuardianAddrInfo) data.getSerializableExtra(FenceActivity.SELECT_FENCE_ADDRESS);
                    shape = Message.Fence.Shape.newBuilder()
                            .setRound(Message.Fence.Shape.Round.newBuilder()
                                    .setLatlon(Message.LatLon.newBuilder()
                                            .setLatitude(addrInfo.lat)
                                            .setLongitude(addrInfo.lng)
                                            .build())
                                    .setRadius(addrInfo.radius)
                                    .build())
                            .build();
                    mFenceEntity.setFenceAddress(addrInfo.address);
                }
                break;
                default:
                    break;
            }
        }
    }

    //开始时间
    private void initStartTimeNp(NumberPicker startHourNp, NumberPicker startMinNp) {
        final int[] editStartTime = {5, 57};

        int currentStartTimeHour = 0;
        List<String> hourList = new ArrayList<>();
        String prefix = "0";
        for (int h = 0; h < 24; ++h) {
            if (h >= 10) {
                prefix = "";
            }
            hourList.add(prefix + h);
            if (h == startTimeHour) {
                currentStartTimeHour = h;
                editStartTime[0] = h;
            }
        }
        final String[] mDisplayedHour = hourList.toArray(new String[0]);
        setNumberPicker(startHourNp, mDisplayedHour, currentStartTimeHour).setOnValueChangedListener((picker, oldVal, newVal) -> {
            editStartTime[0] = Integer.valueOf(mDisplayedHour[newVal]);
            startTimeHour = editStartTime[0];
        });

        int currentStartTimeMin = 0;
        prefix = "0";
        List<String> minList = new ArrayList<>();
        for (int m = 0; m < 60; m++) {
            if (m >= 10) {
                prefix = "";
            }
            minList.add(prefix + m);
            if (m == startTimeMin) {
                currentStartTimeMin = m;
                editStartTime[1] = m;
            }
        }
        final String[] mDisplayedMinValues = minList.toArray(new String[0]);
        setNumberPicker(startMinNp, mDisplayedMinValues, currentStartTimeMin).setOnValueChangedListener((picker, oldVal, newVal) -> {
            editStartTime[1] = Integer.valueOf(mDisplayedMinValues[newVal]);
            startTimeMin = editStartTime[1];
        });

    }

    //结束时间
    private void initEndTimeNp(NumberPicker endHourNp, NumberPicker endMinNp) {
        final int[] editEndTime = {23, 5};
        int currentEndTimeHour = 0;
        List<String> hours = new ArrayList<>();
        String prefix = "0";
        for (int h = 0; h < 24; h++) {
            if (h >= 10) {
                prefix = "";
            }
            hours.add(prefix + h);
            if (endTimeHour == h) {
                currentEndTimeHour = h;
                editEndTime[0] = h;
            }
        }
        final String[] mDisplayHourValues = hours.toArray(new String[0]);
        setNumberPicker(endHourNp, mDisplayHourValues, currentEndTimeHour).setOnValueChangedListener((picker, oldVal, newVal) -> {
            editEndTime[0] = Integer.valueOf(mDisplayHourValues[newVal]);
            endTimeHour = editEndTime[0];
        });

        int currentEndTimeMin = 0;
        prefix = "0";
        List<String> mins = new ArrayList<>();
        for (int m = 0; m < 60; m++) {
            if (m >= 10) {
                prefix = "";
            }
            mins.add(prefix + m);
            if (endTimeMin == m) {
                currentEndTimeMin = m;
                editEndTime[1] = m;
            }
        }
        final String[] mDisplayedMinValues = mins.toArray(new String[0]);
        setNumberPicker(endMinNp, mDisplayedMinValues, currentEndTimeMin).setOnValueChangedListener((picker, oldVal, newVal) -> {
            editEndTime[1] = Integer.valueOf(mDisplayedMinValues[newVal]);
            endTimeMin = editEndTime[1];
        });
    }

    private void showSelectIcon() {
        View mView = LayoutInflater.from(this).inflate(R.layout.fence_select_icon, null);
        Dialog alertDialog = createDialog(this, mView);
        alertDialog.show();


        mView.findViewById(R.id.default_place).setOnClickListener(v -> {
            alertDialog.dismiss();
            fenceImage.setText(R.string.bind_default);
            fenceType = FENCE_DEFAULT_ICON_TYPE;
        });

        mView.findViewById(R.id.private_place).setOnClickListener(v -> {
            alertDialog.dismiss();
            fenceImage.setText(R.string.private_place);
            fenceType = FENCE_HOME_ICON_TYPE;
        });

        mView.findViewById(R.id.public_place).setOnClickListener(v -> {
            alertDialog.dismiss();
            fenceImage.setText(R.string.public_place);
            fenceType = FENCE_SCHOOL_ICON_TYPE;
        });
    }


}