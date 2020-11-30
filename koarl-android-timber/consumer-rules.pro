##
## Proguard-Rules for kotlinx.serialization, as taken from https://github.com/Kotlin/kotlinx.serialization
##
#-keepattributes *Annotation*, InnerClasses
#-dontnote kotlinx.serialization.SerializationKt
#-keep,includedescriptorclasses class dev.moetz.koarl.**$$serializer { *; } # <-- change package name to your app's
#-keepclassmembers class dev.moetz.koarl.** { # <-- change package name to your app's
#    *** Companion;
#}
#-keepclasseswithmembers class dev.moetz.koarl.** { # <-- change package name to your app's
#    kotlinx.serialization.KSerializer serializer(...);
#}