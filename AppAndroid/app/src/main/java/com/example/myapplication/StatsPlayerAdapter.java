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

import androidx.core.content.ContextCompat;

import com.example.myapplication.Utility.StatsUtility;

import java.util.HashMap;
import java.util.List;

public class StatsPlayerAdapter extends BaseAdapter {

    private final PlayerStats stats;
    private final HashMap<String, Boolean> bonus;
    private final Context context;

    public StatsPlayerAdapter(Context context, PlayerStats stats) {
        this(context, stats, new HashMap<>());
    }

    public StatsPlayerAdapter(Context context, PlayerStats stats, HashMap<String, Boolean> bonus) {
        this.context = context;
        this.stats = stats;
        this.bonus = bonus;
    }

    private List<StatDefinition> defs() {
        return StatsUtility.getDefinitions();
    }

    @Override
    public int getCount() {
        return defs().size();
    }

    @Override
    public StatDefinition getItem(int position) {
        return defs().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private float valueFor(StatDefinition def) {
        return stats.get(def.key);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_player_info_spinner, parent, false);
        }

        StatDefinition def = getItem(position);
        TextView txtStatsDescription = convertView.findViewById(R.id.txtStatsDescription);
        txtStatsDescription.setText(def.label);

        Spinner spinnerValue = convertView.findViewById(R.id.spinnerValue);
        ViewGroup llImagesContainer = convertView.findViewById(R.id.llImages);
        ImageView imgBonus = convertView.findViewById(R.id.imgBonus);

        // COL 3 (bonus): sempre presente per riservare lo spazio; visibile solo per STARS con allowBonus.
        if (StatDefinition.TYPE_STARS.equals(def.type) && def.allowBonus) {
            boolean active = Boolean.TRUE.equals(bonus.get(def.key));
            imgBonus.setVisibility(View.VISIBLE);
            imgBonus.setImageTintList(ContextCompat.getColorStateList(context,
                    active ? R.color.main_blue : R.color.list_text_muted));
            final String statKey = def.key;
            imgBonus.setOnClickListener(v -> {
                boolean newVal = !Boolean.TRUE.equals(bonus.get(statKey));
                bonus.put(statKey, newVal);
                notifyDataSetChanged();
            });
        } else {
            imgBonus.setVisibility(View.INVISIBLE);
            imgBonus.setOnClickListener(null);
        }

        if (StatDefinition.TYPE_RANGE.equals(def.type) && def.values != null && !def.values.isEmpty()) {
            spinnerValue.setVisibility(View.VISIBLE);
            llImagesContainer.setVisibility(View.GONE);

            java.util.ArrayList<String> valuesCopy = new java.util.ArrayList<>(def.values);
            TournamentSpinnerTeamAdapter adapterValue = new TournamentSpinnerTeamAdapter(context, valuesCopy);
            spinnerValue.setAdapter(adapterValue);
            adapterValue.setSpinner(spinnerValue);
            // Il valore salvato e' indice * step; per mostrare l'indice, divide per step.
            int idx = (int) Math.round(valueFor(def) / (def.step > 0 ? def.step : 1));
            idx = Math.max(0, Math.min(idx, valuesCopy.size() - 1));
            spinnerValue.setSelection(idx);

            final String statKey = def.key;
            final float statStep = (float) (def.step > 0 ? def.step : 1);
            spinnerValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int posValue, long id) {
                    stats.set(statKey, posValue * statStep);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });
        } else {
            spinnerValue.setVisibility(View.GONE);
            llImagesContainer.setVisibility(View.VISIBLE);
            llImagesContainer.removeAllViews();

            int maxLevel = (int) (def.max / def.step);
            int currentLevel = (int) (valueFor(def) / def.step);

            int starSize = Math.round(context.getResources().getDisplayMetrics().density * 28);
            int marginPx = Math.round(context.getResources().getDisplayMetrics().density * 2);
            for (int i = 0; i <= maxLevel; i++) {
                ImageView image = new ImageView(context);
                image.setImageResource(i <= currentLevel ? R.drawable.star_full : R.drawable.star_empty);

                ViewGroup.MarginLayoutParams starParams = new ViewGroup.MarginLayoutParams(starSize, starSize);
                starParams.setMarginStart(marginPx);
                image.setLayoutParams(starParams);

                final int index = i;
                final String statKey = def.key;
                final float statStep = (float) def.step;
                image.setOnClickListener(v -> {
                    stats.set(statKey, index * statStep);
                    notifyDataSetChanged();
                });

                llImagesContainer.addView(image);
            }
        }

        return convertView;
    }

    public PlayerStats getStats() {
        return stats;
    }

    public HashMap<String, Boolean> getBonus() {
        return bonus;
    }
}
