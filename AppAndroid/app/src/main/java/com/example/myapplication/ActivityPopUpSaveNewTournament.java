package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Utility.TournamentUtility;
import com.example.myapplication.Utility.Utility;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ActivityPopUpSaveNewTournament extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_save_new_tournament);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        String today = sdf.format(calendar.getTime());

        EditText txtName = findViewById(R.id.txtName);
        EditText txtDate = findViewById(R.id.txtDate);
        txtDate.setText(today);

        txtDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(ActivityPopUpSaveNewTournament.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String selectedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                        txtDate.setText(selectedDate);
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

            datePickerDialog.show();
        });

        Button btnConfirm = findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(view -> {
            String dateStr = txtDate.getText().toString();

            if (!String.valueOf(txtName.getText()).isEmpty() && !dateStr.isEmpty()) {
                Calendar newDate = Calendar.getInstance();
                try {
                    newDate.setTime(sdf.parse(dateStr));
                } catch (Exception e) {
                    Log.d("FATAL catch", e.toString());
                    return;
                }

                TournamentUtility.saveNewTournamentTeams(String.valueOf(txtName.getText()), newDate);
                Toast.makeText(this, R.string.saving_tournament, Toast.LENGTH_SHORT).show();

                ActivityTeams.activityTeams.finish();
//              TODO PER QUANDO SI VORRA' FARE SALVA E GENERA ANCORA  ActivityGenerate.activityGenerate.finish();
                TournamentActivityManageTournaments.reloadTournaments();
                finish();
            } else {
                Toast.makeText(this, getResources().getString(R.string.missing_name), Toast.LENGTH_SHORT).show();
            }
        });

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());
    }
}
