package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.myapplication.Utility.PlayerUtility;
import com.example.myapplication.Model.Constants;

import java.util.ArrayList;

public class PlayerGenerateAdapter extends BaseAdapter {
    final private Context context;
    private final ArrayList<Player> playersToSee;

    public PlayerGenerateAdapter(Context context, ArrayList<Player> playersToSee) {
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
            convertView = layoutInflater.inflate(R.layout.layout_player_generate, parent, false);
        }

        Player player = getItem(position);
        boolean isSelected = Constants.playersSelected.contains(player);

        TextView txtName = convertView.findViewById(R.id.txtName);
        TextView txtSurname = convertView.findViewById(R.id.txtSurname);

        txtName.setText(player.name);
        txtSurname.setText(player.surname);

        View viewImage = convertView.findViewById(R.id.icon_background);
        ImageView imageView = convertView.findViewById(R.id.imgCategory);

        boolean isF = "F".equals(player.gender);
        if (isF) {
            imageView.setImageResource(R.drawable.women_icon);
            viewImage.setBackground(ContextCompat.getDrawable(context, R.drawable.women_icon_background));
        } else {
            imageView.setImageResource(R.drawable.men_icon);
            viewImage.setBackground(ContextCompat.getDrawable(context, R.drawable.men_icon_background));
        }

        int nameColor = ContextCompat.getColor(context,
                isF ? R.color.women_color_name_dark : R.color.men_color_name_dark);
        txtName.setTextColor(nameColor);
        txtSurname.setTextColor(nameColor);

        ImageView btnAdd = convertView.findViewById(R.id.btnAdd);
        ImageView btnRemove = convertView.findViewById(R.id.btnRemove);
        LinearLayout llPlayer = convertView.findViewById(R.id.llPlayer);

        llPlayer.setBackground(ContextCompat.getDrawable(context,
                isSelected ? R.drawable.bg_player_card_selected : R.drawable.bg_player_card));
        btnAdd.setVisibility(isSelected ? View.GONE : View.VISIBLE);
        btnRemove.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        llPlayer.setOnClickListener(v -> {
            boolean selected = Constants.playersSelected.contains(player);

            if (selected) {
                Constants.playersSelected.remove(player);
                llPlayer.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_player_card));
                btnAdd.setVisibility(View.VISIBLE);
                btnRemove.setVisibility(View.GONE);
            } else {
                Constants.playersSelected.add(player);
                llPlayer.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_player_card_selected));
                btnAdd.setVisibility(View.GONE);
                btnRemove.setVisibility(View.VISIBLE);
            }

            ActivityGenerate.txtNSelected.setText(String.valueOf(Constants.playersSelected.size()));

            if (Constants.playersSelected.isEmpty()) {
                ActivityGenerate.switchSelectDeselect("DESELECT");
            } else if (Constants.playersSelected.size() == PlayerUtility.getPlayersActive(true).size()) {
                ActivityGenerate.switchSelectDeselect("SELECT");
            }
        });

        return convertView;
    }
}