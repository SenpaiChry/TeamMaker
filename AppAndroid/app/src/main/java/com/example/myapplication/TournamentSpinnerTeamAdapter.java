package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class TournamentSpinnerTeamAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;

    public TournamentSpinnerTeamAdapter(Context context, ArrayList<String> values) {
        super(context, R.layout.layout_spinner_item, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_spinner, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.txtName);
        textView.setText(values.get(position));

        return convertView;
    }
}
