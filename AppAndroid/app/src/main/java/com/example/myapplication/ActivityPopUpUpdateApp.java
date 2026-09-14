package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Utility.UpdateUtility;

/**
 * Popup di conferma aggiornamento. Mostra "versione X → Y", changelog e i tasti
 * AGGIORNA / PIU' TARDI. AGGIORNA delega a {@link UpdateUtility#downloadAndInstall}.
 */
public class ActivityPopUpUpdateApp extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_update_app);

        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }

        int newVersionCode = getIntent().getIntExtra("new_version_code", 0);
        String newVersionName = getIntent().getStringExtra("new_version_name");
        String apkUrl = getIntent().getStringExtra("apk_url");
        String changelog = getIntent().getStringExtra("changelog");

        TextView txtVersion = findViewById(R.id.txtVersion);
        String currentName = BuildConfig.VERSION_NAME;
        int currentCode = BuildConfig.VERSION_CODE;
        String from = currentName + " (" + currentCode + ")";
        String to = (newVersionName != null && !newVersionName.isEmpty())
                ? newVersionName + " (" + newVersionCode + ")"
                : String.valueOf(newVersionCode);
        txtVersion.setText(from + "  →  " + to);

        TextView txtChangelog = findViewById(R.id.txtChangelog);
        if (changelog == null || changelog.trim().isEmpty()) {
            txtChangelog.setVisibility(View.GONE);
        } else {
            txtChangelog.setText(changelog);
        }

        Button btnUpdate = findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(v -> {
            if (apkUrl == null || apkUrl.isEmpty()) {
                finish();
                return;
            }
            UpdateUtility.downloadAndInstall(this, apkUrl, newVersionCode);
            finish();
        });

        Button btnLater = findViewById(R.id.btnLater);
        btnLater.setOnClickListener(v -> finish());
    }
}
