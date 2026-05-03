package com.shatteredpixel.shatteredpixeldungeon.networking;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.RiftStone;

public class Gamemode {
    public static Gamemode current = Gamemode.classic(); //TODO set this
    public String gamemodeID;
    public boolean equipRiftStone;
    public boolean isTimed;
    public boolean isBingo;

    public Gamemode(String gamemodeID){
        this.gamemodeID = gamemodeID;
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
}
