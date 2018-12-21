package com.cqkct.FunKidII.Ui.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.App.MainServiceEventCallback;
import com.cqkct.FunKidII.App.TlcServiceEventCallback;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Listener.DebouncedOnClickListener;
import com.cqkct.FunKidII.Ui.fragment.CustomDialog;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.UserPermission;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.ExecEntity;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.PreferencesWrapper;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.svprogresshub.SVProgressHUB;
import com.cqkct.FunKidII.svprogresshub.listener.OnCancelListener;
import com.cqkct.FunKidII.svprogresshub.listener.OnDismissListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.protobuf.GeneratedMessageV3;
import com.gyf.barlibrary.ImmersionBar;
import com.umeng.analytics.MobclickAgent;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import protocol.Message;

/**
 * 其他Activity基类
 */
public abstract class BaseActivity extends AppCompatActivity implements MainServiceEventCallback, App.OnTlcServiceBindListener, TlcServiceEventCallback, App.OnDeviceChangeListener {
    private static final String TAG = BaseActivity.class.getSimpleName();

    /**
     * 界面处于销毁中？
     */
    private boolean isFinish = false;
    /**
     * 界面处于前台？
     */
    private boolean isForeground = true;
    /**
     * 界面集合
     */
    private static List<BaseActivity> activityList = new ArrayList<>();
    /**
     * 处于打开界面状态(包含准备打开一个本应用的新界面，或者从本应用的界面回到上一个界面)
     */
    private static boolean isOpeningActivity = false;

    private ActivityManager mActivityManager;

    private InputMethodManager imm;
    protected ImmersionBar mImmersionBar;


    @Nullable
    protected TlcService mTlcService;
    @Nullable
    protected BabyEntity mCurrentBabyBean;
    @Nullable
    public String mUserId;
    @Nullable
    protected String mDeviceId;

    private SVProgressHUB mSVProgressHUB;

    private Toast mToast;

    private PreferencesWrapper mPreferencesWrapper;

    private DebouncedOnClickListener mDebouncedOnClickListener = new DebouncedOnClickListener() {
        @Override
        public void onDebouncedClick(View view) {
            BaseActivity.this.onDebouncedClick(view, view.getId());
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isOpeningActivity = true;
        activityList.add(this);

        //初始化沉浸式
        if (isImmersionBarEnabled())
            initImmersionBar();

        registerCallbacks();
    }

    /**
     * 是否可以使用沉浸式
     * Is immersion bar enabled boolean.
     *
     * @return the boolean
     */
    protected boolean isImmersionBarEnabled() {
        return true;
    }

    protected void initImmersionBar() {
        mImmersionBar = ImmersionBar.with(this);
        mImmersionBar.statusBarDarkFont(true);
        mImmersionBar.init();
    }

    protected void registerCallbacks() {
        App app = App.getInstance();
        app.regMainServiceEventCallback(this);
        app.regOnTlcServiceBindListener(this);
        app.regTlcServiceEventCallback(this);
        app.regOnCurrentBabyBeanChangeListener(this);
        mTlcService = app.getTlcService();
        mCurrentBabyBean = app.getCurrentBabyBean();
        mUserId = app.getUserId();
        mDeviceId = app.getDeviceId();
    }

    @Override
    protected void onStart() {
        isOpeningActivity = true;
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isOpeningActivity = false;
        isForeground = true;

        onMobclickAgentOnResume();
    }

    @Override
    protected void onPause() {
        isForeground = false;
        super.onPause();

        onMobclickAgentOnPause();
    }

    protected void onMobclickAgentOnResume() {
        MobclickAgent.onResume(this);
        MobclickAgent.onPageStart(getClass().getName());
    }

    protected void onMobclickAgentOnPause() {
        MobclickAgent.onPageEnd(getClass().getName());
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void finish() {
        synchronized (this) {
            if (isFinishing())
                return;
            // 若之前还有打开的界面(关闭当前界面会打开之前的界面)，标记为true
            if (activityList.size() > 1) {
                isOpeningActivity = true;
            }
            super.finish();
            isFinish = true;

            hideSoftKeyBoard();
        }
    }

    @Override
    protected void onDestroy() {
        App app = App.getInstance();
        app.unregOnCurrentBabyBeanChangeListener(this);
        app.unregTlcServiceEventCallback(this);
        app.unregOnTlcServiceBindListener(this);
        app.unregMainServiceEventCallback(this);
        super.onDestroy();

        isFinish = true;
        activityList.remove(this);

        if (mImmersionBar != null)
            mImmersionBar.destroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        isForeground = true;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);

        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            String className = componentName.getClassName();
            // 如果要打开的界面属于本应用界面，则标记为 true
            if (!TextUtils.isEmpty(className) && className.toLowerCase().contains(this.getApplication().getPackageName())) {
                isOpeningActivity = true;
            }
        }
    }

