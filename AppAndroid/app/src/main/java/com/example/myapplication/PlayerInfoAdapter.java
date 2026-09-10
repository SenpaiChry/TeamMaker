package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.myapplication.Utility.StatsUtility;

import java.util.HashMap;
import java.util.List;

public class PlayerInfoAdapter extends BaseAdapter {
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

    @SuppressLint("DefaultLocale")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_info, parent, false);
        }

        StatDefinition def = getItem(position);
        TextView txtName = convertView.findViewById(R.id.txtName);
        txtName.setText(def.label);

        ViewGroup llImagesContainer = convertView.findViewById(R.id.llImages);
        TextView txtHeight = convertView.findViewById(R.id.txtHeight);
        ImageView imgBonus = convertView.findViewById(R.id.imgBonus);

        // COL 3 (bonus): visibile solo se stat ammette bonus E il giocatore ce l'ha
        if (StatDefinition.TYPE_STARS.equals(def.type) && def.allowBonus
                && Boolean.TRUE.equals(bonus.get(def.key))) {
            imgBonus.setVisibility(View.VISIBLE);
            imgBonus.setImageTintList(ContextCompat.getColorStateList(context, R.color.main_blue));
        } else {
            imgBonus.setVisibility(View.INVISIBLE);
        }

        // Pulisci le stelle vecchie e reintroduci txtHeight (era dentro llImages)
        llImagesContainer.removeAllViews();
        llImagesContainer.addView(txtHeight);

        if (StatDefinition.TYPE_RANGE.equals(def.type) && def.values != null && !def.values.isEmpty()) {
            txtHeight.setVisibility(View.VISIBLE);
            // Valore salvato = indice * step; per mostrare la fascia risalgo all'indice.
            double step = def.step > 0 ? def.step : 1;
            int idx = (int) Math.round(valueFor(def) / step);
            idx = Math.max(0, Math.min(idx, def.values.size() - 1));
            txtHeight.setText(def.values.get(idx));
        } else {
            txtHeight.setVisibility(View.GONE);

            float value = valueFor(def);
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
                llImagesContainer.addView(image);
            }
        }

        return convertView;
    }
}
