package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class TournamentSpinnerTeamAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;      // titoli, es. "TEAM 3" o la fase
    private final ArrayList<String> subtitles;   // giocatori (per le squadre) oppure null
    private Spinner spinner;                      // per evidenziare il selezionato nella tendina

    public TournamentSpinnerTeamAdapter(Context context, ArrayList<String> values) {
        this(context, values, null);
    }

    public TournamentSpinnerTeamAdapter(Context context, ArrayList<String> values, ArrayList<String> subtitles) {
        super(context, R.layout.layout_spinner_dark, values);
        this.context = context;
        this.values = values;
        this.subtitles = subtitles;
    }

    /** Collega lo spinner così la tendina può evidenziare l'elemento scelto. */
    public void setSpinner(Spinner spinner) {
        this.spinner = spinner;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Vista chiusa: solo il titolo (es. "TEAM 3"), testo chiaro sul campo scuro
        View view = LayoutInflater.from(context).inflate(R.layout.layout_spinner_dark, parent, false);
        TextView txtName = view.findViewById(R.id.txtName);
        txtName.setText(values.get(position));
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        // Tendina: titolo + giocatori, con l'elemento scelto evidenziato
        View view = LayoutInflater.from(context).inflate(R.layout.layout_spinner_dropdown_item, parent, false);
        TextView txtTitle = view.findViewById(R.id.txtTitle);
        TextView txtSub = view.findViewById(R.id.txtSub);

        txtTitle.setText(values.get(position));

        String sub = (subtitles != null) ? subtitles.get(position) : "";
        if (sub != null && !sub.isEmpty()) {
            txtSub.setText(sub);
            txtSub.setVisibility(View.VISIBLE);
        } else {
            txtSub.setVisibility(View.GONE);
        }

        boolean selected = spinner != null && spinner.getSelectedItemPosition() == position;
        view.setBackgroundColor(selected
                ? ContextCompat.getColor(context, R.color.spinner_selected_bg)
                : Color.TRANSPARENT);

        return view;
    }
}
