package com.teammaker.app.ui.adapter;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import com.teammaker.app.data.model.Match;
import com.teammaker.app.data.model.Tournament;
import com.teammaker.app.ui.popup.ManageTournamentBottomSheet;
import com.teammaker.app.R;
import com.teammaker.app.data.repository.TournamentRepository;

public class TournamentAdapter extends RecyclerView.Adapter<TournamentAdapter.ViewHolder> {

    private final AppCompatActivity activity;
    private final ArrayList<Tournament> tournaments;
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("EEE d MMM yyyy", Locale.ITALIAN);

    public TournamentAdapter(AppCompatActivity activity) {
        this.activity = activity;
        this.tournaments = TournamentRepository.getAll();

        Collections.sort(tournaments, (t1, t2) -> {
            if (t1.date == null && t2.date == null) return 0;
            if (t1.date == null) return 1;
            if (t2.date == null) return -1;
            return t2.date.getTime().compareTo(t1.date.getTime());
        });
    }

    @Override
    public int getItemCount() { return tournaments.size(); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tournament_layout_tournaments, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Tournament tournament = tournaments.get(position);

        // Card background: highlight se attivo
        h.llTournament.setBackground(ContextCompat.getDrawable(activity,
                tournament.isValid ? R.drawable.bg_list_card_highlight : R.drawable.bg_list_card));

        // Nome torneo
        if (tournament.name == null || tournament.name.isEmpty()) {
            h.txtTournamentN.setText(activity.getString(R.string.tournament) + " " + (position + 1));
        } else {
            h.txtTournamentN.setText(tournament.name);
        }
        h.txtTournamentN.setTextColor(ContextCompat.getColor(activity,
                tournament.isValid ? R.color.list_highlight_text : R.color.list_text_primary));

        // Data
        if (tournament.date != null) {
            String formatted = DATE_FORMAT.format(tournament.date.getTime());
            h.txtDate.setText(formatted.substring(0, 1).toUpperCase() + formatted.substring(1));
            h.txtDate.setVisibility(View.VISIBLE);
        } else {
            h.txtDate.setVisibility(View.GONE);
        }

        // Badge stato
        if (tournament.isValid) {
            h.txtBadge.setText("ATTIVO");
            int green = ContextCompat.getColor(activity, R.color.button_confirm);
            h.txtBadge.setTextColor(green);
            h.txtBadge.setBackgroundResource(R.drawable.bg_badge_active);
            h.txtBadge.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_check_small, 0, 0, 0);
            TextViewCompat.setCompoundDrawableTintList(h.txtBadge,
                    ColorStateList.valueOf(green));
            h.txtBadge.setVisibility(View.VISIBLE);
        } else if (tournament.locked) {
            h.txtBadge.setText("CONCLUSO");
            int muted = ContextCompat.getColor(activity, R.color.list_text_muted);
            h.txtBadge.setTextColor(muted);
            h.txtBadge.setBackgroundResource(R.drawable.bg_badge_locked);
            h.txtBadge.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_lock_small, 0, 0, 0);
            TextViewCompat.setCompoundDrawableTintList(h.txtBadge,
                    ColorStateList.valueOf(muted));
            h.txtBadge.setVisibility(View.VISIBLE);
        } else {
            h.txtBadge.setVisibility(View.GONE);
        }

        // Info line: "N squadre · N partite · formato [· concluso]"
        int nTeams = tournament.teams.size();
        int nMatches = tournament.matches.size();

        StringBuilder info = new StringBuilder();
        info.append(nTeams).append(" squadre");
        if (nMatches > 0) {
            info.append(" · ").append(nMatches).append(" partite");
            info.append(" · ").append(getFormatLabel(tournament));
        }
        h.txtInfo.setText(info.toString());

        // Progress bar: solo se attivo e con partite
        if (tournament.isValid && nMatches > 0) {
            int played = countPlayedMatches(tournament);
            h.llProgress.setVisibility(View.VISIBLE);
            h.progressBar.setMax(nMatches);
            h.progressBar.setProgress(played);
            h.txtProgress.setText(played + "/" + nMatches);
        } else {
            h.llProgress.setVisibility(View.GONE);
        }

        // Menu tre puntini -> bottom sheet gestione torneo
        h.btnMenu.setOnClickListener(v ->
            ManageTournamentBottomSheet.newInstance(tournament.key)
                    .show(activity.getSupportFragmentManager(), "manageTournament"));

    }

    private String getFormatLabel(Tournament t) {
        if (t.nBracket >= 2) {
            return t.nBracket + " gironi";
        }
        for (Match m : t.matches) {
            if (m.hasSource1() || m.hasSource2()) {
                return "champions";
            }
        }
        return "all'italiana";
    }

    private int countPlayedMatches(Tournament t) {
        int count = 0;
        for (Match m : t.matches) {
            if (m.points1 > 0 || m.points2 > 0) count++;
        }
        return count;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout llTournament, llProgress;
        final TextView txtTournamentN, txtDate, txtInfo, txtBadge, txtProgress;
        final ImageView btnMenu;
        final ProgressBar progressBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            llTournament = itemView.findViewById(R.id.llTournament);
            txtTournamentN = itemView.findViewById(R.id.txtTournamentN);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtInfo = itemView.findViewById(R.id.txtInfo);
            txtBadge = itemView.findViewById(R.id.txtBadge);
            btnMenu = itemView.findViewById(R.id.btnMenu);
            llProgress = itemView.findViewById(R.id.llProgress);
            progressBar = itemView.findViewById(R.id.progressBar);
            txtProgress = itemView.findViewById(R.id.txtProgress);
        }
    }
}
