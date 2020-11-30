# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-optimizationpasses 5
-verbose
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**

#for disabling logging completely at compilation:
-assumevalues class dev.moetz.koarl.android.KoarlLogger {
  static boolean enabled return false;
}

#for debugging:
#-dontobfuscate