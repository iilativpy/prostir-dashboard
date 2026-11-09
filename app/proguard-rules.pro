# kotlinx.serialization: keep generated serializers of API models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.prostir.dashboard.data.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.prostir.dashboard.data.model.**$$serializer { *; }
