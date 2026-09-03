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

import com.example.myapplication.Utility.ScheduleOptions;
import com.example.myapplication.Utility.TimeUtility;
import com.example.myapplication.Utility.TournamentScheduleUtility;
import com.example.myapplication.Utility.TournamentUtility;

import java.util.ArrayList;

public class ActivityPopUpGenerateBracket extends AppCompatActivity {

    private ScheduleOptions.Format format = ScheduleOptions.Format.ITALIAN;

    private String tournamentKey;

    private LinearLayout llTimes, llNBrackets, llMatchesPerTeam, llHomeAndAway;
    private TextView btnFormatItalian, btnFormatGroups, btnFormatChampions, txtMatchCount, txtNotHosted;
    private EditText txtNBrackets, txtMatchesPerTeam, txtMinutesForMatch;
    private CheckBox chkHomeAndAway;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_generate_bracket);

        // La modale si adatta al contenuto; la lista fasce ha un tetto (MaxHeightScrollView)
        // così con tante fasce scrolla e i tasti restano visibili.
        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
        MaxHeightScrollView scrollTimes = findViewById(R.id.scrollTimes);
        float density = getResources().getDisplayMetrics().density;
        int chromeHeight = (int) (430 * density); // spazio per titolo + toggle + campi + tasti
        int cap = getResources().getDisplayMetrics().heightPixels - chromeHeight;
        scrollTimes.setMaxHeight(Math.max((int) (120 * density), cap));

        tournamentKey = getIntent().getExtras().getString("tournament_key");

        llTimes = findViewById(R.id.llTimes);
        llNBrackets = findViewById(R.id.llNBrackets);
        llMatchesPerTeam = findViewById(R.id.llMatchesPerTeam);
        llHomeAndAway = findViewById(R.id.llHomeAndAway);
        btnFormatItalian = findViewById(R.id.btnFormatItalian);
        btnFormatGroups = findViewById(R.id.btnFormatGroups);
        btnFormatChampions = findViewById(R.id.btnFormatChampions);
        txtMatchCount = findViewById(R.id.txtMatchCount);
        txtNotHosted = findViewById(R.id.txtNotHosted);
        txtNBrackets = findViewById(R.id.txtNBrackets);
        txtMatchesPerTeam = findViewById(R.id.txtMatchesPerTeam);
        txtMinutesForMatch = findViewById(R.id.txtMinutesForMatch);
        chkHomeAndAway = findViewById(R.id.chkHomeAndAway);

        btnFormatItalian.setOnClickListener(v -> setFormat(ScheduleOptions.Format.ITALIAN));
        btnFormatGroups.setOnClickListener(v -> setFormat(ScheduleOptions.Format.GROUPS));
        btnFormatChampions.setOnClickListener(v -> setFormat(ScheduleOptions.Format.CHAMPIONS));

        TextView btnAddTimes = findViewById(R.id.btnAddTimes);
        btnAddTimes.setOnClickListener(v -> addSlotRow(-1, "", ""));

        TextWatcher updateInfoWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { updateInfo(); }
        };
        txtMinutesForMatch.addTextChangedListener(updateInfoWatcher);
        txtNBrackets.addTextChangedListener(updateInfoWatcher);
        txtMatchesPerTeam.addTextChangedListener(updateInfoWatcher);
        chkHomeAndAway.setOnCheckedChangeListener((buttonView, isChecked) -> updateInfo());

        addSlotRow(-1, "", "");
        setFormat(ScheduleOptions.Format.ITALIAN);

        Button btnConfirm = findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(v -> confirm());

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());
    }

    /** Cambia formato: aggiorna toggle, campi extra e info. */
    private void setFormat(ScheduleOptions.Format newFormat) {
        format = newFormat;

        styleSegment(btnFormatItalian, format == ScheduleOptions.Format.ITALIAN);
        styleSegment(btnFormatGroups, format == ScheduleOptions.Format.GROUPS);
        styleSegment(btnFormatChampions, format == ScheduleOptions.Format.CHAMPIONS);

        llNBrackets.setVisibility(format == ScheduleOptions.Format.GROUPS ? View.VISIBLE : View.GONE);
        llMatchesPerTeam.setVisibility(format == ScheduleOptions.Format.CHAMPIONS ? View.VISIBLE : View.GONE);
        llHomeAndAway.setVisibility(format == ScheduleOptions.Format.CHAMPIONS ? View.GONE : View.VISIBLE);

        updateInfo();
    }

    private void styleSegment(TextView segment, boolean selected) {
        segment.setBackground(selected ? ContextCompat.getDrawable(this, R.drawable.bg_segment_selected) : null);
        segment.setTextColor(ContextCompat.getColor(this, selected ? R.color.white : R.color.list_text_muted));
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

    /** Legge le fasce come lista piatta [giorno, inizio, fine, ...], saltando le righe vuote. */
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

    /** Partite da generare secondo il formato scelto (0 se i parametri non sono validi). */
    private int computeMatches() {
        int nTeams = TournamentUtility.getTournamentByKey(tournamentKey).teams.size();
        boolean homeAndAway = chkHomeAndAway.isChecked();

        switch (format) {
            case GROUPS: {
                int nBrackets = parseIntOrZero(String.valueOf(txtNBrackets.getText()));
                if (nBrackets <= 0) return 0;
                return TournamentScheduleUtility.nMatchesMultipleBracket(tournamentKey, nBrackets) * (homeAndAway ? 2 : 1);
            }
            case CHAMPIONS: {
                int k = parseIntOrZero(String.valueOf(txtMatchesPerTeam.getText()));
                if (!TournamentScheduleUtility.isValidChampions(nTeams, k)) return 0;
                return TournamentScheduleUtility.nMatchesChampions(nTeams, k);
            }
            default:
                return TournamentScheduleUtility.nMatchesItalianBracket(tournamentKey) * (homeAndAway ? 2 : 1);
        }
    }

    /** Aggiorna l'info: "N PARTITE DA GENERARE PER H ORE E M MIN" + indicatore basta/non basta. */
    @SuppressLint("SetTextI18n")
    private void updateInfo() {
        int minutes = parseIntOrZero(String.valueOf(txtMinutesForMatch.getText()));
        int nMatches = computeMatches();
        int capacity = TimeUtility.capacityMatches(readSlots(), minutes);

        String line = nMatches + " " + getString(R.string.matches_to_generate);
        if (nMatches > 0 && minutes > 0) {
            line += " " + getString(R.string.for_time) + " " + timeText(nMatches * minutes);
        }
        txtMatchCount.setText(line);

        if (nMatches > 0 && minutes > 0) {
            boolean enough = capacity >= nMatches;
            txtNotHosted.setText(enough ? " ✓" : " !");
            txtNotHosted.setTextColor(ContextCompat.getColor(this,
                    enough ? R.color.button_confirm : R.color.button_danger));
            txtNotHosted.setVisibility(View.VISIBLE);
        } else {
            txtNotHosted.setVisibility(View.GONE);
        }
    }

    /** Tempo totale in testo esteso, es. "22 ORE E 45 MIN" (o "45 MIN" / "2 ORE"). */
    private String timeText(int totalMinutes) {
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;

        if (h == 0) {
            return m + " " + getString(R.string.minutes_short);
        }
        String hh = h + " " + getString(h == 1 ? R.string.hour : R.string.hours);
        return m == 0 ? hh : hh + " " + getString(R.string.and_word) + " " + m + " " + getString(R.string.minutes_short);
    }

    private void confirm() {
        ArrayList<String> slots = readSlots();
        int minutes = parseIntOrZero(String.valueOf(txtMinutesForMatch.getText()));
        if (minutes <= 0) {
            Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
            return;
        }

        int nTeams = TournamentUtility.getTournamentByKey(tournamentKey).teams.size();
        boolean homeAndAway = chkHomeAndAway.isChecked();

        switch (format) {
            case GROUPS: {
                int nBrackets = parseIntOrZero(String.valueOf(txtNBrackets.getText()));
                if (nBrackets <= 0) {
                    Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
                } else if (nBrackets > (nTeams / 2)) {
                    Toast.makeText(this, R.string.too_many_brackets, Toast.LENGTH_SHORT).show();
                } else if (TimeUtility.checkInputTime(slots, minutes,
                        TournamentScheduleUtility.nMatchesMultipleBracket(tournamentKey, nBrackets) * (homeAndAway ? 2 : 1))) {
                    createMultipleBracket(slots, String.valueOf(txtMinutesForMatch.getText()), nBrackets, homeAndAway);
                } else {
                    Toast.makeText(this, R.string.need_more_time, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case CHAMPIONS: {
                int k = parseIntOrZero(String.valueOf(txtMatchesPerTeam.getText()));
                if (!TournamentScheduleUtility.isValidChampions(nTeams, k)) {
                    Toast.makeText(this, R.string.invalid_champions_config, Toast.LENGTH_SHORT).show();
                } else if (TimeUtility.checkInputTime(slots, minutes, TournamentScheduleUtility.nMatchesChampions(nTeams, k))) {
                    createChampions(slots, String.valueOf(txtMinutesForMatch.getText()), k);
                } else {
                    Toast.makeText(this, R.string.need_more_time, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            default: {
                if (TimeUtility.checkInputTime(slots, minutes,
                        TournamentScheduleUtility.nMatchesItalianBracket(tournamentKey) * (homeAndAway ? 2 : 1))) {
                    createItalianBracket(slots, String.valueOf(txtMinutesForMatch.getText()), homeAndAway);
                } else {
                    Toast.makeText(this, R.string.need_more_time, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void createMultipleBracket(ArrayList<String> stringsInput, String txtMinutesForMatch, int nBrackets, boolean homeAndAway) {
        Toast.makeText(this, R.string.generating, Toast.LENGTH_LONG).show();
        TournamentScheduleUtility.generateMultipleBracket(tournamentKey, stringsInput, txtMinutesForMatch, nBrackets, homeAndAway);
        finish();
    }

    private void createItalianBracket(ArrayList<String> stringsInput, String txtMinutesForMatch, boolean homeAndAway) {
        Toast.makeText(this, R.string.generating, Toast.LENGTH_LONG).show();
        TournamentScheduleUtility.generateItalianBracket(tournamentKey, stringsInput, txtMinutesForMatch, homeAndAway);
        finish();
    }

    private void createChampions(ArrayList<String> stringsInput, String txtMinutesForMatch, int k) {
        Toast.makeText(this, R.string.generating, Toast.LENGTH_LONG).show();
        TournamentScheduleUtility.generateChampions(tournamentKey, stringsInput, txtMinutesForMatch, k);
        finish();
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
