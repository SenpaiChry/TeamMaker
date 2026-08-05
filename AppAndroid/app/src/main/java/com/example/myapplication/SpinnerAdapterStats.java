package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SpinnerAdapterStats extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;

    public SpinnerAdapterStats(Context context, String[] values) {
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
        textView.setText(values[position]);

        return convertView;
    }
}