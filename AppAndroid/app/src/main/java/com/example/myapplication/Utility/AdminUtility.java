package com.example.myapplication.Utility;

import static com.example.myapplication.Model.Constants.dbRoot;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.Model.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Gestisce il nodo "admin-pw" su Firebase: la password dell'area di gestione,
 * letta in tempo reale e tenuta in {@link Constants#password}.
 *
 * Sta fuori dal codice per due motivi:
 *  - la si cambia dalla console Firebase senza ripubblicare l'app;
 *  - è la stessa sorgente condivisa con la web app.
 *
 * ⚠️ Non è una misura di sicurezza: chi apre il traffico Firebase vede il
 * valore in chiaro. È solo un gate a un tocco per non entrare per sbaglio
 * nell'area di gestione; la protezione vera restano le regole del Realtime
 * Database.
 */
public class AdminUtility {

    private static final String ADMIN_PW_NODE = dbRoot + "admin-pw";
    private static ValueEventListener adminPwListener;
    private static DatabaseReference adminPwRef;

    /** Inizia ad ascoltare il nodo admin-pw e aggiorna Constants.password. */
    public static void startListening() {
        adminPwRef = FirebaseDatabase.getInstance().getReference(ADMIN_PW_NODE);

        adminPwListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String value = snapshot.getValue(String.class);
                // null finché il valore non è arrivato: il login lo rifiuta
                Constants.password = (value != null && !value.isEmpty()) ? value : null;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminUtility", "Errore listener admin-pw", error.toException());
            }
        };

        adminPwRef.addValueEventListener(adminPwListener);
    }

    /** Ferma l'ascolto. */
    public static void stopListening() {
        if (adminPwRef != null && adminPwListener != null) {
            adminPwRef.removeEventListener(adminPwListener);
            adminPwListener = null;
        }
    }
}
