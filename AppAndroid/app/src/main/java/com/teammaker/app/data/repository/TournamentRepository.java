package com.teammaker.app.data.repository;


import com.teammaker.app.auth.AdminAuth;
import static com.teammaker.app.data.AppConfig.DB_ROOT;

import android.util.Log;

import androidx.annotation.NonNull;

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
import com.teammaker.app.data.mapper.MapperUtils;
import com.teammaker.app.data.mapper.MatchMapper;
import com.teammaker.app.data.mapper.TeamMapper;
import com.teammaker.app.data.AppConfig;
import com.teammaker.app.data.InputSanitizer;
import com.teammaker.app.domain.TeamGenerator;

public class TournamentRepository {

    /** Lunghezza massima per il nome torneo. */
    private static final int TOURNAMENT_NAME_MAX = 60;

    /** Cache in memoria dei tornei (sincronizzata dal listener realtime). */
    private static final ArrayList<Tournament> tournaments = new ArrayList<>();

    /** Vista mutabile della cache. */
    public static ArrayList<Tournament> getAll() { return tournaments; }

    /**
     * True quando il primo caricamento dei dati e' completato (players + tournaments).
     * Prima si chiamava Constants.downloadEnd.
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
                    tournament.setValid(MapperUtils.boolOr(tournamentSnapshot, "is_valid", false));
                    tournament.name = MapperUtils.str(tournamentSnapshot, "name");
                    tournament.nBracket = MapperUtils.intOr(tournamentSnapshot, "nBracket", 0);
                    tournament.locked = MapperUtils.boolOr(tournamentSnapshot, "locked", false);
                    tournament.key = tournamentSnapshot.getKey();

                    String dateString = MapperUtils.str(tournamentSnapshot, "date");
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                    if (!dateString.isEmpty()) {
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
        if (!AdminAuth.requireAdmin()) return;
        for (Tournament tournament : TournamentRepository.getAll()) {
            if (tournament.isValid) {
                tournament.isValid = false;
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + tournament.key);
                FirebaseWriteHelper.attach(null, "deactivateAllTournaments", dbRef.child("is_valid").setValue(false));
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
        if (!AdminAuth.requireAdmin()) return;
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + key);
        FirebaseWriteHelper.attach(null, "setLocked", dbRef.child("locked").setValue(locked));
        Tournament tournament = getTournamentByKey(key);
        if (tournament != null) tournament.locked = locked;
    }

    public static void deleteTournament(String key) {
        if (!AdminAuth.requireAdmin()) return;
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + key);
        dbRef.removeValue().addOnSuccessListener(aVoid -> {
            Log.d("Firebase", "Torneo eliminato: " + key);
        }).addOnFailureListener(e -> {
            Log.e("Firebase", "Errore eliminazione torneo", e);
        });
    }

    /** Aggiornamento atomico: nome e data vanno o entrambi o nessuno. */
    public static void updateNameAndDateTournament(String key, String name, Calendar date) {
        if (!AdminAuth.requireAdmin()) return;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = sdf.format(date.getTime());

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", InputSanitizer.clean(name, TOURNAMENT_NAME_MAX));
        updates.put("date", formattedDate);

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + key);
        FirebaseWriteHelper.attach(null, "updateNameAndDateTournament", dbRef.updateChildren(updates));
    }

    /**
     * Creazione atomica di un nuovo torneo con le squadre gia' generate. Prima
     * erano ~20 setValue indipendenti: una rete che cadeva a meta' lasciava un
     * torneo mezzo salvato (nome senza data, 3 squadre invece di 5, ...). Adesso
     * un'unica updateChildren multi-path: o passa tutto, o niente.
     */
    public static void saveNewTournamentTeams(String tournamentName, Calendar date) {
        if (!AdminAuth.requireAdmin()) return;
        DatabaseReference tournamentsRoot = FirebaseDatabase.getInstance()
                .getReference(DB_ROOT + "tournaments/");
        String key = tournamentsRoot.push().getKey();
        if (key == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = sdf.format(date.getTime());

        deactivateAllTournaments();

        Map<String, Object> updates = new HashMap<>();
        updates.put(key + "/is_valid", true);
        updates.put(key + "/name", InputSanitizer.clean(tournamentName, TOURNAMENT_NAME_MAX));
        updates.put(key + "/nBracket", 0);
        updates.put(key + "/date", formattedDate);

        DatabaseReference teamsRef = tournamentsRoot.child(key).child("teams");
        for (Team team : TeamGenerator.getGenerated()) {
            String keyTeam = teamsRef.push().getKey();
            if (keyTeam == null) continue;
            team.key = keyTeam;

            updates.put(key + "/teams/" + keyTeam + "/bracket", "");
            for (int i = 0; i < team.players.size(); i++) {
                updates.put(key + "/teams/" + keyTeam + "/player" + (i + 1),
                        team.players.get(i).key);
            }
        }

        FirebaseWriteHelper.attach(null, "saveNewTournamentTeams",
                tournamentsRoot.updateChildren(updates));
    }

    public static void updateNBracketsTournament(String tournamentKey, int nBrackets) {
        if (!AdminAuth.requireAdmin()) return;
        // Scrittura diretta col path: evita l'NPE di quando il torneo non e' piu'
        // in cache locale (race col listener) e ci evita una lookup inutile.
        FirebaseDatabase.getInstance()
                .getReference(DB_ROOT + "tournaments/" + tournamentKey + "/nBracket")
                .setValue(nBrackets);
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
        if (!AdminAuth.requireAdmin()) return;
        Map<String, Object> updates = new HashMap<>();
        for (Tournament tournament : TournamentRepository.getAll()) {
            if (tournament.isValid && !tournament.key.equals(key)) {
                updates.put(tournament.key + "/is_valid", false);
                tournament.isValid = false;
            }
        }
        updates.put(key + "/is_valid", true);

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