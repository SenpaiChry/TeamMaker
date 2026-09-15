package com.teammaker.app.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.teammaker.app.data.model.Constants;
import com.teammaker.app.auth.AdminAuth;
import com.teammaker.app.R;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextView txtWrongPassword = findViewById(R.id.txtWrongPassword);
        EditText txtPassword = findViewById(R.id.txtPassword);

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> finish());

        Button btnAccessAdmin = findViewById(R.id.btnAccessAdmin);
        btnAccessAdmin.setOnClickListener(v -> {
            btnAccessAdmin.setEnabled(false);
            String password = txtPassword.getText().toString();
            // Login via Firebase Auth: la password si confronta lato server, non
            // vive piu' nel DB. UX invariata: l'utente vede solo la password.
            AdminAuth.signIn(password, new AdminAuth.AuthCallback() {
                @Override
                public void onSuccess() {
                    Constants.logged = true;
                    Intent intent = new Intent(LoginActivity.this, ManageTournamentActivity.class);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(String message) {
                    btnAccessAdmin.setEnabled(true);
                    txtWrongPassword.setText(R.string.wrong_password);
                }
            });
        });
    }
}
