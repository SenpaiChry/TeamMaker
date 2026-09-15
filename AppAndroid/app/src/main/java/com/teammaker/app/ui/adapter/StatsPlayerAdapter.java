package com.teammaker.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.data.repository.StatsRepository;

import java.util.HashMap;
import java.util.List;
import com.teammaker.app.data.model.PlayerStats;
import com.teammaker.app.data.model.StatDefinition;
import com.teammaker.app.R;

public class StatsPlayerAdapter extends RecyclerView.Adapter<StatsPlayerAdapter.ViewHolder> {

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
        return StatsRepository.getDefinitions();
    }

    @Override
    public int getItemCount() { return defs().size(); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_player_info_spinner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        StatDefinition def = defs().get(position);
        h.txtStatsDescription.setText(def.label);

        // COL 3 (bonus): sempre presente per riservare lo spazio; visibile solo per STARS con allowBonus.
        if (StatDefinition.TYPE_STARS.equals(def.type) && def.allowBonus) {
            boolean active = Boolean.TRUE.equals(bonus.get(def.key));
            h.imgBonus.setVisibility(View.VISIBLE);
            h.imgBonus.setImageTintList(ContextCompat.getColorStateList(context,
                    active ? R.color.main_blue : R.color.list_text_muted));
            final String statKey = def.key;
            h.imgBonus.setOnClickListener(v -> {
                boolean newVal = !Boolean.TRUE.equals(bonus.get(statKey));
                bonus.put(statKey, newVal);
                notifyDataSetChanged();
            });
        } else {
            h.imgBonus.setVisibility(View.INVISIBLE);
            h.imgBonus.setOnClickListener(null);
        }

        if (StatDefinition.TYPE_RANGE.equals(def.type) && def.values != null && !def.values.isEmpty()) {
            h.spinnerValue.setVisibility(View.VISIBLE);
            h.llImagesContainer.setVisibility(View.GONE);

            java.util.ArrayList<String> valuesCopy = new java.util.ArrayList<>(def.values);
            TournamentSpinnerTeamAdapter adapterValue = new TournamentSpinnerTeamAdapter(context, valuesCopy);
            h.spinnerValue.setAdapter(adapterValue);
            adapterValue.setSpinner(h.spinnerValue);
            // Il valore salvato e' indice * step; per mostrare l'indice, divide per step.
            int idx = (int) Math.round(stats.get(def.key) / (def.step > 0 ? def.step : 1));
            idx = Math.max(0, Math.min(idx, valuesCopy.size() - 1));
            h.spinnerValue.setSelection(idx);

            final String statKey = def.key;
            final float statStep = (float) (def.step > 0 ? def.step : 1);
            h.spinnerValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int posValue, long id) {
                    stats.set(statKey, posValue * statStep);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });
        } else {
            h.spinnerValue.setVisibility(View.GONE);
            h.llImagesContainer.setVisibility(View.VISIBLE);
            h.llImagesContainer.removeAllViews();

            int maxLevel = (int) (def.max / def.step);
            int currentLevel = (int) (stats.get(def.key) / def.step);

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

                h.llImagesContainer.addView(image);
            }
        }
    }

    public PlayerStats getStats() { return stats; }
    public HashMap<String, Boolean> getBonus() { return bonus; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtStatsDescription;
        final Spinner spinnerValue;
        final ViewGroup llImagesContainer;
        final ImageView imgBonus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtStatsDescription = itemView.findViewById(R.id.txtStatsDescription);
            spinnerValue = itemView.findViewById(R.id.spinnerValue);
            llImagesContainer = itemView.findViewById(R.id.llImages);
            imgBonus = itemView.findViewById(R.id.imgBonus);
        }
    }
}
