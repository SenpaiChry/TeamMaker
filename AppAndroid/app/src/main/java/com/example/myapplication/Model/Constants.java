package com.example.myapplication.Model;

import com.example.myapplication.Player;
import com.example.myapplication.Team;
import com.example.myapplication.Tournament;

import java.util.ArrayList;

public class Constants {
    public static String[] statsDescription = {"Altezza", "Agilità", "Fallosità", "Palleggio", "Ricezione", "Attacco", "Muro", "Battuta", "Visione di gioco", "Gioco di squadra", "Mentalità", "Bonus"};
    public static String[] statsDescriptionEng = {"height", "agility", "fallacy", "pass", "reception", "attack", "wall", "serve", "game-vision", "team-play", "mentality", "bonus"};
    public static String[] valueHeight = {"<150cm", "151-160cm", "161-170cm", "171-180cm", "181-190cm", "191-200cm", ">200cm"};
    public static int[] statsMax = {6, 4, 2, 4, 4, 6, 4, 6, 4, 2, 2, 2};
    public static double[] statsStep = {1, 1, 1, 1, 1, 1.5, 1, 1.5, 2, 1, 1, 1};
    public static float inputMaxDifference = 1.0F;
    public static float lastDifference = 0.0F;
    public static int nCycle = 0;
    public static ArrayList<Player> players = new ArrayList<>();
    public static ArrayList<Player> playersSelected = new ArrayList<>();
    public static ArrayList<Team> teams = new ArrayList<>();
    public static ArrayList<Tournament> tournaments = new ArrayList<>();
    public static boolean logged = true; //TODO PRIMA DELLA RELEASE
    public static String password = null; // letta in tempo reale dal nodo admin-pw (AdminUtility)
    public static boolean downloadEnd = false;
    public static final String dbRoot = "teammaker/";
//    public static final String dbRoot = "teammakerStaging/";
//    public static final String dbRoot = "dbRizzi/";
//    public static final String dbRoot = "dbEmpty/";
}
