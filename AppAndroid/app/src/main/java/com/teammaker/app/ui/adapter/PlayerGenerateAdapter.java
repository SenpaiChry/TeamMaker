package com.teammaker.app.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.data.repository.PlayerRepository;

import java.util.ArrayList;
import com.teammaker.app.data.model.Player;
import com.teammaker.app.ui.activity.GenerateActivity;
import com.teammaker.app.R;

public class PlayerGenerateAdapter extends RecyclerView.Adapter<PlayerGenerateAdapter.ViewHolder> {
    final private Context context;
    private final ArrayList<Player> playersToSee;

    public PlayerGenerateAdapter(Context context, ArrayList<Player> playersToSee) {
        this.context = context;
        this.playersToSee = playersToSee;
    }

    @Override
    public int getItemCount() { return playersToSee.size(); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_player_generate, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Player player = playersToSee.get(position);
        boolean isSelected = GenerateActivity.getSelected().contains(player);

        h.txtName.setText(player.name);
        h.txtSurname.setText(player.surname);

        // Colori per genere (senza icona)
        boolean isF = "F".equals(player.gender);
        int nameColor = ContextCompat.getColor(context,
                isF ? R.color.women_color_name_dark : R.color.men_color_name_dark);
        h.txtName.setTextColor(nameColor);
        h.txtSurname.setTextColor(nameColor);

        h.llPlayer.setBackground(ContextCompat.getDrawable(context,
                isSelected ? R.drawable.bg_player_card_selected : R.drawable.bg_player_card));
        h.btnAdd.setVisibility(isSelected ? View.GONE : View.VISIBLE);
        h.btnRemove.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        h.llPlayer.setOnClickListener(v -> {
            boolean selected = GenerateActivity.getSelected().contains(player);

            if (selected) {
                GenerateActivity.getSelected().remove(player);
                h.llPlayer.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_player_card));
                h.btnAdd.setVisibility(View.VISIBLE);
                h.btnRemove.setVisibility(View.GONE);
            } else {
                GenerateActivity.getSelected().add(player);
                h.llPlayer.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_player_card_selected));
                h.btnAdd.setVisibility(View.GONE);
                h.btnRemove.setVisibility(View.VISIBLE);
            }

            GenerateActivity a = GenerateActivity.get();
            if (a != null) {
                a.updateSelectedCount();
                if (GenerateActivity.getSelected().isEmpty()) {
                    a.switchSelectDeselect("DESELECT");
                } else if (GenerateActivity.getSelected().size() == PlayerRepository.getPlayersActive(true).size()) {
                    a.switchSelectDeselect("SELECT");
                }
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout llPlayer;
        final ImageView btnAdd, btnRemove;
        final TextView txtName, txtSurname;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            llPlayer = itemView.findViewById(R.id.llPlayer);
            btnAdd = itemView.findViewById(R.id.btnAdd);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            txtName = itemView.findViewById(R.id.txtName);
            txtSurname = itemView.findViewById(R.id.txtSurname);
        }
    }
}
