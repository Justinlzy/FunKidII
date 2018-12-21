package com.cqkct.FunKidII.Utils;

import android.content.Context;
import android.text.TextUtils;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;

import java.util.List;

import protocol.Message;

public class UserPermission {
    private static final String TAG = UserPermission.class.getSimpleName();

    public static boolean hasEditPermission(int permission) {
        return permission != 0 && permission < Message.UsrDevAssoc.Permission.NORMAL_VALUE;
    }

    public static boolean hasEditPermission(Message.UsrDevAssoc.Permission permission) {
        return hasEditPermission(permission.getNumber());
    }

    public static boolean hasEditPermission(String userId, String deviceId) {
        return hasEditPermission(getPermissionFromDb(userId, deviceId));
    }

    public static boolean hasReadPermission(int permission) {
        return permission != 0 && permission < Message.UsrDevAssoc.Permission.MINI_VALUE;
    }

    public static boolean hasReadPermission(Message.UsrDevAssoc.Permission permission) {
        return hasReadPermission(permission.getNumber());
    }

    public static boolean hasReadPermission(String userId, String deviceId) {
        return hasReadPermission(getPermissionFromDb(userId, deviceId));
    }

    private static int getPermissionFromDb(String userId, String deviceId) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(deviceId)) {
            L.d(TAG, "getPermissionFromDb failure: userId: " + userId + " deviceId: " + deviceId);
            return 0;
        }
        List<BabyEntity> list = GreenUtils.getBabyEntityDao().queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(userId))
                .where(BabyEntityDao.Properties.DeviceId.eq(deviceId))
                .build().list();
        if (list.isEmpty()) {
            L.d(TAG, "getPermissionFromDb failure: userId: " + userId + " deviceId: " + deviceId + " not found");
            return 0;
        }
        Integer permission = list.get(0).getPermission();
        L.d(TAG, "getPermissionFromDb userId: " + userId + " deviceId: " + deviceId + " permission: " + permission);
        if (permission == null)
            return 0;
        return permission;
    }

    public static String toRoleString(Context context, int permission) {
        String[] list = context.getResources().getStringArray(R.array.user_roles);
        if (permission < 0 || permission > Message.UsrDevAssoc.Permission.MINI_VALUE) {
            permission = 0;
        }
        return list[permission];
    }

    public static String toRoleString(Context context, Message.UsrDevAssoc.Permission permission) {
        return toRoleString(context, permission.getNumber());
    }

    public static String toPermissionString(Context context, int permission) {
        String[] list = context.getResources().getStringArray(R.array.user_permissions);
        if (permission < 0 || permission > Message.UsrDevAssoc.Permission.MINI_VALUE) {
            permission = 0;
        }
        return list[permission];
    }

    public static String toPermissionString(Context context, Message.UsrDevAssoc.Permission permission) {
        return toPermissionString(context, permission.getNumber());
    }
}
