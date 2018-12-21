package com.cqkct.FunKidII.Utils;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;

public class AndroidPermissions {
    private static final String TAG = AndroidPermissions.class.getSimpleName();

    /**
     * 是否需要弹出引导用户设置权限的对话框
     */
    public static boolean shouldShowGuide(@NonNull Activity activity, @NonNull String permission) {
        // 根据 shouldShowRequestPermissionRationale 的行为来判断是否需要引导用户进入设置去修改APP权限

        // 0. 准备 SharedPreferences，我们稍后会用到它
        SharedPreferences preferences = activity.getSharedPreferences("AndroidPermissions", Context.MODE_PRIVATE);
        // 1. 取得 shouldShowRequestPermissionRationale 的返回值
        boolean shouldShowRequest = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
        // 2. 取得 SharedPreferences 里之前存储的 shouldShowRequestPermissionRationale 值
        boolean saved = preferences.getBoolean(permission, false);
        // 3. 通过之前存储的 shouldShowRequestPermissionRationale 和当前的 shouldShowRequestPermissionRationale 值来判断是否需要弹框引导用户
        boolean shouldShowGuide = saved && !shouldShowRequest;
        if (!shouldShowGuide) {
            // 不需要弹框引导用户，存储当权的 shouldShowRequestPermissionRationale 值
            preferences.edit().putBoolean(permission, shouldShowRequest).apply();
        }

        return shouldShowGuide;
    }

    public static boolean shouldShowGuide(@NonNull Fragment fragment, @NonNull String permission) {
        // 根据 shouldShowRequestPermissionRationale 的行为来判断是否需要引导用户进入设置去修改APP权限

        // 0. 准备 SharedPreferences，我们稍后会用到它
        SharedPreferences preferences = fragment.getContext().getSharedPreferences("AndroidPermissions", Context.MODE_PRIVATE);
        // 1. 取得 shouldShowRequestPermissionRationale 的返回值
        boolean shouldShowRequest = fragment.shouldShowRequestPermissionRationale(permission);
        // 2. 取得 SharedPreferences 里之前存储的 shouldShowRequestPermissionRationale 值
        boolean saved = preferences.getBoolean(permission, false);
        // 3. 通过之前存储的 shouldShowRequestPermissionRationale 和当前的 shouldShowRequestPermissionRationale 值来判断是否需要弹框引导用户
        boolean shouldShowGuide = saved && !shouldShowRequest;
        if (!shouldShowGuide) {
            // 不需要弹框引导用户，存储当权的 shouldShowRequestPermissionRationale 值
            preferences.edit().putBoolean(permission, shouldShowRequest).apply();
        }

        return shouldShowGuide;
    }

    public static @NonNull Intent permissionSettingPageIntent(@NonNull Context context) {
        Intent intent;

        switch (Rom.getName()) {
//            case Rom.ROM_EMUI: {
//                intent = new Intent();
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                intent.putExtra("packageName", context.getPackageName());
//                ComponentName comp = new ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity");
//                intent.setComponent(comp);
//            }
//                break;

            case Rom.ROM_FLYME: {
                intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.putExtra("packageName", context.getPackageName());
            }
                break;

//            case Rom.ROM_MIUI: {
//                intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
//                ComponentName comp = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
//                intent.setComponent(comp);
//                intent.putExtra("extra_pkgname", context.getPackageName());
//            }
//                break;

//            case Rom.ROM_OPPO: {
//                intent = new Intent();
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                intent.putExtra("packageName", context.getPackageName());
//                ComponentName comp = new ComponentName("com.color.safecenter", "com.color.safecenter.permission.PermissionManagerActivity");
//                intent.setComponent(comp);
//            }
//                break;

//            case Rom.ROM_QIKU: {
//                intent = new Intent("android.intent.action.MAIN");
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                intent.putExtra("packageName", context.getPackageName());
//                ComponentName comp = new ComponentName("com.qihoo360.mobilesafe", "com.qihoo360.mobilesafe.ui.index.AppEnterActivity");
//                intent.setComponent(comp);
//            }
//                break;

            default: {
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
            }
                break;
        }

        return intent;
    }

    public static boolean hasPermission(@NonNull Context context, @NonNull String permission) {
        // For Android < Android M, self permissions are always granted.
        boolean result = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                int targetSdkVersion = info.applicationInfo.targetSdkVersion;
                if (targetSdkVersion >= Build.VERSION_CODES.M) {
                    // targetSdkVersion >= Android M, we can use Context#checkSelfPermission
                    result = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
                } else {
                    // targetSdkVersion < Android M, we have to use PermissionChecker
                    result = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED;
                }
            } catch (Exception e) {
                L.e(TAG, "hasPermission() failure", e);
                result = false;
            }
        }

        return result;
    }


    public static boolean hasCameraPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (Rom.isFlyme()) {
            if (isCameraUsable()) {
                return true;
            }
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    public static boolean isCameraUsable() {
        boolean canUse = true;
        Camera mCamera = null;
        try {
            mCamera = Camera.open();
            // setParameters 是针对魅族MX5。MX5通过Camera.open()拿到的Camera对象不为null
            Camera.Parameters mParameters = mCamera.getParameters();
            mCamera.setParameters(mParameters);
        } catch (Exception e) {
            canUse = false;
        } finally {
            if (mCamera != null) {
                mCamera.release();
            }
        }
        return canUse;
    }

    public static boolean isLocationServiceEnable(Context context) {
        try {
            int locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } catch (Exception e) {
            L.d(TAG, "Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE)", e);
        }
        return false;
    }
}