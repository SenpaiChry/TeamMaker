package com.teammaker.app.ui.popup;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.teammaker.app.bus.DataChangeBus;
import com.teammaker.app.data.repository.TournamentRepository;
import com.teammaker.app.util.NetworkUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import com.teammaker.app.ui.activity.GenerateActivity;
import com.teammaker.app.ui.activity.TeamsActivity;
import com.teammaker.app.R;

public class SaveNewTournamentPopupActivity extends AppCompatActivity {

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
            DatePickerDialog datePickerDialog = new DatePickerDialog(SaveNewTournamentPopupActivity.this,
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
                    Log.e("SaveNewTournament", "Data non parseabile: " + dateStr, e);
                    Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
                    return;
                }

                TournamentRepository.saveNewTournamentTeams(String.valueOf(txtName.getText()), newDate);
                Toast.makeText(this, R.string.saving_tournament, Toast.LENGTH_SHORT).show();

                TeamsActivity a = TeamsActivity.get();
                if (a != null) a.finish();
//              TODO PER QUANDO SI VORRA' FARE SALVA E GENERA ANCORA  GenerateActivity g = GenerateActivity.get(); if (g != null) g.finish();
                DataChangeBus.emit(DataChangeBus.Event.TOURNAMENTS);
                finish();
            } else {
                Toast.makeText(this, getResources().getString(R.string.missing_name), Toast.LENGTH_SHORT).show();
            }
        });

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());
    }
}
