package com.example.myapplication.Utility;

import com.example.myapplication.StatDefinition;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Conversione StatDefinition <-> Firebase in un solo posto. */
public class StatMapper {

    public static StatDefinition fromSnapshot(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        StatDefinition s = new StatDefinition();
        s.key = snapshot.getKey();
        s.label = snapshot.child("label").getValue(String.class);
        String type = snapshot.child("type").getValue(String.class);
        if (type != null && !type.isEmpty()) s.type = type;

        Double max = snapshot.child("max").getValue(Double.class);
        if (max != null) s.max = max;
        Double step = snapshot.child("step").getValue(Double.class);
        if (step != null) s.step = step;
        Integer order = snapshot.child("order").getValue(Integer.class);
        if (order != null) s.order = order;

        Boolean allowBonus = snapshot.child("allow_bonus").getValue(Boolean.class);
        if (allowBonus != null) s.allowBonus = allowBonus;

        if (snapshot.hasChild("values")) {
            s.values = new ArrayList<>();
            for (DataSnapshot v : snapshot.child("values").getChildren()) {
                s.values.add(v.getValue(String.class));
            }
        }
        return s;
    }

    public static Map<String, Object> toMap(StatDefinition s) {
        Map<String, Object> data = new HashMap<>();
        data.put("label", s.label);
        data.put("type", s.type);
        data.put("max", s.max);
        data.put("step", s.step);
        data.put("order", s.order);
        // Salva allow_bonus solo per STARS (per RANGE non ha senso)
        if (StatDefinition.TYPE_STARS.equals(s.type) && s.allowBonus) {
            data.put("allow_bonus", true);
        }
        if (StatDefinition.TYPE_RANGE.equals(s.type) && s.values != null) {
            data.put("values", s.values);
        }
        return data;
    }
}
