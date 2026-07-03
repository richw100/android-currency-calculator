# Keep generic signatures — required for Gson TypeToken to work after R8
-keepattributes Signature
-keepattributes *Annotation*

# Gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# App data classes serialized by Gson (DataStore JSON + Retrofit converter) —
# field names must survive R8 or existing persisted JSON stops deserializing
-keep class com.tripcalc.app.ui.CardProfile { *; }
-keep class com.tripcalc.app.ui.CustomRateEntry { *; }
-keep class com.tripcalc.app.ui.HistoryEntry { *; }
-keep class com.tripcalc.app.data.ExchangeRateResponse { *; }
-keep class com.tripcalc.app.data.CountryLocalisationInfo { *; }
-keep class com.tripcalc.app.data.PlugInfo { *; }
-keep class com.tripcalc.app.data.DrivingInfo { *; }
