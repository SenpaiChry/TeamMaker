package com.teammaker.app.Utility;

import android.content.Context;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Sostituisce android:dividerHeight dei vecchi ListView. Aggiunge un margine
 * verticale sotto ogni elemento del RecyclerView.
 */
public class VerticalSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final int spacingPx;

    public VerticalSpacingItemDecoration(Context context, int spacingDp) {
        this.spacingPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                spacingDp,
                context.getResources().getDisplayMetrics());
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) return;
        // Ultimo item: niente margine sotto (evita spazio extra a fine lista).
        if (position < state.getItemCount() - 1) {
            outRect.bottom = spacingPx;
        }
    }
}
