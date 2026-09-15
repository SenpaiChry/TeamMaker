package com.teammaker.app.update;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.teammaker.app.BuildConfig;
import com.teammaker.app.R;

/**
 * Popup di conferma aggiornamento. Mostra "versione X → Y", changelog e i tasti
 * AGGIORNA / PIU' TARDI. AGGIORNA delega a {@link AppUpdater#downloadAndInstall}.
 *
 * Se l'extra "mandatory" e' true (versione installata sotto teammaker/
 * app_min_version_code), il popup diventa un muro: niente "PIU' TARDI",
 * back disabilitato, touch fuori non chiude. L'utente non puo' usare l'app
 * finche' non aggiorna.
 */
public class UpdatePopupActivity extends AppCompatActivity {

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
        boolean mandatory = getIntent().getBooleanExtra("mandatory", false);

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
        Button btnLater = findViewById(R.id.btnLater);

        // Modalita' mandatory: niente scampo. Nasconde PIU' TARDI, disabilita back,
        // e non chiama finish() dopo il download (l'utente vede solo la schermata
        // dell'installer di sistema).
        if (mandatory) {
            btnLater.setVisibility(View.GONE);
            setFinishOnTouchOutside(false);
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    // Back consumato: non si esce.
                }
            });
        }

        btnUpdate.setOnClickListener(v -> {
            if (apkUrl == null || apkUrl.isEmpty()) {
                if (!mandatory) finish();
                return;
            }
            AppUpdater.downloadAndInstall(this, apkUrl, newVersionCode);
            // In mandatory il popup resta aperto sotto l'installer di sistema.
            // Se l'utente annulla l'installazione, torna qui e vede sempre solo AGGIORNA.
            if (!mandatory) finish();
        });

        btnLater.setOnClickListener(v -> finish());
    }
}
