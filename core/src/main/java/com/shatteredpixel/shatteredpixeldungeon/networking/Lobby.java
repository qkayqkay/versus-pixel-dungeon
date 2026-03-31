package com.shatteredpixel.shatteredpixeldungeon.networking;

import java.util.ArrayList;


public class Lobby {
    private String name;
    private boolean hasPassword;
    private int numPlayers;
    private ArrayList<Player> players;
    private ArrayList<String> admins;
    private ArrayList<String> superAdmins;
    private boolean inGame;
    private int maxPlayers;
    private String id;

    public Lobby(String name, boolean hasPassword, ArrayList<String> admins, ArrayList<String> superAdmins, int numPlayers) {
        this.name = name;
        this.hasPassword = hasPassword;
        this.admins = admins;
        this.superAdmins = superAdmins;
        this.numPlayers = numPlayers;
        this.players = new ArrayList<Player>();
        this.inGame = false;
    }

    public void setID(String id) { this.id = id; }
    public void setInGame(boolean inGame) { this.inGame = inGame; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public void setPlayers(ArrayList<Player> players) { this.players = players; }
    public void setAdmins(ArrayList<String> admins) { this.admins = admins; }
    public void setSuperAdmins(ArrayList<String> superAdmins) { this.superAdmins = superAdmins; }

    public String getID() { return id; }
    public boolean isInGame() { return inGame; }
    public int getMaxPlayers() { return maxPlayers; }
    public ArrayList<Player> getPlayers() { return players; }
    public String getName() { return name; }
    public boolean hasPassword() { return hasPassword; }
    public ArrayList<String> getAdmins() { return admins; }
    public boolean isAdmin(String playerID) { return admins.contains(playerID); } //TODO UPDATE THIS LIKE LINE UNDERNEATH
    public boolean isAdmin() {
        String id = NetworkManager.INSTANCE.self.getID();

        boolean inAdmins = admins.contains(id);
        boolean inSuperAdmins = superAdmins.contains(id);

        System.out.println("Checking admin status for ID: " + id);
        System.out.println("In admins: " + inAdmins);
        System.out.println("In superAdmins: " + inSuperAdmins);

        return inAdmins || inSuperAdmins;
    }    public int getPlayerCount() { return numPlayers; }
}