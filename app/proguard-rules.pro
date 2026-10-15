# Add project specific ProGuard rules here.

# Preserve Line Numbers for Debugging
-keepattributes SourceFile,LineNumberTable,*Annotation*,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# --- Room Database Keep Rules ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.Dao *;
}
-keep class com.example.data.entity.** { *; }
-keepclassmembers class com.example.data.entity.** { *; }
-keep class com.example.data.dao.** { *; }
-keepclassmembers class com.example.data.dao.** { *; }
-keep class com.example.data.db.** { *; }
-keepclassmembers class com.example.data.db.** { *; }
-keep class *_Impl { *; }

# --- ViewModel Keep Rules ---
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class com.example.ui.viewmodel.** { *; }
-keepclassmembers class com.example.ui.viewmodel.** { *; }

# --- Jetpack Compose Keep Rules ---
-keepclassmembers class * extends androidx.compose.ui.node.LayoutNode { *; }
-dontwarn androidx.compose.**

# --- Data Entities, Repository & API Models ---
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }

# --- Security & Keystore Utilities ---
-keep class com.example.data.api.security.** { *; }
-keep class com.example.util.SecurityUtils { *; }

# --- Moshi & Retrofit Keep Rules ---
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers class * {
    @retrofit2.http.** *;
}