    public final void onDebouncedClick(View v) {
        mDebouncedOnClickListener.onClick(v);
    }

    public DebouncedOnClickListener getDebouncedOnClickListener() {
        return mDebouncedOnClickListener;
    }

    public void onDebouncedClick(View view, @IdRes int viewId) {

    }

    /**
     * 获取系统当前使用的语言
     * 设置成简体中文的时候，getLanguage() 返回的是zh
     *
     * @return Language
     */
    protected static String getCurrentLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /**
     * 获得当前系统语言
     *
     * @return Language
     */
    public String getCurrentLanguageUseResources() {
        return getResources().getConfiguration().locale.getLanguage();
    }

    protected int selectAppArea() {
        int area = getPreferencesWrapper().getAppArea();
        if (area == PreferencesWrapper.APP_AREA_UNKNOWN) {
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
            if (resultCode == ConnectionResult.SUCCESS && !getCurrentLanguageUseResources().toLowerCase().equals("zh")) {
                area = PreferencesWrapper.APP_AREA_OVER_SEA;
            } else {
                area = PreferencesWrapper.APP_AREA_CHINA;
            }
            getPreferencesWrapper().setAppArea(area);
        }
        return area;
    }

    protected PreferencesWrapper getPreferencesWrapper() {
        if (mPreferencesWrapper == null) {
            mPreferencesWrapper = PreferencesWrapper.getInstance(this);
        }
        return mPreferencesWrapper;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public boolean isForeground() {
        return isForeground;
    }

    protected BaseActivity getPreviousActivity() {
        try {
            return activityList.get(activityList.size() - 2);
        } catch (Exception e) {
            L.e(TAG, "getPreviousActivity", e);
        }
        return null;
    }

    /**
     * 程序是否在前台运行
     *
     * @return true 程序前台运行，false 程序后台运行
     */
    public static boolean isAppRunOnForeground() {
        // 由于一个界面打开另一个界面是先执行 onPause（此时所有界面都不处于前台）之后再执行的，
        // 所以要加上一个本应用界面正在打开的标识
        boolean result = false;
        if (isOpeningActivity) { // 如果处于界面打开阶段，则肯定在前台
            result = true;
        } else { // 不处于应用打开阶段，若界面有一个处于前台则，当前应用处于前台，否则处于后台
            for (BaseActivity act : activityList) {
                if (act.isForeground) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 退出所有界面
     */
    public static void exitAllActivity() {
        List<BaseActivity> tempList = new ArrayList<>(activityList);
        for (BaseActivity act : tempList) {
            act.finish();
        }
        tempList.clear();
    }

    /**
     * 检测某Activity是否在当前Task的栈顶
     */
    protected boolean isTopActivity(String activityName) {
        ActivityManager activityManager = getActivityManager();
        if (activityManager == null)
            return false;
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = activityManager.getRunningTasks(1);
        if (runningTaskInfos == null || runningTaskInfos.isEmpty())
            return false;
        String cmpNameTemp = runningTaskInfos.get(0).topActivity.toString();
        return cmpNameTemp != null && cmpNameTemp.equals(activityName);
    }

    protected int getTaskNumActivities() {
        ActivityManager activityManager = getActivityManager();
        if (activityManager == null)
            return -1;
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = activityManager.getRunningTasks(1);
        if (runningTaskInfos == null || runningTaskInfos.isEmpty())
            return -1;
        return runningTaskInfos.get(0).numActivities;
    }

    /**
     * 检测某Activity是否在当前Task的栈顶
     */
    protected boolean isTopActivity() {
        return isTopActivity(getClass().getName());
    }

    protected ActivityManager getActivityManager() {
        if (mActivityManager == null) {
            synchronized (this) {
                if (mActivityManager == null)
                    mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            }
        }
        return mActivityManager;
    }

    protected App getApp() {
        return (App) getApplication();
    }

    /**
     * 设置标题栏的标题
     *
     * @param title 标题文字
     */
    protected void setTitleBarTitle(String title) {
        View v = findViewById(R.id.title_bar_title_text);
        if (v != null) {
            ((TextView) v).setText(title);
        }
    }

    /**
     * 设置标题栏的标题
     *
     * @param titleResId 标题文字资源id
     */
    protected void setTitleBarTitle(@StringRes int titleResId) {
        View v = findViewById(R.id.title_bar_title_text);
        if (v != null) {
            ((TextView) v).setText(titleResId);
        }
    }

    protected void setTitleBarOkBtnText(String text) {
        View v = findViewById(R.id.title_bar_right_text);
        if (v != null)
            ((TextView) v).setText(text);
    }

    protected void setTitleBarOkBtnText(int stringResId) {
        setTitleBarOkBtnText(getString(stringResId));
    }

    protected void setTitleBarOkBtnVisibility(int visibility) {
        View v = findViewById(R.id.title_bar_right_text);
        if (v != null)
            v.setVisibility(visibility);
    }

    /**
     * 标题栏相关控件的点击回调
     *
     * @param v 控件
     */
    public void onTitleBarClick(View v) {
        switch (v.getId()) {
            case R.id.title_bar_left_icon:
                onBackPressed();
                break;
            default:
                break;
        }
    }

    /**
     * 设置 view 的点击效果
     *
     * @param viewId viewId
     * @param show   显示/隐藏
     */
    public void setForeground(int viewId, boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            findViewById(viewId).setForeground(show ? getDrawable(R.drawable.foreground_shadow) : null);
        }
    }


    @SuppressLint("ShowToast")
    public void toast(String msg, int duration) {
        synchronized (this) {
            if (mToast == null) {
                mToast = Toast.makeText(this, msg, duration);
            } else {
                mToast.setDuration(duration);
                mToast.setText(msg);
            }
        }
//        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    /**
     * 菜单创建器，在Item要创建菜单的时候调用。
     */
    protected SwipeMenuCreator getSwipeMenuCreator(Activity activity) {
        return (swipeLeftMenu, swipeRightMenu, viewType) -> {
            int width = getResources().getDimensionPixelSize(R.dimen.dp_70);
            // 1. MATCH_PARENT 自适应高度，保持和Item一样高;
            // 2. 指定具体的高，比如80;
            // 3. WRAP_CONTENT，自身高度，不推荐;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            // 添加右侧的，如果不添加，则右侧不会出现菜单。
            SwipeMenuItem deleteItem = new SwipeMenuItem(activity)
                    .setImage(R.drawable.delete_left_slip)
                    .setWidth(width)
                    .setHeight(height);
            swipeRightMenu.addMenuItem(deleteItem);// 添加菜单到右侧。
        };
    }


    public AlertDialog createAlertDialog(Context context, View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setView(view);

        Window window = alertDialog.getWindow();
        assert window != null;
        window.setWindowAnimations(R.style.windowAnimation);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return alertDialog;
    }

    public Dialog createDialog(Context context, View view){
        Dialog dialog = new CustomDialog(context,R.style.TimePickerDialog);
        dialog.setContentView(view);
        return dialog;
    }


    public NumberPicker setNumberPicker(NumberPicker numberPicker, String[] mDisplayedHourValues, int currentHourIndex) {
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(mDisplayedHourValues.length - 1);
        numberPicker.setDisplayedValues(mDisplayedHourValues);
        //设置不可编辑
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        numberPicker.setValue(currentHourIndex);
        return numberPicker;
    }


    @SuppressLint("ShowToast")
    public void toast(int msgResId, int duration) {
        synchronized (this) {
            if (mToast == null) {
                mToast = Toast.makeText(this, msgResId, duration);
            } else {
                mToast.setDuration(duration);
                mToast.setText(msgResId);
            }
        }
//        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    public void toast(String msg) {
        toast(msg, Toast.LENGTH_SHORT);
    }

    public void toast(@StringRes int msgResId) {
        toast(msgResId, Toast.LENGTH_SHORT);
    }


    protected synchronized void popWaitingDialog(final String msg, final boolean cancelable, final OnCancelListener onCancelListener) {
        if (mSVProgressHUB != null)
            mSVProgressHUB.dismissImmediately();
        mSVProgressHUB = new SVProgressHUB(this);
        if (cancelable) {
            mSVProgressHUB.setOnCancelListener(onCancelListener);
            mSVProgressHUB.showWithStatus(msg, SVProgressHUB.SVProgressHUDMaskType.ClearCancel);
        } else {
            mSVProgressHUB.showWithStatus(msg);
        }
    }

    private enum DialogType {SUCCESS, INFO, FAILURE}

    private synchronized void popStatusDialog(String msg, DialogType type, boolean finishAfterDismiss, final OnDismissListener onDismissListener) {
        if (mSVProgressHUB != null)
            mSVProgressHUB.dismissImmediately();
        mSVProgressHUB = new SVProgressHUB(this);
        mSVProgressHUB.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(SVProgressHUB hud) {
                hud.setOnDismissListener(null);
                if (onDismissListener != null)
                    onDismissListener.onDismiss(hud);
            }
        });
        switch (type) {
            case SUCCESS:
                if (finishAfterDismiss) {
                    mSVProgressHUB.setOnDismissListener(new OnDismissListener() {
                        @Override
                        public void onDismiss(SVProgressHUB hud) {
                            hud.setOnDismissListener(null);
                            if (onDismissListener != null)
                                onDismissListener.onDismiss(hud);
                            finish();
                        }
                    });
                }
                mSVProgressHUB.showSuccessWithStatus(msg);
                break;
            case INFO:
                mSVProgressHUB.showInfoWithStatus(msg);
                break;
            default:
                mSVProgressHUB.showErrorWithStatus(msg);
                break;
        }
    }

    protected synchronized void dismissDialog() {
        if (mSVProgressHUB != null) {
            mSVProgressHUB.dismiss();
        }
    }

    protected void popWaitingDialog(String msg) {
        popWaitingDialog(msg, false, null);
    }

    protected void popWaitingDialog(@StringRes int msgResId, boolean cancelable, OnCancelListener onCancelListener) {
        popWaitingDialog(getString(msgResId), cancelable, onCancelListener);
    }

    protected void popWaitingDialog(@StringRes int msgResId) {
        popWaitingDialog(msgResId, false, null);
    }

    protected synchronized void updateDialogText(String msg) {
        if (mSVProgressHUB != null) {
            mSVProgressHUB.setText(msg);
        }
    }

    protected void updateDialogText(@StringRes int msgResId) {
        updateDialogText(getString(msgResId));
    }

    protected void popStatusDialog(@StringRes int msgResId, boolean success, boolean finishAfterDismiss, OnDismissListener onDismissListener) {
        popStatusDialog(getString(msgResId), DialogType.SUCCESS, finishAfterDismiss, onDismissListener);
    }

    protected void popSuccessDialog(String msg) {
        popSuccessDialog(msg, false);
    }

    protected void popSuccessDialog(@StringRes int msgResId) {
        popSuccessDialog(getString(msgResId));
    }

    protected void popSuccessDialog(String msg, OnDismissListener onDismissListener) {
        popSuccessDialog(msg, false, onDismissListener);
    }

    protected void popSuccessDialog(@StringRes int msgResId, OnDismissListener onDismissListener) {
        popSuccessDialog(getString(msgResId), onDismissListener);
    }

    protected void popSuccessDialog(String msg, boolean finishAfterDismiss) {
        popSuccessDialog(msg, finishAfterDismiss, null);
    }

    protected void popSuccessDialog(@StringRes int msgResId, boolean finishAfterDismiss) {
        popSuccessDialog(getString(msgResId), finishAfterDismiss, null);
    }

    protected void popSuccessDialog(String msg, boolean finishAfterDismiss, OnDismissListener onDismissListener) {
        popStatusDialog(msg, DialogType.SUCCESS, finishAfterDismiss, onDismissListener);
    }

    protected void popSuccessDialog(@StringRes int msgResId, boolean finishAfterDismiss, OnDismissListener onDismissListener) {
        popSuccessDialog(getString(msgResId), finishAfterDismiss, onDismissListener);
    }

    protected void popErrorDialog(String msg, OnDismissListener onDismissListener) {
        popStatusDialog(msg, DialogType.FAILURE, false, onDismissListener);
    }

    protected void popErrorDialog(@StringRes int msgResId, OnDismissListener onDismissListener) {
        popStatusDialog(getString(msgResId), DialogType.FAILURE, false, onDismissListener);
    }

    protected void popErrorDialog(String msg) {
        popStatusDialog(msg, DialogType.FAILURE, false, null);
    }

    protected void popErrorDialog(@StringRes int msgResId) {
        popErrorDialog(getString(msgResId));
    }

    protected void popInfoDialog(String msg, OnDismissListener onDismissListener) {
        popStatusDialog(msg, DialogType.INFO, false, onDismissListener);
    }

    protected void popInfoDialog(@StringRes int msgResId, OnDismissListener onDismissListener) {
        popStatusDialog(getString(msgResId), DialogType.INFO, false, onDismissListener);
    }

    protected void popInfoDialog(String msg) {
        popStatusDialog(msg, DialogType.INFO, false, null);
    }

    protected void popInfoDialog(@StringRes int msgResId) {
        popInfoDialog(getString(msgResId));
    }

    private volatile InputMethodManager mInputMethodManager;
    public InputMethodManager getInputMethodManager() {
        if (mInputMethodManager == null) {
            synchronized (this) {
                if (mInputMethodManager == null) {
                    mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                }
            }
        }
        return mInputMethodManager;
    }

    public void hideSoftKeyBoard() {
        View localView = getCurrentFocus();
        if (localView != null) {
            InputMethodManager imm = getInputMethodManager();
            if (imm != null) {
                imm.hideSoftInputFromWindow(localView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    public void showSoftKeyboard(EditText editText) {
        InputMethodManager imm = getInputMethodManager();
        if (imm != null) {
            editText.requestFocus();
            editText.post(() -> imm.showSoftInput(editText, 0));
        }
    }

    /**
     * 在当前宝贝上是否有修改权限
     *
     * @return 在当前宝贝上是否有修改权限
     */
    protected boolean hasEditPermission() {
        BabyEntity babyBean = ((App) getApplication()).getCurrentBabyBean();
        if (babyBean == null)
            return false;
        Integer permission = babyBean.getPermission();
        return permission != null && UserPermission.hasEditPermission(permission);
    }

    /**
     * userId在当deviceId上是否有修改权限
     *
     * @param userId   user ID
     * @param deviceId device ID
     * @return userId在当deviceId上是否有修改权限
     */
    protected boolean hasEditPermission(String userId, String deviceId) {
        BabyEntity babyBean = ((App) getApplication()).getCurrentBabyBean();
        if (babyBean != null && babyBean.getUserId().equals(userId) && babyBean.getDeviceId().equals(deviceId)) {
            Integer permission = babyBean.getPermission();
            return permission != null && UserPermission.hasEditPermission(permission);
        }
        return UserPermission.hasEditPermission(userId, deviceId);
    }


    protected ExecEntity exec(@NonNull GeneratedMessageV3 protoBufMsg, @Nullable final TlcService.OnExecListener listener, @Nullable Long thirdStageTimeoutMillis) {
        if (mTlcService == null) {
            final Exception exception = new Exception("TLC Service is null");
            L.e(TAG, "TlcService is null when exec", exception);
            if (listener != null) {
                final Pkt pkt = Pkt.newBuilder()
                        .setSrcAddr(TextUtils.isEmpty(mUserId) ? "" : mUserId)
                        .setValue(protoBufMsg)
                        .build();
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onException(pkt, exception);
                    }
                });
            }
            return null;
        }
        return mTlcService.exec(protoBufMsg, listener, thirdStageTimeoutMillis);
    }

    public ExecEntity exec(@NonNull GeneratedMessageV3 protoBufMsg, @Nullable TlcService.OnExecListener listener) {
        return exec(protoBufMsg, listener, null);
    }

    protected void send(@NonNull GeneratedMessageV3 protoBufMsg, @NonNull Pkt.Seq seq, final TlcService.OnSendListener listener) {
        if (mTlcService == null) {
            final Exception exception = new Exception("TLC Service is null");
            L.e(TAG, "TlcService is null when sendRspMsg", exception);
            if (listener != null) {
                final Pkt pkt = Pkt.newBuilder()
                        .setSeq(seq)
                        .setSrcAddr(TextUtils.isEmpty(mUserId) ? "" : mUserId)
                        .setValue(protoBufMsg)
                        .build();
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onException(pkt, exception);
                    }
                });
            }
            return;
        }
        mTlcService.send(protoBufMsg, seq, listener);
    }


