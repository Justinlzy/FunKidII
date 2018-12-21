package com.cqkct.FunKidII.Utils;

import android.content.Context;
import android.text.TextUtils;

import com.cqkct.FunKidII.R;

public class RelationUtils {
    /** 宝贝关系: 爸爸 */
    public static final String RELATION_BABA    = "\001";
    /** 宝贝关系: 妈妈 */
    public static final String RELATION_MAMA    = "\002";
    /** 宝贝关系: 姐姐 */
    public static final String RELATION_JIEJIE  = "\003";
    /** 宝贝关系: 爷爷 */
    public static final String RELATION_YEYE    = "\004";
    /** 宝贝关系: 奶奶 */
    public static final String RELATION_NAINAI  = "\005";
    /** 宝贝关系: 哥哥 */
    public static final String RELATION_GEGE    = "\006";
    /** 宝贝关系: 外公 */
    public static final String RELATION_WAIGONG = "\007";
    /** 宝贝关系: 外婆 */
    public static final String RELATION_WAIPO   = "\010";
    /** 宝贝关系: 老师 */
    public static final String RELATION_LAOSHI  = "\011";

    public static String decodeRelation(Context ctx, String rawRelation) {
        if (TextUtils.isEmpty(rawRelation))
            return rawRelation;

        String[] list = ctx.getResources().getStringArray(R.array.decode_relation);
        int idx = -1;
        if (rawRelation.length() == 1)
            idx = rawRelation.charAt(0);
        if (idx >= 1 && idx <= 9)
            return list[idx - 1];

        return rawRelation;
    }

    public static int getIconResId(String relationStr) {
        if (!TextUtils.isEmpty(relationStr)) {
            switch (relationStr) {
                case "\001":
                    return R.drawable.relation_father;
                case "\002":
                    return R.drawable.relation_mother;
                case "\003":
                    return R.drawable.relation_sister;
                case "\004":
                    return R.drawable.relation_yeye;
                case "\005":
                    return R.drawable.relation_nainai;
                case "\006":
                    return R.drawable.relation_brother;
                case "\007":
                    return R.drawable.relation_waigong;
                case "\010":
                    return R.drawable.relation_waipo;
                case "\011":
                    return R.drawable.relation_teacher;
                default:
                    return R.drawable.head_relation;
            }
        }
        return R.drawable.head_relation;
    }

    public static boolean isCustomRelation(String relationStr) {
        if (!TextUtils.isEmpty(relationStr)) {
            switch (relationStr) {
                case "\001":
                case "\002":
                case "\003":
                case "\004":
                case "\005":
                case "\006":
                case "\007":
                case "\010":
                case "\011":
                    return false;
                default:
                    return true;
            }
        }
        return true;
    }
}