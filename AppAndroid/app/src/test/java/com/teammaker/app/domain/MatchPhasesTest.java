package com.teammaker.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import com.teammaker.app.domain.MatchPhases;
import com.teammaker.app.util.NetworkUtils;

/**
 * Test per MatchPhases.normalize() e isGroup(): la conversione da valori legacy
 * (BRACKET/GIRONE/QUARTI/FINALINA/...) ai codici canonici deve restare stabile,
 * altrimenti i vecchi tornei salvati su Firebase non si leggono piu'.
 * label() non e' testato qui perche' richiede Android Context.
 */
public class MatchPhasesTest {

    // ---- normalize: gia' canonici ----

    @Test
    public void normalize_alreadyCanonical_returnsSame() {
        assertEquals("GROUP", MatchPhases.normalize("GROUP"));
        assertEquals("QUARTER", MatchPhases.normalize("QUARTER"));
        assertEquals("SEMIFINAL", MatchPhases.normalize("SEMIFINAL"));
        assertEquals("FINAL", MatchPhases.normalize("FINAL"));
        assertEquals("THIRD", MatchPhases.normalize("THIRD"));
        assertEquals("GROUP_A", MatchPhases.normalize("GROUP_A"));
        assertEquals("GROUP_C", MatchPhases.normalize("GROUP_C"));
    }

    @Test
    public void normalize_canonical_upperCasesIfLower() {
        assertEquals("GROUP", MatchPhases.normalize("group"));
        assertEquals("FINAL", MatchPhases.normalize("final"));
    }

    // ---- normalize: gironi legacy ----

    @Test
    public void normalize_bracketLegacy_becomesGroupPrefix() {
        assertEquals("GROUP_A", MatchPhases.normalize("BRACKET A"));
        assertEquals("GROUP_B", MatchPhases.normalize("Bracket b"));
    }

    @Test
    public void normalize_gironeLegacy_becomesGroupPrefix() {
        assertEquals("GROUP_A", MatchPhases.normalize("GIRONE A"));
        assertEquals("GROUP_C", MatchPhases.normalize("girone c"));
    }

    @Test
    public void normalize_bracketOrGironeAlone_becomesGroup() {
        assertEquals("GROUP", MatchPhases.normalize("BRACKET"));
        assertEquals("GROUP", MatchPhases.normalize("GIRONE"));
    }

    @Test
    public void normalize_bracketWithTrailingSpace_becomesGroup() {
        // "BRACKET " (con spazio) taglia il prefix, ma la lettera dopo lo spazio e' vuota
        assertEquals("GROUP", MatchPhases.normalize("BRACKET "));
    }

    // ---- normalize: finali legacy ----

    @Test
    public void normalize_finalItaLegacy_becomesFinal() {
        assertEquals("FINAL", MatchPhases.normalize("FINALE"));
        assertEquals("FINAL", MatchPhases.normalize("finale"));
    }

    @Test
    public void normalize_semifinalItaLegacy_becomesSemifinal() {
        assertEquals("SEMIFINAL", MatchPhases.normalize("SEMIFINALE"));
    }

    @Test
    public void normalize_quartiItaLegacy_becomesQuarter() {
        assertEquals("QUARTER", MatchPhases.normalize("QUARTI"));
    }

    @Test
    public void normalize_finalinaItaLegacy_becomesThird() {
        assertEquals("THIRD", MatchPhases.normalize("FINALINA"));
    }

    // ---- normalize: casi speciali ----

    @Test
    public void normalize_nullOrEmpty_returnsEmpty() {
        assertEquals("", MatchPhases.normalize(null));
        assertEquals("", MatchPhases.normalize(""));
        assertEquals("", MatchPhases.normalize("   "));
    }

    @Test
    public void normalize_dashPlaceholder_returnsEmpty() {
        assertEquals("", MatchPhases.normalize("---"));
        assertEquals("", MatchPhases.normalize("-"));
    }

    @Test
    public void normalize_unknownValue_returnsTrimmedOriginal() {
        // Un valore sconosciuto viene restituito com'era (trim, ma senza uppercase forzato)
        assertEquals("PIZZA", MatchPhases.normalize("  PIZZA  "));
    }

    // ---- isGroup ----

    @Test
    public void isGroup_trueForGroupAndGroupPrefix() {
        assertTrue(MatchPhases.isGroup("GROUP"));
        assertTrue(MatchPhases.isGroup("GROUP_A"));
        assertTrue(MatchPhases.isGroup("BRACKET A")); // via normalize -> GROUP_A
        assertTrue(MatchPhases.isGroup("GIRONE"));    // via normalize -> GROUP
    }

    @Test
    public void isGroup_falseForFinals() {
        assertFalse(MatchPhases.isGroup("FINAL"));
        assertFalse(MatchPhases.isGroup("SEMIFINAL"));
        assertFalse(MatchPhases.isGroup("QUARTER"));
        assertFalse(MatchPhases.isGroup("THIRD"));
    }

    @Test
    public void isGroup_falseForNullOrEmpty() {
        assertFalse(MatchPhases.isGroup(null));
        assertFalse(MatchPhases.isGroup(""));
    }

    // ---- groupCode ----

    @Test
    public void groupCode_prefixesAndUppercases() {
        assertEquals("GROUP_A", MatchPhases.groupCode("a"));
        assertEquals("GROUP_B", MatchPhases.groupCode("  B  "));
    }
}
