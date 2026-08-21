# Keep Samsung Health Data SDK public APIs when this library is consumed.
-keep public class com.samsung.android.sdk.health.data.** { public protected *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
