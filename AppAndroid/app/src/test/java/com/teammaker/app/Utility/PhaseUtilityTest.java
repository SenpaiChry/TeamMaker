package com.teammaker.app.Utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test per PhaseUtility.normalize() e isGroup(): la conversione da valori legacy
 * (BRACKET/GIRONE/QUARTI/FINALINA/...) ai codici canonici deve restare stabile,
 * altrimenti i vecchi tornei salvati su Firebase non si leggono piu'.
 * label() non e' testato qui perche' richiede Android Context.
 */
public class PhaseUtilityTest {

    // ---- normalize: gia' canonici ----

    @Test
    public void normalize_alreadyCanonical_returnsSame() {
        assertEquals("GROUP", PhaseUtility.normalize("GROUP"));
        assertEquals("QUARTER", PhaseUtility.normalize("QUARTER"));
        assertEquals("SEMIFINAL", PhaseUtility.normalize("SEMIFINAL"));
        assertEquals("FINAL", PhaseUtility.normalize("FINAL"));
        assertEquals("THIRD", PhaseUtility.normalize("THIRD"));
        assertEquals("GROUP_A", PhaseUtility.normalize("GROUP_A"));
        assertEquals("GROUP_C", PhaseUtility.normalize("GROUP_C"));
    }

    @Test
    public void normalize_canonical_upperCasesIfLower() {
        assertEquals("GROUP", PhaseUtility.normalize("group"));
        assertEquals("FINAL", PhaseUtility.normalize("final"));
    }

    // ---- normalize: gironi legacy ----

    @Test
    public void normalize_bracketLegacy_becomesGroupPrefix() {
        assertEquals("GROUP_A", PhaseUtility.normalize("BRACKET A"));
        assertEquals("GROUP_B", PhaseUtility.normalize("Bracket b"));
    }

    @Test
    public void normalize_gironeLegacy_becomesGroupPrefix() {
        assertEquals("GROUP_A", PhaseUtility.normalize("GIRONE A"));
        assertEquals("GROUP_C", PhaseUtility.normalize("girone c"));
    }

    @Test
    public void normalize_bracketOrGironeAlone_becomesGroup() {
        assertEquals("GROUP", PhaseUtility.normalize("BRACKET"));
        assertEquals("GROUP", PhaseUtility.normalize("GIRONE"));
    }

    @Test
    public void normalize_bracketWithTrailingSpace_becomesGroup() {
        // "BRACKET " (con spazio) taglia il prefix, ma la lettera dopo lo spazio e' vuota
        assertEquals("GROUP", PhaseUtility.normalize("BRACKET "));
    }

    // ---- normalize: finali legacy ----

    @Test
    public void normalize_finalItaLegacy_becomesFinal() {
        assertEquals("FINAL", PhaseUtility.normalize("FINALE"));
        assertEquals("FINAL", PhaseUtility.normalize("finale"));
    }

    @Test
    public void normalize_semifinalItaLegacy_becomesSemifinal() {
        assertEquals("SEMIFINAL", PhaseUtility.normalize("SEMIFINALE"));
    }

    @Test
    public void normalize_quartiItaLegacy_becomesQuarter() {
        assertEquals("QUARTER", PhaseUtility.normalize("QUARTI"));
    }

    @Test
    public void normalize_finalinaItaLegacy_becomesThird() {
        assertEquals("THIRD", PhaseUtility.normalize("FINALINA"));
    }

    // ---- normalize: casi speciali ----

    @Test
    public void normalize_nullOrEmpty_returnsEmpty() {
        assertEquals("", PhaseUtility.normalize(null));
        assertEquals("", PhaseUtility.normalize(""));
        assertEquals("", PhaseUtility.normalize("   "));
    }

    @Test
    public void normalize_dashPlaceholder_returnsEmpty() {
        assertEquals("", PhaseUtility.normalize("---"));
        assertEquals("", PhaseUtility.normalize("-"));
    }

    @Test
    public void normalize_unknownValue_returnsTrimmedOriginal() {
        // Un valore sconosciuto viene restituito com'era (trim, ma senza uppercase forzato)
        assertEquals("PIZZA", PhaseUtility.normalize("  PIZZA  "));
    }

    // ---- isGroup ----

    @Test
    public void isGroup_trueForGroupAndGroupPrefix() {
        assertTrue(PhaseUtility.isGroup("GROUP"));
        assertTrue(PhaseUtility.isGroup("GROUP_A"));
        assertTrue(PhaseUtility.isGroup("BRACKET A")); // via normalize -> GROUP_A
        assertTrue(PhaseUtility.isGroup("GIRONE"));    // via normalize -> GROUP
    }

    @Test
    public void isGroup_falseForFinals() {
        assertFalse(PhaseUtility.isGroup("FINAL"));
        assertFalse(PhaseUtility.isGroup("SEMIFINAL"));
        assertFalse(PhaseUtility.isGroup("QUARTER"));
        assertFalse(PhaseUtility.isGroup("THIRD"));
    }

    @Test
    public void isGroup_falseForNullOrEmpty() {
        assertFalse(PhaseUtility.isGroup(null));
        assertFalse(PhaseUtility.isGroup(""));
    }

    // ---- groupCode ----

    @Test
    public void groupCode_prefixesAndUppercases() {
        assertEquals("GROUP_A", PhaseUtility.groupCode("a"));
        assertEquals("GROUP_B", PhaseUtility.groupCode("  B  "));
    }
}
