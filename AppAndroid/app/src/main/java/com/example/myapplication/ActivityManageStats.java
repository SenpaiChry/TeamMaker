package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Utility.StatsUtility;

public class ActivityManageStats extends AppCompatActivity {

    private StatAdapter adapter;
    private final Runnable statsListener = this::refreshAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_stats);

        RecyclerView listView = findViewById(R.id.listViewStats);
        listView.setLayoutManager(new LinearLayoutManager(this));

        // Distanza fra le righe: divider trasparente da 8dp come le altre liste
        int spacingPx = Math.round(getResources().getDisplayMetrics().density * 8);
        listView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull android.view.View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.top = spacingPx / 2;
                outRect.bottom = spacingPx / 2;
            }
        });

        // ItemTouchHelper: consente il drag verticale delle righe. Il drop salva l'ordine.
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
                adapter.onItemMoved(from.getAdapterPosition(), to.getAdapterPosition());
                return true;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }

            @Override
            public void clearView(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder vh) {
                super.clearView(r, vh);
                adapter.onItemDropped();
            }
        };
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(listView);

        adapter = new StatAdapter(this, touchHelper::startDrag);
        listView.setAdapter(adapter);

        StatsUtility.addChangeListener(statsListener);

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> finish());

        Button btnAdd = findViewById(R.id.btnAddNewStat);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, ActivityPopUpEditStat.class);
            startActivity(intent);
        });
    }

    private void refreshAdapter() {
        runOnUiThread(() -> {
            if (adapter != null) adapter.reload();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StatsUtility.removeChangeListener(statsListener);
    }
}
