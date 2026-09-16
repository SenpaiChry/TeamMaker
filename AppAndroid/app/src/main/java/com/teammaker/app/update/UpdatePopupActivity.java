package com.teammaker.app.update;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.teammaker.app.R;

/**
 * Popup di conferma aggiornamento: titolo + AGGIORNA / PIU' TARDI.
 * AGGIORNA delega a {@link AppUpdater#downloadAndInstall}.
 *
 * Se l'extra "mandatory" e' true (versione installata sotto teammaker/
 * app_min_version_code), il popup diventa un muro: niente "PIU' TARDI",
 * back disabilitato, touch fuori non chiude. L'utente non puo' usare l'app
 * finche' non aggiorna.
 */
public class UpdatePopupActivity extends AppCompatActivity {

    /**
     * Prefisso whitelist dell'APK. Qualsiasi URL che non parta da qui viene
     * rifiutato: cosi' se qualcuno lancia questa Activity con un URL arbitrario
     * (deep link malevolo, confused deputy) non scarichiamo mai un pacchetto
     * da un dominio non fidato.
     */
    private static final String TRUSTED_APK_URL_PREFIX =
            "https://github.com/SenpaiChry/TeamMakerReleases/";

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
        String apkUrl = getIntent().getStringExtra("apk_url");
        boolean mandatory = getIntent().getBooleanExtra("mandatory", false);

        // Whitelist: accetto SOLO URL delle release ufficiali su GitHub.
        if (apkUrl == null || !apkUrl.startsWith(TRUSTED_APK_URL_PREFIX)) {
            Toast.makeText(this, R.string.update_download_failed, Toast.LENGTH_LONG).show();
            finish();
            return;
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
            AppUpdater.downloadAndInstall(this, apkUrl, newVersionCode);
            // In mandatory il popup resta aperto sotto l'installer di sistema.
            // Se l'utente annulla l'installazione, torna qui e vede sempre solo AGGIORNA.
            if (!mandatory) finish();
        });

        btnLater.setOnClickListener(v -> finish());
    }
}
