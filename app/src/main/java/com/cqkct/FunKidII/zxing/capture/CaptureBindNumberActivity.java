package com.cqkct.FunKidII.zxing.capture;

import android.Manifest;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.Activity.InputBindNumberActivity;
import com.cqkct.FunKidII.Ui.Activity.SelectRelationActivity;
import com.cqkct.FunKidII.Ui.Activity.WaitDeviceBindSuccessActivity;
import com.cqkct.FunKidII.Utils.AndroidPermissions;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.Rom;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.svprogresshub.SVProgressHUB;
import com.cqkct.FunKidII.svprogresshub.listener.OnDismissListener;
import com.cqkct.FunKidII.ucrop.UCropActivity;
import com.cqkct.FunKidII.zxing.camera.CameraManager;
import com.cqkct.FunKidII.zxing.decoding.CaptureActivityHandler;
import com.cqkct.FunKidII.zxing.decoding.InactivityTimer;
import com.cqkct.FunKidII.zxing.view.ViewfinderView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import protocol.Message;

public class CaptureBindNumberActivity extends BaseActivity implements Callback {
    private static final String TAG = CaptureBindNumberActivity.class.getSimpleName();

    /**
     * 工作模式
     */
    public static final String PARAM_KEY_MODE = "mode";
    /**
     * 工作模式：扫描得到绑定号
     */
    public static final int PARAM_VALUE_MODE_GET_BIND_NUM = 0;
    /**
     * 工作模式：扫描绑定号并绑定设备
     */
    public static final int PARAM_VALUE_MODE_BIND_DEVICE = 1;
    /**
     * 想要显示的 Activity title
     */
    public static final String PARAM_KEY_WINDOW_TITLE = "title";

    public static final String RESULT_ACTION = "com.cqkct.FunKidII.BIND_NUM";
    /**
     * 与 PARAM_KEY_MODE 取值一致
     */
    public static final String RESULT_KEY_MODE = "mode";
    /**
     * 绑定号
     */
    public static final String RESULT_KEY_BIND_NUM = "bind_num";
    /**
     * 绑定设备状态：mode 为 PARAM_VALUE_MODE_BIND_DEVICE 时有效
     */
    public static final String RESULT_KEY_BIND_STATUS = "bind_status";
    /**
     * 绑定状态：绑定请求已成功发送到管理员
     */
    public static final int RESULT_VALUE_BIND_STATUS_WAIT = 0;
    /**
     * 绑定状态：绑定成功
     */
    public static final int RESULT_VALUE_BIND_STATUS_OK = 1;
    /**
     * 绑定状态：该设备已经绑定
     */
    public static final int RESULT_VALUE_BIND_STATUS_ALREADY_BOUND = 2;


    private static final int ACTIVITY_REQUEST_MANUAL_INPUT_BIND_NUM = 0;
    private static final int ACTIVITY_REQUEST_SELECT_RELATION_AND_DO_BIND = 1;
    private static final int ACTIVITY_REQUEST_WAIT_BIND_RESULT = 2;

    private static final int ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_CAMERA_PERMISSION = 1000;

    private static final int PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;

