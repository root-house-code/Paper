# kotlinx.serialization
-keepclassmembers class com.paper.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.paper.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