    @Override
    public void onTlcServiceBound(TlcService service, boolean isSticky) {
        L.d(TAG, "onTlcServiceBound, isSticky: " + isSticky);
        if (!isSticky)
            mTlcService = service;
    }

    @Override
    public void onTlcServiceLost(boolean isSticky) {
        L.e(TAG, "onTlcServiceLost, isSticky: " + isSticky);
        if (!isSticky)
            mTlcService = null;
        L.e(TAG, "onTlcServiceLost finish!!!");
    }

    @Override
    public void onConnected(TlcService tlcService, boolean isSticky) {
    }

    @Override
    public void onDisconnected(TlcService tlcService, boolean isSticky) {
    }

    @Override
    public void onLoggedin(TlcService tlcService, @NonNull String userId, boolean isSticky) {
        if (!userId.equals(mUserId)) {
            mUserId = userId;
        }
    }

    @Override
    public void onLoggedout(TlcService tlcService, @Nullable String userId, boolean isSticky) {
//        if (!isSticky) {
        L.w(getClass().getSimpleName(), "onLoggedout!!! Activity finish");

        int leftActivity;
        if (false) {
            leftActivity = getTaskNumActivities();
        } else {
            leftActivity = activityList.size();
            for (BaseActivity act : activityList) {
                if (act.isFinish) {
                    --leftActivity;
                }
            }
        }
        if (leftActivity <= 1) {
            startActivity(new Intent(this, LoginActivity.class));
        }

        finish();
//        }
    }

