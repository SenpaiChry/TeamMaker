package com.example.myapplication.Utility;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.myapplication.R;
import com.google.android.gms.tasks.Task;

/**
 * Un solo punto per attaccare log e feedback utente alle scritture Firebase.
 * Prima ogni chiamante faceva setValue() senza controllo (o con listener vuoti):
 * un fallimento restava silenzioso e i dati locali potevano divergere da Firebase.
 *
 * attach() aggiunge i listener e mostra un Toast di errore (con messaggio custom
 * o default "SALVATAGGIO FALLITO"). Il Context puo' essere null per chiamate di
 * background: in quel caso solo log.
 */
public class FirebaseWriteHelper {

    private static final String TAG = "FirebaseWrite";

    public static Task<Void> attach(@Nullable Context ctx, String opName, Task<Void> task) {
        return attach(ctx, opName, task, 0);
    }

    /**
     * @param failureMsgResId ID stringa opzionale da mostrare in toast su fallimento;
     *                        0 = usa il messaggio generico R.string.error_while_saving_data.
     */
    public static Task<Void> attach(@Nullable Context ctx, String opName, Task<Void> task, int failureMsgResId) {
        if (task == null) return null;

        return task.addOnSuccessListener(v ->
                Log.d(TAG, opName + ": ok")
        ).addOnFailureListener(e -> {
            Log.e(TAG, opName + ": fallito", e);
            if (ctx != null) {
                int msg = failureMsgResId != 0 ? failureMsgResId : R.string.error_while_saving_data;
                Toast.makeText(ctx.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
