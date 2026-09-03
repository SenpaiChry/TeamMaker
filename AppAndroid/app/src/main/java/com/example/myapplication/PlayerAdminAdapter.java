package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class PlayerAdminAdapter extends BaseAdapter {
    final private Context context;
    ArrayList<Player> playersToSee;

    public PlayerAdminAdapter(Context context, ArrayList<Player> playersToSee) {
        this.context = context;
        this.playersToSee = playersToSee;
    }

    @Override
    public int getCount() {
        return playersToSee.size();
    }

    @Override
    public Player getItem(int position) {
        return playersToSee.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            convertView = layoutInflater.inflate(R.layout.layout_player_admin, parent, false);
        }

        Player player = getItem(position);
        boolean active = player.isActive;

        View llPlayer = convertView.findViewById(R.id.llPlayer);
        llPlayer.setBackground(ContextCompat.getDrawable(context,
                active ? R.drawable.bg_player_card : R.drawable.bg_player_card_archived));
        convertView.findViewById(R.id.btnArchive).setVisibility(active ? View.VISIBLE : View.GONE);
        convertView.findViewById(R.id.btnUnarchive).setVisibility(active ? View.GONE : View.VISIBLE);

        // Numero di classifica
        TextView txtRank = convertView.findViewById(R.id.txtRank);
        txtRank.setText(String.valueOf(position + 1));

        TextView txtName = convertView.findViewById(R.id.txtName);
        TextView txtSurname = convertView.findViewById(R.id.txtSurname);
        TextView txtValue = convertView.findViewById(R.id.txtValue);

        txtName.setText(player.name);
        txtSurname.setText(player.getSurnameOrNickname());
        txtValue.setText(player.getVote().toString());

        // Colore per genere: attivo = colore del genere, archiviato = grigio
        int textColor;
        if (!active) {
            textColor = ContextCompat.getColor(context, R.color.list_text_muted);
            txtRank.setTextColor(ContextCompat.getColor(context, R.color.list_text_muted));
        } else {
            textColor = ContextCompat.getColor(context,
                    "F".equals(player.gender) ? R.color.women_color_name_dark : R.color.men_color_name_dark);
            txtRank.setTextColor(ContextCompat.getColor(context, R.color.list_text_secondary));
        }
        txtName.setTextColor(textColor);
        txtSurname.setTextColor(textColor);
        txtValue.setTextColor(textColor);

        convertView.findViewById(R.id.btnArchive)
                .setOnClickListener(v -> openActivityPopUp(position, PopUpType.ARCHIVE_PLAYER));
        convertView.findViewById(R.id.btnUnarchive)
                .setOnClickListener(v -> openActivityPopUp(position, PopUpType.UNARCHIVE_PLAYER));
        convertView.findViewById(R.id.btnEdit)
                .setOnClickListener(v -> openActivityEditPlayer(position));
        convertView.findViewById(R.id.btnDelete)
                .setOnClickListener(v -> openActivityPopUp(position, PopUpType.DELETE_PLAYER));
        convertView.findViewById(R.id.btnInfo)
                .setOnClickListener(v -> openActivityInfo(position));

        return convertView;
    }

    void openActivityPopUp(int position, PopUpType type) {
        Intent intent = new Intent(ActivityAdmin.activityAdmin.getApplicationContext(), ActivityPopUp.class);
        intent.putExtra("player_key", getItem(position).key);
        intent.putExtra("pop_up_type", type);
        ActivityAdmin.activityAdmin.startActivity(intent);
    }

    void openActivityEditPlayer(int position) {
        Intent intent = new Intent(ActivityAdmin.activityAdmin.getApplicationContext(), ActivityEditPlayer.class);
        intent.putExtra("player_key", getItem(position).key);
        ActivityAdmin.activityAdmin.startActivity(intent);
    }

    void openActivityInfo(int position) {
        Intent intent = new Intent(ActivityAdmin.activityAdmin.getApplicationContext(), ActivityInfoPlayer.class);
        intent.putExtra("player_key", getItem(position).key);
        ActivityAdmin.activityAdmin.startActivity(intent);
    }
}