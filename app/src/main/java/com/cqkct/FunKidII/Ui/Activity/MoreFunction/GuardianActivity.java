package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.Bean.GuardianAddrInfo;
import com.cqkct.FunKidII.Bean.SchoolGuardianInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseMapActivity;
import com.cqkct.FunKidII.Ui.Activity.FenceListActivity;
import com.cqkct.FunKidII.Ui.Adapter.FenceListAdapter;
import com.cqkct.FunKidII.Ui.Listener.OperateDataListener;
import com.cqkct.FunKidII.Ui.Model.GuardianModel;
import com.cqkct.FunKidII.Ui.view.NumberPickerBlue;
import com.cqkct.FunKidII.Utils.Constants;
import com.cqkct.FunKidII.Utils.DateUtil;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.protobuf.GeneratedMessageV3;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import protocol.Message;

public class GuardianActivity extends BaseMapActivity {
    public final String TAG = this.getClass().getSimpleName();

    public static final int SET_REPEAT_DATA = 1;
    public static final int GUARDIAN_AM_TIME_FLAG = 2;
    public static final int GUARDIAN_PM_TIME_FLAG = 3;
    public static final int GUARDIAN_SCHOOL_ADDRESS_FLAG = 4;
    public static final int GUARDIAN_HOME_ADDRESS_FLAG = 5;
    public static final int START_GOOGLE_MAP_PLACEPICKER_HOME_ADDRESS = 6;
    public static final int START_GOOGLE_MAP_FENCE_HOME_ADDRESS = 7;
    public static final int START_GOOGLE_MAP_PLACEPICKER_SCHOOL_ADDRESS = 8;
    public static final int START_GOOGLE_MAP_FENCE_SCHOOL_ADDRESS = 9;

    public static final String GUARDIAN_AM_TIME = "GUARDIAN_AM_TIME";
    public static final String GUARDIAN_PM_TIME = "GUARDIAN_PM_TIME";
    public static final String GUARDIAN_ADDRESS_DATA = "GUARDIAN_ADDRESS_DATA";
    public static final String IS_GUARDIAN_HOME_ADDRESS = "IS_GUARDIAN_HOME_ADDRESS";
    public static final String GUARDIAN_ADDRESS_HOMEORSCHOOL = "GUARDIAN_ADDRESS_HOMEORSCHOOL";
    public static final String GUARDIAN_ADDRESS_FENCE_LAT = "GUARDIAN_ADDRESS_FENCE_LAT";
    public static final String GUARDIAN_ADDRESS_FENCE_LON = "GUARDIAN_ADDRESS_FENCE_LON";
    public static final String GUARDIAN_ADDRESS_FENCE_RADIUS = "GUARDIAN_ADDRESS_FENCE_RADIUS";


    private int flag = 62;
    public static final String WEEK_FLAG = "weekFlag";

