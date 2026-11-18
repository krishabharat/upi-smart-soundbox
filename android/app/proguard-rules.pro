# Keep Room database and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# Keep models for Gson deserialization
-keep class com.upisoundbox.app.data.model.** { *; }
-keep class com.upisoundbox.app.data.local.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

