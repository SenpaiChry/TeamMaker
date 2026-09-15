package com.teammaker.app.data.repository;

import static com.teammaker.app.data.AppConfig.DB_ROOT;

import android.util.Log;

import androidx.annotation.NonNull;

import com.teammaker.app.data.model.Constants;
import com.teammaker.app.data.model.Match;
import com.teammaker.app.data.model.Team;
import com.teammaker.app.data.model.Tournament;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import com.teammaker.app.bus.DataChangeBus;
import com.teammaker.app.data.firebase.FirebaseWriteHelper;
import com.teammaker.app.data.mapper.MatchMapper;
import com.teammaker.app.data.mapper.TeamMapper;
import com.teammaker.app.data.AppConfig;

public class TournamentRepository {

    /** Cache in memoria dei tornei (sincronizzata dal listener realtime). */
    private static final ArrayList<Tournament> tournaments = new ArrayList<>();

    /** Vista mutabile della cache. */
    public static ArrayList<Tournament> getAll() { return tournaments; }

    /**
     * True quando il primo caricamento dei dati e' completato (players + tournaments).
     * Prima si chiamava TournamentRepository.isDataReady().
     */
    private static boolean dataReady = false;
    public static boolean isDataReady() { return dataReady; }

    /** Listener persistente: si stacca con removeTournamentsListener() */
    private static ValueEventListener tournamentsListener;
    private static DatabaseReference tournamentsRef;

    /**
     * Ascolta i tornei in TEMPO REALE: ogni modifica su Firebase
     * (da qualsiasi dispositivo) aggiorna automaticamente la lista locale.
     */
    public static void downloadTournaments() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        tournamentsRef = firebaseDatabase.getReference(DB_ROOT + "tournaments/");

        tournamentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                TournamentRepository.getAll().clear();

                if (!snapshot.exists()) {
                    dataReady = true;
                    return;
                }

                for (DataSnapshot tournamentSnapshot : snapshot.getChildren()) {
                    try {
                    Tournament tournament = new Tournament();
                    tournament.setValid(tournamentSnapshot.child("is_valid").getValue(String.class));
                    tournament.name = tournamentSnapshot.child("name").getValue(String.class);
                    tournament.nBracket = tournamentSnapshot.child("nBracket").getValue(Integer.class) != null
                            ? tournamentSnapshot.child("nBracket").getValue(Integer.class) : 0;
                    Boolean lockedRaw = tournamentSnapshot.child("locked").getValue(Boolean.class);
                    tournament.locked = lockedRaw != null && lockedRaw;
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
                            Log.w("TournamentRepository", "Data torneo malformata: " + dateString, e);
                            tournament.date = null;
                        }
                    } else {
                        tournament.date = null;
                    }

                    for (DataSnapshot matchSnapshot : tournamentSnapshot.child("matches").getChildren()) {
                        try {
                            Match match = MatchMapper.fromSnapshot(matchSnapshot);
                            if (match != null) tournament.matches.add(match);
                        } catch (Exception e) {
                            Log.e("TournamentRepository", "Partita malformata ignorata: " + matchSnapshot.getKey(), e);
                        }
                    }

                    for (DataSnapshot teamSnapshot : tournamentSnapshot.child("teams").getChildren()) {
                        Team team = TeamMapper.fromSnapshot(teamSnapshot);
                        if (team != null) tournament.teams.add(team);
                    }

                    Collections.sort(tournament.matches);
                    TournamentRepository.getAll().add(tournament);
                    } catch (Exception e) {
                        Log.e("TournamentRepository", "Torneo malformato ignorato: " + tournamentSnapshot.getKey(), e);
                    }
                }

                dataReady = true;

                // Notifica chi mostra tornei/partite (l'Activity visibile lo raccoglie)
                DataChangeBus.emit(DataChangeBus.Event.TOURNAMENTS);
                DataChangeBus.emit(DataChangeBus.Event.MATCHES);

                Log.d("Firebase", "Tournaments aggiornati in tempo reale: " + TournamentRepository.getAll().size());
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
        for (Tournament tournament : TournamentRepository.getAll()) {
            if (tournament.isValid) {
                tournament.isValid = false;
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + tournament.key);
                FirebaseWriteHelper.attach(null, "deactivateAllTournaments", dbRef.child("is_valid").setValue("false"));
                return;
            }
        }
    }

    /**
     * Congela/scongela il torneo. Con locked=true la UI nasconde tutti i tasti di
     * modifica (partite, squadre, salvataggio nome/data, elimina): restano usabili
     * solo SBLOCCA e ATTIVA/DISATTIVA. Non e' una guardia server-side.
     */
    public static void setLocked(String key, boolean locked) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + key);
        FirebaseWriteHelper.attach(null, "setLocked", dbRef.child("locked").setValue(locked));
        Tournament tournament = getTournamentByKey(key);
        if (tournament != null) tournament.locked = locked;
    }

    public static void deleteTournament(String key) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + key);
        dbRef.removeValue().addOnSuccessListener(aVoid -> {
            Log.d("Firebase", "Torneo eliminato: " + key);
        }).addOnFailureListener(e -> {
            Log.e("Firebase", "Errore eliminazione torneo", e);
        });
    }

    public static void updateNameAndDateTournament(String key, String name, Calendar date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = sdf.format(date.getTime());

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + key);
        dbRef.child("name").setValue(name);
        dbRef.child("date").setValue(formattedDate);
    }

    public static void saveNewTournamentTeams(String tournamentName, Calendar date) {
        String key = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/").push().getKey();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + key);
        DatabaseReference dbRefTeam;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = sdf.format(date.getTime());

        deactivateAllTournaments();

        dbRef.child("is_valid").setValue("true");
        dbRef.child("name").setValue(tournamentName);
        dbRef.child("nBracket").setValue(0);
        dbRef.child("date").setValue(formattedDate);

        for (Team team : Constants.teams) {
            String keyTeam = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + key + "/teams/").push().getKey();
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
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + tournament.key);
        dbRef.child("nBracket").setValue(nBrackets);
    }

    public static Tournament getActiveTournament() {
        for (Tournament tournament : TournamentRepository.getAll()) {
            if (tournament.isValid) {
                return tournament;
            }
        }
        return null;
    }

    /**
     * Attivazione atomica: in una sola scrittura multi-path disattiva TUTTI i tornei
     * attivi e attiva quello passato. Prima erano due scritture separate: se la
     * seconda falliva poteva restare NESSUN torneo attivo.
     */
    public static void setActiveTournament(String key) {
        Map<String, Object> updates = new HashMap<>();
        for (Tournament tournament : TournamentRepository.getAll()) {
            if (tournament.isValid && !tournament.key.equals(key)) {
                updates.put(tournament.key + "/is_valid", "false");
                tournament.isValid = false;
            }
        }
        updates.put(key + "/is_valid", "true");

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/");
        FirebaseWriteHelper.attach(null, "setActiveTournament", dbRef.updateChildren(updates));
    }

    public static Tournament getTournamentByKey(String key) {
        for (Tournament tournament : TournamentRepository.getAll()) {
            if (tournament.key.equals(key)) {
                return tournament;
            }
        }
        return null;
    }
}