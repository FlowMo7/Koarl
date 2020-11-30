##
## Proguard-Rules for kotlinx.serialization, as taken from https://github.com/Kotlin/kotlinx.serialization
##
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class dev.moetz.koarl.**$$serializer { *; }
-keepclassmembers class dev.moetz.koarl.** {
    *** Companion;
}
-keepclasseswithmembers class dev.moetz.koarl.** {
    kotlinx.serialization.KSerializer serializer(...);
}