    private int workMode = PARAM_VALUE_MODE_GET_BIND_NUM;

    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private SurfaceView surfaceView;
    private boolean surfaceCreated;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private AudioManager mAudioService;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    // private static final float BEEP_VOLUME = 0.10f;
    private boolean vibrate;
    CameraManager cameraManager;
    private Boolean hasCameraPermission;
    private boolean shouldPreviewCamera = true; // 标记是否需要初始化摄像头，启动二维码扫描
    public static CaptureBindNumberActivity ActivityCapture;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        /*
         * this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
         * WindowManager.LayoutParams.FLAG_FULLSCREEN);// 去掉信息栏
         *
         * RelativeLayout layout = new RelativeLayout(this);
         * layout.setLayoutParams(new
         * ViewGroup.LayoutParams(LayoutParams.FILL_PARENT,
         * LayoutParams.FILL_PARENT));
         *
         * this.surfaceView = new SurfaceView(this); this.surfaceView
         * .setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT,
         * LayoutParams.FILL_PARENT));
         *
         * layout.addView(this.surfaceView);
         *
         * this.viewfinderView = new ViewfinderView(this);
         * this.viewfinderView.setBackgroundColor(0x00000000);
         * this.viewfinderView.setLayoutParams(new
         * ViewGroup.LayoutParams(LayoutParams.FILL_PARENT,
         * LayoutParams.FILL_PARENT)); layout.addView(this.viewfinderView);
         *
         * TextView status = new TextView(this); RelativeLayout.LayoutParams
         * params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
         * LayoutParams.WRAP_CONTENT);
         * params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
         * params.addRule(RelativeLayout.CENTER_HORIZONTAL);
         * status.setLayoutParams(params);
         * status.setBackgroundColor(0x00000000);
         * status.setTextColor(0xFFFFFFFF); status.setText("请将条码置于取景框内扫描。");
         * status.setTextSize(14.0f);
         *
         * layout.addView(status); setContentView(layout);
         */
        ActivityCapture = this;
        setContentView(R.layout.activity_capture_get_device_id);
        setTitleBarTitle(R.string.bind_device_scan_qr_code);
        setTitleBarOkBtnText(R.string.album);
        setTitleBarOkBtnVisibility(View.VISIBLE);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        surfaceView.getHolder().addCallback(this);
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinderview);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        surfaceCreated = false;
        inactivityTimer = new InactivityTimer(this);

        processParams();

        mAudioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        playBeep = !(mAudioService != null && mAudioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL);
        initBeepSound();
        vibrate = true;

        hasCameraPermission = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        // 相机权限
        if (hasCameraPermission == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                hasCameraPermission = true;
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    hasCameraPermission = false;
                    if (AndroidPermissions.shouldShowGuide(this, Manifest.permission.CAMERA)) {
                        showCameraPermissionGuide();
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
                    }
                } else if (Rom.isFlyme()) {
                    if (AndroidPermissions.isCameraUsable()) {
                        hasCameraPermission = true;
                    } else {
                        hasCameraPermission = false;
                        showCameraPermissionGuide();
                    }
                } else {
                    hasCameraPermission = true;
                }
            }
        } else {
            hasCameraPermission = AndroidPermissions.hasCameraPermission(this);
        }
        if (hasCameraPermission && shouldPreviewCamera) {
            cameraManager = new CameraManager(this);
            viewfinderView.setCameraManager(cameraManager);
            if (surfaceCreated) {
                initCamera(surfaceView.getHolder());
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        if (cameraManager != null) {
            cameraManager.closeDriver();
            cameraManager = null;
        }
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        ActivityCapture = null;
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA:
                // If request is cancelled, the result arrays are empty.
                if (Rom.isFlyme()) {
                    // 使其在 onResume 里重新检查权限
                    hasCameraPermission = null;
                } else {
                    hasCameraPermission = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (shouldPreviewCamera && !hasCameraPermission) {
                        toast(R.string.no_camera_permission);
                    }
                }
                break;
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    toast(R.string.no_album_permission);
                } else {
                    photo();
                }
                break;
            default:
                break;
        }
    }

    private void showCameraPermissionGuide() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.camera_permission)
                .setMessage(R.string.please_enable_camera_permission_in_setting)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = AndroidPermissions.permissionSettingPageIntent(CaptureBindNumberActivity.this);
                        startActivityForResult(intent, ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_CAMERA_PERMISSION);
                    }
                })
                .show();
    }

    private void processParams() {
        Intent intent = getIntent();
        if (intent == null)
            return;
        workMode = intent.getIntExtra(PARAM_KEY_MODE, PARAM_VALUE_MODE_GET_BIND_NUM);
        String title = intent.getStringExtra(PARAM_KEY_WINDOW_TITLE);
        if (!TextUtils.isEmpty(title)) {
            setTitleBarTitle(title);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (cameraManager == null)
            return;
        try {
            // CameraManager.get().openDriver(surfaceHolder);
            cameraManager.openDriver(surfaceHolder);
        } catch (Exception ioe) {
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(CaptureBindNumberActivity.this, decodeFormats, characterSet);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceCreated = true;
        initCamera(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceCreated = false;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    public void handleDecode(Result obj, Bitmap barcode) {
        L.v(TAG, "handleDecode: " + obj.getText());
        inactivityTimer.onActivity();
        playBeepSoundAndVibrate();
        //showResult(obj, barcode);
        sendResult(obj, barcode);
    }

    public static String getBindNumFromUri(String uriStr) {
        if (TextUtils.isEmpty(uriStr))
            return null;

        if (uriStr.startsWith("http://") || uriStr.startsWith("https://") || uriStr.startsWith("funkidii://")) {
            try {
                URI uri = new URI(uriStr);
                String host = uri.getHost();
                if (TextUtils.isEmpty(host) || !host.toLowerCase().equals("app.cqkct.com")) {
                    L.v(TAG, "getBindNumFromUri failure: invalid host");
                    return null;
                }
                String path = uri.getPath();
                if (TextUtils.isEmpty(path)) {
                    L.v(TAG, "getBindNumFromUri failure: invalid path");
                    return null;
                }
                path = path.toLowerCase();
                if (!(path.equals("/funkidii/") || path.equals("funkidii") || path.equals("/funkidii") || path.equals("funkidii/"))) {
                    L.v(TAG, "getBindNumFromUri failure: invalid path");
                    return null;
                }
                String query = uri.getRawQuery();
                if (TextUtils.isEmpty(query)) {
                    L.v(TAG, "getBindNumFromUri failure: invalid query");
                    return null;
                }
                String[] params = query.split("&");
                Map<String, String> paramMap = new HashMap<>();
                for (String param : params) {
                    int idx = param.indexOf('=');
                    if (idx < 0) {
                        paramMap.put(URLDecoder.decode(param, "UTF-8").toLowerCase(), "");
                    } else if (idx > 0) {
                        paramMap.put(URLDecoder.decode(param.substring(0, idx), "UTF-8").toLowerCase(),
                                URLDecoder.decode(param.substring(idx + 1), "UTF-8"));
                    } else {
                        // just have value??? no key???
                    }
                }
                return paramMap.get("bindnum");
            } catch (Exception e) {
                L.d(TAG, "getBindNumFromUri failure", e);
                return null;
            }
        } else {
            return uriStr;
        }
    }

    private void sendResult(final Result rawResult, Bitmap barcode) {
        String qrRes = rawResult.getText();
        String bindNum = getBindNumFromUri(qrRes);

        if (TextUtils.isEmpty(bindNum)) {
            popErrorDialog(R.string.tip_invalid_device_id, new OnDismissListener() {
                @Override
                public void onDismiss(SVProgressHUB hud) {
                    restartPreviewAfterDelay(0);
                }
            });
            return;
        }

        L.v(TAG, "绑定号：" + bindNum);
        popWaitingDialog(R.string.bind_device_check_bindID);
        Message.CheckDeviceReqMsg reqMsg = Message.CheckDeviceReqMsg.newBuilder()
                .setDeviceId(bindNum)
                .build();

        final String finalBindNum = bindNum;
        exec(
                reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.CheckDeviceRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "sendResult() -> exec() -> onResponse(): " + rspMsg);
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                onBindNumIsvalid(finalBindNum, true);
                            } else {
                                popErrorDialog(R.string.tip_invalid_device_id, new OnDismissListener() {
                                    @Override
                                    public void onDismiss(SVProgressHUB hud) {
                                        restartPreviewAfterDelay(0);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            L.e(TAG, "sendResult() -> exec(" + Message.CheckDeviceReqMsg.class.getSimpleName() + ") -> onResponse() process failure", e);
                            popErrorDialog(R.string.request_failed, new OnDismissListener() {
                                @Override
                                public void onDismiss(SVProgressHUB hud) {
                                    restartPreviewAfterDelay(0);
                                }
                            });
                        }

                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "sendResult() -> exec(" + Message.CheckDeviceReqMsg.class.getSimpleName() + ") -> onException()", cause);
                        popErrorDialog(R.string.request_failed, new OnDismissListener() {
                            @Override
                            public void onDismiss(SVProgressHUB hud) {
                                restartPreviewAfterDelay(0);
                            }
                        });
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        // never to here for this exec
                    }
                }
        );
    }

    private void onBindNumIsvalid(String bindNum, boolean checkWhetherBound) {
        switch (workMode) {
            case PARAM_VALUE_MODE_BIND_DEVICE:
                if (checkWhetherBound) {
                    checkIsAlreadyBound(bindNum);
                } else {
                    doBind(bindNum);
                }
                break;
            default:
                result(bindNum, RESULT_VALUE_BIND_STATUS_WAIT);
                break;
        }
    }

    private void checkIsAlreadyBound(final String bindNum) {
        Message.FetchDeviceListReqMsg reqMsg = Message.FetchDeviceListReqMsg.newBuilder()
                .setUserId(mUserId)
                .build();

        exec(reqMsg, new TlcService.OnExecListener() {
            @Override
            public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                try {
                    Message.FetchDeviceListRspMsg rspMsg = response.getProtoBufMsg();
                    for (Message.UsrDevAssoc one : rspMsg.getUsrDevAssocList()) {
                        if (one.getDeviceId().equals(bindNum)) {
                            popInfoDialog(R.string.device_already_bound, new OnDismissListener() {
                                @Override
                                public void onDismiss(SVProgressHUB hud) {
                                    restartPreviewAfterDelay(0);
                                }
                            });
                            return true;
                        }
                    }
                    dismissDialog();
                    doBind(bindNum);
                } catch (Exception e) {
                    L.e(TAG, "checkIsAlreadyBound() -> exec() -> onResponse() process failure", e);
                    popErrorDialog(R.string.request_failed, new OnDismissListener() {
                        @Override
                        public void onDismiss(SVProgressHUB hud) {
                            restartPreviewAfterDelay(0);
                        }
                    });
                }
                return true;
            }

            @Override
            public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                L.e(TAG, "checkIsAlreadyBound() -> exec() -> onException()", cause);
                popErrorDialog(R.string.request_failed, new OnDismissListener() {
                    @Override
                    public void onDismiss(SVProgressHUB hud) {
                        restartPreviewAfterDelay(0);
                    }
                });
            }

            @Override
            public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

            }
        });
    }

    private void doBind(String bindNum) {
        L.v(TAG, "doBind");
        Intent intent = new Intent(CaptureBindNumberActivity.this, SelectRelationActivity.class);
        intent.putExtra(SelectRelationActivity.PARAM_KEY_MODE, SelectRelationActivity.PARAM_MODE_BIND);
        intent.putExtra(SelectRelationActivity.PARAM_KEY_BIND_NUM, bindNum);
        if (isBabyCardBind())
            intent.putExtra("ACTIVITY_MODE", "MOREFRAGMENT");
        startActivityForResult(intent, ACTIVITY_REQUEST_SELECT_RELATION_AND_DO_BIND);
    }

    private void showResult(final Result rawResult, Bitmap barcode) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        Drawable drawable = new BitmapDrawable(barcode);
        builder.setIcon(drawable);

        builder.setTitle(getString(R.string.user_type_label_name) + ":" + rawResult.getBarcodeFormat() + "\n " + getString(R.string.register_login_result) + rawResult.getText());
        builder.setPositiveButton(getString(R.string.ok), new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent();
                intent.putExtra("result", rawResult.getText());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        builder.setNegativeButton(getString(R.string.bind_device_scan_again), new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                restartPreviewAfterDelay(0L);
            }
        });
        builder.setCancelable(false);
        builder.show();

        // Intent intent = new Intent();
        // intent.putExtra(RESULT_KEY_BIND_NUM, rawResult.getText());
        // setResult(RESULT_OK, intent);
        // finish();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(MessageIDs.restart_preview, delayMS);
        }
    }

    private void initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

            try {
                AssetFileDescriptor fileDescriptor = getAssets().openFd("qrbeep.ogg");
                this.mediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(),
                        fileDescriptor.getLength());
                this.mediaPlayer.setVolume(0.1F, 0.1F);
                this.mediaPlayer.prepare();
            } catch (IOException e) {
                this.mediaPlayer = null;
            }
        }
    }

    private static final long VIBRATE_DURATION = 200L;

    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null)
                vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final OnCompletionListener beepListener = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		if (keyCode == KeyEvent.KEYCODE_BACK) {
//			setResult(RESULT_CANCELED);
//			finish();
//			return true;
//		} else if (keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_CAMERA) {
//			return true;
//		}
//		return super.onKeyDown(keyCode, event);
//	}


    @Override
    public void onTitleBarClick(View v) {
        super.onTitleBarClick(v);
        super.onDebouncedClick(v);
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.title_bar_right_text:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                } else {
                    photo();
                }
                break;
            case R.id.input_bindNumber:
                Intent intent = new Intent(CaptureBindNumberActivity.this, InputBindNumberActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

        }
    }

    private boolean isBabyCardBind() {
        Intent intent1 = getIntent();
        Bundle extras = intent1.getExtras();
        String activity_mode = null;
        if (extras != null) {
            activity_mode = extras.getString("ACTIVITY_MODE");
        }
        if (activity_mode != null) {
            return activity_mode.equals("BABYCARD_BIND");
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_CAMERA_PERMISSION) {
            // 使其在 onResume 里重新检查权限
            // 之所以不使用
            // 是因为有些机器（比如魅族）这种方法不可靠
            hasCameraPermission = null;
            if (Rom.isFlyme()) {
                // 使其在 onResume 里重新检查权限
                hasCameraPermission = null;
            } else {
                hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                if (shouldPreviewCamera && !hasCameraPermission) {
                    toast(R.string.no_camera_permission);
                }
            }
            return;
        }

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
               /* case ACTIVITY_REQUEST_MANUAL_INPUT_BIND_NUM: {
                    String bindNum = data.getStringExtra(InputBindNumberActivity.RESULT_KEY_BIND_NUM);
                    onBindNumIsvalid(bindNum, false);
                }
                break;*/

                case REQUEST_CODE: {
                    UCrop.Options options = new UCrop.Options();
                    options.withMaxResultSize(640, 640);
                    options.withAspectRatio(1, 1);
                    options.setHideBottomControls(true);
                    options.setToolbarColor(getResources().getColor(R.color.common_background));
                    options.withImmersionSystemBar(true);
                    options.setToolbarTitle(getString(R.string.crop));
                    options.setToolbarWidgetColor(getResources().getColor(R.color.title_bar_text));
                    UCrop.of(data.getData(), Uri.fromFile(new File(getExternalCacheDir(), "cut.jpg")))
                            .withOptions(options)
                            .start(this, UCropActivity.class, CUT_IMG);
                }
                break;

                case CUT_IMG: {
                    shouldPreviewCamera = true;
                    final File cutImgFile = new File(getExternalCacheDir(), "cut.jpg");
                    new ScanImageTask(this).execute(cutImgFile);
                }
                break;

                case ACTIVITY_REQUEST_SELECT_RELATION_AND_DO_BIND: {
                    boolean alreadyBind = data.getBooleanExtra(SelectRelationActivity.RESULT_KEY_ALREADY_BIND, false);
                    String bindNum = data.getStringExtra(SelectRelationActivity.RESULT_KEY_BIND_NUM);
                    if (alreadyBind) {
                        // 已经绑定，不用从服务器取数据
                        // 直接先将相关信息插入本地数据库
                        protocol.Message.UsrDevAssoc uda = (Message.UsrDevAssoc) data.getSerializableExtra(SelectRelationActivity.RESULT_KEY_USER_OF_DEV);
                        GreenUtils.saveUsrDevAssoc(uda);
                        result(bindNum, RESULT_VALUE_BIND_STATUS_ALREADY_BOUND);
                    } else {
                        // 等待绑定成功
                        Intent intent = new Intent(CaptureBindNumberActivity.this, WaitDeviceBindSuccessActivity.class);
                        intent.putExtra(WaitDeviceBindSuccessActivity.PARAM_KEY_WAIT_DEVIDE, bindNum);
                        startActivityForResult(intent, ACTIVITY_REQUEST_WAIT_BIND_RESULT);
                    }
                }
                break;

                case ACTIVITY_REQUEST_WAIT_BIND_RESULT:
                    result(data.getStringExtra(WaitDeviceBindSuccessActivity.RESULT_KEY_BIND_NUM), RESULT_VALUE_BIND_STATUS_OK);
                    break;
            }
        } else {
            shouldPreviewCamera = true;

            if (requestCode == ACTIVITY_REQUEST_WAIT_BIND_RESULT) {
                result(data.getStringExtra(WaitDeviceBindSuccessActivity.RESULT_KEY_BIND_NUM), RESULT_VALUE_BIND_STATUS_WAIT);
            }
        }
    }

    private void result(String bindNum, int bindStatus) {
        Intent intent = new Intent(RESULT_ACTION);
        intent.putExtra(RESULT_KEY_MODE, workMode);
        intent.putExtra(RESULT_KEY_BIND_NUM, bindNum);

        switch (workMode) {
            case PARAM_VALUE_MODE_BIND_DEVICE:
                intent.putExtra(RESULT_KEY_BIND_STATUS, bindStatus);
                break;
            default:
                break;
        }

        setResult(RESULT_OK, intent);
        finish();
    }

    private static class ScanImageTask extends AsyncTask<File, String, Result> {
        private WeakReference<CaptureBindNumberActivity> mA;

        ScanImageTask(CaptureBindNumberActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        protected void onPreExecute() {
            CaptureBindNumberActivity a = mA.get();
            if (a == null)
                return;
            a.popWaitingDialog(R.string.bind_device_identify_ing);
        }

        @Override
        protected Result doInBackground(File... imgFile) {
            CaptureBindNumberActivity a = mA.get();
            if (a == null)
                return null;
            return a.scanningImage(imgFile[0].getAbsolutePath());
        }

        @Override
        protected void onPostExecute(Result result) {
            CaptureBindNumberActivity a = mA.get();
            if (a == null)
                return;
            a.dismissDialog();
            if (result == null) {
                a.toast(R.string.bind_device_no_qr_code_was_identified);
            } else {
                a.handleDecode(result, null);
            }
        }
    }

    /**
     * 中文乱码
     *
     * @return
     */
    private String recode(String str) {
        String format = "";

        try {
            boolean ISO = Charset.forName("ISO-8859-1").newEncoder().canEncode(str);
            if (ISO) {
                format = new String(str.getBytes("ISO-8859-1"), "GB2312");
                L.i("1234      ISO8859-1", format);
            } else {
                format = str;
                L.i("1234      stringExtra", str);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return format;
    }

    private static final int REQUEST_CODE = 234;
    private static final int CAMERA_OK = 567;
    private static final int CUT_IMG = 568;

    private void photo() {
        // 激活系统图库，选择一张图片
        shouldPreviewCamera = false;
        Intent innerIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Intent wrapperIntent = Intent.createChooser(innerIntent, getString(R.string.bind_device_select_code_image));
        startActivityForResult(wrapperIntent, REQUEST_CODE);
    }

    /**
     * 解析部分图片
     *
     * @param fileAbsolutePath 图片路径
     * @return
     */
    protected Result scanningImage(String fileAbsolutePath) {
        Hashtable<DecodeHintType, String> hints = new Hashtable<>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8"); // 设置二维码内容的编码
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 先获取原大小
        Bitmap scanBitmap = BitmapFactory.decodeFile(fileAbsolutePath, options);
        options.inJustDecodeBounds = false; // 获取新的大小
        int sampleSize = (int) (options.outHeight / (float) 200);
        if (sampleSize <= 0)
            sampleSize = 1;
        options.inSampleSize = sampleSize;
        scanBitmap = BitmapFactory.decodeFile(fileAbsolutePath, options);

        int width = scanBitmap.getWidth();
        int height = scanBitmap.getHeight();
        int[] pixels = new int[width * height];
        scanBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        Reader reader = new MultiFormatReader();
        Result result = null;
        try {
            result = reader.decode(binaryBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }

    @Override
    protected boolean finishWhenNoMoreBaby(@Nullable BabyEntity oldBabyBean, boolean isSticky) {
        return false;
    }
}