    @Override
    public final void onBindDeviceRequest(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyAdminBindDevReqMsg reqMsg) {
    }

    @Override
    public void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg) {
    }

    @Override
    public void onDeviceUnbind(TlcService tlcService, @Nullable Pkt reqPkt, @NonNull Message.NotifyUserUnbindDevReqMsg reqMsg) {
    }

    @Override
    public void onDevConfChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDevConfChangedReqMsg reqMsg) {
    }

    @Override
    public void onDevConfSynced(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDevConfSyncedReqMsg reqMsg) {
    }

    @Override
    public void onDevFenceChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull Message.NotifyFenceChangedReqMsg reqMsg) {
    }

    @Override
    public void onDevSosChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull Message.NotifySosChangedReqMsg reqMsg) {
    }

    @Override
    public void onDevSosSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifySosSyncedReqMsg reqMsg) {
    }

    @Override
    public void onDevContactChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull Message.NotifyContactChangedReqMsg reqMsg) {
    }

    @Override
    public void onDevContactSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyContactSyncedReqMsg reqMsg) {
    }

    @Override
    public void onAlarmClockChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull Message.NotifyAlarmClockChangedReqMsg reqMsg) {
    }

    @Override
    public void onAlarmClockSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyAlarmClockSyncedReqMsg reqMsg) {
    }

    @Override
    public void onClassDisableChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull Message.NotifyClassDisableChangedReqMsg reqMsg) {
    }

    @Override
    public void onClassDisableSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyClassDisableSyncedReqMsg reqMsg) {
    }

    @Override
    public void onPraiseChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull Message.NotifyPraiseChangedReqMsg reqMsg) {
    }

    @Override
    public void onSchoolGuardChanged(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifySchoolGuardChangedReqMsg reqMsg) {
    }

    @Override
    public void onLocateS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.LocateS3ReqMsg reqMsg) {
    }

    @Override
    public void onDevicePosition(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDevicePositionReqMsg reqMsg) {
    }

    @Override
    public void onLocationModeChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyLocationModeChangedReqMsg reqMsg) {
    }

    @Override
    public void onDeviceIncident(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyIncidentReqMsg reqMsg) {
    }

    @Override
    public void onUsrDevAssocModified(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUsrDevAssocModifiedReqMsg reqMsg) {
    }

    @Override
    public void onFindDeviceS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.FindDeviceS3ReqMsg reqMsg) {
    }

    @Override
    public void onTakePhotoS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.TakePhotoS3ReqMsg reqMsg) {
    }

    @Override
    public void onSimplexCallS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.SimplexCallS3ReqMsg reqMsg) {
    }

    @Override
    public void onFetchDevStatusInfoS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.FetchDeviceSensorDataS3ReqMsg reqMsg) {
    }

    @Override
    public void onDeviceSensorData(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDeviceSensorDataReqMsg reqMsg) {
    }

    @Override
    public void onNewMicroChatMessage(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyChatMessageReqMsg reqMsg) {
    }

    @Override
    public void onNewMicroChatGroupMessage(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyGroupChatMessageReqMsg reqMsg) {
    }

    @Override
    public void onNotifySMSAgentNewSMS(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifySMSAgentNewSMSReqMsg reqMsg) {
    }

    @Override
    public void onDeviceFriendChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyFriendChangedReqMsg reqMsg) {
    }

    @Override
    public void onChatGroupMemberChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyChatGroupMemberChangedReqMsg reqMsg) {
    }

    @Override
    public void onNotifySosCallOrderChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifySosCallOrderChangedReqMsg reqMsg) {
    }

    @Override
    public void onNotProcessedPkt(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull OnResponseSetter responseSetter) {
    }


    @Override
    public void onCurrentBabyChanged(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        if (!isSticky)
            mCurrentBabyBean = newBabyBean;
        if (newBabyBean == null) {
            mDeviceId = null;
        } else {
            mDeviceId = newBabyBean.getDeviceId();
        }

        if (newBabyBean == null) {
            if (finishWhenNoMoreBaby(oldBabyBean, isSticky)) {
                L.w(getClass().getSimpleName(), "No more baby!!! Activity finish");
                finish();
            }
        } else {
            if (oldBabyBean != null && !oldBabyBean.getDeviceId().equals(newBabyBean.getDeviceId())) {
                if (finishWhenCurrentBabySwitched(oldBabyBean, newBabyBean, isSticky)) {
                    L.w(getClass().getSimpleName(), "Current baby switched!!! Activity finish");
                    finish();
                }
            }
        }
    }

    @Override
    public void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo) {
    }

    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return true;
    }

    protected boolean finishWhenNoMoreBaby(@Nullable BabyEntity oldBabyBean, boolean isSticky) {
        return true;
    }

    @Override
    public void onBindRequest(@NonNull String toUser, @NonNull Pkt reqPkt, @NonNull Message.NotifyAdminBindDevReqMsg reqMsg, @NonNull Message.FetchUsrDevParticRspMsg usrDevPartic) {
        if (isForeground || isOpeningActivity) {
            Intent intent = new Intent(this, BindRequestActivity.class);
            intent.putExtra(BindRequestActivity.PARAM_KEY_BINDREQ_SEQ, reqPkt.seq.toString());
            intent.putExtra(BindRequestActivity.PARAM_KEY_BINDREQ_PROTOBUF_MSG, reqMsg);
            intent.putExtra(BindRequestActivity.PARAM_KEY_FROM_USER_INFO_PROTOBUF_MSG, usrDevPartic);
            intent.putExtra(BindRequestActivity.PARAM_KEY_TO_USER_ID, toUser);
            startActivity(intent);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onExtrudedLoggedOut(Event.ExtrudedLoggedOut ev) {
        if (shouldShowExtrudedLoggedOut()) {
            startActivity(new Intent(this, ExtrudedLoggedoutDialogActivity.class));
        }
    }

    protected boolean shouldShowExtrudedLoggedOut() {
        return true;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onServerApiNotCompat(Event.ServerApiNotCompat ev) {
        if (shouldShowServerApiNotCompat()) {
            startActivity(new Intent(this, ServerApiNotCompatDialogActivity.class));
        }
    }

    protected boolean shouldShowServerApiNotCompat() {
        return true;
    }
}
