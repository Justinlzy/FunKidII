
package com.cqkct.FunKidII.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.widget.TextView;

import com.cqkct.FunKidII.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EmotionUtils {

    public static List<String> EMOTICON_NAME_LIST = new ArrayList<String>() {
        {
            add("ok");
            add("eat");
            add("kit");
            add("up");
            add("night");
            add("miss");
            add("thank");
            add("good");
            add("work");
        }
    };

    public static HashMap<String, Integer> EMOTION_CLASSIC_MAP = new HashMap<String, Integer>() {
        {
            put("ok", R.drawable.expression_ok_big);
            put("eat", R.drawable.expression_eat_big);
            put("kit", R.drawable.expression_cute_big);
            put("up", R.drawable.expression_wake_big);
            put("night", R.drawable.expression_sleep_big);
            put("miss", R.drawable.expression_miss_big);
            put("thank", R.drawable.expression_smile_big);
            put("good", R.drawable.expression_good_big);
            put("work", R.drawable.expression_homework_big);
        }
    };

    public static HashMap<String, Integer> EMOTION_DESC_STRING_MAP = new HashMap<String, Integer>() {
        {
            put("ok", R.string.emoticon_ok);
            put("eat", R.string.emoticon_meal);
            put("kit", R.string.emoticon_cute);
            put("up", R.string.emoticon_get_up);
            put("night", R.string.emoticon_good_night);
            put("miss", R.string.emoticon_love);
            put("thank", R.string.emoticon_thanks);
            put("good", R.string.emoticon_good);
            put("work", R.string.emoticon_homework);
        }
    };


    /**
     * 表情类型标志符
     */
    public static final int EMOTION_CLASSIC_TYPE = 0x0001;//经典表情

    /**
     * key-表情文字;
     * value-表情图片资源
     */
    public static HashMap<String, Integer> EMPTY_MAP = new HashMap<>();

    /**
     * 根据名称获取当前表情图标R值
     *
     * @param EmotionType 表情类型标志符
     * @param imgName     名称
     * @return
     */
    public static int getImgByName(int EmotionType, String imgName) {
        Integer integer = null;
        switch (EmotionType) {
            case EMOTION_CLASSIC_TYPE:
                integer = EMOTION_CLASSIC_MAP.get(imgName);
                break;
            default:
                L.e("the emojiMap is null!! Handle Yourself ");
                break;
        }
        return integer == null ? -1 : integer;
    }

    /**
     * 根据类型获取表情数据
     *
     * @param EmotionType
     * @return
     */
    public static HashMap<String, Integer> getEmojiMap(int EmotionType) {
        HashMap EmojiMap = null;
        switch (EmotionType) {
            case EMOTION_CLASSIC_TYPE:
                EmojiMap = EMOTION_CLASSIC_MAP;
                break;
            default:
                EmojiMap = EMPTY_MAP;
                break;
        }
        return EmojiMap;
    }

    /**
     * 文本中的emojb字符处理为表情图片
     */

    public static SpannableString getEmotionContent(int emotion_map_type, final Context context, final TextView tv, String source) {
        SpannableString spannableString = new SpannableString(source);
        Resources res = context.getResources();

        String regexEmotion = "\\[([\u4e00-\u9fa5\\w])+\\]";
        Pattern patternEmotion = Pattern.compile(regexEmotion);
        Matcher matcherEmotion = patternEmotion.matcher(spannableString);

        while (matcherEmotion.find()) {
            // 获取匹配到的具体字符
            String key = matcherEmotion.group();
            // 匹配字符串的开始位置
            int start = matcherEmotion.start();
            // 利用表情名字获取到对应的图片
            Integer imgRes = EmotionUtils.getImgByName(emotion_map_type, key);
            if (imgRes != null) {
                // 压缩表情图片
                int size = (int) tv.getTextSize() * 13 / 10;
                Bitmap bitmap = BitmapFactory.decodeResource(res, imgRes);
                Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);

                ImageSpan span = new ImageSpan(context, scaleBitmap);
                spannableString.setSpan(span, start, start + key.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return spannableString;
    }
}
