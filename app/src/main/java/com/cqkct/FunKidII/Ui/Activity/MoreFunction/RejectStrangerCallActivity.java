package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

/**
 * Created by justin on 2017/9/13.
 */

public class RejectStrangerCallActivity extends BaseActivity {
    public static final String TAG = RejectStrangerCallActivity.class.getSimpleName();
    private SwitchCompat ib_call_in, ib_call_out;
    private boolean mRejectStrangerCallIn = false;
    private boolean mRejectStrangerCallOut = false;
    private boolean hasEditPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.refuse_stranger_call);
        setTitleBarTitle(R.string.refuse_stranger_call);
        if (mDeviceId == null) {
            L.w(TAG, "mDeviceId is null");
            finish();
            return;
        }
        hasEditPermission = hasEditPermission();
        init();
        getData();
    }

    private void getData() {
        getDataQueryDB();
        getDataQueryServer();
    }


    private void init() {
        TextView refuseStrangerInText = findViewById(R.id.refuse_stranger_in_text);
        refuseStrangerInText.setSelected(true);
        TextView refuseStrangerOutText = findViewById(R.id.refuse_stranger_out_text);
        refuseStrangerOutText.setSelected(true);
        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
        long func_module = 0;
        if (deviceInfo != null) {
            func_module = deviceInfo.getDeviceEntity().getFuncModuleInfo().getFuncModule();
        }
        findViewById(R.id.relativeLayout7).setVisibility((func_module & Message.FuncModule.FM_REJECT_STRANGER_CALL_OUT_VALUE) != 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.more_imp_refuse_stranger_).setVisibility((func_module & Message.FuncModule.FM_REJECT_STRANGER_CALL_IN_VALUE) != 0 ? View.VISIBLE : View.GONE);

        /*拒接陌生人*/
        ib_call_in = findViewById(R.id.reject_stranger_call_in);
        ib_call_in.setChecked(mRejectStrangerCallIn);
        if (hasEditPermission) {
            ib_call_in.setEnabled(true);
            ib_call_in.setOnCheckedChangeListener(callInOnCheckedChangeListener);
        } else {
            ib_call_in.setEnabled(false);
        }

        /*拒呼陌生人*/
        ib_call_out = findViewById(R.id.reject_stranger_call_out);
        ib_call_out.setChecked(mRejectStrangerCallOut);
        if (hasEditPermission) {
            ib_call_out.setEnabled(true);
            ib_call_out.setOnCheckedChangeListener(callOutOnCheckedChangeListener);
        } else {
            ib_call_out.setEnabled(false);
        }
    }

    /**
     * @param rejectCallIn ：拒接 ：true 拒呼 ：false
     */
    private boolean setFunctions(final boolean rejectCallIn) {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "setFunctions: deviceId is empty");
            return false;
        }

        popWaitingDialog(R.string.please_wait);

        Message.PushDevConfReqMsg.Builder builder = Message.PushDevConfReqMsg.newBuilder();
        builder.setDeviceId(deviceId);
        builder.setFlag(Message.DevConfFlag.DCF_FUNCTIONS_VALUE);

        Message.Functions.Builder funcBuilder = Message.Functions.newBuilder();
        if (rejectCallIn) {
            funcBuilder.setRejectStrangerCallIn(!mRejectStrangerCallIn)
            .setChangedField(Message.Functions.FieldFlag.REJECT_STRANGER_CALL_IN_VALUE);
        } else {
            funcBuilder.setRejectStrangerCallOut(!mRejectStrangerCallOut)
            .setChangedField(Message.Functions.FieldFlag.REJECT_STRANGER_CALL_OUT_VALUE);
        }

        builder.setConf(Message.DevConf.newBuilder().setFuncs(funcBuilder));


        L.v("RefuseStrangerCall setFunctionsBuilder" + builder);

        final Message.PushDevConfReqMsg reqMsg = builder.build();

        exec(reqMsg,

                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.PushDevConfRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                    L.d(TAG, "saving reject stranger success");
                                    GreenUtils.saveConfigsAsync(reqMsg.getConf(), reqMsg.getFlag(), deviceId);
                                    if (rejectCallIn) {
                                        mRejectStrangerCallIn = reqMsg.getConf().getFuncs().getRejectStrangerCallIn();
                                    } else {
                                        mRejectStrangerCallOut = reqMsg.getConf().getFuncs().getRejectStrangerCallOut();
                                    }
                                    setView(rejectCallIn);
                                    dismissDialog();
                                    return false;
                                default:
                                    break;
                            }
                            L.w(TAG, "setFunctions() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "setFunctions() -> exec() -> onResponse() process failure", e);
                        }
                        setView(rejectCallIn);
                        popErrorDialog(getString(R.string.setup_failed));
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "setFunctions() -> exec() -> onException()", cause);
                        setView(rejectCallIn);
                        popErrorDialog(R.string.setup_failed);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                });

        return true;
    }

    private void getDataQueryServer() {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "getDataQueryServer: deviceId is empty");
            return;
        }

        popWaitingDialog(R.string.loading);

        exec(
                Message.FetchDevConfReqMsg.newBuilder()
                        .setDeviceId(deviceId)
                        .setFlag(Message.DevConfFlag.DCF_FUNCTIONS_VALUE)
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.FetchDevConfRspMsg rspMsg = response.getProtoBufMsg();
                            L.e(TAG, "RefuseStrangerCall getDataServer: " + rspMsg);
                            if (Message.ErrorCode.SUCCESS == rspMsg.getErrCode()) {
                                dismissDialog();
                                if (rspMsg.getFlag() == Message.DevConfFlag.DCF_FUNCTIONS_VALUE) {
                                    Message.DevConf configs = rspMsg.getConf();
                                    GreenUtils.saveConfigs(configs, rspMsg.getFlag(), deviceId);

                                    if (configs.getFuncs() != null)
                                        initViewData(configs.getFuncs());
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        popErrorDialog(cause instanceof TimeoutException ? R.string.load_timeout : R.string.load_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }
        );
    }

    private void getDataQueryDB() {
        List<DeviceEntity> list = GreenUtils.getDeviceEntityDao().queryBuilder()
                .where(DeviceEntityDao.Properties.DeviceId.eq(mDeviceId)).build().list();
        if (!list.isEmpty()) {
            DeviceEntity deviceEntity = list.get(0);
            Message.Functions functions = deviceEntity.getFunctions();
            initViewData(functions);
        }
    }

    private void initViewData(Message.Functions func) {
        mRejectStrangerCallIn = func.getRejectStrangerCallIn();
        mRejectStrangerCallOut = func.getRejectStrangerCallOut();
        setView(true);
        setView(false);
    }

    private void setView(boolean rejectCallIn) {
        if (rejectCallIn) {
            ib_call_in.setOnCheckedChangeListener(null);
            ib_call_in.setChecked(mRejectStrangerCallIn);
            if (hasEditPermission) {
                ib_call_in.setOnCheckedChangeListener(callInOnCheckedChangeListener);
                ib_call_in.setEnabled(true);
            }
        } else {
            ib_call_out.setOnCheckedChangeListener(null);
            ib_call_out.setChecked(mRejectStrangerCallOut);
            if (hasEditPermission) {
                ib_call_out.setOnCheckedChangeListener(callOutOnCheckedChangeListener);
                ib_call_out.setEnabled(true);
            }
        }
    }

    private CompoundButton.OnCheckedChangeListener callInOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked != mRejectStrangerCallIn) {
                ib_call_in.setOnCheckedChangeListener(null);
                ib_call_in.setEnabled(false);
                ib_call_in.setChecked(!isChecked);
                if (!setFunctions(true)) {
                    setView(true);
                }
            }
        }
    };

    private CompoundButton.OnCheckedChangeListener callOutOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked != mRejectStrangerCallOut) {
                ib_call_out.setOnCheckedChangeListener(null);
                ib_call_out.setEnabled(false);
                ib_call_out.setChecked(!isChecked);
                if (!setFunctions(false)) {
                    setView(false);
                }
            }
        }
    };

    @Override
    public void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo) {
        if (deviceInfo.getDeviceId().equals(mDeviceId)) {
            if (deviceInfo.getDeviceEntity().getFunctions().getRejectStrangerCallIn() != mRejectStrangerCallIn) {
                mRejectStrangerCallIn = deviceInfo.getDeviceEntity().getFunctions().getRejectStrangerCallIn();
                setView(true);
            }
            if (deviceInfo.getDeviceEntity().getFunctions().getRejectStrangerCallOut() != mRejectStrangerCallOut) {
                mRejectStrangerCallOut = deviceInfo.getDeviceEntity().getFunctions().getRejectStrangerCallOut();
                setView(false);
            }
        }
    }
}
