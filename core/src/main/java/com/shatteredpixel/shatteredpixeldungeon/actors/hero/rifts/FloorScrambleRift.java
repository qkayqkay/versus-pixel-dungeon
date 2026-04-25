package com.shatteredpixel.shatteredpixeldungeon.actors.hero.rifts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;

public class FloorScrambleRift extends Rift{
    public static final FloorScrambleRift INSTANCE = new FloorScrambleRift();

    private FloorScrambleRift() {
        riftId = "floor_scramble_rift";
        cost = 8;
    }

    @Override
    public void onCast(Hero hero) {
        System.out.println("casting floor scramble rift!");
        //targetIds = new String[]{ null }; // for now, on the server, if no target is set, it will be random.
        super.onCast(hero);
    }

}

