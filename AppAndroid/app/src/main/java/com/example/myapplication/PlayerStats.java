package com.example.myapplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Valori delle statistiche di un giocatore, indicizzati per key della stat.
 *
 * Prima era una HashMap<String, Object> con conversioni Float.parseFloat sparse
 * ovunque; ora la conversione da Firebase (Long/Double/String) a float avviene
 * UNA sola volta in ingresso, e in memoria il tipo e' garantito.
 */
public class PlayerStats {

    private final HashMap<String, Float> values;

    public PlayerStats() {
        this.values = new HashMap<>();
    }

    public PlayerStats(PlayerStats other) {
        this.values = new HashMap<>(other.values);
    }

    /** Valore per la key: 0 se assente o non parsabile. */
    public float get(String key) {
        Float v = values.get(key);
        return v != null ? v : 0f;
    }

    public void set(String key, float value) {
        values.put(key, value);
    }

    /** Legge un valore "grezzo" (Long/Double/String/Boolean) e lo converte in float. */
    public void setFromRaw(String key, Object raw) {
        Float parsed = parseFloat(raw);
        if (parsed != null) values.put(key, parsed);
    }

    public boolean containsKey(String key) { return values.containsKey(key); }
    public int size() { return values.size(); }
    public boolean isEmpty() { return values.isEmpty(); }

    public Set<String> keySet() { return Collections.unmodifiableSet(values.keySet()); }
    public Set<Map.Entry<String, Float>> entrySet() {
        return Collections.unmodifiableSet(values.entrySet());
    }

    /** Serializzazione per Firebase (chiavi → numero, come oggi). */
    public HashMap<String, Object> toRawMap() {
        HashMap<String, Object> out = new HashMap<>(values.size());
        for (Map.Entry<String, Float> e : values.entrySet()) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    /** Costruisce da una mappa grezza Firebase, filtrando valori non numerici. */
    public static PlayerStats fromRawMap(Map<String, Object> raw) {
        PlayerStats stats = new PlayerStats();
        if (raw == null) return stats;
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            Float parsed = parseFloat(e.getValue());
            if (parsed != null) stats.values.put(e.getKey(), parsed);
        }
        return stats;
    }

    private static Float parseFloat(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number) return ((Number) raw).floatValue();
        if (raw instanceof Boolean) return null; // bonus e' un tipo diverso, non entra qui
        try {
            return Float.parseFloat(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
