package com.example.myapplication;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import com.example.myapplication.Model.Constants;

import java.util.ArrayList;

public class PlayerTeamsAdapter extends BaseAdapter {

    private String type = "";

    public PlayerTeamsAdapter(String type) {
        this.type = type;
    }

    @Override
    public int getCount() {
        return Constants.teams.size();
    }

    @Override
    public ArrayList<Player> getItem(int position) {
        return Constants.teams.get(position).players;
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
            convertView = layoutInflater.inflate(R.layout.layout_player_teams, parent, false);
        }

        TextView txtTeamN = convertView.findViewById(R.id.txtTeamN);
        txtTeamN.setText(convertView.getContext().getResources().getString(R.string.team) + " " + (position + 1));

        TextView txtValueTeam = convertView.findViewById(R.id.txtValueTeam);
        Space spaceValue = convertView.findViewById(R.id.spaceValue);

        if (type.equals("TOURNAMENT")) {
            txtValueTeam.setText(Constants.teams.get(position).getTotalVote() + "");
            txtValueTeam.setVisibility(View.VISIBLE);
            spaceValue.setVisibility(View.VISIBLE);
        } else {
            txtValueTeam.setVisibility(View.GONE);
            spaceValue.setVisibility(View.GONE);
        }

        TextView txtPlayer1Name = convertView.findViewById(R.id.txtPlayer1Name);
        TextView txtPlayer2Name = convertView.findViewById(R.id.txtPlayer2Name);
        TextView txtPlayer3Name = convertView.findViewById(R.id.txtPlayer3Name);
        TextView txtPlayer4Name = convertView.findViewById(R.id.txtPlayer4Name);
        TextView txtPlayer5Name = convertView.findViewById(R.id.txtPlayer5Name);

        TextView txtPlayer1Surname = convertView.findViewById(R.id.txtPlayer1Surname);
        TextView txtPlayer2Surname = convertView.findViewById(R.id.txtPlayer2Surname);
        TextView txtPlayer3Surname = convertView.findViewById(R.id.txtPlayer3Surname);
        TextView txtPlayer4Surname = convertView.findViewById(R.id.txtPlayer4Surname);
        TextView txtPlayer5Surname = convertView.findViewById(R.id.txtPlayer5Surname);

        LinearLayout llPlayer2 = convertView.findViewById(R.id.llPlayer2);
        LinearLayout llPlayer4 = convertView.findViewById(R.id.llPlayer4);
        LinearLayout llPlayer34 = convertView.findViewById(R.id.llPlayer34);
        LinearLayout llPlayer5 = convertView.findViewById(R.id.llPlayer5);

        llPlayer2.setVisibility(View.GONE);
        llPlayer34.setVisibility(View.GONE);
        llPlayer5.setVisibility(View.GONE);

        txtPlayer1Name.setText(getItem(position).get(0).name);
        txtPlayer1Surname.setText(getItem(position).get(0).getSurnameOrNickname());

        if (getItem(position).size() >= 2) {
            llPlayer2.setVisibility(View.VISIBLE);
            txtPlayer2Name.setText(getItem(position).get(1).name);
            txtPlayer2Surname.setText(getItem(position).get(1).getSurnameOrNickname());
        }
        if (getItem(position).size() >= 3) {
            llPlayer34.setVisibility(View.VISIBLE);
            llPlayer4.setVisibility(View.GONE);
            txtPlayer3Name.setText(getItem(position).get(2).name);
            txtPlayer3Surname.setText(getItem(position).get(2).getSurnameOrNickname());
        }
        if (getItem(position).size() >= 4) {
            llPlayer4.setVisibility(View.VISIBLE);
            txtPlayer4Name.setText(getItem(position).get(3).name);
            txtPlayer4Surname.setText(getItem(position).get(3).getSurnameOrNickname());
        }
        if (getItem(position).size() >= 5) {
            llPlayer5.setVisibility(View.VISIBLE);
            txtPlayer5Name.setText(getItem(position).get(4).name);
            txtPlayer5Surname.setText(getItem(position).get(4).getSurnameOrNickname());
        }

        return convertView;
    }
}


