package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Utility.TeamGeneratorUtility;
import com.example.myapplication.Utility.TimeUtility;
import com.example.myapplication.Utility.TournamentScheduleUtility;
import com.example.myapplication.Utility.TournamentUtility;
import com.example.myapplication.Utility.Utility;

import java.util.ArrayList;

public class ActivityPopUpGenerateBracket extends AppCompatActivity {
    private int count = 0;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_generate_bracket);

        String type = getIntent().getExtras().getString("bracket_type");
        String tournamentKey = getIntent().getExtras().getString("tournament_key");
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        ArrayList<EditText> txtTimeSections = new ArrayList<>();

        TextView txtTitle = findViewById(R.id.txtTitle);
        TextView txtTimeNeed = findViewById(R.id.txtTimeNeed);
        EditText txtNBrackets = findViewById(R.id.txtNBrackets);
        EditText txtMinutesForMatch = findViewById(R.id.txtMinutesForMatch);
        LinearLayout llTimes = findViewById(R.id.llTimes);

        Button btnConfirm = findViewById(R.id.btnConfirm);
        Button btnAddTimes = findViewById(R.id.btnAddTimes);
        btnAddTimes.setOnClickListener(v -> {
            count ++;
            LayoutInflater inflater = LayoutInflater.from(this);

            View viewDay = inflater.inflate(R.layout.layout_time, llTimes, false);
            TextView txtDay = viewDay.findViewById(R.id.txtDescription);
            txtDay.setText(getResources().getText(R.string.day_number));
            EditText txtInputDay = viewDay.findViewById(R.id.txtInput);
            txtInputDay.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
            txtInputDay.setHint(getResources().getText(R.string.n));
            llTimes.addView(viewDay);

            View viewStart = inflater.inflate(R.layout.layout_time, llTimes, false);
            TextView txtStart = viewStart.findViewById(R.id.txtDescription);
            txtStart.setText(getResources().getText(R.string.start_time_section) + " " + count);
            EditText txtInputStart = viewStart.findViewById(R.id.txtInput);
            llTimes.addView(viewStart);

            View viewEnd = inflater.inflate(R.layout.layout_time, llTimes, false);
            TextView txtEnd = viewEnd.findViewById(R.id.txtDescription);
            txtEnd.setText(getResources().getText(R.string.end_time_section) + " " + count);
            EditText txtInputEnd = viewEnd.findViewById(R.id.txtInput);
            llTimes.addView(viewEnd);

            txtTimeSections.add(txtInputDay);
            txtTimeSections.add(txtInputStart);
            txtTimeSections.add(txtInputEnd);
        });

        TextWatcher updateTimeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                txtTimeNeed.setText(getNeedTime(tournamentKey, type, txtNBrackets, txtMinutesForMatch));
            }
        };

        txtMinutesForMatch.addTextChangedListener(updateTimeWatcher);
        txtNBrackets.addTextChangedListener(updateTimeWatcher);

        switch (type) {
            case "MULTIPLE_BRACKET": {
                txtTitle.setText(R.string.generate_multiple_brackets);

                btnConfirm.setOnClickListener(v -> {
                    ArrayList<String> stringsInput = new ArrayList<>();

                    for (int i = 0; i < txtTimeSections.size(); i++) {
                        stringsInput.add(String.valueOf(txtTimeSections.get(i).getText()));
                    }

                    try {
                        int minutesForMatch = Integer.parseInt(String.valueOf(txtMinutesForMatch.getText()));
                        int nBrackets = Integer.parseInt(String.valueOf(txtNBrackets.getText()));

                        if (Integer.parseInt(txtNBrackets.getText().toString()) > (TournamentUtility.getTournamentByKey(tournamentKey).teams.size() / 2)) {
                            Toast.makeText(this, R.string.too_many_brackets, Toast.LENGTH_SHORT).show();
                        } else if (TimeUtility.checkInputTime(stringsInput, minutesForMatch, TournamentScheduleUtility.nMatchesMultipleBracket(tournamentKey, nBrackets))) {
                            createMultipleBracket(tournament.key, stringsInput, String.valueOf(txtMinutesForMatch.getText()), Integer.parseInt(String.valueOf(txtNBrackets.getText())));
                            txtTimeSections.clear();
                        } else {
                            Toast.makeText(this, R.string.need_more_time, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ignored) { }
                });
                break;
            }
            case "ITALIAN_BRACKET": {
                txtTitle.setText(R.string.generate_italian_bracket);

                findViewById(R.id.llNBrackets).setVisibility(View.GONE);

                btnConfirm.setOnClickListener(v -> {
                    ArrayList<String> stringsInput = new ArrayList<>();

                    for (int i = 0; i < txtTimeSections.size(); i++) {
                        stringsInput.add(String.valueOf(txtTimeSections.get(i).getText()));
                    }

                    try {
                        int minutesForMatch = Integer.parseInt(String.valueOf(txtMinutesForMatch.getText()));

                        if (TimeUtility.checkInputTime(stringsInput, minutesForMatch, TournamentScheduleUtility.nMatchesItalianBracket(tournamentKey))) {
                            createItalianBracket(tournament.key, stringsInput, String.valueOf(txtMinutesForMatch.getText()));
                            txtTimeSections.clear();
                        } else {
                            Toast.makeText(this, R.string.need_more_time, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ignored) {
                    }
                });
                break;
            }
        }

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());
    }

    private void createMultipleBracket(String tournamentKey, ArrayList<String> stringsInput, String txtMinutesForMatch, int txtNBrackets) {
        Toast.makeText(this, R.string.generating, Toast.LENGTH_LONG).show();
        TournamentScheduleUtility.generateMultipleBracket(tournamentKey, stringsInput, txtMinutesForMatch, txtNBrackets);
        finish();
    }

    private void createItalianBracket(String tournamentKey, ArrayList<String> stringsInput, String txtMinutesForMatch) {
        Toast.makeText(this, R.string.generating, Toast.LENGTH_LONG).show();
        TournamentScheduleUtility.generateItalianBracket(tournamentKey, stringsInput, txtMinutesForMatch);
        finish();
    }

    @SuppressLint("DefaultLocale")
    private String getNeedTime(String tournamentKey, String type, TextView txtNBrackets, TextView txtMinutesForMatch) {
        try {
            int minutesPerMatch = Integer.parseInt(txtMinutesForMatch.getText().toString());

            if (type.equals("MULTIPLE_BRACKET") && minutesPerMatch > 0) {
                int nBrackets = Integer.parseInt(txtNBrackets.getText().toString());
                int time = TournamentScheduleUtility.nMatchesMultipleBracket(tournamentKey, nBrackets) * minutesPerMatch;

                if (nBrackets > 0) {
                    return getString(R.string.time_need) + " " + time / 60 + ":" + String.format("%02d", time % 60);
                }

            } else if (type.equals("ITALIAN_BRACKET") && minutesPerMatch > 0) {
                int time = TournamentScheduleUtility.nMatchesItalianBracket(tournamentKey) * minutesPerMatch;
                return getString(R.string.time_need) + " " + time / 60 + ":" + String.format("%02d", time % 60);
            }

        } catch (Exception ignored) {
            return getString(R.string.time_need);
        }

        return getString(R.string.time_need);
    }
}