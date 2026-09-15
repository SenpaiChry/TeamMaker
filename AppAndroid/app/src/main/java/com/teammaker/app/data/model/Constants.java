package com.teammaker.app.data.model;

import com.teammaker.app.data.model.Player;
import com.teammaker.app.data.model.Team;
import com.teammaker.app.data.model.Tournament;

import java.util.ArrayList;

public class Constants {
    public static float inputMaxDifference = 1.0F;
    public static float lastDifference = 0.0F;
    public static int nCycle = 0;
    public static ArrayList<Player> players = new ArrayList<>();
    public static ArrayList<Player> playersSelected = new ArrayList<>();
    public static ArrayList<Team> teams = new ArrayList<>();
    public static ArrayList<Tournament> tournaments = new ArrayList<>();
    public static boolean logged = false;
    public static boolean downloadEnd = false;
    public static final String dbRoot = "teammaker/";
//    public static final String dbRoot = "teammakerStaging/";
}
