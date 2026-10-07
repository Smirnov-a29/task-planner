# Правила ProGuard по умолчанию. В debug-сборке обфускация отключена.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.example.taskplanner.**$$serializer { *; }
-keepclassmembers class com.example.taskplanner.** {
    *** Companion;
}
