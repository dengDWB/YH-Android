# 指定代码的压缩级别
-optimizationpasses 5
# 包明不混合大小写
-dontusemixedcaseclassnames
# 不去忽略非公共的库类
-dontskipnonpubliclibraryclasses
# 不优化输入的类文件
-dontoptimize
# 预校验
-dontpreverify
# 混淆时是否记录日志
-verbose

# LeakCanary
-keep class org.eclipse.mat.** { *; }
-keep class com.squareup.leakcanary.** { *; }
-dontwarn com.pgyersdk.**
-keep class com.pgyersdk.** { *; }

# umengpush
-dontwarn com.taobao.**
-dontwarn anet.channel.**
-dontwarn anetwork.channel.**
-dontwarn org.android.**
-dontwarn org.apache.thrift.**
-dontwarn com.umeng.**
-dontwarn com.xiaomi.**
-dontwarn com.huawei.**
-dontwarn android.support.**
-dontwarn com.handmark.**
-dontwarn okio.**

-keepattributes *Annotation*

-keep class com.taobao.** {*;}
-keep class org.android.** {*;}
-keep class anet.channel.** {*;}
-keep class com.umeng.** {*;}
-keep class com.xiaomi.** {*;}
-keep class com.huawei.** {*;}
-keep class org.apache.thrift.** {*;}

-keep class com.alibaba.sdk.android.**{*;}
-keep class com.ut.**{*;}
-keep class com.ta.**{*;}

-keep public class **.R$*{
   public static final int *;
}

# umeng analytics

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}

# 对第三方库中的类不进行混淆

# -libraryjars libs/SocialSDK_WeiXin_1.jar
# -libraryjars libs/SocialSDK_WeiXin_2.jar
# -libraryjars libs/umeng-analytics-v5.5.3.jar
# -libraryjars libs/umeng_social_sdk.jar
# -libraryjars libs/android-viewbadger.jar

-keep class com.intfocus.yonghuitest.SubjectActivity

# 对自定义控件类不进行混淆

-keep class com.intfocus.yonghuitest.view.TabView
-keep class com.intfocus.yonghuitest.view.CircleImageView

# 对使用了 Gson 之类的工具不进行混淆

-keep class com.intfocus.yonghuitest.BarCodeResultActivity
-keep class com.intfocus.yonghuitest.util.K
-keep class com.intfocus.yonghuitest.util.URLs