    private TextView titleOkTextView;
    private boolean editable = false;
    private boolean btnGuardian_enable = false;
    private boolean hasEditPermission = false; //修改权限
    private SchoolGuardianInfo schoolGuardianInfo;
    private TextView tv_am_time, tv_pm_time, tv_latest_go_home_time, tv_repeat_guardian, tv_holiday_guardian, tv_school_address, tv_home_address;
    private ImageView iv_am_next, iv_pm_next, iv_last_back_next, iv_re_next,
            iv_statutory_holiday_next, iv_home_address_next, iv_school_address_next;
    private int lastBackHomeHour = 18, lastBackHomeMinute = 0;
    private GuardianModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_guardian);
        setTitleBarTitle(R.string.school_guardian);
        hasEditPermission = hasEditPermission();
        schoolGuardianInfo = new SchoolGuardianInfo();
        initView();
    }


    private void initView() {
        model = new GuardianModel(this);
        titleOkTextView = findViewById(R.id.title_bar_right_text);
        titleOkTextView.setVisibility(hasEditPermission ? View.VISIBLE : View.GONE);
        titleOkTextView.setText(R.string.edit);

        iv_am_next = findViewById(R.id.sg_am_next);
        iv_pm_next = findViewById(R.id.sg_pm_next);
        iv_last_back_next = findViewById(R.id.sg_last_back_next);
        iv_re_next = findViewById(R.id.sg_re_next);
        iv_statutory_holiday_next = findViewById(R.id.sg_statutory_holiday_next);
        iv_home_address_next = findViewById(R.id.sg_home_address_next);
        iv_school_address_next = findViewById(R.id.sg_school_address_next);



        tv_am_time = findViewById(R.id.tv_sg_am_time);
        tv_pm_time = findViewById(R.id.tv_sg_pm_time);

        tv_latest_go_home_time = findViewById(R.id.tv_latest_go_home_time);
        tv_repeat_guardian = findViewById(R.id.tv_repeat_guardian);
        tv_holiday_guardian = findViewById(R.id.tv_holiday_guardian);

        tv_school_address = findViewById(R.id.tv_school_address);
        tv_school_address.setSelected(true);
        tv_home_address = findViewById(R.id.tv_home_address);
        tv_home_address.setSelected(true);

        RefreshNextImageView();

        loadData();

        initViewData();
    }

    private void loadData() {
        Message.SchoolGuard schoolGuard = model.loadData(mDeviceId);
        if (schoolGuard != null)
            updateView(schoolGuard);

        //form server
        popWaitingDialog(R.string.loading);
        model.getSchoolGuardianData(mDeviceId, new OperateDataListener() {
            @Override
            public void operateSuccess(GeneratedMessageV3 messageV3) {
                Message.SchoolGuard schoolGuard = (Message.SchoolGuard) messageV3;
                updateView(schoolGuard);
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

    private void updateView(Message.SchoolGuard school_guard) {
        if (school_guard == null) {
            school_guard = Message.SchoolGuard.newBuilder().build();
        }
        Message.SchoolGuard.Addr school_address = school_guard.getSchool();
        schoolGuardianInfo.setId(school_guard.getId());
        schoolGuardianInfo.setName(school_guard.getName());
        schoolGuardianInfo.setSchool_address(school_address.getAddr());

        switch (school_address.getFence().getShapeCase()) {
            case ROUND: {
                Message.Fence.Shape.Round round = school_address.getFence().getRound();
                Message.LatLon latLons = round.getLatlon();
                schoolGuardianInfo.setSchool_Latitude(latLons.getLatitude());
                schoolGuardianInfo.setSchool_LonTitude(latLons.getLongitude());
                schoolGuardianInfo.setSchool_radius(round.getRadius());
            }
            break;
            case POLYGON: {
                //TODO:多边形围栏
                Message.Fence.Shape.Polygon polygon = school_address.getFence().getPolygon();
                List<Message.LatLon> latLons = polygon.getVerticesList();
            }
            break;
            default:
        }
        Message.SchoolGuard.Addr home_address = school_guard.getHome();
        schoolGuardianInfo.setHome_address(home_address.getAddr());
        switch (home_address.getFence().getShapeCase()) {
            case ROUND: {
                Message.Fence.Shape.Round round = home_address.getFence().getRound();
                Message.LatLon latLons = round.getLatlon();
                schoolGuardianInfo.setHome_Latitude(latLons.getLatitude());
                schoolGuardianInfo.setHome_LonTitude(latLons.getLongitude());
                schoolGuardianInfo.setHome_radius(round.getRadius());
            }
            break;
            case POLYGON:
                //TODO:多边形围栏
                break;
            default:
                break;
        }
        schoolGuardianInfo.setAm_arrival_time(school_guard.getForenoon().getStartTime().getTime());
        schoolGuardianInfo.setAm_leave_time(school_guard.getForenoon().getEndTime().getTime());
        schoolGuardianInfo.setPm_arrival_time(school_guard.getAfternoon().getStartTime().getTime());
        schoolGuardianInfo.setPm_leave_time(school_guard.getAfternoon().getEndTime().getTime());
        schoolGuardianInfo.setLatest_arrival_home(school_guard.getGoHome().getTime());
        schoolGuardianInfo.setRepeat(school_guard.getRepeat());
        schoolGuardianInfo.setIs_holiday(school_guard.getGuardDuringHolidays());
        schoolGuardianInfo.setIs_enable(school_guard.getEnable());
        btnGuardian_enable = schoolGuardianInfo.isIs_enable();
        if (hasEditPermission) {
        } else {
        }
        initViewData();
    }

    private void initViewData() {
        Calendar calendar = Calendar.getInstance();
        if (schoolGuardianInfo.getAm_arrival_time() == 0) {
            calendar.set(Calendar.HOUR_OF_DAY, 8);
            calendar.set(Calendar.MINUTE, 0);
            schoolGuardianInfo.setAm_arrival_time(calendar.getTimeInMillis() / 1000L);
        } else {
            calendar.setTimeInMillis(schoolGuardianInfo.getAm_arrival_time() * 1000L);
        }
        int am_arrival_hour = calendar.get(Calendar.HOUR_OF_DAY);
        int am_arrival_min = calendar.get(Calendar.MINUTE);
        String am_arrival = String.format("%02d:%02d", am_arrival_hour, am_arrival_min);

        if (schoolGuardianInfo.getAm_leave_time() == 0) {
            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 0);
            schoolGuardianInfo.setAm_leave_time(calendar.getTimeInMillis() / 1000L);
        } else {
            calendar.setTimeInMillis(schoolGuardianInfo.getAm_leave_time() * 1000L);
        }
        int am_leave_hour = calendar.get(Calendar.HOUR_OF_DAY);
        int am_leave_min = calendar.get(Calendar.MINUTE);
        String am_leave = String.format("%02d:%02d", am_leave_hour, am_leave_min);

        if (schoolGuardianInfo.getPm_arrival_time() == 0) {
            calendar.set(Calendar.HOUR_OF_DAY, 14);
            calendar.set(Calendar.MINUTE, 30);
            schoolGuardianInfo.setPm_arrival_time(calendar.getTimeInMillis() / 1000L);
        } else {
            calendar.setTimeInMillis(schoolGuardianInfo.getPm_arrival_time() * 1000L);
        }
        int pm_arrival_hour = calendar.get(Calendar.HOUR_OF_DAY);
        int pm_arrival_min = calendar.get(Calendar.MINUTE);
        String pm_arrival = String.format("%02d:%02d", pm_arrival_hour, pm_arrival_min);

        if (schoolGuardianInfo.getPm_leave_time() == 0) {
            calendar.set(Calendar.HOUR_OF_DAY, 16);
            calendar.set(Calendar.MINUTE, 30);
            schoolGuardianInfo.setPm_leave_time(calendar.getTimeInMillis() / 1000L);
        } else {
            calendar.setTimeInMillis(schoolGuardianInfo.getPm_leave_time() * 1000L);
        }
        int pm_leave_hour = calendar.get(Calendar.HOUR_OF_DAY);
        int pm_leave_min = calendar.get(Calendar.MINUTE);
        String pm_leave = String.format("%02d:%02d", pm_leave_hour, pm_leave_min);

        tv_am_time.setText(String.valueOf(am_arrival + "-" + am_leave));
        tv_pm_time.setText(String.valueOf(pm_arrival + "-" + pm_leave));

//        long latest_arrival = schoolGuardianInfo.getLatest_arrival_home();
        if (schoolGuardianInfo.getLatest_arrival_home() == 0) {
            calendar.set(Calendar.HOUR_OF_DAY, 18);
            calendar.set(Calendar.MINUTE, 0);
            schoolGuardianInfo.setLatest_arrival_home(calendar.getTimeInMillis() / 1000L);
        } else {
            calendar.setTimeInMillis(schoolGuardianInfo.getLatest_arrival_home() * 1000L);
        }
        lastBackHomeHour = calendar.get(Calendar.HOUR_OF_DAY);
        lastBackHomeMinute = calendar.get(Calendar.MINUTE);
        String latest_arrival_time = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        tv_latest_go_home_time.setText(latest_arrival_time);

        if (schoolGuardianInfo.getRepeat() == 0) {
            //默认周一到周五
            schoolGuardianInfo.setRepeat(62);
        } else
            flag = schoolGuardianInfo.getRepeat();

        String showStr = PublicTools.getDecoderWeak(this, schoolGuardianInfo.getRepeat());
        tv_repeat_guardian.setText(showStr);

        tv_holiday_guardian.setText(schoolGuardianInfo.isIs_holiday() ? getString(R.string.school_guide_guide) : getString(R.string.school_guide_no_guide));

        tv_school_address.setText(TextUtils.isEmpty(schoolGuardianInfo.getSchool_address()) ? getString(R.string.unfilled) : schoolGuardianInfo.getSchool_address());
        tv_home_address.setText(TextUtils.isEmpty(schoolGuardianInfo.getHome_address()) ? getString(R.string.unfilled) : schoolGuardianInfo.getHome_address());
    }

    public void RefreshNextImageView() {
        titleOkTextView.setText(editable ? getString(R.string.school_guide_OK) : getString(R.string.edit));
        iv_am_next.setVisibility(editable ? View.VISIBLE : View.INVISIBLE);
        iv_pm_next.setVisibility(editable ? View.VISIBLE : View.INVISIBLE);
        iv_last_back_next.setVisibility(editable ? View.VISIBLE : View.INVISIBLE);
        iv_re_next.setVisibility(editable ? View.VISIBLE : View.INVISIBLE);
        iv_statutory_holiday_next.setVisibility(editable ? View.VISIBLE : View.INVISIBLE);
        iv_home_address_next.setVisibility(editable ? View.VISIBLE : View.INVISIBLE);
        iv_school_address_next.setVisibility(editable ? View.VISIBLE : View.INVISIBLE);

        findViewById(R.id.sg_rl_am_guard_time).setClickable(editable);
        findViewById(R.id.sg_rl_pm_guard_time).setClickable(editable);
        findViewById(R.id.sg_rl_last_back).setClickable(editable);
        findViewById(R.id.sg_rl_re).setClickable(editable);
        findViewById(R.id.sg_rl_statutory_holiday).setClickable(editable);
        findViewById(R.id.sg_rl_school_address).setClickable(editable);
        findViewById(R.id.sg_rl_home_address).setClickable(editable);
    }

    @Override
    public void onTitleBarClick(View v) {
        super.onTitleBarClick(v);
        switch (v.getId()) {
            case R.id.title_bar_right_text:
                if (editable) {
                    // 可编辑状态
                    modifySchoolGuardian();
                } else {
                    // 不可编辑状态
                    editable = true;
                    RefreshNextImageView();
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.sg_rl_am_guard_time:
                if (editable) {
                    Intent intent = new Intent(this, AmGuardTimeActivity.class);
                    intent.putExtra(GUARDIAN_AM_TIME, schoolGuardianInfo);
                    startActivityForResult(intent, GUARDIAN_AM_TIME_FLAG);
                }
                break;
            case R.id.sg_rl_pm_guard_time:
                if (editable) {
                    Intent intent = new Intent(this, PmGuardTimeActivity.class);
                    intent.putExtra(GUARDIAN_PM_TIME, schoolGuardianInfo);
                    startActivityForResult(intent, GUARDIAN_PM_TIME_FLAG);
                }
                break;
            case R.id.sg_rl_last_back:
                if (editable) {
                    showLatestTimeBackDialog(this);
                }
                break;
            case R.id.sg_rl_re:
                if (editable) {
                    Intent intent = new Intent(this, SetClassDisableRepeatActivity.class);
                    intent.putExtra(WEEK_FLAG, flag);
                    startActivityForResult(intent, SET_REPEAT_DATA);
                }
                break;
            case R.id.sg_rl_statutory_holiday:
                if (editable) {
                    showStatutoryHolidayDialog(view.getContext());
                }
                break;
            case R.id.sg_rl_school_address:
                if (editable) {
                    if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                        LatLng latLng = null;
                        if (schoolGuardianInfo.getSchool_Latitude() == null || schoolGuardianInfo.getSchool_LonTitude() == null) {
                            String deviceId = mDeviceId;
                            if (!TextUtils.isEmpty(deviceId)) {
                                Message.Position lastPosition = DeviceInfo.getLastPosition(deviceId);
                                if (lastPosition != null) {
                                    latLng = new LatLng(lastPosition.getLatLng().getLatitude(), lastPosition.getLatLng().getLongitude());
                                    if (latLng.latitude == 0 && latLng.longitude == 0) {
                                        latLng = null;
                                    }
                                }
                            }
                        } else {
                            latLng = new LatLng(schoolGuardianInfo.getSchool_Latitude(), schoolGuardianInfo.getSchool_LonTitude());
                        }
                        startGoogleMapPlacePicker(START_GOOGLE_MAP_PLACEPICKER_SCHOOL_ADDRESS, latLng);
                    } else if (mMapType == Constants.MAP_TYPE_AMAP) {
                        Intent intent = new Intent(this, GuardianAddressActivity.class);
                        intent.putExtra(GUARDIAN_ADDRESS_HOMEORSCHOOL, false);
                        intent.putExtra(GUARDIAN_ADDRESS_FENCE_LAT, schoolGuardianInfo.getSchool_Latitude());
                        intent.putExtra(GUARDIAN_ADDRESS_FENCE_LON, schoolGuardianInfo.getSchool_LonTitude());
                        intent.putExtra(GUARDIAN_ADDRESS_FENCE_RADIUS, schoolGuardianInfo.getSchool_radius());
                        startActivityForResult(intent, GUARDIAN_SCHOOL_ADDRESS_FLAG);
                    }
                }
                break;
            case R.id.sg_rl_home_address:
                if (editable) {
                    if (mMapType == Constants.MAP_TYPE_GOOGLE) {
                        if (schoolGuardianInfo.getHome_Latitude() == null || schoolGuardianInfo.getHome_LonTitude() == null) {
                            startGoogleMapPlacePicker(START_GOOGLE_MAP_PLACEPICKER_HOME_ADDRESS, null);
                        } else {
                            double lat = schoolGuardianInfo.getHome_Latitude(), lon = schoolGuardianInfo.getHome_LonTitude();
                            startGoogleMapPlacePicker(START_GOOGLE_MAP_PLACEPICKER_HOME_ADDRESS, new LatLng(lat, lon));
                        }
                    } else if (mMapType == Constants.MAP_TYPE_AMAP) {
                        Intent intent = new Intent(this, GuardianAddressActivity.class);
                        intent.putExtra(GUARDIAN_ADDRESS_HOMEORSCHOOL, true);
                        intent.putExtra(GUARDIAN_ADDRESS_FENCE_LAT, schoolGuardianInfo.getHome_Latitude());
                        intent.putExtra(GUARDIAN_ADDRESS_FENCE_LON, schoolGuardianInfo.getHome_LonTitude());
                        intent.putExtra(GUARDIAN_ADDRESS_FENCE_RADIUS, schoolGuardianInfo.getHome_radius());
                        startActivityForResult(intent, GUARDIAN_HOME_ADDRESS_FLAG);
                    }
                }
                break;
//            case R.id.sg_btn_guardian: {
//                btnGuardian_enable = !btnGuardian_enable;
//                schoolGuardianInfo.setIs_enable(btnGuardian_enable);
//                modifySchoolGuardian();
//            }
//            break;
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

    private void showStatutoryHolidayDialog(Context context) {
        final int[] editGuardian = {schoolGuardianInfo.isIs_holiday() ? 1 : 0};
        View mView = LayoutInflater.from(context).inflate(R.layout.baby_information_grade, null);
        TextView title = mView.findViewById(R.id.dialog_title);
        title.setText(R.string.please_select_sex);
        NumberPicker numberPicker = mView.findViewById(R.id.sex_pick);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.statutory_holiday_open_guard)
                .setView(mView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null);
        final AlertDialog alertDialog = builder.create();
        final String[] strings = new String[]{getString(R.string.school_guide_no_guide), getString(R.string.school_guide_guide)};
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(strings.length - 1);
        numberPicker.setDisplayedValues(strings);
        //设置不可编辑
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        numberPicker.setValue(schoolGuardianInfo.isIs_holiday() ? 1 : 0);
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {
                editGuardian[0] = newVal;
            }
        });
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alertDialog.dismiss();
                        schoolGuardianInfo.setIs_holiday(editGuardian[0] != 0);
                        tv_holiday_guardian.setText(schoolGuardianInfo.isIs_holiday() ? getString(R.string.school_guide_guide) : getString(R.string.school_guide_no_guide));
                        L.e(TAG, "schoolGuardianInfo.isIs_holiday():  " + schoolGuardianInfo.isIs_holiday());
                    }
                });
            }
        });
        alertDialog.show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SET_REPEAT_DATA: {
                    flag = intent.getIntExtra(WEEK_FLAG, 0);
                    String showStr = PublicTools.getDecoderWeak(flag, this);
                    schoolGuardianInfo.setRepeat(flag);
                    tv_repeat_guardian.setText(showStr);
                }
                break;
                case GUARDIAN_AM_TIME_FLAG: {
                    setViewTimeData(GUARDIAN_AM_TIME, intent);
                }
                break;
                case GUARDIAN_PM_TIME_FLAG: {
                    setViewTimeData(GUARDIAN_PM_TIME, intent);
                }
                break;
                case GUARDIAN_SCHOOL_ADDRESS_FLAG: {
                    setSchoolAddressViewData(GUARDIAN_ADDRESS_DATA, intent);
                }
                break;
                case GUARDIAN_HOME_ADDRESS_FLAG: {
                    setHomeAddressViewData(GUARDIAN_ADDRESS_DATA, intent);
                }
                break;
                case START_GOOGLE_MAP_PLACEPICKER_SCHOOL_ADDRESS: {
                    startGuardianFenceActivity(intent, false, START_GOOGLE_MAP_FENCE_SCHOOL_ADDRESS);
                }
                break;
                case START_GOOGLE_MAP_PLACEPICKER_HOME_ADDRESS: {
                    startGuardianFenceActivity(intent, true, START_GOOGLE_MAP_FENCE_HOME_ADDRESS);
                }
                break;
                case START_GOOGLE_MAP_FENCE_HOME_ADDRESS: {
                    setHomeAddressViewData(GuardianFenceActivity.SCHOOLFENCE_TO_ADDRESSFENCE, intent);
                }
                break;
                case START_GOOGLE_MAP_FENCE_SCHOOL_ADDRESS: {
                    setSchoolAddressViewData(GuardianFenceActivity.SCHOOLFENCE_TO_ADDRESSFENCE, intent);
                }
                break;
            }
        }
    }

    private void startGuardianFenceActivity(Intent intent, boolean isHome, int startFlag) {
        Place place = PlaceAutocomplete.getPlace(this, intent);
        Log.i(TAG, "Place: " + place.getName());
        com.google.android.gms.maps.model.LatLng latLng = place.getLatLng();
        GuardianAddrInfo guardianAddrInfo = new GuardianAddrInfo();
        guardianAddrInfo.lat = latLng.latitude;
        guardianAddrInfo.lng = latLng.longitude;
        Intent guardianIntent = new Intent(this, GuardianFenceActivity.class);
        guardianIntent.putExtra(IS_GUARDIAN_HOME_ADDRESS, isHome);
        guardianIntent.putExtra(GuardianAddressActivity.SCHOOLADDRESS_TO_SCHOOLFENCE, guardianAddrInfo);
        startActivityForResult(guardianIntent, startFlag);
    }

    private void setViewTimeData(String flag, Intent intent) {
        schoolGuardianInfo = (SchoolGuardianInfo) intent.getSerializableExtra(flag);
        if (flag.equals(GUARDIAN_AM_TIME)) {
            long am_arrival_time = schoolGuardianInfo.getAm_arrival_time();
            long am_leave_time = schoolGuardianInfo.getAm_leave_time();

            Calendar calendarAM = Calendar.getInstance();
            calendarAM.setTimeInMillis(am_arrival_time * 1000L);
            int am_arrival_hour = calendarAM.get(Calendar.HOUR_OF_DAY);
            int am_arrival_min = calendarAM.get(Calendar.MINUTE);
            String am_startTime = String.format("%02d", am_arrival_hour) + ":" + String.format("%02d", am_arrival_min);

            calendarAM.setTimeInMillis(am_leave_time * 1000L);
            int am_leave_hour = calendarAM.get(Calendar.HOUR_OF_DAY);
            int am_leave_min = calendarAM.get(Calendar.MINUTE);
            String am_endTime = String.format("%02d", am_leave_hour) + ":" + String.format("%02d", am_leave_min);
            tv_am_time.setText(String.valueOf(am_startTime + "-" + am_endTime));
        } else if (flag.equals(GUARDIAN_PM_TIME)) {
            long pm_arrival_time = schoolGuardianInfo.getPm_arrival_time();
            long pm_leave_time = schoolGuardianInfo.getPm_leave_time();

            Calendar calendarPm = Calendar.getInstance();
            calendarPm.setTimeInMillis(pm_arrival_time * 1000L);
            int pm_arrival_hour = calendarPm.get(Calendar.HOUR_OF_DAY);
            int pm_arrival_min = calendarPm.get(Calendar.MINUTE);
            String pm_startTime = String.format("%02d", pm_arrival_hour) + ":" + String.format("%02d", pm_arrival_min);

            calendarPm.setTimeInMillis(pm_leave_time * 1000L);
            int leave_hour = calendarPm.get(Calendar.HOUR_OF_DAY);
            int leave_min = calendarPm.get(Calendar.MINUTE);
            String pm_endTime = String.format("%02d", leave_hour) + ":" + String.format("%02d", leave_min);
            tv_pm_time.setText(String.valueOf(pm_startTime + "-" + pm_endTime));
        }
    }

    private void setHomeAddressViewData(String flag, Intent intent) {
        GuardianAddrInfo guardianAddrInfo = (GuardianAddrInfo) intent.getSerializableExtra(flag);
        schoolGuardianInfo.setHome_address(guardianAddrInfo.address);
        schoolGuardianInfo.setHome_Latitude(guardianAddrInfo.lat);
        schoolGuardianInfo.setHome_LonTitude(guardianAddrInfo.lng);
        schoolGuardianInfo.setHome_radius(guardianAddrInfo.radius);
        tv_home_address.setText(guardianAddrInfo.address);

    }

    private void setSchoolAddressViewData(String flag, Intent intent) {
        GuardianAddrInfo guardianAddrInfo = (GuardianAddrInfo) intent.getSerializableExtra(flag);
        schoolGuardianInfo.setSchool_address(guardianAddrInfo.name);
        schoolGuardianInfo.setSchool_Latitude(guardianAddrInfo.lat);
        schoolGuardianInfo.setSchool_LonTitude(guardianAddrInfo.lng);
        schoolGuardianInfo.setSchool_radius(guardianAddrInfo.radius);
        tv_school_address.setText(guardianAddrInfo.name);
    }


    //修改/添加 上学守护
    public void modifySchoolGuardian() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(schoolGuardianInfo.getAm_leave_time() * 1000L);

        Calendar amEndCalendar = Calendar.getInstance();
        amEndCalendar.set(Calendar.SECOND, 0);
        amEndCalendar.set(Calendar.MILLISECOND, 0);
        amEndCalendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
        amEndCalendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));

        calendar.setTimeInMillis(schoolGuardianInfo.getPm_arrival_time() * 1000L);
        Calendar pmBeginCalendar = (Calendar) amEndCalendar.clone();
        pmBeginCalendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
        pmBeginCalendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));

        if (amEndCalendar.after(pmBeginCalendar)) {
            toast(R.string.school_guide_SchoolProNote3);
            return;
        }

        calendar.setTimeInMillis(schoolGuardianInfo.getPm_leave_time() * 1000L);
        Calendar pmEndCalendar = (Calendar) amEndCalendar.clone();
        pmEndCalendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
        pmEndCalendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));

        calendar.setTimeInMillis(schoolGuardianInfo.getLatest_arrival_home() * 1000L);
        Calendar toHomeCalendar = (Calendar) amEndCalendar.clone();
        toHomeCalendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
        toHomeCalendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));

        if (pmEndCalendar.after(toHomeCalendar)) {
            toast(R.string.school_guide_SchoolProNote4);
            return;
        }

        if (TextUtils.isEmpty(schoolGuardianInfo.getSchool_address())) {
            toast(R.string.school_guide_setting_school_address);
            return;
        }
        if (TextUtils.isEmpty(schoolGuardianInfo.getHome_address())) {
            toast(R.string.school_guide_setting_home_address);
            return;
        }


        popWaitingDialog(R.string.school_guide_put_setting);
        String devId = mDeviceId;
        if (TextUtils.isEmpty(devId))
            return;

        Message.SchoolGuard.Addr addrSchool = Message.SchoolGuard.Addr.newBuilder()
                .setAddr(schoolGuardianInfo.getSchool_address())
                .setFence(Message.Fence.Shape.newBuilder()
                        .setRound(Message.Fence.Shape.Round.newBuilder()
                                .setLatlon(Message.LatLon.newBuilder()
                                        .setLongitude(schoolGuardianInfo.getSchool_LonTitude())
                                        .setLatitude(schoolGuardianInfo.getSchool_Latitude())
                                        .build())
                                .setRadius(schoolGuardianInfo.getSchool_radius())
                                .build()
                        ).build()
                ).build();

        Message.SchoolGuard.Addr addrHome = Message.SchoolGuard.Addr.newBuilder()
                .setAddr(schoolGuardianInfo.getHome_address())
                .setFence(Message.Fence.Shape.newBuilder()
                        .setRound(Message.Fence.Shape.Round.newBuilder()
                                .setLatlon(Message.LatLon.newBuilder()
                                        .setLongitude(schoolGuardianInfo.getHome_LonTitude())
                                        .setLatitude(schoolGuardianInfo.getHome_Latitude())
                                        .build())
                                .setRadius(schoolGuardianInfo.getHome_radius())
                                .build()
                        ).build()
                ).build();

        Message.SchoolGuard.Period forenoon = Message.SchoolGuard.Period.newBuilder()
                .setStartTime(Message.TimePoint.newBuilder()
                        .setTime(schoolGuardianInfo.getAm_arrival_time())
                        .build())
                .setEndTime(Message.TimePoint.newBuilder()
                        .setTime(schoolGuardianInfo.getAm_leave_time())
                        .build())
                .build();

        Message.SchoolGuard.Period afternoon = Message.SchoolGuard.Period.newBuilder()
                .setStartTime(Message.TimePoint.newBuilder()
                        .setTime(schoolGuardianInfo.getPm_arrival_time())
                        .build())
                .setEndTime(Message.TimePoint.newBuilder()
                        .setTime(schoolGuardianInfo.getPm_leave_time())
                        .build())
                .build();

        Message.ModifySchoolGuardReqMsg reqMsg = Message.ModifySchoolGuardReqMsg.newBuilder()
                .setDeviceId(mDeviceId)
                .setGuard(Message.SchoolGuard.newBuilder()
//                        .setId(schoolGuardianInfo.getId())
                                .setSchool(addrSchool)
                                .setHome(addrHome)
                                .setForenoon(forenoon)
                                .setAfternoon(afternoon)
                                .setRepeat(schoolGuardianInfo.getRepeat())
                                .setGoHome(Message.TimePoint.newBuilder()
                                        .setTime(schoolGuardianInfo.getLatest_arrival_home()).build())
                                .setTimezone(Message.Timezone.newBuilder()
                                        .setZone(TimeZone.getDefault().getID())
                                        .build())
                                .setGuardDuringHolidays(schoolGuardianInfo.isIs_holiday())
                                .setEnable(schoolGuardianInfo.isIs_enable())
                ).build();
        model.modifyGuardianSchool(reqMsg, devId, new OperateDataListener() {
            @Override
            public void operateSuccess(GeneratedMessageV3 messageV3) {
                Message.ModifySchoolGuardRspMsg rspMsg = (Message.ModifySchoolGuardRspMsg) messageV3;

                dismissDialog();
                editable = false;
                RefreshNextImageView();
                Intent intent = new Intent();
                intent.putExtra(FenceListActivity.GUARDIAN_DATA, rspMsg.getGuard());
                setResult(RESULT_OK, intent);
                finish();

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
        });
    }

    private void showLatestTimeBackDialog(Context context) {
        final int[] editStartTime = {5, 57};
        View v = LayoutInflater.from(context).inflate(R.layout.guardian_latest_back_dialog, null);
        TextView title = v.findViewById(R.id.dialog_title);
        title.setText(R.string.last_back_guard_time);

        final AlertDialog dialogs = createAlertDialog(context, v);


        int currentStartTimeHour = 0;
        List<String> hourList = new ArrayList<>();
        String prefix = "0";
        for (int h = 0; h < 24; ++h) {
            if (h >= 10) {
                prefix = "";
            }
            hourList.add(prefix + h);
            if (h == lastBackHomeHour) {
                currentStartTimeHour = h;
                editStartTime[0] = h;
            }
        }
        NumberPickerBlue startHourNp = v.findViewById(R.id.start_time_hour);
        final String[] mDisplayedHour = hourList.toArray(new String[0]);
        setNumberPicker(startHourNp, mDisplayedHour, currentStartTimeHour).setOnValueChangedListener((picker, oldVal, newVal) -> editStartTime[0] = Integer.valueOf(mDisplayedHour[newVal]));
        int currentStartTimeMin = 0;
        prefix = "0";
        List<String> minList = new ArrayList<>();
        for (int m = 0; m < 60; m++) {
            if (m >= 10) {
                prefix = "";
            }
            minList.add(prefix + m);
            if (m == lastBackHomeMinute) {
                currentStartTimeMin = m;
                editStartTime[1] = m;
            }
        }
        final String[] mDisplayedMinValues = minList.toArray(new String[0]);
        NumberPickerBlue startMinNp = v.findViewById(R.id.start_time_min);
        setNumberPicker(startMinNp, mDisplayedMinValues, currentStartTimeMin).setOnValueChangedListener((picker, oldVal, newVal) -> editStartTime[1] = Integer.valueOf(mDisplayedMinValues[newVal]));

        v.findViewById(R.id.ok).setOnClickListener(v1 -> {
            lastBackHomeMinute = editStartTime[1];
            lastBackHomeHour = editStartTime[0];
            schoolGuardianInfo.setLatest_arrival_home(DateUtil.getTimeAtHM(lastBackHomeHour, lastBackHomeMinute));
            String arrivalTime = String.format("%02d:%02d", lastBackHomeHour, lastBackHomeMinute);
            tv_latest_go_home_time.setText(arrivalTime);
            dialogs.dismiss();
        });
        v.findViewById(R.id.cancel).setOnClickListener(v12 -> dialogs.dismiss());
        dialogs.show();
    }
}
