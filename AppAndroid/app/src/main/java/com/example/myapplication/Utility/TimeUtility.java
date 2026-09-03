package com.example.myapplication.Utility;

import java.util.ArrayList;

public class TimeUtility {

    public static boolean isValidTime(String time) {
        int hour;
        int minute;
        String[] timeParts = time.split(":");

        if (timeParts.length != 2) {
            return false;
        }

        try {
            hour = Integer.parseInt(timeParts[0]);
            minute = Integer.parseInt(timeParts[1]);
        } catch (NumberFormatException e) {
            return false;
        }

        return hour >= 0 && hour <= 24 && minute >= 0 && minute <= 59;
    }

    /**
     * Converte un orario "H:mm" (ore anche a una cifra, es. "9:30") in minuti dalla
     * mezzanotte. Usare questo per ordinare gli orari: il confronto fra stringhe
     * sbaglia (es. "9:30" risulterebbe dopo "10:00"). Orari non validi valgono 0.
     */
    public static int toMinutes(String time) {
        String[] timeParts = time.split(":");

        if (timeParts.length != 2) {
            return 0;
        }

        try {
            return Integer.parseInt(timeParts[0]) * 60 + Integer.parseInt(timeParts[1]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean isAfter(String time1, String time2) {
        int hour1;
        int minute1;
        String[] timeParts1 = time1.split(":");

        int hour2;
        int minute2;
        String[] timeParts2 = time2.split(":");

        if (timeParts1.length!= 2 || timeParts2.length!= 2) {
            return false;
        }

        try {
            hour1 = Integer.parseInt(timeParts1[0]);
            minute1 = Integer.parseInt(timeParts1[1]);
            hour2 = Integer.parseInt(timeParts2[0]);
            minute2 = Integer.parseInt(timeParts2[1]);
        } catch (NumberFormatException e) {
            return false;
        }

        if (hour1 >= 0 && hour1 <= 23 && minute1 >= 0 && minute1 <= 59 && hour2 >= 0 && hour2 <= 23 && minute2 >= 0 && minute2 <= 59 ) {
            return hour2 > hour1 || (hour2 == hour1 && minute2 > minute1);
        }

        return false;
    }

    /**
     * Quante partite riescono a stare nelle fasce inserite, dato il minutaggio.
     * Versione tollerante di checkInputTime: le righe non valide contano 0 invece
     * di far fallire tutto, così l'info live ("le fasce non ospitano X") si aggiorna
     * mentre si digita.
     */
    public static int capacityMatches(ArrayList<String> stringsInput, int minutesForMatch) {
        if (minutesForMatch <= 0) {
            return 0;
        }

        int totalMatches = 0;

        for (int i = 0; i + 2 < stringsInput.size(); i += 3) {
            String startStr = stringsInput.get(i + 1);
            String endStr = stringsInput.get(i + 2);

            if (!isValidTime(startStr) || !isValidTime(endStr) || !isAfter(startStr, endStr)) {
                continue;
            }

            int minutes = toMinutes(endStr) - toMinutes(startStr);
            totalMatches += minutes / minutesForMatch;
        }

        return totalMatches;
    }

    public static boolean checkInputTime(ArrayList<String> stringsInput, int minutesForMatch, int nMatch) {
        int totalMatches = 0;

        for (int i = 0; i + 2 < stringsInput.size(); i += 3) {
            // Parse and validate match number
            try {
                Integer.parseInt(stringsInput.get(i));
            } catch (NumberFormatException e) {
                return false;
            }

            String startStr = stringsInput.get(i + 1);
            String endStr = stringsInput.get(i + 2);

            // Validate time format and order
            if (!isValidTime(startStr) || !isValidTime(endStr) || !isAfter(startStr, endStr)) {
                return false;
            }

            // Calculate match duration
            int minutes = toMinutes(endStr) - toMinutes(startStr);
            totalMatches += minutes / minutesForMatch;
        }

        return totalMatches >= nMatch;
    }
}
