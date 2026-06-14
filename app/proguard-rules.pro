# Keep generic signatures — required for Gson TypeToken to work after R8
-keepattributes Signature
-keepattributes *Annotation*

# Gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# App data classes serialized by Gson
-keep class com.tripcalc.app.** { *; }
