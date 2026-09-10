package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Utility.StatsUtility;

import java.util.ArrayList;

public class ActivityPopUpEditStat extends AppCompatActivity {

    private TextView btnTypeStars, btnTypeRange;
    private LinearLayout llMax, llAllowBonus, llValues;
    private CheckBox chkAllowBonus;
    private RangeValuesAdapter valuesAdapter;
    private String currentType = StatDefinition.TYPE_STARS;

    private StatDefinition editing; // null = nuova stat

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_edit_stat);

        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        TextView txtTitle = findViewById(R.id.txtTitle);
        EditText txtLabel = findViewById(R.id.txtLabel);
        EditText txtMax = findViewById(R.id.txtMax);
        EditText txtStep = findViewById(R.id.txtStep);

        btnTypeStars = findViewById(R.id.btnTypeStars);
        btnTypeRange = findViewById(R.id.btnTypeRange);
        llMax = findViewById(R.id.llMax);
        llAllowBonus = findViewById(R.id.llAllowBonus);
        chkAllowBonus = findViewById(R.id.chkAllowBonus);
        llValues = findViewById(R.id.llValues);

        RecyclerView valuesList = findViewById(R.id.llValuesList);
        valuesList.setLayoutManager(new LinearLayoutManager(this));

        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder vh) {
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }

            @Override public boolean isLongPressDragEnabled() { return false; }
            @Override public boolean isItemViewSwipeEnabled() { return false; }

            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder from,
                                  @NonNull RecyclerView.ViewHolder to) {
                valuesAdapter.onItemMoved(from.getAdapterPosition(), to.getAdapterPosition());
                return true;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }
        };
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(valuesList);

        valuesAdapter = new RangeValuesAdapter(touchHelper::startDrag);
        valuesList.setAdapter(valuesAdapter);

        btnTypeStars.setOnClickListener(v -> setType(StatDefinition.TYPE_STARS));
        btnTypeRange.setOnClickListener(v -> setType(StatDefinition.TYPE_RANGE));

        TextView btnAddValue = findViewById(R.id.btnAddValue);
        btnAddValue.setOnClickListener(v -> valuesAdapter.addValue(""));

        // Precompila se stiamo modificando una stat esistente
        String editKey = getIntent().getStringExtra("stat_key");
        if (editKey != null) {
            editing = StatsUtility.getByKey(editKey);
        }
        if (editing != null) {
            txtTitle.setText(R.string.edit_stat);
            txtLabel.setText(editing.label);
            txtMax.setText(String.valueOf((int) editing.max));
            txtStep.setText(String.valueOf(editing.step));
            chkAllowBonus.setChecked(editing.allowBonus);
            if (editing.values != null) {
                for (String v : editing.values) valuesAdapter.addValue(v);
            }
            setType(editing.type);
        } else {
            txtTitle.setText(R.string.new_stat);
            txtMax.setText("4");
            txtStep.setText("1");
            chkAllowBonus.setChecked(false);
            valuesAdapter.addValue(""); // parte con una riga vuota per le fasce
            setType(StatDefinition.TYPE_STARS);
        }

        Button btnConfirm = findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(v -> {
            String label = txtLabel.getText().toString().trim();
            if (label.isEmpty()) {
                Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
                return;
            }

            double step = parseDoubleOrZero(txtStep.getText().toString());
            if (step <= 0) {
                Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
                return;
            }

            StatDefinition def = editing != null ? editing : new StatDefinition();
            def.label = label;
            def.type = currentType;
            def.step = step;
            // Bonus solo per STARS; su RANGE non ha senso quindi si azzera.
            def.allowBonus = StatDefinition.TYPE_STARS.equals(currentType) && chkAllowBonus.isChecked();

            if (StatDefinition.TYPE_RANGE.equals(currentType)) {
                ArrayList<String> values = new ArrayList<>();
                for (String s : valuesAdapter.currentValues()) {
                    String t = s.trim();
                    if (!t.isEmpty()) values.add(t);
                }
                if (values.isEmpty()) {
                    Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
                    return;
                }
                def.values = values;
                def.max = (values.size() - 1) * step;
            } else {
                double max = parseDoubleOrZero(txtMax.getText().toString());
                if (max <= 0) {
                    Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
                    return;
                }
                def.max = max;
                def.values = null;
            }

            if (editing != null) {
                StatsUtility.updateStat(def);
            } else {
                def.order = StatsUtility.getDefinitions().size();
                StatsUtility.addStat(def);
            }
            finish();
        });

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());
    }

    private void setType(String type) {
        currentType = type;
        boolean isStars = StatDefinition.TYPE_STARS.equals(type);
        styleSegment(btnTypeStars, isStars);
        styleSegment(btnTypeRange, !isStars);
        llMax.setVisibility(isStars ? View.VISIBLE : View.GONE);
        llAllowBonus.setVisibility(isStars ? View.VISIBLE : View.GONE);
        llValues.setVisibility(isStars ? View.GONE : View.VISIBLE);
    }

    private void styleSegment(TextView segment, boolean selected) {
        segment.setBackground(selected ? ContextCompat.getDrawable(this, R.drawable.bg_segment_selected) : null);
        segment.setTextColor(ContextCompat.getColor(this, selected ? R.color.white : R.color.list_text_muted));
    }

    private double parseDoubleOrZero(String s) {
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (Exception e) {
            return 0;
        }
    }
}
