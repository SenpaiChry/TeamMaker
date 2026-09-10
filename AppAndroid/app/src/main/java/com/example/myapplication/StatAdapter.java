package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Utility.StatsUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class StatAdapter extends RecyclerView.Adapter<StatAdapter.StatViewHolder> {

    private final Context context;
    private final List<StatDefinition> items = new ArrayList<>();
    private final DragStartListener dragListener;

    public interface DragStartListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public StatAdapter(Context context, DragStartListener dragListener) {
        this.context = context;
        this.dragListener = dragListener;
        reload();
    }

    public void reload() {
        items.clear();
        items.addAll(StatsUtility.getDefinitions());
        notifyDataSetChanged();
    }

    /** Chiamato dall'ItemTouchHelper mentre trascina. */
    public void onItemMoved(int from, int to) {
        if (from < 0 || to < 0 || from >= items.size() || to >= items.size()) return;
        Collections.swap(items, from, to);
        notifyItemMoved(from, to);
    }

    /** Chiamato quando l'utente rilascia: salva il nuovo ordine su Firebase. */
    public void onItemDropped() {
        StatsUtility.reorder(items);
    }

    @Override public int getItemCount() { return items.size(); }

    @NonNull
    @Override
    public StatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(context).inflate(R.layout.layout_stat_row, parent, false);
        return new StatViewHolder(row);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull StatViewHolder h, int position) {
        StatDefinition def = items.get(position);
        h.txtLabel.setText(def.label);

        if (StatDefinition.TYPE_RANGE.equals(def.type)) {
            int n = def.values != null ? def.values.size() : 0;
            String stepStr = String.format(Locale.getDefault(), "%.1f", def.step);
            h.txtMeta.setText(context.getString(R.string.stat_type_range) + " · " + n + " · step " + stepStr);
        } else {
            int levels = (int) (def.max / def.step) + 1;
            String stepStr = String.format(Locale.getDefault(), "%.1f", def.step);
            h.txtMeta.setText(context.getString(R.string.stat_type_stars)
                    + " · max " + (int) def.max + " · step " + stepStr + " · " + levels + " lvl");
        }

        // Handle di drag: al touch avvia il drag via ItemTouchHelper
        h.btnDrag.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && dragListener != null) {
                dragListener.onStartDrag(h);
            }
            return false;
        });

        h.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, ActivityPopUpEditStat.class);
            intent.putExtra("stat_key", def.key);
            context.startActivity(intent);
        });

        h.btnDelete.setOnClickListener(v -> {
            Intent intent = new Intent(context, ActivityPopUp.class);
            intent.putExtra("stat_key", def.key);
            intent.putExtra("pop_up_type", PopUpType.DELETE_STAT);
            context.startActivity(intent);
        });
    }

    static class StatViewHolder extends RecyclerView.ViewHolder {
        final TextView txtLabel, txtMeta, btnDrag;
        final ImageView btnEdit, btnDelete;

        StatViewHolder(@NonNull View itemView) {
            super(itemView);
            txtLabel = itemView.findViewById(R.id.txtStatLabel);
            txtMeta = itemView.findViewById(R.id.txtStatMeta);
            btnDrag = itemView.findViewById(R.id.btnDrag);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
