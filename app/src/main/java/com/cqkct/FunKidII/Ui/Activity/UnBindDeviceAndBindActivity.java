package com.cqkct.FunKidII.Ui.Activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.QRCodeUtils;
import com.cqkct.FunKidII.Utils.StringUtils;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.NotYetLoginException;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.service.tlc.WaitThirdStageTimeoutException;
import com.google.zxing.WriterException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.nio.channels.NotYetConnectedException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

/**
 * Created by justin on 2017/10/26.
 */

public class UnBindDeviceAndBindActivity extends BaseActivity {
    public static final String TAG = UnBindDeviceAndBindActivity.class.getSimpleName();

    private String deviceId;
    private ImageView bind_unbind_qrc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unbind_bind_device);
        setTitleBarTitle(R.string.device_unbind);
        deviceId = mDeviceId;
        init();
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.bt_bd: {
                assert mCurrentBabyBean != null;

                String babyName = !StringUtils.isEmpty(mCurrentBabyBean.getName()) ? mCurrentBabyBean.getName() : getString(R.string.baby);
                ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                        .setMessage(getString( R.string.ask_really_unbind, babyName))
                        .setPositiveButton(getString(R.string.unbind_short), (dialog, which) -> unbindDevice(deviceId))
                        .setNegativeButton(getString(R.string.cancel), null);
                dialogFragment.show(getSupportFragmentManager(), "UnbindDeviceDialog");
                break;
            }
            default:
                break;
        }
    }


    private void init() {
        TextView tv_deviceId = findViewById(R.id.tv_bd_deviceId);
        tv_deviceId.setText(String.valueOf(getString(R.string.unbind_and_bind_device_ID) + deviceId));
        bind_unbind_qrc = findViewById(R.id.bd_qrc);
        ViewTreeObserver vto2 = bind_unbind_qrc.getViewTreeObserver();
        vto2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                bind_unbind_qrc.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mTaskHandler.sendEmptyMessage(TaskHandler.UPDATE_QR_IMG);
            }
        });
    }

    private void genQR() {
        try {
            int w = bind_unbind_qrc.getWidth();
            int h = bind_unbind_qrc.getHeight();
            if (w < 1 || h < 1)
                return;
            String encodedDeviceId;
            try {
                encodedDeviceId = URLEncoder.encode(deviceId,   "utf-8");
            } catch (UnsupportedEncodingException e) {
                L.e(TAG, "URLEncoder.encode(" + deviceId + ")", e);
                encodedDeviceId = deviceId;
            }
            Bitmap bitmap = QRCodeUtils.createCode(this,
                    "https://app.cqkct.com/funkidii?bindnum=" + encodedDeviceId, w, h);
            bind_unbind_qrc.setImageBitmap(bitmap);
        } catch (WriterException e) {
            L.e(TAG, "getQrc failure", e);
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
            UnbindDeviceDialogFragment.newInstance(deviceId).show(getSupportFragmentManager(), "UnbindDeviceDialogFragment");
        } else {
            doUnbindDevice(deviceId);
        }
    }

    public static class UnbindDeviceDialogFragment extends DialogFragment {
        String deviceId;

        public static UnbindDeviceDialogFragment newInstance(@NonNull String deviceId) {
            UnbindDeviceDialogFragment newFragment = new UnbindDeviceDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString("deviceId", deviceId);
            newFragment.setArguments(bundle);
            return newFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            if (args != null) {
                deviceId = args.getString("deviceId");
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.unbind)
                    .setMessage(R.string.unbind_owner_device_tip)
                    .setPositiveButton(R.string.unbind_short, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            UnBindDeviceAndBindActivity activity = (UnBindDeviceAndBindActivity) getActivity();
                            activity.doUnbindDevice(deviceId);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null);
            return builder.create();
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
                        // never to here for this exec
                    }
                }
        );
    }

    private TaskHandler mTaskHandler = new TaskHandler(this);

    private static class TaskHandler extends Handler {
        static final int UPDATE_QR_IMG = 0;

        private WeakReference<UnBindDeviceAndBindActivity> mA;
        TaskHandler(UnBindDeviceAndBindActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            UnBindDeviceAndBindActivity a = mA.get();
            if (a == null)
                return;

            switch (msg.what) {
                case UPDATE_QR_IMG:
                    a.genQR();
                    break;
            }
        }
    }

}
