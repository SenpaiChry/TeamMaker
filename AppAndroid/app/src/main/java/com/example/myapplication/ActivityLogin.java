package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.Constants;
import com.example.myapplication.Utility.AdminUtility;

public class ActivityLogin extends AppCompatActivity {

    static ActivityLogin activityLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        activityLogin = this;

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
            AdminUtility.signIn(password, new AdminUtility.AuthCallback() {
                @Override
                public void onSuccess() {
                    Constants.logged = true;
                    Intent intent = new Intent(activityLogin.getApplicationContext(), TournamentActivityManage.class);
                    activityLogin.startActivity(intent);
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
