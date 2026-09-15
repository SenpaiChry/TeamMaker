package com.teammaker.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class PlayerAdminAdapter extends RecyclerView.Adapter<PlayerAdminAdapter.ViewHolder> {
    final private Context context;
    ArrayList<Player> playersToSee;

    public PlayerAdminAdapter(Context context, ArrayList<Player> playersToSee) {
        this.context = context;
        this.playersToSee = playersToSee;
    }

    @Override
    public int getItemCount() { return playersToSee.size(); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_player_admin, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Player player = playersToSee.get(position);
        boolean active = player.isActive;

        h.llPlayer.setBackground(ContextCompat.getDrawable(context,
                active ? R.drawable.bg_player_card : R.drawable.bg_player_card_archived));
        h.btnArchive.setVisibility(active ? View.VISIBLE : View.GONE);
        h.btnUnarchive.setVisibility(active ? View.GONE : View.VISIBLE);

        h.txtRank.setText(String.valueOf(position + 1));
        h.txtName.setText(player.name);
        h.txtSurname.setText(player.getSurnameOrNickname());
        h.txtValue.setText(player.getVote().toString());

        // Colore per genere: attivo = colore del genere, archiviato = grigio
        int textColor;
        if (!active) {
            textColor = ContextCompat.getColor(context, R.color.list_text_muted);
            h.txtRank.setTextColor(ContextCompat.getColor(context, R.color.list_text_muted));
        } else {
            textColor = ContextCompat.getColor(context,
                    "F".equals(player.gender) ? R.color.women_color_name_dark : R.color.men_color_name_dark);
            h.txtRank.setTextColor(ContextCompat.getColor(context, R.color.list_text_secondary));
        }
        h.txtName.setTextColor(textColor);
        h.txtSurname.setTextColor(textColor);
        h.txtValue.setTextColor(textColor);

        h.btnArchive.setOnClickListener(v -> openActivityPopUp(position, PopUpType.ARCHIVE_PLAYER));
        h.btnUnarchive.setOnClickListener(v -> openActivityPopUp(position, PopUpType.UNARCHIVE_PLAYER));
        h.btnEdit.setOnClickListener(v -> openActivityEditPlayer(position));
        h.btnDelete.setOnClickListener(v -> openActivityPopUp(position, PopUpType.DELETE_PLAYER));
        h.btnInfo.setOnClickListener(v -> openActivityInfo(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View llPlayer;
        final ImageView btnArchive, btnUnarchive, btnEdit, btnDelete, btnInfo;
        final TextView txtRank, txtName, txtSurname, txtValue;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            llPlayer = itemView.findViewById(R.id.llPlayer);
            btnArchive = itemView.findViewById(R.id.btnArchive);
            btnUnarchive = itemView.findViewById(R.id.btnUnarchive);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnInfo = itemView.findViewById(R.id.btnInfo);
            txtRank = itemView.findViewById(R.id.txtRank);
            txtName = itemView.findViewById(R.id.txtName);
            txtSurname = itemView.findViewById(R.id.txtSurname);
            txtValue = itemView.findViewById(R.id.txtValue);
        }
    }

    void openActivityPopUp(int position, PopUpType type) {
        Intent intent = new Intent(ActivityAdmin.activityAdmin.getApplicationContext(), ActivityPopUp.class);
        intent.putExtra("player_key", playersToSee.get(position).key);
        intent.putExtra("pop_up_type", type);
        ActivityAdmin.activityAdmin.startActivity(intent);
    }

    void openActivityEditPlayer(int position) {
        Intent intent = new Intent(ActivityAdmin.activityAdmin.getApplicationContext(), ActivityEditPlayer.class);
        intent.putExtra("player_key", playersToSee.get(position).key);
        ActivityAdmin.activityAdmin.startActivity(intent);
    }

    void openActivityInfo(int position) {
        Intent intent = new Intent(ActivityAdmin.activityAdmin.getApplicationContext(), ActivityInfoPlayer.class);
        intent.putExtra("player_key", playersToSee.get(position).key);
        ActivityAdmin.activityAdmin.startActivity(intent);
    }
}
