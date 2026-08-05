package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.myapplication.Model.Constants;

import java.util.HashMap;

public class StatsPlayerAdapter extends BaseAdapter {

    HashMap<String, Object> stats;
    Context context;

    public StatsPlayerAdapter(Context context, HashMap<String, Object> stats) {
        this.context = context;
        this.stats = stats;
    }

    @Override
    public int getCount() {
        return Constants.statsDescription.length;
    }

    @Override
    public String getItem(int position) {
        return Constants.statsDescription[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            convertView = layoutInflater.inflate(R.layout.layout_player_info_spinner, parent, false);
        }

        TextView txtStatsDescription = convertView.findViewById(R.id.txtStatsDescription);
        txtStatsDescription.setText(getItem(position));

        Spinner spinnerValue = convertView.findViewById(R.id.spinnerValue);
        LinearLayout llImagesContainer = convertView.findViewById(R.id.llImages);

        if (getItem(position).equals("Altezza")) {
            spinnerValue.setVisibility(View.VISIBLE);
            llImagesContainer.setVisibility(View.GONE);

            SpinnerAdapterStats adapterValueHeight = new SpinnerAdapterStats(context, Constants.valueHeight);

            spinnerValue.setAdapter(adapterValueHeight);
            spinnerValue.setSelection(Integer.parseInt(String.valueOf(stats.get(Constants.statsDescriptionEng[position]) == null ? 0 : stats.get(Constants.statsDescriptionEng[position])).replace(".0", "")));

            spinnerValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int posValue, long id) {
                    stats.put(Constants.statsDescriptionEng[position], posValue * 1.0);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });
        } else {
            spinnerValue.setVisibility(View.GONE);
            llImagesContainer.setVisibility(View.VISIBLE);
            llImagesContainer.removeAllViews();

            int maxLevel = (int) (Constants.statsMax[position] / Constants.statsStep[position]);
            double currentValue = Double.parseDouble(stats.get(Constants.statsDescriptionEng[position]).toString());
            int currentLevel = (int) (currentValue / Constants.statsStep[position]);

            for (int i = 0; i <= maxLevel; i ++) {
                ImageView image = new ImageView(context);
                image.setImageResource(i <= currentLevel ? R.drawable.star_full : R.drawable.star_empty);

                image.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));

                final int index = i;
                image.setOnClickListener(v -> {
                    stats.put(Constants.statsDescriptionEng[position], index * Constants.statsStep[position]);

                    notifyDataSetChanged();
                });

                llImagesContainer.addView(image);
            }
        }

        return convertView;
    }

    public HashMap<String, Object> getStats() {
        return stats;
    }
}
