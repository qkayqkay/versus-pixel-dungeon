package com.shatteredpixel.shatteredpixeldungeon.networking;

public class Lobby {
    private String name;
    private String password;
    private boolean hasPassword;

    public Lobby(String name, boolean hasPassword) {
        this.name = name;
        this.hasPassword = hasPassword;
    }

    public String getName() { return name; }
    public boolean hasPassword() { return hasPassword; }
}