package com.example.myapplication.Utility;

import android.content.Context;

import com.example.myapplication.R;

/**
 * Fase di una partita salvata come CODICE univoco (indipendente dalla lingua):
 * GROUP (girone unico), GROUP_A/GROUP_B... (gironi), QUARTER, SEMIFINAL, FINAL, THIRD.
 * A schermo si mostra l'etichetta tradotta con label(); i dati vecchi/localizzati
 * vengono convertiti da normalize() così restano compatibili.
 */
public class PhaseUtility {

    public static final String GROUP = "GROUP";
    public static final String QUARTER = "QUARTER";
    public static final String SEMIFINAL = "SEMIFINAL";
    public static final String FINAL = "FINAL";
    public static final String THIRD = "THIRD";
    public static final String GROUP_PREFIX = "GROUP_";

    /** Codice del girone per lettera, es. groupCode("A") -> "GROUP_A". */
    public static String groupCode(String letter) {
        return GROUP_PREFIX + letter.trim().toUpperCase();
    }

    /** True se è una fase iniziale (girone/champions), non una finale. */
    public static boolean isGroup(String type) {
        String t = normalize(type);
        return t.equals(GROUP) || t.startsWith(GROUP_PREFIX);
    }

    /** Converte un type salvato (anche legacy/localizzato) nel codice canonico. */
    public static String normalize(String stored) {
        if (stored == null) {
            return "";
        }
        String t = stored.trim();
        if (t.isEmpty() || t.equals("---") || t.equals("-")) {
            return "";
        }
        String u = t.toUpperCase();

        // Gia' canonico
        if (u.equals(GROUP) || u.equals(QUARTER) || u.equals(SEMIFINAL)
                || u.equals(FINAL) || u.equals(THIRD) || u.startsWith(GROUP_PREFIX)) {
            return u;
        }

        // Gironi legacy: "BRACKET A" / "GIRONE A" -> GROUP_A
        if (u.startsWith("BRACKET ") || u.startsWith("GIRONE ")) {
            String letter = u.substring(u.indexOf(' ') + 1).trim();
            return letter.isEmpty() ? GROUP : GROUP_PREFIX + letter;
        }
        if (u.equals("BRACKET") || u.equals("GIRONE")) {
            return GROUP;
        }

        // Finali legacy (it/en)
        if (u.equals("QUARTI") || u.equals("QUARTER")) return QUARTER;
        if (u.equals("SEMIFINALE") || u.equals("SEMIFINAL")) return SEMIFINAL;
        if (u.equals("FINALINA") || u.equals("THIRD")) return THIRD;
        if (u.equals("FINALE") || u.equals("FINAL")) return FINAL;

        // Sconosciuto: lascia il valore originale
        return t;
    }

    /** Etichetta tradotta da mostrare per un type (codice o legacy). "" se nessuna fase. */
    public static String label(Context ctx, String type) {
        String code = normalize(type);
        if (code.isEmpty()) return "";
        if (code.equals(GROUP)) return ctx.getString(R.string.bracketSpace).trim();
        if (code.startsWith(GROUP_PREFIX)) {
            return ctx.getString(R.string.bracket) + " " + code.substring(GROUP_PREFIX.length());
        }
        if (code.equals(QUARTER)) return ctx.getString(R.string.quarter);
        if (code.equals(SEMIFINAL)) return ctx.getString(R.string.semifinal);
        if (code.equals(FINAL)) return ctx.getString(R.string.finalString);
        if (code.equals(THIRD)) return ctx.getString(R.string.finalina);
        return code;
    }
}
