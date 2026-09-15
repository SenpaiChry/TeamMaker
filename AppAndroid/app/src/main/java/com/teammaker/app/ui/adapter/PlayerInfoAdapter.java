package com.teammaker.app.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

public class PlayerInfoAdapter extends RecyclerView.Adapter<PlayerInfoAdapter.ViewHolder> {
    private final PlayerStats stats;
    private final HashMap<String, Boolean> bonus;
    private final Context context;

    public PlayerInfoAdapter(Context context, PlayerStats s) {
        this(context, s, new HashMap<>());
    }

    public PlayerInfoAdapter(Context context, PlayerStats s, HashMap<String, Boolean> bonus) {
        this.context = context;
        this.stats = s;
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
                .inflate(R.layout.layout_info, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        StatDefinition def = defs().get(position);
        h.txtName.setText(def.label);

        // COL 3 (bonus): visibile solo se stat ammette bonus E il giocatore ce l'ha
        if (StatDefinition.TYPE_STARS.equals(def.type) && def.allowBonus
                && Boolean.TRUE.equals(bonus.get(def.key))) {
            h.imgBonus.setVisibility(View.VISIBLE);
            h.imgBonus.setImageTintList(ContextCompat.getColorStateList(context, R.color.main_blue));
        } else {
            h.imgBonus.setVisibility(View.INVISIBLE);
        }

        // Pulisci le stelle vecchie e reintroduci txtHeight (era dentro llImages)
        h.llImagesContainer.removeAllViews();
        h.llImagesContainer.addView(h.txtHeight);

        if (StatDefinition.TYPE_RANGE.equals(def.type) && def.values != null && !def.values.isEmpty()) {
            h.txtHeight.setVisibility(View.VISIBLE);
            // Valore salvato = indice * step; per mostrare la fascia risalgo all'indice.
            double step = def.step > 0 ? def.step : 1;
            int idx = (int) Math.round(stats.get(def.key) / step);
            idx = Math.max(0, Math.min(idx, def.values.size() - 1));
            h.txtHeight.setText(def.values.get(idx));
        } else {
            h.txtHeight.setVisibility(View.GONE);

            float value = stats.get(def.key);
            int maxLevel = (int) (def.max / def.step);
            float density = context.getResources().getDisplayMetrics().density;
            int starSize = Math.round(density * 22);
            int marginPx = Math.round(density * 2);

            for (int i = 0; i <= maxLevel; i++) {
                ImageView image = new ImageView(context);
                image.setImageResource(value / def.step >= i ? R.drawable.star_full : R.drawable.star_empty);
                ViewGroup.MarginLayoutParams starParams = new ViewGroup.MarginLayoutParams(starSize, starSize);
                if (i > 0) starParams.setMarginStart(marginPx);
                image.setLayoutParams(starParams);
                h.llImagesContainer.addView(image);
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtName;
        final ViewGroup llImagesContainer;
        final TextView txtHeight;
        final ImageView imgBonus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            llImagesContainer = itemView.findViewById(R.id.llImages);
            txtHeight = itemView.findViewById(R.id.txtHeight);
            imgBonus = itemView.findViewById(R.id.imgBonus);
        }
    }
}
