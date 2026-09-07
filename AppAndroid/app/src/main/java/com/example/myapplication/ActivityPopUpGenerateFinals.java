package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.Utility.FinalStageGenerator;
import com.example.myapplication.Utility.FinalStageResolver;
import com.example.myapplication.Utility.MatchUtility;
import com.example.myapplication.Utility.TimeUtility;
import com.example.myapplication.Utility.TournamentScheduleUtility;
import com.example.myapplication.Utility.TournamentUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityPopUpGenerateFinals extends AppCompatActivity {

    private String tournamentKey;
    private Tournament tournament;

    private String quarterLabel, semiLabel, finalLabel, thirdLabel;

    private int bracketSize = 0;
    private int lastMatchCount = 0;
    private List<Integer> availableSizes;

    private LinearLayout llTimes;
    private TextView btnPhaseQuarter, btnPhaseSemi, btnPhaseFinal, txtPreview, txtMatchCount, txtNotHosted;
    private LinearLayout llThirdPlace;
    private CheckBox chkThirdPlace;
    private EditText txtMinutesForMatch;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_generate_finals);

        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
        MaxHeightScrollView scrollContent = findViewById(R.id.scrollContent);
        float density = getResources().getDisplayMetrics().density;
        int cap = getResources().getDisplayMetrics().heightPixels - (int) (200 * density);
        scrollContent.setMaxHeight(Math.max((int) (160 * density), cap));

        tournamentKey = getIntent().getExtras().getString("tournament_key");
        tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        quarterLabel = getString(R.string.quarter);
        semiLabel = getString(R.string.semifinal);
        finalLabel = getString(R.string.finalString);
        thirdLabel = getString(R.string.finalina);

        llTimes = findViewById(R.id.llTimes);
        btnPhaseQuarter = findViewById(R.id.btnPhaseQuarter);
        btnPhaseSemi = findViewById(R.id.btnPhaseSemi);
        btnPhaseFinal = findViewById(R.id.btnPhaseFinal);
        llThirdPlace = findViewById(R.id.llThirdPlace);
        chkThirdPlace = findViewById(R.id.chkThirdPlace);
        txtMinutesForMatch = findViewById(R.id.txtMinutesForMatch);
        txtPreview = findViewById(R.id.txtPreview);
        txtMatchCount = findViewById(R.id.txtMatchCount);
        txtNotHosted = findViewById(R.id.txtNotHosted);

        Button btnConfirm = findViewById(R.id.btnConfirm);
        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());

        availableSizes = FinalStageGenerator.availableSizes(tournament);
        if (availableSizes.isEmpty()) {
            findViewById(R.id.txtNoConfig).setVisibility(View.VISIBLE);
            findViewById(R.id.llPhaseToggle).setVisibility(View.GONE);
            llThirdPlace.setVisibility(View.GONE);
            btnConfirm.setEnabled(false);
            return;
        }

        btnPhaseQuarter.setVisibility(availableSizes.contains(FinalStageGenerator.QUARTER) ? View.VISIBLE : View.GONE);
        btnPhaseSemi.setVisibility(availableSizes.contains(FinalStageGenerator.SEMI) ? View.VISIBLE : View.GONE);
        btnPhaseFinal.setVisibility(availableSizes.contains(FinalStageGenerator.FINAL) ? View.VISIBLE : View.GONE);

        btnPhaseQuarter.setOnClickListener(v -> setBracketSize(FinalStageGenerator.QUARTER));
        btnPhaseSemi.setOnClickListener(v -> setBracketSize(FinalStageGenerator.SEMI));
        btnPhaseFinal.setOnClickListener(v -> setBracketSize(FinalStageGenerator.FINAL));

        chkThirdPlace.setOnCheckedChangeListener((b, c) -> { updatePreview(); updateInfo(); });

        TextView btnAddTimes = findViewById(R.id.btnAddTimes);
        btnAddTimes.setOnClickListener(v -> addSlotRow(-1, "", ""));

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { updateInfo(); }
        };
        txtMinutesForMatch.addTextChangedListener(watcher);

        addSlotRow(-1, "", "");
        setBracketSize(availableSizes.get(0)); // parte dalla fase più ampia disponibile

        btnConfirm.setOnClickListener(v -> confirm());
    }

    private void setBracketSize(int size) {
        bracketSize = size;
        styleSegment(btnPhaseQuarter, size == FinalStageGenerator.QUARTER);
        styleSegment(btnPhaseSemi, size == FinalStageGenerator.SEMI);
        styleSegment(btnPhaseFinal, size == FinalStageGenerator.FINAL);

        // 3°/4° posto: dai perdenti delle semifinali (quarti/semi), oppure 3a vs 4a
        // di classifica quando si parte dalla finale (se esistono almeno 4 squadre/qualificate).
        boolean canThird = size >= FinalStageGenerator.SEMI
                || (size == FinalStageGenerator.FINAL && availableSizes.contains(FinalStageGenerator.SEMI));
        llThirdPlace.setVisibility(canThird ? View.VISIBLE : View.GONE);
        if (!canThird) {
            chkThirdPlace.setChecked(false);
        }

        updatePreview();
        updateInfo();
    }

    private void styleSegment(TextView segment, boolean selected) {
        segment.setBackground(selected ? ContextCompat.getDrawable(this, R.drawable.bg_segment_selected) : null);
        segment.setTextColor(ContextCompat.getColor(this, selected ? R.color.white : R.color.list_text_muted));
    }

    /** Ricostruisce l'anteprima del tabellone con i placeholder leggibili. */
    @SuppressLint("SetTextI18n")
    private void updatePreview() {
        List<Match> matches = FinalStageGenerator.generate(tournament, bracketSize, chkThirdPlace.isChecked(),
                quarterLabel, semiLabel, finalLabel, thirdLabel);
        lastMatchCount = matches.size();

        Map<String, String> code = new HashMap<>();
        int qi = 1, si = 1;
        for (Match m : matches) {
            if (m.type.equals(quarterLabel)) code.put(m.key, "Q" + (qi++));
            else if (m.type.equals(semiLabel)) code.put(m.key, "S" + (si++));
            else if (m.type.equals(finalLabel)) code.put(m.key, "F");
            else code.put(m.key, "3°/4°");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            Match m = matches.get(i);
            if (i > 0) sb.append("\n");
            sb.append(code.get(m.key)).append(":  ")
                    .append(slotLabel(m.source1Type, m.source1Ref, code))
                    .append("   -   ")
                    .append(slotLabel(m.source2Type, m.source2Ref, code));
        }
        txtPreview.setText(sb.toString());
    }

    private String slotLabel(String type, String ref, Map<String, String> code) {
        switch (type) {
            case "STANDING":
                return ref + getString(R.string.ordinal_a);
            case "GROUP_STANDING": {
                int split = 0;
                while (split < ref.length() && !Character.isDigit(ref.charAt(split))) split++;
                String letter = ref.substring(0, split);
                String pos = ref.substring(split);
                return pos + getString(R.string.ordinal_a) + " " + getString(R.string.group_abbr) + " " + letter;
            }
            case "WINNER":
                return getString(R.string.winner_abbr) + " " + code.get(ref);
            case "LOSER":
                return getString(R.string.loser_abbr) + " " + code.get(ref);
            default:
                return "TO DO";
        }
    }

    private void confirm() {
        List<Match> matches = FinalStageGenerator.generate(tournament, bracketSize, chkThirdPlace.isChecked(),
                quarterLabel, semiLabel, finalLabel, thirdLabel);
        if (matches.isEmpty()) {
            Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> slots = readSlots();
        int minutes = parseIntOrZero(String.valueOf(txtMinutesForMatch.getText()));
        if (minutes <= 0) {
            Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TimeUtility.checkInputTime(slots, minutes, matches.size())) {
            Toast.makeText(this, R.string.need_more_time, Toast.LENGTH_SHORT).show();
            return;
        }

        // La finalina si gioca prima della finale: mettila penultima per lo scheduling
        int n = matches.size();
        if (n >= 2 && matches.get(n - 1).type.equals(thirdLabel) && matches.get(n - 2).type.equals(finalLabel)) {
            Collections.swap(matches, n - 1, n - 2);
        }

        TournamentScheduleUtility.assignDayAndTime(matches, slots, minutes);
        MatchUtility.saveMatches(tournamentKey, matches);
        tournament.matches.addAll(matches);
        FinalStageResolver.resolveAll(tournamentKey);

        Toast.makeText(this, R.string.generating, Toast.LENGTH_LONG).show();
        finish();
    }

    /** Aggiunge una riga fascia (giorno negativo / orari vuoti = campo vuoto). */
    private void addSlotRow(int day, String start, String end) {
        View row = LayoutInflater.from(this).inflate(R.layout.layout_time_row, llTimes, false);
        EditText etDay = row.findViewById(R.id.etDay);
        EditText etStart = row.findViewById(R.id.etStart);
        EditText etEnd = row.findViewById(R.id.etEnd);
        if (day >= 0) etDay.setText(String.valueOf(day));
        if (!start.isEmpty()) etStart.setText(start);
        if (!end.isEmpty()) etEnd.setText(end);

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { updateInfo(); }
        };
        etDay.addTextChangedListener(watcher);
        etStart.addTextChangedListener(watcher);
        etEnd.addTextChangedListener(watcher);

        row.findViewById(R.id.btnRemoveSlot).setOnClickListener(v -> {
            llTimes.removeView(row);
            updateInfo();
        });

        llTimes.addView(row);
        updateInfo();
    }

    private ArrayList<String> readSlots() {
        ArrayList<String> slots = new ArrayList<>();
        for (int i = 0; i < llTimes.getChildCount(); i++) {
            View row = llTimes.getChildAt(i);
            String day = text(row, R.id.etDay);
            String start = text(row, R.id.etStart);
            String end = text(row, R.id.etEnd);
            if (day.isEmpty() && start.isEmpty() && end.isEmpty()) continue;
            slots.add(day);
            slots.add(start);
            slots.add(end);
        }
        return slots;
    }

    @SuppressLint("SetTextI18n")
    private void updateInfo() {
        int minutes = parseIntOrZero(String.valueOf(txtMinutesForMatch.getText()));
        int capacity = TimeUtility.capacityMatches(readSlots(), minutes);

        String line = lastMatchCount + " " + getString(R.string.matches_to_generate);
        if (lastMatchCount > 0 && minutes > 0) {
            boolean enough = capacity >= lastMatchCount;
            txtNotHosted.setText(enough ? " ✓" : " !");
            txtNotHosted.setTextColor(ContextCompat.getColor(this,
                    enough ? R.color.button_confirm : R.color.button_danger));
            txtNotHosted.setVisibility(View.VISIBLE);
        } else {
            txtNotHosted.setVisibility(View.GONE);
        }
        txtMatchCount.setText(line);
    }

    private String text(View row, int id) {
        return ((EditText) row.findViewById(id)).getText().toString().trim();
    }

    private int parseIntOrZero(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
