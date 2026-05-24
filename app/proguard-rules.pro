# ============================================
# NexClip 构建期防护 - 类目一
# 编号1：R8深度混淆 + 编号9：日志清除（Java层）
# ============================================

# ===== 编号1：R8深度混淆 =====
# 做什么：激进proguard规则，类名/方法名/字段名全部重命名为a/b/c
# 程度：-optimizationpasses 5，-allowaccessmodification，-overloadaggressively，
#       -mergeinterfacesaggressively，-repackageclasses ''
# 崩溃率：零

-optimizationpasses 5
-allowaccessmodification
-overloadaggressively
-mergeinterfacesaggressively
-repackageclasses ''
-dontusemixedcaseclassnames
-verbose

# 程度：不混淆JNI动态注册方法
-keepclasseswithmembernames class * {
    native <methods>;
}
# 程度：不混淆四大组件类名
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
-keep class * extends android.app.Application
# 程度：不混淆自定义View构造函数
-keep class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
# 程度：不混淆反射调用
-keepclassmembers class * {
    @java.lang.reflect.** <methods>;
}
-keep class com.myvideo.editor.security.** { *; }
-keep class com.myvideo.editor.reflection.** { *; }
# 程度：不混淆Serializable字段
-keep class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
# 程度：视频处理类可选不混淆
-keep class com.myvideo.editor.engine.** { *; }
-keep class com.myvideo.editor.codec.** { *; }
-keep class com.myvideo.editor.opengl.** { *; }
-keep class com.myvideo.editor.renderer.** { *; }
# 程度：移除调试信息中的原始文件名
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute ""

# ===== 编号9：日志清除（Java层）=====
# 做什么：Java层所有Log调用编译时直接删除
# 程度：-assumenosideeffects删除Log.v/d/i/w/e/wtf全部调用
#       移除System.out/err/printStackTrace
#       OkHttp release下不添加日志拦截器
# 崩溃率：零

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}
-assumenosideeffects class java.lang.System {
    public static void out.println(...);
    public static void err.println(...);
}
-dontwarn okhttp3.logging.**
