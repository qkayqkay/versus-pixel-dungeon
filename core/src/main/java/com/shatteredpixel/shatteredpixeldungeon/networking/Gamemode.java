package com.shatteredpixel.shatteredpixeldungeon.networking;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.RiftStone;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

public class Gamemode {
    public static Gamemode current = Gamemode.classic(); //TODO set this
    private static Gamemode[] gamemodes = new Gamemode[]{Gamemode.classic(), Gamemode.classicRift(), Gamemode.bingo()};
    public String gamemodeID;
    public String gamemodeName;
    public boolean equipRiftStone;
    public boolean isTimed;
    public boolean isBingo;

    public Gamemode(String gamemodeID){
        this.gamemodeID = gamemodeID;
        System.out.println(this.getClass().getName());
        System.out.println(this.getClass().getPackage().getName());
        this.gamemodeName = Messages.get(this, gamemodeID+"_name");
    }

    public void onGameStart() {

    }

    public void onHeroCreate(Hero h){ // called when the hero object is created.
        if(equipRiftStone) {
            RiftStone stone = new RiftStone();
            h.belongings.riftStone = stone;
            stone.activate(h);
        }

    }

    public static Gamemode fromID(String id) {
        switch (id) {
            case "classic": return classic();
            case "classic_rift": return classicRift();
            case "bingo": return bingo();
            default: return classic();
        }
    }

    public static Gamemode classic() {
        Gamemode g = new Gamemode("classic");
        g.isTimed = true;
        return g;
    }

    public static Gamemode classicRift() {
        Gamemode g = new Gamemode("classic_rift");
        g.isTimed = true;
        g.equipRiftStone = true;
        return g;
    }

    public static Gamemode bingo() {
        Gamemode g = new Gamemode("bingo");
        g.isBingo = true;
        return g;
    }

    public static Gamemode[] listGamemodes(){
        return gamemodes;
    }
}
