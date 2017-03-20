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
-libraryjars libs/pgyer_sdk_x.x.jar
-dontwarn com.pgyersdk.**
-keep class com.pgyersdk.** { *; }