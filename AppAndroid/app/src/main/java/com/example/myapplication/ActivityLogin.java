package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.Constants;

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
            // password null = non ancora arrivata dal DB (o nodo vuoto): rifiuta,
            // così non c'è una finestra iniziale di «password vuota accettata».
            if (Constants.password != null
                    && txtPassword.getText().toString().equals(Constants.password)) {
                Intent intent = new Intent(activityLogin.getApplicationContext(), TournamentActivityManage.class);
                activityLogin.startActivity(intent);
                finish();
                Constants.logged = true;
            } else {
                txtWrongPassword.setText(R.string.wrong_password);
            }
        });
    }
}
