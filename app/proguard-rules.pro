# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Administrator\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
-assumenosideeffects class com.cqkct.FunKidII.Utils.L {
    public static int v(...);
    public static int d(...);
}



# SMSSDK
-keep class cn.smssdk.**{*;}
-keep class com.mob.**{*;}
-dontwarn com.mob.**
-dontwarn cn.smssdk.**



# zxing
-dontwarn com.google.zxing.**
-keep class com.google.zxing.** { *; }



# 高德地图
# 3D 地图 V5.0.0之后：
-keep   class com.amap.api.maps.**{*;}
-keep   class com.autonavi.**{*;}
-keep   class com.amap.api.trace.**{*;}
# 定位
-keep class com.amap.api.location.**{*;}
-keep class com.amap.api.fence.**{*;}
-keep class com.autonavi.aps.amapapi.model.**{*;}
# 搜索
-keep   class com.amap.api.services.**{*;}
# 2D地图
-keep class com.amap.api.maps2d.**{*;}
-keep class com.amap.api.mapcore2d.**{*;}
# 导航
-keep class com.amap.api.navi.**{*;}
-keep class com.autonavi.**{*;}



# gson && protobuf
-dontwarn com.google.**
-keep class com.google.gson.** {*;}
-keep class com.google.protobuf.** {*;}
-keep public class * extends com.google.protobuf.** { *; }



# JPush
-dontwarn cn.jpush.**
-keep class cn.jpush.** { *; }
-keep class * extends cn.jpush.android.helpers.JPushMessageReceiver { *; }

-dontwarn cn.jiguang.**
-keep class cn.jiguang.** { *; }



# netty
-keepattributes Signature,InnerClasses
-keepclasseswithmembers class io.netty.** { *; }
-dontwarn io.netty.**
-dontwarn sun.**



# pingyin4j
-dontwarn net.soureceforge.pinyin4j.**
-dontwarn demo.**
-keep class net.sourceforge.pinyin4j.** { *;}
-keep class demo.** { *;}
-keep class com.hp.** { *;}



# greenDAO 3
-keepclassmembers class * extends org.greenrobot.greendao.AbstractDao {
public static java.lang.String TABLENAME;
}
-keep class **$Properties

-keep class freemarker.** { *; }
-dontwarn freemarker.**

# If you do not use SQLCipher:
-dontwarn org.greenrobot.greendao.database.**
# If you do not use Rx:
-dontwarn rx.**



# facebook tookkit
-keep class com.facebook.** {*;}
-dontwarn okio.**



# com.baoyz.swipemenulistview
-keep class com.baoyz.swipemenulistview.** {*;}



# com.daimajia.swipelayout
-keep class com.daimajia.swipelayout.** {*;}



# com.commit451
-keep class com.commit451.** {*;}



# bugly 配置权限 start
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.** {*;}



# EventBus
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}



# okhttp
-dontwarn com.squareup.okhttp3.**
-keep class com.squareup.okhttp3.** { *;}
-dontwarn okio.**

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**



# SQLCipher
-keep class net.sqlcipher.** {*;}
-dontwarn net.sqlcipher.**



# sharesdk
-keep class cn.sharesdk.**{*;}
-keep class com.sina.**{*;}
-keep class **.R$* {*;}
-keep class **.R{*;}
-keep class com.mob.**{*;}
-dontwarn com.mob.**
-dontwarn cn.sharesdk.**
-dontwarn **.R$*



# 友盟
-keep class com.umeng.commonsdk.** {*;}
-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}
-keep public class com.cqkct.FunKidII.R$*{
    public static final int *;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}



# google 逆地理编码
-keep class com.cqkct.FunKidII.Bean.google.geocode.**{*;}



# ImmersionBar
-keep class com.gyf.barlibrary.* {*;}



# glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder

# Uncomment for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule



# SwipeRecyclerView
-keep class android.support.v7.widget.helper.* {*;}
-keep class com.yanzhenjie.recyclerview.swipe.* {*;}
