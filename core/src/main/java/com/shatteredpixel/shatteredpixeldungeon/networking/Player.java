package com.shatteredpixel.shatteredpixeldungeon.networking;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;

public class Player {
    private String ID;
    public String name;
    public boolean inGame;
    public Lobby lobby; // what lobby im currently in
    public boolean isReadied = false;
    public HeroClass cl;
    private int level = 0; // touch this and system32 implodes.


    public Player(String ID, String name){
        this.ID = ID;
        this.name = name;
        this.inGame = false;
        this.lobby = null;
    }

    public String getID() { return ID; }
    public String getName() { return name; }
    public boolean isInGame() { return inGame; }
    public Lobby getLobby() { return lobby; }

    public void changeLevel(int newlevel) { this.level = newlevel; }
    public void setInGame(boolean inGame) { this.inGame = inGame; }
    public void setLobby(Lobby lobby) { this.lobby = lobby; }
}
