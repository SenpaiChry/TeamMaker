package com.teammaker.app.Utility;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Bus interno per disaccoppiare le Utility dalle Activity.
 * Prima ogni Utility chiamava direttamente notifyDataSetChanged/recreate sulle
 * schermate: adesso emette un evento e chi e' interessato (Activity visibile)
 * si registra in onStart / deregistra in onStop.
 *
 * Le callback vengono sempre eseguite sul main thread: le Utility possono
 * chiamare emit anche da un thread di background.
 */
public class DataChangeBus {

    public enum Event { PLAYERS, TOURNAMENTS, MATCHES, TEAMS }

    private static final Map<Event, List<Runnable>> LISTENERS = new EnumMap<>(Event.class);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private DataChangeBus() { }

    public static synchronized void register(Event event, Runnable listener) {
        List<Runnable> list = LISTENERS.get(event);
        if (list == null) {
            list = new ArrayList<>();
            LISTENERS.put(event, list);
        }
        if (!list.contains(listener)) {
            list.add(listener);
        }
    }

    public static synchronized void unregister(Event event, Runnable listener) {
        List<Runnable> list = LISTENERS.get(event);
        if (list != null) {
            list.remove(listener);
        }
    }

    public static void emit(Event event) {
        List<Runnable> snapshot;
        synchronized (DataChangeBus.class) {
            List<Runnable> list = LISTENERS.get(event);
            if (list == null || list.isEmpty()) return;
            // Copia: cosi' un listener che si deregistra durante il dispatch non causa CME.
            snapshot = new ArrayList<>(list);
        }
        for (Runnable r : snapshot) {
            MAIN.post(r);
        }
    }
}
