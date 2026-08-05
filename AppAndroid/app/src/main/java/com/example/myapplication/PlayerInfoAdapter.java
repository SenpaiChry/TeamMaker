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

import com.example.myapplication.Model.Constants;

import java.util.HashMap;

public class PlayerInfoAdapter extends BaseAdapter {
    HashMap<String, Object> stats;
    Context context;

    public PlayerInfoAdapter(Context context, HashMap<String, Object> s) {
        this.context = context;
        this.stats = s;
    }

    @Override
    public int getCount() {
        return stats.size();
    }

    @Override
    public Float getItem(int position) {
        return Float.parseFloat(String.valueOf(stats.get(Constants.statsDescriptionEng[position])));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            convertView = layoutInflater.inflate(R.layout.layout_info, parent, false);
        }

        TextView txtName = convertView.findViewById(R.id.txtName);
        txtName.setText(Constants.statsDescription[position]);

        LinearLayout llImagesContainer = convertView.findViewById(R.id.llImages);
        TextView txtHeight = convertView.findViewById(R.id.txtHeight);

        if (Constants.statsDescription[position].equalsIgnoreCase("ALTEZZA")) {
            txtHeight.setVisibility(View.VISIBLE);
            txtHeight.setText(Constants.valueHeight[getItem(position).intValue()]);
        } else  {
            for (int i = 0; i <= Constants.statsMax[position] / Constants.statsStep[position]; i ++) {
                ImageView image = new ImageView(context);

                if (getItem(position) / Constants.statsStep[position] >= i) {
                    image.setImageResource(R.drawable.star_full);
                } else {
                    image.setImageResource(R.drawable.star_empty);
                }

                image.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                llImagesContainer.addView(image);
            }
        }

        return convertView;
    }
}