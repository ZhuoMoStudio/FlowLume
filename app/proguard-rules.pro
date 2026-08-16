# LibGDX
-keep class com.badlogic.gdx.** { *; }
-keepclassmembers class com.badlogic.gdx.graphics.glutils.ShaderProgram { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.zhuomo.flowlume.config.** {
    kotlinx.serialization.KSerializer serializer(...);
}
