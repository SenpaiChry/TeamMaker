package com.example.myapplication.Utility;

import static com.example.myapplication.Model.Constants.dbRoot;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.StatDefinition;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogo delle statistiche: prima era una lista hardcoded in Constants, ora
 * arriva dal nodo teammaker/stats. La lista in memoria e' allineata in tempo
 * reale via listener. Le chiavi dei singoli stat sono push-key Firebase.
 */
public class StatsUtility {

    private static final String NODE = dbRoot + "stats";

    private static final List<StatDefinition> DEFINITIONS = new ArrayList<>();
    private static final List<Runnable> LISTENERS = new ArrayList<>();

    private static ValueEventListener remoteListener;
    private static DatabaseReference remoteRef;

    /** Snapshot delle definizioni, ordinate per campo order. */
    public static List<StatDefinition> getDefinitions() {
        return new ArrayList<>(DEFINITIONS);
    }

    /** Cerca una definizione per key. */
    public static StatDefinition getByKey(String key) {
        for (StatDefinition s : DEFINITIONS) {
            if (s.key.equals(key)) return s;
        }
        return null;
    }

    /** Notificato quando la lista cambia. */
    public static void addChangeListener(Runnable listener) {
        if (!LISTENERS.contains(listener)) LISTENERS.add(listener);
    }

    public static void removeChangeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    public static void startListening() {
        remoteRef = FirebaseDatabase.getInstance().getReference(NODE);
        remoteListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DEFINITIONS.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    StatDefinition def = StatMapper.fromSnapshot(s);
                    if (def != null && def.label != null) DEFINITIONS.add(def);
                }
                DEFINITIONS.sort((a, b) -> Integer.compare(a.order, b.order));
                for (Runnable r : new ArrayList<>(LISTENERS)) r.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StatsUtility", "Errore listener stats", error.toException());
            }
        };
        remoteRef.addValueEventListener(remoteListener);
    }

    public static void stopListening() {
        if (remoteRef != null && remoteListener != null) {
            remoteRef.removeEventListener(remoteListener);
            remoteListener = null;
        }
    }

    /** Aggiunge una nuova stat: genera la push-key, la scrive e la restituisce. */
    public static String addStat(StatDefinition def) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(NODE);
        String key = dbRef.push().getKey();
        def.key = key;
        FirebaseWriteHelper.attach(null, "addStat", dbRef.child(key).setValue(StatMapper.toMap(def)));
        return key;
    }

    /** Aggiorna una stat esistente (la key non cambia mai). */
    public static void updateStat(StatDefinition def) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(NODE + "/" + def.key);
        FirebaseWriteHelper.attach(null, "updateStat", dbRef.setValue(StatMapper.toMap(def)));
    }

    /**
     * Elimina la sola definizione: i valori orfani nei player NON vengono cancellati
     * (safe, recuperabile se sbagli). Semplicemente non verranno piu' mostrati.
     */
    public static void deleteStat(String key) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(NODE + "/" + key);
        FirebaseWriteHelper.attach(null, "deleteStat", dbRef.removeValue());
    }

    /** Aggiorna l'ordine di piu' stat in un'unica scrittura atomica. */
    public static void reorder(List<StatDefinition> newOrder) {
        Map<String, Object> updates = new HashMap<>();
        for (int i = 0; i < newOrder.size(); i++) {
            updates.put(newOrder.get(i).key + "/order", i);
            newOrder.get(i).order = i;
        }
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(NODE);
        FirebaseWriteHelper.attach(null, "reorderStats", dbRef.updateChildren(updates));
    }
}
