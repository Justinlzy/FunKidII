package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.widget.NumberPickerView;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.db.Entity.ClassDisableEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.NotYetLoginException;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.service.tlc.WaitThirdStageTimeoutException;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

/**
 * Created by justin on 2017/11/29.
 */

public class ClassDisableItemActivity extends BaseActivity {
    public static final String TAG = ClassDisableItemActivity.class.getSimpleName();
    public static final int SET_REPEAT_DATA = 2;
    public static final String WEEK_FLAG = "weekFlag";

    private int position;
    private int flag;
    private boolean is_add, hasEditPermission = false;
    private String name;

    private List<ClassDisableEntity> list;
    private TextView repeat_text;
    private EditText edName;

    private int mBeginHour = 8, mBeginMinute = 0, mEndHour = 12, mEndMinute = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.class_disable_item);

        Intent intent = getIntent();
        position = intent.getIntExtra("position", -1);
        is_add = intent.getBooleanExtra("isAdd", false);
        Serializable serializable = intent.getSerializableExtra(ClassDisableActivity.INTENT_PARAM_LIST);
        if (serializable == null) {
            L.w(TAG, "invalid params!!! finish()");
            finish();
            return;
        }
        list = (ArrayList<ClassDisableEntity>) serializable;
        if (!is_add && (position < 0 || position >= list.size())) {
            L.w(TAG, "invalid params!!! finish()");
            finish();
            return;
        }
        setTitleBarTitle(is_add ? R.string.class_disable_period_add : R.string.edit_class_disable);
        hasEditPermission = hasEditPermission();
        initUpdateData();
        initView();
    }

    private void initUpdateData() {
        if (is_add)
            return;
        ClassDisableEntity entity = list.get(position);
        mBeginHour = entity.getBeginHour();
        mBeginMinute = entity.getBeginMinute();
        mEndHour = entity.getEndHour();
        mEndMinute = entity.getEndMinute();

        name = entity.getName();
        flag = entity.getRepeat();
        L.e(TAG, "start_Time: " + mBeginHour + ":" + mBeginMinute +
                "---End_Time: " + mEndHour + ":" + mEndMinute +
                "name:  " + name + "  flag : " + flag);
    }

    private void initView() {
        edName = findViewById(R.id.ed_disable_name);
        if (!TextUtils.isEmpty(name))
            edName.setText(name);

        View beginTimePicker = findViewById(R.id.begin_time_picker);
        NumberPickerView beginHourPicker = beginTimePicker.findViewById(R.id.hour_picker);
        beginHourPicker.setSelectedTextColor(getResources().getColor(R.color.blue_tone));
        beginHourPicker.setMinValue(0);
        beginHourPicker.setMaxValue(23);
        beginHourPicker.setValue(mBeginHour);
        beginHourPicker.setEnabled(hasEditPermission);
        beginHourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> mBeginHour = newVal);

        NumberPickerView beginMinutePicker = beginTimePicker.findViewById(R.id.minute_picker);
        beginMinutePicker.setSelectedTextColor(getResources().getColor(R.color.blue_tone));
        beginMinutePicker.setMinValue(0);
        beginMinutePicker.setMaxValue(59);
        beginMinutePicker.setValue(mBeginMinute);
        beginMinutePicker.setEnabled(hasEditPermission);
        beginMinutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> mBeginMinute = newVal);

        View endTimePicker = findViewById(R.id.end_time_picker);
        NumberPickerView endHourPicker = endTimePicker.findViewById(R.id.hour_picker);
        endHourPicker.setMinValue(0);
        endHourPicker.setMaxValue(23);
        endHourPicker.setValue(mEndHour);
        endHourPicker.setEnabled(hasEditPermission);
        endHourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> mEndHour = newVal);

        NumberPickerView endMinutePicker = endTimePicker.findViewById(R.id.minute_picker);
        endMinutePicker.setMinValue(0);
        endMinutePicker.setMaxValue(59);
        endMinutePicker.setValue(mEndMinute);
        endMinutePicker.setEnabled(hasEditPermission);
        endMinutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> mEndMinute = newVal);

        repeat_text = findViewById(R.id.repeat_time);
        repeat_text.setText(PublicTools.getDecoderWeak(this, flag));

        if (hasEditPermission) {
            findViewById(R.id.set_repeat).setClickable(true);
            findViewById(R.id.save_class_disable).setClickable(true);

            findViewById(R.id.repeat_detailed).setVisibility(View.VISIBLE);
            findViewById(R.id.save_class_disable).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.set_repeat).setClickable(false);
            findViewById(R.id.save_class_disable).setClickable(false);

            findViewById(R.id.repeat_detailed).setVisibility(View.INVISIBLE);
            findViewById(R.id.save_class_disable).setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SET_REPEAT_DATA && resultCode == RESULT_OK) {
            flag = data.getIntExtra(WEEK_FLAG, 0);
            String showStr = PublicTools.getDecoderWeak(flag, this);
            L.e("set class disable repeat： " + showStr);
            repeat_text.setText(showStr);
        }
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        if (!hasEditPermission)
            return;
        switch (viewId) {
            case R.id.set_repeat: {
                Intent intent = new Intent(this, SetClassDisableRepeatActivity.class);
                intent.putExtra("weekFlag", flag);
                startActivityForResult(intent, SET_REPEAT_DATA);
            }
            break;
            case R.id.save_class_disable:
                addClassDisable();
                break;
        }
    }

    //添加上课禁用时间段
    private void addClassDisable() {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "addClassDisable: deviceId is empty");
            popErrorDialog(R.string.setting_failure);
            return;
        }


        if (flag <= 0) {
            toast(R.string.class_disable_select_weekclass_disable_select_week);
            return;
        }
        if (edName.getText().toString().isEmpty()) {
            toast(R.string.fence_setting_fence_name_null);
            return;
        }

        ClassDisableEntity disableEntity = new ClassDisableEntity();
        disableEntity.setName(edName.getText().toString());
        disableEntity.setRepeat(flag);

        disableEntity.setBeginHour(mBeginHour);
        disableEntity.setBeginMinute(mBeginMinute);
        disableEntity.setEndHour(mEndHour);
        disableEntity.setEndMinute(mEndMinute);

        if (mBeginHour == mEndHour && mBeginMinute == mEndMinute) {
            toast(R.string.class_disable_begin_over_time_noting_same);
            return;
        }
        if (disableEntity.getBeginTime() > disableEntity.getEndTime()) {
            toast(R.string.class_disable_start_should_be_less_than_over_time);
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            if (i == position) continue;
            ClassDisableEntity cd_bean = list.get(i);
            if ((cd_bean.getBeginMinute() == disableEntity.getBeginMinute())
                    && (cd_bean.getBeginHour() == disableEntity.getBeginHour())
                    && (cd_bean.getEndMinute() == disableEntity.getEndMinute())
                    && (cd_bean.getEndHour() == disableEntity.getEndHour())
                    && (cd_bean.getRepeat() == disableEntity.getRepeat())) {
                toast(R.string.class_disable_with_other_periods);
                return;
            }
        }
        if (is_add) {
            disableEntity.setEnable(true);
            addClassDisable(disableEntity);
        } else {
            disableEntity.setEnable(list.get(position).getEnable());
            disableEntity.setClassDisableId(list.get(position).getClassDisableId());
            disableEntity.setDeviceId(list.get(position).getDeviceId());
            modifyClassDisable(disableEntity, position);
        }
    }

    //修改上课禁用时间段
    private void modifyClassDisable(final ClassDisableEntity classDisableEntity, final int position) {
        if (TextUtils.isEmpty(mUserId) || TextUtils.isEmpty(mDeviceId)) {
            return;
        }
        popWaitingDialog(R.string.please_wait);
        exec(
                Message.ModifyClassDisableReqMsg.newBuilder()
                        .setDeviceId(mDeviceId)
                        .addClassDisable(Message.ClassDisable.newBuilder()
                                .setId(classDisableEntity.getClassDisableId())
                                .setName(classDisableEntity.getName())
                                .setStartTime(Message.TimePoint.newBuilder().setTime(classDisableEntity.getBeginTime()))
                                .setEndTime(Message.TimePoint.newBuilder().setTime(classDisableEntity.getEndTime()))
                                .setTimezone(Message.Timezone.newBuilder().setZone(classDisableEntity.getTimezone()).build())
                                .setRepeat(classDisableEntity.getRepeat())
                                .setEnable(classDisableEntity.getEnable()))
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ModifyClassDisableRspMsg modifyClassDisableRspMsg = response.getProtoBufMsg();
                            switch (modifyClassDisableRspMsg.getErrCode()) {
                                case SUCCESS:
                                    L.e(TAG, "ModifyClassDisableReqMsg SUCCESS");
                                    Intent intent = new Intent();
                                    list.remove(position);
                                    list.add(position, classDisableEntity);
                                    intent.putExtra("addClassDisableRspMsg", (Serializable) list);
                                    setResult(RESULT_OK, intent);
                                    GreenUtils.addOrModifyClassDisable(mDeviceId, classDisableEntity);
                                    popSuccessDialog(R.string.save_success, true);
                                    return false;
                                case OUT_OF_LIMIT:
                                    popInfoDialog(R.string.number_of_class_disable_out_of_limit);
                                    return false;
                                default:
                                    L.w(TAG, "ModifyClassDisableReqMsg failure: " + modifyClassDisableRspMsg.getErrCode());
                                    break;
                            }
                        } catch (Exception e) {
                            L.e(TAG, "modifyClassDisableRspMsg failure", e);
                        }
                        popErrorDialog(R.string.fail_to_save);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "setClassDisable Exception: " + cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.time_out_to_save);
                        } else if (cause instanceof NotYetConnectedException || cause instanceof IOException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof NotYetLoginException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof WaitThirdStageTimeoutException) {
                        } else {
                            popErrorDialog(R.string.fail_to_save);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }

    private void addClassDisable(final ClassDisableEntity entity) {
        if (TextUtils.isEmpty(mUserId) || TextUtils.isEmpty(mDeviceId)) {
            return;
        }
        popWaitingDialog(R.string.please_wait);
        exec(
                Message.AddClassDisableReqMsg.newBuilder()
                        .setDeviceId(mDeviceId)
                        .addClassDisable(Message.ClassDisable.newBuilder()
                                .setName(entity.getName())
                                .setStartTime(Message.TimePoint.newBuilder().setTime(entity.getBeginTime()))
                                .setEndTime(Message.TimePoint.newBuilder().setTime(entity.getEndTime()))
                                .setRepeat(entity.getRepeat())
                                .setTimezone(Message.Timezone.newBuilder().setZone(entity.getTimezone()).build())
                                .setEnable(entity.getEnable()))
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.AddClassDisableRspMsg addClassDisableRspMsg = response.getProtoBufMsg();
                            switch (addClassDisableRspMsg.getErrCode()) {
                                case SUCCESS:
                                    L.e(TAG, "addClassDisableRspMsg SUCCESS");
                                    Message.ClassDisable classDisable = addClassDisableRspMsg.getClassDisable(0);

                                    ClassDisableEntity classDisableEntity = new ClassDisableEntity();
                                    classDisableEntity.setDeviceId(mDeviceId);
                                    classDisableEntity.setClassDisableId(classDisable.getId());
                                    classDisableEntity.setName(classDisable.getName());
                                    classDisableEntity.setBeginTime(classDisable.getStartTime().getTime());
                                    classDisableEntity.setEndTime(classDisable.getEndTime().getTime());
                                    classDisableEntity.setRepeat(classDisable.getRepeat());
                                    classDisableEntity.setTimezone(classDisable.getTimezone().getZone());
                                    classDisableEntity.setEnable(classDisable.getEnable());

                                    GreenUtils.addOrModifyClassDisable(mDeviceId, classDisableEntity);

                                    list.add(classDisableEntity);

                                    Intent intent = new Intent();
                                    intent.putExtra("addClassDisableRspMsg", (Serializable) list);
                                    setResult(RESULT_OK, intent);
                                    popSuccessDialog(R.string.save_success, true);
                                    return false;
                                case OUT_OF_LIMIT:
                                    popInfoDialog(R.string.number_of_class_disable_out_of_limit);
                                    return false;
                                default:
                                    L.w(TAG, "addClassDisableRspMsg failure: " + addClassDisableRspMsg.getErrCode());
                                    break;
                            }
                        } catch (Exception e) {
                            L.e(TAG, "addClassDisableRspMsg failure", e);
                        }
                        popErrorDialog(R.string.fail_to_save);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "setClassDisable Exception: " + cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.time_out_to_save);
                        } else if (cause instanceof NotYetConnectedException || cause instanceof IOException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof NotYetLoginException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof WaitThirdStageTimeoutException) {
                        } else {
                            popErrorDialog(R.string.fail_to_save);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }
}
