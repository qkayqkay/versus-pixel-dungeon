package com.shatteredpixel.shatteredpixeldungeon.networking;

public class ChatMessage {
    public Player player = null;
    public String message;
    public boolean isServerMessage;
    public String cachedName; // todo. When a player leaves a server and a new person joins it, the Player object of that player who left won't exist --> crash. Hence cache the name and use that instead.

    public ChatMessage(Player player, String message) {
        this.player = player;
        this.message = message;
        this.isServerMessage = false;
    }
    public ChatMessage(String message) { // for server messages only
        this.message = message;
        this.isServerMessage = true;
    }
}