package com.example.myapplication.Utility;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Gate admin via Firebase Authentication.
 *
 * Prima la password admin era un nodo in chiaro nel DB (admin-pw) e il confronto
 * era client-side: chiunque avesse letto il DB (chiunque, dato che le regole erano
 * aperte) vedeva la password. Ora l'admin è un utente Firebase Auth reale; il
 * DB è protetto da regole che accettano scritture solo se auth.uid corrisponde a
 * un admin registrato nel nodo teammaker/admins.
 *
 * UX invariata: l'utente digita SOLO la password. L'email dell'admin è fissa
 * (ADMIN_EMAIL) e nascosta nel codice.
 */
public class AdminUtility {

    /** Email fissa dell'account admin, hardcoded perche' non deve essere digitata. */
    public static final String ADMIN_EMAIL = "admin@teammaker.local";

    /** Callback dell'esito login: onSuccess() = ok, onFailure(msg) = errore. */
    public interface AuthCallback {
        void onSuccess();
        void onFailure(String message);
    }

    /** Prova a fare login come admin con la password inserita. */
    public static void signIn(@NonNull String password, @NonNull AuthCallback cb) {
        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(ADMIN_EMAIL, password)
                .addOnSuccessListener(result -> {
                    Log.d("AdminUtility", "Login admin ok: " + result.getUser().getUid());
                    cb.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w("AdminUtility", "Login admin fallito", e);
                    cb.onFailure(e.getLocalizedMessage());
                });
    }

    /** True se l'utente corrente è loggato come admin. */
    public static boolean isAdmin() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null && ADMIN_EMAIL.equalsIgnoreCase(user.getEmail());
    }

    /** Esce dalla sessione admin. */
    public static void signOut() {
        FirebaseAuth.getInstance().signOut();
    }
}
