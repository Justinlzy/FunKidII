package com.cqkct.FunKidII.Ui.Model;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Listener.OperateDataListener;
import com.cqkct.FunKidII.Ui.fragment.MoreFragment;
import com.cqkct.FunKidII.Ui.view.MaskableFrameLayout;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.PhoneNumberInputFilter;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.db.Entity.UserEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;
import uk.co.senab.photoview.PhotoView;

public class MoreModel {

    public static final String TAG = MoreModel.class.getSimpleName();
    private WeakReference<MoreFragment> f;
    private Context mContext;

    public MoreModel(MoreFragment fragment, Context context) {
        this.f = new WeakReference<>(fragment);
        this.mContext = context;
    }


    public List<BabyEntity> getClipViewPagerData() {
        List<BabyEntity> babyDataList = new ArrayList<>();

        String userId = f.get().mUserId;
        if (TextUtils.isEmpty(userId)) {
            L.w(TAG, "refreshClipViewPagerData userId is isEmpty");
            return null;
        }
        List<BabyEntity> list = GreenUtils.getBabyEntityDao().queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(userId))
                .build().list();

        for (int i = 0; i < list.size() && i < 8; ++i) {
            BabyEntity babyEntity = list.get(i);
            babyDataList.add(babyEntity);
        }
        if (babyDataList.size() >= 8) {
            babyDataList.add(new BabyEntity());
        }
        return babyDataList;
    }

    public void sendTakePhoto(OperateDataListener listener) {
        String deviceId = f.get().mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "sendTakePhoto: deviceId is empty");
            return;
        }

        long reqTimeout = 1000L * 60;
        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId);
        if (deviceInfo != null) {
            int sec = deviceInfo.getDeviceEntity().getFuncModuleInfo().getFuncTimeoutDef().getSecOfTakePhoto();
            if (sec > 0) {
                reqTimeout = sec * 1000L;
            }
        }
        f.get().exec(
                Message.TakePhotoS1ReqMsg.newBuilder()
                        .setDeviceId(deviceId).build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.TakePhotoS1RspMsg takePhotoS1RspMsg = response.getProtoBufMsg();
                            if (takePhotoS1RspMsg.getErrCode() != Message.ErrorCode.SUCCESS) {
                                listener.operateFailure(takePhotoS1RspMsg.getErrCode());
                            }else {
                                return true;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "onException");
                        listener.operateFailure(cause instanceof NotYetConnectedException ? Message.ErrorCode.TIMEOUT : Message.ErrorCode.FAILURE);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        try {
                            Message.TakePhotoS3ReqMsg takePhotoS3ReqMsg = thirdStageRequest.getProtoBufMsg();
                            L.d(TAG, "TakePhotoS3ReqMsg: " + takePhotoS3ReqMsg);
                            Message.ErrorCode result = takePhotoS3ReqMsg.getErrCode();
                            if (result == Message.ErrorCode.SUCCESS) {
                                listener.operateSuccess(takePhotoS3ReqMsg);
                                L.i(TAG, "TakePhoto TakePhotoS3ReqMsg success");
                            } else {
                                listener.operateFailure(Message.ErrorCode.FAILURE);
                            }

                            Message.TakePhotoS3RspMsg takePhotoS3RspMsg = Message.TakePhotoS3RspMsg.newBuilder()
                                    .setErrCode(result)
                                    .build();
                            responseSetter.setResponse(takePhotoS3RspMsg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, reqTimeout);
    }


    public void showPhotoDialog(File file, Context context) {
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        Dialog dialog = new Dialog(mContext);
        View mView = LayoutInflater.from(mContext).inflate(R.layout.more_take_picture, null);
        MaskableFrameLayout mMaskableFrameLayout = mView.findViewById(R.id.frm_mask_animated);
        TextView bt_close = mView.findViewById(R.id.ib_close);
        PhotoView imageView = mView.findViewById(R.id.iv_more_take_picture);
        imageView.setImageBitmap(bitmap);
        bt_close.setOnClickListener(view -> dialog.dismiss());
        dialog.setContentView(mView);
        dialog.setCancelable(false);
        dialog.show();
        mView.findViewById(R.id.ib_save).setOnClickListener(v -> {
            //让系统扫描本地监拍图片
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            Toast.makeText(mContext, R.string.save_img_local, Toast.LENGTH_SHORT).show();
            animate(mMaskableFrameLayout);
            new android.os.Handler().postDelayed(dialog::dismiss,1500);

        });

        //确定
        Window window = dialog.getWindow();
        assert window != null;
        window.setWindowAnimations(R.style.windowAnimation);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

    }

    private void animate(MaskableFrameLayout mMaskableFrameLayout) {
        Drawable drawable = mMaskableFrameLayout.getDrawableMask();
        if (drawable instanceof AnimationDrawable) {
            AnimationDrawable animDrawable = (AnimationDrawable) drawable;
            animDrawable.selectDrawable(0);
            animDrawable.stop();
            animDrawable.start();
        }
    }

    public void getUserPhoneToListen(String userId, String deviceId, OperateDataListener listener, List<UserEntity> userEntityList) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "getUserPhoneToListen: userId or deviceId is empty");
            return;
        }

        if (userEntityList.isEmpty()) {
            f.get().exec(Message.FetchUserInfoReqMsg.newBuilder().setUserId(userId).build(), new TlcService.OnExecListener() {
                @Override
                public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {

                    try {
                        Message.FetchUserInfoRspMsg rspMsg = response.getProtoBufMsg();
                        if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                            listener.operateSuccess(rspMsg);

                            return false;
                        } else {
                            L.w(TAG, "getUserPhoneToListen onResponse: " + rspMsg.getErrCode());
                        }
                    } catch (Exception e) {
                        L.e(TAG, "getUserPhoneToListen onResponse", e);
                    }
                    listener.operateFailure(Message.ErrorCode.FAILURE);
                    return false;
                }

                @Override
                public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                    L.e(TAG, "getUserPhoneToListen onException", cause);
                    f.get().dismissDialog();
                }

                @Override
                public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                }
            });
        } else {
            listener.operateFailure(Message.ErrorCode.INVALID_PARAM);
        }
    }

    public void listenDevice(final String userId, final String deviceId, String phone) {
        if (phone == null) {
            phone = "";
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.listen_call_edit_phone_number, null);
        Dialog dialog = f.get().createDialog(mContext, view);
        dialog.show();

        TextView title = view.findViewById(R.id.dialog_title);
        title.setText(R.string.the_number_of_to_listen);

        final EditText numberEditText = view.findViewById(R.id.number);
        numberEditText.setText(phone);
        numberEditText.setSelection(numberEditText.length());
        InputFilter phoneNumberInputFilter = new PhoneNumberInputFilter(f.get().getResources().getInteger(R.integer.maxLength_of_phone_number));
        numberEditText.setFilters(new InputFilter[]{phoneNumberInputFilter});
        view.findViewById(R.id.ok).setOnClickListener(v -> {
            String num = numberEditText.getText().toString().trim();
            if (TextUtils.isEmpty(num)) {
                f.get().toast(R.string.phonenumber_can_not_be_null);
                return;
            }
//                        if (!PublicTools.isValidMobileNo(num.replace("+86", ""))) {
//                            toast(R.string.more_find_number_formal_error);
//                            return;
//                        }
            sendListenDevice(userId, deviceId, num);
            dialog.dismiss();
        });
        view.findViewById(R.id.cancel).setOnClickListener(v -> dialog.dismiss());
    }

    public void sendFindDevice() {
        String deviceId = f.get().mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "sendFindDevice: deviceId is empty");
            return;
        }

        f.get().popWaitingDialog(R.string.looking_watch);

        long reqTimeout = 20 * 1000L;
        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId);
        if (deviceInfo != null) {
            int sec = deviceInfo.getDeviceEntity().getFuncModuleInfo().getFuncTimeoutDef().getSecOfFindDevice();
            if (sec > 0) {
                reqTimeout = sec * 1000L;
            }
        }
        f.get().exec(
                Message.FindDeviceS1ReqMsg.newBuilder()
                        .setDeviceId(deviceId)
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.FindDeviceS1RspMsg findDeviceS1RspMsg = response.getProtoBufMsg();
                            switch (findDeviceS1RspMsg.getErrCode()) {
                                case SUCCESS:
                                    L.e(TAG, "sendFindDevice RESULT_CODE_SUCCESS S1");
                                    return true;
                                case FAILURE:
                                    L.e(TAG, "sendFindDevice RESULT_CODE_FAILURE S1");
                                    f.get().popErrorDialog(R.string.more_find_failure);
                                    return false;
                                case NO_DEVICE:
                                    f.get().popErrorDialog(f.get().getString(R.string.device_does_not_exist));
                                    return false;
                                case OFFLINE:
                                    f.get().popErrorDialog(f.get().getString(R.string.watch_not_net));
                                    return false;
                                case INVALID_PARAM:
                                    f.get().popErrorDialog(f.get().getString(R.string.invalid_parameter));
                                    return false;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "setFunctionsS1() -> exec() -> onException()", cause);
                        if (cause instanceof TimeoutException) {
                            f.get().popErrorDialog(R.string.request_timed_out);
                        } else {
                            f.get().popErrorDialog(R.string.more_find_failure);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        try {
                            Message.FindDeviceS3ReqMsg findDeviceS3ReqMsg = thirdStageRequest.getProtoBufMsg();
                            final Message.ErrorCode result = findDeviceS3ReqMsg.getErrCode();
                            if (result == Message.ErrorCode.SUCCESS) {
                                f.get().popSuccessDialog(R.string.more_find_suc);
                            } else {
                                f.get().popErrorDialog(R.string.more_find_watch_failure);
                            }
                            Message.FindDeviceS3RspMsg findDeviceS3RspMsg = Message.FindDeviceS3RspMsg.newBuilder()
                                    .setErrCode(Message.ErrorCode.SUCCESS)
                                    .build();
                            responseSetter.setResponse(findDeviceS3RspMsg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, reqTimeout);
    }

    private void sendListenDevice(String userId, final String deviceId, final String number) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(number)) {
            L.w(TAG, "listenDevice: userId or deviceId or number is empty");
            return;
        }

        f.get().popWaitingDialog(R.string.one_way_calling);

        long reqTimeout = 20 * 1000L;
        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId);
        if (deviceInfo != null) {
            int sec = deviceInfo.getDeviceEntity().getFuncModuleInfo().getFuncTimeoutDef().getSecOfSimplexCall();
            if (sec > 0) {
                reqTimeout = sec * 1000L;
            }
        }
        f.get().exec(
                Message.SimplexCallS1ReqMsg.newBuilder().setDeviceId(deviceId).setPhoneNum(number).build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.SimplexCallS1RspMsg ld = response.getProtoBufMsg();
                            switch (ld.getErrCode()) {
                                case SUCCESS:
                                    L.e(TAG, "sendListenDevice SUCCESS S1");
                                    f.get().simplexCallRequstTimeMap.put(deviceId, System.currentTimeMillis());
                                    return true;
                                case NO_DEVICE:
                                    f.get().popErrorDialog(R.string.device_does_not_exist);
                                    return false;
                                case OFFLINE:
                                    f.get().popErrorDialog(R.string.watch_not_online);
                                    return false;
                                default:
                                    L.w(TAG, "sendListenDevice failure: " + ld.getErrCode());
                                    break;

                            }
                        } catch (Exception e) {
                            L.e(TAG, "sendListenDevice", e);
                        }
                        f.get().popErrorDialog(R.string.one_way_call_failed);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, " onException: " + cause);
                        if (cause instanceof TimeoutException) {
                            f.get().popErrorDialog(R.string.one_way_call_timeout);
                        } else {
                            f.get().popErrorDialog(R.string.one_way_call_failed);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        responseSetter.setResponse(Message.TakePhotoS3RspMsg.newBuilder().setErrCode(Message.ErrorCode.SUCCESS).build());
                        try {
                            Message.SimplexCallS3ReqMsg listenDeviceS3ReqMsg = thirdStageRequest.getProtoBufMsg();
                            L.v(TAG, "sendListenDevice: " + listenDeviceS3ReqMsg);
                            Message.ErrorCode result = listenDeviceS3ReqMsg.getErrCode();
                            if (result == Message.ErrorCode.SUCCESS) {
                                f.get().popSuccessDialog(R.string.one_way_call_success);
                                return;
                            }
                        } catch (Exception e) {
                            L.e(TAG, "sendListenDevice", e);
                        }
                        f.get().popErrorDialog(R.string.one_way_call_failed);
                    }
                }, reqTimeout);
    }
}
//
//interface DownloadAvatarListener {
//    void refreshView();
//}

