package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.widget.NumberPickerView;
import com.cqkct.FunKidII.Utils.DateUtil;
import com.cqkct.FunKidII.Utils.EmojiInputFilter;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.Utils.Utils;
import com.cqkct.FunKidII.db.Entity.AlarmClockEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.NotYetLoginException;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.service.tlc.WaitThirdStageTimeoutException;

import java.io.IOException;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

import static com.cqkct.FunKidII.Ui.Activity.MoreFunction.ClassDisableItemActivity.WEEK_FLAG;

/**
 * Created by justin on 2017/12/1.
 */

public class AlarmClockItemActivity extends BaseActivity {
    public static final String TAG = AlarmClockItemActivity.class.getSimpleName();

    public static final String INTENT_PARAM_ALARM_CLOCK_POSITION = "position";
    public static final String INTENT_PARAM_ALARM_CLOCK_ENTITY = "AlarmClockEntity";

    public static final int SET_REPEAT_SEND = 1;

    private boolean hasEditPermission = false, is_add, hasVibrationMotor = false;
    private AlarmClockEntity alarmClockEntity;
    private int mNoticeType, mWeekFlagRepeat, position = -1;
    private SwitchCompat mShock, mBeep;
    private TextView tv_repeat;
    private EditText etName;
    private RelativeLayout rlShock;
    private int hour, minute;
    private InputFilter emojiFilter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_clock_item_activity);
        Intent intent = getIntent();
        position = intent.getIntExtra(INTENT_PARAM_ALARM_CLOCK_POSITION, -1);
        alarmClockEntity = (AlarmClockEntity) intent.getSerializableExtra(INTENT_PARAM_ALARM_CLOCK_ENTITY);
        is_add = position < 0;
        if (!is_add) {
            setTitleBarTitle(getString(R.string.edit_alarm_clock));
            if (alarmClockEntity == null) {
                L.w(TAG, "invalid params!!! finish()");
                finish();
                return;
            }
        }else {
            setTitleBarTitle(R.string.add_alarm_clock);
        }
        hasEditPermission = hasEditPermission();
        initData();
    }

    public static String decodeNoticeToString(Context context, int noticeType) {
        String noticeText = "";
        if ((noticeType & Message.AlarmClock.NoticeFlag.SOUND_VALUE) != 0) {
            noticeText = context.getString(R.string.alarm_notice_sound);
        }
        if ((noticeType & Message.AlarmClock.NoticeFlag.VIBRATE_VALUE) != 0) {
            if (!TextUtils.isEmpty(noticeText)) {
                noticeText += " + ";
            }
            noticeText += context.getString(R.string.alarm_notice_vibrate);
        }
        if (TextUtils.isEmpty(noticeText))
            noticeText = context.getString(R.string.none);
        return noticeText;
    }

    private void initView(AlarmClockEntity ac) {

        mBeep = findViewById(R.id.ib_switch_beep);
        mShock = findViewById(R.id.ib_switch_shock);

        etName = findViewById(R.id.ed_name);
        etName.setText(ac.getName());
        etName.setSelection(ac.getName().length());
        etName.setFilters(new InputFilter[]{emojiFilter});
        etName.addTextChangedListener(new TextWatcher() {
            private int editStart;
            private int editEnd;

            private final int maxLength = getResources().getInteger(R.integer.maxLength_of_alarm_clock_name);

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                editStart = etName.getSelectionStart();
                editEnd = etName.getSelectionEnd();

                // 先去掉监听器，否则会出现栈溢出
                etName.removeTextChangedListener(this);

                // 注意这里只能每次都对整个EditText的内容求长度，不能对删除的单个字符求长度
                // 因为是中英文混合，单个字符而言，calculateLength函数都会返回1
                while (Utils.charSequenceLength_zhCN(s.toString()) > maxLength) { // 当输入字符个数超过限制的大小时，进行截断操作
                    s.delete(editStart - 1, editEnd);
                    editStart--;
                    editEnd--;
                }
                etName.setSelection(editStart);

                // 恢复监听器
                etName.addTextChangedListener(this);
            }
        });
        tv_repeat = findViewById(R.id.repeat_text);

        int noticeType = ac.getNoticeFlag();
        mBeep.setChecked((noticeType & Message.AlarmClock.NoticeFlag.SOUND_VALUE) != 0);
        mShock.setChecked((noticeType & Message.AlarmClock.NoticeFlag.VIBRATE_VALUE) != 0);

        tv_repeat.setText(PublicTools.getDecoderWeak(this, ac.getRepeat()));

        if (hasEditPermission) {
            etName.setEnabled(true);
            findViewById(R.id.iv_repeat_next).setVisibility(View.VISIBLE);
            mBeep.setEnabled(true);
            mShock.setEnabled(true);
            findViewById(R.id.save_alarm_clock).setVisibility(View.VISIBLE);
        } else {
            etName.setEnabled(false);
            findViewById(R.id.iv_repeat_next).setVisibility(View.INVISIBLE);
            mBeep.setEnabled(false);
            mShock.setEnabled(false);
            findViewById(R.id.save_alarm_clock).setVisibility(View.INVISIBLE);
        }
    }


    private void initData() {
        emojiFilter = new EmojiInputFilter();
        rlShock = findViewById(R.id.rl_shock);
        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
        if (deviceInfo != null) {
            hasVibrationMotor = (deviceInfo.getDeviceEntity().getSysInfo().getHwFeature() & Message.HwFeature.HWF_VIBRATION_MOTOR_VALUE) != 0;
        }

        AlarmClockEntity ac;
        if (position >= 0) {
            ac = alarmClockEntity;
            mNoticeType = ac.getNoticeFlag();
            mWeekFlagRepeat = ac.getRepeat();
            hour = ac.getHour();
            minute = ac.getMinute();
        } else {
            ac = new AlarmClockEntity();
            ac.setMinute(0);
            ac.setHour(0);
            ac.setNoticeFlag(0);
            ac.setRepeat(0);
//            ac.setName(getString(R.string.alarm_clock));
//            ac.setName(getString(R.string.alarm_intput_note));
            ac.setName("");
            mNoticeType = ac.getNoticeFlag();
            mWeekFlagRepeat = ac.getRepeat();
        }

        if (!hasVibrationMotor && (mNoticeType & Message.AlarmClock.NoticeFlag.VIBRATE_VALUE) != 0) {
            mNoticeType &= ~Message.AlarmClock.NoticeFlag.VIBRATE_VALUE;
        }
        rlShock.setVisibility(hasVibrationMotor ? View.VISIBLE : View.GONE);

        NumberPickerView hourPicker = findViewById(R.id.hour_picker);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(ac.getHour());
        hourPicker.setEnabled(hasEditPermission);
        hourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> hour = newVal);
        NumberPickerView minutePicker = findViewById(R.id.minute_picker);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(ac.getMinute());
        minutePicker.setEnabled(hasEditPermission);
        minutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> minute = newVal);

        initView(ac);
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.save_alarm_clock: {
                if (TextUtils.isEmpty(etName.getText().toString().trim())) {
                    toast(R.string.alarm_note_not_null);
                    return;
                }

                // 实现了单次闹钟
//                if (mWeekFlagRepeat == 0) {
//                    toast("请设置周期");
//                    return;
//                }

                if (getNoticeType() <= 0) {
                    toast(R.string.alarm_clock_setting_hint);
                    return;
                }

                AlarmClockEntity alarmClock = new AlarmClockEntity();
                alarmClock.setName(etName.getText().toString());
                alarmClock.setNoticeFlag(getNoticeType());
                alarmClock.setRepeat(mWeekFlagRepeat);
                alarmClock.setEnable(true);
                alarmClock.setTimezone(DateUtil.timezoneISO8601());
                if (mWeekFlagRepeat == 0) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    if (calendar.before(Calendar.getInstance())) {
                        // 时间已过，闹钟设为明天
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                    }
                    alarmClock.setTimePoint(calendar.getTimeInMillis() / 1000L);
                } else {
                    alarmClock.setHour(hour);
                    alarmClock.setMinute(minute);
                }
                if (is_add) {
                    addAlarmClock(alarmClock);
                } else {
                    alarmClock.setAlarmClockId(alarmClockEntity.getAlarmClockId());
                    modifyAlarmClock(alarmClock);
                }
            }
            break;
            default:
                break;
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_repeat: {
                Intent intent = new Intent(this, SetClassDisableRepeatActivity.class);
                intent.putExtra("weekFlag", mWeekFlagRepeat);
                startActivityForResult(intent, SET_REPEAT_SEND);
            }
            break;
            case R.id.ib_switch_beep:
                if ((mNoticeType & Message.AlarmClock.NoticeFlag.SOUND_VALUE) == 0) {
                    mNoticeType |= Message.AlarmClock.NoticeFlag.SOUND_VALUE;
                    mBeep.setChecked(true);
                } else {
                    mNoticeType &= ~Message.AlarmClock.NoticeFlag.SOUND_VALUE;
                    mBeep.setChecked(false);
                }
                break;
            case R.id.ib_switch_shock:
                if ((mNoticeType & Message.AlarmClock.NoticeFlag.VIBRATE_VALUE) == 0) {
                    mNoticeType |= Message.AlarmClock.NoticeFlag.VIBRATE_VALUE;
                    mShock.setChecked(true);
                } else {
                    mNoticeType &= ~Message.AlarmClock.NoticeFlag.VIBRATE_VALUE;
                    mShock.setChecked(false);
                }
                break;
        }
    }

    private void modifyAlarmClock(AlarmClockEntity alarmClock) {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "modifyAlarmClock: deviceId is empty");
            return;
        }

        popWaitingDialog(getString(R.string.save_setting));

        alarmClock.setSynced(false);
        final Message.ModifyAlarmClockReqMsg reqMsg = Message.ModifyAlarmClockReqMsg.newBuilder()
                .setDeviceId(deviceId)
                .addAlarmClock(alarmClock.getAlarmClock())
                .build();

        L.v(TAG, "modifyAlarmClock: " + reqMsg);
        exec(
                reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ModifyAlarmClockRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "addAlarmClock onResponse: " + rspMsg);
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS: {
                                    AlarmClockEntity entity = GreenUtils.saveAlarmClock(deviceId, reqMsg.getAlarmClock(0));
                                    Intent intent = new Intent();
                                    intent.putExtra(INTENT_PARAM_ALARM_CLOCK_ENTITY, entity);
                                    intent.putExtra(INTENT_PARAM_ALARM_CLOCK_POSITION, position);
                                    setResult(RESULT_OK, intent);
                                    popSuccessDialog(R.string.save_success, true);
                                    return true;
                                }
                                case OUT_OF_LIMIT:
                                    popInfoDialog(R.string.number_of_alarm_clock_out_of_limit);
                                    return false;
                                default:
                                    break;
                            }
                            L.w(TAG, "modifyAlarmClock onResponse: " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "modifyAlarmClock onResponse", e);
                        }
                        popErrorDialog(R.string.fail_to_save);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "modifyAlarmClock onException: " + cause);
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

    private void addAlarmClock(final AlarmClockEntity entity) {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "addAlarmClock: deviceId is empty");
            return;
        }

        popWaitingDialog(getString(R.string.save_setting));

        entity.setSynced(false);
        final Message.AddAlarmClockReqMsg addAlarmClockReqMsg = Message.AddAlarmClockReqMsg.newBuilder()
                .setDeviceId(deviceId)
                .addAlarmClock(entity.getAlarmClock())
                .build();

        L.v(TAG, "addAlarmClock: " + addAlarmClockReqMsg);

        exec(
                addAlarmClockReqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.AddAlarmClockRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "addAlarmClock onResponse: " + rspMsg);
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS: {
                                    List<Message.AlarmClock> clocks = rspMsg.getAlarmClockList();
                                    Message.AlarmClock alarmClock = clocks.get(0);
                                    AlarmClockEntity entity = GreenUtils.saveAlarmClock(deviceId, alarmClock);
                                    Intent intent = new Intent();
                                    intent.putExtra(INTENT_PARAM_ALARM_CLOCK_ENTITY, entity);
                                    intent.putExtra(INTENT_PARAM_ALARM_CLOCK_POSITION, position);
                                    setResult(RESULT_OK, intent);
                                    popSuccessDialog(R.string.save_success, true);
                                    return false;
                                }
                                case OUT_OF_LIMIT:
                                    popInfoDialog(R.string.number_of_alarm_clock_out_of_limit);
                                    return false;
                                default:
                                    break;
                            }
                            L.w(TAG, "addAlarmClock onResponse: " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "addAlarmClock onResponse", e);
                        }
                        popErrorDialog(R.string.fail_to_save);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "addAlarmClock onException", cause);
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

    private int getNoticeType() {
        int notice_type = 0;

        if ((mNoticeType & Message.AlarmClock.NoticeFlag.SOUND_VALUE) != 0)
            notice_type |= Message.AlarmClock.NoticeFlag.SOUND_VALUE;

        if ((mNoticeType & Message.AlarmClock.NoticeFlag.VIBRATE_VALUE) != 0)
            notice_type |= Message.AlarmClock.NoticeFlag.VIBRATE_VALUE;

        return notice_type;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SET_REPEAT_SEND && resultCode == RESULT_OK) {
            mWeekFlagRepeat = data.getIntExtra(WEEK_FLAG, 0);
            L.e(TAG, "onActivityResult mWeekFlagRepeat:" + mWeekFlagRepeat);
            String showStr = PublicTools.getDecoderWeak(this, data.getIntExtra("weekFlag", 0));
            L.e(TAG, "set class disable tv_repeat： " + showStr);
            tv_repeat.setText(showStr);
        }
    }


    @Override
    public void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo) {
        if (deviceInfo.getDeviceId().equals(mDeviceId)) {
            boolean has = (deviceInfo.getDeviceEntity().getSysInfo().getHwFeature() & Message.HwFeature.HWF_VIBRATION_MOTOR_VALUE) != 0;
            if (hasVibrationMotor != has) {
                hasVibrationMotor = has;
                rlShock.setVisibility(hasVibrationMotor ? View.VISIBLE : View.GONE);
            }
        }
    }
}
