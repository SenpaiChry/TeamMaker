package com.teammaker.app.ui.popup;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.teammaker.app.bus.DataChangeBus;
import com.teammaker.app.data.model.Match;
import com.teammaker.app.data.model.PopUpType;
import com.teammaker.app.data.model.Team;
import com.teammaker.app.data.model.Tournament;
import com.teammaker.app.data.repository.MatchRepository;
import com.teammaker.app.data.repository.TournamentRepository;
import com.teammaker.app.domain.MatchPhases;
import com.teammaker.app.ui.activity.ManageMatchesActivity;
import com.teammaker.app.ui.activity.ManageTeamsActivity;
import com.teammaker.app.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class ManageTournamentBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TOURNAMENT_KEY = "tournament_key";
    private Tournament tournament;

    public static ManageTournamentBottomSheet newInstance(String tournamentKey) {
        ManageTournamentBottomSheet sheet = new ManageTournamentBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TOURNAMENT_KEY, tournamentKey);
        sheet.setArguments(args);
        return sheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            View sheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.setBackgroundResource(android.R.color.transparent);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_pop_up_manage_tournament, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String tournamentKey = getArguments() != null
                ? getArguments().getString(ARG_TOURNAMENT_KEY) : null;
        if (tournamentKey == null) { dismiss(); return; }

        tournament = TournamentRepository.getTournamentByKey(tournamentKey);
        if (tournament == null) { dismiss(); return; }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());

        // ---- Title + Badge ----
        TextView txtTitle = view.findViewById(R.id.txtTitle);
        txtTitle.setText(tournament.name != null && !tournament.name.isEmpty()
                ? tournament.name : getString(R.string.tournament));

        TextView txtBadge = view.findViewById(R.id.txtBadge);
        updateBadge(txtBadge);

        // ---- Name + Date ----
        EditText txtName = view.findViewById(R.id.txtName);
        if (tournament.name != null) txtName.setText(tournament.name);

        EditText txtDate = view.findViewById(R.id.txtDate);
        if (tournament.date != null) {
            txtDate.setText(sdf.format(tournament.date.getTime()));
        }
        TextViewCompat.setCompoundDrawableTintList(txtDate,
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.list_text_muted)));

        txtDate.setOnClickListener(v -> {
            if (tournament.locked) return;
            Calendar date = tournament.date != null ? tournament.date : Calendar.getInstance();
            new DatePickerDialog(requireContext(),
                    (dp, y, m, d) -> {
                        String sel = String.format(Locale.getDefault(), "%02d/%02d/%02d", d, m + 1, y % 100);
                        txtDate.setText(sel);
                    },
                    date.get(Calendar.YEAR), date.get(Calendar.MONTH),
                    date.get(Calendar.DAY_OF_MONTH)).show();
        });

        // ---- Save link (appears on change) ----
        String originalName = tournament.name != null ? tournament.name : "";
        String originalDate = tournament.date != null ? sdf.format(tournament.date.getTime()) : "";
        TextView btnSave = view.findViewById(R.id.btnSaveNameDate);

        TextWatcher changeWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                boolean changed = !txtName.getText().toString().equals(originalName)
                        || !txtDate.getText().toString().equals(originalDate);
                btnSave.setVisibility(changed && !tournament.locked ? View.VISIBLE : View.GONE);
            }
        };
        txtName.addTextChangedListener(changeWatcher);
        txtDate.addTextChangedListener(changeWatcher);

        btnSave.setOnClickListener(v -> {
            String name = txtName.getText().toString().trim();
            String dateStr = txtDate.getText().toString().trim();
            if (name.isEmpty() || dateStr.isEmpty()) {
                Toast.makeText(requireContext(), R.string.missing_data, Toast.LENGTH_SHORT).show();
                return;
            }
            SimpleDateFormat parseFmt = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            Calendar newDate = Calendar.getInstance();
            try {
                newDate.setTime(parseFmt.parse(dateStr));
            } catch (Exception e) {
                Log.e("ManageTournament", "Data non parseabile: " + dateStr, e);
                Toast.makeText(requireContext(), R.string.missing_data, Toast.LENGTH_SHORT).show();
                return;
            }
            TournamentRepository.updateNameAndDateTournament(tournamentKey, name, newDate);
            txtTitle.setText(name);
            btnSave.setVisibility(View.GONE);
            Toast.makeText(requireContext(), R.string.save, Toast.LENGTH_SHORT).show();
        });

        // ---- Switch: Attivo ----
        SwitchCompat switchActive = view.findViewById(R.id.switchActive);
        ImageView iconActive = view.findViewById(R.id.iconActive);
        switchActive.setChecked(tournament.isValid);
        styleSwitchColors(switchActive);
        styleTrophyIcon(iconActive, tournament.isValid);

        switchActive.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                TournamentRepository.setActiveTournament(tournamentKey);
            } else {
                TournamentRepository.deactivateAllTournaments();
            }
            tournament.isValid = checked;
            DataChangeBus.emit(DataChangeBus.Event.TOURNAMENTS);
            styleTrophyIcon(iconActive, checked);
            updateBadge(txtBadge);
        });

        // ---- Switch: Concludi torneo ----
        SwitchCompat switchLock = view.findViewById(R.id.switchLock);
        switchLock.setChecked(tournament.locked);
        styleSwitchColors(switchLock);

        switchLock.setOnCheckedChangeListener((btn, checked) -> {
            TournamentRepository.setLocked(tournament.key, checked);
            tournament.locked = checked;
            DataChangeBus.emit(DataChangeBus.Event.TOURNAMENTS);
            applyLockedState(view, checked);
            updateBadge(txtBadge);
        });

        // ---- Counts ----
        TextView txtTeamsCount = view.findViewById(R.id.txtTeamsCount);
        txtTeamsCount.setText(tournament.teams.size() + " squadre");

        TextView txtMatchesCount = view.findViewById(R.id.txtMatchesCount);
        txtMatchesCount.setText(tournament.matches.size() + " partite");

        // ---- Action cards ----
        view.findViewById(R.id.btnManageTeams).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ManageTeamsActivity.class);
            intent.putExtra("tournament_key", tournamentKey);
            intent.putExtra("read_only", tournament.locked);
            startActivity(intent);
        });

        view.findViewById(R.id.btnManageMatches).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ManageMatchesActivity.class);
            intent.putExtra("tournament_key", tournamentKey);
            intent.putExtra("read_only", tournament.locked);
            startActivity(intent);
        });

        // ---- Copy teams ----
        view.findViewById(R.id.btnCopyTeams).setOnClickListener(v -> {
            StringBuilder text = new StringBuilder();
            for (Team team : tournament.teams) {
                text.append(getString(R.string.team)).append(" ")
                        .append(tournament.getNTeamByKey(team.key));
                if (tournament.nBracket > 1) {
                    text.append(" (").append(getString(R.string.bracket))
                            .append(" ").append(team.bracket).append(")");
                }
                text.append(": ").append(team.toStringNameAndSurname()).append("\n");
            }
            copyToClipboard(text.toString());
        });

        // ---- Copy matches ----
        view.findViewById(R.id.btnCopyMatches).setOnClickListener(v -> {
            ArrayList<Match> matches = new ArrayList<>(tournament.matches);
            Collections.sort(matches, MatchRepository.BY_DAY_TIME);
            StringBuilder text = new StringBuilder();
            for (Match match : matches) {
                text.append(getString(R.string.match)).append(" ")
                        .append(tournament.getNMatchByKey(match.key))
                        .append(" - ").append(match.time)
                        .append(" (").append(MatchPhases.label(requireContext(), match.type)).append("): ")
                        .append(getString(R.string.team)).append(" ")
                        .append(tournament.getNTeamByKey(match.keyTeam1))
                        .append(" VS ").append(getString(R.string.team)).append(" ")
                        .append(tournament.getNTeamByKey(match.keyTeam2))
                        .append("\n");
            }
            copyToClipboard(text.toString());
        });

        // ---- Delete ----
        view.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ConfirmPopupActivity.class);
            intent.putExtra("tournament_key", tournament.key);
            intent.putExtra("pop_up_type", PopUpType.DELETE_TOURNAMENT);
            startActivity(intent);
            dismiss();
        });

        // Apply initial locked state
        applyLockedState(view, tournament.locked);
    }

    private void updateBadge(TextView txtBadge) {
        if (tournament.isValid) {
            txtBadge.setText("ATTIVO");
            int green = ContextCompat.getColor(requireContext(), R.color.button_confirm);
            txtBadge.setTextColor(green);
            txtBadge.setBackgroundResource(R.drawable.bg_badge_active);
            txtBadge.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_check_small, 0, 0, 0);
            TextViewCompat.setCompoundDrawableTintList(txtBadge,
                    ColorStateList.valueOf(green));
            txtBadge.setVisibility(View.VISIBLE);
        } else if (tournament.locked) {
            txtBadge.setText("CONCLUSO");
            int muted = ContextCompat.getColor(requireContext(), R.color.list_text_muted);
            txtBadge.setTextColor(muted);
            txtBadge.setBackgroundResource(R.drawable.bg_badge_locked);
            txtBadge.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_lock_small, 0, 0, 0);
            TextViewCompat.setCompoundDrawableTintList(txtBadge,
                    ColorStateList.valueOf(muted));
            txtBadge.setVisibility(View.VISIBLE);
        } else {
            txtBadge.setVisibility(View.GONE);
        }
    }

    private void applyLockedState(View root, boolean locked) {
        EditText txtName = root.findViewById(R.id.txtName);
        EditText txtDate = root.findViewById(R.id.txtDate);
        txtName.setEnabled(!locked);
        txtDate.setEnabled(!locked);
        if (locked) {
            root.findViewById(R.id.btnSaveNameDate).setVisibility(View.GONE);
        }
    }

    private void styleTrophyIcon(ImageView icon, boolean active) {
        icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                active ? R.color.button_confirm : R.color.list_text_muted)));
    }

    private void styleSwitchColors(SwitchCompat sw) {
        int colorOn = ContextCompat.getColor(requireContext(), R.color.button_confirm);
        int colorOff = ContextCompat.getColor(requireContext(), R.color.list_text_muted);
        int colorWhite = ContextCompat.getColor(requireContext(), R.color.white);
        sw.setThumbTintList(ColorStateList.valueOf(colorWhite));
        sw.setTrackTintList(new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{colorOn, colorOff}
        ));
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Testo copiato", text));
        Toast.makeText(requireContext(), "Testo copiato negli appunti", Toast.LENGTH_SHORT).show();
    }
}
