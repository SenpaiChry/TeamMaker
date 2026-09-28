package com.teammaker.app.ui.popup;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Legacy: tutto e' stato spostato in ManageTournamentBottomSheet.
 * La classe resta nel Manifest per evitare crash su riferimenti residui.
 */
public class ManageTournamentPopupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }
}
