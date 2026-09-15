package com.teammaker.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.teammaker.app.data.model.Match;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.teammaker.app.data.repository.MatchRepository;
import com.teammaker.app.domain.Times;
import com.teammaker.app.util.NetworkUtils;

/**
 * Test per MatchRepository.BY_DAY_TIME: prima per day, poi per orario (in MINUTI:
 * il confronto stringa sbagliava "9:30" vs "10:00"). Anche i comportamenti su
 * orari malformati (via Times.toMinutes) sono verificati qui perche' il
 * comparator ci si appoggia.
 */
public class MatchByDayTimeTest {

    private static Match match(int day, String time) {
        // key/team/points non contano per il comparator, uso valori dummy
        return new Match("t1", "t2", day, time, 0, 0);
    }

    @Test
    public void sameDay_sortedByMinutes_notByLexicographic() {
        Match m9  = match(1, "9:30");
        Match m10 = match(1, "10:00");
        // Con confronto stringa "10:00" < "9:30" (sbagliato). Con toMinutes: 570 < 600.
        List<Match> list = new ArrayList<>(Arrays.asList(m10, m9));
        Collections.sort(list, MatchRepository.BY_DAY_TIME);
        assertEquals(m9, list.get(0));
        assertEquals(m10, list.get(1));
    }

    @Test
    public void differentDay_dayHasHigherPriorityThanTime() {
        // Giorno 1 anche se in orario tardo, viene prima di giorno 2 anche se all'alba.
        Match dayOneLate  = match(1, "23:00");
        Match dayTwoEarly = match(2, "08:00");
        List<Match> list = new ArrayList<>(Arrays.asList(dayTwoEarly, dayOneLate));
        Collections.sort(list, MatchRepository.BY_DAY_TIME);
        assertEquals(dayOneLate, list.get(0));
        assertEquals(dayTwoEarly, list.get(1));
    }

    @Test
    public void sameDayAndTime_returnsZero() {
        Match a = match(1, "10:00");
        Match b = match(1, "10:00");
        assertEquals(0, MatchRepository.BY_DAY_TIME.compare(a, b));
    }

    @Test
    public void malformedTime_treatedAsZeroMinutes() {
        // Times.toMinutes(malformed) -> 0, quindi la partita finisce all'inizio.
        Match ok      = match(1, "09:00");
        Match malformed = match(1, "abc");
        List<Match> list = new ArrayList<>(Arrays.asList(ok, malformed));
        Collections.sort(list, MatchRepository.BY_DAY_TIME);
        assertEquals(malformed, list.get(0));
        assertEquals(ok, list.get(1));
    }

    @Test
    public void bigList_sortedStableChronologically() {
        Match m1 = match(1, "09:30");
        Match m2 = match(1, "10:00");
        Match m3 = match(1, "16:00");
        Match m4 = match(2, "08:00");
        Match m5 = match(2, "12:00");
        List<Match> shuffled = new ArrayList<>(Arrays.asList(m5, m1, m4, m3, m2));
        Collections.sort(shuffled, MatchRepository.BY_DAY_TIME);
        assertEquals(Arrays.asList(m1, m2, m3, m4, m5), shuffled);
    }

    @Test
    public void matchCompareToDelegatesToBYDAYTIME() {
        // Match implements Comparable via MatchRepository.BY_DAY_TIME: verifichiamo la delega.
        Match early = match(1, "09:00");
        Match late  = match(1, "17:00");
        assertTrue(early.compareTo(late) < 0);
        assertTrue(late.compareTo(early) > 0);
        assertEquals(0, early.compareTo(early));
    }
}
