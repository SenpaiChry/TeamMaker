package com.example.myapplication.Utility;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

    public static boolean checkInputTime(ArrayList<String> stringsInput, int minutesForMatch, int nMatch) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
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
            LocalTime start = LocalTime.parse(startStr, formatter);
            LocalTime end = LocalTime.parse(endStr, formatter);
            long minutes = Duration.between(start, end).toMinutes();
            totalMatches += (int) (minutes / minutesForMatch);
        }

        return totalMatches >= nMatch;
    }
}
