package com.teammaker.app.data;

/**
 * Pulizia delle stringhe utente prima della scrittura su Firebase: unico
 * posto centralizzato usato dai repository. Difesa contro spazi extra,
 * tab/newline copia-incollati, caratteri di controllo Unicode e paste
 * chilometrici — senza rifiutare caratteri leciti come apostrofi, lettere
 * accentate, alfabeti non latini.
 */
public final class InputSanitizer {

    private InputSanitizer() { }

    /**
     * Trim, collasso di spazi/tab/newline multipli in uno, rimozione di caratteri
     * di controllo (Unicode Cntrl), troncamento a maxLen. Null-safe: ritorna "".
     */
    public static String clean(String raw, int maxLen) {
        if (raw == null) return "";
        String s = raw.replaceAll("\\p{Cntrl}+", " ").replaceAll("\\s+", " ").trim();
        if (s.length() > maxLen) s = s.substring(0, maxLen);
        return s;
    }
}
