package com.example.myapplication.Utility;

import static com.example.myapplication.Model.Constants.dbRoot;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.Match;
import com.example.myapplication.Team;
import com.example.myapplication.Tournament;
import com.example.myapplication.TournamentActivityManageTournaments;
import com.example.myapplication.Model.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class TournamentUtility {

    /** Listener persistente: si stacca con removeTournamentsListener() */
    private static ValueEventListener tournamentsListener;
    private static DatabaseReference tournamentsRef;

    /** Parsing difensivo: un valore nullo o non numerico vale 0 invece di far crashare il download. */
    private static int parseIntOrZero(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Ascolta i tornei in TEMPO REALE: ogni modifica su Firebase
     * (da qualsiasi dispositivo) aggiorna automaticamente la lista locale.
     */
    public static void downloadTournaments() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        tournamentsRef = firebaseDatabase.getReference(dbRoot + "tournaments/");

        tournamentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Constants.tournaments.clear();

                if (!snapshot.exists()) {
                    Constants.downloadEnd = true;
                    return;
                }

                for (DataSnapshot tournamentSnapshot : snapshot.getChildren()) {
                    try {
                    Tournament tournament = new Tournament();
                    tournament.setValid(tournamentSnapshot.child("is_valid").getValue(String.class));
                    tournament.name = tournamentSnapshot.child("name").getValue(String.class);
                    tournament.nBracket = tournamentSnapshot.child("nBracket").getValue(Integer.class) != null
                            ? tournamentSnapshot.child("nBracket").getValue(Integer.class) : 0;
                    tournament.key = tournamentSnapshot.getKey();

                    String dateString = tournamentSnapshot.child("date").getValue(String.class);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                    if (dateString != null && !dateString.isEmpty()) {
                        try {
                            Date parsedDate = sdf.parse(dateString);
                            if (parsedDate != null) {
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(parsedDate);
                                tournament.date = calendar;
                            } else {
                                tournament.date = null;
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                            tournament.date = null;
                        }
                    } else {
                        tournament.date = null;
                    }

                    for (DataSnapshot matchSnapshot : tournamentSnapshot.child("matches").getChildren()) {
                        try {
                            Match match = new Match(
                                    matchSnapshot.child("team1").getValue(String.class),
                                    matchSnapshot.child("team2").getValue(String.class),
                                    parseIntOrZero(matchSnapshot.child("day").getValue(String.class)),
                                    matchSnapshot.child("time").getValue(String.class),
                                    parseIntOrZero(matchSnapshot.child("points1").getValue(String.class)),
                                    parseIntOrZero(matchSnapshot.child("points2").getValue(String.class))
                            );

                            if (matchSnapshot.hasChild("type")) {
                                match.type = String.valueOf(matchSnapshot.child("type").getValue(String.class));
                            }

                            for (DataSnapshot setSnap : matchSnapshot.child("detail").getChildren()) {
                                match.detail.add(new int[]{
                                        parseIntOrZero(setSnap.child("points1").getValue(String.class)),
                                        parseIntOrZero(setSnap.child("points2").getValue(String.class))
                                });
                            }

                            match.key = matchSnapshot.getKey();
                            tournament.matches.add(match);
                        } catch (Exception e) {
                            Log.e("TournamentUtility", "Partita malformata ignorata: " + matchSnapshot.getKey(), e);
                        }
                    }

                    for (DataSnapshot teamSnapshot : tournamentSnapshot.child("teams").getChildren()) {
                        Team team = new Team();
                        team.key = teamSnapshot.getKey();
                        team.bracket = teamSnapshot.child("bracket").getValue(String.class);

                        for (int i = 1; i < teamSnapshot.getChildrenCount() + 1; i++) {
                            team.addPlayerByKey(teamSnapshot.child("player" + i).getValue(String.class));
                        }

                        tournament.teams.add(team);
                    }

                    Collections.sort(tournament.matches);
                    Constants.tournaments.add(tournament);
                    } catch (Exception e) {
                        Log.e("TournamentUtility", "Torneo malformato ignorato: " + tournamentSnapshot.getKey(), e);
                    }
                }

                Constants.downloadEnd = true;

                // Aggiorna la lista tornei se la schermata e' aperta
                try {
                    TournamentActivityManageTournaments.reloadTournaments();
                } catch (Exception ignored) { }

                Log.d("Firebase", "Tournaments aggiornati in tempo reale: " + Constants.tournaments.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Errore listener tournaments", error.toException());
            }
        };

        tournamentsRef.addValueEventListener(tournamentsListener);
    }

    /** Rimuove il listener in tempo reale (da chiamare in onDestroy della MainActivity). */
    public static void removeTournamentsListener() {
        if (tournamentsRef != null && tournamentsListener != null) {
            tournamentsRef.removeEventListener(tournamentsListener);
            tournamentsListener = null;
        }
    }

    public static void deactivateAllTournaments() {
        for (Tournament tournament : Constants.tournaments) {
            if (tournament.isValid) {
                tournament.isValid = false;
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournament.key);
                dbRef.child("is_valid").setValue("false");
                return;
            }
        }
    }

    public static void deleteTournament(String key) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + key);
        dbRef.removeValue().addOnSuccessListener(aVoid -> {
            Log.d("Firebase", "Torneo eliminato: " + key);
        }).addOnFailureListener(e -> {
            Log.e("Firebase", "Errore eliminazione torneo", e);
        });
    }

    public static void updateNameAndDateTournament(String key, String name, Calendar date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = sdf.format(date.getTime());

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + key);
        dbRef.child("name").setValue(name);
        dbRef.child("date").setValue(formattedDate);
    }

    public static void saveNewTournamentTeams(String tournamentName, Calendar date) {
        String key = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/").push().getKey();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + key);
        DatabaseReference dbRefTeam;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = sdf.format(date.getTime());

        deactivateAllTournaments();

        dbRef.child("is_valid").setValue("true");
        dbRef.child("name").setValue(tournamentName);
        dbRef.child("nBracket").setValue(0);
        dbRef.child("date").setValue(formattedDate);

        for (Team team : Constants.teams) {
            String keyTeam = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + key + "/teams/").push().getKey();
            dbRefTeam = dbRef.child("teams").child(keyTeam);
            team.key = keyTeam;

            dbRefTeam.child("bracket").setValue("");

            for (int i = 0; i < team.players.size(); i++) {
                dbRefTeam.child("player" + (i + 1)).setValue(team.players.get(i).key);
            }
        }
    }

    public static void updateNBracketsTournament(String tournamentKey, int nBrackets) {
        Tournament tournament = getTournamentByKey(tournamentKey);
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournament.key);
        dbRef.child("nBracket").setValue(nBrackets);
    }

    public static Tournament getActiveTournament() {
        for (Tournament tournament : Constants.tournaments) {
            if (tournament.isValid) {
                return tournament;
            }
        }
        return null;
    }

    public static void setActiveTournament(String key) {
        deactivateAllTournaments();

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + key);
        dbRef.child("is_valid").setValue("true");
    }

    public static Tournament getTournamentByKey(String key) {
        for (Tournament tournament : Constants.tournaments) {
            if (tournament.key.equals(key)) {
                return tournament;
            }
        }
        return null;
    }
}