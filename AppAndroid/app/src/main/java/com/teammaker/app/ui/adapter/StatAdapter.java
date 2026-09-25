package com.teammaker.app.ui.adapter;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.data.model.StatDefinition;
import com.teammaker.app.data.repository.StatsRepository;
import com.teammaker.app.ui.popup.EditStatBottomSheet;
import com.teammaker.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatAdapter extends RecyclerView.Adapter<StatAdapter.StatViewHolder> {

    private final AppCompatActivity activity;
    private final List<StatDefinition> items = new ArrayList<>();
    private final DragStartListener dragListener;

    public interface DragStartListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public StatAdapter(AppCompatActivity activity, DragStartListener dragListener) {
        this.activity = activity;
        this.dragListener = dragListener;
        reload();
    }

    public void reload() {
        items.clear();
        items.addAll(StatsRepository.getDefinitions());
        notifyDataSetChanged();
    }

    public void onItemMoved(int from, int to) {
        if (from < 0 || to < 0 || from >= items.size() || to >= items.size()) return;
        Collections.swap(items, from, to);
        notifyItemMoved(from, to);
    }

    public void onItemDropped() {
        StatsRepository.reorder(items);
    }

    @Override public int getItemCount() { return items.size(); }

    @NonNull
    @Override
    public StatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(activity).inflate(R.layout.layout_stat_row, parent, false);
        return new StatViewHolder(row);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull StatViewHolder h, int position) {
        StatDefinition def = items.get(position);
        h.txtLabel.setText(def.label);

        boolean isRange = StatDefinition.TYPE_RANGE.equals(def.type);

        // Badge icon
        if (isRange) {
            h.badgeBg.setBackgroundResource(R.drawable.bg_badge_range);
            h.badgeIcon.setImageResource(R.drawable.ic_stat_bars);
        } else {
            h.badgeBg.setBackgroundResource(R.drawable.bg_badge_stars);
            h.badgeIcon.setImageResource(R.drawable.ic_stat_star);
        }

        // Meta subtitle
        if (isRange) {
            int n = def.values != null ? def.values.size() : 0;
            int stepInt = (int) def.step;
            h.txtMeta.setText(activity.getString(R.string.stat_meta_range, n, stepInt));
        } else {
            int maxInt = (int) def.max;
            int stepInt = (int) def.step;
            if (def.allowBonus) {
                h.txtMeta.setText(activity.getString(R.string.stat_meta_stars_bonus, maxInt, stepInt));
            } else {
                h.txtMeta.setText(activity.getString(R.string.stat_meta_stars, maxInt, stepInt));
            }
        }

        // Preview
        h.previewContainer.removeAllViews();
        if (isRange) {
            buildRangePreview(h.previewContainer, def);
        } else {
            buildStarPreview(h.previewContainer, def);
        }

        // Drag handle
        h.btnDrag.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && dragListener != null) {
                dragListener.onStartDrag(h);
            }
            return false;
        });

        // Click row -> edit bottom sheet
        h.itemView.setOnClickListener(v -> {
            FragmentManager fm = activity.getSupportFragmentManager();
            EditStatBottomSheet.newInstance(def.key).show(fm, "editStat");
        });
    }

    private void buildStarPreview(LinearLayout container, StatDefinition def) {
        int starCount = (int) (def.max / def.step);
        int starColor = ContextCompat.getColor(activity, R.color.stars);
        int separatorColor = ContextCompat.getColor(activity, R.color.list_text_muted);

        for (int i = 0; i < starCount; i++) {
            TextView star = new TextView(activity);
            star.setText("★");
            star.setTextColor(starColor);
            star.setTextSize(12);
            container.addView(star);
        }

        if (def.allowBonus) {
            TextView sep = new TextView(activity);
            sep.setText("|");
            sep.setTextColor(separatorColor);
            sep.setTextSize(12);
            sep.setPadding(dpToPx(2), 0, dpToPx(2), 0);
            container.addView(sep);

            TextView bonus = new TextView(activity);
            bonus.setText("★");
            bonus.setTextColor(ContextCompat.getColor(activity, R.color.main_blue));
            bonus.setTextSize(12);
            container.addView(bonus);
        }
    }

    private void buildRangePreview(LinearLayout container, StatDefinition def) {
        int n = def.values != null ? def.values.size() : 0;
        int color = ContextCompat.getColor(activity, R.color.main_blue);
        int[] heights = { dpToPx(8), dpToPx(12), dpToPx(16) };

        for (int i = 0; i < Math.min(n, 3); i++) {
            View bar = new View(activity);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(8), heights[i]);
            lp.rightMargin = dpToPx(2);
            lp.gravity = Gravity.BOTTOM;
            bar.setLayoutParams(lp);
            bar.setBackgroundColor(color);
            container.addView(bar);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }

    static class StatViewHolder extends RecyclerView.ViewHolder {
        final TextView txtLabel, txtMeta, btnDrag;
        final View badgeBg;
        final ImageView badgeIcon;
        final LinearLayout previewContainer;

        StatViewHolder(@NonNull View itemView) {
            super(itemView);
            txtLabel = itemView.findViewById(R.id.txtStatLabel);
            txtMeta = itemView.findViewById(R.id.txtStatMeta);
            btnDrag = itemView.findViewById(R.id.btnDrag);
            badgeBg = itemView.findViewById(R.id.badgeBg);
            badgeIcon = itemView.findViewById(R.id.badgeIcon);
            previewContainer = itemView.findViewById(R.id.previewContainer);
        }
    }
}
