package com.teammaker.app.ui.popup;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.teammaker.app.data.model.PopUpType;
import com.teammaker.app.data.model.StatDefinition;
import com.teammaker.app.data.repository.StatsRepository;
import com.teammaker.app.ui.adapter.RangeValuesAdapter;
import com.teammaker.app.R;

import java.util.ArrayList;

public class EditStatBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_STAT_KEY = "stat_key";

    private View btnTypeStars, btnTypeRange;
    private TextView txtStepLabel;
    private LinearLayout llStarsSection, llValues, llMaxStepper;
    private SwitchCompat switchBonus;
    private RangeValuesAdapter valuesAdapter;
    private String currentType = StatDefinition.TYPE_STARS;

    private TextView txtMax, txtStep;
    private LinearLayout previewStars;
    private TextView txtPreviewRange;

    private int maxValue = 4;
    private float stepValue = 1f;

    private StatDefinition editing;

    public static EditStatBottomSheet newInstance(@Nullable String statKey) {
        EditStatBottomSheet sheet = new EditStatBottomSheet();
        if (statKey != null) {
            Bundle args = new Bundle();
            args.putString(ARG_STAT_KEY, statKey);
            sheet.setArguments(args);
        }
        return sheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            View sheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.setBackgroundResource(android.R.color.transparent);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_pop_up_edit_stat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView txtTitle = view.findViewById(R.id.txtTitle);
        EditText txtLabel = view.findViewById(R.id.txtLabel);
        txtMax = view.findViewById(R.id.txtMax);
        txtStep = view.findViewById(R.id.txtStep);

        btnTypeStars = view.findViewById(R.id.btnTypeStars);
        btnTypeRange = view.findViewById(R.id.btnTypeRange);
        llStarsSection = view.findViewById(R.id.llStarsSection);
        llMaxStepper = view.findViewById(R.id.llMaxStepper);
        txtStepLabel = view.findViewById(R.id.txtStepLabel);
        llValues = view.findViewById(R.id.llValues);

        switchBonus = view.findViewById(R.id.switchBonus);
        previewStars = view.findViewById(R.id.previewStars);
        txtPreviewRange = view.findViewById(R.id.txtPreviewRange);

        styleSwitchColors();

        RecyclerView valuesList = view.findViewById(R.id.llValuesList);
        valuesList.setLayoutManager(new LinearLayoutManager(requireContext()));

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

        TextView btnAddValue = view.findViewById(R.id.btnAddValue);
        btnAddValue.setOnClickListener(v -> valuesAdapter.addValue(""));

        // Stepper: max
        view.findViewById(R.id.btnMaxMinus).setOnClickListener(v -> {
            if (maxValue > 1) { maxValue--; txtMax.setText(String.valueOf(maxValue)); updatePreview(); }
        });
        view.findViewById(R.id.btnMaxPlus).setOnClickListener(v -> {
            if (maxValue < 10) { maxValue++; txtMax.setText(String.valueOf(maxValue)); updatePreview(); }
        });

        // Stepper: step (incrementi di 0.5)
        view.findViewById(R.id.btnStepMinus).setOnClickListener(v -> {
            if (stepValue > 0.5f) { stepValue -= 0.5f; txtStep.setText(formatStep(stepValue)); updatePreview(); }
        });
        view.findViewById(R.id.btnStepPlus).setOnClickListener(v -> {
            if (stepValue < 5f) { stepValue += 0.5f; txtStep.setText(formatStep(stepValue)); updatePreview(); }
        });

        // Switch bonus
        switchBonus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            styleSwitchColors();
            updatePreview();
        });

        // Precompila se stiamo modificando una stat esistente
        String editKey = getArguments() != null ? getArguments().getString(ARG_STAT_KEY) : null;
        if (editKey != null) {
            editing = StatsRepository.getByKey(editKey);
        }

        ImageView btnDelete = view.findViewById(R.id.btnDelete);
        if (editing != null) {
            txtTitle.setText(R.string.stat_edit_title);
            txtLabel.setText(editing.label);
            maxValue = (int) editing.max;
            stepValue = (float) editing.step;
            txtMax.setText(String.valueOf(maxValue));
            txtStep.setText(formatStep(stepValue));
            switchBonus.setChecked(editing.allowBonus);
            if (editing.values != null) {
                for (String v : editing.values) valuesAdapter.addValue(v);
            }
            setType(editing.type);

            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ConfirmPopupActivity.class);
                intent.putExtra("stat_key", editing.key);
                intent.putExtra("pop_up_type", PopUpType.DELETE_STAT);
                startActivity(intent);
                dismiss();
            });
        } else {
            txtTitle.setText(R.string.stat_new_title);
            txtMax.setText(String.valueOf(maxValue));
            txtStep.setText(formatStep(stepValue));
            switchBonus.setChecked(false);
            valuesAdapter.addValue("");
            setType(StatDefinition.TYPE_STARS);

            btnDelete.setVisibility(View.GONE);
        }

        updatePreview();

        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(v -> {
            String label = txtLabel.getText().toString().trim();
            if (label.isEmpty()) {
                Toast.makeText(requireContext(), R.string.missing_data, Toast.LENGTH_SHORT).show();
                return;
            }

            StatDefinition def = editing != null ? editing : new StatDefinition();
            def.label = label;
            def.type = currentType;
            def.step = stepValue;
            def.allowBonus = StatDefinition.TYPE_STARS.equals(currentType) && switchBonus.isChecked();

            if (StatDefinition.TYPE_RANGE.equals(currentType)) {
                ArrayList<String> values = new ArrayList<>();
                for (String s : valuesAdapter.currentValues()) {
                    String t = s.trim();
                    if (!t.isEmpty()) values.add(t);
                }
                if (values.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.missing_data, Toast.LENGTH_SHORT).show();
                    return;
                }
                def.values = values;
                def.max = values.size() - 1;
            } else {
                if (maxValue <= 0) {
                    Toast.makeText(requireContext(), R.string.missing_data, Toast.LENGTH_SHORT).show();
                    return;
                }
                def.max = maxValue;
                def.values = null;
            }

            if (editing != null) {
                StatsRepository.updateStat(def);
            } else {
                def.order = StatsRepository.getDefinitions().size();
                StatsRepository.addStat(def);
            }
            dismiss();
        });
    }

    private void setType(String type) {
        currentType = type;
        boolean isStars = StatDefinition.TYPE_STARS.equals(type);
        styleSegment(btnTypeStars, isStars);
        styleSegment(btnTypeRange, !isStars);
        llMaxStepper.setVisibility(isStars ? View.VISIBLE : View.GONE);
        txtStepLabel.setText(isStars ? R.string.stat_each_star_value : R.string.stat_each_range_value);
        llStarsSection.setVisibility(isStars ? View.VISIBLE : View.GONE);
        llValues.setVisibility(isStars ? View.GONE : View.VISIBLE);
    }

    private void styleSegment(View segment, boolean selected) {
        segment.setBackground(selected
                ? ContextCompat.getDrawable(requireContext(), R.drawable.bg_segment_selected) : null);
        int color = ContextCompat.getColor(requireContext(),
                selected ? R.color.white : R.color.list_text_muted);
        if (segment instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) segment;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(color);
                } else if (child instanceof ImageView) {
                    ((ImageView) child).setImageTintList(
                            android.content.res.ColorStateList.valueOf(color));
                }
            }
        }
    }

    private void styleSwitchColors() {
        int colorOn = ContextCompat.getColor(requireContext(), R.color.button_confirm);
        int colorOff = ContextCompat.getColor(requireContext(), R.color.list_text_muted);
        int colorWhite = ContextCompat.getColor(requireContext(), R.color.white);

        switchBonus.setThumbTintList(ColorStateList.valueOf(colorWhite));
        switchBonus.setTrackTintList(new ColorStateList(
                new int[][]{ { android.R.attr.state_checked }, {} },
                new int[]{ colorOn, colorOff }
        ));
    }

    private void updatePreview() {
        previewStars.removeAllViews();
        int starCount = maxValue;
        int starColor = ContextCompat.getColor(requireContext(), R.color.stars);
        int mutedColor = ContextCompat.getColor(requireContext(), R.color.list_text_muted);

        for (int i = 0; i < starCount; i++) {
            TextView star = new TextView(requireContext());
            star.setText("★");
            star.setTextColor(starColor);
            star.setTextSize(16);
            previewStars.addView(star);
        }

        if (switchBonus.isChecked()) {
            TextView sep = new TextView(requireContext());
            sep.setText("|");
            sep.setTextColor(mutedColor);
            sep.setTextSize(16);
            int pad = dpToPx(3);
            sep.setPadding(pad, 0, pad, 0);
            previewStars.addView(sep);

            TextView bonus = new TextView(requireContext());
            bonus.setText("★");
            bonus.setTextColor(ContextCompat.getColor(requireContext(), R.color.main_blue));
            bonus.setTextSize(16);
            previewStars.addView(bonus);
        }

        float totalPoints = (maxValue + (switchBonus.isChecked() ? 1 : 0)) * stepValue;
        txtPreviewRange.setText("0–" + formatStep(totalPoints) + " punti");
    }

    private String formatStep(float value) {
        return value == (int) value ? String.valueOf((int) value) : String.valueOf(value);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
