package com.cqkct.FunKidII.Ui.view;


import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.AndroidPermissions;
import com.cqkct.FunKidII.Utils.DensityUtils;
import com.cqkct.FunKidII.Utils.FileUtils;
import com.cqkct.FunKidII.Utils.L;
import com.gyf.barlibrary.ImmersionBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;


public class RecordButton extends AppCompatTextView implements MediaRecorder.OnInfoListener {

    private Drawable drawableLeft, drawableRight;
    private float textMarginLeft, textMarginRight;
    private int textPaddingLeft, textPaddingRight;
    private int paddingLeft, paddingRight;

    public RecordButton(Context context) {
        super(context);
        init(context, null);
    }

    public RecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RecordButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RecordButton, 0, 0);
            try {
                textMarginRight = a.getDimension(R.styleable.RecordButton_textMarginRight, 0);
                textMarginLeft = a.getDimension(R.styleable.RecordButton_textMarginLeft, 0);
                setDrawable(a.getDrawable(R.styleable.RecordButton_drawableLeft), a.getDrawable(R.styleable.RecordButton_drawableRight));
            } finally {
                a.recycle();
            }
        }
        paddingLeft = getPaddingLeft();
        paddingRight = getPaddingRight();

        volumeHandler = new ShowVolumeHandler(this);
    }

    public void setDrawable(final Drawable left, final Drawable right) {
        int[] state;
        state = getDrawableState();
        if (left != null) {
            left.setState(state);
            left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
            left.setCallback(this);
        }
        if (right != null) {
            right.setState(state);
            right.setBounds(0, 0, right.getIntrinsicWidth(), right.getIntrinsicHeight());
            right.setCallback(this);
        }
        drawableRight = right;
        drawableLeft = left;
        requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (drawableRight != null) {
            textPaddingRight = drawableRight.getIntrinsicWidth() / 2;
        }
        if (drawableLeft != null) {
            textPaddingLeft = drawableLeft.getIntrinsicWidth() / 2;
        }
        if (drawableLeft != null && drawableRight != null) {
            textPaddingRight = 0;
            textPaddingLeft = 0;
        }
        setPadding(paddingLeft + textPaddingLeft * 2 + (int) textMarginLeft / 2, getPaddingTop(),
                paddingRight + textPaddingRight * 2 + (int) textMarginRight / 2, getPaddingBottom());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = 0;
        int height = 0;
        if (drawableRight != null) {
            width += drawableRight.getIntrinsicWidth();
            height = drawableRight.getIntrinsicHeight();
        }
        if (drawableLeft != null) {
            width += drawableLeft.getIntrinsicWidth();
            int drawableLeftHeight = drawableLeft.getIntrinsicHeight();
            height = height > drawableLeftHeight ? height : drawableLeftHeight;
        }
        setMeasuredDimension(Math.max(getMeasuredWidth(), width), Math.max(getMeasuredHeight(), height));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (drawableRight != null) {
            Rect rect = drawableRight.getBounds();
            float textWidth = getPaint().measureText(getText().toString());
            float drawableX = getWidth() / 2 + textWidth / 2 - textPaddingRight + textMarginRight / 2;
            if (drawableX + rect.width() >= getWidth()) {
                drawableX = getWidth() - rect.width();
            }
            canvas.save();
            canvas.translate(drawableX, getHeight() / 2 - rect.bottom / 2);
            drawableRight.draw(canvas);
            canvas.restore();
        }
        if (drawableLeft != null) {
            Rect rect = drawableLeft.getBounds();
            float textWidth = getPaint().measureText(getText().toString());
            float drawableX = getWidth() / 2 - textWidth / 2 - rect.width() + textPaddingLeft - textMarginLeft / 2;
            if (drawableX <= 0) {
                drawableX = 0;
            }
            canvas.save();
            canvas.translate(drawableX, getHeight() / 2 - rect.bottom / 2);
            drawableLeft.draw(canvas);
            canvas.restore();
        }
    }


    private static final String TAG = RecordButton.class.getSimpleName();

    public static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 28723;
    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 28724;
    public static final int ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_RECORD_AUDIO_PERMISSION = 28723;
    public static final int ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_WRITE_EXTERNAL_STORAGE = 28724;

    private static final int MIN_TIME = 1000 * 1;
    private static final int MAX_TIME = 1000 * 15;

    private Activity mActivity;
    private Toast mToast;

    private Long startRecordTime;


    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    public void setOnFinishedRecordListener(OnFinishedRecordListener listener) {
        finishedListener = listener;
    }


    private void toast(String msg, int duration) {
        synchronized (this) {
            if (mToast == null) {
                mToast = Toast.makeText(getContext(), msg, duration);
            } else {
                mToast.setDuration(duration);
                mToast.setText(msg);
            }
        }
        mToast.show();
    }

    private void toast(int strResId, int duration) {
        synchronized (this) {
            if (mToast == null) {
                mToast = Toast.makeText(getContext(), strResId, duration);
            } else {
                mToast.setDuration(duration);
                mToast.setText(strResId);
            }
        }
        mToast.show();
    }

    private void showRecordAudioPermissionGuide() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.record_audio_permission)
                .setMessage(R.string.please_enable_record_audio_permission_in_setting)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = AndroidPermissions.permissionSettingPageIntent(mActivity);
                        mActivity.startActivityForResult(intent, ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_RECORD_AUDIO_PERMISSION);
                    }
                })
                .show();
    }

    private void showStorePermissionGuide() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.external_storage_permission)
                .setMessage(R.string.please_enable_external_storage_permission_in_setting)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = AndroidPermissions.permissionSettingPageIntent(mActivity);
                        mActivity.startActivityForResult(intent, ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_WRITE_EXTERNAL_STORAGE);
                    }
                })
                .show();
    }

    private static class ShowRecordAudioPermissionGuideRunnable implements Runnable {
        private WeakReference<RecordButton> mB;

        ShowRecordAudioPermissionGuideRunnable(RecordButton b) {
            mB = new WeakReference<>(b);
        }

        @Override
        public void run() {
            RecordButton b = mB.get();
            if (b == null)
                return;
            b.showRecordAudioPermissionGuide();
        }
    }

    private File voiceFile = null;

    private OnFinishedRecordListener finishedListener;


    /**
     * 取消语音发送
     */
    private RecordIndicatorDialog recordIndicatorDialog;
    private View recordIndicatorDialogDepView;

    private static int[] res = {
            R.drawable.voice_level_0, R.drawable.voice_level_1, R.drawable.voice_level_2, R.drawable.voice_level_3, R.drawable.voice_level_4,
            R.drawable.voice_level_5, R.drawable.voice_level_6, R.drawable.voice_level_7, R.drawable.voice_level_8, R.drawable.voice_level_9,
            R.drawable.voice_level_10, R.drawable.voice_level_11, R.drawable.voice_level_12, R.drawable.voice_level_13, R.drawable.voice_level_14,
            R.drawable.voice_level_15, R.drawable.voice_level_16, R.drawable.voice_level_17, R.drawable.voice_level_18, R.drawable.voice_level_19,
            R.drawable.voice_level_20, R.drawable.voice_level_21, R.drawable.voice_level_22, R.drawable.voice_level_23, R.drawable.voice_level_24
    };

    private ImageView mImageView;

    private TextView tv_Countdown;

    private MediaRecorder recorder;

    private ObtainDecibelThread thread;

    private Count_DownTimer count_downTimer;

    private Handler volumeHandler;

    private TextView send_state;

    private boolean hasRecordPermisstion() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (AndroidPermissions.shouldShowGuide(mActivity, Manifest.permission.RECORD_AUDIO)) {
                showRecordAudioPermissionGuide();
            } else {
                ActivityCompat.requestPermissions((Activity) getContext(), new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            }
            return false;
        } else {
            return true;
        }
    }

    private boolean hasStorePermisstion() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (AndroidPermissions.shouldShowGuide(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showStorePermissionGuide();
            } else {
                ActivityCompat.requestPermissions((Activity) getContext(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
            return false;
        } else {
            return true;
        }
    }

    private static class TouchEventHandler extends Handler {
        private WeakReference<RecordButton> mB;

        TouchEventHandler(RecordButton b) {
            mB = new WeakReference<>(b);
        }

        @Override
        public void handleMessage(Message msg) {
            RecordButton b = mB.get();
            if (b == null)
                return;

            TouchEvent event = (TouchEvent) msg.obj;
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    b.startRecordTime = null;
                    if (!b.hasRecordPermisstion())
                        break;
                    if (!b.hasStorePermisstion())
                        break;
                    b.downY = event.getY();
                    b.initDialogAndStartRecord();
                    if (b.voiceFile != null) {
                        b.setText(R.string.chat_loosen_over);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (b.voiceFile != null) {
                        if (b.movedY > b.getCancelRecordYThreshold()) {
                            b.cancelRecord();
                        } else {
                            b.finishRecord();
                        }
                    }
                    b.setText(R.string.chat_long_click_speak);
                    break;

                default:
                    break;
            }
        }
    }

    private TouchEventHandler mTouchEventHandler = new TouchEventHandler(this);

    private class TouchEvent {
        int action;
        float x, y;

        TouchEvent(int action, float x, float y) {
            this.action = action;
            this.x = x;
            this.y = y;
        }

        int getAction() {
            return action;
        }

        float getX() {
            return x;
        }

        float getY() {
            return y;
        }
    }

    private float downY, movedY;
    private Float cancelRecordYThreshold;

    private float getCancelRecordYThreshold() {
        if (cancelRecordYThreshold == null) {
            cancelRecordYThreshold = DensityUtils.dp2px(getContext(), 120);
        }
        return cancelRecordYThreshold;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                setBackgroundResource(R.drawable.record_button_background_pressed);
                setText(R.string.chat_loosen_over);
                TouchEvent touchEvent = new TouchEvent(action, event.getX(), event.getY());
                mTouchEventHandler.removeMessages(action);
                mTouchEventHandler.sendMessageDelayed(mTouchEventHandler.obtainMessage(action, touchEvent), 50);
            }
            break;

            case MotionEvent.ACTION_UP: {
                setBackgroundResource(R.drawable.record_button_background_release);
                TouchEvent touchEvent = new TouchEvent(action, event.getX(), event.getY());
                mTouchEventHandler.removeMessages(action);
                mTouchEventHandler.removeMessages(MotionEvent.ACTION_DOWN);
                mTouchEventHandler.sendMessageDelayed(mTouchEventHandler.obtainMessage(action, touchEvent), 50);
            }
            break;

            case MotionEvent.ACTION_CANCEL:// 当手指移动到view外面，会cancel
                setBackgroundResource(R.drawable.record_button_background_release);
                setText(R.string.chat_long_click_speak);
                if (voiceFile != null) {
                    cancelRecord();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (voiceFile != null) {
                    movedY = downY - event.getY();
                    if (movedY > getCancelRecordYThreshold()) {
                        send_state.setText(R.string.chat_loosen_finger_cancel_send);
                        send_state.setTextColor(getResources().getColor(R.color.white));
                        send_state.setBackgroundResource(R.drawable.micro_chat_record_indicator_dialog_tip_text_bg);
                    } else {
                        send_state.setText(R.string.chat_finger_up_cancel_send);
                        send_state.setTextColor(getResources().getColor(R.color.micro_chat_record_indicator_dialog_tip_text));
                        send_state.setBackgroundResource(0);
                    }
                }
                break;
        }

        return true;
    }

    public void setRecordIndicatorDialogDepView(View view) {
        recordIndicatorDialogDepView = view;
    }

    private void initDialogAndStartRecord() {
        L.v(TAG, "init Dialog and Start Record ");
        String filename = getVoiceName();
        if (TextUtils.isEmpty(filename)) {
            L.e(TAG, "initDialogAndStartRecord() -> getVoiceName() return empty string");
            return;
        }

        voiceFile = new File(FileUtils.getExternalStorageVoiceChatCacheDirFile(), filename);
        int status = startRecording();
        if (status != 0) {
            voiceFile.delete();
            voiceFile = null;
        }
        switch (status) {
            case 0:
                // 成功
                break;
            case 1:
                // 无录音权限
                showRecordAudioPermissionGuide();
                return;
            case 2:
                // 无存储权限
                showStorePermissionGuide();
                return;
            default:
                // 失败
                return;
        }

        startRecordTime = System.currentTimeMillis();

        recordIndicatorDialog = new RecordIndicatorDialog(mActivity, R.style.chat_record_dialog_style);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.view_record_button, null);
        mImageView = view.findViewById(R.id.emoticon_image);
        tv_Countdown = view.findViewById(R.id.tv_count_down);
        send_state = view.findViewById(R.id.send_state);
        recordIndicatorDialog.setContentView(view);
        Window window = recordIndicatorDialog.getWindow();
        if (window != null) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

            recordIndicatorDialog.immersionBar("recordIndicatorDialog." + System.currentTimeMillis() + Math.random());
            window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.gravity = Gravity.TOP;
            layoutParams.height = (int) (recordIndicatorDialogDepView.getY() + recordIndicatorDialogDepView.getHeight());
        }
        recordIndicatorDialog.setOnShowListener(dialog -> {
            if (mOnDialogShowDismissListener != null) {
                mOnDialogShowDismissListener.onEvent(true);
            }
        });
        recordIndicatorDialog.setOnDismissListener(dialog -> {
            if (mOnDialogShowDismissListener != null) {
                mOnDialogShowDismissListener.onEvent(false);
            }
            RecordIndicatorDialog riDialog = (RecordIndicatorDialog) dialog;
            if (riDialog.mImmersionBar != null) {
                riDialog.mImmersionBar.destroy();
            }
        });
        count_downTimer = new Count_DownTimer(15 * 1000, 500);
        count_downTimer.start();
        recordIndicatorDialog.show();
    }

    public interface OnDialogShowDismissListener {
        void onEvent(boolean show);
    }

    private OnDialogShowDismissListener mOnDialogShowDismissListener;

    public void setOnDialogShowDismissListener(OnDialogShowDismissListener listener) {
        mOnDialogShowDismissListener = listener;
    }

    private class RecordIndicatorDialog extends Dialog {

        private Activity mActivity;
        private ImmersionBar mImmersionBar;

        public RecordIndicatorDialog(@NonNull Activity activity) {
            super(activity);
            mActivity = activity;
            init();
        }

        public RecordIndicatorDialog(@NonNull Activity activity, int themeResId) {
            super(activity, themeResId);
            mActivity = activity;
            init();
        }

        protected RecordIndicatorDialog(@NonNull Activity activity, boolean cancelable, @Nullable OnCancelListener cancelListener) {
            super(activity, cancelable, cancelListener);
            mActivity = activity;
            init();
        }

        private void init() {
        }

        public void immersionBar(@NonNull String dialogTag) {
            mImmersionBar = ImmersionBar.with(mActivity, recordIndicatorDialog, dialogTag);
            mImmersionBar.init();
        }
    }

    private void finishRecord() {
        L.v(TAG, "finish Record ");
        long nowTime = System.currentTimeMillis();
        if (voiceFile == null)
            return;
        File audioFile = voiceFile;
        voiceFile = null;
        setBackgroundResource(R.drawable.record_button_background_release);
        setText(R.string.chat_long_click_speak);
        stopRecording();
        recordIndicatorDialog.dismiss();
        Long startTime = startRecordTime;
        if (startTime != null) {
            long intervalTime = nowTime - startTime;
            L.d(TAG, "finishRecord the intervalTime: " + intervalTime);
            if (intervalTime < MIN_TIME) {
                toast(R.string.chat_time_to_short, Toast.LENGTH_SHORT);
                audioFile.delete();
                return;
            }
            if (finishedListener != null)
                finishedListener.onFinishedRecord(audioFile, (int) ((intervalTime + 1000 / 2) / 1000));
        }
    }

    private void cancelRecord() {
        L.v(TAG, "cancel Record ");
        File savedVoiceFile = voiceFile;
        voiceFile = null;
        stopRecording();
        recordIndicatorDialog.dismiss();
        if (savedVoiceFile != null) {
            savedVoiceFile.delete();
        }
    }

    /**
     * @return -1: 失败， 0: 成功， 1: 无录音权限，2: 无存储权限
     */
    private synchronized int startRecording() {
        L.i("start Recording ");
        recorder = new MediaRecorder();
        try {
            try {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            } catch (RuntimeException e) {
                // 无录音权限
                recorder.release();
                recorder = null;
                return 1;
            }
            recorder.setAudioChannels(1);
            recorder.setAudioEncodingBitRate(4000);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//            recorder.setVideoFrameRate(4000);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                recorder.setOutputFile(voiceFile);
            } else {
                recorder.setOutputFile(voiceFile.getAbsolutePath());
            }
            recorder.setMaxDuration(MAX_TIME);
            recorder.setOnInfoListener(this);
        } catch (Exception e) {
            L.e(TAG, "init MediaRecorder failure", e);
            recorder.release();
            recorder = null;
            return -1;
        }

        try {
            try {
                recorder.prepare();
            } catch (FileNotFoundException e) {
                String msg = e.getMessage();
                if (!TextUtils.isEmpty(msg) && msg.contains("Permission denied")) {
                    // 无存储权限
                    recorder.release();
                    recorder = null;
                    return 2;
                }
            }
            try {
                recorder.start();
            } catch (IllegalStateException e) {
                recorder.release();
                recorder = null;
                return -1;
            }
        } catch (Exception e) {
            try {
                recorder.release();
                recorder = null;
            } catch (Exception ignored) {
            }
            L.d(TAG, "startRecording", e);
            String msg = e.getMessage();
            if (!TextUtils.isEmpty(msg) && e.getMessage().contains("Permission deny")) {
                return 1;
            }
            return -1;
        }

        try {
            thread = new ObtainDecibelThread(this);
            thread.start();
        } catch (Exception e) {
            L.e(TAG, "startRecording ObtainDecibelThread failure", e);
            try {
                recorder.stop();
            } finally {
                recorder.release();
                recorder = null;
            }
            return -1;
        }

        return 0;
    }

    private class Count_DownTimer extends CountDownTimer {

        Count_DownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            tv_Countdown.setText(String.format("%ds", millisUntilFinished / 1000 + 1));
        }

        @Override
        public void onFinish() {
            tv_Countdown.setText("0s");
        }
    }

    /**
     * 检查是否存在SDCard
     *
     * @return
     */
    public static boolean hasSdcard() {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }


    private synchronized void stopRecording() {
        L.i("stop Recording");
        count_downTimer.cancel();
        try {
            if (thread != null) {
                try {
                    thread.exit();
                } finally {
                    thread = null;
                }
            }
            if (recorder != null) {
                try {
                    recorder.stop();
                } finally {
                    recorder.release();
                    recorder = null;
                }
            }
        } catch (Exception e) {
            L.w(TAG, "stopRecording", e);
        }

    }

    private OnDismissListener onDismiss = new OnDismissListener() {

        @Override
        public void onDismiss(DialogInterface dialog) {
            stopRecording();
            L.e("on dismiss Listener ");
        }
    };

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            L.i(TAG, "VIDEOCAPTURE: Maximum Duration Reached");
            finishRecord();
        }
    }

    private static class ShowVolumeHandler extends Handler {
        private WeakReference<RecordButton> mB;

        ShowVolumeHandler(RecordButton b) {
            mB = new WeakReference<>(b);
        }

        @Override
        public void handleMessage(Message msg) {
            RecordButton b = mB.get();
            if (b == null)
                return;

            int idx = msg.what;
            if (idx < 0)
                idx = 0;
            if (idx >= res.length)
                idx = res.length - 1;
            b.mImageView.setImageResource(res[idx]);
        }
    }

    public interface OnFinishedRecordListener {
        void onFinishedRecord(File audioFile, int time);
    }

    private String getVoiceName() {
        String deviceId = App.getInstance().getDeviceId();
        String userId = App.getInstance().getUserId();
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(userId))
            return null;
        String time = String.valueOf(System.currentTimeMillis() / 1000);
        return userId + '-' + deviceId + '-' + time + ".amr";
    }

    private static class ObtainDecibelThread extends Thread {
        private WeakReference<RecordButton> mB;
        private volatile boolean running = true;

        ObtainDecibelThread(RecordButton b) {
            mB = new WeakReference<>(b);
        }

        public void exit() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!running)
                    break;
                doRun();
            }
        }

        private void doRun() {
            RecordButton b = mB.get();
            if (b == null)
                return;

            if (b.recorder == null)
                return;

            try {
                int x = b.recorder.getMaxAmplitude();
                if (x > 0) {
                    double db = 20 * Math.log10(x);
                    int level = (int) ((db - 40) * res.length / 50);
                    L.v(TAG, "db: " + db + ", level: " + level);
                    b.volumeHandler.sendEmptyMessage(level);
                }
            } catch (Exception ignored) {
            }
        }
    }


}
