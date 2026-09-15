package com.teammaker.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Test per PlayerStats: dopo il refactor da HashMap<String,Object> a classe
 * tipizzata, la conversione da Firebase (che ritorna Long/Double/String) deve
 * finire sempre in un float, con default 0 per le key assenti.
 */
public class PlayerStatsTest {

    @Test
    public void get_missingKey_returnsZero() {
        PlayerStats stats = new PlayerStats();
        assertEquals(0f, stats.get("agility"), 0.0001f);
    }

    @Test
    public void setAndGet_roundtrip() {
        PlayerStats stats = new PlayerStats();
        stats.set("attack", 4.5f);
        assertEquals(4.5f, stats.get("attack"), 0.0001f);
    }

    @Test
    public void set_overwritesPreviousValue() {
        PlayerStats stats = new PlayerStats();
        stats.set("k", 1f);
        stats.set("k", 3f);
        assertEquals(3f, stats.get("k"), 0.0001f);
    }

    @Test
    public void containsKey_reflectsSet() {
        PlayerStats stats = new PlayerStats();
        assertFalse(stats.containsKey("k"));
        stats.set("k", 2f);
        assertTrue(stats.containsKey("k"));
    }

    @Test
    public void copyConstructor_deepCopiesValues() {
        PlayerStats src = new PlayerStats();
        src.set("k", 1f);
        PlayerStats copy = new PlayerStats(src);
        copy.set("k", 9f);
        // Modifica sulla copia non riflette sull'originale
        assertEquals(1f, src.get("k"), 0.0001f);
        assertEquals(9f, copy.get("k"), 0.0001f);
        assertNotSame(src, copy);
    }

    // ---- fromRawMap ----

    @Test
    public void fromRawMap_null_returnsEmpty() {
        PlayerStats stats = PlayerStats.fromRawMap(null);
        assertTrue(stats.isEmpty());
        assertEquals(0, stats.size());
    }

    @Test
    public void fromRawMap_parsesLongDoubleAndStringNumbers() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("a", 3L);          // Firebase int -> Long
        raw.put("b", 2.5d);        // Firebase float -> Double
        raw.put("c", "4.5");       // vecchi record salvati come stringa
        PlayerStats stats = PlayerStats.fromRawMap(raw);
        assertEquals(3f, stats.get("a"), 0.0001f);
        assertEquals(2.5f, stats.get("b"), 0.0001f);
        assertEquals(4.5f, stats.get("c"), 0.0001f);
    }

    @Test
    public void fromRawMap_skipsBooleans() {
        // Il bonus e' un tipo diverso (Boolean), non deve entrare qui
        Map<String, Object> raw = new HashMap<>();
        raw.put("bonusStar", true);
        raw.put("k", 2L);
        PlayerStats stats = PlayerStats.fromRawMap(raw);
        assertFalse(stats.containsKey("bonusStar"));
        assertEquals(2f, stats.get("k"), 0.0001f);
    }

    @Test
    public void fromRawMap_skipsUnparsableStrings() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("k", "not a number");
        PlayerStats stats = PlayerStats.fromRawMap(raw);
        assertFalse(stats.containsKey("k"));
    }

    // ---- toRawMap ----

    @Test
    public void toRawMap_roundtripsBack() {
        PlayerStats stats = new PlayerStats();
        stats.set("a", 1.5f);
        stats.set("b", 3f);
        HashMap<String, Object> raw = stats.toRawMap();
        assertEquals(2, raw.size());
        // I valori nella raw sono Float
        assertEquals(1.5f, ((Number) raw.get("a")).floatValue(), 0.0001f);
        assertEquals(3f, ((Number) raw.get("b")).floatValue(), 0.0001f);

        // Round-trip: from -> to conserva i valori
        PlayerStats back = PlayerStats.fromRawMap(raw);
        assertEquals(1.5f, back.get("a"), 0.0001f);
        assertEquals(3f, back.get("b"), 0.0001f);
    }

    @Test
    public void setFromRaw_writesParsedValue() {
        PlayerStats stats = new PlayerStats();
        stats.setFromRaw("a", "2.5");
        stats.setFromRaw("b", 4L);
        // Un raw non parsabile viene ignorato: la key non deve apparire
        stats.setFromRaw("c", "boh");
        assertEquals(2.5f, stats.get("a"), 0.0001f);
        assertEquals(4f, stats.get("b"), 0.0001f);
        assertFalse(stats.containsKey("c"));
    }
}
