package com.example.myapplication;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lista editabile delle fasce di una stat RANGE, con drag&drop. */
public class RangeValuesAdapter extends RecyclerView.Adapter<RangeValuesAdapter.RangeVH> {

    public interface DragStartListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    private final List<String> items = new ArrayList<>();
    private final DragStartListener dragListener;

    public RangeValuesAdapter(DragStartListener dragListener) {
        this.dragListener = dragListener;
    }

    public void addValue(String value) {
        items.add(value != null ? value : "");
        notifyItemInserted(items.size() - 1);
    }

    public List<String> currentValues() {
        return new ArrayList<>(items);
    }

    public void onItemMoved(int from, int to) {
        if (from < 0 || to < 0 || from >= items.size() || to >= items.size()) return;
        Collections.swap(items, from, to);
        notifyItemMoved(from, to);
    }

    @Override public int getItemCount() { return items.size(); }

    @NonNull
    @Override
    public RangeVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_stat_range_row, parent, false);
        return new RangeVH(row);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RangeVH h, int position) {
        // Rimuovi il watcher precedente prima di scrivere il testo, altrimenti il
        // rebind aggiorna items[position] col vecchio contenuto del riciclato.
        if (h.watcher != null) h.etValue.removeTextChangedListener(h.watcher);
        h.etValue.setText(items.get(position));

        h.watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                int pos = h.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) items.set(pos, s.toString());
            }
        };
        h.etValue.addTextChangedListener(h.watcher);

        h.btnRemove.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            items.remove(pos);
            notifyItemRemoved(pos);
        });

        h.btnDrag.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && dragListener != null) {
                dragListener.onStartDrag(h);
            }
            return false;
        });
    }

    static class RangeVH extends RecyclerView.ViewHolder {
        final EditText etValue;
        final TextView btnRemove, btnDrag;
        TextWatcher watcher;

        RangeVH(@NonNull View itemView) {
            super(itemView);
            etValue = itemView.findViewById(R.id.etValue);
            btnRemove = itemView.findViewById(R.id.btnRemoveValue);
            btnDrag = itemView.findViewById(R.id.btnDrag);
        }
    }
}
