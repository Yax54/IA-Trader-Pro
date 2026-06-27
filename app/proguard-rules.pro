# =============================================================================
# AI Trader Pro — ProGuard / R8 rules complètes
# Généré suite audit crash V1.7.1 — TOUTES les dépendances protégées
# =============================================================================

# ─── 1. ROOM DATABASE ─────────────────────────────────────────────────────────
# Les entités Room et leurs DAOs sont introspectés par réflexion à runtime
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}
# Conserver les migrations Room (accédées par réflexion)
-keepnames class androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }

# ─── 2. DATASTORE / PREFERENCES ───────────────────────────────────────────────
# DataStore utilise la réflexion pour sérialiser les Preferences
-keep class androidx.datastore.** { *; }
-keepclassmembers class * {
    @androidx.datastore.preferences.core.* *;
}

# ─── 3. KOTLIN SERIALIZATION (Kotlinx) ────────────────────────────────────────
# Les classes @Serializable sont introspectées à runtime par le plugin
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    static final long serialVersionUID;
    private static final kotlinx.serialization.descriptors.SerialDescriptor serialVersionUID;
    static kotlinx.serialization.KSerializer serializer(...);
    <fields>;
}
-keep class kotlinx.serialization.** { *; }
-keep class kotlin.Metadata { *; }

# ─── 4. RETROFIT + OKHTTP ─────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep interface com.privateinvest.aitraderpro.network.** { *; }
-keep class com.privateinvest.aitraderpro.data.model.** { *; }

# ─── 5. VIEWMODEL + LIFECYCLE ─────────────────────────────────────────────────
# ViewModelProvider.Factory accède aux ViewModels par réflexion
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class androidx.lifecycle.** { *; }

# ─── 6. ENUMS (Navigation + Repositories) ─────────────────────────────────────
# Les enums Kotlin sont accédés par valueOf() à runtime — CRITIQUE pour la navigation
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    *;
}

# ─── 7. COMPOSE + NAVIGATION ──────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }
-dontwarn androidx.compose.**

# ─── 8. WORKMANAGER ───────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }

# ─── 9. BIOMETRIC ─────────────────────────────────────────────────────────────
-keep class androidx.biometric.** { *; }

# ─── 10. COROUTINES ───────────────────────────────────────────────────────────
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ─── 11. CLASSES INTERNES DU PROJET ───────────────────────────────────────────
# ServiceLocator, Application, MainActivity — ne jamais obfusquer
-keep class com.privateinvest.aitraderpro.AITraderApplication { *; }
-keep class com.privateinvest.aitraderpro.MainActivity { *; }
-keep class com.privateinvest.aitraderpro.ServiceLocator { *; }

# Entités Room du projet
-keep class com.privateinvest.aitraderpro.database.** { *; }

# Repositories — accédés par ServiceLocator
-keep class com.privateinvest.aitraderpro.repository.** { *; }

# ViewModels du projet — accédés par réflexion via Factory
-keep class com.privateinvest.aitraderpro.viewmodel.** { *; }

# Navigation — destinations et stores
-keep class com.privateinvest.aitraderpro.navigation.** { *; }

# Workers
-keep class com.privateinvest.aitraderpro.worker.** { *; }

# ─── 12. KOTLIN METADATA (requis pour réflexion Kotlin) ───────────────────────
-keepattributes RuntimeVisibleAnnotations
-keepattributes AnnotationDefault
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ─── 13. GSON / JAVA REFLECTION (sécurité) ────────────────────────────────────
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable

# ─── 14. NOTIFICATIONS ────────────────────────────────────────────────────────
-keep class com.privateinvest.aitraderpro.notifications.** { *; }

# ─── 15. SUPPRESSION WARNINGS INUTILES ────────────────────────────────────────
-dontwarn com.google.errorprone.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
