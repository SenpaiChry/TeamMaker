# ============================================================================
# TeamMaker ProGuard/R8 rules
# ============================================================================
# In release: isMinifyEnabled=true + isShrinkResources=true.
# Chi rompe cosa se rimuovi queste regole:
#   - Firebase/GMS         -> deserializzazione dati Firebase, listener realtime
#   - fastexcel            -> export .xlsx della lista giocatori
#   - data.model + enum    -> se un giorno usi Firebase.getValue(Modello.class)
#   - ui.common (Views)    -> layout XML che referenzia com.teammaker.app.ui.common.*
# ============================================================================

# Tieni nome file + riga negli stack trace: crash leggibili anche dopo l'offuscamento.
-keepattributes SourceFile,LineNumberTable

# ---------- Firebase / Google Play Services ----------
# Firebase usa riflessione interna per Auth, Database, callback, listener.
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ---------- fastexcel (export .xlsx) ----------
-keep class org.dhatim.fastexcel.** { *; }
-dontwarn org.dhatim.fastexcel.**

# ---------- Modelli TeamMaker ----------
# Oggi usiamo mapper manuali (MatchMapper/TeamMapper/StatMapper) quindi
# in teoria potremmo lasciar rinominare. Ma se in futuro qualcuno usa
# DataSnapshot.getValue(Modello.class), Firebase cerca i campi per nome:
# tenere campi e costruttori evita ore di debug su dati "misteriosamente null".
-keep class com.teammaker.app.data.model.** { *; }
-keepclassmembers class com.teammaker.app.data.model.** {
    <init>(...);
    *;
}

# ---------- Enum (deserializzazione) ----------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- Parcelable (nel caso qualche modello lo implementi) ----------
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ---------- Custom View referenziate in XML layout ----------
# MaxHeightScrollView, TopToast, VerticalSpacingItemDecoration:
# AAPT le istanzia con i costruttori (Context, AttributeSet).
-keep public class com.teammaker.app.ui.common.** {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}
